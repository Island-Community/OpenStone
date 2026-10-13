package stone.module;

import stone.render.Icon;

public enum Category {
    COMBAT("Combat", Icon.SWORDS, null),
    PLAYER("Player", Icon.USER_ROUND, null),
    MOVEMENT("Movement", Icon.FOOTPRINTS, PLAYER),
    VISUALS("Visuals", Icon.EYE, null),
    HUD("HUD", Icon.MONITOR, VISUALS),
    OTHER("Other", Icon.WRENCH, null),
    MINIGAMES("Minigames", Icon.FLAME, OTHER),
    CLIENT("Client", Icon.SNOWFLAKE, OTHER),
    SCRIPTS("Scripts", Icon.BRACES, null);

    public final String displayName;
    public final Icon icon;

    public final Category tab;

    Category(String displayName, Icon icon, Category parent) {
        this.displayName = displayName;
        this.icon = icon;
        this.tab = parent == null ? this : parent;
    }
}
