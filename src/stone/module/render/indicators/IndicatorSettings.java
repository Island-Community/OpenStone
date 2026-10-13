package stone.module.render.indicators;

import stone.module.Module;
import stone.module.Setting;

public final class IndicatorSettings {
    public final Setting renderArrows;
    public final Setting renderEnderPearls;
    public final Setting renderFireballs;
    public final Setting renderEggs;
    public final Setting renderSnowballs;
    public final Setting radius;
    public final Setting itemColors;
    public final Setting renderItem;
    public final Setting renderDistance;
    public final Setting onlyWhenApproaching;
    public final Setting renderOnlyOffscreen;

    public IndicatorSettings(Module var1) {
        this.renderArrows = var1.addBoolean("Render arrows", "", true);
        this.renderEnderPearls = var1.addBoolean("Render ender pearls", "", true);
        this.renderFireballs = var1.addBoolean("Render fireballs", "", true);
        this.renderEggs = var1.addBoolean("Render eggs", "", false);
        this.renderSnowballs = var1.addBoolean("Render snowballs", "", false);
        this.radius = var1.addNumber("Radius", "", 50.0F, 30.0F, 200.0F, 0);
        this.itemColors = var1.addBoolean("Item colors", "", true);
        this.renderItem = var1.addBoolean("Render item", "", true);
        this.renderDistance = var1.addBoolean("Render distance", "", true);
        this.onlyWhenApproaching = var1.addBoolean("Only when approaching", "", false);
        this.renderOnlyOffscreen = var1.addBoolean("Render only offscreen", "", false);
    }

}
