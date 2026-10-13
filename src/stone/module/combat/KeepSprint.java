package stone.module.combat;

import stone.event.Phase;
import stone.event.PreUpdateEvent;
import stone.event.Subscribe;
import stone.event.TickEndEvent;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.module.combat.keepsprint.KeepSprintState;
import stone.module.combat.keepsprint.KeepSprintState$0;
import stone.module.movement.NoSlow;
import stone.util.ClientUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public class KeepSprint extends Module {
    private final KeepSprintState lF8 = new KeepSprintState();
    private final Setting slowdown;

    public KeepSprint() {
        super("KeepSprint", "Keeps sprinting through your own hits", Category.COMBAT, Subcategory.ADVANCED);
        this.slowdown = addNumber("Slowdown", "", 0.0F, 0.0F, 100.0F, 0);
    }

    @Override
    public void onEnable(Context context) {
        this.resetPredictionState();
    }

    @Override
    public void onDisable(Context context) {
        this.resetPredictionState();
    }

    @Override
    public String suffix() {
        return "Prediction";
    }

    public void applySprintSlowdown(EntityPlayer var1) {
        if (var1 != null && var1.isSprinting()) {
            if (this.lF8.isAwaitingRestore()) {
                double var2 = 1.0 - 0.4 * this.slowdown.number() / 100.0;
                var1.motionX *= var2;
                var1.motionZ *= var2;
                if ((int)this.slowdown.number() == 60) {
                    var1.setSprinting(false);
                }
            } else {
                var1.motionX *= 0.6;
                var1.motionZ *= 0.6;
                var1.setSprinting(false);
            }
        }
    }

    @Subscribe(priority = Subscribe.Priority.NORMAL)
    public void onTickEnd(TickEndEvent var1) {
        if (var1.phase == Phase.START) {
            this.lF8.clearTickFlag();
        }
    }

    public boolean shouldCancelAttack(EntityPlayer var1, Entity var2) {
        if (ClientUtils.isInWorld() && var1 == mc.thePlayer) {
            KeepSprintState$0 var3 = this.lF8.onAttack(var2 instanceof EntityPlayer, mc.thePlayer.isSprinting());
            if (var3 == KeepSprintState$0.CANCEL_ATTACK) {
                return true;
            } else {
                this.CSgX1(var3);
                return false;
            }
        } else {
            return false;
        }
    }

    @Subscribe(priority = Subscribe.Priority.LOW)
    public void onPreUpdate(PreUpdateEvent var1) {
        if (!ClientUtils.isInWorld()) {
            this.resetPredictionState();
        } else {
            this.CSgX1(this.lF8.advanceState());
        }
    }

    public void applyPredictedSprintState(EntityPlayer var1) {
        if (ClientUtils.isInWorld() && var1 == mc.thePlayer) {
            this.CSgX1(this.lF8.getCurrentSprintAction());
        }
    }

    private boolean Vvguv3() {
        return !mc.thePlayer.isUsingItem()
            ? true
            : mc.thePlayer.moveForward > 0.0F
                && !mc.thePlayer.isSneaking()
                && !mc.thePlayer.isCollidedHorizontally
                && mc.thePlayer.getFoodStats().getFoodLevel() > 6
                && NoSlow.isVanillaModeActive();
    }

    private void CSgX1(KeepSprintState$0 var1) {
        if (var1 == KeepSprintState$0.STOP_SPRINT) {
            mc.thePlayer.setSprinting(false);
        } else if (var1 == KeepSprintState$0.RESTORE_SPRINT && this.Vvguv3()) {
            mc.thePlayer.setSprinting(true);
        }
    }

    private void resetPredictionState() {
        this.lF8.YWGs();
    }
}
