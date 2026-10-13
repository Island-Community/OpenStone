package stone.render;

final class Shaders {
    private Shaders() {}

    static final String UI_VERTEX = """
            #version 330 compatibility
            layout(location = 0) in vec2 aPos;
            layout(location = 1) in vec2 aUv;
            layout(location = 2) in vec4 aColor;
            layout(location = 3) in vec4 aRect;
            layout(location = 4) in vec4 aStyle;

            uniform mat4 uProjection;

            out vec2 vPos;
            out vec2 vUv;
            out vec4 vColor;
            out vec4 vRect;
            out vec4 vStyle;

            void main() {
                vPos = aPos;
                vUv = aUv;
                vColor = aColor;
                vRect = aRect;
                vStyle = aStyle;
                gl_Position = uProjection * vec4(aPos, 0.0, 1.0);
            }
            """;

    static final String UI_FRAGMENT = """
            #version 330 compatibility
            in vec2 vPos;
            in vec2 vUv;
            in vec4 vColor;
            in vec4 vRect;
            in vec4 vStyle;

            uniform sampler2D uFontAtlas;
            uniform sampler2D uIconAtlas;
            uniform sampler2D uImage;
            uniform float uTime;
            uniform vec2 uFontAtlasTexel;
            uniform vec2 uIconAtlasTexel;

            out vec4 fragColor;

            const int MODE_FILL = 0;
            const int MODE_STROKE = 1;
            const int MODE_TEXT = 2;
            const int MODE_ICON = 3;
            const int MODE_IMAGE = 4;
            const int MODE_SHADOW = 5;
            const int MODE_GLOW = 6;
            const int MODE_RING = 7;
            const int MODE_SWEEP = 8;
            const int MODE_LINE = 9;
            const int MODE_TEXT_BLOOM = 10;

            float sdRoundBox(vec2 p, vec2 halfSize, float radius) {
                vec2 q = abs(p) - halfSize + radius;
                return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius;
            }

            float dither(vec2 p) {
                return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453) - 0.5;
            }

            float coverage(float distance) {
                return clamp(0.5 - distance / max(fwidth(distance), 1e-5), 0.0, 1.0);
            }

            float sdfAlpha(sampler2D field, vec2 uv, vec2 texel, float alpha) {
                float centre = texture(field, uv).r;
                float fx = texture(field, uv + vec2(texel.x * 0.5, 0.0)).r -
                           texture(field, uv - vec2(texel.x * 0.5, 0.0)).r;
                float fy = texture(field, uv + vec2(0.0, texel.y * 0.5)).r -
                           texture(field, uv - vec2(0.0, texel.y * 0.5)).r;
                float width = max(fwidth(centre), max(abs(fx), abs(fy)));
                return alpha * clamp((centre - 0.5) / max(width, 1e-4) + 0.5, 0.0, 1.0);
            }

            void main() {
                int mode = int(vStyle.z + 0.5);
                vec2 local = vPos - vRect.xy;
                vec2 halfSize = vRect.zw;
                float radius = min(vStyle.x, min(halfSize.x, halfSize.y));
                vec4 color = vColor;

                if (mode == MODE_TEXT) {
                    fragColor = vec4(color.rgb, sdfAlpha(uFontAtlas, vUv, uFontAtlasTexel, color.a));
                    return;
                }

                if (mode == MODE_TEXT_BLOOM) {
                    float field = texture(uFontAtlas, vUv).r;
                    float soft = max(vStyle.w, 0.01);
                    float halo = exp(-max((0.5 - field) / soft, 0.0) * 4.0);
                    fragColor = vec4(color.rgb, color.a * halo);
                    return;
                }

                if (mode == MODE_ICON) {
                    fragColor = vec4(color.rgb, sdfAlpha(uIconAtlas, vUv, uIconAtlasTexel, color.a));
                    return;
                }

                if (mode == MODE_IMAGE) {
                    vec4 sampled = texture(uImage, vUv);
                    float alpha = coverage(sdRoundBox(local, halfSize, radius));
                    fragColor = vec4(sampled.rgb * color.rgb, sampled.a * color.a * alpha);
                    fragColor.rgb += dither(vPos) * (1.0 / 255.0);
                    return;
                }

                if (mode == MODE_SHADOW) {
                    float distance = sdRoundBox(local, halfSize, radius);
                    float spread = max(vStyle.y, 0.001);
                    float alpha = exp(-max(distance, 0.0) * (2.2 / spread));
                    alpha *= step(-0.5, distance) * 0.85 + 0.15;
                    fragColor = vec4(color.rgb, color.a * alpha);
                    return;
                }

                if (mode == MODE_GLOW) {
                    float distance = sdRoundBox(local, halfSize, radius);
                    float spread = max(vStyle.y, 0.001);
                    float halo = exp(-max(distance, 0.0) * (3.0 / spread));
                    float core = 1.0 - smoothstep(-1.5, 1.5, distance);
                    float alpha = max(halo * 0.85, core);
                    fragColor = vec4(color.rgb, color.a * alpha);
                    return;
                }

                if (mode == MODE_RING) {
                    float distance = abs(length(local) - (min(halfSize.x, halfSize.y) - vStyle.w * 0.5)) -
                                     vStyle.w * 0.5;
                    float angle = atan(local.x, -local.y) / 6.2831853 + 0.5;
                    float progress = clamp(vStyle.y, 0.0, 1.0);
                    float within = step(angle, progress);
                    fragColor = vec4(color.rgb, color.a * coverage(distance) * within);
                    return;
                }

                if (mode == MODE_LINE) {
                    float across = abs(vUv.y);
                    fragColor = vec4(color.rgb, color.a * coverage(across - 1.0));
                    return;
                }

                if (mode == MODE_SWEEP) {
                    float distance = abs(sdRoundBox(local, halfSize, radius)) - vStyle.w * 0.5;
                    float angle = atan(local.y, local.x) / 6.2831853 + 0.5;
                    float wave = 0.55 + 0.45 * sin((angle * 2.0 - uTime * 0.35) * 6.2831853);
                    fragColor = vec4(color.rgb, color.a * coverage(distance) * wave);
                    return;
                }

                float distance = sdRoundBox(local, halfSize, radius);
                if (mode == MODE_STROKE) {
                    distance = abs(distance) - vStyle.w * 0.5;
                }
                float alpha = coverage(distance);
                fragColor = vec4(color.rgb + dither(vPos) * (1.0 / 255.0), color.a * alpha);
            }
            """;

    static final String FULLSCREEN_VERTEX = """
            #version 330 compatibility
            out vec2 vUv;
            void main() {
                vec2 corner = vec2(float((gl_VertexID << 1) & 2), float(gl_VertexID & 2));
                vUv = corner;
                gl_Position = vec4(corner * 2.0 - 1.0, 0.0, 1.0);
            }
            """;

    static final String KAWASE_DOWN_FRAGMENT = """
            #version 330 compatibility
            in vec2 vUv;
            uniform sampler2D uSource;
            uniform vec2 uHalfPixel;
            uniform float uOffset;
            out vec4 fragColor;

            void main() {
                vec4 sum = texture(uSource, vUv) * 4.0;
                sum += texture(uSource, vUv - uHalfPixel * uOffset);
                sum += texture(uSource, vUv + uHalfPixel * uOffset);
                sum += texture(uSource, vUv + vec2(uHalfPixel.x, -uHalfPixel.y) * uOffset);
                sum += texture(uSource, vUv - vec2(uHalfPixel.x, -uHalfPixel.y) * uOffset);
                fragColor = sum / 8.0;
            }
            """;

    static final String KAWASE_UP_FRAGMENT = """
            #version 330 compatibility
            in vec2 vUv;
            uniform sampler2D uSource;
            uniform vec2 uHalfPixel;
            uniform float uOffset;
            out vec4 fragColor;

            void main() {
                vec4 sum = texture(uSource, vUv + vec2(-uHalfPixel.x * 2.0, 0.0) * uOffset);
                sum += texture(uSource, vUv + vec2(-uHalfPixel.x, uHalfPixel.y) * uOffset) * 2.0;
                sum += texture(uSource, vUv + vec2(0.0, uHalfPixel.y * 2.0) * uOffset);
                sum += texture(uSource, vUv + vec2(uHalfPixel.x, uHalfPixel.y) * uOffset) * 2.0;
                sum += texture(uSource, vUv + vec2(uHalfPixel.x * 2.0, 0.0) * uOffset);
                sum += texture(uSource, vUv + vec2(uHalfPixel.x, -uHalfPixel.y) * uOffset) * 2.0;
                sum += texture(uSource, vUv + vec2(0.0, -uHalfPixel.y * 2.0) * uOffset);
                sum += texture(uSource, vUv + vec2(-uHalfPixel.x, -uHalfPixel.y) * uOffset) * 2.0;
                fragColor = sum / 12.0;
            }
            """;

    static final String BACKDROP_FRAGMENT = """
            #version 330 compatibility
            in vec2 vUv;
            uniform sampler2D uSource;
            uniform vec4 uTint;
            uniform float uSaturation;
            out vec4 fragColor;

            void main() {
                vec3 color = texture(uSource, vUv).rgb;
                float luma = dot(color, vec3(0.2126, 0.7152, 0.0722));
                color = mix(vec3(luma), color, uSaturation);
                color = mix(color, uTint.rgb, uTint.a);
                fragColor = vec4(color, 1.0);
            }
            """;
}
