package stone.module.movement;

import stone.game.FrameState;
import stone.game.Game;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.Subcategory;

public final class Sprint extends Module {
    public Sprint() {
        super("Sprint", "Keeps sprinting while moving", Category.MOVEMENT, Subcategory.BASIC);
    }

    @Override
    public void onDisable(Context context) {
        if (context.state.player.valid) {
            Game.setSprinting(false);
        }
    }

    @Override
    public void onTick(Context context) {
        FrameState state = context.state;
        if (!state.inWorld || !state.player.valid || state.screenOpen || state.player.sneaking) {
            return;
        }

        boolean moving = Game.bindingPressed("forward") || Game.bindingPressed("back")
                || Game.bindingPressed("left") || Game.bindingPressed("right");
        if (moving && !state.player.sprinting) {
            Game.setSprinting(true);
        }
    }
}
