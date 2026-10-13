package stone.ui;

import stone.core.Color;
import stone.core.Ease;
import stone.core.Maths;
import stone.core.Rect;
import stone.render.Backdrop;
import stone.render.Renderer;

public final class Theme {
    public static final String CLIENT_NAME = "Stone";

    public static final String CLIENT_SHORT_NAME = "Stone";

    public static final String CLIENT_VERSION = "2";

    public static final Color PANEL = Color.rgb(0x171A21);
    public static final Color PANEL_FOOT = Color.rgb(0x171A21);

    public static final Color SURFACE = Color.rgb(0x000000, 0.196f);
    public static final Color SURFACE_ACTIVE = Color.rgb(0x000000, 0.137f);
    public static final Color BORDER = Color.rgb(0xFFFFFF, 0.06f);
    public static final Color SCROLLBAR = Color.rgb(0xFFFFFF, 0.20f);

    public static final Color TEXT_BRIGHT = Color.rgb(0xFFFFFF);
    public static final Color TEXT = Color.rgb(0xFFFFFF, 0.86f);
    public static final Color TEXT_DIM = Color.rgb(0xFFFFFF, 0.51f);
    public static final Color TEXT_FAINT = Color.rgb(0xFFFFFF, 0.27f);

    public static final Color POSITIVE = Color.rgb(0x5BE59A);
    public static final Color WARNING = Color.rgb(0xFFC55C);
    public static final Color NEGATIVE = Color.rgb(0xFF5C6E);

    public static final Color SHADOW = Color.rgb(0x000000, 0.55f);

    public static final float RADIUS_PANEL = 12.0f;
    public static final float RADIUS_CARD = 6.0f;
    public static final float RADIUS_PILL = 5.0f;

    private static Color accent = Color.rgb(0xBDA4BD);
    private static Color accentSoft = Color.rgb(0xBDA4BD, 0.16f);
    private static Color accentDim = Color.rgb(0x877687);

    private Theme() {}

    public static Color accent() {
        return accent;
    }

    public static Color accentSoft() {
        return accentSoft;
    }

    public static Color accentDim() {
        return accentDim;
    }

    public static void setAccent(Color value) {
        accent = value;
        accentSoft = value.withAlpha(0.16f);

        accentDim = new Color(value.r() * 0.7f, value.g() * 0.7f, value.b() * 0.7f, 1.0f);
    }

    public static Color accentShade(float lightness, float alpha) {
        if (lightness <= 1.0f) {
            float scale = Maths.clamp(lightness, 0.0f, 1.0f);
            return new Color(accent.r() * scale, accent.g() * scale, accent.b() * scale, accent.a() * alpha);
        }

        float towards = Maths.clamp(lightness - 1.0f, 0.0f, 1.0f);
        return new Color(Maths.lerp(accent.r(), 1.0f, towards), Maths.lerp(accent.g(), 1.0f, towards),
                Maths.lerp(accent.b(), 1.0f, towards), accent.a() * alpha);
    }

    public static Color accentShade(float lightness) {
        return accentShade(lightness, 1.0f);
    }

    public static Color healthColour(float health, float maxHealth) {
        float fraction = maxHealth <= 0.0f ? 0.0f : Maths.clamp(health / maxHealth, 0.0f, 1.0f);
        if (fraction > 0.5f) {
            return Color.mix(WARNING, POSITIVE, (fraction - 0.5f) * 2.0f);
        }
        return Color.mix(NEGATIVE, WARNING, fraction * 2.0f);
    }

    public static void drawPanel(Rect bounds, float radius, float alpha, boolean useBackdrop) {
        if (alpha <= 0.005f) {
            return;
        }
        Renderer gfx = Renderer.instance();
        Backdrop backdrop = Client.backdrop();
        if (useBackdrop && backdrop.valid()) {

            float screenWidth = gfx.width();
            float screenHeight = gfx.height();
            Rect uv = new Rect(bounds.x() / screenWidth, 1.0f - bounds.y() / screenHeight,
                    bounds.width() / screenWidth, -bounds.height() / screenHeight);
            gfx.image(bounds, uv, backdrop.texture(), new Color(1.0f, 1.0f, 1.0f, alpha), radius);
            gfx.rectVertical(bounds, PANEL.withAlpha(alpha * 0.82f), PANEL_FOOT.withAlpha(alpha * 0.82f), radius);
        } else {

            gfx.rectVertical(bounds, PANEL.withAlpha(alpha), PANEL_FOOT.withAlpha(alpha), radius);
        }
    }

    public static void drawTargetMarker(Rect bounds, float fade, float time) {
        Renderer gfx = Renderer.instance();
        float eased = Ease.outBack(Maths.clamp(fade, 0.0f, 1.0f), 1.2f);
        float inset = (1.0f - eased) * 10.0f;
        Rect box = bounds.inset(-4.0f + inset);
        float radius = Math.min(6.0f, Math.min(box.width(), box.height()) * 0.25f);

        gfx.glow(box, accent.withAlpha(0.18f * fade), 20.0f, radius);

        float armX = Math.min(box.width() * 0.28f, 16.0f);
        float armY = Math.min(box.height() * 0.28f, 16.0f);
        float thickness = 1.6f;
        Color colour = accent.withAlpha(fade);
        float left = box.x();
        float right = box.right();
        float top = box.y();
        float bottom = box.bottom();

        gfx.line(left, top, left + armX, top, thickness, colour);
        gfx.line(left, top, left, top + armY, thickness, colour);
        gfx.line(right - armX, top, right, top, thickness, colour);
        gfx.line(right, top, right, top + armY, thickness, colour);
        gfx.line(left, bottom - armY, left, bottom, thickness, colour);
        gfx.line(left, bottom, left + armX, bottom, thickness, colour);
        gfx.line(right - armX, bottom, right, bottom, thickness, colour);
        gfx.line(right, bottom - armY, right, bottom, thickness, colour);

        float orbitRadius = Math.min(box.width(), box.height()) * 0.5f + 5.0f;
        gfx.ring(box.centerX(), box.centerY(), orbitRadius, 1.4f, 0.18f + 0.06f * (float) Math.sin(time * 2.1f),
                accent.withAlpha(0.5f * fade));
    }

    public static void drawMeter(Rect bounds, float fraction, Color track, Color fill, float radius) {
        Renderer gfx = Renderer.instance();
        gfx.rect(bounds, track, radius);
        float width = bounds.width() * Maths.clamp(fraction, 0.0f, 1.0f);
        if (width > 0.5f) {
            gfx.rect(new Rect(bounds.x(), bounds.y(), width, bounds.height()), fill, radius);
        }
    }
}
