package stone.module.render;

import stone.core.Color;
import stone.core.Maths;
import stone.core.Rect;
import stone.core.Vec3;
import stone.game.EntityView;
import stone.game.FrameState;
import stone.game.Game;
import stone.game.Projection.ScreenPoint;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.render.Align;
import stone.render.FontRole;
import stone.render.Icon;
import stone.render.Renderer;
import stone.ui.Theme;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Esp extends Module {
    private static final float FADE_RATE = 12.0f;

    private static final int[][] EDGES = {
            {0, 1}, {2, 3}, {4, 5}, {6, 7},
            {0, 2}, {1, 3}, {4, 6}, {5, 7},
            {0, 4}, {1, 5}, {2, 6}, {3, 7}};
    private static final int[][] FACES = {
            {0, 1, 3, 2}, {4, 5, 7, 6},
            {0, 1, 5, 4}, {2, 3, 7, 6},
            {0, 2, 6, 4}, {1, 3, 7, 5}};
    private static final float NAME_SIZE = 8.5f;

    private static final class Entry {
        float fade;
        Rect bounds = Rect.EMPTY;
        float health;
        float maxHealth;
        double distance;
        boolean seen;
        String name = "";

        Vec3 boxMin = Vec3.ZERO;
        Vec3 boxMax = Vec3.ZERO;

        Vec3 centre() {
            return new Vec3((boxMin.x() + boxMax.x()) * 0.5, (boxMin.y() + boxMax.y()) * 0.5,
                    (boxMin.z() + boxMax.z()) * 0.5);
        }
    }

    private final Setting mode = addChoice("Mode", "Outline style", List.of("Box", "Corners", "Glow", "None"), 0);
    private final Setting players = addBoolean("Players", "Highlight players", true);
    private final Setting mobs = addBoolean("Mobs", "Highlight mobs", false);
    private final Setting invisibles = addBoolean("Invisibles", "Also highlight invisible entities", false);
    private final Setting range = addNumber("Range", "Maximum distance to highlight", 64, 8, 256, 0);
    private final Setting colourMode = addChoice("Colour", "The client accent, or how much health is left",
            List.of("Accent", "Health"), 0);
    private final Setting thickness = addNumber("Thickness", "Outline width in pixels", 1.2f, 0.5f, 5, 1);
    private final Setting glowStrength = addNumber("Glow", "Halo intensity around the outline", 0.45f, 0, 1, 2);
    private final Setting fill = addNumber("Fill", "Opacity of the box interior", 0.10f, 0, 0.5f, 2);
    private final Setting healthBar = addBoolean("Health bar", "Vertical health meter beside the box", true);
    private final Setting nameLabel = addBoolean("Name", "Print the name above the box", false);
    private final Setting distanceLabel = addBoolean("Distance", "Print the distance under the box", false);
    private final Setting smoothing = addNumber("Smoothing", "Softens box movement; 0 tracks the entity exactly",
            0, 0, 1, 2);

    private final Setting box3d = addBoolean("3D box", "Wireframe hitbox drawn in the world", false);
    private final Setting thickness3d = addNumber("3D thickness", "Wireframe width in pixels", 1, 0.5f, 5, 1)
            .visibleWhen(box3d::bool);
    private final Setting fill3d = addBoolean("3D fill", "Fill the six faces of the wireframe", false)
            .visibleWhen(box3d::bool);

    private final Setting tracers = addBoolean("Tracers", "Line from you to every target", false);
    private final Setting tracerFrom = addChoice("Tracer origin", "Where the line starts on screen",
            List.of("Crosshair", "Bottom"), 0).visibleWhen(tracers::bool);
    private final Setting tracerThickness = addNumber("Tracer thickness", "Line width in pixels", 1, 0.5f, 5, 1)
            .visibleWhen(tracers::bool);

    private final Setting arrows = addBoolean("Arrows", "Ring of pointers to targets that are off screen", false);
    private final Setting arrowStyle = addChoice("Arrow style", "Shape of the pointer",
            List.of("Classic", "Chevron"), 0).visibleWhen(arrows::bool);
    private final Setting arrowRadius = addNumber("Arrow radius", "Pixels from the crosshair", 100, 20, 300, 0)
            .visibleWhen(arrows::bool);
    private final Setting arrowSize = addNumber("Arrow size", "Scale of the pointer", 1, 0.5f, 2, 1)
            .visibleWhen(arrows::bool);
    private final Setting arrowOutline = addNumber("Arrow outline", "Width of the dark edge", 1, 0, 5, 1)
            .visibleWhen(arrows::bool);

    private final Setting arrowFade = addNumber("Arrow fade", "Blocks over which the pointer dims", 30, 5, 120, 0)
            .visibleWhen(arrows::bool);

    private final Map<Integer, Entry> entries = new HashMap<>();

    public Esp() {
        super("ESP", "Outlines, tracers and off-screen arrows for entities", Category.VISUALS, Subcategory.ADVANCED);
        setIcon(Icon.EYE);
    }

    @Override
    public String suffix() {
        if (mode.choice() == 3) {
            return box3d.bool() ? "3D" : "";
        }
        return mode.choiceName();
    }

    private Color colourFor(Entry entry) {

        return colourMode.choice() == 1 ? Theme.healthColour(entry.health, entry.maxHealth) : Theme.accent();
    }

    private void drawBox(Rect bounds, Color colour, float fade) {
        Renderer gfx = Renderer.instance();
        float radius = Math.min(3.0f, Math.min(bounds.width(), bounds.height()) * 0.2f);
        float glow = glowStrength.number();
        float width = thickness.number();

        if (glow > 0.0f) {
            gfx.glow(bounds, colour.withAlpha(0.34f * glow * fade), 16.0f * glow + 4.0f, radius);
        }
        if (fill.number() > 0.0f) {
            gfx.rect(bounds, colour.withAlpha(fill.number() * fade), radius);
        }

        gfx.outline(bounds.inset(-0.6f), Color.rgb(0x05070A, 0.55f * fade), width, radius);
        if (mode.choice() == 2) {
            gfx.sweepOutline(bounds, colour.withAlpha(fade), width + 0.2f, radius);
        } else {
            gfx.outline(bounds, colour.withAlpha(fade), width, radius);
        }
    }

    private void drawCorners(Rect bounds, Color colour, float fade) {
        Renderer gfx = Renderer.instance();
        float armX = Math.min(bounds.width() * 0.3f, 14.0f);
        float armY = Math.min(bounds.height() * 0.3f, 14.0f);
        float stroke = thickness.number() + 0.2f;
        Color line = colour.withAlpha(fade);
        Color backing = Color.rgb(0x05070A, 0.5f * fade);

        float left = bounds.x();
        float right = bounds.right();
        float top = bounds.y();
        float bottom = bounds.bottom();

        float[][] brackets = {
                {left, top, left + armX, top}, {left, top, left, top + armY},
                {right - armX, top, right, top}, {right, top, right, top + armY},
                {left, bottom - armY, left, bottom}, {left, bottom, left + armX, bottom},
                {right - armX, bottom, right, bottom}, {right, bottom - armY, right, bottom}};
        for (float[] bracket : brackets) {
            gfx.line(bracket[0], bracket[1] + 0.8f, bracket[2], bracket[3] + 0.8f, stroke + 1.2f, backing);
            gfx.line(bracket[0], bracket[1], bracket[2], bracket[3], stroke, line);
        }

        if (glowStrength.number() > 0.0f) {
            gfx.glow(bounds, colour.withAlpha(0.22f * glowStrength.number() * fade), 14.0f, 4.0f);
        }
    }

    private void drawBox3d(Context context, Entry entry, Color colour, float fade) {

        float[] xs = new float[8];
        float[] ys = new float[8];
        for (int index = 0; index < 8; ++index) {
            ScreenPoint point = context.projection.project(new Vec3(
                    (index & 1) != 0 ? entry.boxMax.x() : entry.boxMin.x(),
                    (index & 2) != 0 ? entry.boxMax.y() : entry.boxMin.y(),
                    (index & 4) != 0 ? entry.boxMax.z() : entry.boxMin.z()));
            if (point == null) {
                return;
            }
            xs[index] = point.x();
            ys[index] = point.y();
        }

        Renderer gfx = Renderer.instance();
        if (fill3d.bool()) {
            Color face = colour.withAlpha(0.18f * fade);
            for (int[] quad : FACES) {
                gfx.face(new float[] {xs[quad[0]], xs[quad[1]], xs[quad[2]], xs[quad[3]]},
                        new float[] {ys[quad[0]], ys[quad[1]], ys[quad[2]], ys[quad[3]]}, face);
            }
        }

        float width = thickness3d.number();
        Color backing = Color.rgb(0x05070A, 0.55f * fade);
        Color line = colour.withAlpha(fade);
        for (int[] edge : EDGES) {
            gfx.line(xs[edge[0]], ys[edge[0]] + 0.7f, xs[edge[1]], ys[edge[1]] + 0.7f, width + 1.0f, backing);
        }
        for (int[] edge : EDGES) {
            gfx.line(xs[edge[0]], ys[edge[0]], xs[edge[1]], ys[edge[1]], width, line);
        }
    }

    private void drawTracer(Context context, Entry entry, float fade) {
        ScreenPoint point = context.projection.project(entry.centre());
        if (point == null) {
            return;
        }
        float originX = context.state.windowWidth * 0.5f;
        float originY = tracerFrom.choice() == 0 ? context.state.windowHeight * 0.5f : context.state.windowHeight;

        Renderer gfx = Renderer.instance();
        float stroke = tracerThickness.number();
        gfx.line(originX, originY, point.x(), point.y() + 0.7f, stroke + 1.0f, Color.rgb(0x05070A, 0.5f * fade));
        gfx.line(originX, originY, point.x(), point.y(), stroke, Theme.accent().withAlpha(fade));
    }

    private void drawArrow(Context context, Entry entry, float fade) {
        FrameState state = context.state;
        float centreX = state.windowWidth * 0.5f;
        float centreY = state.windowHeight * 0.5f;
        float radius = arrowRadius.number();

        Vec3 centre = entry.centre();
        float dirX;
        float dirY;
        ScreenPoint point = context.projection.project(centre);
        if (point != null) {
            dirX = point.x() - centreX;
            dirY = point.y() - centreY;

            if (dirX * dirX + dirY * dirY < radius * radius) {
                return;
            }
        } else {

            double dx = centre.x() - state.camera.pos.x();
            double dz = centre.z() - state.camera.pos.z();
            float bearing = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
            double radians = Math.toRadians(Maths.angleDifference(state.camera.yaw, bearing));
            dirX = (float) Math.sin(radians);
            dirY = -(float) Math.cos(radians);
        }

        float length = (float) Math.sqrt(dirX * dirX + dirY * dirY);
        dirX /= length;
        dirY /= length;

        float away = Maths.clamp((float) entry.distance / arrowFade.number(), 0.0f, 1.0f);
        Color colour = Theme.accentShade(Maths.lerp(1.35f, 0.6f, away));

        float[][] shape = arrowStyle.choice() == 1
                ? new float[][] {{10.0f, 0.0f}, {-4.0f, 7.0f}, {-1.0f, 0.0f}, {-4.0f, -7.0f}}
                : new float[][] {{10.0f, 0.0f}, {-6.0f, 6.0f}, {-6.0f, -6.0f}};
        float scale = arrowSize.number();
        float baseX = centreX + dirX * radius;
        float baseY = centreY + dirY * radius;
        int corners = shape.length;
        float[] px = new float[corners];
        float[] py = new float[corners];
        for (int index = 0; index < corners; ++index) {
            float along = shape[index][0];
            float across = shape[index][1];
            px[index] = baseX + (dirX * along - dirY * across) * scale;
            py[index] = baseY + (dirY * along + dirX * across) * scale;
        }

        Renderer gfx = Renderer.instance();

        if (corners == 3) {
            gfx.face(new float[] {px[0], px[1], px[2], px[0]}, new float[] {py[0], py[1], py[2], py[0]},
                    colour.withAlpha(fade));
        } else {
            gfx.face(px, py, colour.withAlpha(fade));
        }
        float outline = arrowOutline.number();
        if (outline > 0.0f) {
            Color edge = Color.rgb(0x05070A, 0.95f * fade);
            for (int index = 0; index < corners; ++index) {
                int next = (index + 1) % corners;
                gfx.line(px[index], py[index], px[next], py[next], outline, edge);
            }
        }
    }

    private static void label(Rect box, String text, float y, float size, Color tint, float fade) {
        Renderer gfx = Renderer.instance();
        gfx.text(FontRole.MEDIUM, text, box.centerX() + 0.7f, y + 0.7f, size, Color.rgb(0x05070A, 0.75f * fade),
                Align.CENTER);
        gfx.text(FontRole.MEDIUM, text, box.centerX(), y, size, tint, Align.CENTER);
    }

    @Override
    public void onRender(Context context) {
        if (!context.projection.valid()) {

            entries.clear();
            return;
        }
        Renderer gfx = Renderer.instance();
        float step = 1.0f - (float) Math.exp(-FADE_RATE * context.deltaTime);

        for (Entry entry : entries.values()) {
            entry.seen = false;
        }

        for (EntityView entity : context.state.entities) {
            if (entity.isPlayer ? !players.bool() : !mobs.bool()) {
                continue;
            }
            if (entity.invisible && !invisibles.bool()) {
                continue;
            }
            if (entity.distance > range.number()) {
                continue;
            }

            Rect bounds = context.projection.projectBox(entity.boxMin(), entity.boxMax());

            Entry entry = entries.computeIfAbsent(entity.id, id -> new Entry());
            entry.seen = true;
            entry.health = entity.health + entity.absorption;
            entry.maxHealth = entity.maxHealth;
            entry.distance = entity.distance;

            entry.boxMin = entity.boxMin().add(-0.1, -0.1, -0.1);
            entry.boxMax = entity.boxMax().add(0.1, 0.1, 0.1);
            if (nameLabel.bool()) {
                entry.name = Game.entityName(entity.handle);
            }

            if (bounds != null) {
                if (entry.fade <= 0.001f || smoothing.number() <= 0.0f) {
                    entry.bounds = bounds;
                } else {
                    float rate = Maths.lerp(45.0f, 9.0f, smoothing.number());
                    float follow = 1.0f - (float) Math.exp(-rate * context.deltaTime);
                    Rect from = entry.bounds;
                    entry.bounds = new Rect(from.x() + (bounds.x() - from.x()) * follow,
                            from.y() + (bounds.y() - from.y()) * follow,
                            from.width() + (bounds.width() - from.width()) * follow,
                            from.height() + (bounds.height() - from.height()) * follow);
                }
            } else {

                Rect from = entry.bounds;
                entry.bounds = new Rect(from.x(), from.y(), 0.0f, from.height());
            }
            entry.fade += (1.0f - entry.fade) * step;
        }

        for (Iterator<Entry> iterator = entries.values().iterator(); iterator.hasNext();) {
            Entry entry = iterator.next();
            if (!entry.seen) {
                entry.fade -= entry.fade * step;
                if (entry.fade < 0.01f) {
                    iterator.remove();
                    continue;
                }
            }

            Color colour = colourFor(entry);
            float fade = entry.fade * animation();

            if (box3d.bool()) {
                drawBox3d(context, entry, colour, fade);
            }
            if (tracers.bool()) {
                drawTracer(context, entry, fade);
            }
            if (arrows.bool()) {
                drawArrow(context, entry, fade);
            }

            Rect box = entry.bounds;
            if (box.width() <= 0.5f || box.height() <= 0.5f) {
                continue;
            }

            switch (mode.choice()) {
                case 1 -> drawCorners(box, colour, fade);
                case 3 -> { }
                default -> drawBox(box, colour, fade);
            }

            if (healthBar.bool() && entry.maxHealth > 0.0f) {
                float fraction = Maths.clamp(entry.health / entry.maxHealth, 0.0f, 1.0f);
                Rect track = new Rect(box.x() - 4.5f, box.y(), 2.0f, box.height());
                gfx.rect(track.inset(-0.6f), Color.rgb(0x05070A, 0.6f * fade), 1.4f);
                gfx.rect(track, Color.rgb(0x1A1F27, 0.85f * fade), 1.0f);
                float filled = track.height() * fraction;
                gfx.rect(new Rect(track.x(), track.bottom() - filled, track.width(), filled),
                        Theme.healthColour(entry.health, entry.maxHealth).withAlpha(fade), 1.0f);
            }

            if (nameLabel.bool() && !entry.name.isEmpty()) {
                label(box, entry.name, box.y() - NAME_SIZE - 2.5f, NAME_SIZE, Theme.TEXT_BRIGHT.withAlpha(fade), fade);
            }
            if (distanceLabel.bool()) {
                label(box, String.format(Locale.ROOT, "%.1fm", entry.distance), box.bottom() + 2.0f, 7.5f,
                        Theme.TEXT.withAlpha(fade), fade);
            }
        }
    }
}
