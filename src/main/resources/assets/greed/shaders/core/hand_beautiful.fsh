#version 150

uniform sampler2D Sampler0;

layout(std140) uniform HandBeautifulParams {
    vec4 Color;
    vec4 Color2;
    vec4 Params0;
    vec4 Params1;
    vec4 TexelSize;
};

in vec2 TexCoord;
out vec4 OutColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 345.45));
    p += dot(p, p + 34.345);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 4; i++) {
        value += noise(p) * amplitude;
        p = p * 2.02 + vec2(8.4, 5.7);
        amplitude *= 0.5;
    }
    return value;
}

float maskAt(vec2 uv) {
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
        return 0.0;
    }
    return 1.0 - step(1.0, texture(Sampler0, uv).r);
}

void main() {
    vec2 uv = TexCoord;
    vec2 px = TexelSize.xy;

    float time = Params0.x;
    float speed = Params0.y;
    float scale = Params0.z;
    float outlineK = Params0.w;
    float glowK = Params1.x;
    float fillK = Params1.y;
    float alphaK = Params1.z;

    float inside = maskAt(uv);

    float width = 1.0 + outlineK * 2.0;
    float glowRadius = 6.0 + glowK * 6.0;

    float nearMin = 1.0;
    float nearMax = 0.0;
    float glowAcc = 0.0;

    for (int i = 0; i < 12; i++) {
        float a = float(i) * 0.5235988;
        vec2 d = vec2(cos(a), sin(a));
        float n = maskAt(uv + d * px * width);
        nearMin = min(nearMin, n);
        nearMax = max(nearMax, n);
        glowAcc += maskAt(uv + d * px * glowRadius * 0.5);
        glowAcc += maskAt(uv + d * px * glowRadius);
    }
    glowAcc /= 24.0;

    if (inside < 0.5 && nearMax < 0.5 && glowAcc < 0.001) {
        discard;
    }

    float aspect = px.y / px.x;
    vec2 flow = vec2(uv.x * aspect, uv.y) * mix(2.0, 5.0, clamp(scale / 3.0, 0.0, 1.0));
    float t = time * max(speed, 0.001);
    vec2 drift = vec2(t * 0.20, -t * 0.15);

    vec2 warp = vec2(
        fbm(flow + drift + vec2(0.0, 4.1)),
        fbm(flow - drift * 0.6 + vec2(3.7, 1.8))
    );
    vec2 q = flow + (warp - 0.5) * 1.6;

    float mist = fbm(q * 0.8 - drift * 0.3 + vec2(4.2, 8.1));
    float stripe = 1.0 - abs(sin((q.x * 1.1 + q.y * 0.5) * 1.8 + t * 0.9 + mist * 4.0));
    stripe = pow(clamp(stripe, 0.0, 1.0), 4.0);

    float energy = clamp(mist * 0.6 + stripe * 0.7, 0.0, 1.0);
    float core = smoothstep(0.15, 0.95, energy);

    vec3 base = Color.rgb;
    vec3 accent = Color2.rgb;
    vec3 tint = mix(base, accent, core);

    if (inside > 0.5) {
        float edgeA = clamp((1.0 - nearMin) * outlineK * 1.15, 0.0, 1.0);
        float fillA = fillK * (0.35 + 0.65 * core);
        float coverage = edgeA + fillA * (1.0 - edgeA);
        if (coverage * alphaK < 0.003) {
            discard;
        }
        vec3 edgeColor = mix(base, accent, 0.35 + 0.4 * core);
        vec3 rgb = (edgeColor * edgeA + tint * fillA * (1.0 - edgeA)) / max(coverage, 0.0001);
        OutColor = vec4(rgb, min(coverage * alphaK, 0.95));
        return;
    }

    float rim = nearMax * outlineK;
    float halo = clamp(pow(clamp(glowAcc, 0.0, 1.0), 0.6) * glowK * 0.8, 0.0, 1.0);
    float alpha = min(halo + rim * (1.0 - halo), 0.85) * alphaK;
    if (alpha < 0.003) {
        discard;
    }
    vec3 rimColor = mix(base, vec3(1.0), 0.18);
    OutColor = vec4(mix(tint, rimColor, rim), alpha);
}
