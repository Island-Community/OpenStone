package stone.util.font;

public final class RasterizedGlyph {
    public final int texture;
    public final float width;
    public final float height;
    public final float rawAdvance;
    public final float advance;
    public final int visibleTop;
    public final int visibleBottom;

    public RasterizedGlyph(int var1, float var2, float var3, float var4, float var5, int var6, int var7) {
        this.texture = var1;
        this.width = var2;
        this.height = var3;
        this.rawAdvance = var4;
        this.advance = var5;
        this.visibleTop = var6;
        this.visibleBottom = var7;
    }

    public boolean visible() {
        return this.visibleBottom > this.visibleTop;
    }
}
