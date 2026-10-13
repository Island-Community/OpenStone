package stone.module.combat;

import stone.core.Log;
import stone.core.Rng;
import stone.core.Time;
import stone.core.Timer;
import stone.game.CombatTarget;
import stone.game.CrosshairTarget;
import stone.game.EntityView;
import stone.game.FrameState;
import stone.game.Game;
import stone.game.ItemKind;
import stone.hook.Input;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.render.Icon;
import java.util.List;

public final class TriggerBot extends Module {
    private final Setting mode = addChoice("Mode",
            "Smart only swings falling crits and settled hits, Any swings at whatever is aimed at once the rest is met",
            List.of("Smart", "Any"), 0);
    private final Setting clickMode = addChoice("Click",
            "Vanilla queues a press the game acts on itself; Direct sends the attack from here like Argon does",
            List.of("Vanilla", "Direct"), 0);
    private final Setting allItems = addBoolean("All Items", "Fire with anything held, on the sword delay", false);
    private final Setting swordMin = addNumber("Sword Delay Min", "Fastest swing with a sword, in ms", 540, 0, 1000, 0);
    private final Setting swordMax = addNumber("Sword Delay Max", "Slowest swing with a sword, in ms", 550, 0, 1000, 0);
    private final Setting axeMin = addNumber("Axe Delay Min", "Fastest swing with an axe, in ms", 780, 0, 1000, 0);
    private final Setting axeMax = addNumber("Axe Delay Max", "Slowest swing with an axe, in ms", 800, 0, 1000, 0);
    private final Setting inScreen = addBoolean("Work In Screen", "Trigger even while a screen is open", false);
    private final Setting whileUse = addBoolean("While Use", "Hit even while eating or blocking", false);
    private final Setting onLeftClick = addBoolean("On Left Click", "Only trigger while holding left click", false);
    private final Setting checkBlock = addBoolean("Check Block", "Skip targets holding their sword up", false);
    private final Setting whileAscend = addBoolean("While Ascending", "Allow hits while rising from a jump", false);
    private final Setting strayBypass = addBoolean("Stray Bypass",
            "Also hit zombies, bypassing Stray's anti-triggerbot", false);
    private final Setting allEntities = addBoolean("All Entities", "Hit whatever the crosshair lands on", false);
    private final Setting sticky = addBoolean("Same Player", "Only hit the current attack target, good for FFA", false);
    private final Setting sprintReset = addBoolean("Sprint reset", "Drop sprint for a tick before hitting", true);
    private final Setting swing = addBoolean("Swing", "Play the hand animation on Direct hits", true);

    private final Timer delayTimer = new Timer();
    private final Rng rng = new Rng();
    private int currentSwordDelay;
    private int currentAxeDelay;

    private int pendingId = -1;
    private boolean pendingSprintReset;
    private int groundTicks;
    private int targetId = -1;
    private int hits;

    public TriggerBot() {
        super("TriggerBot", "Attacks whatever the crosshair is already on", Category.COMBAT, Subcategory.ADVANCED);
        setIcon(Icon.CROSSHAIR);
    }

    public int hits() {
        return hits;
    }

    @Override
    public String suffix() {
        return targetId < 0 ? "" : mode.choiceName();
    }

    @Override
    public void onEnable(Context context) {
        currentSwordDelay = rollDelay(swordMin.number(), swordMax.number());
        currentAxeDelay = rollDelay(axeMin.number(), axeMax.number());

        delayTimer.reset();
        delayTimer.elapse(Math.max(currentSwordDelay, currentAxeDelay));
        pendingId = -1;
        pendingSprintReset = false;
        groundTicks = 0;
        targetId = -1;
        hits = 0;
    }

    @Override
    public void onDisable(Context context) {
        if (pendingSprintReset && context.state.player.valid) {
            Game.setSprinting(true);
        }
        pendingId = -1;
        pendingSprintReset = false;
        targetId = -1;
    }

    private int rollDelay(float low, float high) {
        int lo = (int) low;
        int hi = (int) high;
        return hi <= lo ? lo : rng.range(lo, hi);
    }

    private boolean axeInHand() {
        return !allItems.bool() && !Game.holdingSword() && Game.holdingItem(ItemKind.AXE);
    }

    private int delayFor(boolean axe) {
        if (allItems.bool() || Game.holdingSword()) {
            return currentSwordDelay;
        }
        return axe ? currentAxeDelay : -1;
    }

    private boolean entityAllowed(FrameState state, CrosshairTarget target) {
        EntityView view = null;
        for (EntityView entity : state.entities) {
            if (entity.id == target.entityId()) {
                view = entity;
                break;
            }
        }

        boolean allowed = view != null && view.isPlayer
                || strayBypass.bool() && target.entity() != null && Game.isZombie(target.entity())
                || allEntities.bool();

        return allowed && !(checkBlock.bool() && view != null && view.blocking);
    }

    private boolean stanceAllows(FrameState state) {
        boolean falling = state.player.velocity.y() < 0.0 && !state.player.onGround;
        if (!falling && state.player.velocity.y() > 0.0) {
            return false;
        }
        if (mode.choice() == 1) {
            return true;
        }
        boolean critFall = falling && state.player.fallDistance > 0.05;
        return critFall || state.player.onGround && groundTicks >= 2;
    }

    private void strike(CrosshairTarget target, boolean axe) {
        if (clickMode.choice() == 0) {
            Game.queueClick(false);
        } else {
            Game.attackEntity(target.entity());
            if (swing.bool()) {
                Game.swingHand();
            }
            Game.setAttackCooldown(0);
        }
        if (axe) {
            currentAxeDelay = rollDelay(axeMin.number(), axeMax.number());
        } else {
            currentSwordDelay = rollDelay(swordMin.number(), swordMax.number());
        }
        delayTimer.reset();
        ++hits;
    }

    private void restoreSprint() {
        if (pendingSprintReset) {
            Game.setSprinting(true);
            pendingSprintReset = false;
        }
    }

    @Override
    public void onTick(Context context) {
        FrameState state = context.state;
        if (!state.inWorld || !state.player.valid || !inScreen.bool() && state.screenOpen) {
            restoreSprint();
            pendingId = -1;
            targetId = -1;
            return;
        }

        groundTicks = state.player.onGround ? groundTicks + 1 : 0;

        if (onLeftClick.bool() && !Input.attackHeld()) {
            targetId = -1;
            return;
        }
        if (state.player.usingItem && Input.useHeld() && !whileUse.bool()) {
            targetId = -1;
            return;
        }

        if (!whileAscend.bool() && !state.player.onGround
                && (state.player.velocity.y() > 0.0 || state.player.fallDistance <= 0.0)) {
            targetId = -1;
            return;
        }

        if (pendingId >= 0) {
            int wanted = pendingId;
            pendingId = -1;
            CrosshairTarget repicked = Game.crosshair();
            boolean aimed = repicked.kind() == CrosshairTarget.Kind.ENTITY && repicked.entity() != null;
            targetId = aimed ? repicked.entityId() : -1;
            if (aimed) {
                CombatTarget.publish(targetId, Time.nowSeconds());
            }
            boolean vanilla = clickMode.choice() == 0;
            if (aimed && (vanilla || repicked.entityId() == wanted) && entityAllowed(state, repicked)
                    && stanceAllows(state)) {
                boolean axe = axeInHand();
                int delay = delayFor(axe);
                if (delay >= 0 && delayTimer.elapsed(delay)) {
                    strike(repicked, axe);
                }
            }
            restoreSprint();
            return;
        }

        CrosshairTarget target = Game.crosshair();
        if (target.kind() != CrosshairTarget.Kind.ENTITY || target.entity() == null) {
            targetId = -1;
            return;
        }
        targetId = target.entityId();
        CombatTarget.publish(targetId, Time.nowSeconds());

        if (!entityAllowed(state, target)) {
            return;
        }
        if (sticky.bool()) {
            int current = Game.attackTargetId();
            if (current >= 0 && current != target.entityId()) {
                return;
            }
        }
        boolean axe = axeInHand();
        int delay = delayFor(axe);
        if (delay < 0 || !stanceAllows(state) || !delayTimer.elapsed(delay)) {
            return;
        }

        if (sprintReset.bool() && state.player.sprinting && state.player.onGround) {
            Game.setSprinting(false);
            pendingSprintReset = true;
            pendingId = target.entityId();
            return;
        }

        strike(target, axe);
        if (hits == 1) {
            Log.debug("trigger", "first hit on entity %d", target.entityId());
        }
    }
}
