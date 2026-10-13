package stone.util.font;

public interface ShadowTextRenderer extends TextRenderer {
    default int drawStringWithShadow(String var1, float var2, float var3, int var4) {
        return this.drawString(var1, var2, var3, var4, true);
    }
}
