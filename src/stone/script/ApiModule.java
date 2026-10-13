package stone.script;

import stone.core.Color;
import stone.module.Category;
import stone.module.Module;
import stone.module.ModuleManager;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.render.Icon;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

final class ApiModule {
    private static final String MODULE_FIELD = "__module";
    private static final String[] STALE_FIELDS = {"bind", "key", "enabled", "category", "subcategory"};
    private static final Color DEFAULT_COLOUR = Color.rgb(0x58C6FF);

    private ApiModule() {}

    private static String callbackField(String event) {
        return switch (event) {
            case "enable" -> "onEnable";
            case "disable" -> "onDisable";
            case "tick", "update" -> "onTick";
            case "render", "draw" -> "onRender";
            default -> null;
        };
    }

    private static String lowercase(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private static Subcategory subcategoryOf(LuaTable definition) {
        String wanted = Lua.fieldString(definition, "subcategory", "");
        if (wanted.isEmpty()) {
            return Subcategory.GENERAL;
        }
        for (Subcategory subcategory : Subcategory.values()) {
            if (subcategory.displayName.equalsIgnoreCase(wanted)) {
                return subcategory;
            }
        }
        throw Lua.error("unknown subcategory '%s'", wanted);
    }

    private static Module ownerOf(LuaValue table) {
        LuaValue name = table.rawget(MODULE_FIELD);
        return name.isstring() ? ModuleManager.find(name.tojstring()) : null;
    }

    private static Module moduleOf(Varargs args) {
        LuaValue name = args.checktable(1).rawget(MODULE_FIELD);
        Module module = ownerOf(args.arg1());
        if (module == null) {
            throw Lua.error("module '%s' is not registered any more", name.isstring() ? name.tojstring() : "?");
        }
        return module;
    }

    private static LuaValue settingValue(Script script, Setting setting) {
        return switch (setting.type) {
            case BOOLEAN -> LuaValue.valueOf(setting.bool());
            case NUMBER -> LuaValue.valueOf(setting.number());
            case CHOICE -> LuaValue.valueOf(setting.choiceName());
            case KEY -> LuaValue.valueOf(setting.key());
            case COLOUR -> script.colour(setting.colour());
        };
    }

    private static void applySettingValue(Setting setting, Varargs args, int index) {
        switch (setting.type) {
            case BOOLEAN -> setting.setBool(args.arg(index).toboolean());
            case NUMBER -> setting.setNumber(Lua.checkFloat(args, index));
            case CHOICE -> {
                if (args.arg(index).isnumber()) {
                    setting.setChoice(args.arg(index).toint());
                    return;
                }
                String wanted = lowercase(args.checkjstring(index));
                List<String> choices = setting.choices();
                for (int entry = 0; entry < choices.size(); entry++) {
                    if (lowercase(choices.get(entry)).equals(wanted)) {
                        setting.setChoice(entry);
                        return;
                    }
                }
                throw Lua.error("'%s' is not one of the choices for %s", wanted, setting.name);
            }
            case KEY -> setting.setKey(args.checkint(index));
            case COLOUR -> setting.setColour(Lua.checkColour(args, index));
        }
    }

    private static LuaTable settingsType(Script script) {
        LuaTable type = new LuaTable();
        type.set(LuaValue.INDEX, Lua.fn(args -> {
            Module module = ownerOf(args.arg1());
            Setting setting = module == null ? null : module.setting(args.checkjstring(2));
            return setting == null ? LuaValue.NIL : settingValue(script, setting);
        }));
        type.set(LuaValue.NEWINDEX, Lua.fn(args -> {
            Module module = ownerOf(args.arg1());
            String key = args.checkjstring(2);
            Setting setting = module == null ? null : module.setting(key);
            if (setting == null) {
                throw Lua.error("no setting called '%s'", key);
            }
            applySettingValue(setting, args, 3);
            return LuaValue.NONE;
        }));
        type.set("__name", "settings");
        return type;
    }

    private static LuaTable settingsProxy(Script script, String moduleName) {
        LuaTable proxy = new LuaTable();
        proxy.rawset(MODULE_FIELD, LuaValue.valueOf(moduleName));
        proxy.setmetatable(script.settingsType);
        return proxy;
    }

    private static LuaTable settingInfo(Script script, Setting setting) {
        LuaTable info = new LuaTable();
        info.set("name", setting.name);
        info.set("description", setting.description);
        switch (setting.type) {
            case BOOLEAN -> info.set("type", "boolean");
            case NUMBER -> {
                info.set("type", "number");
                info.set("min", setting.minimum());
                info.set("max", setting.maximum());
                info.set("decimals", setting.decimals());
            }
            case CHOICE -> {
                info.set("type", "choice");
                LuaTable choices = new LuaTable();
                for (String choice : setting.choices()) {
                    choices.insert(0, LuaValue.valueOf(choice));
                }
                info.set("choices", choices);
            }
            case KEY -> info.set("type", "key");
            case COLOUR -> info.set("type", "color");
        }
        info.set("value", settingValue(script, setting));
        return info;
    }

    private static LuaTable methods(Script script) {
        LuaTable methods = new LuaTable();
        methods.set("toggle", Lua.fn(args -> {
            Module module = moduleOf(args);
            module.toggle(ScriptManager.context());
            return LuaValue.valueOf(module.enabled());
        }));
        methods.set("setting", Lua.fn(args -> {
            Setting setting = moduleOf(args).setting(args.checkjstring(2));
            return setting == null ? LuaValue.NIL : settingInfo(script, setting);
        }));
        methods.set("settingNames", Lua.fn(args -> {
            LuaTable names = new LuaTable();
            for (Setting setting : moduleOf(args).settings()) {
                names.insert(0, LuaValue.valueOf(setting.name));
            }
            return names;
        }));
        methods.set("on", Lua.fn(args -> {
            LuaTable handle = args.checktable(1);
            String event = lowercase(args.checkjstring(2));
            LuaValue handler = args.checkfunction(3);
            String field = callbackField(event);
            if (field == null) {
                throw Lua.error("a module has no '%s' event", event);
            }
            LuaValue existing = handle.get(field);

            handle.set(field, existing.isfunction() ? Lua.fn(callArgs -> {
                existing.invoke(callArgs);
                handler.invoke(callArgs);
                return LuaValue.NONE;
            }) : handler);
            return handle;
        }));
        return methods;
    }

    private static LuaTable moduleType(Script script) {
        LuaTable methods = methods(script);
        LuaTable type = new LuaTable();
        type.set(LuaValue.INDEX, Lua.fn(args -> {
            String key = args.checkjstring(2);
            LuaValue method = methods.rawget(key);
            if (!method.isnil()) {
                return method;
            }
            Module module = moduleOf(args);
            return switch (key) {
                case "enabled" -> LuaValue.valueOf(module.enabled());
                case "bind", "key" -> LuaValue.valueOf(module.key());
                case "name" -> LuaValue.valueOf(module.name);
                case "description" -> LuaValue.valueOf(module.description);
                case "category" -> LuaValue.valueOf(module.category.displayName);
                case "subcategory" -> LuaValue.valueOf(module.subcategory.displayName);
                case "animation" -> LuaValue.valueOf(module.animation());
                default -> LuaValue.NIL;
            };
        }));
        type.set(LuaValue.NEWINDEX, Lua.fn(args -> {
            String key = args.checkjstring(2);
            switch (key) {
                case "enabled" -> moduleOf(args).setEnabled(args.arg(3).toboolean(), ScriptManager.context());
                case "bind", "key" -> moduleOf(args).setKey(args.checkint(3));

                default -> args.arg1().rawset(args.arg(2), args.arg(3));
            }
            return LuaValue.NONE;
        }));
        type.set("__name", "module");
        return type;
    }

    static LuaTable handle(Script script, String name) {
        LuaTable handle = new LuaTable();
        handle.rawset(MODULE_FIELD, LuaValue.valueOf(name));
        handle.rawset("settings", settingsProxy(script, name));
        handle.setmetatable(script.moduleType);
        return handle;
    }

    private static void defineSetting(LuaTable entry, ScriptModule module) {
        String name = Lua.fieldString(entry, "name", "");
        if (name.isEmpty()) {
            throw Lua.error("every setting needs a name");
        }
        String description = Lua.fieldString(entry, "description", "");

        String type = lowercase(Lua.fieldString(entry, "type", ""));
        LuaValue value = entry.get("value");
        if (type.isEmpty()) {

            if (entry.get("choices").istable()) {
                type = "choice";
            } else if (value.type() == LuaValue.TTABLE) {
                type = "color";
            } else if (value.type() == LuaValue.TNUMBER) {
                type = "number";
            } else {
                type = "boolean";
            }
        }

        Setting setting = switch (type) {
            case "boolean", "bool", "check" ->
                    module.defineBoolean(name, description, Lua.fieldBoolean(entry, "value", false));
            case "number", "float", "int", "slider" -> {
                float minimum = (float) Lua.fieldNumber(entry, "min", 0.0);
                float maximum = (float) Lua.fieldNumber(entry, "max", 100.0);
                int decimals = Lua.fieldInt(entry, "decimals", type.equals("int") || maximum - minimum > 40.0f ? 0 : 2);
                yield module.defineNumber(name, description, (float) Lua.fieldNumber(entry, "value", minimum), minimum,
                        maximum, decimals);
            }
            case "choice", "mode", "list", "enum" -> {
                List<String> choices = new ArrayList<>();
                LuaValue listed = entry.get("choices");
                if (listed.istable()) {
                    for (int index = 1, count = listed.length(); index <= count; index++) {
                        LuaValue choice = listed.rawget(index);
                        if (choice.isstring()) {
                            choices.add(choice.tojstring());
                        }
                    }
                }
                if (choices.isEmpty()) {
                    throw Lua.error("setting '%s' is a choice with nothing to choose from", name);
                }
                String initial = lowercase(Lua.fieldString(entry, "value", choices.get(0)));
                int selected = 0;
                for (int index = 0; index < choices.size(); index++) {
                    if (lowercase(choices.get(index)).equals(initial)) {
                        selected = index;
                        break;
                    }
                }
                yield module.defineChoice(name, description, choices, selected);
            }
            case "key", "bind" -> module.defineKey(name, description, Lua.fieldInt(entry, "value", 0));
            case "color", "colour" -> module.defineColour(name, description, Lua.optColour(value, 1, DEFAULT_COLOUR));
            default -> throw Lua.error("setting '%s' has an unknown type '%s'", name, type);
        };

        LuaValue visible = entry.get("visible");
        if (visible.isfunction()) {
            module.defineVisibility(setting, visible);
        }
    }

    static void install(Script script, LuaTable cold) {
        script.settingsType = settingsType(script);
        script.moduleType = moduleType(script);

        cold.set("module", Lua.fn(args -> {
            LuaTable definition = args.checktable(1);
            if (!ScriptManager.loading()) {
                throw Lua.error("modules can only be declared while the script is loading");
            }
            String name = Lua.fieldString(definition, "name", "");
            if (name.isEmpty()) {
                throw Lua.error("a module needs a name");
            }
            if (ModuleManager.find(name) != null) {
                throw Lua.error("a module called '%s' already exists", name);
            }

            String description = Lua.fieldString(definition, "description", "");

            int bind = Lua.fieldInt(definition, "bind", 0);
            if (bind == 0) {
                bind = Lua.fieldInt(definition, "key", 0);
            }
            ScriptModule module = new ScriptModule(script, name, description, Category.SCRIPTS,
                    subcategoryOf(definition), bind);
            module.defineIcon(Icon.byName(Lua.fieldString(definition, "icon", ""), Category.SCRIPTS.icon));

            LuaValue settings = definition.get("settings");
            if (settings.istable()) {
                for (int index = 1, count = settings.length(); index <= count; index++) {
                    LuaValue entry = settings.rawget(index);
                    if (entry.istable()) {
                        defineSetting((LuaTable) entry, module);
                    }
                }
            }

            definition.rawset(MODULE_FIELD, LuaValue.valueOf(name));
            definition.rawset("settings", settingsProxy(script, name));
            for (String stale : STALE_FIELDS) {
                definition.rawset(stale, LuaValue.NIL);
            }
            definition.setmetatable(script.moduleType);
            module.bind(definition);
            ScriptManager.attachModule(script, module);
            return definition;
        }));

        LuaTable modules = new LuaTable();
        modules.set("get", Lua.fn(args -> {
            Module module = ModuleManager.find(args.checkjstring(1));
            return module == null ? LuaValue.NIL : handle(script, module.name);
        }));
        modules.set("list", Lua.fn(args -> {
            LuaTable names = new LuaTable();
            for (Module module : ModuleManager.all()) {
                names.insert(0, LuaValue.valueOf(module.name));
            }
            return names;
        }));
        modules.set("enabled", Lua.fn(args -> {
            Module module = ModuleManager.find(args.checkjstring(1));
            return LuaValue.valueOf(module != null && module.enabled());
        }));
        modules.set("toggle", Lua.fn(args -> {
            Module module = ModuleManager.find(args.checkjstring(1));
            if (module == null) {
                return LuaValue.FALSE;
            }
            module.setEnabled(args.isnoneornil(2) ? !module.enabled() : args.arg(2).toboolean(),
                    ScriptManager.context());
            return LuaValue.valueOf(module.enabled());
        }));
        cold.set("modules", modules);
    }
}
