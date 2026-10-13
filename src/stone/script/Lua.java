package stone.script;

import stone.core.Color;
import stone.core.Maths;
import stone.core.Rect;
import stone.core.Vec3;
import java.util.Locale;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

final class Lua {
    private static final LuaValue NAME = LuaValue.valueOf("__name");

    private Lua() {}

    interface Body {
        Varargs call(Varargs args);
    }

    static LuaValue fn(Body body) {
        return new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return body.call(args);
            }
        };
    }

    static LuaError error(String format, Object... args) {
        return new LuaError(String.format(Locale.ROOT, format, args));
    }

    private static float clamp01(double value) {
        return (float) Maths.clamp(value, 0.0, 1.0);
    }

    static LuaTable colourType() {
        LuaTable type = new LuaTable();
        LuaTable methods = new LuaTable();
        methods.set("alpha", fn(args -> {
            Color colour = checkColour(args, 1);
            return colour(type, new Color(colour.r(), colour.g(), colour.b(), clamp01(args.checkdouble(2))));
        }));
        methods.set("fade", fn(args -> {
            Color colour = checkColour(args, 1);
            return colour(type, new Color(colour.r(), colour.g(), colour.b(),
                    clamp01(colour.a() * args.checkdouble(2))));
        }));
        methods.set("mix", fn(args -> colour(type,
                Color.mix(checkColour(args, 1), checkColour(args, 2), clamp01(args.checkdouble(3))))));
        methods.set("hex", fn(args ->
                LuaValue.valueOf(String.format(Locale.ROOT, "#%06X", checkColour(args, 1).packed() >>> 8))));
        methods.set("unpack", fn(args -> {
            Color colour = checkColour(args, 1);
            return LuaValue.varargsOf(new LuaValue[] {
                LuaValue.valueOf(colour.r()), LuaValue.valueOf(colour.g()),
                LuaValue.valueOf(colour.b()), LuaValue.valueOf(colour.a()),
            });
        }));
        type.set("__index", methods);
        type.set("__tostring", fn(args -> {
            Color colour = checkColour(args, 1);
            return LuaValue.valueOf(String.format(Locale.ROOT, "colour(#%06X, a=%.2f)",
                    colour.packed() >>> 8, colour.a()));
        }));
        type.set(NAME, LuaValue.valueOf("colour"));
        return type;
    }

    static LuaTable colour(LuaTable type, Color colour) {
        LuaTable table = new LuaTable(0, 4);
        table.set("r", colour.r());
        table.set("g", colour.g());
        table.set("b", colour.b());
        table.set("a", colour.a());
        table.setmetatable(type);
        return table;
    }

    static Color checkColour(Varargs args, int index) {
        LuaValue value = args.arg(index);
        if (value.isnumber()) {
            long packed = (long) value.todouble();
            return Long.compareUnsigned(packed, 0xFFFFFFL) > 0 ? Color.rgba((int) packed) : Color.rgb((int) packed);
        }
        if (!value.istable()) {
            throw error("expected a colour at argument %d, got %s", index, value.typename());
        }
        return new Color(
                clamp01(fieldNumber(value, "r", 1)),
                clamp01(fieldNumber(value, "g", 1)),
                clamp01(fieldNumber(value, "b", 1)),
                clamp01(fieldNumber(value, "a", 1)));
    }

    static Color optColour(Varargs args, int index, Color fallback) {
        return args.isnoneornil(index) ? fallback : checkColour(args, index);
    }

    static LuaTable vec3Type() {
        LuaTable type = new LuaTable();
        type.set(NAME, LuaValue.valueOf("vec3"));
        return type;
    }

    static LuaTable vec3(LuaTable type, Vec3 value) {
        LuaTable table = new LuaTable(0, 3);
        table.set("x", value.x());
        table.set("y", value.y());
        table.set("z", value.z());
        table.setmetatable(type);
        return table;
    }

    static LuaTable rect(Rect bounds) {
        LuaTable table = new LuaTable(0, 4);
        table.set("x", bounds.x());
        table.set("y", bounds.y());
        table.set("width", bounds.width());
        table.set("height", bounds.height());
        return table;
    }

    record Vec3Arg(Vec3 value, int next) {}

    static Vec3Arg readVec3(Varargs args, int index) {
        LuaValue first = args.arg(index);
        if (first.istable()) {
            return new Vec3Arg(new Vec3(fieldNumber(first, "x", 0), fieldNumber(first, "y", 0),
                    fieldNumber(first, "z", 0)), index + 1);
        }
        return new Vec3Arg(new Vec3(args.checkdouble(index), args.checkdouble(index + 1),
                args.checkdouble(index + 2)), index + 3);
    }

    static float checkFloat(Varargs args, int index) {
        return (float) args.checkdouble(index);
    }

    static float optFloat(Varargs args, int index, float fallback) {
        return (float) args.optdouble(index, fallback);
    }

    static boolean optBoolean(Varargs args, int index, boolean fallback) {
        return args.isnoneornil(index) ? fallback : args.arg(index).toboolean();
    }

    static boolean fieldBoolean(LuaValue table, String key, boolean fallback) {
        LuaValue value = table.get(key);
        return value.isnil() ? fallback : value.toboolean();
    }

    static double fieldNumber(LuaValue table, String key, double fallback) {
        LuaValue value = table.get(key);
        return value.isnumber() ? value.todouble() : fallback;
    }

    static int fieldInt(LuaValue table, String key, int fallback) {
        LuaValue value = table.get(key);
        return value.isnumber() ? value.toint() : fallback;
    }

    static String fieldString(LuaValue table, String key, String fallback) {
        LuaValue value = table.get(key);
        return value.isstring() ? value.tojstring() : fallback;
    }

    static String tostring(LuaValue value) {
        if (value.type() == LuaValue.TNUMBER) {
            return Stdlib.number(value.todouble());
        }
        LuaValue handler = value.metatag(LuaValue.TOSTRING);
        if (!handler.isnil()) {
            LuaValue text = handler.call(value);
            if (!text.isstring()) {
                throw new LuaError("'__tostring' must return a string");
            }
            return text.tojstring();
        }
        LuaValue name = value.metatag(NAME);
        if (name.isstring() && (value.istable() || value.isuserdata())) {
            return name.tojstring() + ": 0x" + Integer.toHexString(System.identityHashCode(value));
        }
        return value.tojstring();
    }
}
