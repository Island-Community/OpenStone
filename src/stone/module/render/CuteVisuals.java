package stone.module.render;

import stone.core.Color;
import stone.core.Maths;
import stone.core.Rect;
import stone.core.Rng;
import stone.core.Timer;
import stone.core.Vec2;
import stone.core.Vec3;
import stone.game.BlockKind;
import stone.game.CrosshairTarget;
import stone.game.FrameState;
import stone.game.Game;
import stone.game.Projection.ScreenPoint;
import stone.hook.Input;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.render.Icon;
import stone.render.Renderer;
import stone.ui.Theme;
import java.util.ArrayList;
import java.util.List;

public final class CuteVisuals extends Module {
    private static final int MAX_HEARTS = 40;
    private static final int MAX_DOTS = 80;
    private static final int MAX_SPARKS = 160;
    private static final int MAX_RAINBOWS = 4;

    private static final int RAINBOW_SEGMENTS = 28;
    private static final double HEART_RADIUS = 1.5;
    private static final double DOT_SPREAD = 0.9;
    private static final double DOT_DRIFT = 0.3;
    private static final double TWO_PI = Math.PI * 2.0;

    private static final float MAX_PARTICLE_PIXELS = 90.0f;

    private static final Vec2[] HEART = heartShape(20);
    private static final Vec2[] STAR = starShape();
    private static final Vec2[] DIAMOND = {new Vec2(0.0f, -14.0f), new Vec2(8.0f, 0.0f), new Vec2(0.0f, 14.0f),
            new Vec2(-8.0f, 0.0f), new Vec2(0.0f, -14.0f)};

    private static final float[] HEART_WEIGHTS = {1.15f, 0.85f, 1.0f};
    private static final float[] DOT_WEIGHTS = {1.0f, 1.15f, 0.8f, 1.25f};

    private abstract static class Particle {
        Vec3 position;
        float age;
    }

    private static final class Heart extends Particle {
        float tilt;
        float scale;
        int type;
        int seed;
    }

    private static final class Dot extends Particle {
        Vec3 drift;
        float scale;
        int type;
        int seed;
    }

    private static final class Spark extends Particle {
        Vec3 velocity;
        float scale;
        int type;
        int seed;
    }

    private static final class Rainbow extends Particle {
        float yaw;
    }

    private record Billboard(float x, float y, float pixelsPerBlock) {}

    private final Setting onlyMoving = addBoolean("Only while moving", "Stop the trail when standing still", true);
    private final Setting opacity = addNumber("Opacity", "Percent opacity of the trail", 85, 20, 100, 0);
    private final Setting hearts = addBoolean("Hearts", "Floating hearts behind you", true);
    private final Setting heartRate = addNumber("Heart rate", "Milliseconds between hearts", 200, 50, 500, 0)
            .visibleWhen(hearts::bool);
    private final Setting heartLife = addNumber("Heart lifetime", "Milliseconds a heart lasts", 1500, 500, 4000, 0)
            .visibleWhen(hearts::bool);
    private final Setting dots = addBoolean("Dots", "Drifting dots behind you", true);
    private final Setting dotRate = addNumber("Dot rate", "Milliseconds between dots", 100, 20, 200, 0)
            .visibleWhen(dots::bool);
    private final Setting dotLife = addNumber("Dot lifetime", "Milliseconds a dot lasts", 1500, 500, 5000, 0)
            .visibleWhen(dots::bool);
    private final Setting pulse = addBoolean("Pulse", "Flicker the dots", false).visibleWhen(dots::bool);

    private final Setting bedBurst = addBoolean("Bed burst", "Shower of shapes when a bed breaks", true);
    private final Setting burstCount = addNumber("Burst count", "Shapes per burst", 20, 5, 40, 0)
            .visibleWhen(bedBurst::bool);
    private final Setting burstSize = addNumber("Burst size", "Scale of each shape", 0.20f, 0.05f, 0.60f, 2)
            .visibleWhen(bedBurst::bool);
    private final Setting burstSpeed = addNumber("Burst speed", "How hard they are thrown", 2.5f, 0.5f, 7, 1)
            .visibleWhen(bedBurst::bool);
    private final Setting burstLife = addNumber("Burst lifetime", "Milliseconds a shape lasts", 1500, 500, 3000, 0)
            .visibleWhen(bedBurst::bool);
    private final Setting rainbow = addBoolean("Arc", "Banded arch over the bed that was broken", true);
    private final Setting rainbowWidth = addNumber("Arc width", "Band thickness in pixels", 5, 1, 12, 1)
            .visibleWhen(rainbow::bool);
    private final Setting rainbowLife = addNumber("Arc lifetime", "Milliseconds the arc lasts", 3000, 1000, 6000, 0)
            .visibleWhen(rainbow::bool);
    private final Setting bedSound = addBoolean("Bed sound", "Play a chime with the burst", true);

    private final Rng random = new Rng(0xC0FFEE11);
    private final List<Heart> heartPool = new ArrayList<>();
    private final List<Dot> dotPool = new ArrayList<>();
    private final List<Spark> sparkPool = new ArrayList<>();
    private final List<Rainbow> rainbowPool = new ArrayList<>();

    private final Timer heartTimer = new Timer();
    private final Timer dotTimer = new Timer();
    private Vec3 lastPosition;

    private boolean watchingBed;
    private int bedX;
    private int bedY;
    private int bedZ;

    public CuteVisuals() {
        super("CuteVisuals", "Heart and dot trail, and a burst when a bed goes", Category.VISUALS, Subcategory.BASIC);
        setIcon(Icon.FLAME);
    }

    private static Vec2[] heartShape(int segments) {
        Vec2[] shape = new Vec2[segments + 1];
        for (int index = 0; index <= segments; ++index) {
            double t = (double) index / segments * TWO_PI;
            double sine = Math.sin(t);
            shape[index] = new Vec2((float) (16.0 * sine * sine * sine),
                    (float) -(13.0 * Math.cos(t) - 5.0 * Math.cos(2.0 * t) - 2.0 * Math.cos(3.0 * t)
                            - Math.cos(4.0 * t)));
        }
        return shape;
    }

    private static Vec2[] starShape() {
        Vec2[] shape = new Vec2[9];
        for (int index = 0; index <= 8; ++index) {
            double angle = index * Math.PI / 4.0 - Math.PI / 2.0;
            double radius = index % 2 == 0 ? 12.0 : 5.0;
            shape[index] = new Vec2((float) (Math.cos(angle) * radius), (float) (Math.sin(angle) * radius));
        }
        return shape;
    }

    private static float envelope(float progress, float fadeIn, float holdUntil) {
        if (progress < fadeIn) {
            return progress / fadeIn;
        }
        if (progress > holdUntil) {
            return (1.0f - progress) / (1.0f - holdUntil);
        }
        return 1.0f;
    }

    private static float bandWeight(int band) {
        return Maths.lerp(1.5f, 0.55f, band / 6.0f);
    }

    private static void strokeShape(Vec2[] points, float x, float y, float scale, float rotation, Color colour,
            float alpha, float width) {
        Renderer gfx = Renderer.instance();
        float cosine = (float) Math.cos(rotation);
        float sine = (float) Math.sin(rotation);

        for (int layer = 2; layer >= 0; --layer) {
            float layerScale = scale * (1.0f + layer * 0.1f);
            float layerAlpha = layer == 0 ? alpha * 0.9f : alpha * (0.28f / layer);
            if (layerAlpha < 0.01f) {
                continue;
            }
            Color tint = colour.withAlpha(layerAlpha);
            float previousX = 0.0f;
            float previousY = 0.0f;
            for (int index = 0; index < points.length; ++index) {
                float sx = points[index].x() * layerScale;
                float sy = points[index].y() * layerScale;
                float px = x + sx * cosine - sy * sine;
                float py = y + sx * sine + sy * cosine;
                if (index > 0) {
                    gfx.line(previousX, previousY, px, py, width + layer, tint);
                }
                previousX = px;
                previousY = py;
            }
        }
    }

    private static void advance(List<? extends Particle> pool, float delta, float lifetime) {
        for (Particle particle : pool) {
            particle.age += delta;
        }
        pool.removeIf(particle -> particle.age >= lifetime);
    }

    private void clear() {
        heartPool.clear();
        dotPool.clear();
        sparkPool.clear();
        rainbowPool.clear();
        watchingBed = false;
    }

    @Override
    public void onEnable(Context context) {
        clear();
        lastPosition = null;
    }

    @Override
    public void onDisable(Context context) {
        clear();
    }

    private static Billboard billboard(Context context, Vec3 world) {
        ScreenPoint centre = context.projection.project(world);
        if (centre == null) {
            return null;
        }

        double yaw = Math.toRadians(context.state.camera.yaw);
        ScreenPoint edge = context.projection.project(world.add(Math.cos(yaw), 0.0, Math.sin(yaw)));
        if (edge == null) {
            return null;
        }
        float pixels = (float) Math.hypot(edge.x() - centre.x(), edge.y() - centre.y());
        return pixels > 0.05f ? new Billboard(centre.x(), centre.y(), pixels) : null;
    }

    private void watchBed(FrameState state) {
        if (!bedBurst.bool() && !rainbow.bool() && !bedSound.bool()) {
            watchingBed = false;
            return;
        }

        if (watchingBed && !Game.blockIs(bedX, bedY, bedZ, BlockKind.BED)) {
            watchingBed = false;
            burst(state, new Vec3(bedX + 0.5, bedY + 0.5, bedZ + 0.5));
            return;
        }

        if (Input.attackHeld() && !state.screenOpen) {
            CrosshairTarget target = Game.crosshair();
            if (target.kind() == CrosshairTarget.Kind.BLOCK
                    && Game.blockIs(target.x(), target.y(), target.z(), BlockKind.BED)) {
                watchingBed = true;
                bedX = target.x();
                bedY = target.y();
                bedZ = target.z();
            }
        }
    }

    private void burst(FrameState state, Vec3 centre) {
        if (rainbow.bool() && rainbowPool.size() < MAX_RAINBOWS) {
            Rainbow arc = new Rainbow();
            arc.position = centre;

            arc.yaw = (float) Math.toDegrees(Math.atan2(state.player.pos.x() - centre.x(),
                    state.player.pos.z() - centre.z()));
            rainbowPool.add(arc);
        }

        if (bedBurst.bool()) {
            int count = (int) burstCount.number();
            for (int index = 0; index < count && sparkPool.size() < MAX_SPARKS; ++index) {
                Spark spark = new Spark();
                spark.position = centre;
                double theta = random.unit() * TWO_PI;
                double phi = random.unit() * Math.PI * 0.67 - Math.PI / 6.0;
                double speed = (0.8 + random.unit() * 1.2) * burstSpeed.number();
                double cosPhi = Math.cos(phi);
                spark.velocity = new Vec3(cosPhi * Math.cos(theta) * speed, Math.sin(phi) * speed + 1.0,
                        cosPhi * Math.sin(theta) * speed);
                int roll = random.range(0, 4);
                spark.type = roll < 2 ? 0 : roll - 1;
                spark.scale = burstSize.number() * (0.6f + random.unit() * 0.8f);
                spark.seed = sparkPool.size();
                sparkPool.add(spark);
            }
        }

        if (bedSound.bool()) {
            Game.playSound("random.orb", 1.0f, 1.5f);
            Game.playSound("random.levelup", 0.5f, 2.0f);
        }
    }

    @Override
    public void onTick(Context context) {
        FrameState state = context.state;
        if (!state.inWorld || !state.player.valid) {
            watchingBed = false;
            return;
        }

        watchBed(state);

        Vec3 position = state.player.pos;
        Vec3 previous = lastPosition;
        lastPosition = position;
        if (previous == null) {
            return;
        }
        if (onlyMoving.bool() && position.sub(previous).horizontalLength() < 0.01) {
            return;
        }

        if (hearts.bool() && heartPool.size() < MAX_HEARTS && heartTimer.tryReset((long) heartRate.number())) {
            Heart heart = new Heart();
            double angle = random.unit() * TWO_PI;
            double distance = random.unit() * HEART_RADIUS;
            heart.position = new Vec3(position.x() + Math.cos(angle) * distance,
                    position.y() + 0.5 + random.unit() * 0.5, position.z() + Math.sin(angle) * distance);
            heart.tilt = random.range(-15.0f, 15.0f);
            heart.scale = 0.15f * (0.6f + random.unit() * 0.8f);
            heart.type = random.range(0, 2);
            heart.seed = random.range(0, 255);
            heartPool.add(heart);
        }

        if (dots.bool() && dotPool.size() < MAX_DOTS && dotTimer.tryReset((long) dotRate.number())) {
            Dot dot = new Dot();
            dot.position = new Vec3(previous.x() + (random.unit() - 0.5f) * DOT_SPREAD,
                    position.y() + 0.3 + random.unit() * 1.2, previous.z() + (random.unit() - 0.5f) * DOT_SPREAD);
            dot.drift = new Vec3((random.unit() - 0.5f) * DOT_DRIFT, (0.3 + random.unit() * 0.7) * DOT_DRIFT,
                    (random.unit() - 0.5f) * DOT_DRIFT);
            dot.scale = 0.04f * (0.5f + random.unit());
            dot.type = random.range(0, 3);
            dot.seed = random.range(0, 255);
            dotPool.add(dot);
        }
    }

    private void drawHearts(Context context, float fade) {
        float lifetime = heartLife.number() / 1000.0f;
        for (Heart heart : heartPool) {
            float progress = heart.age / lifetime;
            float alpha = envelope(progress, 0.1f, 0.6f) * fade;
            float scaleFade = envelope(progress, 0.1f, 0.8f);
            if (alpha < 0.02f || scaleFade <= 0.0f) {
                continue;
            }
            Vec3 world = new Vec3(heart.position.x() + Math.sin(heart.age * 2.0f + heart.seed * 1.7) * 0.1,
                    heart.position.y() + 1.5 * progress,
                    heart.position.z() + Math.cos(heart.age * 1.5f + heart.seed * 2.3) * 0.1);

            Billboard frame = billboard(context, world);
            if (frame == null) {
                continue;
            }

            float scale = Math.min(heart.scale * scaleFade * frame.pixelsPerBlock(), MAX_PARTICLE_PIXELS * 2.0f)
                    / 16.0f;
            float rotation = (heart.tilt + heart.age * 20.0f) * Maths.DEG_TO_RAD;
            strokeShape(HEART, frame.x(), frame.y(), scale, rotation, Theme.accentShade(HEART_WEIGHTS[heart.type]),
                    alpha, 1.2f);
        }
    }

    private void drawDots(Context context, float fade) {
        float lifetime = dotLife.number() / 1000.0f;
        boolean flicker = pulse.bool();
        Renderer gfx = Renderer.instance();

        for (Dot dot : dotPool) {
            float progress = dot.age / lifetime;
            float alpha = envelope(progress, 0.1f, 0.5f) * fade;
            if (flicker) {
                float speed = 3.0f + dot.type * 1.5f;
                alpha *= 0.5f + 0.5f * (float) Math.sin(dot.age * speed + dot.seed * 2.7f);
            }
            if (alpha < 0.02f) {
                continue;
            }
            Vec3 world = new Vec3(
                    dot.position.x() + dot.drift.x() * dot.age + Math.sin(dot.age * 1.5f + dot.seed * 1.3) * 0.15,
                    dot.position.y() + dot.drift.y() * dot.age,
                    dot.position.z() + dot.drift.z() * dot.age + Math.cos(dot.age * 1.2f + dot.seed * 2.1) * 0.15);

            Billboard frame = billboard(context, world);
            if (frame == null) {
                continue;
            }
            float radius = Math.min(dot.scale * frame.pixelsPerBlock(), MAX_PARTICLE_PIXELS);
            if (radius < 0.3f) {
                continue;
            }
            Color colour = Theme.accentShade(DOT_WEIGHTS[dot.type]);
            Rect disc = new Rect(frame.x() - radius, frame.y() - radius, radius * 2.0f, radius * 2.0f);
            gfx.glow(disc, colour.withAlpha(0.5f * alpha), radius * 1.6f, radius);
            gfx.rect(disc, colour.withAlpha(alpha), radius);
        }
    }

    private void drawSparks(Context context) {
        float lifetime = burstLife.number() / 1000.0f;
        Renderer gfx = Renderer.instance();

        for (Spark spark : sparkPool) {
            float progress = spark.age / lifetime;
            float alpha = envelope(progress, 0.1f, 0.7f);
            if (alpha < 0.02f) {
                continue;
            }

            double seconds = spark.age;
            Vec3 world = new Vec3(
                    spark.position.x() + spark.velocity.x() * seconds
                            + Math.sin(spark.age * 2.0f + spark.seed * 1.7) * 0.05,
                    spark.position.y() + spark.velocity.y() * seconds - 1.5 * seconds * seconds,
                    spark.position.z() + spark.velocity.z() * seconds
                            + Math.cos(spark.age * 1.5f + spark.seed * 2.3) * 0.05);

            Billboard frame = billboard(context, world);
            if (frame == null) {
                continue;
            }

            Color colour = Theme.accentShade(bandWeight(spark.seed % 7));
            float rotation = (spark.age * 40.0f + spark.seed * 60.0f) * Maths.DEG_TO_RAD;
            float size = Math.min(spark.scale * alpha * frame.pixelsPerBlock(), MAX_PARTICLE_PIXELS * 2.0f);

            switch (spark.type) {
                case 0 -> strokeShape(HEART, frame.x(), frame.y(), size / 16.0f, rotation, colour, alpha, 1.2f);
                case 1 -> strokeShape(STAR, frame.x(), frame.y(), size / 16.0f, rotation, colour, alpha, 1.2f);
                case 2 -> {
                    float radius = Math.max(size * 0.5f, 0.4f);
                    Rect disc = new Rect(frame.x() - radius, frame.y() - radius, radius * 2.0f, radius * 2.0f);
                    gfx.glow(disc, colour.withAlpha(0.45f * alpha), radius * 1.8f, radius);
                    gfx.rect(disc, colour.withAlpha(alpha), radius);
                }
                default -> strokeShape(DIAMOND, frame.x(), frame.y(), size / 16.0f, rotation, colour, alpha, 1.2f);
            }
        }
    }

    private void drawRainbows(Context context) {
        float lifetime = rainbowLife.number() / 1000.0f;
        float width = rainbowWidth.number();
        Renderer gfx = Renderer.instance();

        for (Rainbow arc : rainbowPool) {
            float progress = arc.age / lifetime;
            float alpha = envelope(progress, 0.15f, 0.6f);
            if (alpha < 0.02f) {
                continue;
            }

            float sweep = progress < 0.2f ? (progress / 0.2f) * (progress / 0.2f) : 1.0f;

            double yaw = Math.toRadians(arc.yaw);
            double rightX = Math.cos(yaw);
            double rightZ = -Math.sin(yaw);

            for (int band = 0; band < 7; ++band) {
                double radius = 3.0 + (band - 3) * 0.15;
                Color colour = Theme.accentShade(bandWeight(band));
                ScreenPoint previous = null;
                for (int segment = 0; segment <= RAINBOW_SEGMENTS; ++segment) {
                    double angle = (double) segment / RAINBOW_SEGMENTS * Math.PI * sweep;
                    ScreenPoint point = context.projection.project(new Vec3(
                            arc.position.x() + rightX * Math.cos(angle) * radius,
                            arc.position.y() + Math.sin(angle) * radius,
                            arc.position.z() + rightZ * Math.cos(angle) * radius));
                    if (point != null && previous != null) {
                        gfx.line(previous.x(), previous.y(), point.x(), point.y(), width + 2.0f,
                                colour.withAlpha(0.2f * alpha));
                        gfx.line(previous.x(), previous.y(), point.x(), point.y(), width,
                                colour.withAlpha(0.85f * alpha));
                    }
                    previous = point;
                }
            }
        }
    }

    @Override
    public void onRender(Context context) {
        float delta = context.deltaTime;
        advance(heartPool, delta, heartLife.number() / 1000.0f);
        advance(dotPool, delta, dotLife.number() / 1000.0f);
        advance(sparkPool, delta, burstLife.number() / 1000.0f);
        advance(rainbowPool, delta, rainbowLife.number() / 1000.0f);

        if (!context.projection.valid()) {
            return;
        }
        float fade = opacity.number() / 100.0f * animation();
        if (hearts.bool()) {
            drawHearts(context, fade);
        }
        if (dots.bool()) {
            drawDots(context, fade);
        }
        drawSparks(context);
        drawRainbows(context);
    }
}
