package stone.util.font;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

public final class FontScale {
    private FontScale() {
    }

    public static float resolve() {
        int var0 = 1;

        try {
            var0 = Math.max(1, new ScaledResolution(Minecraft.getMinecraft()).getScaleFactor());
        } catch (Exception var2) {
        }

        return Math.max(2.0F, var0 * 2.0F);
    }
}
