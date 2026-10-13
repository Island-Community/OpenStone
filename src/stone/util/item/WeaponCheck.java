package stone.util.item;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemSword;

public final class WeaponCheck {
    private WeaponCheck() {
    }

    public static boolean isHoldingEnabledWeapon(EntityLivingBase entity) {
        if (entity.getHeldItem() == null) {
            return false;
        }
        Item item = entity.getHeldItem().getItem();
        return item instanceof ItemSword || item == Items.stick;
    }
}
