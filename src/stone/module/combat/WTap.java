package stone.module.combat;

import stone.core.Log;
import stone.core.Rng;
import stone.core.Time;
import stone.game.FrameState;
import stone.game.Game;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.ModuleManager;
import stone.module.Setting;
import stone.module.Subcategory;

public final class WTap extends Module {
    private final Setting delayMin = addNumber("Delay min", "Shortest W release in milliseconds", 230, 0, 1000, 0);
    private final Setting delayMax = addNumber("Delay max", "Longest W release in milliseconds", 270, 0, 1000, 0);
    private final Setting inAir = addBoolean("In air", "Also tap while airborne", false);

    private final Rng random = new Rng(0x7AC401);
    private int lastHits;
    private boolean tapping;
    private boolean logged;
    private long tapEnd;

    public WTap() {
        super("WTap", "W-taps every landed hit", Category.COMBAT, Subcategory.BASIC);
    }

    private void endTap() {
        if (Game.bindingPhysicallyHeld("forward")) {
            Game.setBinding("forward", true);
        }
        tapping = false;
    }

    @Override
    public void onEnable(Context context) {
        tapping = false;
        logged = false;
        lastHits = 0;
    }

    @Override
    public void onDisable(Context context) {
        if (tapping) {
            endTap();
        }
        TriggerBot trigger = ModuleManager.get(TriggerBot.class);
        if (trigger != null) {
            lastHits = trigger.hits();
        }
    }

    @Override
    public void onTick(Context context) {
        FrameState state = context.state;
        TriggerBot trigger = ModuleManager.get(TriggerBot.class);
        if (!state.inWorld || !state.player.valid || state.screenOpen || trigger == null) {
            if (tapping) {
                endTap();
            }
            return;
        }

        boolean holdingW = Game.bindingPhysicallyHeld("forward");
        if (tapping) {
            boolean jumped = Game.bindingPressed("jump");
            if (!holdingW || jumped && !inAir.bool() || Time.nowMillis() >= tapEnd) {
                endTap();
            }
            lastHits = trigger.hits();
            return;
        }

        boolean struck = trigger.hits() != lastHits;
        lastHits = trigger.hits();
        if (!struck || !holdingW || !state.player.sprinting || !inAir.bool() && !state.player.onGround) {
            return;
        }
        float span = Math.max(delayMax.number() - delayMin.number(), 0.0f);
        tapEnd = Time.nowMillis() + (long) (delayMin.number() + random.unit() * span);
        Game.setBinding("forward", false);
        tapping = true;
        if (!logged) {
            logged = true;
            Log.debug("wtap", "first tap %dms after strike", tapEnd - Time.nowMillis());
        }
    }
}
