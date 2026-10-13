package stone.module.movement;

import stone.event.MoveInputEvent;
import stone.event.Subscribe;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.Subcategory;
import stone.module.movement.nullmove.OppositeKeyResolver;
import stone.util.ClientUtils;

public class NullMove extends Module {
    private final OppositeKeyResolver forwardInputTracker = new OppositeKeyResolver();
    private final OppositeKeyResolver strafeInputTracker = new OppositeKeyResolver();

    public NullMove() {
        super("NullMove", "The last key pressed wins when both directions are held", Category.MOVEMENT,
                Subcategory.BASIC);
    }

    @Override
    public void onDisable(Context context) {
        this.forwardInputTracker.reset();
        this.strafeInputTracker.reset();
    }

    @Subscribe(priority = Subscribe.Priority.HIGH)
    public void onMoveInput(MoveInputEvent var1) {
        if (this.enabled()) {
            if (ClientUtils.isInWorld() && mc.currentScreen == null) {
                boolean var2 = mc.gameSettings.keyBindForward.isKeyDown();
                boolean var3 = mc.gameSettings.keyBindBack.isKeyDown();
                boolean var4 = mc.gameSettings.keyBindLeft.isKeyDown();
                boolean var5 = mc.gameSettings.keyBindRight.isKeyDown();
                int var6 = this.forwardInputTracker.resolveDirection(var2, var3);
                int var7 = this.strafeInputTracker.resolveDirection(var4, var5);
                if (var6 != 0) {
                    var1.setMoveForward(var6);
                }

                if (var7 != 0) {
                    var1.setMoveStrafe(var7);
                }
            }
        }
    }
}
