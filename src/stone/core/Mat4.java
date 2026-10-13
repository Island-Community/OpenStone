package stone.core;

public final class Mat4 {
    public final float[] m = {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};

    public float at(int column, int row) {
        return m[column * 4 + row];
    }

    public void set(int column, int row, float value) {
        m[column * 4 + row] = value;
    }

    public Mat4 mul(Mat4 rhs) {
        Mat4 result = new Mat4();
        for (int column = 0; column < 4; ++column) {
            for (int row = 0; row < 4; ++row) {
                float sum = 0.0f;
                for (int k = 0; k < 4; ++k) {
                    sum += at(k, row) * rhs.at(column, k);
                }
                result.set(column, row, sum);
            }
        }
        return result;
    }

    public void transform(double px, double py, double pz, float[] out) {
        float x = (float) px;
        float y = (float) py;
        float z = (float) pz;
        for (int row = 0; row < 4; ++row) {
            out[row] = at(0, row) * x + at(1, row) * y + at(2, row) * z + at(3, row);
        }
    }

    public static Mat4 orthographic(float left, float right, float bottom, float top) {
        Mat4 result = new Mat4();
        result.set(0, 0, 2.0f / (right - left));
        result.set(1, 1, 2.0f / (top - bottom));
        result.set(2, 2, -1.0f);
        result.set(3, 0, -(right + left) / (right - left));
        result.set(3, 1, -(top + bottom) / (top - bottom));
        return result;
    }
}
