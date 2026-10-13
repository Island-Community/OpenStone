package stone.script;

import stone.core.Color;
import stone.core.Rect;
import stone.game.FrameState;
import stone.game.Projection;
import stone.module.Context;
import stone.render.Align;
import stone.render.FontRole;
import stone.render.Icon;
import stone.render.Renderer;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

final class ApiRender {
    private static final Color WHITE = Color.rgb(0xFFFFFF);

    private ApiRender() {}

    static Renderer requireDrawing() {
        if (!ScriptManager.drawing()) {
            throw Lua.error("drawing is only allowed from a render callback");
        }
        return Renderer.instance();
    }

    private static FontRole fontRole(String name) {
        return switch (name) {
            case "medium" -> FontRole.MEDIUM;
            case "semibold" -> FontRole.SEMI_BOLD;
            case "bold" -> FontRole.BOLD;
            case "mono" -> FontRole.MONO;
            case "monobold" -> FontRole.MONO_BOLD;
            default -> FontRole.REGULAR;
        };
    }

    private static FontRole optFontRole(Varargs args, int index) {
        return fontRole(args.arg(index).isstring() ? args.arg(index).tojstring() : "medium");
    }

    private static Align alignment(String name) {
        return switch (name) {
            case "center", "centre" -> Align.CENTER;
            case "right" -> Align.RIGHT;
            default -> Align.LEFT;
        };
    }

    private static Rect checkRect(Varargs args, int index) {
        return new Rect(Lua.checkFloat(args, index), Lua.checkFloat(args, index + 1), Lua.checkFloat(args, index + 2),
                Lua.checkFloat(args, index + 3));
    }

    private static float drawText(Renderer gfx, boolean byCapHeight, FontRole role, String value, float x, float y,
            float size, Color colour, Align align) {
        return byCapHeight ? gfx.textCap(role, value, x, y, size, colour, align)
                : gfx.text(role, value, x, y, size, colour, align);
    }

    private static Varargs text(Varargs args) {
        Renderer gfx = requireDrawing();
        String value = args.checkjstring(1);
        float x = Lua.checkFloat(args, 2);
        float y = Lua.checkFloat(args, 3);
        float size = Lua.optFloat(args, 4, 12.0f);
        Color colour = Lua.optColour(args, 5, WHITE);

        FontRole role = FontRole.MEDIUM;
        Align align = Align.LEFT;
        boolean shadow = false;
        boolean byCapHeight = false;
        float bloom = 0.0f;
        LuaValue outline = LuaValue.NIL;
        LuaValue options = args.arg(6);
        if (options.istable()) {
            role = fontRole(Lua.fieldString(options, "font", "medium"));
            align = alignment(Lua.fieldString(options, "align", "left"));
            shadow = Lua.fieldBoolean(options, "shadow", false);
            byCapHeight = Lua.fieldBoolean(options, "cap", false);
            bloom = (float) Lua.fieldNumber(options, "bloom", 0.0);
            outline = options.get("outline");
        }

        if (bloom > 0.0f) {
            gfx.textBloom(role, value, x, byCapHeight ? y : y + gfx.fonts().face(role).capTop(size), size,
                    colour.withAlpha(0.5f), bloom, align);
        }
        if (!outline.isnil()) {

            Color outlineColour = Lua.checkColour(outline, 1);
            float offset = Math.max(1.0f, size * 0.075f);
            float[][] steps = {{-offset, 0.0f}, {offset, 0.0f}, {0.0f, -offset}, {0.0f, offset}};
            for (float[] step : steps) {
                drawText(gfx, byCapHeight, role, value, x + step[0], y + step[1], size, outlineColour, align);
            }
        }
        if (shadow) {
            float offset = Math.max(1.0f, size * 0.09f);
            drawText(gfx, byCapHeight, role, value, x + offset, y + offset, size, Color.rgb(0x000000, 0.55f * colour.a()),
                    align);
        }
        return LuaValue.valueOf(drawText(gfx, byCapHeight, role, value, x, y, size, colour, align));
    }

    static void install(LuaTable cold) {
        LuaTable render = new LuaTable();
        render.set("rect", Lua.fn(args -> {
            Renderer gfx = requireDrawing();
            gfx.rect(checkRect(args, 1), Lua.checkColour(args, 5), Lua.optFloat(args, 6, 0.0f));
            return LuaValue.NONE;
        }));
        render.set("gradient", Lua.fn(args -> {
            Renderer gfx = requireDrawing();
            Rect bounds = checkRect(args, 1);
            Color from = Lua.checkColour(args, 5);
            Color to = Lua.checkColour(args, 6);
            float radius = Lua.optFloat(args, 8, 0.0f);
            if (Lua.optBoolean(args, 7, false)) {
                gfx.rectHorizontal(bounds, from, to, radius);
            } else {
                gfx.rectVertical(bounds, from, to, radius);
            }
            return LuaValue.NONE;
        }));
        render.set("outline", Lua.fn(args -> {
            Renderer gfx = requireDrawing();
            gfx.outline(checkRect(args, 1), Lua.checkColour(args, 5), Lua.optFloat(args, 6, 1.0f),
                    Lua.optFloat(args, 7, 0.0f));
            return LuaValue.NONE;
        }));
        render.set("shadow", Lua.fn(args -> {
            Renderer gfx = requireDrawing();
            gfx.shadow(checkRect(args, 1), Lua.checkColour(args, 5), Lua.optFloat(args, 6, 8.0f),
                    Lua.optFloat(args, 7, 0.0f));
            return LuaValue.NONE;
        }));
        render.set("glow", Lua.fn(args -> {
            Renderer gfx = requireDrawing();
            gfx.glow(checkRect(args, 1), Lua.checkColour(args, 5), Lua.optFloat(args, 6, 8.0f),
                    Lua.optFloat(args, 7, 0.0f));
            return LuaValue.NONE;
        }));
        render.set("line", Lua.fn(args -> {
            Renderer gfx = requireDrawing();
            gfx.line(Lua.checkFloat(args, 1), Lua.checkFloat(args, 2), Lua.checkFloat(args, 3), Lua.checkFloat(args, 4),
                    Lua.optFloat(args, 5, 1.0f), Lua.checkColour(args, 6));
            return LuaValue.NONE;
        }));
        render.set("ring", Lua.fn(args -> {
            Renderer gfx = requireDrawing();
            gfx.ring(Lua.checkFloat(args, 1), Lua.checkFloat(args, 2), Lua.checkFloat(args, 3),
                    Lua.optFloat(args, 4, 1.0f), Lua.optFloat(args, 5, 1.0f), Lua.checkColour(args, 6));
            return LuaValue.NONE;
        }));
        render.set("circle", Lua.fn(args -> {
            Renderer gfx = requireDrawing();
            float x = Lua.checkFloat(args, 1);
            float y = Lua.checkFloat(args, 2);
            float radius = Lua.checkFloat(args, 3);
            gfx.rect(new Rect(x - radius, y - radius, radius * 2.0f, radius * 2.0f), Lua.checkColour(args, 4), radius);
            return LuaValue.NONE;
        }));

        render.set("quad", Lua.fn(args -> {
            Renderer gfx = requireDrawing();
            float[] xs = new float[4];
            float[] ys = new float[4];
            for (int corner = 0; corner < 4; corner++) {
                xs[corner] = Lua.checkFloat(args, corner * 2 + 1);
                ys[corner] = Lua.checkFloat(args, corner * 2 + 2);
            }
            gfx.face(xs, ys, Lua.checkColour(args, 9));
            return LuaValue.NONE;
        }));

        render.set("sweep", Lua.fn(args -> {
            Renderer gfx = requireDrawing();
            gfx.sweepOutline(checkRect(args, 1), Lua.checkColour(args, 5), Lua.optFloat(args, 6, 1.0f),
                    Lua.optFloat(args, 7, 0.0f));
            return LuaValue.NONE;
        }));
        render.set("text", Lua.fn(ApiRender::text));
        render.set("measure", Lua.fn(args -> LuaValue.valueOf(Renderer.instance().measure(optFontRole(args, 3),
                args.checkjstring(1), Lua.optFloat(args, 2, 12.0f)))));
        render.set("lineHeight", Lua.fn(args -> LuaValue.valueOf(
                Renderer.instance().lineHeight(optFontRole(args, 2), Lua.optFloat(args, 1, 12.0f)))));
        render.set("icon", Lua.fn(args -> {
            Renderer gfx = requireDrawing();
            String name = args.checkjstring(1);
            Icon icon = Icon.byName(name, null);
            if (icon == null) {
                throw Lua.error("no icon called '%s'", name);
            }
            gfx.icon(icon, checkRect(args, 2), Lua.optColour(args, 6, WHITE));
            return LuaValue.NONE;
        }));
        render.set("icons", Lua.fn(args -> {
            LuaTable list = new LuaTable();
            for (Icon icon : Icon.values()) {
                list.insert(0, LuaValue.valueOf(icon.file));
            }
            return list;
        }));
        render.set("clip", Lua.fn(args -> {
            Renderer gfx = requireDrawing();
            gfx.pushClip(checkRect(args, 1));
            ScriptManager.noteClipPush();
            return LuaValue.NONE;
        }));
        render.set("unclip", Lua.fn(args -> {
            requireDrawing();
            ScriptManager.noteClipPop();
            return LuaValue.NONE;
        }));
        render.set("size", Lua.fn(args -> {
            FrameState state = ApiCore.requireState();
            return LuaValue.varargsOf(LuaValue.valueOf(state.windowWidth), LuaValue.valueOf(state.windowHeight));
        }));
        render.set("scale", Lua.fn(args -> {
            FrameState state = ApiCore.requireState();
            return LuaValue.valueOf(state.scaledWidth > 0 ? (double) state.windowWidth / state.scaledWidth : 2.0);
        }));

        render.set("project", Lua.fn(args -> {
            Context context = ApiCore.requireContext();
            Projection.ScreenPoint screen = context.projection.project(Lua.readVec3(args, 1).value());
            if (screen == null) {
                return LuaValue.NIL;
            }
            return LuaValue.varargsOf(LuaValue.valueOf(screen.x()), LuaValue.valueOf(screen.y()), LuaValue.TRUE);
        }));
        render.set("projectBox", Lua.fn(args -> {
            Context context = ApiCore.requireContext();
            Lua.Vec3Arg minimum = Lua.readVec3(args, 1);
            Rect bounds = context.projection.projectBox(minimum.value(), Lua.readVec3(args, minimum.next()).value());
            if (bounds == null) {
                return LuaValue.NIL;
            }
            return LuaValue.varargsOf(new LuaValue[] {LuaValue.valueOf(bounds.x()), LuaValue.valueOf(bounds.y()),
                    LuaValue.valueOf(bounds.width()), LuaValue.valueOf(bounds.height())});
        }));
        cold.set("render", render);
    }
}
