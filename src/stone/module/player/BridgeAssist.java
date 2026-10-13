package stone.module.player;

import stone.core.Maths;
import stone.core.Rng;
import stone.core.Vec3;
import stone.game.FrameState;
import stone.game.Game;
import stone.game.Raycast;
import stone.game.RotationEngine;
import stone.game.RotationTarget;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.render.Icon;
import java.util.List;
import java.util.Locale;
import net.minecraft.util.EnumFacing;

public final class BridgeAssist extends Module {

    private static final double HALF_WIDTH = 0.3;
    private static final int PRE_PLACE_PRIORITY = 40;

    private final Setting mode = addChoice("Mode", "Always assists, or only while you hold sneak yourself",
            List.of("Always", "Hold sneak"), 0);
    private final Setting edgeOffset = addNumber("Edge offset",
            "How far past the edge the feet may go before sneaking", 0, 0, 0.30f, 2);
    private final Setting unsneakDelay = addNumber("Unsneak delay",
            "Milliseconds the sneak is held after it is safe", 50, 50, 300, 0);
    private final Setting sneakOnJump = addNumber("Sneak on jump",
            "Milliseconds of sneak when a jump leaves the ground", 0, 0, 500, 0);
    private final Setting holdingBlocks = addBoolean("Holding blocks", "Only while a placeable block is in hand",
            false);
    private final Setting lookingDown = addBoolean("Looking down", "Only while looking down past seventy degrees",
            false);
    private final Setting notMovingForward = addBoolean("Not moving forward", "Stop assisting while walking forward",
            false);
    private final Setting prePlace = addBoolean("Pre place", "Aim at the side face of the block under you", false);
    private final Setting prePlaceSpeed = addNumber("Pre place speed", "Degrees the aim may travel per tick", 40, 5,
            180, 0).visibleWhen(prePlace::bool);

    private final Rng random = new Rng(0x2B7E1516);
    private boolean sneaking;

    private int releaseHold;
    private int jumpHold;
    private boolean wasJumping;

    private boolean overEdge;

    public BridgeAssist() {
        super("BridgeAssist", "Sneaks at the edge and aims at the face you are bridging off", Category.PLAYER,
                Subcategory.BASIC);
        setIcon(Icon.LAYERS);
    }

    @Override
    public String suffix() {
        return String.format(Locale.ROOT, "%.2f", edgeOffset.number());
    }

    @Override
    public void onEnable(Context context) {
        sneaking = false;
        releaseHold = 0;
        jumpHold = 0;
        wasJumping = false;
        overEdge = false;
    }

    @Override
    public void onDisable(Context context) {
        if (sneaking) {
            Game.setBinding("sneak", false);
            sneaking = false;
        }
        releaseHold = 0;
        jumpHold = 0;
    }

    private int ticksFor(float millis) {
        float ticks = millis / 50.0f;
        return (int) ticks + (random.unit() < ticks - (float) Math.floor(ticks) ? 1 : 0);
    }

    private void holdSneak() {
        if (!sneaking) {
            Game.setBinding("sneak", true);
            sneaking = true;
        }
    }

    private void releaseSneak(boolean immediately) {
        if (!sneaking) {
            return;
        }
        if (!immediately) {
            if (jumpHold > 0) {
                --jumpHold;
                return;
            }
            if (releaseHold > 0) {
                --releaseHold;
                return;
            }
        }

        Game.setBinding("sneak", false);
        sneaking = false;
        jumpHold = 0;
        releaseHold = 0;
    }

    private static double edgeDistance(FrameState state) {

        double feetX = state.player.pos.x() + state.player.velocity.x();
        double feetZ = state.player.pos.z() + state.player.velocity.z();
        int groundY = Maths.floor(state.player.pos.y() - 0.01);

        double nearest = -1.0;
        for (int x = Maths.floor(feetX - HALF_WIDTH); x <= Maths.floor(feetX + HALF_WIDTH); ++x) {
            for (int z = Maths.floor(feetZ - HALF_WIDTH); z <= Maths.floor(feetZ + HALF_WIDTH); ++z) {
                if (Raycast.blockPassable(x, groundY, z)) {
                    continue;
                }

                double closestX = Maths.clamp(feetX, x, x + 1);
                double closestZ = Maths.clamp(feetZ, z, z + 1);
                double distance = Math.max(Math.abs(feetX - closestX), Math.abs(feetZ - closestZ));
                if (nearest < 0.0 || distance < nearest) {
                    nearest = distance;
                }
            }
        }
        return nearest;
    }

    private static boolean wantsSneak() {
        return Game.bindingPressed("sneak") || Game.bindingPhysicallyHeld("sneak");
    }

    private boolean conditionsMet(FrameState state) {
        if (notMovingForward.bool() && Game.bindingPressed("forward")) {
            return false;
        }
        if (lookingDown.bool() && state.player.pitch < 70.0f) {
            return false;
        }
        if (holdingBlocks.bool() && !Game.holdingBlock()) {
            return false;
        }
        return mode.choice() != 1 || sneaking || wantsSneak();
    }

    private void aimPrePlace(FrameState state) {
        float yaw = RotationEngine.sentYaw();
        Vec3 pos = state.player.pos;
        int feetY = Maths.floor(pos.y()) - 1;
        int minX = Maths.floor(pos.x() - HALF_WIDTH);
        int maxX = Maths.floor(pos.x() + HALF_WIDTH);
        int minZ = Maths.floor(pos.z() - HALF_WIDTH);
        int maxZ = Maths.floor(pos.z() + HALF_WIDTH);

        float bestPitch = 0.0f;
        float bestDelta = Float.MAX_VALUE;
        boolean found = false;

        for (float pitch = 60.0f; pitch <= 90.0f;) {
            pitch += random.range(0.6f, 1.5f);
            float sample = Math.min(pitch, 90.0f);
            Raycast.BlockHit hit = Raycast.raycastBlocks(state.player.eyePos, Raycast.lookVector(yaw, sample), 4.5);
            if (hit == null || hit.face().getAxis() == EnumFacing.Axis.Y) {
                continue;
            }

            if (hit.y() != feetY || hit.x() < minX || hit.x() > maxX || hit.z() < minZ || hit.z() > maxZ) {
                continue;
            }
            if (!Raycast.blockPassable(hit.x() + hit.face().getFrontOffsetX(), feetY,
                    hit.z() + hit.face().getFrontOffsetZ())) {
                continue;
            }
            float delta = Math.abs(sample - state.player.pitch);
            if (delta < bestDelta) {
                bestDelta = delta;
                bestPitch = sample;
                found = true;
            }
        }
        if (!found) {
            return;
        }

        RotationTarget target = new RotationTarget(yaw, bestPitch);
        target.speed = prePlaceSpeed.number();
        target.smoothing = 0.35f;
        target.silent = false;
        target.priority = PRE_PLACE_PRIORITY;
        RotationEngine.submit(target);
    }

    @Override
    public void onTick(Context context) {
        FrameState state = context.state;
        if (!state.inWorld || !state.player.valid || state.screenOpen) {
            onDisable(context);
            return;
        }
        if (!conditionsMet(state)) {
            releaseSneak(true);
            wasJumping = state.player.jumping;
            return;
        }

        if (prePlace.bool()) {
            aimPrePlace(state);
        }

        boolean jumping = Game.bindingPressed("jump");
        if (jumping && !wasJumping && state.player.onGround && sneakOnJump.number() > 0.0f) {
            jumpHold = ticksFor(sneakOnJump.number());
            holdSneak();
            wasJumping = true;
            return;
        }
        wasJumping = jumping;

        double distance = edgeDistance(state);
        if (distance < 0.0) {

            if (state.player.onGround) {
                overEdge = true;
                holdSneak();
            } else {
                releaseSneak(false);
            }
            return;
        }

        if (distance > edgeOffset.number()) {
            overEdge = true;
            holdSneak();
            return;
        }
        if (overEdge) {
            overEdge = false;
            releaseHold = ticksFor(unsneakDelay.number() - 50.0f);
        }
        releaseSneak(false);
    }
}
