//! Bridges the map crate to Dioxus Native's GPU canvas: draws the scene with
//! Vello into a wgpu texture each frame.
//!
//! Not implemented: raster satellite/topography layers, 3D tilt.

mod glass_pass;

pub use gabay_glass::{Panel, Surface};
use glass_pass::GlassPass;

use std::path::PathBuf;
use std::sync::mpsc::{channel, Receiver, Sender};

use dioxus_native::{CustomPaintCtx, CustomPaintSource, DeviceHandle, TextureHandle};
use gabay_map::{
    build_scene, overlay_items, Camera, Color, DrawItem, LonLat, MapTheme, Overlays, TileData,
    VectorSource,
};
use gabay_vault::{Layer, Vault};
use vello::kurbo::{Affine, BezPath, Cap, Circle, Join, Point, Stroke};
use vello::peniko::{Color as VColor, Fill};
use vello::{AaConfig, RenderParams, Renderer, RendererOptions, Scene};
use wgpu::{Device, Extent3d, Queue, Texture, TextureDescriptor, TextureDimension, TextureFormat, TextureUsages};

/// Messages from the UI thread to the painter.
pub enum MapMsg {
    Pan { dx: f64, dy: f64 },
    ZoomAbout { x: f64, y: f64, delta: f64 },
    Rotate(f64),
    Center(LonLat, f64),
    Overlays(Overlays),
    Dark(bool),
    Panels(Vec<Surface>),
}

pub struct MapPaintSource {
    tx: Sender<MapMsg>,
    rx: Receiver<MapMsg>,
    vault_dir: PathBuf,
    source: Option<VectorSource>,
    source_tried: bool,
    cam: Camera,
    overlays: Overlays,
    dark: bool,
    panels: Vec<Surface>,
    gpu: Option<Gpu>,
}

struct Gpu {
    device: Device,
    queue: Queue,
    renderer: Renderer,
    glass: GlassPass,
    scene: Scene,
    front: Option<(Texture, TextureHandle)>,
    back: Option<(Texture, TextureHandle)>,
}

impl MapPaintSource {
    pub fn new(vault_dir: PathBuf) -> Self {
        let (tx, rx) = channel();
        // Manila until the first fix or restored camera arrives.
        let cam = Camera::new(LonLat::new(120.98, 14.60), 11.0, 1.0, 1.0);
        Self { tx, rx, vault_dir, source: None, source_tried: false, cam, overlays: Overlays::default(), dark: true, panels: Vec::new(), gpu: None }
    }

    pub fn sender(&self) -> Sender<MapMsg> {
        self.tx.clone()
    }

    fn drain(&mut self) {
        while let Ok(m) = self.rx.try_recv() {
            match m {
                MapMsg::Pan { dx, dy } => self.cam.pan_pixels(dx, dy),
                MapMsg::ZoomAbout { x, y, delta } => {
                    // Negative focal point means "view centre".
                    let f = if x < 0.0 || y < 0.0 { [self.cam.width / 2.0, self.cam.height / 2.0] } else { [x, y] };
                    self.cam.zoom_about(f, delta)
                }
                MapMsg::Rotate(d) => self.cam.rotate_by(d),
                MapMsg::Center(c, z) => {
                    self.cam.center = c;
                    self.cam.zoom = z.clamp(Camera::MIN_ZOOM, Camera::MAX_ZOOM);
                }
                MapMsg::Overlays(o) => self.overlays = o,
                MapMsg::Dark(d) => self.dark = d,
                MapMsg::Panels(p) => self.panels = p,
            }
        }
    }

    /// Opens the vector archive on first use; only the header is read here.
    fn ensure_source(&mut self) {
        if self.source.is_some() || self.source_tried {
            return;
        }
        self.source_tried = true;
        match Vault::new(&self.vault_dir).open_layer(Layer::Vector) {
            Ok(Some(a)) => self.source = Some(VectorSource::new(a)),
            Ok(None) => log::warn!("vector map file missing"),
            Err(e) => log::warn!("vector map unreadable: {e}"),
        }
    }

    fn items(&mut self, theme: &MapTheme) -> Vec<DrawItem> {
        let Some(src) = &self.source else { return Vec::new() };
        let (lo, hi) = src.zoom_range();
        let tz = self.cam.tile_zoom(lo, hi);
        let tiles: Vec<TileData> = self
            .cam
            .visible_tiles(tz)
            .into_iter()
            .filter_map(|id| src.tile(id).map(|layers| TileData { id, layers }))
            .collect();
        let mut items = build_scene(&self.cam, &tiles, theme);
        items.extend(overlay_items(&self.cam, &self.overlays, theme));
        items
    }
}

fn vc(c: Color) -> VColor {
    VColor::from_rgba8(c.r, c.g, c.b, c.a)
}

fn paint(scene: &mut Scene, items: &[DrawItem], scale: f64) {
    let at = Affine::scale(scale);
    for item in items {
        match item {
            DrawItem::Fill { rings, color } => {
                let mut path = BezPath::new();
                for ring in rings {
                    let mut it = ring.iter();
                    if let Some(p) = it.next() {
                        path.move_to(Point::new(f64::from(p[0]), f64::from(p[1])));
                        for p in it {
                            path.line_to(Point::new(f64::from(p[0]), f64::from(p[1])));
                        }
                        path.close_path();
                    }
                }
                scene.fill(Fill::EvenOdd, at, vc(*color), None, &path);
            }
            DrawItem::Stroke { points, width, color } => {
                let mut it = points.iter();
                let Some(p) = it.next() else { continue };
                let mut path = BezPath::new();
                path.move_to(Point::new(f64::from(p[0]), f64::from(p[1])));
                for p in it {
                    path.line_to(Point::new(f64::from(p[0]), f64::from(p[1])));
                }
                let style = Stroke::new(f64::from(*width)).with_caps(Cap::Round).with_join(Join::Round);
                scene.stroke(&style, at, vc(*color), None, &path);
            }
            DrawItem::Dot { center, radius, color } => {
                let c = Circle::new(Point::new(f64::from(center[0]), f64::from(center[1])), f64::from(*radius));
                scene.fill(Fill::NonZero, at, vc(*color), None, &c);
            }
        }
    }
}

fn make_texture(device: &Device, w: u32, h: u32) -> Texture {
    let w = w.max(1);
    let h = h.max(1);
    device.create_texture(&TextureDescriptor {
        label: Some("gabay-map"),
        size: Extent3d { width: w, height: h, depth_or_array_layers: 1 },
        mip_level_count: 1,
        sample_count: 1,
        dimension: TextureDimension::D2,
        format: TextureFormat::Rgba8Unorm,
        usage: TextureUsages::STORAGE_BINDING | TextureUsages::TEXTURE_BINDING | TextureUsages::COPY_SRC | TextureUsages::RENDER_ATTACHMENT,
        view_formats: &[],
    })
}

impl CustomPaintSource for MapPaintSource {
    fn resume(&mut self, handle: &DeviceHandle) {
        match Renderer::new(&handle.device, RendererOptions::default()) {
            Ok(renderer) => {
                self.gpu = Some(Gpu {
                    device: handle.device.clone(),
                    queue: handle.queue.clone(),
                    glass: GlassPass::new(&handle.device),
                    renderer,
                    scene: Scene::new(),
                    front: None,
                    back: None,
                });
            }
            Err(e) => log::error!("vello renderer init failed: {e}"),
        }
    }

    fn suspend(&mut self) {
        self.gpu = None;
    }

    fn render(&mut self, mut ctx: CustomPaintCtx<'_>, width: u32, height: u32, scale: f64) -> Option<TextureHandle> {
        log::info!("[Gabay Shell] CustomPaintSource::render called! {width}x{height} scale={scale}");
        eprintln!("[Gabay Shell] CustomPaintSource::render called! {width}x{height} scale={scale}");
        if width == 0 || height == 0 || !scale.is_finite() || scale <= 0.0 {
            return None;
        }
        self.drain();
        self.ensure_source();
        self.cam.width = f64::from(width) / scale;
        self.cam.height = f64::from(height) / scale;
        let theme = if self.dark { MapTheme::DARK } else { MapTheme::LIGHT };
        let items = self.items(&theme);

        let gpu = self.gpu.as_mut()?;
        // Drop the back buffer if the size changed.
        let stale = gpu.back.as_ref().is_some_and(|(t, _)| t.width() != width || t.height() != height);
        if stale {
            if let Some((_, h)) = gpu.back.take() {
                ctx.unregister_texture(h);
            }
        }
        if gpu.back.is_none() {
            let t = make_texture(&gpu.device, width, height);
            let h = ctx.register_texture(t.clone());
            gpu.back = Some((t, h));
        }
        let (tex, handle) = gpu.back.as_ref().map(|(t, h)| (t.clone(), h.clone()))?;

        gpu.scene.reset();
        paint(&mut gpu.scene, &items, scale);
        let view = tex.create_view(&wgpu::TextureViewDescriptor::default());
        let params = RenderParams {
            base_color: vc(theme.background),
            width,
            height,
            antialiasing_method: AaConfig::Area,
        };
        if let Err(e) = gpu.renderer.render_to_texture(&gpu.device, &gpu.queue, &gpu.scene, &view, &params) {
            log::error!("map render failed: {e}");
            return None;
        }
        gpu.glass.draw(&gpu.device, &gpu.queue, &tex, &self.panels, scale as f32, self.dark);
        std::mem::swap(&mut gpu.front, &mut gpu.back);
        Some(handle)
    }
}
