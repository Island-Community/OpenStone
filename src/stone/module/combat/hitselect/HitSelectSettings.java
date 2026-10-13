package stone.module.combat.hitselect;

import stone.module.Module;
import stone.module.Setting;
import java.util.List;

public final class HitSelectSettings {
    public final Setting pauseDuration;
    public final Setting mode;
    public final Setting waitForFirstHit;
    public final Setting disableDuringKnockback;
    public final Setting onlyWhileDamaged;
    public final Setting useServerAttackTime;
    public final Setting fakeSwing;
    public final Setting inCombat;
    public final Setting missedSwings;

    public HitSelectSettings(Module var1) {
        this.pauseDuration = var1.addNumber("Pause duration", "", 500.0F, 0.0F, 500.0F, 0);
        this.mode = var1.addChoice("Mode", "", List.of(HitSelectMode.labels()), HitSelectMode.BURST.ordinal());
        this.waitForFirstHit = var1.addNumber("Wait for first hit", "", 0.0F, 0.0F, 500.0F, 0);
        this.disableDuringKnockback = var1.addBoolean("Disable during knockback", "", false);
        this.onlyWhileDamaged = var1.addBoolean("Only while damaged", "", false);
        this.useServerAttackTime = var1.addBoolean("Use server attack time", "", false);
        this.fakeSwing = var1.addBoolean("Fake swing", "", false);
        this.inCombat = var1.addNumber("In combat", "", 100.0F, 0.0F, 100.0F, 0);
        this.missedSwings = var1.addNumber("Missed swings", "", 0.0F, 0.0F, 100.0F, 0);
    }

}
