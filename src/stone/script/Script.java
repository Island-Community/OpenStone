package stone.script;

import stone.core.Color;
import stone.core.Log;
import stone.core.Vec3;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.BaseLib;
import org.luaj.vm2.lib.CoroutineLib;
import org.luaj.vm2.lib.DebugLib;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.jse.JseMathLib;
import org.luaj.vm2.lib.jse.JseOsLib;

final class Script {
    private static final String SCOPE = "script";

    private static final long CALL_BUDGET_NANOS = 250_000_000L;
    private static final int HOOK_INSTRUCTIONS = 200_000;

    private static final int LOGGED_ERRORS = 5;
    private static final int ERROR_LIMIT = 40;
    private static final String[] REMOVED_FROM_OS = {"execute", "exit", "remove", "rename", "tmpname", "setlocale"};
    private static final String[] REMOVED_GLOBALS = {"dofile", "loadfile", "package", "require"};

    enum Event {
        LOAD, UNLOAD, FRAME, TICK, RENDER, RENDER3D, KEY, CHAR_TYPED, MOUSE, MOUSE_MOVE, SCROLL,
        WORLD_LOAD, WORLD_LEAVE, SCREEN_OPEN, SCREEN_CLOSE, ENTITY_SPAWN, ENTITY_REMOVE, ENTITY_DEATH,
        HEALTH_CHANGE, DEATH, RESPAWN, SLOT_CHANGE, MODULE_TOGGLED, EFFECT_ADDED, EFFECT_REMOVED;

        private static final Map<String, Event> ALIASES = Map.ofEntries(
                Map.entry("update", TICK),
                Map.entry("gametick", TICK),
                Map.entry("draw", RENDER),
                Map.entry("render2d", RENDER),
                Map.entry("hudrender", RENDER),
                Map.entry("keypress", KEY),
                Map.entry("keydown", KEY),
                Map.entry("join", WORLD_LOAD),
                Map.entry("connect", WORLD_LOAD),
                Map.entry("worldchange", WORLD_LOAD),
                Map.entry("leave", WORLD_LEAVE),
                Map.entry("disconnect", WORLD_LEAVE),
                Map.entry("closescreen", SCREEN_CLOSE),
                Map.entry("openscreen", SCREEN_OPEN),
                Map.entry("moduletoggle", MODULE_TOGGLED),
                Map.entry("hurt", HEALTH_CHANGE),
                Map.entry("heldslot", SLOT_CHANGE));

        final String luaName = normalise(name());

        private static String normalise(String name) {
            StringBuilder out = new StringBuilder(name.length());
            for (int i = 0; i < name.length(); i++) {
                char c = name.charAt(i);
                if (c != '_' && c != '-' && c != ' ') {
                    out.append(c >= 'A' && c <= 'Z' ? (char) (c + 32) : c);
                }
            }
            return out.toString();
        }

        static Event fromName(String name) {
            String wanted = normalise(name);
            for (Event event : values()) {
                if (event.luaName.equals(wanted)) {
                    return event;
                }
            }
            return ALIASES.get(wanted);
        }
    }

    private final class Watchdog extends DebugLib {
        private int instructions;

        @Override
        public void onInstruction(int pc, Varargs v, int top) {
            super.onInstruction(pc, v, top);
            if (++instructions < HOOK_INSTRUCTIONS) {
                return;
            }
            instructions = 0;
            if (deadline != 0 && System.nanoTime() - deadline > 0) {
                throw Lua.error("callback ran for longer than %d ms and was stopped", CALL_BUDGET_NANOS / 1_000_000);
            }
        }
    }

    final String file;
    final Path path;
    private String name;
    private String version = "";
    private String author = "";
    private String description = "";
    private String error = "";

    private Globals globals;
    LuaTable colourType;
    LuaTable vec3Type;
    LuaTable entityType;
    LuaTable moduleType;
    LuaTable settingsType;
    private boolean stopped;
    private int errors;
    private int depth;
    private long deadline;

    private final Map<Event, List<LuaValue>> handlers = new EnumMap<>(Event.class);
    private final List<ScriptModule> modules = new ArrayList<>();

    Script(String file, Path path) {
        this.file = file;
        this.path = path;
        this.name = stem(file);
    }

    private static String stem(String file) {
        int dot = file.lastIndexOf('.');
        return dot < 0 ? file : file.substring(0, dot);
    }

    Globals globals() {
        return globals;
    }

    boolean running() {
        return globals != null && !stopped;
    }

    boolean stopped() {
        return stopped;
    }

    String name() {
        return name;
    }

    String version() {
        return version;
    }

    String author() {
        return author;
    }

    String description() {
        return description;
    }

    String error() {
        return error;
    }

    LuaTable colour(Color colour) {
        return Lua.colour(colourType, colour);
    }

    LuaTable vec3(Vec3 value) {
        return Lua.vec3(vec3Type, value);
    }

    private void openLibraries() {
        globals = new Globals();
        globals.load(new BaseLib());

        globals.load(new PackageLib());
        globals.load(new TableLib());
        globals.load(new StringLib());
        globals.load(new JseMathLib());
        globals.load(new CoroutineLib());
        globals.load(new JseOsLib());
        globals.load(new Watchdog());
        LoadState.install(globals);
        LuaC.install(globals);
        Stdlib.install(globals);

        LuaValue os = globals.get("os");
        for (String key : REMOVED_FROM_OS) {
            os.set(key, LuaValue.NIL);
        }
        for (String key : REMOVED_GLOBALS) {
            globals.set(key, LuaValue.NIL);
        }
    }

    boolean load() {
        close();
        stopped = false;
        errors = 0;
        error = "";
        name = stem(file);
        version = "";
        author = "";
        description = "";

        byte[] source = ScriptManager.readSource(path);
        if (source == null) {
            fail("file could not be read");
            return false;
        }

        openLibraries();
        colourType = Lua.colourType();
        vec3Type = Lua.vec3Type();
        if (!ApiCore.installApi(this)) {
            fail("the client API could not be installed");
            return false;
        }

        LuaValue chunk;
        try {
            chunk = globals.load(new ByteArrayInputStream(source), "@" + file, "t", globals);
        } catch (LuaError syntax) {
            fail(syntax.getMessage() != null ? syntax.getMessage() : "syntax error");
            return false;
        }
        if (call(chunk, LuaValue.NONE) == null) {
            stopped = true;
            return false;
        }
        return true;
    }

    void fireUnload() {
        if (globals == null) {
            return;
        }

        boolean wasStopped = stopped;
        stopped = false;
        for (LuaValue handler : List.copyOf(handlers(Event.UNLOAD))) {
            call(handler, LuaValue.NONE);
        }
        stopped = wasStopped;
    }

    void close() {
        globals = null;
        handlers.clear();
        modules.clear();
    }

    void setMetadata(String newName, String newVersion, String newAuthor, String newDescription) {
        if (!newName.isEmpty()) {
            name = newName;
        }
        if (!newVersion.isEmpty()) {
            version = newVersion;
        }
        if (!newAuthor.isEmpty()) {
            author = newAuthor;
        }
        if (!newDescription.isEmpty()) {
            description = newDescription;
        }
    }

    List<LuaValue> handlers(Event event) {
        return handlers.getOrDefault(event, List.of());
    }

    void subscribe(Event event, LuaValue function) {
        handlers.computeIfAbsent(event, key -> new ArrayList<>()).add(function);
    }

    boolean unsubscribe(Event event, LuaValue function) {
        List<LuaValue> list = handlers.get(event);
        return list != null && list.removeIf(function::raweq);
    }

    void addModule(ScriptModule module) {
        modules.add(module);
    }

    List<ScriptModule> modules() {
        return modules;
    }

    Varargs call(LuaValue function, Varargs args) {
        return protect(() -> function.invoke(args));
    }

    Varargs protect(Supplier<Varargs> body) {
        if (depth == 0) {
            deadline = System.nanoTime() + CALL_BUDGET_NANOS;
        }
        depth++;
        try {
            return body.get();
        } catch (LuaError failure) {
            reportError(describe(failure));
            return null;
        } catch (StackOverflowError overflow) {

            reportError("stack overflow");
            return null;
        } finally {
            if (--depth == 0) {
                deadline = 0;
            }
        }
    }

    private static String describe(LuaError failure) {
        LuaValue object = failure.getMessageObject();
        if (object == null) {
            return "unknown error";
        }
        if (object.isstring()) {
            return failure.getMessage();
        }
        LuaValue handler = object.metatag(LuaValue.TOSTRING);
        if (!handler.isnil()) {
            LuaValue text = handler.call(object);
            if (text.type() == LuaValue.TSTRING) {
                return text.tojstring();
            }
        }
        String full = failure.getMessage();
        int trace = full == null ? -1 : full.indexOf("\nstack traceback:");
        return String.format(Locale.ROOT, "(error object is a %s value)", object.typename())
                + (trace < 0 ? "" : full.substring(trace));
    }

    private void reportError(String message) {
        errors++;
        error = message;
        if (errors <= LOGGED_ERRORS) {
            Log.error(SCOPE, "%s: %s", file, error);
        }
        if (errors == 1) {
            ScriptManager.notify(file + " errored, see the log", ScriptManager.Notice.BAD);
        }
        if (errors >= ERROR_LIMIT && !stopped) {
            stopped = true;
            Log.error(SCOPE, "%s stopped after %d errors", file, errors);
            ScriptManager.notify(file + " stopped after too many errors", ScriptManager.Notice.BAD);
        }
    }

    void fail(String message) {
        stopped = true;
        error = message;
        Log.error(SCOPE, "%s: %s", file, error);
        ScriptManager.notify(file + ": " + error, ScriptManager.Notice.BAD);
    }
}
