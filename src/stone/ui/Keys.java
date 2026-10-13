package stone.ui;

public final class Keys {
    private Keys() {}

    public static String keyName(int key) {
        if (key == 0) {
            return "none";
        }
        if ((key >= 'A' && key <= 'Z') || (key >= '0' && key <= '9')) {
            return String.valueOf((char) key);
        }
        if (key >= 290 && key <= 301) {
            return "f" + (key - 289);
        }
        if (key >= 320 && key <= 329) {
            return "num" + (key - 320);
        }
        return switch (key) {
            case 32 -> "space";
            case 39 -> "'";
            case 44 -> ",";
            case 45 -> "-";
            case 46 -> ".";
            case 47 -> "/";
            case 59 -> ";";
            case 61 -> "=";
            case 91 -> "[";
            case 92 -> "\\";
            case 93 -> "]";
            case 96 -> "`";
            case 256 -> "esc";
            case 257 -> "enter";
            case 258 -> "tab";
            case 259 -> "back";
            case 260 -> "ins";
            case 261 -> "del";
            case 262 -> "right";
            case 263 -> "left";
            case 264 -> "down";
            case 265 -> "up";
            case 266 -> "pgup";
            case 267 -> "pgdn";
            case 268 -> "home";
            case 269 -> "end";
            case 280 -> "caps";
            case 284 -> "pause";
            case 340 -> "lshift";
            case 341 -> "lctrl";
            case 342 -> "lalt";
            case 344 -> "rshift";
            case 345 -> "rctrl";
            case 346 -> "ralt";
            default -> "#" + key;
        };
    }
}
