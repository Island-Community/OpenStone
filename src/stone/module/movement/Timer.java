package stone.module.movement;

import stone.event.GuiOpenEvent;
import stone.event.Phase;
import stone.event.RenderTickEvent;
import stone.event.Subscribe;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.util.ClientUtils;

public class Timer extends Module {
    private final Setting speedSetting;
    private final Setting hypixelTimerSpeed;
    private final Setting hypixelSwitchTime;
    private final Setting hypixel;
    private long switchCycleStart = -1L;
    private int berb;

    public Timer() {
        super("Timer", "Speeds up or slows down the game tick", Category.MOVEMENT, Subcategory.EXTREME);
        this.speedSetting = addNumber("Speed", "", 1.0F, 0.0F, 10.0F, 2);
        this.hypixel = addBoolean("Hypixel", "", false);
        this.hypixelTimerSpeed = addNumber("Hypixel Timer Speed", "", 0.013F, 0.01F, 0.035F, 3);
        this.hypixelSwitchTime = addNumber("Hypixel Switch Time", "", 3800.0F, 500.0F, 10000.0F, 0);
        this.hypixelTimerSpeed.setVisible(false);
        this.hypixelSwitchTime.setVisible(false);
    }

    @Override
    public void guiUpdate() {
        this.hypixelTimerSpeed.setVisible(this.hypixel.bool());
        this.hypixelSwitchTime.setVisible(this.hypixel.bool());
    }

    @Override
    public String suffix() {
        return this.formatSpeed(this.speedSetting.number()) + "x";
    }

    @Override
    public void onEnable(Context context) {
        this.migrateLegacyHypixelDefaults();
        this.resetSwitchCycle();
        if (mc.currentScreen != null) {
            mc.displayGuiScreen(null);
        }

        this.applyTimerSpeed();
    }

    @Override
    public void onDisable(Context context) {
        this.resetSwitchCycle();
        ClientUtils.eqyXnoq();
    }

    @Subscribe
    public void onRenderTick(RenderTickEvent var1) {
        if (var1.phase == Phase.START) {
            this.SsDx0();
            this.applyTimerSpeed();
        }
    }

    @Subscribe(priority = Subscribe.Priority.HIGHEST)
    public void onGuiOpen(GuiOpenEvent var1) {
        if (var1.guiScreen != null) {
            var1.setCancelled(true);
        }
    }

    @Override
    public void onUpdate() {
        this.applyTimerSpeed();
    }

    private void SsDx0() {
        if (!this.GxFknY()) {
            this.resetSwitchCycle();
        } else {
            long var1 = System.currentTimeMillis();
            if (this.switchCycleStart < 0L) {
                this.switchCycleStart = var1;
            }

            long var3 = var1 - this.switchCycleStart;
            long var5 = (long)this.hypixelSwitchTime.number();
            if (var3 >= var5 + 100L) {
                this.berb = 2;
            } else if (var3 >= var5) {
                this.berb = 1;
            }
        }
    }

    private void applyTimerSpeed() {
        float var1 = this.speedSetting.number();
        if (this.GxFknY()) {
            if (this.berb == 1) {
                var1 = 1.0F;
            } else if (this.berb == 2) {
                var1 = (float)this.hypixelTimerSpeed.number();
            }
        }

        mc.timer.timerSpeed = var1;
    }

    private boolean GxFknY() {
        return this.hypixel.bool() && this.speedSetting.number() <= 0.01F;
    }

    private void resetSwitchCycle() {
        this.switchCycleStart = -1L;
        this.berb = 0;
    }

    private void migrateLegacyHypixelDefaults() {
        if (Math.abs(this.hypixelTimerSpeed.number() - 0.015) < 1.0E-4 && Math.abs(this.hypixelSwitchTime.number() - 3400.0) < 0.1) {
            this.hypixelTimerSpeed.setNumber((float) (0.013));
            this.hypixelSwitchTime.setNumber((float) (3800.0));
        }
    }

    public boolean DZshJt() {
        return this.enabled();
    }

    private String formatSpeed(float var1) {
        return Math.abs(var1 - Math.round(var1)) < 1.0E-4F
            ? String.valueOf(Math.round(var1))
            : String.format("%.2f", var1).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
