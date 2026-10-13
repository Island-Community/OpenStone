package stone.module.combat;

import stone.event.GameLoopEvent;
import stone.event.LeftClickEvent;
import stone.event.PacketSendEvent;
import stone.event.Subscribe;
import stone.event.TickStartEvent;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.module.combat.attributeswap.ItemScorer;
import stone.util.ClientUtils;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class AttributeSwap extends Module {
    private final Setting rightMouseHeld;
    private final Setting swapDelay;
    private int previousSlot = -1;
    private long restoreAtNanos = -1L;
    private boolean attackPending;

    public AttributeSwap() {
        super("AttributeSwap", "Swaps to your best sword for the hit, then back", Category.COMBAT, Subcategory.BASIC);
        this.rightMouseHeld = addBoolean("Right mouse held", "", false);
        this.swapDelay = addNumber("Swap delay", "", 50.0F, 0.0F, 250.0F, 0);
    }

    @Override
    public void onEnable(Context context) {
        this.previousSlot = -1;
        this.restoreAtNanos = -1L;
        this.attackPending = false;
    }

    @Override
    public void onDisable(Context context) {
        this.restorePreviousSlot();
        this.attackPending = false;
    }

    @Subscribe
    public void onTickStart(TickStartEvent var1) {
        if (!ClientUtils.isInWorld()) {
            this.previousSlot = -1;
            this.restoreAtNanos = -1L;
            this.attackPending = false;
        } else if (this.attackPending && !this.bjFs()) {
            this.attackPending = false;
        }
    }

    @Subscribe
    public void onGameLoop(GameLoopEvent var1) {
        if (this.previousSlot >= 0 && System.nanoTime() >= this.restoreAtNanos) {
            this.restorePreviousSlot();
        }
    }

    @Subscribe(priority = Subscribe.Priority.LOWEST)
    public void onPacketSend(PacketSendEvent var1) {
        if (!var1.cancelled()
            && var1.getPacket() instanceof C02PacketUseEntity
            && ((C02PacketUseEntity)var1.getPacket()).getAction() == Action.ATTACK
            && ClientUtils.isInWorld()
            && this.bjFs()) {
            this.attackPending = true;
        }
    }

    @Subscribe
    public void onLeftClick(LeftClickEvent var1) {
        if (this.attackPending
            && ClientUtils.isInWorld()
            && mc.leftClickCounter <= 0
            && mc.objectMouseOver != null
            && mc.objectMouseOver.typeOfHit == MovingObjectType.ENTITY
            && this.bjFs()
            && (!this.rightMouseHeld.bool() || ClientUtils.xusXfhC(mc.gameSettings.keyBindUseItem))) {
            int var2 = this.findBestSwordSlot();
            if (var2 >= 0 && var2 != mc.thePlayer.inventory.currentItem) {
                this.attackPending = false;
                this.previousSlot = mc.thePlayer.inventory.currentItem;
                this.restoreAtNanos = System.nanoTime() + (long)(this.swapDelay.number() * 1000000.0);
                mc.thePlayer.inventory.currentItem = var2;
                mc.playerController.syncCurrentPlayItem();
            }
        }
    }

    private void restorePreviousSlot() {
        int var1 = this.previousSlot;
        this.previousSlot = -1;
        this.restoreAtNanos = -1L;
        if (var1 >= 0 && var1 < 9 && ClientUtils.isInWorld() && mc.playerController != null) {
            if (mc.thePlayer.inventory.currentItem != var1) {
                mc.thePlayer.inventory.currentItem = var1;
                mc.playerController.syncCurrentPlayItem();
            }
        }
    }

    private boolean bjFs() {
        ItemStack var1 = mc.thePlayer.getHeldItem();
        return var1 != null && (var1.getItem() == Items.golden_pickaxe || EnchantmentHelper.getEnchantmentLevel(Enchantment.knockback.effectId, var1) > 0);
    }

    private int findBestSwordSlot() {
        int var1 = -1;
        double var2 = Double.NEGATIVE_INFINITY;

        for (int var4 = 0; var4 < 9; var4++) {
            ItemStack var5 = mc.thePlayer.inventory.getStackInSlot(var4);
            if (var5 != null && var5.getItem() instanceof ItemSword) {
                double var6 = ItemScorer.getTotalMeleeDamage(var5);
                if (var6 > var2) {
                    var2 = var6;
                    var1 = var4;
                }
            }
        }

        return var1;
    }
}
