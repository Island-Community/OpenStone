package stone.script;

import stone.core.Rect;
import stone.render.Renderer;
import stone.ui.Theme;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

final class HudElement {
    final Script owner;
    final String name;
    private LuaTable table;
    private float x = 4.0f;
    private float y = 4.0f;
    private float width = 70.0f;
    private float height = 16.0f;
    private boolean showing = true;

    HudElement(Script owner, String name) {
        this.owner = owner;
        this.name = name;
    }

    void bind(LuaTable value) {
        table = value;
    }

    LuaTable table() {
        return table;
    }

    float x() {
        return x;
    }

    float y() {
        return y;
    }

    float width() {
        return width;
    }

    float height() {
        return height;
    }

    boolean showing() {
        return showing;
    }

    void setShowing(boolean value) {
        showing = value;
    }

    void setPosition(float newX, float newY) {
        x = newX;
        y = newY;
    }

    void setSize(float newWidth, float newHeight) {
        width = Math.max(4.0f, newWidth);
        height = Math.max(4.0f, newHeight);
    }

    Rect bounds(float unit) {
        return new Rect(x * unit, y * unit, width * unit, height * unit);
    }

    boolean visible() {
        if (!showing || !owner.running() || table == null) {
            return false;
        }
        Varargs result = owner.protect(() -> {
            LuaValue predicate = table.get("visible");
            return predicate.isfunction() ? predicate.call(table) : LuaValue.TRUE;
        });
        return result != null && result.arg1().toboolean();
    }

    void render(float unit, boolean menuOpen, boolean dragging) {
        if (!owner.running() || table == null) {
            return;
        }
        Rect rect = bounds(unit);

        if (menuOpen) {
            Renderer renderer = Renderer.instance();
            renderer.rect(rect, Theme.SURFACE.withAlpha(dragging ? 0.9f : 0.55f), unit * 2.0f);
            renderer.outline(rect, (dragging ? Theme.accent() : Theme.BORDER).withAlpha(0.9f),
                    Math.max(1.0f, unit * 0.5f), unit * 2.0f);
        }
        owner.protect(() -> {
            LuaValue callback = table.get("render");
            if (!callback.isfunction()) {
                return LuaValue.NONE;
            }

            LuaTable frame = new LuaTable();
            frame.set("x", rect.x());
            frame.set("y", rect.y());
            frame.set("width", rect.width());
            frame.set("height", rect.height());
            frame.set("unit", unit);
            frame.set("dragging", LuaValue.valueOf(dragging));
            frame.set("menu", LuaValue.valueOf(menuOpen));
            return callback.call(table, frame);
        });
    }
}
