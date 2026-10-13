package stone.module.player.ghosthand;

import stone.module.Module;
import stone.module.Setting;

public final class GhostHandSettings {
    public final Setting nonPlayerEntities;
    public final Setting bots;
    public final Setting friendlies;
    public final Setting enemies;
    public final Setting everything;
    public final Setting bed;
    public final Setting nextToBed;
    public final Setting sword;
    public final Setting tool;
    public final Setting fists;
    public final Setting bucket;
    public final Setting flintAndSteel;
    public final Setting cobweb;
    public final Setting other;
    public final Setting requireLeftMouse;
    public final Setting requireRightMouse;
    public final Setting notHoldingASword;

    public GhostHandSettings(Module var1) {
        this.nonPlayerEntities = var1.addBoolean("Non-player entities", "", true);
        this.bots = var1.addBoolean("Bots", "", false);
        this.friendlies = var1.addBoolean("Friendlies", "", false);
        this.enemies = var1.addBoolean("Enemies", "", true);
        this.everything = var1.addBoolean("Everything", "", true);
        this.bed = var1.addBoolean("Bed", "", false);
        this.nextToBed = var1.addBoolean("Next to bed", "", false);
        this.sword = var1.addBoolean("Sword", "", false);
        this.tool = var1.addBoolean("Tool", "", true);
        this.fists = var1.addBoolean("Fists", "", true);
        this.bucket = var1.addBoolean("Bucket", "", true);
        this.flintAndSteel = var1.addBoolean("Flint and steel", "", true);
        this.cobweb = var1.addBoolean("Cobweb", "", true);
        this.other = var1.addBoolean("Other", "", true);
        this.requireLeftMouse = var1.addBoolean("Require Left mouse", "", false);
        this.requireRightMouse = var1.addBoolean("Require right mouse", "", false);
        this.notHoldingASword = var1.addBoolean("Not holding a sword", "", false);
    }

}
