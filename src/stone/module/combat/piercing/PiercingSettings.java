package stone.module.combat.piercing;

import stone.module.Module;
import stone.module.Setting;
import java.util.List;

public final class PiercingSettings {
    public static final String[] SORT_MODE_LABELS = new String[]{"Hurt time", "Health"};
    public final Setting sortMode;
    public final Setting ignoreBlocks;
    public final Setting ignoreTeammates;
    public final Setting ignoreNonPlayers;
    public final Setting weaponOnly;
    public final Setting insideHitboxOnly;

    public PiercingSettings(Module var1) {
        this.sortMode = var1.addChoice("Sort mode", "", List.of(SORT_MODE_LABELS), 0);
        this.ignoreBlocks = var1.addBoolean("Ignore blocks", "", false);
        this.ignoreNonPlayers = var1.addBoolean("Ignore non-players", "", true);
        this.ignoreTeammates = var1.addBoolean("Ignore teammates", "", true);
        this.weaponOnly = var1.addBoolean("Weapon only", "", false);
        this.insideHitboxOnly = var1.addBoolean("Inside hitbox only", "", false);
    }

}
