package stone.script;

import stone.core.Config;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

final class ApiHud {
    private static final String ELEMENT_FIELD = "__hud";
    private static final String[] STALE_FIELDS = {"x", "y", "width", "height", "showing"};

    private ApiHud() {}

    private static HudElement elementOf(Varargs args) {
        LuaValue name = args.checktable(1).rawget(ELEMENT_FIELD);
        HudElement element = name.isstring() ? ScriptManager.findHudElement(name.tojstring()) : null;
        if (element == null) {
            throw Lua.error("the HUD element '%s' is gone", name.isstring() ? name.tojstring() : "?");
        }
        return element;
    }

    private static LuaTable methods() {
        LuaTable methods = new LuaTable();
        methods.set("pos", Lua.fn(args -> {
            HudElement element = elementOf(args);
            if (args.arg(2).isnumber()) {
                element.setPosition(Lua.checkFloat(args, 2), Lua.checkFloat(args, 3));
                return args.arg1();
            }
            return LuaValue.varargsOf(LuaValue.valueOf(element.x()), LuaValue.valueOf(element.y()));
        }));
        methods.set("size", Lua.fn(args -> {
            HudElement element = elementOf(args);
            if (args.arg(2).isnumber()) {
                element.setSize(Lua.checkFloat(args, 2), Lua.checkFloat(args, 3));
                return args.arg1();
            }
            return LuaValue.varargsOf(LuaValue.valueOf(element.width()), LuaValue.valueOf(element.height()));
        }));
        methods.set("show", Lua.fn(args -> {
            elementOf(args).setShowing(true);
            return args.arg1();
        }));
        methods.set("hide", Lua.fn(args -> {
            elementOf(args).setShowing(false);
            return args.arg1();
        }));
        methods.set("remove", Lua.fn(args -> {
            ScriptManager.removeHudElement(elementOf(args));
            return LuaValue.NONE;
        }));
        return methods;
    }

    private static LuaTable handleType() {
        LuaTable methods = methods();
        LuaTable type = new LuaTable();
        type.set(LuaValue.INDEX, Lua.fn(args -> {
            String key = args.checkjstring(2);
            LuaValue method = methods.rawget(key);
            if (!method.isnil()) {
                return method;
            }
            HudElement element = elementOf(args);
            return switch (key) {
                case "x" -> LuaValue.valueOf(element.x());
                case "y" -> LuaValue.valueOf(element.y());
                case "width" -> LuaValue.valueOf(element.width());
                case "height" -> LuaValue.valueOf(element.height());
                case "showing" -> LuaValue.valueOf(element.showing());
                case "name" -> LuaValue.valueOf(element.name);
                default -> LuaValue.NIL;
            };
        }));
        type.set(LuaValue.NEWINDEX, Lua.fn(args -> {
            String key = args.checkjstring(2);
            switch (key) {
                case "x", "y" -> {
                    HudElement element = elementOf(args);
                    float value = Lua.checkFloat(args, 3);
                    element.setPosition(key.equals("x") ? value : element.x(), key.equals("y") ? value : element.y());
                }
                case "width", "height" -> {
                    HudElement element = elementOf(args);
                    float value = Lua.checkFloat(args, 3);
                    element.setSize(key.equals("width") ? value : element.width(),
                            key.equals("height") ? value : element.height());
                }
                case "showing" -> elementOf(args).setShowing(args.arg(3).toboolean());
                default -> args.arg1().rawset(args.arg(2), args.arg(3));
            }
            return LuaValue.NONE;
        }));
        type.set("__name", "hudElement");
        return type;
    }

    private static LuaTable entry(HudElement element) {
        LuaTable entry = new LuaTable();
        entry.set("name", element.name);
        entry.set("script", element.owner.file);
        entry.set("x", element.x());
        entry.set("y", element.y());
        entry.set("width", element.width());
        entry.set("height", element.height());
        entry.set("showing", LuaValue.valueOf(element.showing()));
        return entry;
    }

    static void install(Script script, LuaTable cold) {
        LuaTable handleType = handleType();
        LuaTable hud = new LuaTable();
        hud.set("element", Lua.fn(args -> {
            LuaTable definition = args.checktable(1);
            String name = Lua.fieldString(definition, "name", "");
            if (name.isEmpty()) {
                throw Lua.error("a HUD element needs a name");
            }
            if (ScriptManager.findHudElement(name) != null) {
                throw Lua.error("a HUD element called '%s' already exists", name);
            }
            HudElement element = new HudElement(script, name);
            element.setSize((float) Lua.fieldNumber(definition, "width", 70.0),
                    (float) Lua.fieldNumber(definition, "height", 16.0));
            element.setShowing(Lua.fieldBoolean(definition, "showing", true));

            Config store = ScriptManager.store();
            String prefix = "hud." + name + ".";
            element.setPosition(
                    store.getNumber(script.file, prefix + "x", (float) Lua.fieldNumber(definition, "x", 4.0)),
                    store.getNumber(script.file, prefix + "y", (float) Lua.fieldNumber(definition, "y", 40.0)));

            definition.rawset(ELEMENT_FIELD, LuaValue.valueOf(name));
            for (String stale : STALE_FIELDS) {
                definition.rawset(stale, LuaValue.NIL);
            }
            definition.setmetatable(handleType);
            element.bind(definition);
            ScriptManager.attachHudElement(element);
            return definition;
        }));
        hud.set("find", Lua.fn(args -> {
            HudElement element = ScriptManager.findHudElement(args.checkjstring(1));
            return element == null || element.owner != script ? LuaValue.NIL : element.table();
        }));
        hud.set("all", Lua.fn(args -> {
            LuaTable list = new LuaTable();
            for (HudElement element : ScriptManager.hudElements()) {
                list.insert(0, entry(element));
            }
            return list;
        }));
        hud.set("remove", Lua.fn(args -> {
            HudElement element = ScriptManager.findHudElement(args.checkjstring(1));
            if (element == null) {
                return LuaValue.FALSE;
            }
            ScriptManager.removeHudElement(element);
            return LuaValue.TRUE;
        }));
        cold.set("hud", hud);
    }
}
