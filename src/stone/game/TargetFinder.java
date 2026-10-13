package stone.game;

import stone.core.Maths;
import stone.core.Vec3;

public final class TargetFinder {
    public enum Sort { DISTANCE, HEALTH, ANGLE, HURT_TIME }

    public static final class Filter {
        public float range = 3.0f;
        public boolean players = true;
        public boolean mobs = true;
        public boolean invisibles;
    }

    private TargetFinder() {}

    public static double hitboxDistance(Vec3 eye, Vec3 minimum, Vec3 maximum) {
        double x = Maths.clamp(eye.x(), minimum.x(), maximum.x()) - eye.x();
        double y = Maths.clamp(eye.y(), minimum.y(), maximum.y()) - eye.y();
        double z = Maths.clamp(eye.z(), minimum.z(), maximum.z()) - eye.z();
        return Math.sqrt(x * x + y * y + z * z);
    }

    public static Vec3 aimPoint(Vec3 eye, EntityView entity, float shrink) {
        Vec3 minimum = entity.tickBoxMin();
        Vec3 maximum = entity.tickBoxMax();
        double insetXZ = Math.min(shrink, entity.width * 0.35f);
        double insetY = Math.min(shrink, entity.height * 0.2f);
        return new Vec3(Maths.clamp(eye.x(), minimum.x() + insetXZ, maximum.x() - insetXZ),
                Maths.clamp(eye.y(), minimum.y() + insetY, maximum.y() - insetY),
                Maths.clamp(eye.z(), minimum.z() + insetXZ, maximum.z() - insetXZ));
    }

    public static EntityView selectTarget(FrameState state, Filter filter, Sort sort) {
        if (!state.player.valid) {
            return null;
        }
        Vec3 eye = state.player.eyePos;
        EntityView best = null;
        double bestScore = 0.0;

        for (EntityView entity : state.entities) {
            if (entity.isPlayer ? !filter.players : !filter.mobs) {
                continue;
            }
            if (!filter.invisibles && entity.invisible) {
                continue;
            }
            double distance = hitboxDistance(eye, entity.tickBoxMin(), entity.tickBoxMax());
            if (distance > filter.range) {
                continue;
            }

            double score = switch (sort) {
                case DISTANCE -> -distance;
                case HEALTH -> -(entity.health + entity.absorption);
                case ANGLE -> {
                    double dx = entity.tickPos.x() - eye.x();
                    double dz = entity.tickPos.z() - eye.z();
                    float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
                    yield -Math.abs(Maths.wrapDegrees(yaw - state.player.yaw));
                }
                case HURT_TIME -> -entity.hurtTime * 100.0 - distance;
            };
            if (best == null || score > bestScore) {
                best = entity;
                bestScore = score;
            }
        }
        return best;
    }
}
