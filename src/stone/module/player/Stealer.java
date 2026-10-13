package stone.module.player;

import stone.event.LoadWorldEvent;
import stone.event.PreUpdateEvent;
import stone.event.Subscribe;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.util.ClientUtils;
import stone.util.SkywarsGameState;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldSettings.GameType;

public class Stealer extends Module {
    private final Setting nameCheck;
    private final Setting delayTicks;
    private final Setting randomize;
    private final Setting autoClose;
    private final Setting skipBadItems;
    private final Setting onlyInSkywars;
    private int clickDelayTicks;
    private boolean stealSessionStarted;

    public Stealer() {
        super("Stealer", "Takes items out of chests", Category.PLAYER, Subcategory.BASIC);
        this.nameCheck = addBoolean("Name Check", "", true);
        this.delayTicks = addNumber("Delay Ticks", "", 2.0F, 0.0F, 20.0F, 0);
        this.randomize = addBoolean("Randomize", "", false);
        this.autoClose = addBoolean("Auto Close", "", false);
        this.skipBadItems = addBoolean("Skip bad items", "", true);
        this.onlyInSkywars = addBoolean("Only in Skywars", "", true);
    }

    @Override
    public void onEnable(Context context) {
        this.resetStealerState();
    }

    @Override
    public void onDisable(Context context) {
        this.resetStealerState();
    }

    @Subscribe
    public void onLoadWorld(LoadWorldEvent var1) {
        this.resetStealerState();
    }

    @Subscribe
    public void onPreUpdate(PreUpdateEvent var1) {
        if (!ClientUtils.isInWorld() || !(mc.currentScreen instanceof GuiChest) || !(mc.thePlayer.openContainer instanceof ContainerChest)) {
            this.resetStealerState();
        } else if (!this.isGameModeAllowed()) {
            this.resetStealerState();
        } else {
            ContainerChest var2 = (ContainerChest)mc.thePlayer.openContainer;
            IInventory var3 = var2.getLowerChestInventory();
            if (this.nameCheck.bool() && !isUnnamedChest(var3)) {
                this.resetStealerState();
            } else if (isSurvivalMode()) {
                if (!this.stealSessionStarted) {
                    this.stealSessionStarted = true;
                    this.clickDelayTicks = 0;
                }

                if (this.clickDelayTicks > 0) {
                    this.clickDelayTicks--;
                } else {
                    int var4 = this.findNextStealSlot(var2, var3);
                    if (var4 < 0) {
                        this.AGfvX();
                    } else {
                        mc.playerController.windowClick(var2.windowId, var4, 0, 1, mc.thePlayer);
                        this.clickDelayTicks = this.XKhj5();
                    }
                }
            }
        }
    }

    private void resetStealerState() {
        this.clickDelayTicks = 0;
        this.stealSessionStarted = false;
    }

    private boolean isGameModeAllowed() {
        return !this.onlyInSkywars.bool() || SkywarsGameState.isFightInProgress();
    }

    private int findNextStealSlot(ContainerChest var1, IInventory var2) {
        for (int var3 = 0; var3 < var2.getSizeInventory(); var3++) {
            if (var1.getSlot(var3).getHasStack()) {
                ItemStack var4 = var1.getSlot(var3).getStack();
                if (this.hasInventoryRoom(var4) && this.MLzz(var4)) {
                    return var3;
                }
            }
        }

        return -1;
    }

    private boolean MLzz(ItemStack var1) {
        if (this.isUselessItem(var1) && this.xOhkcd5(var1)) {
            return false;
        } else if (!this.skipBadItems.bool()) {
            return true;
        } else {
            return var1 != null && var1.getItem() instanceof ItemArmor ? this.isArmorUpgrade(var1) : InventoryManager.isWantedItem(var1);
        }
    }

    private boolean isUselessItem(ItemStack var1) {
        return var1 != null && (var1.getItem() == Items.fishing_rod || var1.getItem() == Items.water_bucket || var1.getItem() == Items.lava_bucket);
    }

    private boolean xOhkcd5(ItemStack var1) {
        for (int var2 = 0; var2 < mc.thePlayer.inventory.mainInventory.length; var2++) {
            ItemStack var3 = mc.thePlayer.inventory.mainInventory[var2];
            if (var3 != null && var3.getItem() == var1.getItem()) {
                return true;
            }
        }

        return false;
    }

    private boolean isArmorUpgrade(ItemStack var1) {
        int var2 = InventoryManager.getArmorType(var1);
        if (var2 < 0) {
            return false;
        } else {
            double var3 = InventoryManager.getItemArmorScore(var1);
            double var5 = InventoryManager.getWornArmorScore(var2, true);

            for (int var7 = 0; var7 < mc.thePlayer.inventory.mainInventory.length; var7++) {
                ItemStack var8 = mc.thePlayer.inventory.mainInventory[var7];
                if (InventoryManager.getArmorType(var8) == var2) {
                    var5 = Math.max(var5, InventoryManager.getItemArmorScore(var8));
                }
            }

            return var3 > var5 + 1.0E-6;
        }
    }

    private boolean hasInventoryRoom(ItemStack var1) {
        if (var1 == null) {
            return false;
        } else if (mc.thePlayer.inventory.getFirstEmptyStack() != -1) {
            return true;
        } else {
            for (int var2 = 0; var2 < mc.thePlayer.inventory.mainInventory.length; var2++) {
                ItemStack var3 = mc.thePlayer.inventory.mainInventory[var2];
                if (uj28(var3, var1) && var3.stackSize < var3.getMaxStackSize()) {
                    return true;
                }
            }

            return false;
        }
    }

    private static boolean uj28(ItemStack var0, ItemStack var1) {
        if (var0 == null || var1 == null || var0.getItem() != var1.getItem()) {
            return false;
        } else {
            return var0.getHasSubtypes() && var0.getMetadata() != var1.getMetadata() ? false : ItemStack.areItemStackTagsEqual(var0, var1);
        }
    }

    private void AGfvX() {
        if (this.autoClose.bool()) {
            mc.thePlayer.closeScreen();
        }
    }

    private int XKhj5() {
        int var1 = Math.max(0, (int)Math.round(this.delayTicks.number()));
        if (this.randomize.bool()) {
            var1 += 1 + (int)(Math.random() * 2.0);
        }

        return var1;
    }

    private static boolean isUnnamedChest(IInventory var0) {
        if (var0 != null && var0.hasCustomName()) {
            String var1 = var0.getDisplayName() != null ? var0.getDisplayName().getUnformattedText() : "";
            return var1 == null || var1.trim().isEmpty();
        } else {
            return true;
        }
    }

    private static boolean isSurvivalMode() {
        GameType var0 = mc.playerController.getCurrentGameType();
        return var0 == GameType.SURVIVAL || var0 == GameType.ADVENTURE;
    }
}
