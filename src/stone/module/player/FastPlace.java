package stone.module.player;

import stone.core.Timer;
import stone.game.FrameState;
import stone.game.Game;
import stone.hook.Input;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.render.Icon;

public final class FastPlace extends Module {
    private final Setting delay = addNumber("Tick delay", "Ticks between uses; vanilla is four", 1, 0, 3, 0);
    private final Setting activation = addNumber("Activation time",
            "Milliseconds the button must be held first", 0, 0, 100, 0);
    private final Setting blocksOnly = addBoolean("Blocks only", "Only while a placeable block is in hand", true);
    private final Setting pitchCheck = addBoolean("Pitch check",
            "Only while looking down past seventy degrees", false);

    private final Timer heldSince = new Timer();
    private boolean wasHeld;

    public FastPlace() {
        super("FastPlace", "Removes the four tick wait between right clicks", Category.PLAYER, Subcategory.BASIC);
        setIcon(Icon.HAMMER);
    }

    @Override
    public void onEnable(Context context) {
        wasHeld = false;
    }

    @Override
    public void onTick(Context context) {
        FrameState state = context.state;
        if (!state.inWorld || !state.player.valid || state.screenOpen
                || !(Input.useHeld() || state.player.usingItem)) {
            wasHeld = false;
            return;
        }
        if (!wasHeld) {
            wasHeld = true;
            heldSince.reset();
        }
        if (!heldSince.elapsed((long) activation.number())) {
            return;
        }
        if (blocksOnly.bool() && !Game.holdingBlock()) {
            return;
        }
        if (pitchCheck.bool() && state.player.pitch < 70.0f) {
            return;
        }

        int ticks = (int) delay.number();
        if (Game.itemUseCooldown() > ticks) {
            Game.setItemUseCooldown(ticks);
        }
    }
}
