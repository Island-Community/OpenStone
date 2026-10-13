package stone.game;

import stone.core.Vec3;
import net.minecraft.util.EnumFacing;

public final class Raycast {
    public record BlockHit(int x, int y, int z, EnumFacing face, Vec3 point, double distance) {}

    private static final int CACHE_SIZE = 1024;
    private static final int[] cacheX = new int[CACHE_SIZE];
    private static final int[] cacheY = new int[CACHE_SIZE];
    private static final int[] cacheZ = new int[CACHE_SIZE];
    private static final boolean[] cacheValid = new boolean[CACHE_SIZE];
    private static final boolean[] cachePassable = new boolean[CACHE_SIZE];

    private Raycast() {}

    public static boolean blockPassable(int x, int y, int z) {
        int slot = Integer.remainderUnsigned(x * 73856093 ^ y * 19349663 ^ z * 83492791, CACHE_SIZE);
        if (cacheValid[slot] && cacheX[slot] == x && cacheY[slot] == y && cacheZ[slot] == z) {
            return cachePassable[slot];
        }
        cacheX[slot] = x;
        cacheY[slot] = y;
        cacheZ[slot] = z;
        cacheValid[slot] = true;
        cachePassable[slot] = Game.isBlockReplaceable(x, y, z);
        return cachePassable[slot];
    }

    public static void resetBlockCache() {
        java.util.Arrays.fill(cacheValid, false);
    }

    public static double rayBox(Vec3 origin, Vec3 direction, Vec3 minimum, Vec3 maximum, double reach) {
        double[] from = {origin.x(), origin.y(), origin.z()};
        double[] along = {direction.x(), direction.y(), direction.z()};
        double[] low = {minimum.x(), minimum.y(), minimum.z()};
        double[] high = {maximum.x(), maximum.y(), maximum.z()};
        if (from[0] >= low[0] && from[0] <= high[0] && from[1] >= low[1] && from[1] <= high[1]
                && from[2] >= low[2] && from[2] <= high[2]) {
            return 0.0;
        }

        double near = 0.0;
        double far = reach;
        for (int axis = 0; axis < 3; ++axis) {
            if (Math.abs(along[axis]) < 1e-9) {
                if (from[axis] < low[axis] || from[axis] > high[axis]) {
                    return -1.0;
                }
                continue;
            }
            double inverse = 1.0 / along[axis];
            double entry = (low[axis] - from[axis]) * inverse;
            double exit = (high[axis] - from[axis]) * inverse;
            near = Math.max(near, Math.min(entry, exit));
            far = Math.min(far, Math.max(entry, exit));
            if (near > far) {
                return -1.0;
            }
        }
        return near;
    }

    public static Vec3 lookVector(float yaw, float pitch) {
        double yawRadians = Math.toRadians(yaw);
        double pitchRadians = Math.toRadians(pitch);
        double horizontal = Math.cos(pitchRadians);
        return new Vec3(-Math.sin(yawRadians) * horizontal, -Math.sin(pitchRadians),
                Math.cos(yawRadians) * horizontal);
    }

    public static BlockHit raycastBlocks(Vec3 origin, Vec3 direction, double reach) {
        if (reach <= 0.0) {
            return null;
        }

        int cellX = (int) Math.floor(origin.x());
        int cellY = (int) Math.floor(origin.y());
        int cellZ = (int) Math.floor(origin.z());

        int stepX = (int) Math.signum(direction.x());
        int stepY = (int) Math.signum(direction.y());
        int stepZ = (int) Math.signum(direction.z());

        double deltaX = stepX != 0 ? Math.abs(1.0 / direction.x()) : Double.MAX_VALUE;
        double deltaY = stepY != 0 ? Math.abs(1.0 / direction.y()) : Double.MAX_VALUE;
        double deltaZ = stepZ != 0 ? Math.abs(1.0 / direction.z()) : Double.MAX_VALUE;

        double nextX = stepX != 0 ? boundary(origin.x(), cellX, stepX) * deltaX : Double.MAX_VALUE;
        double nextY = stepY != 0 ? boundary(origin.y(), cellY, stepY) * deltaY : Double.MAX_VALUE;
        double nextZ = stepZ != 0 ? boundary(origin.z(), cellZ, stepZ) * deltaZ : Double.MAX_VALUE;

        if (!blockPassable(cellX, cellY, cellZ)) {
            return null;
        }

        for (int guard = 0; guard < 96; ++guard) {
            double travelled;
            EnumFacing entered;
            if (nextX <= nextY && nextX <= nextZ) {
                travelled = nextX;
                nextX += deltaX;
                cellX += stepX;
                entered = stepX > 0 ? EnumFacing.WEST : EnumFacing.EAST;
            } else if (nextY <= nextZ) {
                travelled = nextY;
                nextY += deltaY;
                cellY += stepY;
                entered = stepY > 0 ? EnumFacing.DOWN : EnumFacing.UP;
            } else {
                travelled = nextZ;
                nextZ += deltaZ;
                cellZ += stepZ;
                entered = stepZ > 0 ? EnumFacing.NORTH : EnumFacing.SOUTH;
            }
            if (travelled > reach) {
                return null;
            }
            if (!blockPassable(cellX, cellY, cellZ)) {
                return new BlockHit(cellX, cellY, cellZ, entered, origin.add(direction.mul(travelled)), travelled);
            }
        }
        return null;
    }

    private static double boundary(double position, int cell, int step) {
        return step > 0 ? cell + 1 - position : position - cell;
    }
}
