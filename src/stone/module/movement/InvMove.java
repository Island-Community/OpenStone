package stone.module.movement;

import stone.game.FrameState;
import stone.game.Game;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.Subcategory;

public final class InvMove extends Module {
    private static final String[] BINDINGS = {"forward", "back", "left", "right", "jump", "sneak"};

    private boolean active;

    public InvMove() {
        super("InvMove", "Walks while the inventory is open", Category.MOVEMENT, Subcategory.BASIC);
    }

    private void releaseAll() {
        for (String binding : BINDINGS) {
            Game.setBinding(binding, false);
        }
        active = false;
    }

    @Override
    public void onDisable(Context context) {
        if (active) {
            releaseAll();
        }
    }

    @Override
    public void onTick(Context context) {
        FrameState state = context.state;
        if (!state.inWorld || !state.player.valid || !state.screenOpen || !Game.inventoryOpen()) {
            if (active) {
                releaseAll();
            }
            return;
        }
        active = true;
        for (String binding : BINDINGS) {
            Game.setBinding(binding, Game.bindingPhysicallyHeld(binding));
        }
    }
}
