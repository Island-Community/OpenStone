package stone.module.combat;

import stone.core.Color;
import stone.core.Maths;
import stone.core.Time;
import stone.core.Vec3;
import stone.game.CombatTarget;
import stone.game.EntityView;
import stone.game.FrameState;
import stone.game.Game;
import stone.game.Projection;
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
import stone.render.Renderer;
import stone.ui.Theme;
import java.util.List;

public final class Displace extends Module {
    private static final int DIRECTIONS = 16;
    private static final int STATIC_RINGS = 10;
    private static final double RING_STEP = 0.5;
    private static final int VOID_DEPTH = 10;
    private static final double LANE_STEP = 0.5;
    private static final double LANE_DISTANCE = 6.0;
    private static final double LANE_SIDE = 0.45;

    private static final int WINDOW_TICKS = 10;
    private static final int DISPLACE_PRIORITY = 60;
    private static final double[] COMPASS_X = new double[DIRECTIONS];
    private static final double[] COMPASS_Z = new double[DIRECTIONS];

    static {
        for (int i = 0; i < DIRECTIONS; ++i) {
            double angle = 2.0 * Math.PI * i / DIRECTIONS;
            COMPASS_X[i] = Math.cos(angle);
            COMPASS_Z[i] = Math.sin(angle);
        }
    }

    private final Setting mode = addChoice("Mode",
            "Static turns by a fixed offset; Dynamic aims down the emptiest lane", List.of("Static", "Dynamic"), 0);
    private final Setting yawOffset = addNumber("Yaw offset", "Degrees to turn on the displace tick", 90, 0, 180, 0)
            .visibleWhen(() -> mode.choice() == 0);
    private final Setting direction = addChoice("Direction", "Which way to turn when there is no void to aim at",
            List.of("Left", "Right"), 0).visibleWhen(() -> mode.choice() == 0);
    private final Setting findVoid = addBoolean("Find void", "Aim at the nearest hole rather than the fixed offset",
            true).visibleWhen(() -> mode.choice() == 0);
    private final Setting delay = addNumber("Delay", "Milliseconds into the window before displacing", 0, 0, 500, 0);
    private final Setting range = addNumber("Range", "How far a target may be", 4.5f, 2, 9, 1);
    private final Setting showDirection = addBoolean("Show direction", "Draw the arrow the knockback follows", true);

    private boolean displaceTick;
    private boolean compensate;
    private boolean left;
    private boolean forwardHeld;
    private boolean strafeHeld;

    private boolean forwardWasTheirs;
    private boolean strafeWasTheirs;

    private boolean strafeLeft;
    private int holdTicks;
    private int tick;
    private int windowStart = -1;
    private int windowTargetId = -1;

    private boolean arrowValid;
    private float arrowYaw;
    private int arrowTargetId = -1;
    private float arrowFade;

    public Displace() {
        super("Displace", "Turns on the hit so the knockback throws them sideways", Category.COMBAT,
                Subcategory.EXTREME);
        setIcon(Icon.MOVE_3D);
    }

    @Override
    public String suffix() {
        return (int) delay.number() + "ms";
    }

    @Override
    public void onEnable(Context context) {
        displaceTick = false;
        compensate = false;
        forwardHeld = false;
        strafeHeld = false;
        forwardWasTheirs = false;
        strafeWasTheirs = false;
        tick = 0;
        windowStart = -1;
        windowTargetId = -1;
        arrowValid = false;
        arrowFade = 0.0f;
    }

    @Override
    public void onDisable(Context context) {
        releaseMovement();
        displaceTick = false;
        compensate = false;
        arrowValid = false;
    }

    private void releaseMovement() {
        if (forwardHeld) {
            if (!forwardWasTheirs) {
                Game.setBinding("forward", false);
            }
            forwardHeld = false;
        }
        if (strafeHeld) {
            if (!strafeWasTheirs) {
                Game.setBinding(strafeLeft ? "left" : "right", false);
            }
            strafeHeld = false;
        }
        holdTicks = 0;
    }

    private static float yawFromForward(double x, double z) {
        return (float) Math.toDegrees(Math.atan2(z, x)) - 90.0f;
    }

    private static boolean voidColumn(double x, double y, double z) {
        int cellX = Maths.floor(x);
        int cellZ = Maths.floor(z);
        int top = Maths.floor(y) - 1;
        for (int cellY = top; cellY > top - VOID_DEPTH; --cellY) {
            if (!Raycast.blockPassable(cellX, cellY, cellZ)) {
                return false;
            }
        }
        return true;
    }

    private float staticVoidYaw(FrameState state, EntityView target) {
        Vec3 feet = target.tickPos;
        Vec3 self = state.player.pos;
        double bestX = 0.0;
        double bestZ = 0.0;
        double bestScore = 0.0;
        boolean found = false;

        for (int ring = 1; ring <= STATIC_RINGS && !found; ++ring) {
            double radius = ring * RING_STEP;
            for (int index = 0; index < DIRECTIONS; ++index) {
                double x = feet.x() + COMPASS_X[index] * radius;
                double z = feet.z() + COMPASS_Z[index] * radius;
                if (!voidColumn(x, feet.y(), z)) {
                    continue;
                }
                double dx = x - self.x();
                double dz = z - self.z();
                double score = dx * dx + dz * dz;
                if (!found || score < bestScore) {
                    bestScore = score;
                    bestX = x;
                    bestZ = z;
                    found = true;
                }
            }
        }
        if (!found) {
            return Float.NaN;
        }

        double targetDx = feet.x() - self.x();
        double targetDz = feet.z() - self.z();
        left = targetDx * (bestZ - self.z()) - targetDz * (bestX - self.x()) < 0.0;

        double dx = bestX - feet.x();
        double dz = bestZ - feet.z();
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 0.001) {
            return Float.NaN;
        }
        double radius = Math.min(length, Math.max(0.35, target.width * 0.5 + 0.15));
        double aimX = feet.x() + dx / length * radius;
        double aimZ = feet.z() + dz / length * radius;
        return yawFromForward(aimX - state.player.eyePos.x(), aimZ - state.player.eyePos.z());
    }

    private float dynamicVoidYaw(FrameState state, EntityView target) {
        Vec3 feet = target.tickPos;
        Vec3 self = state.player.pos;
        double bestScore = 0.0;
        double bestX = 0.0;
        double bestZ = 0.0;

        int steps = (int) (LANE_DISTANCE / LANE_STEP);
        for (int index = 0; index < DIRECTIONS; ++index) {
            double forwardX = COMPASS_X[index];
            double forwardZ = COMPASS_Z[index];
            double sideX = -forwardZ;
            double sideZ = forwardX;
            double score = 0.0;
            int run = 0;

            for (int step = 1; step <= steps; ++step) {
                double along = step * LANE_STEP;
                double centreX = feet.x() + forwardX * along;
                double centreZ = feet.z() + forwardZ * along;

                if (!Raycast.blockPassable(Maths.floor(centreX), Maths.floor(feet.y()), Maths.floor(centreZ))) {
                    break;
                }

                boolean centreVoid = false;
                for (int lane = -1; lane <= 1; ++lane) {
                    double offset = lane * LANE_SIDE;
                    if (!voidColumn(centreX + sideX * offset, feet.y(), centreZ + sideZ * offset)) {
                        continue;
                    }

                    score += (lane == 0 ? 1.4 : 1.0) * (LANE_DISTANCE + LANE_STEP - along);
                    centreVoid |= lane == 0;
                }
                run = centreVoid ? run + 1 : 0;
                score += run * 2.0;
            }

            if (score > bestScore) {
                bestScore = score;
                bestX = forwardX;
                bestZ = forwardZ;
            }
        }
        if (bestScore <= 0.0) {
            return Float.NaN;
        }

        double targetDx = feet.x() - self.x();
        double targetDz = feet.z() - self.z();
        left = targetDx * (feet.z() + bestZ - self.z()) - targetDz * (feet.x() + bestX - self.x()) < 0.0;
        return yawFromForward(bestX, bestZ);
    }

    private EntityView pickTarget(FrameState state) {
        int published = CombatTarget.current(Time.nowSeconds());
        if (!Input.attackHeld() && published < 0) {
            return null;
        }

        EntityView best = null;
        double bestDistance = range.number();
        for (EntityView entity : state.entities) {
            if (!entity.isPlayer || entity.health <= 0.0f) {
                continue;
            }

            if (entity.id == published && entity.distance <= range.number()) {
                return entity;
            }
            if (entity.distance <= bestDistance) {
                bestDistance = entity.distance;
                best = entity;
            }
        }
        return best;
    }

    @Override
    public void onTick(Context context) {
        FrameState state = context.state;
        if (!state.inWorld || !state.player.valid || state.screenOpen) {
            onDisable(context);
            return;
        }

        ++tick;

        if (holdTicks > 0 && --holdTicks == 0) {
            releaseMovement();
        }

        EntityView target = pickTarget(state);
        if (target == null) {
            releaseMovement();
            displaceTick = false;
            compensate = false;
            arrowValid = false;
            windowTargetId = -1;
            return;
        }

        if (compensate) {
            compensate = false;
            strafeLeft = left;
            String strafe = strafeLeft ? "left" : "right";
            strafeWasTheirs = Game.bindingPressed(strafe);
            Game.setBinding(strafe, true);
            strafeHeld = true;
            holdTicks = 1;
            return;
        }

        float yaw;
        if (mode.choice() == 1) {
            yaw = dynamicVoidYaw(state, target);
        } else {
            yaw = findVoid.bool() ? staticVoidYaw(state, target) : Float.NaN;
            if (Float.isNaN(yaw)) {
                left = direction.choice() == 0;
                float base = RotationEngine.sentYaw();
                yaw = left ? base - yawOffset.number() : base + yawOffset.number();
            }
        }
        if (Float.isNaN(yaw)) {
            arrowValid = false;
            return;
        }

        arrowValid = true;
        arrowYaw = yaw;
        arrowTargetId = target.id;

        displaceTick = !displaceTick;
        if (!displaceTick) {
            return;
        }

        if (windowTargetId != target.id || windowStart < 0 || tick - windowStart >= WINDOW_TICKS) {
            windowTargetId = target.id;
            windowStart = tick;
        } else {
            int delayTicks = (int) Math.ceil(delay.number() / 50.0f);
            if (tick - windowStart < delayTicks) {
                displaceTick = false;
                return;
            }
        }

        RotationTarget rotation = new RotationTarget(yaw, RotationEngine.sentPitch());
        rotation.speed = 180.0f;
        rotation.smoothing = 0.0f;
        rotation.silent = true;
        rotation.priority = DISPLACE_PRIORITY;
        RotationEngine.submit(rotation);

        forwardWasTheirs = Game.bindingPressed("forward");
        Game.setBinding("forward", true);
        forwardHeld = true;
        holdTicks = 1;
        compensate = true;
    }

    @Override
    public void onRender(Context context) {
        arrowFade = Maths.approach(arrowFade, arrowValid ? 1.0f : 0.0f, 9.0f, context.deltaTime);
        Projection projection = context.projection;
        if (!showDirection.bool() || arrowFade < 0.02f || !projection.valid()) {
            return;
        }

        EntityView target = null;
        for (EntityView entity : context.state.entities) {
            if (entity.id == arrowTargetId) {
                target = entity;
                break;
            }
        }
        if (target == null) {
            return;
        }

        double yawRadians = Math.toRadians(arrowYaw);
        double forwardX = -Math.sin(yawRadians);
        double forwardZ = Math.cos(yawRadians);
        double sideX = -forwardZ;
        double sideZ = forwardX;
        double centreX = target.pos.x();
        double centreY = target.pos.y() + target.height * 0.5;
        double centreZ = target.pos.z();
        double base = target.width * 0.5 + 0.24;

        double[][] shape = {
            {0.0, -0.08}, {0.74, -0.08}, {0.56, -0.30}, {1.26, 0.0}, {0.56, 0.30}, {0.74, 0.08}, {0.0, 0.08},
        };
        float[] xs = new float[shape.length];
        float[] ys = new float[shape.length];
        for (int index = 0; index < shape.length; ++index) {
            double along = base + shape[index][0];
            double across = shape[index][1];
            Projection.ScreenPoint point = projection.project(new Vec3(centreX + forwardX * along + sideX * across,
                    centreY, centreZ + forwardZ * along + sideZ * across));
            if (point == null) {
                return;
            }
            xs[index] = point.x();
            ys[index] = point.y();
        }

        Renderer gfx = Renderer.instance();
        float alpha = arrowFade * animation();
        Color fill = Theme.accent().withAlpha(0.82f * alpha);
        Color edge = Color.rgb(0x05070A, 0.9f * alpha);

        gfx.face(new float[] {xs[0], xs[1], xs[5], xs[6]}, new float[] {ys[0], ys[1], ys[5], ys[6]}, fill);
        gfx.face(new float[] {xs[2], xs[3], xs[3], xs[4]}, new float[] {ys[2], ys[3], ys[3], ys[4]}, fill);
        for (int index = 0; index < shape.length; ++index) {
            int next = (index + 1) % shape.length;
            gfx.line(xs[index], ys[index], xs[next], ys[next], 1.4f, edge);
        }
    }
}
