package stone.render;

import java.util.HashMap;
import java.util.Map;

public final class FontFace {

    public record Glyph(float u0, float v0, float u1, float v1, float offsetX, float offsetY, float width,
            float height, float advance) {}

    final Map<Integer, Glyph> glyphs = new HashMap<>();
    float atlasPixelHeight = 48.0f;
    float ascent;
    float capHeight;
    float descent;
    float lineGap;

    public Glyph glyph(int codepoint) {
        Glyph found = glyphs.get(codepoint);
        return found != null ? found : glyphs.get(0xFFFD);
    }

    public float scaleFor(float pixelSize) {
        return pixelSize / atlasPixelHeight;
    }

    public float capTop(float pixelSize) {
        return (ascent - capHeight) * scaleFor(pixelSize);
    }

    public float lineHeight(float pixelSize) {
        return (ascent - descent + lineGap) * scaleFor(pixelSize);
    }

    public float boxHeight(float pixelSize) {
        return (ascent - descent) * scaleFor(pixelSize);
    }

    public float baseline(float pixelSize) {
        return ascent * scaleFor(pixelSize);
    }

    public float measure(String text, float pixelSize) {
        float width = 0.0f;
        for (int index = 0; index < text.length(); ) {
            int codepoint = text.codePointAt(index);
            index += Character.charCount(codepoint);
            Glyph found = glyph(codepoint);
            if (found != null) {
                width += found.advance();
            }
        }
        return width * scaleFor(pixelSize);
    }

    public boolean ready() {
        return !glyphs.isEmpty();
    }
}
