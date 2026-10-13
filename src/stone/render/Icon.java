package stone.render;

public enum Icon {
    ACTIVITY("activity"),
    BOX("box"),
    BRACES("braces"),
    CHECK("check"),
    CHEVRON_DOWN("chevron-down"),
    CHEVRON_LEFT("chevron-left"),
    CHEVRON_RIGHT("chevron-right"),
    CHEVRON_UP("chevron-up"),
    CPU("cpu"),
    CROSSHAIR("crosshair"),
    EYE("eye"),
    FLAME("flame"),
    FOOTPRINTS("footprints"),
    GAUGE("gauge"),
    GLOBE("globe"),
    HAMMER("hammer"),
    KEYBOARD("keyboard"),
    LAYERS("layers"),
    LOCK("lock"),
    MINUS("minus"),
    MONITOR("monitor"),
    MOVE_3D("move-3d"),
    PALETTE("palette"),
    PIN("pin"),
    PLUS("plus"),
    RADIO("radio"),
    ROTATE_CCW("rotate-ccw"),
    SEARCH("search"),
    SETTINGS("settings"),
    SETTINGS_2("settings-2"),
    SLIDERS_HORIZONTAL("sliders-horizontal"),
    SNOWFLAKE("snowflake"),
    SWORDS("swords"),
    TARGET("target"),
    USER_ROUND("user-round"),
    WIFI("wifi"),
    WRENCH("wrench"),
    X("x"),
    ZAP("zap");

    public final String file;

    Icon(String file) {
        this.file = file;
    }

    public static Icon byName(String name, Icon fallback) {
        for (Icon icon : values()) {
            if (icon.file.equals(name)) {
                return icon;
            }
        }
        return fallback;
    }
}
