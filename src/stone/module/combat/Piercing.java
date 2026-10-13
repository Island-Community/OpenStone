package stone.module.combat;

import stone.module.Category;
import stone.module.Module;
import stone.module.Subcategory;
import stone.module.combat.piercing.PiercingCandidateFilter;
import stone.module.combat.piercing.PiercingSettings;
import stone.module.combat.piercing.ReachRaytrace;
import stone.module.combat.piercing.ReachRaytrace$2;
import stone.util.ClientUtils;
import stone.util.raytrace.PointedObjectOverrider;

public class Piercing extends Module implements PointedObjectOverrider {
    private final PiercingSettings piercingSettings = new PiercingSettings(this);

    public Piercing() {
        super("Piercing", "Hits the target through blocks and other entities in reach", Category.COMBAT,
                Subcategory.EXTREME);
    }

    @Override
    public String suffix() {
        return PiercingSettings.SORT_MODE_LABELS[(int)this.piercingSettings.sortMode.choice()];
    }

    @Override
    public boolean shouldOverridePointedObject() {
        boolean var1 = mc != null && mc.thePlayer != null && mc.theWorld != null;
        return PiercingCandidateFilter.arePiercingConditionsMet(
            this.enabled(), var1, this.piercingSettings.weaponOnly.bool(), var1 && ClientUtils.isHoldingWeapon(), this.piercingSettings.ignoreBlocks.bool(), mc == null ? null : mc.objectMouseOver
        );
    }

    @Override
    public void applyPointedObjectOverride(float var1) {
        if (this.shouldOverridePointedObject()) {
            this.performReachRaytrace(var1);
        }
    }

    private void performReachRaytrace(float var1) {
        ReachRaytrace$2 var2 = ReachRaytrace.YUam(mc, var1, (int)this.piercingSettings.sortMode.choice(), this.piercingSettings.ignoreNonPlayers.bool(), this.piercingSettings.ignoreTeammates.bool(), this.piercingSettings.insideHitboxOnly.bool());
        ReachRaytrace.applyRaytraceResult(mc, var2);
    }

}
