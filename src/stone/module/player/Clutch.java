package stone.module.player;

import stone.core.Maths;
import stone.core.Rng;
import stone.core.Vec3;
import stone.game.FrameState;
import stone.game.Game;
import stone.game.Raycast;
import stone.game.RotationEngine;
import stone.game.RotationTarget;
import stone.hook.Input;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.render.Icon;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.util.EnumFacing;

public final class Clutch extends Module {
    private static final double HALF_WIDTH = 0.3;
    private static final double HEIGHT = 1.8;
    private static final int CLUTCH_PRIORITY = 80;

    private static final int SEARCH_RADIUS = 5;
    private static final int SEARCH_DEPTH = 4;

    private static final double SAMPLE_STEP = 0.2;
    private static final double INSET = 0.05;

    private static final int RAY_BUDGET = 2400;

    private final Setting reach = addNumber("Reach", "How far a face may be to count", 4.5f, 1, 4.5f, 1);
    private final Setting speed = addNumber("Speed", "Degrees the aim may travel per tick", 80, 5, 180, 0);
    private final Setting snapback = addNumber("Snapback speed", "Degrees per tick on the way back", 120, 5, 180,
            0);
    private final Setting maxBlocks = addNumber("Max blocks", "Blocks to spend in one fall; zero is unlimited", 10,
            0, 20, 0);
    private final Setting tolerance = addNumber("Rotation tolerance",
            "Degrees the aim may be from the sent rotation and still place", 25, 5, 100, 0);
    private final Setting predict = addBoolean("Simulate future position",
            "Weight the search towards where the fall is taking you", true);
    private final Setting clickMode = addChoice("Click",
            "Vanilla queues a press the game acts on itself; Direct places through the controller from here",
            List.of("Vanilla", "Direct"), 0);
    private final Setting silent = addBoolean("Silent", "Send the aim without moving the camera", true);
    private final Setting auto = addBoolean("Auto clutch", "Arm on a hit and clutch a long fall by itself", false);
    private final Setting minimumFall = addNumber("Minimum fall distance",
            "Blocks of predicted fall before auto arms", 10, 3, 20, 0).visibleWhen(auto::bool);
    private final Setting bind = addKey("Bind", "Held to clutch by hand", 0);

    private final Rng random = new Rng(0xA5C3D19F);

    private RotationTarget aim;
    private int placed;
    private int previousSlot = -1;
    private int plannedSlot = -1;
    private boolean bindingsTaken;
    private int raysLeft;

    private boolean hasLastPlaced;
    private int lastPlacedX;
    private int lastPlacedY;
    private int lastPlacedZ;

    private boolean autoArmed;
    private boolean autoChecking;
    private int autoCheckTick;
    private boolean autoLandedGuard;
    private int autoLandedAge;
    private int lastHurtTime = -1;

    private record Candidate(double cost, RotationTarget rotation) {}

    private record Cell(double score, int x, int y, int z) {}

    private static final class Body {
        private double x;
        private double y;
        private double z;
        private double motionX;
        private double motionY;
        private double motionZ;
        private boolean onGround;

        Body(FrameState state) {
            x = state.player.pos.x();
            y = state.player.pos.y();
            z = state.player.pos.z();
            motionX = state.player.velocity.x();
            motionY = state.player.velocity.y();
            motionZ = state.player.velocity.z();
            onGround = state.player.onGround;
        }

        void step(boolean stopHorizontal) {
            if (stopHorizontal) {
                motionX = 0.0;
                motionZ = 0.0;
            }
            motionY -= 0.08;

            double longest = Math.max(Math.abs(motionX), Math.max(Math.abs(motionY), Math.abs(motionZ)));
            int slices = Math.max(1, (int) Math.ceil(longest / 0.45));
            double sliceX = motionX / slices;
            double sliceY = motionY / slices;
            double sliceZ = motionZ / slices;

            boolean hitX = false;
            boolean hitY = false;
            boolean hitZ = false;
            onGround = false;

            for (int slice = 0; slice < slices && !hitY; ++slice) {
                double nextY = y + sliceY;
                if (blockedAt(x, nextY, z)) {
                    hitY = true;
                    if (sliceY < 0.0) {
                        y = Math.floor(nextY) + 1.0;
                        onGround = true;
                    }
                } else {
                    y = nextY;
                }

                if (blockedAt(x + sliceX, y, z)) {
                    hitX = true;
                } else {
                    x += sliceX;
                }

                if (blockedAt(x, y, z + sliceZ)) {
                    hitZ = true;
                } else {
                    z += sliceZ;
                }
            }

            if (hitX) {
                motionX = 0.0;
            }
            if (hitY) {
                motionY = 0.0;
            }
            if (hitZ) {
                motionZ = 0.0;
            }

            motionY *= 0.9800000190734863;
            motionX *= 0.91;
            motionZ *= 0.91;
        }
    }

    public Clutch() {
        super("Clutch", "Turns to a face within reach and blocks the fall", Category.PLAYER, Subcategory.ADVANCED);
        setIcon(Icon.BOX);
    }

    @Override
    public String suffix() {
        return aim != null ? Integer.toString(placed) : "";
    }

    @Override
    public void onEnable(Context context) {
        aim = null;
        placed = 0;
        previousSlot = -1;
        plannedSlot = -1;
        bindingsTaken = false;
        hasLastPlaced = false;
        autoArmed = false;
        autoChecking = false;
        autoCheckTick = 0;
        autoLandedGuard = false;
        autoLandedAge = 0;
        lastHurtTime = -1;
    }

    @Override
    public void onDisable(Context context) {
        giveBindings(context.state);
        restoreSlot(context.state);
        aim = null;
        hasLastPlaced = false;
        autoArmed = false;
        autoChecking = false;
        autoLandedGuard = false;
    }

    private void restoreSlot(FrameState state) {
        if (previousSlot >= 0 && state.player.valid) {
            Game.setSelectedSlot(previousSlot);
        }
        previousSlot = -1;
        plannedSlot = -1;
    }

    private void takeBindings() {

        bindingsTaken = true;
        Game.setBinding("attack", false);
        Game.setBinding("use", false);
    }

    private void giveBindings(FrameState state) {
        if (!bindingsTaken) {
            return;
        }
        bindingsTaken = false;
        if (state.screenOpen) {
            return;
        }
        Game.setBinding("attack", Input.attackHeld());
        Game.setBinding("use", Input.useHeld());
    }

    private static double cellDistance(Vec3 from, int x, int y, int z) {
        double dx = from.x() - Maths.clamp(from.x(), x, x + 1);
        double dy = from.y() - Maths.clamp(from.y(), y, y + 1);
        double dz = from.z() - Maths.clamp(from.z(), z, z + 1);
        return dx * dx + dy * dy + dz * dz;
    }

    private static boolean intersectsPlayer(FrameState state, int x, int y, int z) {
        Vec3 feet = state.player.pos;
        return x < feet.x() + HALF_WIDTH && x + 1 > feet.x() - HALF_WIDTH
                && y < feet.y() + HEIGHT && y + 1 > feet.y()
                && z < feet.z() + HALF_WIDTH && z + 1 > feet.z() - HALF_WIDTH;
    }

    private static boolean blockedAt(double x, double y, double z) {
        int lowY = Maths.floor(y);
        int highY = Maths.floor(y + HEIGHT - 0.01);
        for (int layer = lowY; layer <= highY; ++layer) {
            for (int dx = -1; dx <= 1; dx += 2) {
                for (int dz = -1; dz <= 1; dz += 2) {
                    if (!Raycast.blockPassable(Maths.floor(x + HALF_WIDTH * dx), layer,
                            Maths.floor(z + HALF_WIDTH * dz))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean willFallFar(FrameState state, double minimum) {
        Body body = new Body(state);
        double startY = body.y;
        for (int tick = 0; tick < 60; ++tick) {
            body.step(false);
            if (body.onGround) {
                return false;
            }

            if (startY - body.y > minimum) {
                return true;
            }
        }
        return false;
    }

    private static boolean willFallSoon(FrameState state) {
        Body body = new Body(state);
        for (int tick = 0; tick < 10; ++tick) {
            body.step(true);
            if (!body.onGround && body.motionY < 0.0) {
                return true;
            }
        }
        return false;
    }

    private static Vec3 futurePosition(FrameState state) {
        Body body = new Body(state);
        double startY = body.y;
        for (int tick = 0; tick < 20; ++tick) {
            body.step(false);

            if (body.y < startY - 2.0 || body.onGround) {
                break;
            }
        }
        return new Vec3(body.x, body.y, body.z);
    }

    private static boolean underPlayer(FrameState state, int cellX, int cellY, int cellZ) {
        Vec3 pos = state.player.pos;
        if (cellY >= Maths.floor(pos.y())) {
            return false;
        }
        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dz = -1; dz <= 1; dz += 2) {
                if (cellX == Maths.floor(pos.x() + HALF_WIDTH * dx)
                        && cellZ == Maths.floor(pos.z() + HALF_WIDTH * dz)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean overAir(FrameState state) {
        Vec3 pos = state.player.pos;
        return Raycast.blockPassable(Maths.floor(pos.x()), Maths.floor(pos.y()) - 1, Maths.floor(pos.z()));
    }

    private static boolean placeable(FrameState state, Raycast.BlockHit hit) {
        int x = hit.x() + hit.face().getFrontOffsetX();
        int y = hit.y() + hit.face().getFrontOffsetY();
        int z = hit.z() + hit.face().getFrontOffsetZ();
        return Raycast.blockPassable(x, y, z) && !intersectsPlayer(state, x, y, z);
    }

    private static Candidate candidate(Vec3 eye, Vec3 point, float baseYaw, float basePitch) {
        RotationTarget rotation = RotationEngine.aimAt(eye, point, baseYaw);
        double cost = Math.abs(Maths.angleDifference(baseYaw, rotation.yaw)) + Math.abs(rotation.pitch - basePitch);
        return new Candidate(cost, rotation);
    }

    private RotationTarget faceAim(FrameState state, int cellX, int cellY, int cellZ, boolean under) {
        Vec3 eye = state.player.eyePos;
        float baseYaw = RotationEngine.pendingYaw();
        float basePitch = RotationEngine.pendingPitch();

        boolean nearSouth = eye.z() > cellZ + 0.5;
        boolean nearEast = eye.x() > cellX + 0.5;
        int samples = (int) Math.round(1.0 / SAMPLE_STEP);
        float jitter = (float) (SAMPLE_STEP * 0.1);

        List<Candidate> candidates = new ArrayList<>((samples + 1) * (samples + 1) * 3 + 1);

        candidates.add(new Candidate(0.0, new RotationTarget(baseYaw, basePitch)));

        for (int row = 0; row <= samples; ++row) {
            double v = Maths.clamp(row * SAMPLE_STEP + random.range(-jitter, jitter), 0.0, 1.0);
            for (int column = 0; column <= samples; ++column) {
                double u = Maths.clamp(column * SAMPLE_STEP + random.range(-jitter, jitter), 0.0, 1.0);
                if (under) {
                    candidates.add(candidate(eye, new Vec3(cellX + u, cellY + 1.0 - INSET, cellZ + v), baseYaw,
                            basePitch));
                }
                candidates.add(candidate(eye,
                        new Vec3(cellX + u, cellY + v, nearSouth ? cellZ + 1.0 - INSET : cellZ + INSET), baseYaw,
                        basePitch));
                candidates.add(candidate(eye,
                        new Vec3(nearEast ? cellX + 1.0 - INSET : cellX + INSET, cellY + v, cellZ + u), baseYaw,
                        basePitch));
            }
        }
        candidates.sort(Comparator.comparingDouble(Candidate::cost));

        double distance = reach.number();
        for (Candidate candidate : candidates) {
            if (raysLeft <= 0) {
                return null;
            }
            --raysLeft;
            RotationTarget rotation = candidate.rotation();
            Raycast.BlockHit hit = Raycast.raycastBlocks(eye, Raycast.lookVector(rotation.yaw, rotation.pitch),
                    distance);
            if (hit == null || hit.x() != cellX || hit.y() != cellY || hit.z() != cellZ) {
                continue;
            }
            if (hit.face() == EnumFacing.DOWN || (hit.face() == EnumFacing.UP && !under)) {
                continue;
            }

            if (placeable(state, hit)) {
                return rotation;
            }
        }
        return null;
    }

    private RotationTarget search(FrameState state) {
        boolean weighted = predict.bool();
        Vec3 pos = state.player.pos;
        Vec3 future = weighted ? futurePosition(state) : pos;

        int feetX = Maths.floor(pos.x());
        int feetY = Maths.floor(pos.y());
        int feetZ = Maths.floor(pos.z());
        double reachSquared = (double) reach.number() * reach.number();

        List<Cell> cells = new ArrayList<>();
        for (int y = feetY - 1; y >= feetY - SEARCH_DEPTH; --y) {
            for (int x = feetX - SEARCH_RADIUS; x <= feetX + SEARCH_RADIUS; ++x) {
                for (int z = feetZ - SEARCH_RADIUS; z <= feetZ + SEARCH_RADIUS; ++z) {

                    if (cellDistance(state.player.eyePos, x, y, z) > reachSquared || Raycast.blockPassable(x, y, z)) {
                        continue;
                    }
                    double now = cellDistance(pos, x, y, z);
                    double score = weighted ? now * 0.3 + cellDistance(future, x, y, z) * 0.7 : now;
                    if (hasLastPlaced && x == lastPlacedX && y == lastPlacedY && z == lastPlacedZ) {
                        score *= 0.95;
                    }
                    cells.add(new Cell(score, x, y, z));
                }
            }
        }
        cells.sort(Comparator.comparingDouble(Cell::score));

        raysLeft = RAY_BUDGET;
        for (Cell cell : cells) {
            if (raysLeft <= 0) {
                return null;
            }
            RotationTarget found = faceAim(state, cell.x(), cell.y(), cell.z(),
                    underPlayer(state, cell.x(), cell.y(), cell.z()));
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static int blockSlot() {
        int current = Game.selectedSlot();
        if (Game.stack(current).block()) {
            return current;
        }
        for (int slot = 8; slot >= 0; --slot) {
            if (Game.stack(slot).block()) {
                return slot;
            }
        }
        return -1;
    }

    private void updateAuto(FrameState state) {
        if (!auto.bool()) {
            autoArmed = false;
            autoChecking = false;
            autoLandedGuard = false;
            lastHurtTime = state.player.hurtTime;
            return;
        }

        if (state.player.hurtTime > lastHurtTime) {
            autoChecking = true;
            autoCheckTick = 0;
            autoLandedGuard = false;
        }
        lastHurtTime = state.player.hurtTime;

        if (autoChecking && !autoArmed && !autoLandedGuard) {
            if (autoCheckTick % 3 == 0 && willFallFar(state, minimumFall.number())) {
                autoArmed = true;
            }
            ++autoCheckTick;
        }

        if (autoLandedGuard) {
            boolean expired = state.player.age - autoLandedAge >= 10;
            boolean jumped = Game.bindingPressed("jump");
            boolean rising = !state.player.onGround && state.player.velocity.y() > 0.0;
            if (expired || jumped || rising) {
                autoArmed = false;
                autoChecking = false;
                autoLandedGuard = false;
            }
        }

        if (autoArmed && !autoLandedGuard && state.player.onGround
                && state.player.hurtTime < state.player.maxHurtTime - 2) {
            autoLandedGuard = true;
            autoLandedAge = state.player.age;

            if (!willFallSoon(state)) {
                autoArmed = false;
                autoChecking = false;
                autoLandedGuard = false;
            }
        }

        if (!autoArmed && !autoLandedGuard && state.player.onGround && state.player.hurtTime == 0) {
            autoChecking = false;
            autoCheckTick = 0;
        }
    }

    @Override
    public void onTick(Context context) {
        FrameState state = context.state;
        if (!state.inWorld || !state.player.valid || state.screenOpen) {
            onDisable(context);
            return;
        }

        if (state.player.onGround) {
            placed = 0;
        }
        updateAuto(state);

        boolean held = bind.key() != 0 && Input.bindHeld(bind.key());
        if ((!held && !autoArmed) || !overAir(state)) {
            if (aim != null) {

                RotationTarget back = new RotationTarget(state.player.yaw, state.player.pitch);
                back.speed = snapback.number();
                back.smoothing = 0.2f;
                back.silent = silent.bool();
                back.priority = CLUTCH_PRIORITY;
                RotationEngine.submit(back);
                aim = null;
            }
            hasLastPlaced = false;
            giveBindings(state);
            restoreSlot(state);
            return;
        }

        int slot = blockSlot();
        if (slot < 0) {
            aim = null;
            giveBindings(state);
            restoreSlot(state);
            return;
        }

        RotationTarget found = search(state);
        if (found != null) {
            aim = found;
        }
        if (aim == null) {
            giveBindings(state);
            restoreSlot(state);
            return;
        }

        takeBindings();
        if (previousSlot < 0) {
            previousSlot = Game.selectedSlot();
        }
        if (plannedSlot != slot) {
            plannedSlot = slot;
            Game.setSelectedSlot(slot);
        }

        aim.speed = speed.number();
        aim.smoothing = 0.25f;
        aim.jitter = random.range(0, 1);
        aim.silent = silent.bool();
        aim.priority = CLUTCH_PRIORITY;
        RotationEngine.submit(aim);

        int limit = (int) maxBlocks.number();
        if (limit > 0 && placed >= limit) {
            return;
        }

        float packetYaw = RotationEngine.pendingYaw();
        float packetPitch = RotationEngine.pendingPitch();
        if (Math.abs(Maths.angleDifference(packetYaw, aim.yaw)) > tolerance.number()
                || Math.abs(packetPitch - aim.pitch) > tolerance.number()) {
            return;
        }

        Raycast.BlockHit hit = Raycast.raycastBlocks(state.player.eyePos, Raycast.lookVector(packetYaw, packetPitch),
                reach.number());
        boolean usable = hit != null && hit.y() < Maths.floor(state.player.pos.y()) && hit.face() != EnumFacing.DOWN
                && (hit.face() != EnumFacing.UP || underPlayer(state, hit.x(), hit.y(), hit.z()))
                && placeable(state, hit);
        if (!usable) {
            return;
        }

        hasLastPlaced = true;
        lastPlacedX = hit.x();
        lastPlacedY = hit.y();
        lastPlacedZ = hit.z();

        boolean counts = hit.face() != EnumFacing.UP;

        if (clickMode.choice() == 0) {

            Game.setCrosshairTarget(hit.x(), hit.y(), hit.z(), hit.face(), hit.point());

            Game.setItemUseCooldown(0);
            Game.queueClick(true);
            if (counts) {
                ++placed;
            }
            return;
        }
        if (Game.placeBlock(hit.x(), hit.y(), hit.z(), hit.face(), hit.point(), true) && counts) {
            ++placed;
        }
    }
}
