package stone.ui;

import stone.core.Color;
import stone.core.Ease;
import stone.core.Maths;
import stone.core.Rect;
import stone.game.FrameState;
import stone.game.Game;
import stone.game.PlayerRadar;
import stone.hook.Input;
import stone.hook.MenuScreen;
import stone.module.Context;
import stone.render.Align;
import stone.render.FontRole;
import stone.render.Icon;
import stone.render.Renderer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Radar {

    private static final float PANEL_WIDTH = 460.0f;
    private static final float PANEL_HEIGHT = 300.0f;
    private static final float PANEL_WIDTH_SHARE = 0.52f;
    private static final float PANEL_HEIGHT_SHARE = 0.62f;
    private static final float PANEL_MINIMUM_SCALE = 1.4f;
    private static final float EDGE = 1.0f;
    private static final float DRAG_STRIP = 15.0f;
    private static final float CAP_MIDDLE = 0.3635f;

    private static final float MARGIN = 10.0f;
    private static final float TITLE_CAP_TOP = 12.0f;
    private static final float TITLE_SIZE = 14.0f;
    private static final float SUB_CAP_TOP = 28.0f;
    private static final float SUB_SIZE = 7.2f;

    private static final float SEARCH_TOP = 38.0f;
    private static final float SEARCH_HEIGHT = 16.0f;
    private static final float LIST_LEFT = 10.0f;
    private static final float LIST_WIDTH = 162.0f;
    private static final float LIST_TOP = 60.0f;
    private static final float SORT_HEIGHT = 13.0f;
    private static final float ROW_HEIGHT = 32.0f;
    private static final float ROW_GAP = 4.0f;
    private static final float HEAD_SIZE = 22.0f;

    private static final float PANE_LEFT = 182.0f;
    private static final float PANE_WIDTH = 268.0f;
    private static final float NAME_CAP_TOP = 40.0f;
    private static final float NAME_SIZE = 14.0f;

    private static final float STATS_TOP = 58.0f;
    private static final float STATS_WIDTH = 80.0f;
    private static final float STAT_HEIGHT = 23.0f;
    private static final float STAT_GAP = 2.0f;

    private static final float SKIN_LEFT = 268.0f;
    private static final float SKIN_WIDTH = 72.0f;
    private static final float SKIN_HEIGHT = 98.0f;

    private static final float ARMOUR_LEFT = 346.0f;
    private static final float ARMOUR_WIDTH = 104.0f;
    private static final float ARMOUR_ROW = 23.0f;

    private static final float HANDS_TOP = 156.0f;
    private static final float HANDS_HEIGHT = 26.0f;

    private static final float EFFECTS_TOP = 188.0f;
    private static final float EFFECTS_HEIGHT = 16.0f;

    private static final float HISTORY_TOP = 210.0f;
    private static final float HISTORY_ROW = 20.0f;

    private static final float ICON_CHIP = 13.0f;

    private static final float SKIN_TEXTURE = 64.0f;
    private static final Rect SKIN_FACE = new Rect(8.0f / 64.0f, 8.0f / 64.0f, 8.0f / 64.0f, 8.0f / 64.0f);
    private static final Rect SKIN_HAT = new Rect(40.0f / 64.0f, 8.0f / 64.0f, 8.0f / 64.0f, 8.0f / 64.0f);

    private static final String[] ARMOUR_SLOTS = {"Helmet", "Chestplate", "Leggings", "Boots"};
    private static final String[] HAND_LABELS = {"Main hand", "Off hand"};
    private static final int[] NUMERAL_VALUES = {10, 9, 5, 4, 1};
    private static final String[] NUMERALS = {"X", "IX", "V", "IV", "I"};

    private enum Sort {
        DISTANCE("Distance"), HEALTH("Health"), NAME("Name");

        final String label;

        Sort(String label) {
            this.label = label;
        }

        Sort next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private record Row(String uuid, Rect bounds, float hover, float active) {}

    private record SkinQuad(float[] xs, float[] ys, Rect uv, float depth, float shade) {}

    private record SkinBox(float x, float y, float z, float w, float h, float d, int uvX, int uvY,
                           boolean overlay) {}

    private record Face(int a, int b, int c, int d, int uvX, int uvY, int uvW, int uvH, float shade) {}

    private final Renderer gfx = Renderer.instance();

    private List<Row> rows = new ArrayList<>();
    private final List<PlayerRadar.Player> shown = new ArrayList<>();
    private String selected;
    private String query = "";

    private boolean open;
    private float alpha;
    private float appear;

    private float panelX;
    private float panelY;
    private boolean placed;
    private Rect panel = Rect.EMPTY;
    private float unit = 2.0f;

    private float scroll;
    private float scrollTarget;
    private float listHeight;
    private float historyScroll;
    private float historyTarget;
    private float historyHeight;
    private Rect listBounds = Rect.EMPTY;
    private Rect historyBounds = Rect.EMPTY;
    private Rect skinBounds = Rect.EMPTY;
    private Rect closeBounds = Rect.EMPTY;
    private Rect sortBounds = Rect.EMPTY;
    private float closeHover;
    private float sortHover;

    private Sort sort = Sort.DISTANCE;

    private float mouseX;
    private float mouseY;
    private boolean dragging;
    private boolean turning;
    private float dragOffsetX;
    private float dragOffsetY;
    private float turnFromX;
    private float turnFromY;

    private float modelYaw = 24.0f;
    private float modelPitch;
    private float grabYaw;
    private float grabPitch;
    private boolean touched;

    public void open() {
        if (open) {
            return;
        }
        open = true;
        appear = 0.0f;
        query = "";
        MenuScreen.show();
    }

    public void close() {
        if (!open) {
            return;
        }
        open = false;
        dragging = false;
        turning = false;

        if (!Client.gui().interactive()) {
            MenuScreen.hide();
        }
    }

    public boolean interactive() {
        return open;
    }

    private float px(float units) {
        return units * unit;
    }

    private void collect() {
        shown.clear();
        String needle = query.toLowerCase(Locale.ROOT);
        for (PlayerRadar.Player player : PlayerRadar.players()) {
            if (!needle.isEmpty() && !player.name.toLowerCase(Locale.ROOT).contains(needle)) {
                continue;
            }
            shown.add(player);
        }

        shown.sort((a, b) -> {

            if (a.self != b.self) {
                return a.self ? -1 : 1;
            }
            if (a.loaded != b.loaded) {
                return a.loaded ? -1 : 1;
            }
            return switch (sort) {
                case HEALTH -> Float.compare(a.health, b.health);
                case NAME -> a.name.toLowerCase(Locale.ROOT).compareTo(b.name.toLowerCase(Locale.ROOT));
                case DISTANCE -> Double.compare(a.distance, b.distance);
            };
        });

        if (selected != null && PlayerRadar.find(selected) == null) {
            selected = null;
        }
        if (selected == null && !shown.isEmpty()) {
            selected = shown.get(0).uuid;
        }
    }

    private void layout(Context context) {
        FrameState state = context.state;
        float deltaTime = context.deltaTime;

        float base = Maths.clamp(Math.min(state.windowWidth * PANEL_WIDTH_SHARE / PANEL_WIDTH,
                state.windowHeight * PANEL_HEIGHT_SHARE / PANEL_HEIGHT), PANEL_MINIMUM_SCALE, 6.0f);
        float scaledWidth = state.windowWidth / base;
        float scaledHeight = state.windowHeight / base;

        alpha = Maths.approach(alpha, open ? 1.0f : 0.0f, 18.0f, deltaTime);
        appear = Maths.approach(appear, open ? 1.0f : 0.0f, open ? 13.0f : 22.0f, deltaTime);

        if (!placed || panelX + PANEL_WIDTH > scaledWidth + 1.0f || panelY + PANEL_HEIGHT > scaledHeight + 1.0f) {
            panelX = Math.max(0.0f, (scaledWidth - PANEL_WIDTH) * 0.5f);
            panelY = Math.max(0.0f, (scaledHeight - PANEL_HEIGHT) * 0.5f);
            placed = true;
        }
        if (dragging) {
            panelX = (mouseX - dragOffsetX) / base;
            panelY = (mouseY - dragOffsetY) / base;
        }
        panelX = Maths.clamp(panelX, -PANEL_WIDTH + 90.0f, scaledWidth - 90.0f);
        panelY = Maths.clamp(panelY, 0.0f, Math.max(0.0f, scaledHeight - DRAG_STRIP));

        float grow = 0.94f + 0.06f * Ease.outQuint(Maths.clamp(appear, 0.0f, 1.0f));
        unit = base * grow;
        float centreX = (panelX + PANEL_WIDTH * 0.5f) * base;
        float centreY = (panelY + PANEL_HEIGHT * 0.5f) * base;
        panel = new Rect(centreX - px(PANEL_WIDTH) * 0.5f, centreY - px(PANEL_HEIGHT) * 0.5f,
                px(PANEL_WIDTH), px(PANEL_HEIGHT));

        listBounds = new Rect(panel.x() + px(LIST_LEFT), panel.y() + px(LIST_TOP), px(LIST_WIDTH),
                px(PANEL_HEIGHT - LIST_TOP - MARGIN - SORT_HEIGHT - 4.0f));
        historyBounds = new Rect(panel.x() + px(PANE_LEFT), panel.y() + px(HISTORY_TOP + 12.0f), px(PANE_WIDTH),
                px(PANEL_HEIGHT - HISTORY_TOP - 12.0f - MARGIN));
        skinBounds = new Rect(panel.x() + px(SKIN_LEFT), panel.y() + px(STATS_TOP), px(SKIN_WIDTH),
                px(SKIN_HEIGHT));
        closeBounds = new Rect(panel.right() - px(MARGIN + 15.0f), panel.y() + px(MARGIN), px(15.0f), px(15.0f));
        sortBounds = new Rect(panel.x() + px(LIST_LEFT), listBounds.bottom() + px(4.0f), px(LIST_WIDTH),
                px(SORT_HEIGHT));

        boolean overClose = open && closeBounds.contains(mouseX, mouseY);
        closeHover = Maths.approach(closeHover, overClose ? 1.0f : 0.0f, 18.0f, deltaTime);
        boolean overSort = open && sortBounds.contains(mouseX, mouseY);
        sortHover = Maths.approach(sortHover, overSort ? 1.0f : 0.0f, 18.0f, deltaTime);

        List<Row> next = new ArrayList<>(shown.size());
        float y = 0.0f;
        for (PlayerRadar.Player player : shown) {
            float hover = 0.0f;
            float active = 0.0f;
            for (Row old : rows) {
                if (old.uuid().equals(player.uuid)) {
                    hover = old.hover();
                    active = old.active();
                    break;
                }
            }
            Rect bounds = new Rect(listBounds.x(), listBounds.y() + px(y) + scroll, listBounds.width(),
                    px(ROW_HEIGHT));
            boolean hovered = open && listBounds.contains(mouseX, mouseY) && bounds.contains(mouseX, mouseY);
            next.add(new Row(player.uuid, bounds,
                    Maths.approach(hover, hovered ? 1.0f : 0.0f, 18.0f, deltaTime),
                    Maths.approach(active, player.uuid.equals(selected) ? 1.0f : 0.0f, 16.0f, deltaTime)));
            y += ROW_HEIGHT + ROW_GAP;
        }
        rows = next;
        listHeight = px(Math.max(0.0f, y - ROW_GAP));

        float lowest = Math.min(0.0f, listBounds.height() - listHeight);
        scrollTarget = Maths.clamp(scrollTarget, lowest, 0.0f);
        scroll = Maths.approach(scroll, scrollTarget, 18.0f, deltaTime);

        float historyLowest = Math.min(0.0f, historyBounds.height() - historyHeight);
        historyTarget = Maths.clamp(historyTarget, historyLowest, 0.0f);
        historyScroll = Maths.approach(historyScroll, historyTarget, 18.0f, deltaTime);

        if (turning) {
            modelYaw = grabYaw + (mouseX - turnFromX) / Math.max(unit, 0.1f) * 1.6f;
            modelPitch = Maths.clamp(grabPitch + (mouseY - turnFromY) / Math.max(unit, 0.1f) * 1.0f, -32.0f, 32.0f);
        } else if (!touched) {
            modelYaw += deltaTime * 16.0f;
        }
        modelYaw %= 360.0f;
    }

    public void render(Context context) {
        collect();
        layout(context);
        if (alpha <= 0.004f) {
            rows.clear();
            return;
        }

        gfx.shadow(panel, Theme.SHADOW.withAlpha(0.8f * alpha), px(18.0f), px(Theme.RADIUS_PANEL));
        Theme.drawPanel(panel, px(Theme.RADIUS_PANEL), alpha, false);

        gfx.pushClip(panel.inset(px(EDGE)));
        renderHeader(context.time);
        renderList();
        renderDetail();
        gfx.popClip();
    }

    private void renderHeader(float time) {
        gfx.textCap(FontRole.BOLD, "Radar", panel.x() + px(MARGIN + 4.0f), panel.y() + px(TITLE_CAP_TOP),
                px(TITLE_SIZE), Theme.TEXT_BRIGHT.withAlpha(alpha));
        gfx.textCap(FontRole.REGULAR, "Tracking nearby players", panel.x() + px(MARGIN + 4.0f),
                panel.y() + px(SUB_CAP_TOP), px(SUB_SIZE), Theme.TEXT_DIM.withAlpha(alpha * 0.9f));

        String count = PlayerRadar.loadedCount() + " loaded";
        float pillWidth = gfx.measure(FontRole.MEDIUM, count, px(7.6f)) + px(16.0f);
        Rect pill = new Rect(closeBounds.x() - px(6.0f) - pillWidth, panel.y() + px(MARGIN), pillWidth, px(15.0f));
        gfx.rect(pill, Theme.SURFACE.withAlpha(alpha), px(Theme.RADIUS_PILL));
        gfx.textCap(FontRole.MEDIUM, count, pill.centerX(), pill.y() + px(15.0f) * 0.5f - px(7.6f) * CAP_MIDDLE,
                px(7.6f), Theme.TEXT.withAlpha(alpha), Align.CENTER);

        gfx.rect(closeBounds, Theme.SURFACE.withAlpha(alpha * (0.7f + 0.5f * closeHover)), px(Theme.RADIUS_PILL));
        gfx.icon(Icon.X, closeBounds.inset(px(4.0f)),
                Color.mix(Theme.TEXT_DIM, Theme.TEXT_BRIGHT, closeHover).withAlpha(alpha));

        Rect search = new Rect(panel.x() + px(LIST_LEFT), panel.y() + px(SEARCH_TOP), px(LIST_WIDTH),
                px(SEARCH_HEIGHT));
        gfx.rect(search, Theme.SURFACE.withAlpha(alpha), px(Theme.RADIUS_CARD));
        gfx.icon(Icon.SEARCH, new Rect(search.x() + px(5.0f), search.centerY() - px(4.5f), px(9.0f), px(9.0f)),
                Theme.TEXT_FAINT.withAlpha(alpha));
        boolean empty = query.isEmpty();
        gfx.textCap(FontRole.REGULAR, empty ? "Search player..." : query, search.x() + px(17.0f),
                search.centerY() - px(8.2f) * CAP_MIDDLE, px(8.2f),
                (empty ? Theme.TEXT_FAINT : Theme.TEXT).withAlpha(alpha));
        if (!empty && time % 1.0f < 0.5f) {
            float caret = search.x() + px(17.0f) + gfx.measure(FontRole.REGULAR, query, px(8.2f));
            gfx.rect(new Rect(caret + px(1.0f), search.centerY() - px(4.5f), px(0.9f), px(9.0f)),
                    Theme.accent().withAlpha(alpha));
        }
    }

    private void renderList() {
        gfx.pushClip(listBounds);
        for (int index = 0; index < rows.size() && index < shown.size(); ++index) {
            Row row = rows.get(index);
            Rect bounds = row.bounds();
            PlayerRadar.Player player = shown.get(index);
            if (bounds.bottom() < listBounds.y() || bounds.y() > listBounds.bottom()) {
                continue;
            }

            float lift = 0.35f * row.hover() + row.active();
            gfx.rect(bounds, Theme.SURFACE.withAlpha(alpha * (0.75f + 0.35f * lift)), px(Theme.RADIUS_CARD));
            if (row.active() > 0.01f) {
                gfx.outline(bounds, Theme.accent().withAlpha(alpha * row.active() * 0.7f), px(0.9f),
                        px(Theme.RADIUS_CARD));
            }

            Rect head = new Rect(bounds.x() + px(5.0f), bounds.centerY() - px(HEAD_SIZE) * 0.5f, px(HEAD_SIZE),
                    px(HEAD_SIZE));
            gfx.rect(head, Theme.SURFACE_ACTIVE.withAlpha(alpha), px(2.0f));
            int skin = player.skin.texture();
            if (skin != 0) {
                Color tint = Color.rgb(0xFFFFFF, alpha * (player.loaded ? 1.0f : 0.45f));
                gfx.imageCrisp(head, SKIN_FACE, skin, tint, px(2.0f));
                gfx.imageCrisp(head, SKIN_HAT, skin, tint, px(2.0f));
            } else {
                gfx.icon(Icon.USER_ROUND, head.inset(px(5.0f)), Theme.TEXT_FAINT.withAlpha(alpha));
            }

            float textX = head.right() + px(6.0f);
            Color nameColour = (player.loaded ? Theme.TEXT_BRIGHT : Theme.TEXT_DIM).withAlpha(alpha);
            float nameWidth = bounds.right() - px(6.0f) - textX;
            gfx.textCap(FontRole.SEMI_BOLD, gfx.elide(FontRole.SEMI_BOLD, player.name, px(9.4f), nameWidth),
                    textX, bounds.y() + px(7.0f), px(9.4f), nameColour);

            int ping = Math.max(player.ping, 0);
            String detail;
            if (player.self) {
                detail = "you  " + ping + "ms";
            } else if (player.loaded) {
                detail = String.format(Locale.ROOT, "%.1fm  %dms", player.distance, ping);
            } else {
                detail = "out of range";
            }
            gfx.textCap(FontRole.REGULAR, detail, textX, bounds.y() + px(17.5f), px(7.2f),
                    Theme.TEXT_DIM.withAlpha(alpha * 0.85f));

            Rect meter = new Rect(textX, bounds.bottom() - px(7.0f), nameWidth * 0.62f, px(2.0f));
            float fraction = player.maxHealth > 0.0f ? Maths.clamp(player.health / player.maxHealth, 0.0f, 1.0f) : 0.0f;
            Color health = Theme.healthColour(player.health, player.maxHealth).withAlpha(alpha);
            Theme.drawMeter(meter, fraction, Theme.SURFACE_ACTIVE.withAlpha(alpha), health, px(1.0f));
            gfx.textCap(FontRole.MONO, String.format(Locale.ROOT, "%.1f", player.health), bounds.right() - px(6.0f),
                    bounds.bottom() - px(11.0f), px(7.0f), health, Align.RIGHT);
        }
        gfx.popClip();

        if (rows.isEmpty()) {
            gfx.textCap(FontRole.REGULAR, query.isEmpty() ? "No players in range" : "No match", listBounds.centerX(),
                    listBounds.y() + px(20.0f), px(8.0f), Theme.TEXT_FAINT.withAlpha(alpha), Align.CENTER);
        }

        if (listHeight > listBounds.height() + 1.0f) {
            float ratio = listBounds.height() / listHeight;
            float travel = listBounds.height() * (1.0f - ratio);
            float offset = -scroll / (listHeight - listBounds.height());
            gfx.rect(new Rect(listBounds.right() + px(2.0f), listBounds.y() + travel * offset, px(2.0f),
                    listBounds.height() * ratio), Theme.SCROLLBAR.withAlpha(alpha * 0.7f), px(1.0f));
        }

        gfx.rect(sortBounds, Theme.SURFACE.withAlpha(alpha * (0.7f + 0.4f * sortHover)), px(Theme.RADIUS_PILL));
        gfx.textCap(FontRole.REGULAR, "Sort by", sortBounds.x() + px(7.0f),
                sortBounds.centerY() - px(7.0f) * CAP_MIDDLE, px(7.0f), Theme.TEXT_FAINT.withAlpha(alpha));
        gfx.textCap(FontRole.MEDIUM, sort.label, sortBounds.right() - px(7.0f),
                sortBounds.centerY() - px(7.4f) * CAP_MIDDLE, px(7.4f), Theme.accent().withAlpha(alpha), Align.RIGHT);
    }

    private void renderEmpty(Rect bounds) {
        gfx.textCap(FontRole.REGULAR, "Select a player", bounds.centerX(), bounds.y() + bounds.height() * 0.45f,
                px(9.0f), Theme.TEXT_FAINT.withAlpha(alpha), Align.CENTER);
    }

    private void renderStatCard(Rect bounds, Icon icon, String label, String value, Color tint) {
        gfx.rect(bounds, Theme.SURFACE.withAlpha(alpha * 0.8f), px(Theme.RADIUS_CARD));
        gfx.icon(icon, new Rect(bounds.x() + px(5.0f), bounds.centerY() - px(4.0f), px(8.0f), px(8.0f)),
                Theme.TEXT_FAINT.withAlpha(alpha));
        gfx.textCap(FontRole.REGULAR, label, bounds.x() + px(16.0f), bounds.y() + px(5.0f), px(6.2f),
                Theme.TEXT_FAINT.withAlpha(alpha));
        gfx.textCap(FontRole.MEDIUM, gfx.elide(FontRole.MEDIUM, value, px(8.4f), bounds.width() - px(20.0f)),
                bounds.x() + px(16.0f), bounds.y() + px(13.0f), px(8.4f), tint.withAlpha(alpha));
    }

    private void renderStats(PlayerRadar.Player player, Rect bounds) {
        float step = px(STAT_HEIGHT + STAT_GAP);
        float x = bounds.x();
        float y = bounds.y();
        float width = bounds.width();
        float height = px(STAT_HEIGHT);

        renderStatCard(new Rect(x, y, width, height), Icon.ACTIVITY, "Health",
                String.format(Locale.ROOT, "%.1f / %.0f", player.health + player.absorption, player.maxHealth),
                Theme.healthColour(player.health, player.maxHealth));

        renderStatCard(new Rect(x, y + step, width, height), Icon.WIFI, "Ping",
                player.ping < 0 ? "-" : player.ping + " ms", pingColour(player.ping));

        renderStatCard(new Rect(x, y + step * 2.0f, width, height), Icon.MOVE_3D, "Distance",
                player.loaded ? String.format(Locale.ROOT, "%.1f m", player.distance) : "-", Theme.accent());

        String coords = (int) Math.floor(player.pos.x()) + ", " + (int) Math.floor(player.pos.y()) + ", "
                + (int) Math.floor(player.pos.z());
        renderStatCard(new Rect(x, y + step * 3.0f, width, height), Icon.PIN, "Coordinates", coords, Theme.TEXT);
    }

    private void renderSkin(PlayerRadar.Player player, Rect bounds) {
        gfx.rect(bounds, Theme.SURFACE.withAlpha(alpha * 0.8f), px(Theme.RADIUS_CARD));
        int texture = player.skin.texture();
        if (texture == 0) {
            gfx.icon(Icon.USER_ROUND, bounds.inset(bounds.width() * 0.32f), Theme.TEXT_FAINT.withAlpha(alpha));
            return;
        }

        float armWidth = player.skin.slim() ? 3.0f : 4.0f;
        float armOffset = 4.0f + armWidth * 0.5f;
        SkinBox[] boxes = {
            new SkinBox(0.0f, 28.0f, 0.0f, 8.0f, 8.0f, 8.0f, 0, 0, false),
            new SkinBox(0.0f, 18.0f, 0.0f, 8.0f, 12.0f, 4.0f, 16, 16, false),
            new SkinBox(-armOffset, 18.0f, 0.0f, armWidth, 12.0f, 4.0f, 40, 16, false),
            new SkinBox(armOffset, 18.0f, 0.0f, armWidth, 12.0f, 4.0f, 32, 48, false),
            new SkinBox(-2.0f, 6.0f, 0.0f, 4.0f, 12.0f, 4.0f, 0, 16, false),
            new SkinBox(2.0f, 6.0f, 0.0f, 4.0f, 12.0f, 4.0f, 16, 48, false),
            new SkinBox(0.0f, 28.0f, 0.0f, 8.0f, 8.0f, 8.0f, 32, 0, true),
            new SkinBox(0.0f, 18.0f, 0.0f, 8.0f, 12.0f, 4.0f, 16, 32, true),
            new SkinBox(-armOffset, 18.0f, 0.0f, armWidth, 12.0f, 4.0f, 40, 32, true),
            new SkinBox(armOffset, 18.0f, 0.0f, armWidth, 12.0f, 4.0f, 48, 48, true),
            new SkinBox(-2.0f, 6.0f, 0.0f, 4.0f, 12.0f, 4.0f, 0, 32, true),
            new SkinBox(2.0f, 6.0f, 0.0f, 4.0f, 12.0f, 4.0f, 0, 48, true),
        };

        float scale = Math.min(bounds.width() / 22.0f, bounds.height() / 38.0f);
        float originX = bounds.centerX();
        float originY = bounds.centerY() + 17.0f * scale;
        float yaw = modelYaw * Maths.DEG_TO_RAD;
        float pitch = modelPitch * Maths.DEG_TO_RAD;
        float cosYaw = (float) Math.cos(yaw);
        float sinYaw = (float) Math.sin(yaw);
        float cosPitch = (float) Math.cos(pitch);
        float sinPitch = (float) Math.sin(pitch);

        List<SkinQuad> quads = new ArrayList<>();
        float[] cx = new float[8];
        float[] cy = new float[8];
        float[] cz = new float[8];
        for (SkinBox box : boxes) {
            float grow = box.overlay() ? 0.5f : 0.0f;
            float hw = box.w() * 0.5f + grow;
            float hh = box.h() * 0.5f + grow;
            float hd = box.d() * 0.5f + grow;

            for (int corner = 0; corner < 8; ++corner) {
                float lx = ((corner & 1) != 0 ? hw : -hw) + box.x();
                float ly = ((corner & 2) != 0 ? hh : -hh) + box.y();
                float lz = ((corner & 4) != 0 ? hd : -hd) + box.z();
                float rx = lx * cosYaw + lz * sinYaw;
                float rz = -lx * sinYaw + lz * cosYaw;
                float ry = ly * cosPitch - rz * sinPitch;
                cz[corner] = ly * sinPitch + rz * cosPitch;
                cx[corner] = originX + rx * scale;
                cy[corner] = originY - ry * scale;
            }

            int w = (int) box.w();
            int h = (int) box.h();
            int d = (int) box.d();
            int u = box.uvX();
            int v = box.uvY();

            Face[] faces = {
                new Face(2, 3, 1, 0, u + d, v + d, w, h, 1.0f),
                new Face(7, 6, 4, 5, u + d + w + d, v + d, w, h, 0.74f),
                new Face(6, 2, 0, 4, u, v + d, d, h, 0.86f),
                new Face(3, 7, 5, 1, u + d + w, v + d, d, h, 0.86f),
                new Face(6, 7, 3, 2, u + d, v, w, d, 1.0f),
                new Face(0, 1, 5, 4, u + d + w, v, w, d, 0.62f),
            };

            for (Face face : faces) {
                int[] order = {face.a(), face.b(), face.c(), face.d()};
                float[] xs = new float[4];
                float[] ys = new float[4];
                float depth = 0.0f;
                for (int index = 0; index < 4; ++index) {
                    xs[index] = cx[order[index]];
                    ys[index] = cy[order[index]];
                    depth += cz[order[index]];
                }

                float area = 0.0f;
                for (int index = 0; index < 4; ++index) {
                    int next = (index + 1) & 3;
                    area += xs[index] * ys[next] - xs[next] * ys[index];
                }
                if (area <= 0.0f) {
                    continue;
                }
                Rect uv = new Rect(face.uvX() / SKIN_TEXTURE, face.uvY() / SKIN_TEXTURE,
                        face.uvW() / SKIN_TEXTURE, face.uvH() / SKIN_TEXTURE);
                quads.add(new SkinQuad(xs, ys, uv, depth * 0.25f - (box.overlay() ? 0.01f : 0.0f), face.shade()));
            }
        }

        quads.sort((a, b) -> Float.compare(b.depth(), a.depth()));

        gfx.pushClip(bounds);
        for (SkinQuad quad : quads) {
            gfx.imageFace(quad.xs(), quad.ys(), quad.uv(), texture, shaded(quad.shade(), alpha));
        }
        gfx.popClip();
    }

    private void renderArmour(PlayerRadar.Player player, Rect bounds) {
        for (int slot = 0; slot < ARMOUR_SLOTS.length; ++slot) {
            Rect row = new Rect(bounds.x(), bounds.y() + px(ARMOUR_ROW + 1.5f) * slot, bounds.width(),
                    px(ARMOUR_ROW));
            Game.EquipmentView item = player.slots[slot];
            gfx.rect(row, Theme.SURFACE.withAlpha(alpha * 0.8f), px(Theme.RADIUS_CARD));

            Rect chip = new Rect(row.x() + px(3.5f), row.centerY() - px(ICON_CHIP) * 0.5f, px(ICON_CHIP),
                    px(ICON_CHIP));
            if (!item.present()) {
                gfx.icon(Icon.MINUS, chip.inset(px(3.5f)), Theme.TEXT_FAINT.withAlpha(alpha * 0.6f));
                gfx.textCap(FontRole.REGULAR, ARMOUR_SLOTS[slot], chip.right() + px(4.0f),
                        row.centerY() - px(7.4f) * CAP_MIDDLE, px(7.4f), Theme.TEXT_FAINT.withAlpha(alpha * 0.7f));
                continue;
            }

            renderItemIcon(item.id(), chip);

            float textX = chip.right() + px(4.0f);
            gfx.textCap(FontRole.MEDIUM, gfx.elide(FontRole.MEDIUM, item.name(), px(7.8f),
                    row.right() - px(4.0f) - textX), textX, row.y() + px(4.5f), px(7.8f),
                    Theme.TEXT_BRIGHT.withAlpha(alpha));

            float wear = item.maxDamage() > 0 ? 1.0f - (float) item.damage() / item.maxDamage() : 1.0f;
            String percent = String.format(Locale.ROOT, "%.0f%%", wear * 100.0f);
            float percentWidth = gfx.measure(FontRole.MONO, percent, px(6.6f));
            gfx.textCap(FontRole.MONO, percent, row.right() - px(4.0f), row.y() + px(13.0f), px(6.6f),
                    (wear > 0.35f ? Theme.POSITIVE : Theme.WARNING).withAlpha(alpha), Align.RIGHT);

            String line = enchantLine(item.enchantments());
            gfx.textCap(FontRole.REGULAR, gfx.elide(FontRole.REGULAR, line.isEmpty() ? "No enchantments" : line,
                    px(6.6f), row.right() - px(8.0f) - percentWidth - textX), textX, row.y() + px(13.0f), px(6.6f),
                    (line.isEmpty() ? Theme.TEXT_FAINT : Theme.accentDim()).withAlpha(alpha));
        }
    }

    private void renderHands(PlayerRadar.Player player, Rect bounds) {
        float half = (bounds.width() - px(6.0f)) * 0.5f;
        for (int hand = 0; hand < HAND_LABELS.length; ++hand) {
            Rect card = new Rect(bounds.x() + (half + px(6.0f)) * hand, bounds.y(), half, bounds.height());
            Game.EquipmentView item = player.slots[Game.Slot.MAIN_HAND.ordinal() + hand];
            gfx.rect(card, Theme.SURFACE.withAlpha(alpha * 0.8f), px(Theme.RADIUS_CARD));
            gfx.textCap(FontRole.REGULAR, HAND_LABELS[hand], card.x() + px(6.0f), card.y() + px(4.5f), px(6.4f),
                    Theme.TEXT_FAINT.withAlpha(alpha));

            Rect chip = new Rect(card.x() + px(6.0f), card.bottom() - px(4.0f) - px(ICON_CHIP), px(ICON_CHIP),
                    px(ICON_CHIP));
            if (!item.present()) {
                gfx.textCap(FontRole.REGULAR, "Empty", card.x() + px(6.0f), card.y() + px(14.5f), px(8.0f),
                        Theme.TEXT_FAINT.withAlpha(alpha));
                continue;
            }
            renderItemIcon(item.id(), chip);
            String line = enchantLine(item.enchantments());
            float textX = chip.right() + px(4.0f);
            gfx.textCap(FontRole.MEDIUM, gfx.elide(FontRole.MEDIUM, item.name(), px(8.0f),
                    card.right() - px(4.0f) - textX), textX, card.y() + px(11.0f), px(8.0f),
                    Theme.TEXT_BRIGHT.withAlpha(alpha));
            if (!line.isEmpty()) {
                gfx.textCap(FontRole.REGULAR, gfx.elide(FontRole.REGULAR, line, px(6.4f),
                        card.right() - px(4.0f) - textX), textX, card.y() + px(19.0f), px(6.4f),
                        Theme.accentDim().withAlpha(alpha));
            }
        }
    }

    private void renderEffects(PlayerRadar.Player player, Rect bounds) {
        if (player.effects.isEmpty()) {
            gfx.textCap(FontRole.REGULAR, "No active effects", bounds.x(), bounds.centerY() - px(7.0f) * CAP_MIDDLE,
                    px(7.0f), Theme.TEXT_FAINT.withAlpha(alpha));
            return;
        }
        float x = bounds.x();
        for (Game.EffectView effect : player.effects) {
            String label = prettify(effect.id());
            if (effect.amplifier() > 0) {
                label += " " + roman(effect.amplifier() + 1);
            }
            float width = gfx.measure(FontRole.MEDIUM, label, px(6.8f)) + px(11.0f);
            if (x + width > bounds.right()) {
                break;
            }
            Rect chip = new Rect(x, bounds.y(), width, bounds.height());
            gfx.rect(chip, Theme.accentSoft().withAlpha(alpha * 0.9f), px(Theme.RADIUS_PILL));
            gfx.textCap(FontRole.MEDIUM, label, chip.centerX(), chip.centerY() - px(6.8f) * CAP_MIDDLE, px(6.8f),
                    Theme.accent().withAlpha(alpha), Align.CENTER);
            x += width + px(4.0f);
        }
    }

    private void renderHistory(PlayerRadar.Player player, Rect bounds) {
        gfx.textCap(FontRole.SEMI_BOLD, "Seen items  " + player.history.size(), bounds.x(), bounds.y() - px(9.0f),
                px(8.0f), Theme.TEXT.withAlpha(alpha));

        historyHeight = px(player.history.size() * (HISTORY_ROW + 2.0f));

        gfx.pushClip(bounds);

        List<PlayerRadar.Item> ordered = new ArrayList<>(player.history);
        ordered.sort((a, b) -> a.slot != b.slot ? a.slot.compareTo(b.slot) : Float.compare(b.lastSeen, a.lastSeen));

        float y = bounds.y() + historyScroll;
        for (PlayerRadar.Item item : ordered) {
            Rect row = new Rect(bounds.x(), y, bounds.width(), px(HISTORY_ROW));
            y += px(HISTORY_ROW + 2.0f);
            if (row.bottom() < bounds.y() || row.y() > bounds.bottom()) {
                continue;
            }
            gfx.rect(row, Theme.SURFACE.withAlpha(alpha * 0.55f), px(Theme.RADIUS_CARD));

            Rect chip = new Rect(row.x() + px(3.5f), row.centerY() - px(ICON_CHIP) * 0.5f, px(ICON_CHIP),
                    px(ICON_CHIP));
            renderItemIcon(item.id, chip);

            String tally = "x" + item.count;
            float tallyWidth = gfx.measure(FontRole.MONO, tally, px(7.0f));
            gfx.textCap(FontRole.MONO, tally, row.right() - px(5.0f), row.centerY() - px(7.0f) * CAP_MIDDLE, px(7.0f),
                    Theme.TEXT_DIM.withAlpha(alpha), Align.RIGHT);

            float textX = chip.right() + px(4.0f);
            float width = row.right() - px(9.0f) - tallyWidth - textX;
            String line = enchantLine(item.enchantments);
            if (line.isEmpty()) {
                gfx.textCap(FontRole.MEDIUM, gfx.elide(FontRole.MEDIUM, item.name, px(7.8f), width), textX,
                        row.centerY() - px(7.8f) * CAP_MIDDLE, px(7.8f), Theme.TEXT_BRIGHT.withAlpha(alpha));
            } else {
                gfx.textCap(FontRole.MEDIUM, gfx.elide(FontRole.MEDIUM, item.name, px(7.6f), width), textX,
                        row.y() + px(4.0f), px(7.6f), Theme.TEXT_BRIGHT.withAlpha(alpha));
                gfx.textCap(FontRole.REGULAR, gfx.elide(FontRole.REGULAR, line, px(6.4f), width), textX,
                        row.y() + px(12.0f), px(6.4f), Theme.accentDim().withAlpha(alpha));
            }
        }
        gfx.popClip();

        if (player.history.isEmpty()) {
            gfx.textCap(FontRole.REGULAR, "Nothing seen yet", bounds.x(), bounds.y() + px(10.0f), px(7.4f),
                    Theme.TEXT_FAINT.withAlpha(alpha));
        }
        if (historyHeight > bounds.height() + 1.0f) {
            float ratio = bounds.height() / historyHeight;
            float travel = bounds.height() * (1.0f - ratio);
            float offset = -historyScroll / (historyHeight - bounds.height());
            gfx.rect(new Rect(bounds.right() + px(2.0f), bounds.y() + travel * offset, px(2.0f),
                    bounds.height() * ratio), Theme.SCROLLBAR.withAlpha(alpha * 0.7f), px(1.0f));
        }
    }

    private void renderItemIcon(String id, Rect bounds) {
        Game.SpriteView sprite = Game.itemSprite(id);
        if (sprite.valid()) {
            Rect uv = new Rect(sprite.u0(), sprite.v0(), sprite.u1() - sprite.u0(), sprite.v1() - sprite.v0());
            gfx.image(bounds, uv, sprite.texture(), Color.rgb(0xFFFFFF, alpha), 0.0f);
            return;
        }
        gfx.rect(bounds, Theme.SURFACE_ACTIVE.withAlpha(alpha * 0.8f), px(2.0f));
        gfx.icon(iconFor(id), bounds.inset(px(2.6f)), Theme.accentDim().withAlpha(alpha));
    }

    private void renderDetail() {
        Rect pane = new Rect(panel.x() + px(PANE_LEFT), panel.y() + px(MARGIN), px(PANE_WIDTH),
                px(PANEL_HEIGHT - MARGIN * 2.0f));
        PlayerRadar.Player player = selected == null ? null : PlayerRadar.find(selected);
        if (player == null) {
            renderEmpty(pane);
            return;
        }

        gfx.textCap(FontRole.BOLD, gfx.elide(FontRole.BOLD, player.name, px(NAME_SIZE), pane.width() - px(60.0f)),
                pane.x(), panel.y() + px(NAME_CAP_TOP), px(NAME_SIZE), Theme.TEXT_BRIGHT.withAlpha(alpha));
        if (!player.loaded) {
            gfx.textCap(FontRole.REGULAR, "last seen", pane.right(), panel.y() + px(NAME_CAP_TOP + 3.0f), px(7.0f),
                    Theme.TEXT_FAINT.withAlpha(alpha), Align.RIGHT);
        }

        renderStats(player, new Rect(pane.x(), panel.y() + px(STATS_TOP), px(STATS_WIDTH),
                px(STAT_HEIGHT * 4.0f + STAT_GAP * 3.0f)));
        renderSkin(player, skinBounds);
        renderArmour(player, new Rect(panel.x() + px(ARMOUR_LEFT), panel.y() + px(STATS_TOP), px(ARMOUR_WIDTH),
                px(ARMOUR_ROW * 4.0f + 4.5f)));
        renderHands(player, new Rect(pane.x(), panel.y() + px(HANDS_TOP), pane.width(), px(HANDS_HEIGHT)));
        renderEffects(player, new Rect(pane.x(), panel.y() + px(EFFECTS_TOP), pane.width(), px(EFFECTS_HEIGHT)));
        renderHistory(player, historyBounds);
    }

    private void typeCharacter(int codepoint) {
        if (codepoint < 32 || codepoint > 126 || query.length() >= 24) {
            return;
        }
        query += (char) codepoint;
        scrollTarget = 0.0f;
    }

    private void pressAt(float x, float y, int button) {
        if (!panel.contains(x, y) || button != Input.MOUSE_LEFT) {
            return;
        }
        if (closeBounds.contains(x, y)) {
            close();
            return;
        }
        if (sortBounds.contains(x, y)) {
            sort = sort.next();
            return;
        }
        if (skinBounds.contains(x, y)) {
            turning = true;
            touched = true;
            grabYaw = modelYaw;
            grabPitch = modelPitch;
            turnFromX = x;
            turnFromY = y;
            return;
        }
        if (listBounds.contains(x, y)) {
            for (Row row : rows) {
                if (row.bounds().contains(x, y)) {
                    if (!row.uuid().equals(selected)) {
                        selected = row.uuid();
                        historyTarget = 0.0f;
                        historyScroll = 0.0f;
                    }
                    return;
                }
            }
            return;
        }

        if (y < panel.y() + px(SEARCH_TOP)) {
            dragging = true;
            dragOffsetX = x - panel.x();
            dragOffsetY = y - panel.y();
        }
    }

    public void handleEvent(Input.Event event) {
        if (!open) {
            return;
        }
        switch (event.kind()) {
            case MOUSE_MOVE -> {
                mouseX = (float) event.x();
                mouseY = (float) event.y();
            }
            case MOUSE_BUTTON -> {
                mouseX = (float) event.x();
                mouseY = (float) event.y();
                if (event.action() == Input.PRESS) {
                    pressAt(mouseX, mouseY, event.code());
                } else {
                    dragging = false;
                    turning = false;
                }
            }
            case SCROLL -> {
                if (historyBounds.contains(mouseX, mouseY)) {
                    historyTarget += (float) event.y() * px(28.0f);
                } else if (listBounds.contains(mouseX, mouseY)) {
                    scrollTarget += (float) event.y() * px(36.0f);
                }
            }
            case CHAR -> typeCharacter(event.code());
            case KEY -> {
                if (event.action() == Input.RELEASE) {
                    return;
                }
                if (event.code() == Input.KEY_BACKSPACE && !query.isEmpty()) {
                    query = query.substring(0, query.length() - 1);
                } else if (event.code() == Input.KEY_ESCAPE) {
                    close();
                }
            }
        }
    }

    private static String prettify(String id) {
        StringBuilder out = new StringBuilder(id.length());
        boolean boundary = true;
        for (char character : id.toCharArray()) {
            if (character == '_') {
                out.append(' ');
                boundary = true;
                continue;
            }
            out.append(boundary && character >= 'a' && character <= 'z' ? (char) (character - 32) : character);
            boundary = false;
        }
        return out.toString();
    }

    private static String roman(int value) {
        if (value <= 0 || value > 40) {
            return Integer.toString(value);
        }
        StringBuilder out = new StringBuilder();
        for (int index = 0; index < NUMERALS.length; ++index) {
            while (value >= NUMERAL_VALUES[index]) {
                out.append(NUMERALS[index]);
                value -= NUMERAL_VALUES[index];
            }
        }
        return out.toString();
    }

    private static String enchantLine(List<Game.EnchantView> enchantments) {
        StringBuilder out = new StringBuilder();
        for (Game.EnchantView enchant : enchantments) {
            if (!out.isEmpty()) {
                out.append(", ");
            }
            out.append(prettify(enchant.id())).append(' ').append(roman(enchant.level()));
        }
        return out.toString();
    }

    private static Icon iconFor(String id) {
        if (id.contains("helmet") || id.contains("chestplate") || id.contains("leggings") || id.contains("boots")) {
            return Icon.LAYERS;
        }

        if (id.contains("pickaxe") || id.contains("shovel") || id.contains("hoe") || id.contains("shears")) {
            return Icon.HAMMER;
        }
        if (id.contains("sword") || id.contains("axe")) {
            return Icon.SWORDS;
        }
        if (id.contains("bow") || id.contains("arrow")) {
            return Icon.TARGET;
        }
        if (id.contains("potion")) {
            return Icon.FLAME;
        }
        return Icon.BOX;
    }

    private static Color shaded(float factor, float alpha) {
        return new Color(factor, factor, factor, alpha);
    }

    private static Color pingColour(int ping) {
        if (ping < 0) {
            return Theme.TEXT_FAINT;
        }
        if (ping < 80) {
            return Theme.POSITIVE;
        }
        if (ping < 200) {
            return Theme.WARNING;
        }
        return Theme.NEGATIVE;
    }
}
