package stone.module.combat;

import stone.core.Maths;
import stone.core.Rng;
import stone.game.CrosshairTarget;
import stone.game.EntityView;
import stone.game.FrameState;
import stone.game.Game;
import stone.game.RotationEngine;
import stone.hook.Input;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.render.Icon;

public final class JumpReset extends Module {
    private static final float FOV_CONE = 165.0f;

    private final Setting chance = addNumber("Chance", "Percent of hits to reset on", 80, 0, 100, 0);
    private final Setting requireAttack = addBoolean("Require mouse down", "Only while attacking", false);
    private final Setting requireForward = addBoolean("Require moving forward", "Only while walking forward", true);
    private final Setting requireAim = addBoolean("Require aim", "Only while the crosshair is on a player", true);

    private final Rng random = new Rng(0x51ED2701);
    private int lastHurtTime;
    private double lastFallDistance;
    private boolean ignoreNext;
    private boolean holdingJump;

    private boolean jumpWasTheirs;

    public JumpReset() {
        super("JumpReset", "Jumps on the tick a hit lands so the knockback carries", Category.COMBAT,
                Subcategory.BASIC);
        setIcon(Icon.ACTIVITY);
    }

    @Override
    public String suffix() {
        int percent = (int) chance.number();
        return percent >= 100 ? "" : percent + "%";
    }

    @Override
    public void onEnable(Context context) {
        lastHurtTime = 0;
        lastFallDistance = 0.0;
        ignoreNext = false;
        holdingJump = false;
        jumpWasTheirs = false;
    }

    @Override
    public void onDisable(Context context) {
        if (holdingJump && !jumpWasTheirs) {
            Game.setBinding("jump", false);
        }
        holdingJump = false;
        jumpWasTheirs = false;
    }

    private static boolean badEffect(FrameState state) {
        for (Game.EffectView effect : Game.statusEffects(state.player.handle)) {
            switch (effect.id()) {
                case "jump_boost", "poison", "wither" -> {
                    return true;
                }
                default -> {
                }
            }
        }
        return false;
    }

    private static boolean aimingAtPlayer(FrameState state) {
        CrosshairTarget target = Game.crosshair();
        if (target.kind() != CrosshairTarget.Kind.ENTITY) {
            return false;
        }
        for (EntityView entity : state.entities) {
            if (entity.id == target.entityId()) {
                return entity.isPlayer;
            }
        }
        return false;
    }

    private static boolean pushedBackwards(FrameState state) {
        if (state.player.velocity.horizontalLength() < 0.02) {
            return true;
        }

        float motionYaw = (float) Math.toDegrees(Math.atan2(-state.player.velocity.x(), state.player.velocity.z()));
        return Math.abs(Maths.angleDifference(RotationEngine.sentYaw(), motionYaw)) <= FOV_CONE;
    }

    @Override
    public void onTick(Context context) {
        FrameState state = context.state;
        if (!state.inWorld || !state.player.valid || state.screenOpen) {
            onDisable(context);
            return;
        }

        if (holdingJump) {
            holdingJump = false;
            if (!jumpWasTheirs) {
                Game.setBinding("jump", false);
            }
        }

        int hurtTime = state.player.hurtTime;

        if (state.player.onGround && lastFallDistance > 3.0) {
            ignoreNext = true;
        }

        if (hurtTime > lastHurtTime) {
            boolean attacking = !requireAttack.bool() || Input.attackHeld();
            boolean forward = !requireForward.bool() || Game.bindingPressed("forward");
            boolean aimed = !requireAim.bool() || aimingAtPlayer(state);
            boolean rolled = chance.number() >= 100.0f || random.range(0.0f, 100.0f) < chance.number();
            if (!ignoreNext && state.player.onGround && attacking && forward && aimed && rolled
                    && pushedBackwards(state) && !badEffect(state)) {
                jumpWasTheirs = Game.bindingPressed("jump");
                Game.setBinding("jump", true);
                holdingJump = true;
            }
            ignoreNext = false;
        }

        lastHurtTime = hurtTime;
        lastFallDistance = state.player.fallDistance;
    }
}
