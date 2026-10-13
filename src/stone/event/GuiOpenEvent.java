package stone.event;

import net.minecraft.client.gui.GuiScreen;

public final class GuiOpenEvent extends Event {
    public GuiScreen guiScreen;

    public GuiOpenEvent(GuiScreen guiScreen) {
        this.guiScreen = guiScreen;
    }
}
