package stone.render;

import stone.core.Maths;

final class DistanceField {
    private static final float INFINITY = 1e20f;

    private DistanceField() {}

    static void encode(byte[] coverage, int rasterWidth, int rasterHeight, int supersample, float spread,
            byte[] target, int offset, int stride, int width, int height) {
        int size = rasterWidth * rasterHeight;
        float[] inside = new float[size];
        float[] outside = new float[size];
        for (int pixel = 0; pixel < size; ++pixel) {
            boolean solid = (coverage[pixel] & 0xFF) >= 128;
            inside[pixel] = solid ? 0.0f : INFINITY;
            outside[pixel] = solid ? INFINITY : 0.0f;
        }
        transform2d(inside, rasterWidth, rasterHeight);
        transform2d(outside, rasterWidth, rasterHeight);

        int half = supersample / 2;
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                int sample = (y * supersample + half) * rasterWidth + x * supersample + half;

                float distance = inside[sample] == 0.0f
                        ? (float) Math.sqrt(outside[sample]) - 0.5f
                        : 0.5f - (float) Math.sqrt(inside[sample]);
                float encoded = 128.0f + Maths.clamp(distance / supersample / spread, -1.0f, 1.0f) * 127.0f;
                target[offset + y * stride + x] = (byte) (int) (encoded + 0.5f);
            }
        }
    }

    private static void transform1d(float[] source, float[] destination, int length, int[] hull, float[] boundary) {
        int k = 0;
        hull[0] = 0;
        boundary[0] = -INFINITY;
        boundary[1] = INFINITY;
        for (int q = 1; q < length; ++q) {
            float intersection = intersect(source, hull[k], q);
            while (intersection <= boundary[k]) {
                --k;
                intersection = intersect(source, hull[k], q);
            }
            ++k;
            hull[k] = q;
            boundary[k] = intersection;
            boundary[k + 1] = INFINITY;
        }
        k = 0;
        for (int q = 0; q < length; ++q) {
            while (boundary[k + 1] < q) {
                ++k;
            }
            float delta = q - hull[k];
            destination[q] = delta * delta + source[hull[k]];
        }
    }

    private static float intersect(float[] source, int p, int q) {
        return ((source[q] + (float) (q * q)) - (source[p] + (float) (p * p))) / (float) (2 * q - 2 * p);
    }

    private static void transform2d(float[] grid, int width, int height) {
        int longest = Math.max(width, height);
        float[] column = new float[longest];
        float[] result = new float[longest];
        int[] hull = new int[longest];
        float[] boundary = new float[longest + 1];

        for (int y = 0; y < height; ++y) {
            System.arraycopy(grid, y * width, column, 0, width);
            transform1d(column, result, width, hull, boundary);
            System.arraycopy(result, 0, grid, y * width, width);
        }
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                column[y] = grid[y * width + x];
            }
            transform1d(column, result, height, hull, boundary);
            for (int y = 0; y < height; ++y) {
                grid[y * width + x] = result[y];
            }
        }
    }
}
