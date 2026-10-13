package stone.module.player.autodeflect;

import stone.module.Module;
import stone.module.Setting;

public final class AutoDeflectSettings {
    public final Setting fov;
    public final Setting range;
    public final Setting cps;
    public final Setting aimSpeed;
    public final Setting onGround;
    public final Setting sneak;

    public AutoDeflectSettings(Module var1) {
        this.fov = var1.addNumber("FOV", "", 360.0F, 30.0F, 360.0F, 0);
        this.range = var1.addNumber("Range", "", 8.0F, 3.0F, 15.0F, 1);
        this.cps = var1.addNumber("CPS", "", 12.0F, 1.0F, 20.0F, 1);
        this.aimSpeed = var1.addNumber("Aim speed", "", 15.0F, 1.0F, 30.0F, 0);
        this.onGround = var1.addBoolean("On ground", "", false);
        this.sneak = var1.addBoolean("Sneak", "", false);
    }
}
