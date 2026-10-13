package stone.util;

import stone.event.EventBus;
import stone.event.MouseEvent;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import org.lwjgl.input.Mouse;

public final class InputHookManager {
    private static final ByteBuffer mouseButtons = mouseButtons();

    private InputHookManager() {
    }

    private static ByteBuffer mouseButtons() {
        try {
            Field var0 = Mouse.class.getDeclaredField("buttons");
            var0.setAccessible(true);
            return (ByteBuffer)var0.get(null);
        } catch (ReflectiveOperationException var1) {
            throw new IllegalStateException("LWJGL Mouse.buttons is unavailable", var1);
        }
    }

    public static void simulateMouseButton(int var0, boolean var1) {
        EventBus.post(new MouseEvent(var0, var1, Mouse.getX(), Mouse.getY(), 0, 0, 0));
        setMouseButtonState(var0, var1);
    }

    public static void setMouseButtonState(int var0, boolean var1) {
        mouseButtons.put(var0, (byte)(var1 ? 1 : 0));
    }
}
