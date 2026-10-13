package stone.ui;

import stone.core.Color;
import stone.core.Config;
import stone.core.Ease;
import stone.core.Log;
import stone.core.Maths;
import stone.core.Rect;
import stone.game.FrameState;
import stone.hook.Input;
import stone.hook.MenuScreen;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.ModuleManager;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.module.client.ClickGuiModule;
import stone.render.Align;
import stone.render.Backdrop;
import stone.render.FontRole;
import stone.render.Icon;
import stone.render.Renderer;
import stone.script.ScriptManager;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class ClickGui {
    private static final String SCOPE = "gui";

    private static final float MENU_WIDTH = 660.0f;
    private static final float TOP_MARGIN = 18.0f;
    private static final float TAB_BAR_HEIGHT = 38.0f;
    private static final float TAB_BAR_RADIUS = 12.0f;
    private static final float PANEL_GAP = 9.0f;
    private static final float CARD_RADIUS = 14.0f;
    private static final float HEADER_HEIGHT = 38.0f;
    private static final float CARD_PAD_X = 12.0f;
    private static final float ROW_HEIGHT = 50.0f;
    private static final float ROW_GAP = 4.0f;
    private static final float ROW_RADIUS = 8.0f;
    private static final float SECTION_HEIGHT = 28.0f;
    private static final float GEAR_SLOT_WIDTH = 52.0f;

    private static final float SELECTOR_SLOT = 70.0f;
    private static final float SELECTOR_INSET = 3.0f;
    private static final float SELECTOR_MARGIN = 5.0f;
    private static final float SELECTOR_RADIUS = 9.0f;
    private static final float SELECTOR_LABEL_BOX = 13.0f;
    private static final String PATH_ARROW = ">>";

    private static final float ROW_ICON_CENTRE = 27.0f;
    private static final float ROW_TEXT_X = 56.0f;
    private static final float ROW_NAME_TOP = 9.0f;
    private static final float ROW_DESC_TOP = 27.0f;
    private static final float PILL_WIDTH = 38.0f;
    private static final float PILL_HEIGHT = 19.0f;
    private static final float PILL_MARGIN = 16.0f;

    private static final float SETTING_HEIGHT = 40.0f;
    private static final float SETTING_PAD_X = 14.0f;
    private static final float CHOICE_ITEM_HEIGHT = 26.0f;
    private static final float SLIDER_TRACK = 4.0f;
    private static final float EDITOR_FIELD = 96.0f;
    private static final float EDITOR_STRIP = 10.0f;
    private static final float EDITOR_SWATCH = 18.0f;
    private static final float EDITOR_GAP = 8.0f;

    private static final float EDITOR_HEIGHT = EDITOR_GAP * 4.0f + EDITOR_FIELD + EDITOR_STRIP + EDITOR_SWATCH;

    private static final float LABEL_BOX = 15.0f;
    private static final float DESC_BOX = 12.5f;
    private static final float MICRO_BOX = 9.5f;
    private static final float HEADING_BOX = 11.5f;
    private static final float ICON_BOX = 17.0f;
    private static final float CHEVRON_BOX = 13.0f;
    private static final float TRACKING = 1.4f;
    private static final FontRole LABEL_FONT = FontRole.BOLD;
    private static final FontRole DESC_FONT = FontRole.MEDIUM;
    private static final FontRole MICRO_FONT = FontRole.BOLD;
    private static final FontRole HEADING_FONT = FontRole.BOLD;

    private static final float SLIDE_SPEED = 16.0f;
    private static final float HOVER_SPEED = 16.0f;
    private static final float ON_SPEED = 18.0f;
    private static final float TAB_SPEED = 20.0f;

    private static final float CHIP_LEAD_SPEED = 30.0f;
    private static final float CHIP_TRAIL_SPEED = 13.0f;
    private static final float SUBPAGE_SPEED = 22.0f;
    private static final float EXPAND_SPEED = 14.0f;

    private static final float CARD_SPEED = 15.0f;

    private static final float PAGE_OUT_SECONDS = 0.10f;
    private static final float PAGE_IN_SECONDS = 0.20f;
    private static final float PAGE_CASCADE_SECONDS = 0.42f;
    private static final float ROW_RISE = 14.0f;
    private static final float ROW_STAGGER = 0.05f;
    private static final float ROW_STAGGER_CAP = 0.58f;

    private static final float REVEAL_POP_END = 0.42f;
    private static final float REVEAL_HOLD_END = 0.72f;
    private static final float REVEAL_WIDEN_END = 1.20f;
    private static final float REVEAL_UNFOLD_START = 1.06f;
    private static final float REVEAL_UNFOLD_END = 1.66f;

    private static final float REVEAL_END = 1.70f;
    private static final float REVEAL_POP_SCALE = 0.32f;

    private static final float EXIT_FOLD_END = 0.16f;
    private static final float EXIT_NARROW_START = 0.10f;
    private static final float EXIT_NARROW_END = 0.28f;

    private static final float EXIT_LABEL_IN = 0.19f;
    private static final float EXIT_LABEL_FULL = 0.31f;
    private static final float EXIT_POP_START = 0.40f;
    private static final float EXIT_END = 0.52f;

    private static final int MAX_CONFIG_NAME = 24;

    private static final Color CARD_BG = Color.rgb(0x111113, 236 / 255.0f);
    private static final Color CARD_BORDER = Color.rgb(0x262629);
    private static final Color TAB_BAR_BG = Color.rgb(0x171719, 236 / 255.0f);
    private static final Color TAB_CHIP_BG = Color.rgb(0x28282B);
    private static final Color ROW_BG = Color.rgb(0x171719, 170 / 255.0f);
    private static final Color ROW_BG_ON = Color.rgb(0x202023, 235 / 255.0f);
    private static final Color SEPARATOR = Color.rgb(0x303034);
    private static final Color TEXT_PRIMARY = Color.rgb(0xEDEDEF);
    private static final Color TEXT_MUTED = Color.rgb(0x8A8A90);
    private static final Color TEXT_FAINT = Color.rgb(0x6C6C72);
    private static final Color CHIP_BG = Color.rgb(0x2C2C2F);
    private static final Color CHIP_TEXT = Color.rgb(0x9E9EA4);
    private static final Color TOGGLE_OFF = Color.rgb(0x3A3A3F);
    private static final Color KNOB_OFF = Color.rgb(0xA8A8AE);
    private static final Color KNOB_ON = Color.rgb(0xFFFFFF);
    private static final Color ICON_ON = Color.rgb(0xD6D6DA);
    private static final Color HOVER_WASH = Color.rgb(0xFFFFFF, 18 / 255.0f);
    private static final Color TRACK_BG = Color.rgb(0x000000, 0.35f);

    private enum RowKind {
        MODULE,
        FLAG,
        SLIDER,
        COLOUR,
        CONFIGS,
        ACTION,
        SECTION
    }

    private enum Action { RELOAD_SCRIPTS, RELOAD_CONFIGS, OPEN_FOLDER, PANIC, UNLOAD }

    private static final Subcategory[] SUBCATEGORIES = Subcategory.values();
    private static final String[] SUBCATEGORY_TITLES = Arrays.stream(SUBCATEGORIES)
            .map(subcategory -> upperCase(subcategory.displayName)).toArray(String[]::new);

    private static final class SettingRow {

        final Setting setting;
        Rect bounds = Rect.EMPTY;
        Rect control = Rect.EMPTY;
        float hover;
        float knob;
        float enter = 1.0f;
        boolean expanded;
        float expand;
        boolean listeningForKey;
        boolean pickerOpen;
        float picker;

        float hue;

        SettingRow(Setting setting) {
            this.setting = setting;
        }
    }

    private static final class Row {
        final RowKind kind;
        final String name;
        final String description;
        final Icon icon;
        final String title;
        Module module;
        Setting setting;
        SettingRow sliderRow;
        Rect sliderControl = Rect.EMPTY;
        Action action;
        final List<SettingRow> settings = new ArrayList<>();
        Rect bounds = Rect.EMPTY;
        Rect chevron = Rect.EMPTY;
        float hover;
        float on;
        float enter = 1.0f;
        boolean binding;

        String descShown = "";
        float descLimit = -1.0f;
        float descScale;
        String bindShown = "";
        int bindKey = -2;
        boolean bindListening;

        Row(RowKind kind, String name, String description, Icon icon) {
            this.kind = kind;
            this.name = name;
            this.description = description;
            this.icon = icon;
            this.title = upperCase(name);
        }
    }

    private static final class Tab {
        final String key;
        final String label;
        final Icon icon;
        final String countNoun;

        final boolean iconOnly;
        final List<Row> rows = new ArrayList<>();
        Rect bounds = Rect.EMPTY;
        float hover;

        final String title;
        float titleWidth;
        float titleScale;

        String count = "";
        int countFor = -1;

        Tab(String label, Icon icon, String countNoun, boolean iconOnly) {
            this.key = label;
            this.label = label;
            this.icon = icon;
            this.countNoun = countNoun;
            this.iconOnly = iconOnly;
            this.title = upperCase(label);
        }
    }

    private static final class ConfigFile {
        final String name;
        Rect bounds = Rect.EMPTY;
        Rect remove = Rect.EMPTY;
        float hover;
        float removeHover;

        ConfigFile(String name) {
            this.name = name;
        }
    }

    private static final class Reveal {
        Rect bar = Rect.EMPTY;
        Rect card = Rect.EMPTY;
        float barRadius;
        float glow;
        float label;
        float barContent = 1.0f;
        float cardContent = 1.0f;
        boolean cardVisible = true;
    }

    private final Renderer gfx = Renderer.instance();

    private final List<Row> rows = new ArrayList<>();
    private final List<Tab> tabs = new ArrayList<>();
    private final List<ConfigFile> configs = new ArrayList<>();

    private int tab;
    private int shownTab;
    private float pageFade = 1.0f;
    private float pageEnter = 1.0f;
    private boolean pageLeaving;

    private boolean sectionHeld;
    private float cardHeight;

    private Subcategory subcategory = Subcategory.GENERAL;
    private Subcategory shownSubcategory = Subcategory.GENERAL;

    private final List<Row> shownRows = new ArrayList<>();
    private int shownEntries;
    private Rect selector = Rect.EMPTY;
    private float selectorAlpha = 1.0f;
    private float chipLeft;
    private float chipRight = 1.0f;
    private final float[] subcategoryHover = new float[SUBCATEGORIES.length];

    private final float[] subcategoryTitleWidths = new float[SUBCATEGORIES.length];
    private float arrowWidth;
    private float pathScale;

    private String brandText = "";
    private float brandWidth;
    private float brandScale;

    private boolean revealSpent;
    private boolean revealing;
    private float revealClock;

    private boolean exiting;
    private float exitClock;
    private final Reveal reveal = new Reveal();

    private Row subpage;
    private float subpageAnim;
    private float indicator;
    private float indicatorAlpha = 1.0f;
    private float gearHover;
    private float backHover;
    private String pendingTab = "";

    private int selectedConfig = -1;
    private String configName = "";
    private String configStatus = "";
    private boolean editingName;
    private Rect nameField = Rect.EMPTY;
    private Rect saveButton = Rect.EMPTY;
    private float nameHover;
    private float saveHover;

    private boolean built;
    private boolean open;
    private boolean closing;
    private float slide;

    private float alpha;
    private float blurStrength = 1.0f;

    private float presence = 1.0f;

    private float scale = 1.0f;
    private float screenW;
    private float screenH;

    private Rect bar = Rect.EMPTY;
    private Rect card = Rect.EMPTY;
    private Rect body = Rect.EMPTY;
    private float scroll;
    private float content;

    private float mouseX;
    private float mouseY;
    private boolean mouseDown;
    private float time;

    private SettingRow activeSlider;
    private SettingRow keyListener;
    private Row bindRow;
    private SettingRow activePicker;
    private int pickerAxis = -1;

    public boolean visible() {
        return open || slide > 0.002f;
    }

    public boolean interactive() {
        return open && !closing && !exiting;
    }

    public boolean capturingKey() {
        return keyListener != null || bindRow != null || editingName;
    }

    public boolean contains(float x, float y) {
        return bar.contains(x, y) || card.contains(x, y);
    }

    public void setBlurStrength(float value) {
        blurStrength = value;
    }

    public float blurStrength() {
        return blurStrength;
    }

    public void setBlur(boolean value) {
        blurStrength = value ? (blurStrength > 0.01f ? blurStrength : 1.0f) : 0.0f;
    }

    public boolean blurEnabled() {
        return blurStrength > 0.01f;
    }

    private boolean pointable() {
        return interactive() && !revealing;
    }

    private float s(float value) {
        return value * scale;
    }

    private Color fade(Color colour) {
        return colour.withAlpha(alpha);
    }

    private Color fade(Color colour, float extra) {
        return colour.withAlpha(alpha * extra);
    }

    private Color accent() {
        return accent(1.0f);
    }

    private Color accent(float amount) {
        return Theme.accent().withAlpha(alpha * amount);
    }

    private float fontEm(FontRole role, float box) {
        float ratio = gfx.fonts().face(role).boxHeight(1.0f);
        return ratio > 0.0001f ? box / ratio : box;
    }

    private float textWidth(FontRole role, String value, float box) {
        return gfx.measure(role, value, fontEm(role, box));
    }

    private float drawText(FontRole role, String value, float x, float top, float box, Color colour) {
        return gfx.text(role, value, x, top, fontEm(role, box), colour);
    }

    private float drawText(FontRole role, String value, float x, float top, float box, Color colour,
            Align align) {
        return gfx.text(role, value, x, top, fontEm(role, box), colour, align);
    }

    private void drawTracked(FontRole role, String value, float x, float top, float box, Color colour,
            float tracking) {
        float em = fontEm(role, box);
        for (int i = 0; i < value.length(); i++) {
            String glyph = value.substring(i, i + 1);
            gfx.text(role, glyph, x, top, em, colour);
            x += gfx.measure(role, glyph, em) + tracking;
        }
    }

    private float trackedWidth(FontRole role, String value, float box, float tracking) {
        float em = fontEm(role, box);
        float width = 0.0f;
        for (int i = 0; i < value.length(); i++) {
            width += gfx.measure(role, value.substring(i, i + 1), em) + tracking;
        }
        return width > 0.0f ? width - tracking : 0.0f;
    }

    private static String upperCase(String value) {
        return value.toUpperCase(Locale.ROOT);
    }

    private static String bindLabel(int key, boolean listening) {
        if (listening) {
            return "...";
        }
        if (key == 0) {
            return "NONE";
        }
        if (key <= 8) {
            return "MOUSE";
        }
        return upperCase(Keys.keyName(key));
    }

    private static String formatNumber(Setting setting) {
        String text = String.format(Locale.ROOT, "%." + setting.decimals() + "f", setting.number());
        if (text.indexOf('.') >= 0) {
            int end = text.length();
            while (end > 0 && text.charAt(end - 1) == '0') {
                end--;
            }
            if (end > 0 && text.charAt(end - 1) == '.') {
                end--;
            }
            text = text.substring(0, end);
        }
        return text;
    }

    private static float hueOf(Color colour) {
        float maximum = Math.max(colour.r(), Math.max(colour.g(), colour.b()));
        float minimum = Math.min(colour.r(), Math.min(colour.g(), colour.b()));
        float delta = maximum - minimum;
        if (delta <= 0.0001f) {
            return 0.0f;
        }
        float hue;
        if (maximum == colour.r()) {
            hue = ((colour.g() - colour.b()) / delta) % 6.0f;
        } else if (maximum == colour.g()) {
            hue = (colour.b() - colour.r()) / delta + 2.0f;
        } else {
            hue = (colour.r() - colour.g()) / delta + 4.0f;
        }
        hue /= 6.0f;
        return hue < 0.0f ? hue + 1.0f : hue;
    }

    private static boolean validConfigChar(char character) {
        return (character >= 'A' && character <= 'Z') || (character >= 'a' && character <= 'z')
                || (character >= '0' && character <= '9') || character == '_' || character == '-';
    }

    private void refreshBrand() {
        if (brandScale == scale && !brandText.isEmpty()) {
            return;
        }
        brandScale = scale;

        brandText = upperCase(Theme.CLIENT_SHORT_NAME) + " | V" + Theme.CLIENT_VERSION;

        brandWidth = textWidth(HEADING_FONT, brandText, s(HEADING_BOX));
    }

    private float capsuleWidth(float barWidth) {
        refreshBrand();
        return Math.min(barWidth, brandWidth + s(56.0f));
    }

    private float entranceAt(int ordinal) {
        if (pageEnter >= 1.0f) {
            return 1.0f;
        }
        float delay = Math.min(ROW_STAGGER_CAP, ordinal * ROW_STAGGER);
        float span = Math.max(0.12f, 1.0f - delay);
        return Ease.outCubic(Maths.clamp((pageEnter - delay) / span, 0.0f, 1.0f));
    }

    private void settleReveal() {
        reveal.bar = bar;
        reveal.card = card;
        reveal.barRadius = s(TAB_BAR_RADIUS);
        reveal.glow = 0.0f;
        reveal.label = 0.0f;
        reveal.barContent = 1.0f;
        reveal.cardContent = 1.0f;
        reveal.cardVisible = true;
    }

    private void advanceReveal(float deltaTime) {
        if (exiting) {

            float clock = exitClock;
            float foldT = Ease.morph(Ease.window(clock, 0.0f, EXIT_FOLD_END));
            float narrowT = Ease.morph(Ease.window(clock, EXIT_NARROW_START, EXIT_NARROW_END));
            float popT = Ease.morph(Ease.window(clock, EXIT_POP_START, EXIT_END));

            settleReveal();

            reveal.cardContent = 1.0f - Ease.outCubic(Ease.window(clock, 0.0f, EXIT_FOLD_END * 0.6f));
            reveal.barContent = 1.0f - Ease.outCubic(Ease.window(clock, EXIT_NARROW_START - 0.06f,
                    EXIT_NARROW_START + 0.08f));

            float foldedHeight = card.height() * (1.0f - foldT);
            reveal.cardVisible = foldedHeight > 1.0f;
            reveal.card = new Rect(card.x(), card.y(), card.width(), foldedHeight);

            float capsule = capsuleWidth(bar.width());
            float shrink = 1.0f - popT;
            float width = Maths.lerp(bar.width(), capsule, narrowT) * shrink;
            float height = bar.height() * shrink;
            reveal.bar = new Rect(bar.centerX() - width * 0.5f, bar.centerY() - height * 0.5f, width, height);
            reveal.barRadius = Maths.lerp(s(TAB_BAR_RADIUS), reveal.bar.height() * 0.5f, narrowT);

            reveal.glow = popT * popT;

            reveal.label = Ease.outExpo(Ease.window(clock, EXIT_LABEL_IN, EXIT_LABEL_FULL))
                    * (1.0f - Ease.morph(Ease.window(clock, EXIT_POP_START, EXIT_POP_START + 0.07f)));
            return;
        }

        if (!revealing) {
            settleReveal();
            return;
        }

        revealClock += deltaTime;
        if (revealClock >= REVEAL_END) {
            revealing = false;
            settleReveal();
            return;
        }

        float clock = revealClock;
        float popT = Ease.springIn(Ease.window(clock, 0.0f, REVEAL_POP_END));
        float widenT = Ease.morph(Ease.window(clock, REVEAL_HOLD_END, REVEAL_WIDEN_END));
        float unfoldT = Ease.morph(Ease.window(clock, REVEAL_UNFOLD_START, REVEAL_UNFOLD_END));

        float pop = Maths.lerp(REVEAL_POP_SCALE, 1.0f, popT);
        float capsule = capsuleWidth(bar.width());

        float width = Math.min(bar.width(), Maths.lerp(capsule * pop, bar.width(), widenT));
        float height = bar.height() * pop;

        reveal.bar = widenT >= 1.0f ? bar
                : new Rect(bar.centerX() - width * 0.5f, bar.centerY() - height * 0.5f, width, height);
        reveal.barRadius = Maths.lerp(reveal.bar.height() * 0.5f, s(TAB_BAR_RADIUS), widenT);

        float ripple = Ease.window(clock, 0.0f, REVEAL_POP_END * 0.9f);
        reveal.glow = (1.0f - ripple) * (1.0f - ripple);

        reveal.label = Ease.outExpo(Ease.window(clock, 0.12f, REVEAL_POP_END + 0.06f))
                * (1.0f - Ease.morph(Ease.window(clock, REVEAL_HOLD_END, REVEAL_HOLD_END + 0.16f)));

        reveal.barContent = Ease.outCubic(Ease.window(clock, REVEAL_WIDEN_END - 0.04f,
                REVEAL_WIDEN_END + 0.20f));

        float unfoldedHeight = card.height() * unfoldT;
        reveal.cardVisible = unfoldedHeight > 1.0f;
        reveal.card = new Rect(card.x(), card.y(), card.width(), unfoldedHeight);
        reveal.cardContent = Ease.outCubic(Ease.window(clock, REVEAL_UNFOLD_START + 0.16f, REVEAL_UNFOLD_END));
    }

    public void build() {
        if (built) {
            return;
        }
        rows.clear();
        tabs.clear();

        for (Category tabCategory : Category.values()) {
            if (tabCategory.tab != tabCategory) {
                continue;
            }
            Tab categoryTab = new Tab(tabCategory.displayName, tabCategory.icon, "MODULES", false);
            for (Category category : Category.values()) {
                if (category.tab != tabCategory) {
                    continue;
                }
                List<Module> inCategory = ModuleManager.byCategory(category);
                if (inCategory.isEmpty()) {
                    continue;
                }
                if (category != tabCategory) {
                    categoryTab.rows.add(new Row(RowKind.SECTION, category.displayName, "", category.icon));
                }
                for (Module module : inCategory) {
                    Row row = new Row(RowKind.MODULE, module.name, module.description, module.icon());
                    row.module = module;

                    row.settings.add(new SettingRow(null));
                    for (Setting setting : module.settings()) {
                        SettingRow settingRow = new SettingRow(setting);
                        settingRow.knob = setting.normalised();
                        settingRow.hue = hueOf(setting.colour());
                        row.settings.add(settingRow);
                    }
                    categoryTab.rows.add(row);
                    rows.add(row);
                }
            }
            if (!categoryTab.rows.isEmpty()) {
                tabs.add(categoryTab);
            }
        }

        Tab settings = new Tab("Settings", Icon.SETTINGS, "OPTIONS", true);
        settings.rows.add(new Row(RowKind.CONFIGS, "Configs", "Create, load and delete your configs.", Icon.BOX));

        Module self = ModuleManager.find("ClickGUI");
        if (self != null) {
            Setting accentSetting = self.setting("Accent");
            if (accentSetting != null) {
                Row row = new Row(RowKind.COLOUR, "Accent", "Pick the colour the whole client follows.", Icon.PALETTE);
                row.setting = accentSetting;
                SettingRow editor = new SettingRow(accentSetting);
                editor.hue = hueOf(accentSetting.colour());
                editor.pickerOpen = true;
                editor.picker = 1.0f;
                row.settings.add(editor);
                settings.rows.add(row);
            }
            Setting blurSetting = self.setting("Backdrop blur");
            if (blurSetting != null) {
                Row row = new Row(RowKind.SLIDER, "Backdrop blur", "Frost the scene behind the menu.", Icon.LAYERS);
                row.setting = blurSetting;
                row.sliderRow = new SettingRow(blurSetting);
                settings.rows.add(row);
            }
        }

        settings.rows.add(actionRow(Action.RELOAD_SCRIPTS, "Reload scripts", "Re-read every Lua script from disk.",
                Icon.BRACES));
        settings.rows.add(actionRow(Action.RELOAD_CONFIGS, "Reload configs", "Rescan the config folder.",
                Icon.ROTATE_CCW));
        settings.rows.add(actionRow(Action.OPEN_FOLDER, "Open folder", "Show the client's data folder.", Icon.GLOBE));
        settings.rows.add(actionRow(Action.PANIC, "Panic", "Turn every module off.", Icon.ZAP));
        settings.rows.add(actionRow(Action.UNLOAD, "Unload", "Detach the client from the game.", Icon.X));
        rows.addAll(settings.rows);
        tabs.add(settings);

        tab = 0;
        if (!pendingTab.isEmpty()) {
            for (int index = 0; index < tabs.size(); index++) {
                if (tabs.get(index).key.equals(pendingTab)) {
                    tab = index;
                    break;
                }
            }
            pendingTab = "";
        }

        subpage = null;
        subpageAnim = 0.0f;
        shownTab = tab;
        shownSubcategory = subcategory;
        refreshShown();
        pageLeaving = false;
        pageFade = 1.0f;
        indicator = tab;
        indicatorAlpha = tabs.get(tab).iconOnly ? 0.0f : 1.0f;
        selectorAlpha = indicatorAlpha;

        refreshConfigs();
        built = true;
        Log.info(SCOPE, "%d rows across %d tabs", rows.size(), tabs.size());
    }

    private static Row actionRow(Action action, String name, String description, Icon icon) {
        Row row = new Row(RowKind.ACTION, name, description, icon);
        row.action = action;
        return row;
    }

    public void reset() {
        if (!tabs.isEmpty() && tab < tabs.size()) {
            pendingTab = tabs.get(tab).key;
        }
        releaseAll();
        stopListening();
        if (bindRow != null) {
            bindRow.binding = false;
            bindRow = null;
        }
        editingName = false;
        subpage = null;
        built = false;
        rows.clear();
        tabs.clear();
        shownRows.clear();
    }

    public void open() {
        if (interactive()) {
            return;
        }
        build();

        open = true;
        closing = false;
        exiting = false;
        exitClock = 0.0f;
        slide = 0.0f;
        scroll = 0.0f;

        pageEnter = 0.0f;
        if (!revealSpent) {
            revealSpent = true;
            revealing = true;
            revealClock = 0.0f;
        }
        refreshConfigs();
        MenuScreen.show();
    }

    public void close() {
        if (!interactive()) {
            return;
        }

        boolean wasRevealing = revealing;
        revealing = false;

        exiting = !wasRevealing;
        closing = wasRevealing;
        exitClock = 0.0f;
        releaseAll();
        stopListening();
        if (bindRow != null) {
            bindRow.binding = false;
            bindRow = null;
        }
        editingName = false;

        if (!Client.radar().interactive()) {
            MenuScreen.hide();
        }
    }

    public void toggle() {
        if (interactive()) {
            close();
        } else {
            open();
        }
    }

    private void releaseAll() {
        mouseDown = false;
        activeSlider = null;
        activePicker = null;
        pickerAxis = -1;
    }

    private void stopListening() {
        if (keyListener != null) {
            keyListener.listeningForKey = false;
            keyListener = null;
        }
    }

    private void closePickers() {
        for (Row row : rows) {
            if (row.kind == RowKind.COLOUR) {
                continue;
            }
            for (SettingRow setting : row.settings) {
                setting.pickerOpen = false;
            }
        }
        activePicker = null;
        pickerAxis = -1;
    }

    private void selectTab(int index) {
        if (index >= tabs.size()) {
            return;
        }
        if (index == tab) {

            if (subpage != null) {
                closeSubpage();
            }
            return;
        }

        tab = index;
        subcategory = Subcategory.GENERAL;
        sectionHeld = false;
        closeSubpage();
        pageLeaving = true;
    }

    private void selectSubcategory(Subcategory value) {
        if (value == subcategory) {
            return;
        }

        if (!pageLeaving && pageFade >= 1.0f) {
            sectionHeld = true;
        }
        subcategory = value;
        pageLeaving = true;
    }

    private void refreshShown() {
        shownRows.clear();
        shownEntries = 0;
        Row heading = null;
        for (Row row : tabs.get(shownTab).rows) {
            if (row.kind == RowKind.SECTION) {
                heading = row;
            } else if (row.module == null || shownSubcategory.shows(row.module)) {
                if (heading != null) {
                    shownRows.add(heading);
                    heading = null;
                }
                shownRows.add(row);
                shownEntries++;
            }
        }
    }

    private void openSubpage(Row row) {

        subpage = row;
        subpageAnim = 0.0f;
        scroll = 0.0f;
        pageEnter = 0.0f;
    }

    private void closeSubpage() {
        boolean wasOpen = subpage != null;
        subpage = null;
        stopListening();
        closePickers();
        editingName = false;
        scroll = 0.0f;
        if (wasOpen) {
            pageEnter = 0.0f;
        }
    }

    public void loadLayout(Config config) {
        if (!config.hasSection("gui.menu")) {
            return;
        }
        String wanted = config.get("gui.menu", "tab", "");
        if (wanted.isEmpty()) {
            return;
        }
        if (tabs.isEmpty()) {
            pendingTab = wanted;
            return;
        }
        for (int index = 0; index < tabs.size(); index++) {
            if (tabs.get(index).key.equals(wanted)) {

                tab = index;
                shownTab = index;
                subcategory = Subcategory.GENERAL;
                shownSubcategory = subcategory;
                refreshShown();
                chipLeft = 0.0f;
                chipRight = 1.0f;
                pageLeaving = false;
                pageFade = 1.0f;
                indicator = index;
                closeSubpage();
                break;
            }
        }
    }

    public void saveLayout(Config config) {
        if (tabs.isEmpty() || tab >= tabs.size()) {
            return;
        }
        config.set("gui.menu", "tab", tabs.get(tab).key);
    }

    private static Path configPath(String name) {
        return Client.directory().resolve("stone-" + name + ".cfg");
    }

    private void refreshConfigs() {
        configs.clear();
        selectedConfig = -1;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(Client.directory(), "stone-*.cfg")) {
            for (Path file : files) {
                String fileName = file.getFileName().toString();
                if (fileName.length() <= "stone-.cfg".length() || !fileName.endsWith(".cfg")) {
                    continue;
                }

                String name = fileName.substring("stone-".length(), fileName.length() - 4);
                if (!name.isEmpty() && name.chars().allMatch(c -> validConfigChar((char) c))) {
                    configs.add(new ConfigFile(name));
                }
            }
        } catch (IOException e) {
            Log.warn(SCOPE, "config folder unreadable: %s", e.getMessage());
        }
        configs.sort((a, b) -> a.name.compareTo(b.name));
    }

    private boolean loadNamedConfig(String name) {
        if (name.isEmpty()) {
            return false;
        }
        Path path = configPath(name);
        if (!Files.exists(path)) {
            return false;
        }
        ModuleManager.loadConfig(path);
        loadLayout(ModuleManager.config());
        Client.hud().loadLayout(ModuleManager.config());
        Log.info(SCOPE, "config %s loaded", name);
        return true;
    }

    private boolean saveNamedConfig(String name) {
        if (name.isEmpty()) {
            return false;
        }
        Path path = configPath(name);
        ModuleManager.saveConfig(path);

        Config document = new Config();
        document.load(path);
        saveLayout(document);
        Client.hud().saveLayout(document);
        boolean ok = document.save(path);
        if (ok) {
            Log.info(SCOPE, "config %s saved", name);
            refreshConfigs();
            for (int i = 0; i < configs.size(); i++) {
                if (configs.get(i).name.equals(name)) {
                    selectedConfig = i;
                    break;
                }
            }
        }
        return ok;
    }

    private boolean deleteNamedConfig(String name) {
        if (name.isEmpty()) {
            return false;
        }
        try {
            Files.delete(configPath(name));
        } catch (IOException e) {
            return false;
        }
        Log.info(SCOPE, "config %s deleted", name);
        refreshConfigs();
        return true;
    }

    private void runAction(Action action, Context context) {
        switch (action) {
            case RELOAD_SCRIPTS -> {
                ScriptManager.requestReload();
                Log.info(SCOPE, "scripts reloading");
            }
            case RELOAD_CONFIGS -> refreshConfigs();
            case OPEN_FOLDER -> {
                try {
                    new ProcessBuilder("explorer", Client.directory().toString()).start();
                } catch (IOException e) {
                    Log.warn(SCOPE, "could not open %s: %s", Client.directory(), e.getMessage());
                }
            }
            case PANIC -> {

                for (Module module : ModuleManager.all()) {
                    if (module.enabled() && !(module instanceof ClickGuiModule)) {
                        module.setEnabled(false, context);
                    }
                }
            }
            case UNLOAD -> {
                ClickGuiModule self = ModuleManager.get(ClickGuiModule.class);
                if (self != null) {
                    self.setEnabled(false, context);
                }
                Client.requestShutdown();
            }
        }
    }

    public void handleEvent(Input.Event event, Context context) {
        if (!interactive()) {
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

                    if (!revealing) {
                        mouseDown = event.code() == Input.MOUSE_LEFT;
                        pressAt(mouseX, mouseY, event.code(), context);
                    }
                } else {
                    mouseDown = false;
                    activeSlider = null;
                    activePicker = null;
                    pickerAxis = -1;
                }
            }
            case SCROLL -> {

                if (content > body.height()) {
                    scroll = Maths.clamp(scroll - (float) event.y() * s(48.0f), 0.0f, content - body.height());
                }
            }
            case KEY -> {
                if (event.action() != Input.RELEASE) {
                    pressKey(event.code());
                }
            }
            case CHAR -> {
                char character = (char) event.code();
                if (editingName && validConfigChar(character) && configName.length() < MAX_CONFIG_NAME) {
                    configName += character;
                }
            }
        }
    }

    private void pressKey(int code) {
        if (editingName) {
            if (code == Input.KEY_ESCAPE) {
                editingName = false;
            } else if (code == Input.KEY_BACKSPACE) {
                if (!configName.isEmpty()) {
                    configName = configName.substring(0, configName.length() - 1);
                }
            } else if (code == Input.KEY_ENTER && !configName.isEmpty()) {
                configStatus = saveNamedConfig(configName) ? "Saved" : "Could not save";
                editingName = false;
            }
            return;
        }
        if (keyListener != null) {
            keyListener.setting.setKey(code == Input.KEY_ESCAPE ? 0 : code);
            keyListener.listeningForKey = false;
            keyListener = null;
            return;
        }
        if (bindRow != null) {
            bindRow.module.setKey(code == Input.KEY_ESCAPE || code == Input.KEY_BACKSPACE ? 0 : code);
            bindRow.binding = false;
            bindRow = null;
            return;
        }
        if (code == Input.KEY_ESCAPE) {
            if (subpage != null) {
                closeSubpage();
            } else {
                close();
            }
        }
    }

    private Rect backButton() {
        return new Rect(card.x() + s(8.0f), card.y() + s(4.0f), s(34.0f), s(HEADER_HEIGHT) - s(8.0f));
    }

    private Rect colourEditor(SettingRow setting) {
        return new Rect(setting.bounds.x(), setting.bounds.y() + s(SETTING_HEIGHT), setting.bounds.width(),
                s(EDITOR_HEIGHT));
    }

    private Rect colourField(Rect editor) {
        return new Rect(editor.x() + s(SETTING_PAD_X), editor.y() + s(EDITOR_GAP),
                editor.width() - s(SETTING_PAD_X) * 2.0f, s(EDITOR_FIELD));
    }

    private void pressAt(float x, float y, int button, Context context) {
        stopListening();

        if (!contains(x, y)) {
            editingName = false;
            return;
        }

        if (bar.contains(x, y)) {
            if (button != Input.MOUSE_LEFT) {
                return;
            }
            for (int index = 0; index < tabs.size(); index++) {
                Rect bounds = tabs.get(index).bounds;
                if (bounds.width() > 0.0f && bounds.contains(x, y)) {
                    selectTab(index);
                    return;
                }
            }
            return;
        }

        if (subpage != null && backButton().contains(x, y)) {
            if (button == Input.MOUSE_LEFT || button == Input.MOUSE_RIGHT) {
                closeSubpage();
            }
            return;
        }

        if (subpage == null && tab < tabs.size() && !tabs.get(tab).iconOnly && selector.contains(x, y)) {
            if (button == Input.MOUSE_LEFT) {
                int slot = (int) ((x - selector.x() - s(SELECTOR_INSET)) / s(SELECTOR_SLOT));
                selectSubcategory(SUBCATEGORIES[Maths.clamp(slot, 0, SUBCATEGORIES.length - 1)]);
            }
            return;
        }

        if (!body.contains(x, y)) {
            editingName = false;
            return;
        }

        if (subpage != null) {
            if (subpage.kind == RowKind.CONFIGS) {
                pressConfigs(x, y, button);
                return;
            }
            for (SettingRow setting : subpage.settings) {
                if (setting.bounds.width() > 0.0f && setting.bounds.contains(x, y)) {
                    pressSetting(setting, x, y, button);
                    return;
                }
            }
            return;
        }

        for (Row row : shownRows) {
            if (row.bounds.width() > 0.0f && row.bounds.contains(x, y)) {
                pressRow(row, x, y, button, context);
                return;
            }
        }
    }

    private void pressRow(Row row, float x, float y, int button, Context context) {
        boolean onChevron = row.chevron.width() > 0.0f && row.chevron.contains(x, y);

        if (row.kind == RowKind.ACTION) {
            if (button == Input.MOUSE_LEFT) {
                runAction(row.action, context);
            }
            return;
        }

        if (row.kind == RowKind.MODULE && button == Input.MOUSE_MIDDLE) {
            if (bindRow != null) {
                bindRow.binding = false;
            }
            bindRow = row;
            row.binding = true;
            return;
        }

        boolean wantsSubpage = row.kind == RowKind.COLOUR || row.kind == RowKind.CONFIGS
                || ((onChevron || button == Input.MOUSE_RIGHT) && row.kind == RowKind.MODULE);
        if (wantsSubpage) {
            if (button == Input.MOUSE_LEFT || button == Input.MOUSE_RIGHT) {
                openSubpage(row);
            }
            return;
        }

        if (button != Input.MOUSE_LEFT) {
            return;
        }
        if (row.kind == RowKind.MODULE) {
            row.module.toggle(context);
        } else if (row.kind == RowKind.FLAG) {
            row.setting.setBool(!row.setting.bool());
        } else if (row.kind == RowKind.SLIDER) {
            if (row.sliderControl.width() > 0.0f && row.sliderRow != null) {
                activeSlider = row.sliderRow;
                row.setting.setNormalised((x - row.sliderControl.x()) / row.sliderControl.width());
            }
        }
    }

    private void pressSetting(SettingRow setting, float x, float y, int button) {
        if (setting.setting == null) {
            if (subpage.module == null) {
                return;
            }
            if (button == Input.MOUSE_LEFT) {
                if (bindRow != null) {
                    bindRow.binding = false;
                }
                bindRow = subpage;
                subpage.binding = true;
            } else if (button == Input.MOUSE_RIGHT) {
                subpage.module.setKey(0);
            }
            return;
        }

        Setting value = setting.setting;
        switch (value.type) {
            case BOOLEAN -> {
                if (button == Input.MOUSE_LEFT) {
                    value.setBool(!value.bool());
                }
            }
            case NUMBER -> {
                if (button == Input.MOUSE_LEFT && setting.control.width() > 0.0f) {
                    activeSlider = setting;
                    value.setNormalised((x - setting.control.x()) / setting.control.width());
                }
            }
            case CHOICE -> {
                float localY = y - setting.bounds.y();
                if (localY < s(SETTING_HEIGHT)) {
                    if (button == Input.MOUSE_LEFT || button == Input.MOUSE_RIGHT) {
                        setting.expanded = !setting.expanded;
                    }
                } else if (setting.expand > 0.3f) {
                    int item = (int) ((localY - s(SETTING_HEIGHT)) / s(CHOICE_ITEM_HEIGHT));
                    if (item >= 0 && item < value.choices().size()) {
                        value.setChoice(item);
                    }
                }
            }
            case KEY -> {
                if (button == Input.MOUSE_LEFT) {
                    setting.listeningForKey = true;
                    keyListener = setting;
                } else if (button == Input.MOUSE_RIGHT) {
                    value.setKey(0);
                }
            }
            case COLOUR -> {
                Rect editor = colourEditor(setting);
                if (setting.pickerOpen && setting.picker > 0.5f && editor.contains(x, y)) {
                    activePicker = setting;
                    pickerAxis = y < colourField(editor).bottom() + s(EDITOR_GAP) * 0.5f ? 0 : 1;
                } else if (button == Input.MOUSE_LEFT || button == Input.MOUSE_RIGHT) {
                    setting.pickerOpen = !setting.pickerOpen;
                    if (setting.pickerOpen) {
                        setting.hue = hueOf(value.colour());
                    }
                }
            }
        }
    }

    private void pressConfigs(float x, float y, int button) {
        if (button != Input.MOUSE_LEFT) {
            return;
        }
        if (nameField.width() > 0.0f && nameField.contains(x, y)) {
            editingName = true;
            return;
        }
        if (saveButton.width() > 0.0f && saveButton.contains(x, y)) {
            editingName = false;
            configStatus = configName.isEmpty() ? "Name it first"
                    : saveNamedConfig(configName) ? "Saved" : "Could not save";
            return;
        }
        for (int i = 0; i < configs.size(); i++) {
            ConfigFile entry = configs.get(i);
            if (entry.remove.width() > 0.0f && entry.remove.contains(x, y)) {
                editingName = false;
                configStatus = deleteNamedConfig(entry.name) ? "Deleted" : "Could not delete";
                return;
            }
            if (entry.bounds.width() > 0.0f && entry.bounds.contains(x, y)) {
                editingName = false;
                selectedConfig = i;
                configName = entry.name;
                configStatus = loadNamedConfig(entry.name) ? "Loaded" : "Could not load";
                return;
            }
        }
        editingName = false;
    }

    private float settingHeight(SettingRow row) {
        float height = s(SETTING_HEIGHT);
        if (row.setting == null) {
            return height;
        }
        if (row.setting.type == Setting.Type.CHOICE) {
            height += row.setting.choices().size() * s(CHOICE_ITEM_HEIGHT) * row.expand;
        } else if (row.setting.type == Setting.Type.COLOUR) {
            height += s(EDITOR_HEIGHT) * row.picker;
        }
        return height;
    }

    private float rowHeight(Row row) {
        return s(row.kind == RowKind.SECTION ? SECTION_HEIGHT : ROW_HEIGHT);
    }

    private float listHeight() {
        float height = s(ROW_GAP);
        for (Row row : shownRows) {
            height += rowHeight(row) + s(ROW_GAP);
        }
        return height;
    }

    private float subpageHeight(Row row) {
        if (row.kind == RowKind.CONFIGS) {
            return s(ROW_GAP) + s(SETTING_HEIGHT) + s(ROW_GAP)
                    + configs.size() * (s(SETTING_HEIGHT) + s(ROW_GAP)) + s(SETTING_HEIGHT);
        }
        float height = s(ROW_GAP);
        for (SettingRow setting : row.settings) {
            if (setting.setting != null && !setting.setting.visible()) {
                continue;
            }
            height += settingHeight(setting) + s(ROW_GAP);
        }
        return height;
    }

    private void layout(Context context) {
        FrameState state = context.state;
        float deltaTime = context.deltaTime;
        time = context.time;

        float factor = state.scaledWidth > 0 ? (float) state.windowWidth / state.scaledWidth : 2.0f;

        scale = Maths.clamp(Math.max(factor, 0.5f) * 0.5f, 0.7f, 2.2f);

        screenW = state.windowWidth;
        screenH = state.windowHeight;

        if (exiting) {

            exitClock += deltaTime;
            slide = 1.0f;
            alpha = 1.0f - Ease.morph(Ease.window(exitClock, EXIT_POP_START + 0.04f, EXIT_END));
            if (exitClock >= EXIT_END) {
                exiting = false;
                open = false;
                closing = false;
                slide = 0.0f;
                alpha = 0.0f;
            }
        } else {
            slide = Maths.approach(slide, open && !closing ? 1.0f : 0.0f, SLIDE_SPEED, deltaTime);
            if (closing && slide < 0.02f) {
                open = false;
                closing = false;
                slide = 0.0f;
            }
            alpha = Maths.clamp(Ease.outCubic(Maths.clamp(slide, 0.0f, 1.0f)), 0.0f, 1.0f);
        }
        if (revealing) {

            slide = 1.0f;
            alpha = 1.0f;
        }
        presence = Maths.approach(presence, Client.hud().dragging() ? 0.0f : 1.0f, 18.0f, deltaTime);
        alpha *= presence;

        if (pageLeaving) {
            pageFade = Math.max(0.0f, pageFade - deltaTime / PAGE_OUT_SECONDS);
            if (pageFade <= 0.0f) {
                shownTab = tab;
                shownSubcategory = subcategory;
                refreshShown();
                pageLeaving = false;
                pageEnter = 0.0f;
                scroll = 0.0f;
            }
        } else {
            pageFade = Math.min(1.0f, pageFade + deltaTime / PAGE_IN_SECONDS);
        }

        if (!revealing || revealClock >= REVEAL_UNFOLD_START + 0.10f) {
            pageEnter = Math.min(1.0f, pageEnter + deltaTime / PAGE_CASCADE_SECONDS);
        }

        if (tabs.isEmpty()) {
            bar = Rect.EMPTY;
            card = Rect.EMPTY;
            body = Rect.EMPTY;
            return;
        }
        if (tab >= tabs.size()) {
            tab = 0;
        }
        if (shownTab >= tabs.size()) {
            shownTab = tab;
            refreshShown();
        }

        float width = Math.min(s(MENU_WIDTH), screenW - s(32.0f));

        if (width <= s(120.0f) || screenH <= s(120.0f)) {
            bar = Rect.EMPTY;
            card = Rect.EMPTY;
            body = Rect.EMPTY;
            return;
        }
        float x = (float) Math.floor((screenW - width) * 0.5f);

        float rise = (revealing || exiting) ? 0.0f : (1.0f - alpha) * s(10.0f);
        float y = (float) Math.floor(s(TOP_MARGIN) - rise);

        bar = new Rect(x, y, width, s(TAB_BAR_HEIGHT));

        float inset = s(4.0f);
        float gearWidth = s(GEAR_SLOT_WIDTH);
        int named = namedTabs();
        float slotWidth = named > 0 ? (width - inset * 2.0f - gearWidth) / named : 0.0f;

        int slot = 0;
        for (int index = 0; index < tabs.size(); index++) {
            Tab current = tabs.get(index);
            if (current.iconOnly) {
                current.bounds = new Rect(bar.right() - inset - gearWidth, bar.y() + s(3.0f), gearWidth,
                        bar.height() - s(6.0f));
            } else {
                current.bounds = new Rect(bar.x() + inset + slot * slotWidth, bar.y() + s(3.0f), slotWidth,
                        bar.height() - s(6.0f));
                if (index == tab) {
                    indicator = Maths.approach(indicator, slot, TAB_SPEED, deltaTime);
                }
                slot++;
            }
            boolean hovered = pointable() && current.bounds.contains(mouseX, mouseY);
            if (current.iconOnly) {
                gearHover = Maths.approach(gearHover, hovered ? 1.0f : 0.0f, HOVER_SPEED, deltaTime);
            }
            current.hover = Maths.approach(current.hover, hovered ? 1.0f : 0.0f, HOVER_SPEED, deltaTime);
        }
        indicatorAlpha = Maths.approach(indicatorAlpha, tabs.get(tab).iconOnly ? 0.0f : 1.0f, 18.0f, deltaTime);
        selectorAlpha = Maths.approach(selectorAlpha, tabs.get(tab).iconOnly ? 0.0f : 1.0f, 18.0f, deltaTime);

        float chipTarget = subcategory.ordinal();
        boolean rightward = chipTarget >= chipLeft;
        chipLeft = Maths.approach(chipLeft, chipTarget, rightward ? CHIP_TRAIL_SPEED : CHIP_LEAD_SPEED, deltaTime);
        chipRight = Maths.approach(chipRight, chipTarget + 1.0f, rightward ? CHIP_LEAD_SPEED : CHIP_TRAIL_SPEED,
                deltaTime);

        subpageAnim = Maths.approach(subpageAnim, subpage != null ? 1.0f : 0.0f, SUBPAGE_SPEED, deltaTime);
        boolean backHovered = pointable() && subpage != null && backButton().contains(mouseX, mouseY);
        backHover = Maths.approach(backHover, backHovered ? 1.0f : 0.0f, HOVER_SPEED, deltaTime);

        for (Row row : rows) {
            float target = 0.0f;
            if (row.kind == RowKind.MODULE) {
                target = row.module.enabled() ? 1.0f : 0.0f;
            } else if (row.kind == RowKind.FLAG) {
                target = row.setting.bool() ? 1.0f : 0.0f;
            }
            row.on = Maths.approach(row.on, target, ON_SPEED, deltaTime);
        }

        content = subpage != null ? subpageHeight(subpage) : listHeight();

        float cardY = bar.bottom() + s(PANEL_GAP);
        float header = s(HEADER_HEIGHT);
        float maxContentHeight = 7.5f * (s(ROW_HEIGHT) + s(ROW_GAP));
        float maxHeight = Math.min(header + maxContentHeight + s(8.0f), Math.max(header + s(40.0f), screenH - cardY - s(24.0f)));
        float wanted = header + content + s(8.0f);
        float height = Math.min(maxHeight, Math.max(header + s(40.0f), wanted));

        if (cardHeight <= 0.0f || !visible()) {
            cardHeight = height;
        } else {
            cardHeight = Maths.approach(cardHeight, height, CARD_SPEED, deltaTime);
        }

        card = new Rect(x, cardY, width, cardHeight);
        body = new Rect(card.x() + s(CARD_PAD_X), card.y() + header + s(4.0f), card.width() - s(CARD_PAD_X) * 2.0f,
                Math.max(s(20.0f), card.height() - header - s(6.0f)));

        scroll = Maths.clamp(scroll, 0.0f, Math.max(0.0f, content - body.height()));

        float selectorWidth = s(SELECTOR_SLOT) * SUBCATEGORIES.length + s(SELECTOR_INSET) * 2.0f;
        selector = new Rect(card.right() - s(SELECTOR_MARGIN) - selectorWidth, card.y() + s(SELECTOR_MARGIN),
                selectorWidth, header - s(SELECTOR_MARGIN) * 2.0f);
        boolean selectorLive = pointable() && subpage == null && !tabs.get(tab).iconOnly;
        for (int index = 0; index < SUBCATEGORIES.length; index++) {
            float slotX = selector.x() + s(SELECTOR_INSET) + index * s(SELECTOR_SLOT);
            boolean hovered = selectorLive && selector.contains(mouseX, mouseY) && mouseX >= slotX
                    && mouseX < slotX + s(SELECTOR_SLOT);
            subcategoryHover[index] = Maths.approach(subcategoryHover[index], hovered ? 1.0f : 0.0f, HOVER_SPEED,
                    deltaTime);
        }

        if (subpage != null) {
            layoutSubpage(subpage, deltaTime);
        } else {
            layoutList(deltaTime);
        }

        advanceReveal(deltaTime);

        if (activeSlider != null && mouseDown && activeSlider.control.width() > 0.0f) {
            Rect track = activeSlider.control;
            activeSlider.setting.setNormalised((mouseX - track.x()) / track.width());
        }

        if (activePicker != null && mouseDown && activePicker.setting != null) {
            SettingRow picker = activePicker;
            Rect field = colourField(colourEditor(picker));
            Color current = picker.setting.colour();
            if (pickerAxis == 0) {
                float saturation = Maths.clamp((mouseX - field.x()) / Math.max(field.width(), 1.0f), 0.0f, 1.0f);
                float value = 1.0f - Maths.clamp((mouseY - field.y()) / Math.max(field.height(), 1.0f), 0.0f, 1.0f);
                picker.setting.setColour(Color.hsv(picker.hue, saturation, value, current.a()));
            } else {
                picker.hue = Maths.clamp((mouseX - field.x()) / Math.max(field.width(), 1.0f), 0.0f, 1.0f);
                float maximum = Math.max(current.r(), Math.max(current.g(), current.b()));
                float minimum = Math.min(current.r(), Math.min(current.g(), current.b()));
                float saturation = maximum <= 0.0f ? 1.0f : (maximum - minimum) / maximum;
                picker.setting.setColour(Color.hsv(picker.hue, saturation, maximum <= 0.0f ? 1.0f : maximum,
                        current.a()));
            }
        }
    }

    private int namedTabs() {
        int named = 0;
        for (Tab current : tabs) {
            if (!current.iconOnly) {
                named++;
            }
        }
        return named;
    }

    private void layoutList(float deltaTime) {
        float top = body.y() + s(ROW_GAP) - scroll;
        int ordinal = 0;
        for (Row row : shownRows) {

            row.enter = entranceAt(ordinal++);
            float drop = (1.0f - row.enter) * s(ROW_RISE);
            float height = rowHeight(row);
            row.bounds = new Rect(body.x(), top - drop, body.width(), height);
            top += height + s(ROW_GAP);
            if (row.kind == RowKind.SECTION) {
                continue;
            }

            boolean hasPill = row.kind == RowKind.MODULE || row.kind == RowKind.FLAG;
            float chevronCentre = row.bounds.right()
                    - (hasPill ? s(PILL_MARGIN + PILL_WIDTH + CHEVRON_BOX) : s(22.0f));
            row.chevron = (row.kind == RowKind.FLAG || row.kind == RowKind.SLIDER) ? Rect.EMPTY
                    : new Rect(chevronCentre - s(14.0f), row.bounds.y(), s(28.0f), row.bounds.height());

            if (row.kind == RowKind.SLIDER) {
                float sliderWidth = s(80.0f);
                float sliderHeight = s(SLIDER_TRACK);
                float trackX = row.bounds.right() - s(PILL_MARGIN) - sliderWidth;
                float trackY = row.bounds.centerY() + s(3.0f);
                row.sliderControl = new Rect(trackX, trackY - sliderHeight * 0.5f, sliderWidth, sliderHeight);
                if (row.sliderRow != null) {
                    row.sliderRow.control = row.sliderControl;
                    row.sliderRow.knob = Maths.approach(row.sliderRow.knob, row.setting.normalised(), 20.0f, deltaTime);
                }
            }

            boolean hovered = pointable() && body.contains(mouseX, mouseY) && row.bounds.contains(mouseX, mouseY);
            row.hover = Maths.approach(row.hover, hovered ? 1.0f : 0.0f, HOVER_SPEED, deltaTime);
        }
    }

    private void layoutSubpage(Row row, float deltaTime) {
        float shift = s(10.0f) * (1.0f - subpageAnim);
        float top = body.y() + s(ROW_GAP) - scroll;

        if (row.kind == RowKind.CONFIGS) {
            layoutConfigs(top, shift, deltaTime);
            return;
        }

        int ordinal = 0;
        for (SettingRow setting : row.settings) {
            if (setting.setting != null && !setting.setting.visible()) {
                setting.bounds = Rect.EMPTY;
                setting.control = Rect.EMPTY;
                continue;
            }
            setting.enter = entranceAt(ordinal++);
            setting.expand = Maths.approach(setting.expand, setting.expanded ? 1.0f : 0.0f, EXPAND_SPEED, deltaTime);
            setting.picker = Maths.approach(setting.picker, setting.pickerOpen ? 1.0f : 0.0f, 16.0f, deltaTime);
            boolean number = setting.setting != null && setting.setting.type == Setting.Type.NUMBER;
            if (number) {
                setting.knob = Maths.approach(setting.knob, setting.setting.normalised(), 20.0f, deltaTime);
            }

            float height = settingHeight(setting);
            float drop = (1.0f - setting.enter) * s(ROW_RISE);
            setting.bounds = new Rect(body.x() + shift, top - drop, body.width() - shift, height);

            boolean hovered = pointable() && body.contains(mouseX, mouseY)
                    && setting.bounds.contains(mouseX, mouseY);
            setting.hover = Maths.approach(setting.hover, hovered ? 1.0f : 0.0f, HOVER_SPEED, deltaTime);

            setting.control = number
                    ? new Rect(setting.bounds.x() + s(SETTING_PAD_X), setting.bounds.y() + s(SETTING_HEIGHT) - s(11.0f),
                            setting.bounds.width() - s(SETTING_PAD_X) * 2.0f, s(SLIDER_TRACK))
                    : setting.bounds;
            top += height + s(ROW_GAP);
        }
    }

    private void layoutConfigs(float top, float shift, float deltaTime) {
        float left = body.x() + shift;
        float width = body.width() - shift;
        float buttonWidth = s(84.0f);

        nameField = new Rect(left, top, width - buttonWidth - s(ROW_GAP), s(SETTING_HEIGHT));
        saveButton = new Rect(nameField.right() + s(ROW_GAP), top, buttonWidth, s(SETTING_HEIGHT));
        boolean nameHovered = pointable() && nameField.contains(mouseX, mouseY);
        boolean saveHovered = pointable() && saveButton.contains(mouseX, mouseY);
        nameHover = Maths.approach(nameHover, nameHovered ? 1.0f : 0.0f, HOVER_SPEED, deltaTime);
        saveHover = Maths.approach(saveHover, saveHovered ? 1.0f : 0.0f, HOVER_SPEED, deltaTime);
        top += s(SETTING_HEIGHT) + s(ROW_GAP);

        for (ConfigFile entry : configs) {
            entry.bounds = new Rect(left, top, width, s(SETTING_HEIGHT));
            entry.remove = new Rect(entry.bounds.right() - s(SETTING_PAD_X) - s(62.0f), entry.bounds.y() + s(7.0f),
                    s(62.0f), s(SETTING_HEIGHT) - s(14.0f));
            boolean hovered = pointable() && body.contains(mouseX, mouseY) && entry.bounds.contains(mouseX, mouseY);
            boolean removeHovered = hovered && entry.remove.contains(mouseX, mouseY);
            entry.hover = Maths.approach(entry.hover, hovered ? 1.0f : 0.0f, HOVER_SPEED, deltaTime);
            entry.removeHover = Maths.approach(entry.removeHover, removeHovered ? 1.0f : 0.0f, HOVER_SPEED, deltaTime);
            top += s(SETTING_HEIGHT) + s(ROW_GAP);
        }
    }

    private boolean visibleInBody(Rect bounds) {
        return bounds.bottom() > body.y() - 1.0f && bounds.y() < body.bottom() + 1.0f;
    }

    private boolean bodyNeedsClip() {
        if (scroll > 0.5f) {
            return true;
        }
        if (content > body.height() - 0.5f) {
            return true;
        }
        return pageEnter < 0.999f || pageFade < 0.999f;
    }

    private String rowBind(Row row) {
        int key = row.module.key();
        if (row.bindKey != key || row.bindListening != row.binding) {
            row.bindKey = key;
            row.bindListening = row.binding;
            row.bindShown = bindLabel(key, row.binding);
        }
        return row.bindShown;
    }

    private String rowDescription(Row row, float limit) {
        if (row.descLimit != limit || row.descScale != scale) {
            row.descLimit = limit;
            row.descScale = scale;
            row.descShown = gfx.elide(DESC_FONT, row.description, fontEm(DESC_FONT, s(DESC_BOX)), limit);
        }
        return row.descShown;
    }

    public void render(Context context) {
        build();
        layout(context);

        if (slide <= 0.004f || tabs.isEmpty() || bar.width() <= 0.0f) {
            return;
        }

        Backdrop backdrop = Client.backdrop();
        if (blurEnabled() && backdrop.valid()) {
            Rect full = new Rect(0.0f, 0.0f, screenW, screenH);
            gfx.image(full, new Rect(0.0f, 1.0f, 1.0f, -1.0f), backdrop.texture(), fade(Color.WHITE), 0.0f);
            gfx.rect(full, Color.rgb(0x000000, 0.28f * alpha * Math.min(1.0f, blurStrength)), 0.0f);
        }

        drawTabBar();
        drawCard();
    }

    private void drawTabBar() {

        Rect frame = reveal.bar;
        float radius = reveal.barRadius;

        if (reveal.glow > 0.002f) {
            gfx.glow(frame, Theme.accent().withAlpha(0.30f * reveal.glow), s(22.0f) * reveal.glow, radius);
        }

        gfx.rect(frame, fade(TAB_BAR_BG), radius);
        gfx.outline(frame, fade(CARD_BORDER), Math.max(1.0f, s(1.0f)), radius);

        if (reveal.label > 0.002f) {
            drawRevealLabel();
        }

        if (reveal.barContent <= 0.002f) {
            return;
        }
        int named = namedTabs();
        if (named == 0) {
            return;
        }

        float saved = alpha;
        alpha *= reveal.barContent;
        float inset = s(4.0f);
        float slotWidth = (bar.width() - inset * 2.0f - s(GEAR_SLOT_WIDTH)) / named;

        if (indicatorAlpha > 0.001f) {
            Rect chip = new Rect(bar.x() + inset + indicator * slotWidth, bar.y() + s(3.0f), slotWidth,
                    bar.height() - s(6.0f));
            gfx.rect(chip, fade(TAB_CHIP_BG, indicatorAlpha), s(9.0f));

            float underlineWidth = slotWidth * 0.78f;
            float underlineHeight = s(2.5f);
            gfx.rect(new Rect(chip.x() + (slotWidth - underlineWidth) * 0.5f, chip.bottom() - underlineHeight,
                    underlineWidth, underlineHeight), accent(indicatorAlpha), underlineHeight * 0.5f);
        }

        for (int index = 0; index < tabs.size(); index++) {
            Tab current = tabs.get(index);
            if (current.iconOnly) {

                float box = s(ICON_BOX);
                Color colour = Color.mix(Theme.accent(), Color.WHITE, gearHover * 0.35f);
                gfx.icon(current.icon, new Rect(current.bounds.centerX() - box * 0.5f,
                        current.bounds.centerY() - box * 0.5f, box, box), fade(colour));
                continue;
            }
            Color colour = index == tab ? TEXT_PRIMARY : Color.mix(TEXT_MUTED, TEXT_PRIMARY, current.hover * 0.7f);
            drawText(LABEL_FONT, current.label, current.bounds.centerX(),
                    current.bounds.centerY() - s(LABEL_BOX) * 0.5f, s(LABEL_BOX), fade(colour), Align.CENTER);
        }
        alpha = saved;
    }

    private void drawRevealLabel() {
        refreshBrand();
        float box = s(HEADING_BOX);
        Rect frame = reveal.bar;
        drawText(HEADING_FONT, brandText, frame.centerX() - brandWidth * 0.5f, frame.centerY() - box * 0.5f, box,
                Theme.accent().withAlpha(alpha * reveal.label));
    }

    private void drawCard() {
        if (!reveal.cardVisible) {
            return;
        }

        Rect frame = reveal.card;
        float cardRadius = Math.min(s(CARD_RADIUS), frame.height() * 0.5f);
        gfx.rect(frame, fade(CARD_BG), cardRadius);
        gfx.outline(frame, fade(CARD_BORDER), Math.max(1.0f, s(1.0f)), cardRadius);

        boolean unfolding = frame.height() < card.height() - 0.5f;
        if (unfolding) {
            gfx.pushClip(frame);
        }

        float saved = alpha;
        alpha *= reveal.cardContent;
        if (subpage != null) {
            drawSubpageHeader(subpage);
        } else {
            drawCardHeader(tabs.get(shownTab));
        }

        alpha *= pageFade;
        boolean clipBody = bodyNeedsClip();
        if (clipBody) {
            gfx.pushClip(body);
        }
        if (subpage == null) {
            for (Row row : shownRows) {
                drawRow(row);
            }
        } else if (subpage.kind == RowKind.CONFIGS) {
            drawConfigs();
        } else {
            for (SettingRow setting : subpage.settings) {
                if (setting.bounds.width() > 0.0f) {
                    drawSetting(subpage, setting);
                }
            }
        }
        if (clipBody) {
            gfx.popClip();
        }
        if (content > body.height()) {
            float trackHeight = body.height();
            float thumbHeight = Math.max(s(24.0f), trackHeight * (trackHeight / content));
            float maxScroll = Math.max(1.0f, content - trackHeight);
            float thumbY = body.y() + (scroll / maxScroll) * (trackHeight - thumbHeight);
            float thumbWidth = s(3.0f);
            float thumbX = body.right() - thumbWidth + s(6.0f);
            gfx.rect(new Rect(thumbX, thumbY, thumbWidth, thumbHeight), fade(Color.rgb(0xFFFFFF, 0.22f)), s(1.5f));
        }
        alpha = saved;

        if (unfolding) {
            gfx.popClip();
        }
    }

    private void drawCardHeader(Tab page) {
        float height = s(HEADER_HEIGHT);
        float centreY = card.y() + height * 0.5f;
        float cursorX = card.x() + s(16.0f);

        float saved = alpha;
        alpha = saved * (sectionHeld ? 1.0f : pageFade);

        float iconBox = s(16.0f);
        gfx.icon(page.icon, new Rect(cursorX, centreY - iconBox * 0.5f, iconBox, iconBox), accent());
        cursorX += iconBox + s(10.0f);

        float tracking = s(TRACKING);
        if (page.titleScale != scale) {
            page.titleScale = scale;
            page.titleWidth = trackedWidth(HEADING_FONT, page.title, s(HEADING_BOX), tracking);
        }
        float textTop = centreY - s(HEADING_BOX) * 0.5f;
        drawTracked(HEADING_FONT, page.title, cursorX, textTop, s(HEADING_BOX), accent(), tracking);
        cursorX += page.titleWidth;

        if (!page.iconOnly) {
            refreshPath();
            cursorX += s(10.0f);
            drawTracked(HEADING_FONT, PATH_ARROW, cursorX, textTop, s(HEADING_BOX), fade(TEXT_FAINT), tracking);
            cursorX += arrowWidth + s(10.0f);
            alpha = saved * pageFade;
            int shown = shownSubcategory.ordinal();
            drawTracked(HEADING_FONT, SUBCATEGORY_TITLES[shown], cursorX, textTop, s(HEADING_BOX),
                    fade(TEXT_PRIMARY), tracking);
            cursorX += subcategoryTitleWidths[shown];
        }
        alpha = saved * pageFade;
        cursorX += s(14.0f);

        gfx.line(cursorX, centreY - s(7.0f), cursorX, centreY + s(7.0f), Math.max(1.0f, s(1.0f)), fade(SEPARATOR));
        cursorX += s(14.0f);

        if (page.countFor != shownEntries) {
            page.countFor = shownEntries;
            page.count = shownEntries + " " + page.countNoun;
        }
        drawText(MICRO_FONT, page.count, cursorX, centreY - s(MICRO_BOX) * 0.5f, s(MICRO_BOX), fade(TEXT_FAINT));
        alpha = saved;

        if (selectorAlpha > 0.002f) {
            drawSelector();
        }

        gfx.line(card.x(), card.y() + height, card.right(), card.y() + height, Math.max(1.0f, s(1.0f)),
                fade(SEPARATOR));
    }

    private void refreshPath() {
        if (pathScale == scale) {
            return;
        }
        pathScale = scale;
        float tracking = s(TRACKING);
        arrowWidth = trackedWidth(HEADING_FONT, PATH_ARROW, s(HEADING_BOX), tracking);
        for (int index = 0; index < SUBCATEGORIES.length; index++) {
            subcategoryTitleWidths[index] = trackedWidth(HEADING_FONT, SUBCATEGORY_TITLES[index], s(HEADING_BOX),
                    tracking);
        }
    }

    private void drawSelector() {
        float saved = alpha;
        alpha *= selectorAlpha;

        float radius = s(SELECTOR_RADIUS);
        gfx.rect(selector, fade(TAB_BAR_BG), radius);
        gfx.outline(selector, fade(CARD_BORDER), Math.max(1.0f, s(1.0f)), radius);

        float inset = s(SELECTOR_INSET);
        float slot = s(SELECTOR_SLOT);
        float left = selector.x() + inset;
        Rect chip = new Rect(left + chipLeft * slot, selector.y() + inset, (chipRight - chipLeft) * slot,
                selector.height() - inset * 2.0f);
        gfx.rect(chip, fade(TAB_CHIP_BG), radius - inset);

        float dividerHalf = s(6.0f);
        for (int index = 1; index < SUBCATEGORIES.length; index++) {
            float clearance = Math.max(chipLeft - index, index - chipRight);
            float visibility = Maths.clamp(clearance * 2.0f, 0.0f, 1.0f);
            if (visibility > 0.002f) {
                float x = left + index * slot;
                gfx.line(x, selector.centerY() - dividerHalf, x, selector.centerY() + dividerHalf,
                        Math.max(1.0f, s(1.0f)), fade(SEPARATOR, visibility));
            }
        }

        float box = s(SELECTOR_LABEL_BOX);
        for (int index = 0; index < SUBCATEGORIES.length; index++) {
            Color colour = index == subcategory.ordinal() ? TEXT_PRIMARY
                    : Color.mix(TEXT_MUTED, TEXT_PRIMARY, subcategoryHover[index] * 0.7f);
            drawText(LABEL_FONT, SUBCATEGORIES[index].displayName, left + (index + 0.5f) * slot,
                    selector.centerY() - box * 0.5f, box, fade(colour), Align.CENTER);
        }
        alpha = saved;
    }

    private void drawSubpageHeader(Row row) {
        float height = s(HEADER_HEIGHT);
        float centreY = card.y() + height * 0.5f;

        Rect back = backButton();
        if (backHover > 0.001f) {
            gfx.rect(back, fade(Color.rgb(0xFFFFFF, 20 / 255.0f), backHover), s(7.0f));
        }
        float box = s(CHEVRON_BOX);
        gfx.icon(Icon.CHEVRON_LEFT, new Rect(back.centerX() - box * 0.5f, back.centerY() - box * 0.5f, box, box),
                fade(Color.mix(TEXT_MUTED, TEXT_PRIMARY, backHover)));

        drawTracked(HEADING_FONT, row.title, back.right() + s(8.0f), centreY - s(HEADING_BOX) * 0.5f, s(HEADING_BOX),
                accent(), s(TRACKING));

        gfx.line(card.x(), card.y() + height, card.right(), card.y() + height, Math.max(1.0f, s(1.0f)),
                fade(SEPARATOR));
    }

    private void drawPill(Rect bounds, float on) {
        float radius = bounds.height() * 0.5f;
        if (on > 0.001f) {
            gfx.rect(bounds.inset(-s(2.0f)), accent(0.16f * on), radius + s(2.0f));
        }
        gfx.rect(bounds, fade(Color.mix(TOGGLE_OFF, Theme.accent(), on)), radius);

        float knobRadius = radius - s(2.5f);
        float left = bounds.x() + s(2.5f) + knobRadius;
        float right = bounds.right() - s(2.5f) - knobRadius;
        float centreX = left + (right - left) * on;
        gfx.rect(new Rect(centreX - knobRadius, bounds.centerY() - knobRadius, knobRadius * 2.0f, knobRadius * 2.0f),
                fade(Color.mix(KNOB_OFF, KNOB_ON, on)), knobRadius);
    }

    private void drawChip(float x, float centreY, String text, Color background, Color textColour) {
        float padX = s(6.0f);
        float box = s(MICRO_BOX);
        float chipHeight = box + s(5.0f);
        float width = textWidth(MICRO_FONT, text, box) + padX * 2.0f;
        Rect chip = new Rect(x, centreY - chipHeight * 0.5f, width, chipHeight);
        gfx.rect(chip, fade(background), s(4.0f));
        drawText(MICRO_FONT, text, chip.x() + padX, chip.centerY() - box * 0.5f, box, fade(textColour));
    }

    private void drawTrailingChip(float rightEdge, float centreY, String text, Color textColour) {
        float chipWidth = textWidth(MICRO_FONT, text, s(MICRO_BOX)) + s(12.0f);
        drawChip(rightEdge - chipWidth, centreY, text, CHIP_BG, textColour);
    }

    private void drawRow(Row row) {
        if (row.bounds.width() <= 0.0f || row.enter <= 0.002f) {
            return;
        }

        if (!visibleInBody(row.bounds)) {
            return;
        }

        float saved = alpha;
        alpha *= row.enter;

        Rect bounds = row.bounds;
        if (row.kind == RowKind.SECTION) {
            float iconBox = s(13.0f);
            float centreY = bounds.bottom() - s(10.0f);
            gfx.icon(row.icon, new Rect(bounds.x() + s(6.0f), centreY - iconBox * 0.5f, iconBox, iconBox),
                    fade(TEXT_FAINT));
            drawTracked(HEADING_FONT, row.title, bounds.x() + s(6.0f) + iconBox + s(8.0f),
                    centreY - s(HEADING_BOX) * 0.5f, s(HEADING_BOX), fade(TEXT_MUTED), s(TRACKING));
            alpha = saved;
            return;
        }
        float height = bounds.height();
        boolean hasPill = row.kind == RowKind.MODULE || row.kind == RowKind.FLAG;

        Color background = Color.mix(ROW_BG, ROW_BG_ON, row.on);
        background = Color.mix(background, HOVER_WASH, row.hover * 0.6f);
        gfx.rect(bounds, fade(background), s(ROW_RADIUS));

        if (row.on > 0.001f) {
            float barInset = s(7.0f);
            gfx.rect(new Rect(bounds.x(), bounds.y() + barInset, s(3.0f), height - barInset * 2.0f), accent(row.on),
                    s(1.5f));
        }

        float iconBox = s(ICON_BOX);
        gfx.icon(row.icon, new Rect(bounds.x() + s(ROW_ICON_CENTRE) - iconBox * 0.5f,
                bounds.centerY() - iconBox * 0.5f, iconBox, iconBox), fade(Color.mix(TEXT_MUTED, ICON_ON, row.on)));

        float textX = bounds.x() + s(ROW_TEXT_X);
        float nameTop = bounds.y() + s(ROW_NAME_TOP);
        float nameEnd = drawText(LABEL_FONT, row.name, textX, nameTop, s(LABEL_BOX), fade(TEXT_PRIMARY));

        if (row.kind == RowKind.MODULE) {
            drawChip(nameEnd + s(9.0f), nameTop + s(LABEL_BOX) * 0.5f, rowBind(row), CHIP_BG,
                    row.binding ? Theme.accent() : CHIP_TEXT);
        }

        if (!row.description.isEmpty()) {
            float limit = bounds.right() - s(PILL_MARGIN) - textX;
            if (hasPill) {
                limit -= s(PILL_WIDTH);
            }
            if (row.kind == RowKind.SLIDER) {
                limit -= s(85.0f);
            }
            if (row.chevron.width() > 0.0f) {
                limit -= s(28.0f);
            }
            drawText(DESC_FONT, rowDescription(row, Math.max(limit, s(30.0f))), textX, bounds.y() + s(ROW_DESC_TOP),
                    s(DESC_BOX), fade(TEXT_MUTED));
        }

        if (hasPill) {
            float pillWidth = s(PILL_WIDTH);
            float pillHeight = s(PILL_HEIGHT);
            drawPill(new Rect(bounds.right() - s(PILL_MARGIN) - pillWidth, bounds.centerY() - pillHeight * 0.5f,
                    pillWidth, pillHeight), row.on);
        }

        if (row.kind == RowKind.SLIDER && row.setting != null) {
            drawText(MICRO_FONT, formatNumber(row.setting), bounds.right() - s(PILL_MARGIN), bounds.centerY() - s(5.0f), s(MICRO_BOX), fade(CHIP_TEXT), Align.RIGHT);
            Rect track = row.sliderControl;
            if (track.width() > 0.0f) {
                gfx.rect(track, fade(TRACK_BG), track.height() * 0.5f);
                float fill = track.width() * Maths.clamp(row.sliderRow != null ? row.sliderRow.knob : row.setting.normalised(), 0.0f, 1.0f);
                if (fill > 0.5f) {
                    gfx.rect(new Rect(track.x(), track.y(), fill, track.height()), accent(), track.height() * 0.5f);
                }
                float knobRadius = s(5.0f);
                gfx.rect(new Rect(track.x() + fill - knobRadius, track.centerY() - knobRadius, knobRadius * 2.0f,
                        knobRadius * 2.0f), fade(KNOB_ON), knobRadius);
            }
        }

        if (row.chevron.width() > 0.0f) {
            float box = s(CHEVRON_BOX);
            gfx.icon(Icon.CHEVRON_RIGHT, new Rect(row.chevron.centerX() - box * 0.5f,
                    row.chevron.centerY() - box * 0.5f, box, box), fade(Color.mix(TEXT_FAINT, TEXT_PRIMARY, row.hover)));
        }
        alpha = saved;
    }

    private void drawSetting(Row owner, SettingRow setting) {
        if (setting.enter <= 0.002f) {
            return;
        }

        if (!visibleInBody(setting.bounds)) {
            return;
        }
        float saved = alpha;
        alpha *= setting.enter;

        Rect bounds = setting.bounds;
        Rect line = new Rect(bounds.x(), bounds.y(), bounds.width(), s(SETTING_HEIGHT));
        gfx.rect(line, fade(Color.mix(ROW_BG, HOVER_WASH, setting.hover * 0.6f)), s(ROW_RADIUS));

        float labelBox = s(13.0f);
        float labelX = line.x() + s(SETTING_PAD_X);
        float labelTop = line.centerY() - labelBox * 0.5f;
        float rightEdge = line.right() - s(SETTING_PAD_X);

        Setting value = setting.setting;
        if (value == null) {
            drawText(LABEL_FONT, "Keybind", labelX, labelTop, labelBox, fade(TEXT_PRIMARY));
            drawTrailingChip(rightEdge, line.centerY(), rowBind(owner), owner.binding ? Theme.accent() : CHIP_TEXT);
            alpha = saved;
            return;
        }

        switch (value.type) {
            case BOOLEAN -> {
                drawText(LABEL_FONT, value.name, labelX, labelTop, labelBox, fade(TEXT_PRIMARY));
                float pillWidth = s(32.0f);
                float pillHeight = s(17.0f);
                drawPill(new Rect(rightEdge - pillWidth, line.centerY() - pillHeight * 0.5f, pillWidth, pillHeight),
                        value.bool() ? 1.0f : 0.0f);
            }
            case NUMBER -> {
                drawText(LABEL_FONT, value.name, labelX, line.y() + s(8.0f), labelBox, fade(TEXT_PRIMARY));
                drawText(MICRO_FONT, formatNumber(value), rightEdge, line.y() + s(9.5f), s(MICRO_BOX), fade(CHIP_TEXT),
                        Align.RIGHT);
                Rect track = setting.control;
                if (track.width() > 0.0f) {
                    gfx.rect(track, fade(TRACK_BG), track.height() * 0.5f);
                    float fill = track.width() * Maths.clamp(setting.knob, 0.0f, 1.0f);
                    if (fill > 0.5f) {
                        gfx.rect(new Rect(track.x(), track.y(), fill, track.height()), accent(), track.height() * 0.5f);
                    }
                    float knobRadius = s(5.0f);
                    gfx.rect(new Rect(track.x() + fill - knobRadius, track.centerY() - knobRadius, knobRadius * 2.0f,
                            knobRadius * 2.0f), fade(KNOB_ON), knobRadius);
                }
            }
            case CHOICE -> {
                drawText(LABEL_FONT, value.name, labelX, labelTop, labelBox, fade(TEXT_PRIMARY));
                drawTrailingChip(rightEdge, line.centerY(), value.choiceName(), CHIP_TEXT);
                if (setting.expand > 0.01f) {
                    drawChoiceItems(setting);
                }
            }
            case KEY -> {
                drawText(LABEL_FONT, value.name, labelX, labelTop, labelBox, fade(TEXT_PRIMARY));
                drawTrailingChip(rightEdge, line.centerY(), bindLabel(value.key(), setting.listeningForKey),
                        setting.listeningForKey ? Theme.accent() : CHIP_TEXT);
            }
            case COLOUR -> {
                drawText(LABEL_FONT, value.name, labelX, labelTop, labelBox, fade(TEXT_PRIMARY));
                float swatchWidth = s(30.0f);
                float swatchHeight = s(16.0f);
                Rect swatch = new Rect(rightEdge - swatchWidth, line.centerY() - swatchHeight * 0.5f, swatchWidth,
                        swatchHeight);
                gfx.rect(swatch, fade(value.colour()), s(4.0f));
                gfx.outline(swatch, fade(CARD_BORDER), Math.max(1.0f, s(1.0f)), s(4.0f));
                if (setting.picker > 0.01f) {
                    drawColourEditor(setting);
                }
            }
        }
        alpha = saved;
    }

    private void drawChoiceItems(SettingRow setting) {
        List<String> choices = setting.setting.choices();
        float itemHeight = s(CHOICE_ITEM_HEIGHT);
        float full = choices.size() * itemHeight;
        Rect area = new Rect(setting.bounds.x(), setting.bounds.y() + s(SETTING_HEIGHT), setting.bounds.width(),
                full * Maths.clamp(setting.expand, 0.0f, 1.0f));
        if (area.height() <= 0.5f) {
            return;
        }
        gfx.pushClip(area);
        for (int i = 0; i < choices.size(); i++) {
            float itemY = area.y() + i * itemHeight;
            boolean hovered = pointable() && mouseX >= area.x() && mouseX <= area.right() && mouseY >= itemY
                    && mouseY <= itemY + itemHeight;
            if (hovered) {
                gfx.rect(new Rect(area.x() + s(6.0f), itemY + s(1.0f), area.width() - s(12.0f), itemHeight - s(2.0f)),
                        fade(HOVER_WASH, 0.7f), s(5.0f));
            }
            Color colour = i == setting.setting.choice() ? Theme.accent() : hovered ? TEXT_PRIMARY : TEXT_MUTED;
            drawText(DESC_FONT, choices.get(i), area.x() + s(SETTING_PAD_X + 10.0f),
                    itemY + (itemHeight - s(DESC_BOX)) * 0.5f, s(DESC_BOX), fade(colour));
        }
        gfx.popClip();
    }

    private void drawColourEditor(SettingRow setting) {
        Rect area = colourEditor(setting);
        gfx.pushClip(new Rect(area.x(), area.y(), area.width(), area.height() * Maths.clamp(setting.picker, 0.0f, 1.0f)));

        Color current = setting.setting.colour();
        float maximum = Math.max(current.r(), Math.max(current.g(), current.b()));
        float minimum = Math.min(current.r(), Math.min(current.g(), current.b()));
        float saturation = maximum <= 0.0f ? 0.0f : (maximum - minimum) / maximum;

        Rect field = colourField(area);
        gfx.rectHorizontal(field, fade(Color.WHITE), fade(Color.hsv(setting.hue, 1.0f, 1.0f)), s(6.0f));
        gfx.rectVertical(field, new Color(0.0f, 0.0f, 0.0f, 0.0f), fade(Color.rgb(0x000000)), s(6.0f));
        gfx.ring(field.x() + field.width() * saturation, field.y() + field.height() * (1.0f - maximum), s(5.0f),
                s(2.0f), 1.0f, fade(Color.WHITE));

        Rect strip = new Rect(field.x(), field.bottom() + s(EDITOR_GAP), field.width(), s(EDITOR_STRIP));
        int segments = 6;
        float segmentWidth = strip.width() / segments;
        for (int index = 0; index < segments; index++) {
            float from = (float) index / segments;
            float to = (float) (index + 1) / segments;
            gfx.rectHorizontal(new Rect(strip.x() + segmentWidth * index, strip.y(), segmentWidth, strip.height()),
                    fade(Color.hsv(from, 1.0f, 1.0f)), fade(Color.hsv(to - 0.0001f, 1.0f, 1.0f)), 0.0f);
        }
        gfx.rect(new Rect(strip.x() + strip.width() * setting.hue - s(1.5f), strip.y() - s(2.0f), s(3.0f),
                strip.height() + s(4.0f)), fade(Color.WHITE), s(1.5f));

        Rect preview = new Rect(field.x(), strip.bottom() + s(EDITOR_GAP), field.width(), s(EDITOR_SWATCH));
        gfx.rect(preview, fade(current), s(5.0f));
        drawText(MICRO_FONT, String.format("#%06X", current.packed() >>> 8), preview.centerX(),
                preview.centerY() - s(MICRO_BOX) * 0.5f, s(MICRO_BOX), fade(Color.rgb(0x000000, 0.7f)), Align.CENTER);

        gfx.popClip();
    }

    private void drawConfigs() {
        float labelBox = s(13.0f);
        float padX = s(SETTING_PAD_X);

        gfx.rect(nameField, fade(Color.mix(ROW_BG, HOVER_WASH, nameHover * 0.6f)), s(ROW_RADIUS));
        boolean empty = configName.isEmpty();
        float end = drawText(LABEL_FONT, empty ? "Config name" : configName, nameField.x() + padX,
                nameField.centerY() - labelBox * 0.5f, labelBox, fade(empty ? TEXT_FAINT : TEXT_PRIMARY));
        if (editingName) {
            float blink = 0.5f + 0.5f * (float) Math.sin(time * 6.0f);
            gfx.rect(new Rect(end + s(2.0f), nameField.centerY() - labelBox * 0.5f, s(1.5f), labelBox), accent(blink),
                    s(0.75f));
        }

        gfx.rect(saveButton, fade(Color.mix(ROW_BG, HOVER_WASH, 0.4f + saveHover * 0.6f)), s(ROW_RADIUS));
        drawText(LABEL_FONT, "Save", saveButton.centerX(), saveButton.centerY() - labelBox * 0.5f, labelBox,
                fade(Color.mix(TEXT_MUTED, TEXT_PRIMARY, saveHover)), Align.CENTER);

        for (int i = 0; i < configs.size(); i++) {
            ConfigFile entry = configs.get(i);
            if (entry.bounds.width() <= 0.0f) {
                continue;
            }
            boolean selected = i == selectedConfig;
            gfx.rect(entry.bounds, fade(Color.mix(ROW_BG, HOVER_WASH, entry.hover * 0.6f)), s(ROW_RADIUS));
            if (selected) {
                gfx.rect(new Rect(entry.bounds.x(), entry.bounds.y() + s(7.0f), s(3.0f),
                        entry.bounds.height() - s(14.0f)), accent(), s(1.5f));
            }
            drawText(LABEL_FONT, entry.name, entry.bounds.x() + padX, entry.bounds.centerY() - labelBox * 0.5f,
                    labelBox, fade(selected ? TEXT_PRIMARY : Color.mix(TEXT_MUTED, TEXT_PRIMARY, entry.hover)));

            gfx.rect(entry.remove, fade(Color.mix(CHIP_BG, Theme.NEGATIVE, entry.removeHover * 0.75f)), s(5.0f));
            drawText(MICRO_FONT, "DELETE", entry.remove.centerX(), entry.remove.centerY() - s(MICRO_BOX) * 0.5f,
                    s(MICRO_BOX), fade(Color.mix(CHIP_TEXT, TEXT_PRIMARY, entry.removeHover)), Align.CENTER);
        }

        if (!configStatus.isEmpty()) {
            float above = configs.isEmpty() ? nameField.bottom() : configs.get(configs.size() - 1).bounds.bottom();
            drawText(DESC_FONT, configStatus, body.x() + padX, above + s(ROW_GAP) + s(10.0f), s(DESC_BOX),
                    fade(TEXT_FAINT));
        }
    }
}
