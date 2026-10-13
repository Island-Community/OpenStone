package stone.util.render;

import stone.module.render.shared.ShaderProgram;

public final class RoundedRect {
    public static ShaderProgram ABgU = new ShaderProgram("roundedRect");
    private static final ShaderProgram CMqQ = new ShaderProgram("roundedRectRise");

    private RoundedRect() {
    }

    public static void drawRoundedRectArgb(float var0, float var1, float var2, float var3, float var4, int var5) {
        RoundedShader.Fzsi8(ABgU, var0, var1, var2, var3, var4, false, var5);
    }

    public static void drawRoundedRectWithCornerFlags(float var0, float var1, float var2, float var3, float var4, int var5, boolean var6, boolean var7, boolean var8, boolean var9) {
        ZFcV(var0, var1, var2, var3, var4, var5, var6, var7, var8, var9, false, false);
    }

    public static void ZFcV(
        float var0,
        float var1,
        float var2,
        float var3,
        float var4,
        int var5,
        boolean var6,
        boolean var7,
        boolean var8,
        boolean var9,
        boolean var10,
        boolean var11
    ) {
        RoundedShader.drawRoundedRectWithCornerFlags(CMqQ, var0, var1, var2, var3, var4, var5, var6, var7, var8, var9, var10, var11);
    }
}
