package stone.script;

import stone.core.Assets;
import stone.core.Color;
import stone.core.Log;
import stone.core.Time;
import stone.game.FrameState;
import stone.game.Game;
import stone.game.LocalPlayerView;
import stone.module.Context;
import stone.module.Module;
import stone.module.ModuleManager;
import stone.render.FontRole;
import stone.render.Renderer;
import stone.ui.Client;
import stone.ui.Keys;
import stone.ui.Theme;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

final class ApiCore {
    private static final String SCOPE = "script";
    private static final String PRELUDE = "assets/lua/prelude.lua";
    private static final String CONFIG_PREFIX = "var.";
    private static final String FRIENDS = "friends";
    private static final FrameState NO_STATE = new FrameState();

    private static final Object[][] KEYS = {
        {"SPACE", 32}, {"APOSTROPHE", 39}, {"COMMA", 44}, {"MINUS", 45},
        {"PERIOD", 46}, {"SLASH", 47}, {"SEMICOLON", 59}, {"EQUAL", 61},
        {"LBRACKET", 91}, {"BACKSLASH", 92}, {"RBRACKET", 93}, {"GRAVE", 96},
        {"ESCAPE", 256}, {"ENTER", 257}, {"TAB", 258}, {"BACKSPACE", 259},
        {"INSERT", 260}, {"DELETE", 261}, {"RIGHT", 262}, {"LEFT", 263},
        {"DOWN", 264}, {"UP", 265}, {"PAGEUP", 266}, {"PAGEDOWN", 267},
        {"HOME", 268}, {"END", 269}, {"CAPSLOCK", 280}, {"NUMLOCK", 282},
        {"PRINTSCREEN", 283}, {"PAUSE", 284}, {"LSHIFT", 340}, {"LCONTROL", 341},
        {"LALT", 342}, {"LSUPER", 343}, {"RSHIFT", 344}, {"RCONTROL", 345},
        {"RALT", 346}, {"RSUPER", 347}, {"MENU", 348},
    };

    private ApiCore() {}

    static boolean installApi(Script script) {
        LuaTable cold = new LuaTable();
        installCore(script, cold);
        ApiGame.install(script, cold);
        ApiRender.install(cold);
        ApiRender3d.install(cold);
        ApiHud.install(script, cold);
        ApiModule.install(script, cold);
        script.globals().set("cold", cold);

        script.globals().set("print", cold.get("log"));

        try {
            script.globals().load(new ByteArrayInputStream(Assets.read(PRELUDE)), "@cold.prelude", "t",
                    script.globals()).call(script.vec3Type);
        } catch (LuaError failure) {
            Log.error(SCOPE, "prelude: %s", failure.getMessage());
            return false;
        }
        return true;
    }

    static Context requireContext() {
        Context context = ScriptManager.context();
        if (context == null) {
            throw new LuaError("the game is only reachable from inside a client callback");
        }
        return context;
    }

    static FrameState requireState() {
        return requireContext().state;
    }

    static LocalPlayerView requirePlayer() {
        LocalPlayerView player = requireState().player;
        if (!player.valid) {
            throw new LuaError("there is no player right now");
        }
        return player;
    }

    static String joinArguments(Varargs args, int first) {
        StringBuilder text = new StringBuilder();
        for (int index = first; index <= args.narg(); index++) {
            if (index > first) {
                text.append(' ');
            }
            text.append(Lua.tostring(args.arg(index)));
        }
        return text.toString();
    }

    private static Script.Event event(String name) {
        Script.Event event = Script.Event.fromName(name);
        if (event == null) {
            throw Lua.error("unknown event '%s'", name);
        }
        return event;
    }

    private static void installCore(Script script, LuaTable cold) {
        cold.set("script", Lua.fn(args -> {
            LuaTable info = args.checktable(1);
            String author = Lua.fieldString(info, "author", "");
            if (author.isEmpty()) {
                author = Lua.fieldString(info, "authors", "");
            }
            script.setMetadata(Lua.fieldString(info, "name", ""), Lua.fieldString(info, "version", ""), author,
                    Lua.fieldString(info, "description", ""));
            return LuaValue.NONE;
        }));

        cold.set("on", Lua.fn(args -> {
            Script.Event event = event(args.checkjstring(1));
            LuaValue handler = args.checkfunction(2);
            script.subscribe(event, handler);
            return handler;
        }));
        cold.set("off", Lua.fn(args ->
                LuaValue.valueOf(script.unsubscribe(event(args.checkjstring(1)), args.arg(2)))));
        cold.set("chat", Lua.fn(args -> {
            requireContext();
            Game.printChat(joinArguments(args, 1));
            return LuaValue.TRUE;
        }));
        cold.set("say", Lua.fn(args -> {
            requireContext();
            Game.sendChat(args.checkjstring(1), false);
            return LuaValue.TRUE;
        }));
        cold.set("command", Lua.fn(args -> {
            requireContext();
            String text = args.checkjstring(1);
            Game.sendChat(text.startsWith("/") ? text.substring(1) : text, true);
            return LuaValue.TRUE;
        }));
        cold.set("log", Lua.fn(args -> {
            Log.info(SCOPE, "%s: %s", script.file, joinArguments(args, 1));
            return LuaValue.NONE;
        }));
        cold.set("include", include(script));

        LuaValue colour = Lua.fn(args -> {
            Color value = new Color(channel(args.checkdouble(1)), channel(args.checkdouble(2)),
                    channel(args.checkdouble(3)), channel(args.optdouble(4, 1.0)));
            return script.colour(value);
        });
        cold.set("color", colour);
        cold.set("colour", colour);
        cold.set("rgb", Lua.fn(args ->
                script.colour(Color.rgb((int) (args.checklong(1) & 0xFFFFFF), Lua.optFloat(args, 2, 1.0f)))));
        cold.set("rgba", Lua.fn(args -> script.colour(Color.rgba((int) args.checklong(1)))));
        cold.set("hsv", Lua.fn(args -> script.colour(Color.hsv(Lua.checkFloat(args, 1),
                Lua.optFloat(args, 2, 1.0f), Lua.optFloat(args, 3, 1.0f), Lua.optFloat(args, 4, 1.0f)))));
        cold.set("accent", Lua.fn(args -> script.colour(Theme.accent())));
        cold.set("keyName", Lua.fn(args -> LuaValue.valueOf(Keys.keyName(args.checkint(1)))));
        cold.set("name", Theme.CLIENT_NAME);
        cold.set("version", Theme.CLIENT_VERSION);

        cold.set("keys", keys());
        LuaTable mouseButtons = new LuaTable();
        mouseButtons.set("LEFT", 0);
        mouseButtons.set("RIGHT", 1);
        mouseButtons.set("MIDDLE", 2);
        mouseButtons.set("BUTTON4", 3);
        mouseButtons.set("BUTTON5", 4);
        cold.set("mouseButtons", mouseButtons);

        cold.set("config", config(script));
        cold.set("scripts", scripts());
        cold.set("theme", theme(script));
        cold.set("friends", friends());
        LuaTable sound = new LuaTable();
        sound.set("play", Lua.fn(args -> {
            requirePlayer();
            Game.playSound(args.checkjstring(1), Lua.optFloat(args, 2, 1.0f), Lua.optFloat(args, 3, 1.0f));
            return LuaValue.TRUE;
        }));
        cold.set("sound", sound);
        cold.set("notify", notifications());
        cold.set("client", client(script));
    }

    private static float channel(double value) {
        return (float) (value > 1.0 ? value / 255.0 : value);
    }

    private static LuaValue include(Script script) {
        Map<String, LuaValue> included = new HashMap<>();
        return Lua.fn(args -> {
            String name = args.checkjstring(1);
            if (name.contains("..") || name.contains(":") || name.contains("\\") || name.startsWith("/")) {
                throw Lua.error("include stays inside the scripts folder: '%s'", name);
            }
            String file = name.endsWith(".lua") ? name : name + ".lua";
            LuaValue cached = included.get(file);
            if (cached != null) {
                return cached;
            }
            byte[] source = ScriptManager.readSource(ScriptManager.scriptPath(file));
            if (source == null) {
                throw Lua.error("include could not read '%s'", file);
            }
            LuaValue result = script.globals().load(new ByteArrayInputStream(source), "@" + file, "t",
                    script.globals()).call();
            if (result.isnil()) {
                result = LuaValue.TRUE;
            }
            included.put(file, result);
            return result;
        });
    }

    private static LuaTable keys() {
        LuaTable keys = new LuaTable();
        for (Object[] key : KEYS) {
            keys.set((String) key[0], (Integer) key[1]);
        }
        for (char letter = 'A'; letter <= 'Z'; letter++) {
            keys.set(String.valueOf(letter), letter);
        }
        for (int digit = 0; digit <= 9; digit++) {
            keys.set("NUM" + digit, '0' + digit);
            keys.set("KP" + digit, 320 + digit);
        }
        for (int index = 1; index <= 25; index++) {
            keys.set("F" + index, 289 + index);
        }
        return keys;
    }

    private static Varargs notify(Varargs args) {
        String text = args.checkjstring(1);
        float seconds = Lua.optFloat(args, 2, 4.0f);
        LuaValue kind = args.arg(3);
        if (kind.istable()) {
            Client.hud().notify("Stone", text, Lua.checkColour(args, 3), seconds);
            return LuaValue.NONE;
        }
        ScriptManager.Notice notice = ScriptManager.Notice.INFO;
        if (kind.isstring()) {
            notice = switch (kind.tojstring()) {
                case "good", "success" -> ScriptManager.Notice.GOOD;
                case "warn", "warning" -> ScriptManager.Notice.WARN;
                case "bad", "error" -> ScriptManager.Notice.BAD;
                default -> ScriptManager.Notice.INFO;
            };
        }
        ScriptManager.notify(text, notice, seconds);
        return LuaValue.NONE;
    }

    private static LuaValue notifyAs(ScriptManager.Notice kind) {
        return Lua.fn(args -> {
            ScriptManager.notify(args.checkjstring(1), kind, Lua.optFloat(args, 2, 4.0f));
            return LuaValue.NONE;
        });
    }

    private static LuaValue overlay() {
        return Lua.fn(args -> {
            requireContext();
            Game.overlayMessage(args.checkjstring(1));
            return LuaValue.TRUE;
        });
    }

    private static LuaTable notifications() {
        LuaTable notify = new LuaTable();
        notify.set("info", notifyAs(ScriptManager.Notice.INFO));
        notify.set("success", notifyAs(ScriptManager.Notice.GOOD));
        notify.set("warn", notifyAs(ScriptManager.Notice.WARN));
        notify.set("error", notifyAs(ScriptManager.Notice.BAD));
        notify.set("overlay", overlay());
        LuaTable meta = new LuaTable();

        meta.set(LuaValue.CALL, Lua.fn(args -> notify(args.subargs(2))));
        notify.setmetatable(meta);
        return notify;
    }

    private static LuaTable theme(Script script) {
        LuaTable theme = new LuaTable();
        LuaValue colour = Lua.fn(args -> {
            String name = args.arg1().isstring() ? args.arg1().tojstring() : "accent";
            return script.colour(switch (name) {
                case "text" -> Theme.TEXT;
                case "textDim", "second" -> Theme.TEXT_DIM;
                case "textBright" -> Theme.TEXT_BRIGHT;
                case "background", "panel" -> Theme.PANEL;
                case "surface" -> Theme.SURFACE;
                case "outline", "border" -> Theme.BORDER;
                case "positive", "good" -> Theme.POSITIVE;
                case "warning", "warn" -> Theme.WARNING;
                case "negative", "bad" -> Theme.NEGATIVE;
                case "shadow" -> Theme.SHADOW;
                default -> Theme.accent();
            });
        });
        theme.set("color", colour);
        theme.set("colour", colour);
        theme.set("setAccent", Lua.fn(args -> {
            Theme.setAccent(Lua.checkColour(args, 1));
            return LuaValue.NONE;
        }));

        theme.set("readable", Lua.fn(args -> {
            Color background = Lua.checkColour(args, 1);
            float luminance = 0.2126f * background.r() + 0.7152f * background.g() + 0.0722f * background.b();
            return script.colour(luminance > 0.55f ? Color.rgb(0x101317) : Color.WHITE);
        }));
        theme.set("health", Lua.fn(args ->
                script.colour(Theme.healthColour(Lua.checkFloat(args, 1), Lua.optFloat(args, 2, 20.0f)))));
        return theme;
    }

    private static LuaTable friends() {
        LuaTable friends = new LuaTable();
        friends.set("add", Lua.fn(args -> {
            String name = args.checkjstring(1);
            String key = name.toLowerCase(Locale.ROOT);
            if (key.isEmpty()) {
                return LuaValue.FALSE;
            }
            ScriptManager.store().set(FRIENDS, key, name);
            ScriptManager.saveStore();
            return LuaValue.TRUE;
        }));
        friends.set("remove", Lua.fn(args -> {
            String key = args.checkjstring(1).toLowerCase(Locale.ROOT);
            boolean had = !ScriptManager.store().get(FRIENDS, key, "").isEmpty();
            ScriptManager.store().set(FRIENDS, key, "");
            ScriptManager.saveStore();
            return LuaValue.valueOf(had);
        }));
        friends.set("is", Lua.fn(args -> LuaValue.valueOf(
                !ScriptManager.store().get(FRIENDS, args.checkjstring(1).toLowerCase(Locale.ROOT), "").isEmpty())));
        friends.set("list", Lua.fn(args -> {
            LuaTable list = new LuaTable();
            for (String key : ScriptManager.store().keys(FRIENDS)) {
                String display = ScriptManager.store().get(FRIENDS, key, "");
                if (!display.isEmpty()) {
                    list.insert(0, LuaValue.valueOf(display));
                }
            }
            return list;
        }));
        friends.set("clear", Lua.fn(args -> {
            for (String key : ScriptManager.store().keys(FRIENDS)) {
                ScriptManager.store().set(FRIENDS, key, "");
            }
            ScriptManager.saveStore();
            return LuaValue.NONE;
        }));
        return friends;
    }

    private static LuaTable config(Script script) {
        LuaTable config = new LuaTable();
        config.set("get", Lua.fn(args -> {
            String key = CONFIG_PREFIX + args.checkjstring(1);
            LuaValue fallback = args.arg(2);
            return switch (fallback.type()) {
                case LuaValue.TNUMBER -> LuaValue.valueOf(
                        ScriptManager.store().getNumber(script.file, key, (float) fallback.todouble()));
                case LuaValue.TBOOLEAN -> LuaValue.valueOf(
                        ScriptManager.store().getBool(script.file, key, fallback.toboolean()));
                default -> {
                    String value = ScriptManager.store().get(script.file, key,
                            fallback.isstring() ? fallback.tojstring() : "");
                    yield value.isEmpty() && fallback.isnil() ? LuaValue.NIL : LuaValue.valueOf(value);
                }
            };
        }));
        config.set("set", Lua.fn(args -> {
            String key = CONFIG_PREFIX + args.checkjstring(1);
            LuaValue value = args.arg(2);
            switch (value.type()) {
                case LuaValue.TNUMBER -> ScriptManager.store().setNumber(script.file, key, (float) value.todouble(), 4);
                case LuaValue.TBOOLEAN -> ScriptManager.store().setBool(script.file, key, value.toboolean());
                case LuaValue.TNIL -> ScriptManager.store().set(script.file, key, "");
                default -> ScriptManager.store().set(script.file, key, args.checkjstring(2));
            }
            return LuaValue.NONE;
        }));
        config.set("save", Lua.fn(args -> {
            ScriptManager.saveStore();
            return LuaValue.NONE;
        }));
        return config;
    }

    private static LuaTable scripts() {
        LuaTable scripts = new LuaTable();
        scripts.set("list", Lua.fn(args -> {
            LuaTable list = new LuaTable();
            for (Script entry : ScriptManager.scripts()) {
                LuaTable row = new LuaTable();
                row.set("file", entry.file);
                row.set("name", entry.name());
                row.set("version", entry.version());
                row.set("author", entry.author());
                row.set("description", entry.description());
                row.set("error", entry.error());
                row.set("enabled", LuaValue.valueOf(ScriptManager.enabled(entry.file)));
                row.set("loaded", LuaValue.valueOf(entry.running()));
                list.insert(0, row);
            }
            return list;
        }));
        scripts.set("reload", Lua.fn(args -> {
            if (args.arg1().isstring()) {
                ScriptManager.requestReload(args.arg1().tojstring());
            } else {
                ScriptManager.requestReload();
            }
            return LuaValue.NONE;
        }));
        scripts.set("enable", Lua.fn(args -> {
            ScriptManager.requestEnable(args.checkjstring(1), Lua.optBoolean(args, 2, true));
            return LuaValue.NONE;
        }));
        return scripts;
    }

    private static LuaTable client(Script script) {
        LuaTable client = new LuaTable();
        client.set("msg", chatLine(""));

        client.set("warn", chatLine("§e"));
        client.set("error", chatLine("§c"));
        client.set("overlay", overlay());
        client.set("server", Lua.fn(args -> {
            requireContext();
            String address = Game.serverAddress();
            return address.isEmpty() ? LuaValue.NIL : LuaValue.valueOf(address);
        }));
        client.set("gameDir", Lua.fn(args -> {
            requireContext();
            return LuaValue.valueOf(Game.gameDirectory());
        }));
        client.set("find", Lua.fn(args -> {
            Module module = ModuleManager.find(args.checkjstring(1));
            if (module == null) {
                return LuaValue.NIL;
            }
            LuaTable handle = ApiModule.handle(script, module.name);
            if (!args.arg(2).isstring()) {
                return handle;
            }
            return module.setting(args.tojstring(2)) == null ? LuaValue.NIL : handle.get("settings").get(args.arg(2));
        }));
        client.set("fontWidth", Lua.fn(args -> LuaValue.valueOf(
                Renderer.instance().measure(FontRole.MEDIUM, args.checkjstring(1), Lua.optFloat(args, 2, 10.0f)))));
        client.set("fontHeight", Lua.fn(args -> LuaValue.valueOf(
                Renderer.instance().lineHeight(FontRole.MEDIUM, Lua.optFloat(args, 1, 10.0f)))));

        LuaTable meta = new LuaTable();
        meta.set(LuaValue.INDEX, Lua.fn(args -> property(args.checkjstring(2))));
        meta.set("__name", "client");
        client.setmetatable(meta);
        return client;
    }

    private static LuaValue chatLine(String prefix) {
        return Lua.fn(args -> {
            requireContext();
            Game.printChat(prefix + joinArguments(args, 1));
            return LuaValue.TRUE;
        });
    }

    private static LuaValue property(String key) {
        Context context = ScriptManager.context();
        FrameState state = context != null ? context.state : NO_STATE;
        return switch (key) {
            case "name" -> LuaValue.valueOf(Theme.CLIENT_NAME);
            case "version" -> LuaValue.valueOf(Theme.CLIENT_VERSION);
            case "fps" -> LuaValue.valueOf(state.fps);
            case "ping" -> LuaValue.valueOf(state.ping);
            case "inWorld" -> LuaValue.valueOf(state.inWorld);
            case "screenOpen" -> LuaValue.valueOf(state.screenOpen);
            case "menuOpen" -> LuaValue.valueOf(Client.gui().interactive());
            case "width" -> LuaValue.valueOf(state.windowWidth);
            case "height" -> LuaValue.valueOf(state.windowHeight);
            case "scaledWidth" -> LuaValue.valueOf(state.scaledWidth);
            case "scaledHeight" -> LuaValue.valueOf(state.scaledHeight);

            case "scale" -> LuaValue.valueOf(
                    state.scaledWidth > 0 ? (double) state.windowWidth / state.scaledWidth : 2.0);
            case "guiScale" -> LuaValue.valueOf(state.scaleFactor);
            case "sensitivity" -> LuaValue.valueOf(state.mouseSensitivity);
            case "time" -> LuaValue.valueOf(context != null ? context.time : Time.nowSeconds());
            case "delta" -> LuaValue.valueOf(context != null ? context.deltaTime : 0.0);
            case "tick" -> LuaValue.valueOf((double) ScriptManager.ticks());
            case "frame" -> LuaValue.valueOf((double) ScriptManager.frames());
            case "worldTime" -> LuaValue.valueOf((double) state.worldTime);
            case "overlay" -> LuaValue.valueOf((double) ScriptManager.overlayMicros());
            case "tickProgress" -> LuaValue.valueOf(state.tickProgress);
            default -> LuaValue.NIL;
        };
    }
}
