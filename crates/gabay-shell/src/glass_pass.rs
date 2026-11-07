//! Draws liquid-glass panels over the finished map texture.

use gabay_glass::{GlassUniform, Params, Surface, SHADER, UNIFORM_BYTES};
use wgpu::*;

const PARAMS: Params = Params {
    blur_radius: 3.0,
    refraction_height: 14.0,
    refraction_amount: 26.0,
    saturation: 1.4,
    dispersion: 0.0,
    rim: 0.6,
};
const FORMAT: TextureFormat = TextureFormat::Rgba8Unorm;

pub struct GlassPass {
    pipeline: RenderPipeline,
    layout: BindGroupLayout,
    sampler: Sampler,
    backdrop: Option<Texture>,
}

impl GlassPass {
    pub fn new(device: &Device) -> Self {
        let module = device.create_shader_module(ShaderModuleDescriptor {
            label: Some("glass"),
            source: ShaderSource::Wgsl(SHADER.into()),
        });
        let entry = |binding, visibility, ty| BindGroupLayoutEntry { binding, visibility, ty, count: None };
        let layout = device.create_bind_group_layout(&BindGroupLayoutDescriptor {
            label: Some("glass"),
            entries: &[
                entry(0, ShaderStages::FRAGMENT, BindingType::Texture {
                    sample_type: TextureSampleType::Float { filterable: true },
                    view_dimension: TextureViewDimension::D2,
                    multisampled: false,
                }),
                entry(1, ShaderStages::FRAGMENT, BindingType::Sampler(SamplerBindingType::Filtering)),
                entry(2, ShaderStages::VERTEX_FRAGMENT, BindingType::Buffer {
                    ty: BufferBindingType::Uniform,
                    has_dynamic_offset: false,
                    min_binding_size: BufferSize::new(UNIFORM_BYTES as u64),
                }),
            ],
        });
        let pl = device.create_pipeline_layout(&PipelineLayoutDescriptor {
            label: Some("glass"),
            bind_group_layouts: &[&layout],
            push_constant_ranges: &[],
        });
        let over = BlendComponent {
            src_factor: BlendFactor::One,
            dst_factor: BlendFactor::OneMinusSrcAlpha,
            operation: BlendOperation::Add,
        };
        let pipeline = device.create_render_pipeline(&RenderPipelineDescriptor {
            label: Some("glass"),
            layout: Some(&pl),
            vertex: VertexState { module: &module, entry_point: Some("vs_main"), buffers: &[], compilation_options: Default::default() },
            fragment: Some(FragmentState {
                module: &module,
                entry_point: Some("fs_main"),
                compilation_options: Default::default(),
                targets: &[Some(ColorTargetState {
                    format: FORMAT,
                    blend: Some(BlendState { color: over, alpha: over }),
                    write_mask: ColorWrites::ALL,
                })],
            }),
            primitive: PrimitiveState { topology: PrimitiveTopology::TriangleStrip, ..Default::default() },
            depth_stencil: None,
            multisample: MultisampleState::default(),
            multiview: None,
            cache: None,
        });
        let sampler = device.create_sampler(&SamplerDescriptor {
            mag_filter: FilterMode::Linear,
            min_filter: FilterMode::Linear,
            ..Default::default()
        });
        Self { pipeline, layout, sampler, backdrop: None }
    }

    /// `target` must have RENDER_ATTACHMENT and COPY_SRC usage.
    pub fn draw(&mut self, device: &Device, queue: &Queue, target: &Texture, panels: &[Surface], scale: f32, dark: bool) {
        let (w, h) = (target.width(), target.height());
        let rects: Vec<([f32; 4], f32)> = panels
            .iter()
            .filter_map(|p| p.resolve(w as f32 / scale, h as f32 / scale))
            .map(|(r, radius)| (r.map(|v| v * scale), radius))
            .collect();
        if rects.is_empty() {
            return;
        }
        if self.backdrop.as_ref().is_none_or(|b| b.width() != w || b.height() != h) {
            self.backdrop = Some(device.create_texture(&TextureDescriptor {
                label: Some("glass-backdrop"),
                size: Extent3d { width: w, height: h, depth_or_array_layers: 1 },
                mip_level_count: 1,
                sample_count: 1,
                dimension: TextureDimension::D2,
                format: FORMAT,
                usage: TextureUsages::TEXTURE_BINDING | TextureUsages::COPY_DST,
                view_formats: &[],
            }));
        }
        let Some(backdrop) = &self.backdrop else { return };
        // Earthy Minimal: a green-black wash in dark, cream in light; rim is cream.
        let (wash, rim) = if dark { ([0.106, 0.137, 0.110, 0.52], [0.97, 0.96, 0.93]) } else { ([0.973, 0.965, 0.933, 0.50], [1.0, 1.0, 1.0]) };

        let mut enc = device.create_command_encoder(&CommandEncoderDescriptor { label: Some("glass") });
        enc.copy_texture_to_texture(target.as_image_copy(), backdrop.as_image_copy(), Extent3d { width: w, height: h, depth_or_array_layers: 1 });

        let view = backdrop.create_view(&TextureViewDescriptor::default());
        let groups: Vec<BindGroup> = rects
            .iter()
            .map(|(r, radius)| {
                let u = GlassUniform::new(*r, *radius, PARAMS, scale, wash, rim, [w as f32, h as f32], 0.0);
                let buf = device.create_buffer(&BufferDescriptor {
                    label: Some("glass-uniform"),
                    size: UNIFORM_BYTES as u64,
                    usage: BufferUsages::UNIFORM | BufferUsages::COPY_DST,
                    mapped_at_creation: false,
                });
                queue.write_buffer(&buf, 0, &u.to_bytes());
                device.create_bind_group(&BindGroupDescriptor {
                    label: Some("glass"),
                    layout: &self.layout,
                    entries: &[
                        BindGroupEntry { binding: 0, resource: BindingResource::TextureView(&view) },
                        BindGroupEntry { binding: 1, resource: BindingResource::Sampler(&self.sampler) },
                        BindGroupEntry { binding: 2, resource: buf.as_entire_binding() },
                    ],
                })
            })
            .collect();
        {
            let tview = target.create_view(&TextureViewDescriptor::default());
            let mut pass = enc.begin_render_pass(&RenderPassDescriptor {
                label: Some("glass"),
                color_attachments: &[Some(RenderPassColorAttachment {
                    view: &tview,
                    resolve_target: None,
                    ops: Operations { load: LoadOp::Load, store: StoreOp::Store },
                    depth_slice: None,
                })],
                depth_stencil_attachment: None,
                timestamp_writes: None,
                occlusion_query_set: None,
            });
            pass.set_pipeline(&self.pipeline);
            for g in &groups {
                pass.set_bind_group(0, g, &[]);
                pass.draw(0..4, 0..1);
            }
        }
        queue.submit(Some(enc.finish()));
    }
}
