package stone.ui;

import stone.core.Color;
import stone.core.Config;
import stone.core.Ease;
import stone.core.Maths;
import stone.core.Rect;
import stone.hook.Input;
import stone.module.Context;
import stone.module.Module;
import stone.render.FontRole;
import stone.render.Renderer;
import java.util.ArrayList;
import java.util.List;

public final class Hud {
    private static final float NOTICE_WIDTH = 180.0f;
    private static final float NOTICE_HEIGHT = 28.0f;
    private static final float NOTICE_ROUNDING = 6.0f;
    private static final float NOTICE_PADDING = 8.0f;
    private static final float NOTICE_TITLE_SIZE = 9.0f;
    private static final float NOTICE_MESSAGE_SIZE = 8.0f;
    private static final float NOTICE_MARGIN = 8.0f;
    private static final float NOTICE_SPACING = 4.0f;
    private static final float NOTICE_ENTER = 0.22f;
    private static final float NOTICE_EXIT = 0.16f;
    private static final float NOTICE_GLIDE = 22.0f;

    private static final Color NOTICE_BG = Color.rgb(0x171A21, 0.94f);
    private static final Color NOTICE_BORDER = Color.rgb(0xFFFFFF, 0.08f);
    private static final Color NOTICE_SHADOW = Color.rgb(0x000000, 0.40f);
    private static final Color NOTICE_TITLE = Color.rgb(0xFFFFFF);
    private static final Color NOTICE_MESSAGE = Color.rgb(0xFFFFFF, 0.65f);

    private static final class Notification {
        final String title;
        final String message;
        final Color colour;
        final float total;
        float life;
        float y;
        boolean placed;

        Notification(String title, String message, Color colour, float duration) {
            this.title = title;
            this.message = message;
            this.colour = colour;
            this.total = duration;
            this.life = duration;
        }
    }

    private final Renderer gfx;
    private final List<Notification> notices = new ArrayList<>();
    private float unit = 2.0f;
    private float scale = 1.0f;
    private boolean showNotifications = true;

    public Hud() {
        this(Renderer.instance());
    }

    public Hud(Renderer gfx) {
        this.gfx = gfx;
    }

    private float px(float units) {
        return Math.round(units * unit);
    }

    public void notify(String title, String message, Color colour, float duration) {
        notices.add(new Notification(title, message, colour, Math.max(0.5f, duration)));
    }

    public void moduleToggled(Module module) {
    }

    public void forget(Module module) {
    }

    public void loadLayout(Config config) {
    }

    public void saveLayout(Config config) {
    }

    public boolean dragging() {
        return false;
    }

    public int takeToggle() {
        return -1;
    }

    public void setMenuOpen(boolean value) {
    }

    public void setScale(float value) {
        this.scale = Maths.clamp(value, 0.6f, 2.5f);
    }

    public void setShowWatermark(boolean value) {
    }

    public void setShowArrayList(boolean value) {
    }

    public void setShowPlayerInfo(boolean value) {
    }

    public void setShowTargetInfo(boolean value) {
    }

    public void setShowNotifications(boolean value) {
        this.showNotifications = value;
    }

    public void setShowSpeed(boolean value) {
    }

    public void handleEvent(Input.Event event, Context context) {
    }

    private void drawNotifications(Context context) {
        if (notices.isEmpty() || !showNotifications) {
            return;
        }
        float width = px(NOTICE_WIDTH);
        float height = px(NOTICE_HEIGHT);
        float rounding = px(NOTICE_ROUNDING);
        float padding = px(NOTICE_PADDING);
        float titleSize = px(NOTICE_TITLE_SIZE) / gfx.fonts().face(FontRole.BOLD).boxHeight(1.0f);
        float messageSize = px(NOTICE_MESSAGE_SIZE) / gfx.fonts().face(FontRole.BOLD).boxHeight(1.0f);

        float travel = width + px(NOTICE_MARGIN);
        int slot = 0;
        for (int index = notices.size() - 1; index >= 0; --index) {
            Notification notice = notices.get(index);
            notice.life -= context.deltaTime;
            if (notice.life <= 0.0f) {
                continue;
            }

            float elapsed = notice.total - notice.life;
            float slide = Ease.outBack(Ease.window(elapsed, 0.0f, NOTICE_ENTER), 0.6f);
            float alpha = Ease.outCubic(Ease.window(elapsed, 0.0f, 0.18f));
            if (notice.life < NOTICE_EXIT) {
                float leaving = 1.0f - notice.life / NOTICE_EXIT;
                slide -= leaving * leaving * leaving;
                alpha *= 1.0f - leaving * leaving;
            }

            float target = context.state.windowHeight - px(NOTICE_MARGIN) - height
                    - slot * (height + px(NOTICE_SPACING));
            notice.y = notice.placed ? Maths.approach(notice.y, target, NOTICE_GLIDE, context.deltaTime) : target;
            notice.placed = true;

            float x = context.state.windowWidth - px(NOTICE_MARGIN) - width + (1.0f - slide) * travel;
            float y = notice.y;
            Rect panel = new Rect(x, y, width, height);
            Color accent = notice.colour.withAlpha(alpha);

            gfx.rect(new Rect(x + px(1.0f), y + px(2.0f), width, height), NOTICE_SHADOW.withAlpha(alpha), rounding);
            gfx.rect(panel, NOTICE_BG.withAlpha(alpha), rounding);
            gfx.outline(panel, NOTICE_BORDER.withAlpha(alpha), Math.max(1.0f, px(0.5f)), rounding);

            float textX = x + padding;
            gfx.text(FontRole.BOLD, notice.title, textX, y + px(5.0f), titleSize, NOTICE_TITLE.withAlpha(alpha));
            gfx.text(FontRole.BOLD, gfx.elide(FontRole.BOLD, notice.message, messageSize, panel.right() - padding - textX),
                    textX, y + px(15.5f), messageSize, NOTICE_MESSAGE.withAlpha(alpha));

            float barHeight = px(1.0f);
            gfx.rect(new Rect(x, panel.bottom() - barHeight, width * (notice.life / notice.total), barHeight), accent,
                    barHeight * 0.5f);
            ++slot;
        }

        notices.removeIf(notice -> notice.life <= 0.0f);
    }

    public void render(Context context) {
        float factor = context.state.scaledWidth > 0 ? (float) context.state.windowWidth / context.state.scaledWidth : 2.0f;
        unit = Math.max(factor * scale, 0.5f);
        drawNotifications(context);
    }
}
