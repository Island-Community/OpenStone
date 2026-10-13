package stone.module.player.hidewindow;

import stone.core.Color;
import stone.module.Module;
import stone.module.Setting;

public final class HideWindowSettings {
    public final Setting iconColor;
    public final Setting iconScale;
    public final Setting onlyWhileCrouching;

    public HideWindowSettings(Module var1) {
        this.iconColor = var1.addColour("Icon color", "", Color.rgb(0xFFFFFF));
        this.iconScale = var1.addNumber("Icon scale", "", 1.0F, 0.5F, 3.0F, 1);
        this.iconScale.setVisible(false);
        this.onlyWhileCrouching = var1.addBoolean("Only while crouching", "", false);
    }
}
