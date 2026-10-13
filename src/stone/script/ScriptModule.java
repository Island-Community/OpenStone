package stone.script;

import stone.core.Color;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.render.Icon;
import java.util.List;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

final class ScriptModule extends Module {
    final Script owner;
    private LuaTable table;
    private boolean started;

    ScriptModule(Script owner, String name, String description, Category category, Subcategory subcategory,
            int key) {
        super(name, description, category, subcategory, key);
        this.owner = owner;
    }

    void bind(LuaTable value) {
        table = value;
    }

    LuaTable table() {
        return table;
    }

    @Override
    public String suffix() {
        if (!owner.running() || table == null) {
            return "";
        }
        Varargs result = owner.protect(() -> {
            LuaValue value = table.get("suffix");
            return value.isfunction() ? value.call(table) : value;
        });
        return result != null && result.arg1().type() == LuaValue.TSTRING ? result.arg1().tojstring() : "";
    }

    private void invoke(String key, Context context) {
        if (!owner.running() || table == null) {
            return;
        }
        ScriptManager.within(context, () -> owner.protect(() -> {
            LuaValue callback = table.get(key);
            return callback.isfunction() ? callback.call(table) : LuaValue.NONE;
        }));
    }

    private void ensureStarted(Context context) {
        if (started || !enabled()) {
            return;
        }
        started = true;
        invoke("onEnable", context);
    }

    @Override
    public void onEnable(Context context) {
        if (started) {
            return;
        }
        started = true;
        invoke("onEnable", context);
    }

    @Override
    public void onDisable(Context context) {
        if (!started) {
            return;
        }
        started = false;
        invoke("onDisable", context);
    }

    @Override
    public void onTick(Context context) {
        ensureStarted(context);
        invoke("onTick", context);
    }

    @Override
    public void onRender(Context context) {
        ensureStarted(context);
        ScriptManager.draw(() -> invoke("onRender", context));
    }

    Setting defineBoolean(String settingName, String settingDescription, boolean value) {
        return addBoolean(settingName, settingDescription, value);
    }

    Setting defineNumber(String settingName, String settingDescription, float value, float minimum, float maximum,
            int decimals) {
        return addNumber(settingName, settingDescription, value, minimum, maximum, decimals);
    }

    Setting defineChoice(String settingName, String settingDescription, List<String> choices, int index) {
        return addChoice(settingName, settingDescription, choices, index);
    }

    Setting defineKey(String settingName, String settingDescription, int value) {
        return addKey(settingName, settingDescription, value);
    }

    Setting defineColour(String settingName, String settingDescription, Color colour) {
        return addColour(settingName, settingDescription, colour);
    }

    void defineIcon(Icon icon) {
        setIcon(icon);
    }

    void defineVisibility(Setting setting, LuaValue predicate) {
        setting.visibleWhen(() -> {
            if (!owner.running()) {
                return true;
            }
            Varargs result = owner.call(predicate, LuaValue.NONE);
            return result == null || result.arg1().toboolean();
        });
    }
}
