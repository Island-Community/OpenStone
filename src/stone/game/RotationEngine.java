package stone.game;

import stone.core.Log;
import stone.core.Maths;
import stone.core.Rng;
import stone.core.Vec3;
import net.minecraft.client.entity.EntityPlayerSP;

public final class RotationEngine {
    private static final Rng noise = new Rng(0x9E3779B9);

    private static double sensitivity = -1.0;
    private static float step = 0.15f;

    private static final RotationTarget request = new RotationTarget();
    private static boolean hasRequest;
    private static boolean engaged;

    private static float currentYaw;
    private static float currentPitch;
    private static boolean active;
    private static float lastCameraYaw;
    private static float lastCameraPitch;
    private static float sentYaw;
    private static float sentPitch;
    private static float pendingYaw;
    private static float pendingPitch;

    private static boolean silentArmed;
    private static boolean silentApplied;
    private static float silentYaw;
    private static float silentPitch;
    private static float restoreYaw;
    private static float restorePitch;

    private RotationEngine() {}

    public static void configure(double value) {
        if (Math.abs(value - sensitivity) < 1e-9) {
            return;
        }
        sensitivity = value;
        double scaled = value * 0.6 + 0.2;
        step = (float) (scaled * scaled * scaled * 8.0 * 0.15);
        if (!(step > 0.0f)) {
            step = 0.15f;
        }
        Log.info("aim", "sensitivity %.4f -> %.6f deg per mouse count", value, step);
    }

    public static float step() {
        return step;
    }

    public static void submit(RotationTarget target) {
        if (hasRequest && target.priority < request.priority) {
            return;
        }
        request.yaw = target.yaw;
        request.pitch = target.pitch;
        request.speed = target.speed;
        request.smoothing = target.smoothing;
        request.jitter = target.jitter;
        request.silent = target.silent;
        request.priority = target.priority;
        hasRequest = true;
    }

    public static void reset() {
        hasRequest = false;
        engaged = false;
        silentArmed = false;
        silentApplied = false;
        active = false;
    }

    public static void tick(FrameState state) {
        silentArmed = false;
        if (!state.player.valid) {
            reset();
            return;
        }
        configure(state.mouseSensitivity);

        float cameraYaw = state.player.yaw;
        float cameraPitch = state.player.pitch;

        if (!active) {
            currentYaw = cameraYaw;
            currentPitch = cameraPitch;
        } else {
            currentYaw += Maths.wrapDegrees(cameraYaw - lastCameraYaw);
            currentPitch = Maths.clamp(currentPitch + (cameraPitch - lastCameraPitch), -90.0f, 90.0f);
        }
        lastCameraYaw = cameraYaw;
        lastCameraPitch = cameraPitch;

        if (!hasRequest) {
            engaged = false;
            active = false;
            pendingYaw = cameraYaw;
            pendingPitch = cameraPitch;
            return;
        }

        float yawError = Maths.wrapDegrees(request.yaw - currentYaw);
        float pitchError = Maths.clamp(request.pitch, -90.0f, 90.0f) - currentPitch;

        float magnitude = (float) Math.sqrt(yawError * yawError + pitchError * pitchError);
        float approach = Maths.clamp(magnitude / 45.0f, 0.12f, 1.0f);
        float allowance = request.speed * approach * (1.0f - Maths.clamp(request.smoothing, 0.0f, 0.95f));

        currentYaw += quantise(Maths.clamp(yawError, -allowance, allowance), request.jitter);
        currentPitch = Maths.clamp(
                currentPitch + quantise(Maths.clamp(pitchError, -allowance, allowance), request.jitter),
                -90.0f, 90.0f);
        pendingYaw = currentYaw;
        pendingPitch = currentPitch;

        if (request.silent) {
            silentYaw = currentYaw;
            silentPitch = currentPitch;
            silentArmed = true;
        } else {
            lastCameraYaw = currentYaw;
            lastCameraPitch = currentPitch;
            Game.setRotation(currentYaw, currentPitch, false);
        }

        active = true;
        engaged = true;
        hasRequest = false;
    }

    public static void preMotion(EntityPlayerSP player) {
        if (!silentArmed) {
            return;
        }
        restoreYaw = player.rotationYaw;
        restorePitch = player.rotationPitch;
        player.rotationYaw = silentYaw;
        player.rotationPitch = silentPitch;
        silentArmed = false;
        silentApplied = true;
    }

    public static void postMotion(EntityPlayerSP player) {
        if (silentApplied) {
            player.rotationYaw = restoreYaw;
            player.rotationPitch = restorePitch;
            silentApplied = false;
        }
        sentYaw = player.lastReportedYaw;
        sentPitch = player.lastReportedPitch;
    }

    public static boolean engaged() {
        return engaged;
    }

    public static float yaw() {
        return currentYaw;
    }

    public static float pitch() {
        return currentPitch;
    }

    public static float sentYaw() {
        return sentYaw;
    }

    public static float sentPitch() {
        return sentPitch;
    }

    public static float pendingYaw() {
        return pendingYaw;
    }

    public static float pendingPitch() {
        return pendingPitch;
    }

    public static RotationTarget aimAt(Vec3 eye, Vec3 point, float currentYaw) {
        double dx = point.x() - eye.x();
        double dy = point.y() - eye.y();
        double dz = point.z() - eye.z();
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        float rawYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        return new RotationTarget(currentYaw + Maths.wrapDegrees(rawYaw - currentYaw),
                Maths.clamp((float) -Math.toDegrees(Math.atan2(dy, horizontal)), -90.0f, 90.0f));
    }

    private static float quantise(float degrees, int jitter) {
        long rounded = Math.round(degrees / step);
        if (jitter > 0) {
            rounded += noise.range(-jitter, jitter);
        }

        if (rounded == 0 && Math.abs(degrees) > step * 0.5f) {
            rounded = degrees > 0.0f ? 1 : -1;
        }
        return rounded * step;
    }
}
