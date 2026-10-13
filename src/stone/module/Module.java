package stone.module;

import stone.core.Color;
import stone.core.Log;
import stone.render.Icon;
import stone.ui.Client;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;

public abstract class Module {
    private static final float ANIMATION_RATE = 11.0f;
    protected static final Minecraft mc = Minecraft.getMinecraft();

    public final String name;
    public final String description;
    public final Category category;
    public final Subcategory subcategory;
    private Icon icon;
    private int key;
    private boolean enabled;
    private boolean running;
    private float animation;
    private final List<Setting> settings = new ArrayList<>();

    protected Module(String name, String description, Category category, Subcategory subcategory) {
        this(name, description, category, subcategory, 0);
    }

    protected Module(String name, String description, Category category, Subcategory subcategory, int defaultKey) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.subcategory = subcategory;
        this.icon = category.icon;
        this.key = defaultKey;
    }

    public Icon icon() {
        return icon;
    }

    protected void setIcon(Icon value) {
        icon = value;
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean value, Context context) {
        if (enabled == value) {
            return;
        }
        enabled = value;
        Log.debug("module", "%s %s", name, value ? "enabled" : "disabled");
        if (context != null) {
            run(value, context);
            if (announcesToggle()) {
                Client.hud().moduleToggled(this);
            }
        }
    }

    protected boolean announcesToggle() {
        return !holdsKey();
    }

    void sync(Context context) {
        if (running != enabled) {
            run(enabled, context);
        }
    }

    private void run(boolean value, Context context) {
        running = value;
        if (value) {
            onEnable(context);
        } else {
            onDisable(context);
        }
    }

    public void toggle(Context context) {
        setEnabled(!enabled, context);
    }

    public int key() {
        return key;
    }

    public void setKey(int value) {
        key = value;
    }

    public boolean holdsKey() {
        return false;
    }

    public void keyHeld(boolean held, Context context) {
        setEnabled(held, context);
    }

    public float animation() {
        return animation;
    }

    void advanceAnimation(float deltaTime) {
        float target = enabled ? 1.0f : 0.0f;
        animation += (target - animation) * (1.0f - (float) Math.exp(-ANIMATION_RATE * deltaTime));
        if (Math.abs(target - animation) < 0.002f) {
            animation = target;
        }
    }

    public String suffix() {
        return "";
    }

    public void onEnable(Context context) {}

    public void onDisable(Context context) {}

    public void onTick(Context context) {}

    public void onRender(Context context) {}

    public void onUpdate() {}

    public void guiUpdate() {}

    public List<Setting> settings() {
        return Collections.unmodifiableList(settings);
    }

    public Setting setting(String settingName) {
        for (Setting setting : settings) {
            if (setting.name.equals(settingName)) {
                return setting;
            }
        }
        return null;
    }

    protected void settingChanged(Setting setting) {}

    private Setting track(Setting setting) {
        setting.onChange = () -> settingChanged(setting);
        settings.add(setting);
        return setting;
    }

    public Setting addBoolean(String settingName, String settingDescription, boolean value) {
        Setting setting = new Setting(Setting.Type.BOOLEAN, settingName, settingDescription);
        setting.setBool(value);
        return track(setting);
    }

    public Setting addNumber(String settingName, String settingDescription, float value, float minimum,
                                float maximum, int decimals) {
        Setting setting = new Setting(Setting.Type.NUMBER, settingName, settingDescription);
        setting.minimum = minimum;
        setting.maximum = maximum;
        setting.decimals = decimals;
        setting.step = decimals <= 0 ? 1.0f : (float) Math.pow(0.1, decimals);
        setting.setNumber(value);
        return track(setting);
    }

    public Setting addChoice(String settingName, String settingDescription, List<String> choices, int index) {
        Setting setting = new Setting(Setting.Type.CHOICE, settingName, settingDescription);
        setting.choices = List.copyOf(choices);
        setting.setChoice(index);
        return track(setting);
    }

    public Setting addKey(String settingName, String settingDescription, int value) {
        Setting setting = new Setting(Setting.Type.KEY, settingName, settingDescription);
        setting.setKey(value);
        settings.add(setting);
        return setting;
    }

    public Setting addColour(String settingName, String settingDescription, Color colour) {
        Setting setting = new Setting(Setting.Type.COLOUR, settingName, settingDescription);
        setting.setColour(colour);
        settings.add(setting);
        return setting;
    }
}
