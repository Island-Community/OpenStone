package stone.render;

import stone.core.Assets;
import stone.core.Log;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.lwjgl.opengl.GL11;

public final class FontAtlas {
    private static final String SCOPE = "font";
    private static final int PADDING = 6;
    private static final int SUPERSAMPLE = 4;

    private static final int FIRST_CODEPOINT = 32;
    private static final int LAST_CODEPOINT = 255;
    private static final int[] EXTRA = {0x2026, 0x2022, 0x2192, 0x25B8, 0x25BE, 0xFFFD};

    private record Request(FontRole role, Font font, int codepoint) {}

    private record Raster(FontRole role, int codepoint, byte[] field, int width, int height, int offsetX,
            int offsetY, float advance) {}

    private final Map<FontRole, FontFace> faces = new EnumMap<>(FontRole.class);
    private byte[] pixels;
    private int atlasWidth;
    private int atlasHeight;
    private int texture;
    private boolean dirty;

    public FontAtlas() {
        for (FontRole role : FontRole.values()) {
            faces.put(role, new FontFace());
        }
    }

    public void build(float atlasPixelHeight) {
        long start = System.nanoTime();
        FontRenderContext context = new FontRenderContext(null, true, true);
        List<Request> requests = new ArrayList<>();
        for (FontRole role : FontRole.values()) {
            Font base;
            try {
                base = Font.createFont(Font.TRUETYPE_FONT,
                        new ByteArrayInputStream(Assets.read("assets/fonts/" + role.file + ".ttf")));
            } catch (FontFormatException | IOException e) {
                throw new IllegalStateException("font " + role.file + " failed to load", e);
            }

            LineMetrics reference = base.deriveFont(100.0f).getLineMetrics("H", context);
            float emPixels = atlasPixelHeight * 100.0f / (reference.getAscent() + reference.getDescent());
            FontFace face = faces.get(role);
            face.atlasPixelHeight = emPixels;
            face.ascent = reference.getAscent() / 100.0f * emPixels;
            face.descent = -reference.getDescent() / 100.0f * emPixels;
            face.lineGap = reference.getLeading() / 100.0f * emPixels;

            Font font = base.deriveFont(emPixels);
            for (int codepoint = FIRST_CODEPOINT; codepoint <= LAST_CODEPOINT; ++codepoint) {
                requests.add(new Request(role, font, codepoint));
            }
            for (int codepoint : EXTRA) {
                requests.add(new Request(role, font, codepoint));
            }
        }

        List<Raster> rasters = requests.parallelStream()
                .filter(request -> request.font().canDisplay(request.codepoint()))
                .map(request -> rasterise(request, context))
                .sorted(Comparator.comparingInt(Raster::height).reversed())
                .toList();

        int atlasSize = 512;
        while (!fits(rasters, atlasSize)) {
            atlasSize *= 2;
            if (atlasSize > 4096) {
                throw new IllegalStateException("glyph set does not fit a 4096 atlas");
            }
        }
        atlasWidth = atlasSize;
        atlasHeight = atlasSize;
        pixels = new byte[atlasSize * atlasSize];

        float inverseSize = 1.0f / atlasSize;
        int penX = 1;
        int penY = 1;
        int shelfHeight = 0;
        for (Raster raster : rasters) {
            FontFace.Glyph glyph;
            if (raster.field() == null) {
                glyph = new FontFace.Glyph(0, 0, 0, 0, 0, 0, 0, 0, raster.advance());
            } else {
                if (penX + raster.width() + 1 > atlasSize) {
                    penX = 1;
                    penY += shelfHeight + 1;
                    shelfHeight = 0;
                }
                for (int row = 0; row < raster.height(); ++row) {
                    System.arraycopy(raster.field(), row * raster.width(), pixels, (penY + row) * atlasSize + penX,
                            raster.width());
                }
                glyph = new FontFace.Glyph(penX * inverseSize, penY * inverseSize,
                        (penX + raster.width()) * inverseSize, (penY + raster.height()) * inverseSize,
                        raster.offsetX(), raster.offsetY(), raster.width(), raster.height(), raster.advance());
                penX += raster.width() + 1;
                shelfHeight = Math.max(shelfHeight, raster.height());
            }
            FontFace face = faces.get(raster.role());
            if (raster.codepoint() == 'H' && glyph.height() > 0.0f) {

                face.capHeight = -(glyph.offsetY() + PADDING);
            }
            face.glyphs.put(raster.codepoint(), glyph);
        }
        dirty = true;
        Log.info(SCOPE, "%d glyphs across %d faces in a %dx%d atlas (%.0f ms)", rasters.size(),
                FontRole.values().length, atlasWidth, atlasHeight, (System.nanoTime() - start) / 1e6);
    }

    private static Raster rasterise(Request request, FontRenderContext context) {
        String text = new String(Character.toChars(request.codepoint()));
        GlyphVector vector = request.font().createGlyphVector(context, text);
        float advance = vector.getGlyphMetrics(0).getAdvanceX();
        Shape outline = vector.getGlyphOutline(0);
        Rectangle2D bounds = outline.getBounds2D();
        if (bounds.isEmpty()) {
            return new Raster(request.role(), request.codepoint(), null, 0, 0, 0, 0, advance);
        }
        int originX = (int) Math.floor(bounds.getMinX()) - PADDING;
        int originY = (int) Math.floor(bounds.getMinY()) - PADDING;
        int width = (int) Math.ceil(bounds.getMaxX()) + PADDING - originX;
        int height = (int) Math.ceil(bounds.getMaxY()) + PADDING - originY;

        BufferedImage image = new BufferedImage(width * SUPERSAMPLE, height * SUPERSAMPLE,
                BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        graphics.scale(SUPERSAMPLE, SUPERSAMPLE);
        graphics.translate(-originX, -originY);
        graphics.setColor(Color.WHITE);
        graphics.fill(outline);
        graphics.dispose();

        byte[] coverage = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        byte[] field = new byte[width * height];
        DistanceField.encode(coverage, width * SUPERSAMPLE, height * SUPERSAMPLE, SUPERSAMPLE, PADDING, field, 0,
                width, width, height);
        return new Raster(request.role(), request.codepoint(), field, width, height, originX, originY, advance);
    }

    private static boolean fits(List<Raster> rasters, int atlasSize) {
        int penX = 1;
        int penY = 1;
        int shelfHeight = 0;
        for (Raster raster : rasters) {
            if (raster.field() == null) {
                continue;
            }
            if (penX + raster.width() + 1 > atlasSize) {
                penX = 1;
                penY += shelfHeight + 1;
                shelfHeight = 0;
            }
            if (penY + raster.height() + 1 > atlasSize) {
                return false;
            }
            penX += raster.width() + 1;
            shelfHeight = Math.max(shelfHeight, raster.height());
        }
        return true;
    }

    public void upload() {
        if (!dirty || pixels == null) {
            return;
        }
        texture = Gl.uploadField(texture, atlasWidth, atlasHeight, pixels);
        dirty = false;
        Gl.checkError("font atlas upload");
    }

    public void destroy() {
        if (texture != 0) {
            GL11.glDeleteTextures(texture);
            texture = 0;
        }
        dirty = pixels != null;
    }

    public boolean ready() {
        return texture != 0;
    }

    public int texture() {
        return texture;
    }

    public float texelWidth() {
        return atlasWidth > 0 ? 1.0f / atlasWidth : 0.0f;
    }

    public float texelHeight() {
        return atlasHeight > 0 ? 1.0f / atlasHeight : 0.0f;
    }

    public FontFace face(FontRole role) {
        return faces.get(role);
    }
}
