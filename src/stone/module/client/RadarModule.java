package stone.module.client;

import stone.hook.Input;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.ModuleManager;
import stone.module.Subcategory;
import stone.render.Icon;
import stone.ui.Client;

public final class RadarModule extends Module {
    public RadarModule() {
        super("Radar", "Everybody in a loaded chunk, and what they are carrying", Category.HUD, Subcategory.ADVANCED,
                Input.KEY_RIGHT_CONTROL);
        setIcon(Icon.RADIO);
    }

    @Override
    public void onEnable(Context context) {

        Client.radar().open();
        ClickGuiModule menu = ModuleManager.get(ClickGuiModule.class);
        if (menu != null && menu.enabled()) {
            menu.setEnabled(false, context);
        }
    }

    @Override
    public void onDisable(Context context) {
        Client.radar().close();
    }

    @Override
    public void onRender(Context context) {

        if (enabled() && !Client.radar().interactive()) {
            setEnabled(false, null);
        }
    }
}
