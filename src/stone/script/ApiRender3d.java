package stone.script;

import stone.core.Color;
import stone.core.Maths;
import stone.core.Rect;
import stone.core.Vec3;
import stone.game.FrameState;
import stone.game.Projection;
import stone.render.Align;
import stone.render.FontRole;
import stone.render.Renderer;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

final class ApiRender3d {
    private static final Color WHITE = Color.rgb(0xFFFFFF);

    private static final int[][] EDGES = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4},
            {1, 5}, {2, 6}, {3, 7}};
    private static final int[][] FACES = {{0, 1, 2, 3}, {4, 5, 6, 7}, {0, 1, 5, 4}, {2, 3, 7, 6}, {1, 2, 6, 5},
            {3, 0, 4, 7}};

    private record Box(Vec3 minimum, Vec3 maximum, int next) {}

    private ApiRender3d() {}

    private static Projection requireProjection() {
        Projection projection = ApiCore.requireContext().projection;
        if (!projection.valid()) {
            throw Lua.error("the frame has no camera to project with");
        }
        return projection;
    }

    private static void drawSegment(Renderer gfx, Projection projection, Vec3 from, Vec3 to, float thickness,
            Color colour) {
        Projection.ScreenPoint start = projection.project(from);
        Projection.ScreenPoint end = projection.project(to);
        if (start != null && end != null) {
            gfx.line(start.x(), start.y(), end.x(), end.y(), thickness, colour);
        }
    }

    private static Vec3[] corners(Vec3 minimum, Vec3 maximum) {
        return new Vec3[] {
            new Vec3(minimum.x(), minimum.y(), minimum.z()), new Vec3(maximum.x(), minimum.y(), minimum.z()),
            new Vec3(maximum.x(), minimum.y(), maximum.z()), new Vec3(minimum.x(), minimum.y(), maximum.z()),
            new Vec3(minimum.x(), maximum.y(), minimum.z()), new Vec3(maximum.x(), maximum.y(), minimum.z()),
            new Vec3(maximum.x(), maximum.y(), maximum.z()), new Vec3(minimum.x(), maximum.y(), maximum.z()),
        };
    }

    private static Box readBox(Varargs args, int index) {
        Lua.Vec3Arg first = Lua.readVec3(args, index);
        Lua.Vec3Arg second = Lua.readVec3(args, first.next());
        Vec3 a = first.value();
        Vec3 b = second.value();
        return new Box(new Vec3(Math.min(a.x(), b.x()), Math.min(a.y(), b.y()), Math.min(a.z(), b.z())),
                new Vec3(Math.max(a.x(), b.x()), Math.max(a.y(), b.y()), Math.max(a.z(), b.z())), second.next());
    }

    private static Varargs text(Varargs args) {
        Renderer gfx = ApiRender.requireDrawing();
        Projection projection = requireProjection();
        Lua.Vec3Arg point = Lua.readVec3(args, 1);
        int next = point.next();
        String value = args.checkjstring(next);
        float size = Lua.optFloat(args, next + 1, 10.0f);
        Color colour = Lua.optColour(args, next + 2, WHITE);
        boolean shadow = Lua.optBoolean(args, next + 3, true);

        Projection.ScreenPoint screen = projection.project(point.value());
        if (screen == null) {
            return LuaValue.FALSE;
        }
        if (shadow) {
            float offset = Math.max(1.0f, size * 0.09f);
            gfx.textCap(FontRole.MEDIUM, value, screen.x() + offset, screen.y() + offset, size,
                    Color.rgb(0x000000, 0.55f * colour.a()), Align.CENTER);
        }
        gfx.textCap(FontRole.MEDIUM, value, screen.x(), screen.y(), size, colour, Align.CENTER);
        return LuaValue.TRUE;
    }

    static void install(LuaTable cold) {
        LuaTable render3d = new LuaTable();
        render3d.set("line", Lua.fn(args -> {
            Renderer gfx = ApiRender.requireDrawing();
            Projection projection = requireProjection();
            Lua.Vec3Arg from = Lua.readVec3(args, 1);
            Lua.Vec3Arg to = Lua.readVec3(args, from.next());
            Color colour = Lua.checkColour(args, to.next());
            drawSegment(gfx, projection, from.value(), to.value(), Lua.optFloat(args, to.next() + 1, 1.5f), colour);
            return LuaValue.NONE;
        }));
        render3d.set("box", Lua.fn(args -> {
            Renderer gfx = ApiRender.requireDrawing();
            Projection projection = requireProjection();
            Box box = readBox(args, 1);
            Color colour = Lua.checkColour(args, box.next());
            float thickness = Lua.optFloat(args, box.next() + 1, 1.5f);
            Vec3[] points = corners(box.minimum(), box.maximum());
            for (int[] edge : EDGES) {
                drawSegment(gfx, projection, points[edge[0]], points[edge[1]], thickness, colour);
            }
            return LuaValue.NONE;
        }));
        render3d.set("filledBox", Lua.fn(args -> {
            Renderer gfx = ApiRender.requireDrawing();
            Projection projection = requireProjection();
            Box box = readBox(args, 1);
            Color colour = Lua.checkColour(args, box.next());
            Vec3[] points = corners(box.minimum(), box.maximum());
            Projection.ScreenPoint[] screen = new Projection.ScreenPoint[8];
            for (int index = 0; index < 8; index++) {
                screen[index] = projection.project(points[index]);
            }
            for (int[] face : FACES) {
                float[] xs = new float[4];
                float[] ys = new float[4];
                boolean visible = true;
                for (int corner = 0; corner < 4 && visible; corner++) {
                    Projection.ScreenPoint point = screen[face[corner]];
                    visible = point != null;
                    if (visible) {
                        xs[corner] = point.x();
                        ys[corner] = point.y();
                    }
                }
                if (visible) {
                    gfx.face(xs, ys, colour);
                }
            }
            return LuaValue.NONE;
        }));

        render3d.set("tracer", Lua.fn(args -> {
            Renderer gfx = ApiRender.requireDrawing();
            Projection projection = requireProjection();
            FrameState state = ApiCore.requireState();
            Lua.Vec3Arg point = Lua.readVec3(args, 1);
            Color colour = Lua.checkColour(args, point.next());
            float thickness = Lua.optFloat(args, point.next() + 1, 1.5f);
            Projection.ScreenPoint screen = projection.project(point.value());
            if (screen != null) {
                gfx.line(state.windowWidth * 0.5f, state.windowHeight, screen.x(), screen.y(), thickness, colour);
            }
            return LuaValue.NONE;
        }));
        render3d.set("marker", Lua.fn(args -> {
            Renderer gfx = ApiRender.requireDrawing();
            Projection projection = requireProjection();
            Lua.Vec3Arg point = Lua.readVec3(args, 1);
            float size = Lua.optFloat(args, point.next(), 4.0f);
            Color colour = Lua.optColour(args, point.next() + 1, WHITE);
            Projection.ScreenPoint screen = projection.project(point.value());
            if (screen != null) {
                gfx.rect(new Rect(screen.x() - size * 0.5f, screen.y() - size * 0.5f, size, size), colour, size * 0.5f);
            }
            return LuaValue.NONE;
        }));

        render3d.set("circle", Lua.fn(args -> {
            Renderer gfx = ApiRender.requireDrawing();
            Projection projection = requireProjection();
            Lua.Vec3Arg centreArg = Lua.readVec3(args, 1);
            Vec3 centre = centreArg.value();
            int next = centreArg.next();
            double radius = args.optdouble(next, 1.0);
            Color colour = Lua.optColour(args, next + 1, WHITE);
            int segments = Maths.clamp(args.optint(next + 2, 32), 3, 128);
            float thickness = Lua.optFloat(args, next + 3, 1.5f);
            Vec3 previous = new Vec3(centre.x() + radius, centre.y(), centre.z());
            for (int step = 1; step <= segments; step++) {
                double angle = Math.PI * 2.0 * step / segments;
                Vec3 point = new Vec3(centre.x() + Math.cos(angle) * radius, centre.y(),
                        centre.z() + Math.sin(angle) * radius);
                drawSegment(gfx, projection, previous, point, thickness, colour);
                previous = point;
            }
            return LuaValue.NONE;
        }));
        render3d.set("text", Lua.fn(ApiRender3d::text));

        render3d.set("visible", Lua.fn(args ->
                LuaValue.valueOf(requireProjection().project(Lua.readVec3(args, 1).value()) != null)));
        cold.set("render3d", render3d);
    }
}
