package stone.game;

import stone.core.Mat4;
import stone.core.Rect;
import stone.core.Vec3;

public final class Projection {
    public record ScreenPoint(float x, float y, float depth) {}

    private Mat4 viewProjection = new Mat4();
    private Vec3 cameraPosition = Vec3.ZERO;
    private float width;
    private float height;
    private boolean valid;
    private final float[] clip = new float[4];

    public void update(FrameState state, int targetWidth, int targetHeight) {
        width = targetWidth;
        height = targetHeight;
        cameraPosition = state.camera.pos;
        viewProjection = state.projection.mul(state.view);
        valid = state.inWorld && state.matricesValid && width > 0.0f && height > 0.0f;
    }

    public boolean valid() {
        return valid;
    }

    public ScreenPoint project(Vec3 world) {
        if (!valid) {
            return null;
        }
        viewProjection.transform(world.x() - cameraPosition.x(), world.y() - cameraPosition.y(),
                world.z() - cameraPosition.z(), clip);
        if (clip[3] <= 0.0001f) {
            return null;
        }
        float inverseW = 1.0f / clip[3];
        float ndcX = clip[0] * inverseW;
        float ndcY = clip[1] * inverseW;
        return new ScreenPoint((ndcX * 0.5f + 0.5f) * width, (1.0f - (ndcY * 0.5f + 0.5f)) * height, clip[3]);
    }

    public Rect projectBox(Vec3 minimum, Vec3 maximum) {
        if (!valid) {
            return null;
        }
        float left = width;
        float top = height;
        float right = 0.0f;
        float bottom = 0.0f;

        for (int corner = 0; corner < 8; ++corner) {
            ScreenPoint point = project(new Vec3((corner & 1) != 0 ? maximum.x() : minimum.x(),
                    (corner & 2) != 0 ? maximum.y() : minimum.y(), (corner & 4) != 0 ? maximum.z() : minimum.z()));

            if (point == null) {
                return null;
            }
            left = Math.min(left, point.x());
            right = Math.max(right, point.x());
            top = Math.min(top, point.y());
            bottom = Math.max(bottom, point.y());
        }

        if (right <= 0.0f || bottom <= 0.0f || left >= width || top >= height) {
            return null;
        }
        Rect out = new Rect(left, top, right - left, bottom - top);
        return out.width() > 0.5f && out.height() > 0.5f ? out : null;
    }
}
