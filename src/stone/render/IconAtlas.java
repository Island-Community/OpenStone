package stone.render;

import stone.core.Assets;
import stone.core.Log;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.charset.StandardCharsets;
import java.util.stream.IntStream;
import org.lwjgl.opengl.GL11;

public final class IconAtlas {
    private static final String SCOPE = "icons";
    private static final int TILE = 64;
    private static final int SUPERSAMPLE = 4;
    private static final int RASTER = TILE * SUPERSAMPLE;
    private static final float SPREAD = 6.0f;
    private static final int COLUMNS = 8;
    private static final float VIEWBOX = 24.0f;

    public record Tile(float u0, float v0, float u1, float v1) {}

    private final Tile[] tiles = new Tile[Icon.values().length];
    private byte[] pixels;
    private int atlasWidth;
    private int atlasHeight;
    private int texture;
    private boolean dirty;

    public void build() {
        long start = System.nanoTime();
        Icon[] icons = Icon.values();
        int rows = (icons.length + COLUMNS - 1) / COLUMNS;
        atlasWidth = COLUMNS * TILE;
        atlasHeight = rows * TILE;
        pixels = new byte[atlasWidth * atlasHeight];

        IntStream.range(0, icons.length).parallel().forEach(index -> {
            BufferedImage image = new BufferedImage(RASTER, RASTER, BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            float margin = RASTER * 0.10f;
            graphics.translate(margin, margin);
            graphics.scale((RASTER - margin * 2.0f) / VIEWBOX, (RASTER - margin * 2.0f) / VIEWBOX);
            graphics.setColor(Color.WHITE);
            graphics.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Svg.paint(new String(Assets.read("assets/icons/" + icons[index].file + ".svg"), StandardCharsets.UTF_8),
                    graphics);
            graphics.dispose();

            int originX = index % COLUMNS * TILE;
            int originY = index / COLUMNS * TILE;
            DistanceField.encode(((DataBufferByte) image.getRaster().getDataBuffer()).getData(), RASTER, RASTER,
                    SUPERSAMPLE, SPREAD, pixels, originY * atlasWidth + originX, atlasWidth, TILE, TILE);
            tiles[index] = new Tile((float) originX / atlasWidth, (float) originY / atlasHeight,
                    (float) (originX + TILE) / atlasWidth, (float) (originY + TILE) / atlasHeight);
        });
        dirty = true;
        Log.info(SCOPE, "%d icons in a %dx%d atlas (%.0f ms)", icons.length, atlasWidth, atlasHeight,
                (System.nanoTime() - start) / 1e6);
    }

    public void upload() {
        if (!dirty || pixels == null) {
            return;
        }
        texture = Gl.uploadField(texture, atlasWidth, atlasHeight, pixels);
        dirty = false;
        Gl.checkError("icon atlas upload");
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

    public Tile tile(Icon icon) {
        return tiles[icon.ordinal()];
    }
}
