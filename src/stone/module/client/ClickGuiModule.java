package stone.module.client;

import stone.core.Color;
import stone.hook.Input;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.ModuleManager;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.render.Icon;
import stone.ui.ClickGui;
import stone.ui.Client;
import stone.ui.Theme;

public final class ClickGuiModule extends Module {
    private final Setting blur;
    private final Setting accent;

    public ClickGuiModule() {
        super("ClickGUI", "Opens the Stone menu", Category.CLIENT, Subcategory.BASIC, Input.KEY_INSERT);
        setIcon(Icon.SLIDERS_HORIZONTAL);
        blur = addNumber("Backdrop blur", "Frost the scene behind the panels", 1.0f, 0.0f, 1.5f, 2);
        accent = addColour("Accent", "Drives the menu, the HUD and the ESP", Color.rgb(0xBDA4BD));
    }

    @Override
    protected boolean announcesToggle() {
        return false;
    }

    @Override
    public void onEnable(Context context) {
        ClickGui gui = Client.gui();
        gui.setBlurStrength(blur.number());
        Theme.setAccent(accent.colour());
        gui.open();

        RadarModule radar = ModuleManager.get(RadarModule.class);
        if (radar != null && radar.enabled()) {
            radar.setEnabled(false, context);
        }
    }

    @Override
    public void onDisable(Context context) {
        Client.gui().close();
    }

    @Override
    public void onRender(Context context) {
        ClickGui gui = Client.gui();
        gui.setBlurStrength(blur.number());
        Theme.setAccent(accent.colour());

        if (enabled() && !gui.interactive()) {
            setEnabled(false, null);
        }
    }
}
