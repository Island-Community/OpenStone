package stone.module.player;

import stone.event.PacketSendEvent;
import stone.event.Subscribe;
import stone.event.TickStartEvent;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.module.player.autoswap.SwapStateTracker;
import stone.util.ClientUtils;
import stone.util.block.BlockList;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;

public class AutoSwap extends Module {
    private final Setting swapAt;
    private final Setting useBlockWhitelist;
    private final BlockList whitelistedBlocks;
    private final Setting blockBlacklist;
    private final BlockList blacklistedBlocks;
    private final Setting lookingDown;
    private final Setting movingBackwards;
    private final SwapStateTracker Sxek = new SwapStateTracker();

    public AutoSwap() {
        super("AutoSwap", "Moves to the next block stack when one runs out", Category.PLAYER, Subcategory.BASIC);
        this.swapAt = addNumber("Swap at", "", 1.0F, 1.0F, 64.0F, 0);
        this.useBlockWhitelist = addBoolean("Use block whitelist", "", false);
        this.whitelistedBlocks = new BlockList();
        this.lookingDown = addBoolean("Looking down", "", false);
        this.movingBackwards = addBoolean("Moving backwards", "", false);
        this.blockBlacklist = addBoolean("Block blacklist", "", false);
        this.blacklistedBlocks = new BlockList();
    }

    @Override
    public void guiUpdate() {
    }

    @Override
    public void onEnable(Context context) {
        this.SiIn();
    }

    @Override
    public void onDisable(Context context) {
        this.SiIn();
    }

    @Subscribe
    public void onPacketSend(PacketSendEvent var1) {
        if (ClientUtils.isInWorld() && var1.getPacket() instanceof C08PacketPlayerBlockPlacement) {
            C08PacketPlayerBlockPlacement var2 = (C08PacketPlayerBlockPlacement)var1.getPacket();
            if (var2.getPlacedBlockDirection() != 255) {
                ItemStack var3 = var2.getStack();
                if (var3 != null && var3.getItem() instanceof ItemBlock) {
                    this.Sxek.recordPlacement(var3, mc.thePlayer.inventory.currentItem, this.TzZp(var3));
                }
            }
        }
    }

    @Subscribe
    public void onTickStart(TickStartEvent var1) {
        if (this.Sxek.consumePendingSwap()) {
            ItemStack var2 = this.Sxek.getPlacedStack();
            int var3 = this.Sxek.getPlacedSlot();
            if (ClientUtils.isInWorld() && var2 != null && var3 != -1 && this.zdte5()) {
                if (this.TzZp(var2)) {
                    ItemStack var4 = mc.thePlayer.inventory.getStackInSlot(var3);
                    if (var4 == null || var4.stackSize != 1) {
                        if (var4 == null || var4.stackSize <= this.getSwapStackLimit()) {
                            this.swapToBestBlock();
                        }
                    }
                }
            }
        }
    }

    private boolean zdte5() {
        return this.lookingDown.bool() && mc.thePlayer.rotationPitch < 70.0F ? false : !this.movingBackwards.bool() || ClientUtils.xusXfhC(mc.gameSettings.keyBindBack);
    }

    private boolean isAllowedBlock(ItemStack var1) {
        return (!this.blockBlacklist.bool() || !this.matchesBlockList(var1, this.blacklistedBlocks)) && (!this.useBlockWhitelist.bool() || this.matchesBlockList(var1, this.whitelistedBlocks));
    }

    private boolean matchesBlockList(ItemStack var1, BlockList var2) {
        if (var1 != null && var1.getItem() instanceof ItemBlock) {
            Block var3 = ((ItemBlock)var1.getItem()).getBlock();
            Object var4 = Block.blockRegistry.getNameForObject(var3);
            if (var3 != null && var4 != null) {
                String var5 = var4.toString();
                int var6 = var1.getMetadata();
                String var7 = var6 != 0 ? var5 + ":" + var6 : var5;
                return var2.containsEntry(var7) || var2.containsEntry(var5);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean TzZp(ItemStack var1) {
        if (var1 != null && var1.getItem() instanceof ItemBlock) {
            Block var2 = ((ItemBlock)var1.getItem()).getBlock();
            return var2 != null && var2.isFullBlock() && this.isAllowedBlock(var1);
        } else {
            return false;
        }
    }

    private void swapToBestBlock() {
        long var1 = System.currentTimeMillis();
        int var3 = -1;

        for (int var4 = 8; var4 >= 0; var4--) {
            if (this.Sxek.canSwapToSlot(var4, var1)) {
                ItemStack var5 = mc.thePlayer.inventory.getStackInSlot(var4);
                if (this.TzZp(var5)) {
                    if (((ItemBlock)var5.getItem()).getBlock() == Blocks.wool) {
                        var3 = var4;
                        break;
                    }

                    if (var3 == -1) {
                        var3 = var4;
                    }
                }
            }
        }

        if (var3 != -1) {
            this.selectHotbarSlot(var3);
            this.Sxek.wfTy1(var3, var1);
        }
    }

    private int getSwapStackLimit() {
        return Math.max(0, (int)Math.round(this.swapAt.number()));
    }

    private void selectHotbarSlot(int var1) {
        if (var1 != -1 && var1 != mc.thePlayer.inventory.currentItem) {
            mc.thePlayer.inventory.currentItem = var1;
            mc.playerController.syncCurrentPlayItem();
        }
    }

    private void SiIn() {
        this.Sxek.clear();
    }
}
