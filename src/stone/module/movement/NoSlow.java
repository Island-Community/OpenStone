package stone.module.movement;

import stone.module.Category;
import stone.module.Module;
import stone.module.ModuleManager;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.module.combat.AutoBlock;
import stone.module.movement.noslow.NoSlowMode;
import stone.util.ClientUtils;
import java.util.List;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;

public class NoSlow extends Module {
    public static Setting swordMode;
    public static Setting foodMode;
    public static Setting bowMode;
    public static Setting slow;

    public NoSlow() {
        super("NoSlow", "Walks at full speed while using items", Category.MOVEMENT, Subcategory.ADVANCED);
        swordMode = addChoice("Sword Mode", "", List.of(NoSlowMode.labels()), NoSlowMode.NONE.ordinal());
        foodMode = addChoice("Food Mode", "", List.of(NoSlowMode.labels()), NoSlowMode.NONE.ordinal());
        bowMode = addChoice("Bow Mode", "", List.of(NoSlowMode.labels()), NoSlowMode.NONE.ordinal());
        slow = addNumber("Slow %", "", 80.0F, 0.0F, 80.0F, 0);
        slow.setVisible(this.MFejozs());
    }

    @Override
    public void guiUpdate() {
        slow.setVisible(this.MFejozs());
    }

    public static boolean isVanillaModeActive() {
        return !hAugQ() && getActiveMode() == NoSlowMode.VANILLA;
    }

    private static boolean hAugQ() {
        return ModuleManager.get(AutoBlock.class) != null && ModuleManager.get(AutoBlock.class).isSwapBlocking();
    }

    private static NoSlowMode getActiveMode() {
        if (ClientUtils.isInWorld() && ModuleManager.get(NoSlow.class) != null && ModuleManager.get(NoSlow.class).enabled()) {
            ItemStack var0 = mc.thePlayer.getHeldItem();
            if (var0 == null || !mc.thePlayer.isUsingItem()) {
                return NoSlowMode.NONE;
            } else if (var0.getItem() instanceof ItemSword) {
                return FPZSd(swordMode);
            } else if (isConsumableItem(var0)) {
                return FPZSd(foodMode);
            } else {
                return var0.getItem() instanceof ItemBow ? FPZSd(bowMode) : NoSlowMode.NONE;
            }
        } else {
            return NoSlowMode.NONE;
        }
    }

    private static NoSlowMode FPZSd(Setting var0) {
        return NoSlowMode.fromSetting(var0.choice());
    }

    private static boolean isConsumableItem(ItemStack var0) {
        return var0.getItem() instanceof ItemFood || var0.getItem() instanceof ItemPotion && !ItemPotion.isSplash(var0.getItemDamage());
    }

    private boolean MFejozs() {
        return FPZSd(swordMode) == NoSlowMode.VANILLA || FPZSd(foodMode) == NoSlowMode.VANILLA || FPZSd(bowMode) == NoSlowMode.VANILLA;
    }

    @Override
    public String suffix() {
        return FPZSd(swordMode).getLabel();
    }
}
