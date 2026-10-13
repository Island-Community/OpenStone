package stone.hook;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public final class Input {
    public static final int RELEASE = 0;
    public static final int PRESS = 1;
    public static final int REPEAT = 2;

    public static final int KEY_ESCAPE = 256;
    public static final int KEY_ENTER = 257;
    public static final int KEY_TAB = 258;
    public static final int KEY_BACKSPACE = 259;
    public static final int KEY_INSERT = 260;
    public static final int KEY_DELETE = 261;
    public static final int KEY_RIGHT = 262;
    public static final int KEY_LEFT = 263;
    public static final int KEY_DOWN = 264;
    public static final int KEY_UP = 265;
    public static final int KEY_HOME = 268;
    public static final int KEY_END = 269;
    public static final int KEY_LEFT_SHIFT = 340;
    public static final int KEY_LEFT_CONTROL = 341;
    public static final int KEY_RIGHT_SHIFT = 344;
    public static final int KEY_RIGHT_CONTROL = 345;

    public static final int MOD_SHIFT = 1;
    public static final int MOD_CONTROL = 2;
    public static final int MOD_ALT = 4;

    public static final int MOUSE_LEFT = 0;
    public static final int MOUSE_RIGHT = 1;
    public static final int MOUSE_MIDDLE = 2;

    private static final int LAST_KEY = 348;
    private static final int MOUSE_BUTTONS = 8;

    public record Event(Kind kind, int code, int action, int modifiers, double x, double y) {
        public enum Kind { KEY, CHAR, MOUSE_BUTTON, MOUSE_MOVE, SCROLL }
    }

    private static final int[] toLwjgl = new int[LAST_KEY + 1];
    private static final int[] toGlfw = new int[256];

    static {
        int[] pairs = {
            32, Keyboard.KEY_SPACE, 39, Keyboard.KEY_APOSTROPHE, 44, Keyboard.KEY_COMMA,
            45, Keyboard.KEY_MINUS, 46, Keyboard.KEY_PERIOD, 47, Keyboard.KEY_SLASH,
            48, Keyboard.KEY_0, 49, Keyboard.KEY_1, 50, Keyboard.KEY_2, 51, Keyboard.KEY_3,
            52, Keyboard.KEY_4, 53, Keyboard.KEY_5, 54, Keyboard.KEY_6, 55, Keyboard.KEY_7,
            56, Keyboard.KEY_8, 57, Keyboard.KEY_9, 59, Keyboard.KEY_SEMICOLON, 61, Keyboard.KEY_EQUALS,
            65, Keyboard.KEY_A, 66, Keyboard.KEY_B, 67, Keyboard.KEY_C, 68, Keyboard.KEY_D,
            69, Keyboard.KEY_E, 70, Keyboard.KEY_F, 71, Keyboard.KEY_G, 72, Keyboard.KEY_H,
            73, Keyboard.KEY_I, 74, Keyboard.KEY_J, 75, Keyboard.KEY_K, 76, Keyboard.KEY_L,
            77, Keyboard.KEY_M, 78, Keyboard.KEY_N, 79, Keyboard.KEY_O, 80, Keyboard.KEY_P,
            81, Keyboard.KEY_Q, 82, Keyboard.KEY_R, 83, Keyboard.KEY_S, 84, Keyboard.KEY_T,
            85, Keyboard.KEY_U, 86, Keyboard.KEY_V, 87, Keyboard.KEY_W, 88, Keyboard.KEY_X,
            89, Keyboard.KEY_Y, 90, Keyboard.KEY_Z,
            91, Keyboard.KEY_LBRACKET, 92, Keyboard.KEY_BACKSLASH, 93, Keyboard.KEY_RBRACKET,
            96, Keyboard.KEY_GRAVE,
            KEY_ESCAPE, Keyboard.KEY_ESCAPE, KEY_ENTER, Keyboard.KEY_RETURN, KEY_TAB, Keyboard.KEY_TAB,
            KEY_BACKSPACE, Keyboard.KEY_BACK, KEY_INSERT, Keyboard.KEY_INSERT, KEY_DELETE, Keyboard.KEY_DELETE,
            KEY_RIGHT, Keyboard.KEY_RIGHT, KEY_LEFT, Keyboard.KEY_LEFT, KEY_DOWN, Keyboard.KEY_DOWN,
            KEY_UP, Keyboard.KEY_UP, 266, Keyboard.KEY_PRIOR, 267, Keyboard.KEY_NEXT,
            KEY_HOME, Keyboard.KEY_HOME, KEY_END, Keyboard.KEY_END,
            280, Keyboard.KEY_CAPITAL, 281, Keyboard.KEY_SCROLL, 282, Keyboard.KEY_NUMLOCK,
            283, Keyboard.KEY_SYSRQ, 284, Keyboard.KEY_PAUSE,
            290, Keyboard.KEY_F1, 291, Keyboard.KEY_F2, 292, Keyboard.KEY_F3, 293, Keyboard.KEY_F4,
            294, Keyboard.KEY_F5, 295, Keyboard.KEY_F6, 296, Keyboard.KEY_F7, 297, Keyboard.KEY_F8,
            298, Keyboard.KEY_F9, 299, Keyboard.KEY_F10, 300, Keyboard.KEY_F11, 301, Keyboard.KEY_F12,
            320, Keyboard.KEY_NUMPAD0, 321, Keyboard.KEY_NUMPAD1, 322, Keyboard.KEY_NUMPAD2,
            323, Keyboard.KEY_NUMPAD3, 324, Keyboard.KEY_NUMPAD4, 325, Keyboard.KEY_NUMPAD5,
            326, Keyboard.KEY_NUMPAD6, 327, Keyboard.KEY_NUMPAD7, 328, Keyboard.KEY_NUMPAD8,
            329, Keyboard.KEY_NUMPAD9, 330, Keyboard.KEY_DECIMAL, 331, Keyboard.KEY_DIVIDE,
            332, Keyboard.KEY_MULTIPLY, 333, Keyboard.KEY_SUBTRACT, 334, Keyboard.KEY_ADD,
            335, Keyboard.KEY_NUMPADENTER, 336, Keyboard.KEY_NUMPADEQUALS,
            KEY_LEFT_SHIFT, Keyboard.KEY_LSHIFT, KEY_LEFT_CONTROL, Keyboard.KEY_LCONTROL,
            342, Keyboard.KEY_LMENU, 343, Keyboard.KEY_LMETA,
            KEY_RIGHT_SHIFT, Keyboard.KEY_RSHIFT, KEY_RIGHT_CONTROL, Keyboard.KEY_RCONTROL,
            346, Keyboard.KEY_RMENU, 347, Keyboard.KEY_RMETA, 348, Keyboard.KEY_APPS,
        };
        for (int index = 0; index < pairs.length; index += 2) {
            toLwjgl[pairs[index]] = pairs[index + 1];
            toGlfw[pairs[index + 1]] = pairs[index];
        }
    }

    private static final List<Event> queue = new ArrayList<>();
    private static final boolean[] keysDown = new boolean[LAST_KEY + 1];
    private static final boolean[] buttonsDown = new boolean[MOUSE_BUTTONS];
    private static boolean capturing;
    private static double cursorX;
    private static double cursorY;

    private Input() {}

    public static int glfwKey(int lwjglKey) {
        return lwjglKey > 0 && lwjglKey < toGlfw.length ? toGlfw[lwjglKey] : 0;
    }

    public static boolean keyHeld(int key) {
        int code = key > 0 && key <= LAST_KEY ? toLwjgl[key] : 0;
        return code != 0 && Keyboard.isKeyDown(code);
    }

    public static boolean bindHeld(int code) {
        if (code > MOUSE_BUTTONS) {
            return keyHeld(code);
        }
        return buttonHeld(code);
    }

    public static boolean attackHeld() {
        return buttonHeld(MOUSE_LEFT);
    }

    public static boolean useHeld() {
        return buttonHeld(MOUSE_RIGHT);
    }

    private static boolean buttonHeld(int button) {
        return !capturing && button < Mouse.getButtonCount() && Mouse.isButtonDown(button);
    }

    public static double cursorX() {
        return cursorX;
    }

    public static double cursorY() {
        return cursorY;
    }

    public static void setCapturing(boolean value) {
        capturing = value;
        for (int key = 0; key <= LAST_KEY; ++key) {
            keysDown[key] = toLwjgl[key] != 0 && Keyboard.isKeyDown(toLwjgl[key]);
        }
        for (int button = 0; button < MOUSE_BUTTONS; ++button) {
            buttonsDown[button] = button < Mouse.getButtonCount() && Mouse.isButtonDown(button);
        }
    }

    public static int modifiers() {
        int modifiers = 0;
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            modifiers |= MOD_SHIFT;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
            modifiers |= MOD_CONTROL;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU)) {
            modifiers |= MOD_ALT;
        }
        return modifiers;
    }

    static double flipY(int lwjglY) {
        return Minecraft.getMinecraft().displayHeight - lwjglY - 1;
    }

    static void push(Event event) {
        if (event.kind() == Event.Kind.MOUSE_MOVE || event.kind() == Event.Kind.MOUSE_BUTTON) {
            cursorX = event.x();
            cursorY = event.y();
        }
        if (queue.size() < 256) {
            queue.add(event);
        }
    }

    public static void poll() {
        if (capturing) {
            return;
        }
        double x = Mouse.getX();
        double y = flipY(Mouse.getY());
        if (x != cursorX || y != cursorY) {
            push(new Event(Event.Kind.MOUSE_MOVE, 0, 0, 0, x, y));
        }
        int modifiers = modifiers();
        for (int key = 0; key <= LAST_KEY; ++key) {
            if (toLwjgl[key] == 0) {
                continue;
            }
            boolean down = Keyboard.isKeyDown(toLwjgl[key]);
            if (down != keysDown[key]) {
                keysDown[key] = down;
                push(new Event(Event.Kind.KEY, key, down ? PRESS : RELEASE, modifiers, 0, 0));
            }
        }
        int buttons = Math.min(MOUSE_BUTTONS, Mouse.getButtonCount());
        for (int button = 0; button < buttons; ++button) {
            boolean down = Mouse.isButtonDown(button);
            if (down != buttonsDown[button]) {
                buttonsDown[button] = down;
                push(new Event(Event.Kind.MOUSE_BUTTON, button, down ? PRESS : RELEASE, modifiers, x, y));
            }
        }
    }

    public static List<Event> drain() {
        List<Event> events = new ArrayList<>(queue);
        queue.clear();
        return events;
    }
}
