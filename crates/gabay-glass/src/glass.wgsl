// Liquid-glass surface: rounded-rect SDF refraction of a backdrop texture,
// small blur, saturation, tint wash and a specular rim.
//
// The refraction model follows Kyant0/backdrop (Apache-2.0): inside a band of
// `lens.x` pixels at the surface edge, the sample point is displaced along the
// SDF gradient by circle_map(1 - depth / band) * amount.
//
// Output is premultiplied alpha; blend with One / OneMinusSrcAlpha.

struct Glass {
    rect: vec4<f32>,   // x, y, width, height in pixels (top-left origin)
    radii: vec4<f32>,  // top-left, top-right, bottom-right, bottom-left
    lens: vec4<f32>,   // refraction height, refraction amount, blur radius, saturation
    wash: vec4<f32>,   // straight-alpha tint laid over the backdrop
    rim: vec4<f32>,    // rim light rgb, intensity
    view: vec4<f32>,   // viewport width, viewport height, dispersion, press 0..1
};

@group(0) @binding(0) var backdrop: texture_2d<f32>;
@group(0) @binding(1) var backdrop_sampler: sampler;
@group(0) @binding(2) var<uniform> g: Glass;

struct VsOut {
    @builtin(position) position: vec4<f32>,
    @location(0) pixel: vec2<f32>,
};

@vertex
fn vs_main(@builtin(vertex_index) index: u32) -> VsOut {
    // Triangle strip: (0,0) (1,0) (0,1) (1,1).
    let corner = vec2<f32>(f32(index & 1u), f32((index >> 1u) & 1u));
    let pixel = g.rect.xy + corner * g.rect.zw;
    let ndc = vec2<f32>(pixel.x / g.view.x * 2.0 - 1.0, 1.0 - pixel.y / g.view.y * 2.0);
    var out: VsOut;
    out.position = vec4<f32>(ndc, 0.0, 1.0);
    out.pixel = pixel;
    return out;
}

fn radius_at(p: vec2<f32>) -> f32 {
    if (p.x >= 0.0) {
        if (p.y <= 0.0) {
            return g.radii.y;
        }
        return g.radii.z;
    }
    if (p.y <= 0.0) {
        return g.radii.x;
    }
    return g.radii.w;
}

fn sd_rounded_rect(p: vec2<f32>, half_size: vec2<f32>, radius: f32) -> f32 {
    let q = abs(p) - (half_size - vec2<f32>(radius));
    return length(max(q, vec2<f32>(0.0))) + min(max(q.x, q.y), 0.0) - radius;
}

// Outward unit gradient of the rounded-rect SDF.
fn grad_sd(p: vec2<f32>, half_size: vec2<f32>, radius: f32) -> vec2<f32> {
    let q = abs(p) - (half_size - vec2<f32>(radius));
    let m = max(q, vec2<f32>(0.0));
    let l = length(m);
    if (l > 0.00001) {
        return sign(p) * (m / l);
    }
    let gx = step(q.y, q.x);
    return sign(p) * vec2<f32>(gx, 1.0 - gx);
}

fn sample_backdrop(px: vec2<f32>) -> vec3<f32> {
    let uv = clamp(px / g.view.xy, vec2<f32>(0.0), vec2<f32>(1.0));
    return textureSampleLevel(backdrop, backdrop_sampler, uv, 0.0).rgb;
}

// Two concentric 8-tap rings plus the centre: cheap and good enough for the
// small radii used on chrome.
fn blurred(px: vec2<f32>, radius: f32) -> vec3<f32> {
    var acc = sample_backdrop(px);
    if (radius <= 0.01) {
        return acc;
    }
    var total = 1.0;
    for (var ring = 1; ring <= 2; ring = ring + 1) {
        let r = radius * f32(ring) * 0.5;
        for (var i = 0; i < 8; i = i + 1) {
            let a = f32(i) * 0.785398;
            acc = acc + sample_backdrop(px + vec2<f32>(cos(a), sin(a)) * r);
            total = total + 1.0;
        }
    }
    return acc / total;
}

@fragment
fn fs_main(in: VsOut) -> @location(0) vec4<f32> {
    let half_size = g.rect.zw * 0.5;
    let p = in.pixel - (g.rect.xy + half_size);
    let min_half = min(half_size.x, half_size.y);
    let radius = min(radius_at(p), min_half);
    let sd = sd_rounded_rect(p, half_size, radius);
    let coverage = clamp(0.5 - sd, 0.0, 1.0);
    if (coverage <= 0.0) {
        discard;
    }

    let inside = max(-sd, 0.0);
    let grad = grad_sd(p, half_size, min(radius * 1.5, min_half));

    // Refraction: pull the sample point inward inside the edge band.
    let band = g.lens.x;
    let amount = g.lens.y * (1.0 + 0.25 * g.view.w);
    var displacement = vec2<f32>(0.0);
    if (band > 0.0 && inside < band) {
        let t = 1.0 - inside / band;
        let d = (1.0 - sqrt(max(1.0 - t * t, 0.0))) * amount;
        displacement = d * grad;
    }
    let at = in.pixel - displacement;

    var c = blurred(at, g.lens.z);
    let dispersion = g.view.z;
    if (dispersion > 0.0) {
        let k = dispersion * (p.x * p.y) / max(half_size.x * half_size.y, 1.0);
        let shift = displacement * k;
        c.r = sample_backdrop(at - shift).r;
        c.b = sample_backdrop(at + shift).b;
    }

    let luma = dot(c, vec3<f32>(0.2126, 0.7152, 0.0722));
    c = mix(vec3<f32>(luma), c, g.lens.w);
    c = mix(c, g.wash.rgb, g.wash.a);

    // Specular rim: brightest where the edge faces the light (top-left).
    let light = normalize(vec2<f32>(-0.6, -0.8));
    let facing = abs(dot(grad, light));
    let edge = 1.0 - smoothstep(0.0, 1.5, inside);
    c = c + g.rim.rgb * (pow(facing, 1.5) * edge * g.rim.a);

    return vec4<f32>(clamp(c, vec3<f32>(0.0), vec3<f32>(1.0)) * coverage, coverage);
}
