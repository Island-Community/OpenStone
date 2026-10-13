package stone.module.minigames;

import stone.event.ChatReceivedEvent;
import stone.event.EventBus;
import stone.event.LoadWorldEvent;
import stone.event.PreUpdateEvent;
import stone.event.RotationEvent;
import stone.event.Subscribe;
import stone.event.TickStartEvent;
import stone.hook.Input;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.ui.Client;
import stone.util.ClientUtils;
import stone.util.block.BlockUtils;
import stone.util.rotation.RotationHandler;
import stone.util.rotation.RotationUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;

public class Deposit extends Module {
    private final Setting smartMode;
    private final Setting chests;
    private final Setting enderChests;
    private final Setting blocks;
    private final Setting fireballs;
    private final Setting swords;
    private final Setting emeralds;
    private final Setting diamonds;
    private final Setting iron;
    private final Setting gold;
    private final Setting other;
    private final Setting throughWalls;
    private final Setting rotations;
    private final Setting range;
    private final Setting holdDelay;
    private final Setting swapDelay;
    private BlockPos blockPos;
    private long enabledAtMillis;
    private long lastDigMillis;
    private boolean depositStarted;
    private boolean Qgv;
    private boolean keyHeldLastPoll;
    private boolean depositPlanInitialized;
    private boolean[] usedHotbarSlots;
    private List<Integer> VkpT;
    private int plannedSlotIndex;
    private int locrawDelayTicks;
    private boolean locrawRequested;
    private long sRd;
    private boolean inBedwars;
    private boolean kpl;
    private String VMx = "";
    private final Object gameModeTracker = new Object() {
        @Subscribe
        public void onLoadWorld(LoadWorldEvent var1) {
            Deposit.this.VMx = "";
            Deposit.this.inBedwars = false;
            Deposit.this.kpl = false;
            Deposit.this.locrawRequested = false;
            if (Deposit.this.smartMode.bool() && var1.world != null) {
                Deposit.this.locrawDelayTicks = 25;
            }
        }

        @Subscribe
        public void onTickStart(TickStartEvent var1) {
            if (Deposit.this.smartMode.bool() && !Deposit.this.kpl && Deposit.this.VMx.length() == 0 && ClientUtils.isInWorld()) {
                Deposit.this.refreshGameMode();
            }

            if (Deposit.this.smartMode.bool() && Deposit.this.locrawDelayTicks > 0) {
                Deposit.this.locrawDelayTicks--;
                if (Deposit.this.locrawDelayTicks <= 0 && ClientUtils.isInWorld()) {
                    Deposit.this.refreshGameMode();
                    Deposit.this.requestLocraw(true);
                }
            }
        }

        @Subscribe
        public void onChatReceived(ChatReceivedEvent var1) {
            if (Deposit.this.smartMode.bool() && var1 != null && var1.messageType != 2 && var1.component != null) {
                String var2 = ClientUtils.AOAtn(var1.component.getUnformattedText());
                if (Deposit.this.isLocrawResponse(var2)) {
                    var1.setCancelled(true);
                    Deposit.this.locrawRequested = false;
                    Deposit.this.parseLocrawResponse(var2);
                }
            }
        }
    };

    public Deposit() {
        super("Deposit", "Puts resources and items into nearby chests", Category.MINIGAMES, Subcategory.ADVANCED);
        EventBus.register(this.gameModeTracker);
        this.smartMode = addBoolean("Smart mode", "", true);
        this.chests = addBoolean("Chests", "", true);
        this.enderChests = addBoolean("Ender Chests", "", true);
        this.chests.visibleWhen(() -> !this.smartMode.bool());
        this.enderChests.visibleWhen(() -> !this.smartMode.bool());
        this.blocks = addBoolean("Blocks", "", true);
        this.fireballs = addBoolean("Fireballs", "", true);
        this.swords = addBoolean("Swords", "", true);
        this.emeralds = addBoolean("Emeralds", "", true);
        this.diamonds = addBoolean("Diamonds", "", true);
        this.iron = addBoolean("Iron", "", true);
        this.gold = addBoolean("Gold", "", true);
        this.other = addBoolean("Other", "", true);
        this.range = addNumber("Range", "", 4.5F, 2.0F, 6.0F, 1);
        this.throughWalls = addBoolean("Through walls", "", false);
        this.rotations = addBoolean("Rotations", "", true);
        this.holdDelay = addNumber("Hold delay", "", 150.0F, 50.0F, 500.0F, 0);
        this.swapDelay = addNumber("Swap delay", "", 100.0F, 0.0F, 500.0F, 0);
    }

    @Override
    public boolean holdsKey() {
        return true;
    }

    @Override
    public void keyHeld(boolean held, Context context) {
        if (held && !this.keyHeldLastPoll && !this.enabled()) {
            setEnabled(true, context);
        }
        this.keyHeldLastPoll = held;
    }

    @Override
    public void onEnable(Context context) {
        this.enabledAtMillis = System.currentTimeMillis();
        this.lastDigMillis = 0L;
        this.depositStarted = false;
        this.Qgv = false;
        this.depositPlanInitialized = false;
        this.usedHotbarSlots = new boolean[InventoryPlayer.getHotbarSize()];
        this.VkpT = new ArrayList<>();
        this.plannedSlotIndex = 0;
        this.blockPos = null;
        this.refreshGameMode();
        this.fiA5();
    }

    @Override
    public void onDisable(Context context) {
        this.blockPos = null;
        this.Qgv = false;
        this.depositPlanInitialized = false;
        this.VkpT = Collections.emptyList();
        this.plannedSlotIndex = 0;
    }

    @Subscribe
    public void onPreUpdate(PreUpdateEvent var1) {
        if (this.enabled()) {
            if (ClientUtils.isInWorld() && mc.currentScreen == null && !mc.isGamePaused()) {
                boolean var2 = this.key() != 0 && Input.bindHeld(this.key());
                if (!var2 && this.depositStarted) {
                    setEnabled(false, Client.context());
                } else {
                    if (!var2) {
                        this.Qgv = true;
                    }

                    if (!this.depositStarted) {
                        this.fiA5();
                    } else if (!this.hasRemainingDepositItem()) {
                        setEnabled(false, Client.context());
                    } else {
                        this.blockPos = this.findTargetContainer();
                        if (this.blockPos != null) {
                            long var3 = System.currentTimeMillis();
                            if (!this.Qgv && var2) {
                                if (!this.depositPlanInitialized) {
                                    if (var3 - this.enabledAtMillis < this.holdDelay.number()) {
                                        return;
                                    }

                                    this.depositPlanInitialized = true;
                                    this.VkpT = this.planDepositSlots();
                                    this.plannedSlotIndex = 0;
                                    if (this.VkpT.isEmpty()) {
                                        setEnabled(false, Client.context());
                                        return;
                                    }
                                }

                                if (!(var3 - this.lastDigMillis < this.swapDelay.number())) {
                                    int var5 = this.nextPlannedSlot();
                                    if (var5 == -1) {
                                        setEnabled(false, Client.context());
                                    } else {
                                        ClientUtils.setHeldSlot(var5, true);
                                        this.usedHotbarSlots[var5] = true;
                                        this.sendContainerDigPacket();
                                        this.lastDigMillis = var3;
                                        if (!this.hasRemainingDepositItem()) {
                                            setEnabled(false, Client.context());
                                        }
                                    }
                                }
                            }
                        } else {
                            if (this.Qgv || this.key() == 0 || !Input.bindHeld(this.key())) {
                                setEnabled(false, Client.context());
                            }
                        }
                    }
                }
            } else {
                setEnabled(false, Client.context());
            }
        }
    }

    @Subscribe
    public void onRotation(RotationEvent var1) {
        if (this.enabled() && this.rotations.bool() && ClientUtils.isInWorld() && this.blockPos != null) {
            float[] var2 = RotationUtils.anglesToBlockCenter(this.blockPos);
            var1.setRotation(var2[0], var2[1], 45);
        }
    }

    private void sendContainerDigPacket() {
        if (this.blockPos != null && mc.thePlayer != null && mc.thePlayer.sendQueue != null) {
            EnumFacing var1 = this.findDigFacing(this.blockPos);
            if (this.rotations.bool()) {
                float[] var2 = RotationUtils.anglesToBlockCenter(this.blockPos);
                this.applySilentRotation(var2[0], var2[1]);
            }

            mc.thePlayer.sendQueue.addToSendQueue(new C0APacketAnimation());
            mc.thePlayer.sendQueue.addToSendQueue(new C07PacketPlayerDigging(Action.START_DESTROY_BLOCK, this.blockPos, var1));
        }
    }

    private void fiA5() {
        if (ClientUtils.isInWorld() && mc.currentScreen == null && !mc.isGamePaused()) {
            if (this.smartMode.bool() && this.VMx.length() == 0) {
                this.refreshGameMode();
                this.requestLocraw(true);
            }

            if (!this.isDepositTarget(mc.thePlayer.getHeldItem())) {
                this.depositStarted = true;
                this.usedHotbarSlots[mc.thePlayer.inventory.currentItem] = true;
                this.lastDigMillis = System.currentTimeMillis();
                if (this.Qgv || this.key() == 0 || !Input.bindHeld(this.key())) {
                    setEnabled(false, Client.context());
                }
            } else {
                this.blockPos = this.findTargetContainer();
                if (this.blockPos == null) {
                    if (this.Qgv || this.key() == 0 || !Input.bindHeld(this.key())) {
                        setEnabled(false, Client.context());
                    }
                } else {
                    this.sendContainerDigPacket();
                    this.depositStarted = true;
                    this.usedHotbarSlots[mc.thePlayer.inventory.currentItem] = true;
                    this.lastDigMillis = System.currentTimeMillis();
                    if (this.Qgv || this.key() == 0 || !Input.bindHeld(this.key())) {
                        setEnabled(false, Client.context());
                    }
                }
            }
        }
    }

    private BlockPos findTargetContainer() {
        if (!this.smartMode.bool() && !this.chests.bool() && !this.enderChests.bool()) {
            return null;
        } else {
            double var1 = this.range.number();
            int var3 = (int)Math.ceil(var1);
            Vec3 var4 = mc.thePlayer.getPositionEyes(1.0F);
            BlockPos var5 = mc.thePlayer.getPosition();
            BlockPos var6 = null;
            BlockPos var7 = null;
            double var8 = var1 * var1;
            double var10 = var1 * var1;

            for (int var12 = -var3; var12 <= var3; var12++) {
                for (int var13 = -var3; var13 <= var3; var13++) {
                    for (int var14 = -var3; var14 <= var3; var14++) {
                        BlockPos var15 = var5.add(var12, var13, var14);
                        Block var16 = BlockUtils.iepjdt(var15);
                        if (this.isValidContainerTarget(var16, var15)) {
                            Vec3 var17 = new Vec3(var15.getX() + 0.5, var15.getY() + 0.5, var15.getZ() + 0.5);
                            double var18 = var4.squareDistanceTo(var17);
                            if (this.throughWalls.bool() || BlockUtils.hasClearLineOfSight(var15)) {
                                if (this.isEnderChest(var16)) {
                                    if (var18 <= var8) {
                                        var6 = var15;
                                        var8 = var18;
                                    }
                                } else if (this.isChest(var16) && var18 <= var10) {
                                    var7 = var15;
                                    var10 = var18;
                                }
                            }
                        }
                    }
                }
            }

            if (!this.smartMode.bool()) {
                if (var6 == null) {
                    return var7;
                } else if (var7 == null) {
                    return var6;
                } else {
                    return var8 <= var10 ? var6 : var7;
                }
            } else if ("BEDWARS_EIGHT_ONE".equals(this.VMx)) {
                return var6 != null ? var6 : var7;
            } else if (this.inBedwars) {
                return var7 != null ? var7 : var6;
            } else {
                return var6;
            }
        }
    }

    private boolean isValidContainerTarget(Block var1, BlockPos var2) {
        if (var1 == null || var1 == Blocks.air) {
            return false;
        } else if (!this.smartMode.bool()) {
            return this.chests.bool() && this.isChest(var1) || this.enderChests.bool() && this.isEnderChest(var1);
        } else {
            return this.isEnderChest(var1) ? true : this.inBedwars && this.isChest(var1) && !"BEDWARS_EIGHT_ONE".equals(this.VMx) && this.HgCdv(var2);
        }
    }

    private boolean isChest(Block var1) {
        return var1 instanceof BlockChest || var1 == Blocks.chest || var1 == Blocks.trapped_chest;
    }

    private boolean isEnderChest(Block var1) {
        return var1 instanceof BlockEnderChest || var1 == Blocks.ender_chest;
    }

    private boolean HgCdv(BlockPos var1) {
        byte var2 = 25;

        for (int var3 = -var2; var3 <= var2; var3++) {
            for (int var4 = -var2; var4 <= var2; var4++) {
                for (int var5 = -var2; var5 <= var2; var5++) {
                    BlockPos var6 = var1.add(var3, var4, var5);
                    if (!(var1.distanceSq(var6) > 625.0) && BlockUtils.iepjdt(var6) instanceof BlockBed) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private EnumFacing findDigFacing(BlockPos var1) {
        Vec3 var2 = mc.thePlayer.getPositionEyes(1.0F);
        EnumFacing[] var3 = BlockUtils.getClosestBlockFacings(var2, var1);
        return var3.length > 0 ? var3[0] : EnumFacing.UP;
    }

    private List<Integer> planDepositSlots() {
        ArrayList var1 = new ArrayList();

        for (int var2 = 0; var2 < InventoryPlayer.getHotbarSize(); var2++) {
            if (!this.usedHotbarSlots[var2]) {
                ItemStack var3 = mc.thePlayer.inventory.getStackInSlot(var2);
                if (var3 != null && !this.isExcludedItem(var3) && this.isDepositTarget(var3)) {
                    var1.add(var2);
                }
            }
        }

        Collections.sort(var1, new Comparator<Integer>() {
            public int compare(Integer var1, Integer var2x) {
                int var3 = Deposit.JOFP(Deposit.this, Deposit.getMinecraftInstance().thePlayer.inventory.getStackInSlot(var1));
                int var4 = Deposit.JOFP(Deposit.this, Deposit.getMc().thePlayer.inventory.getStackInSlot(var2x));
                return var3 != var4 ? Integer.compare(var3, var4) : Integer.compare(var1, var2x);
            }
        });
        return var1;
    }

    private int nextPlannedSlot() {
        while (this.plannedSlotIndex < this.VkpT.size()) {
            int var1 = this.VkpT.get(this.plannedSlotIndex);
            this.plannedSlotIndex++;
            if (var1 >= 0 && var1 < InventoryPlayer.getHotbarSize() && !this.usedHotbarSlots[var1]) {
                ItemStack var2 = mc.thePlayer.inventory.getStackInSlot(var1);
                if (this.isDepositTarget(var2)) {
                    return var1;
                }
            }
        }

        return -1;
    }

    private boolean hasRemainingDepositItem() {
        for (int var1 = 0; var1 < InventoryPlayer.getHotbarSize(); var1++) {
            if (!this.usedHotbarSlots[var1] && this.isDepositTarget(mc.thePlayer.inventory.getStackInSlot(var1))) {
                return true;
            }
        }

        return false;
    }

    private void applySilentRotation(float var1, float var2) {
        RotationHandler.getInstance().ywbo(var1, var2);
    }

    private boolean isExcludedItem(ItemStack var1) {
        Item var2 = var1.getItem();
        return var2 == Items.wooden_sword || var2 == Items.compass || var2 instanceof ItemShears || var2 instanceof ItemPickaxe || var2 instanceof ItemAxe;
    }

    private boolean isDepositTarget(ItemStack var1) {
        if (var1 != null && !this.isExcludedItem(var1)) {
            Item var2 = var1.getItem();
            if (var2 == Items.emerald) {
                return this.emeralds.bool();
            } else if (var2 == Items.diamond) {
                return this.diamonds.bool();
            } else if (var2 == Items.gold_ingot) {
                return this.gold.bool();
            } else if (var2 == Items.iron_ingot) {
                return this.iron.bool();
            } else if (var2 == Items.fire_charge) {
                return this.fireballs.bool();
            } else if (!(var2 instanceof ItemSword)) {
                return var2 instanceof ItemBlock ? this.blocks.bool() : this.other.bool();
            } else {
                return this.swords.bool() && this.countHotbarItem(var2) >= 2;
            }
        } else {
            return false;
        }
    }

    private int countHotbarItem(Item var1) {
        int var2 = 0;

        for (int var3 = 0; var3 < InventoryPlayer.getHotbarSize(); var3++) {
            ItemStack var4 = mc.thePlayer.inventory.getStackInSlot(var3);
            if (var4 != null && var4.getItem() == var1) {
                var2++;
            }
        }

        return var2;
    }

    private int getDepositPriority(ItemStack var1) {
        if (var1 == null) {
            return 6;
        } else {
            Item var2 = var1.getItem();
            if (var2 == Items.emerald) {
                return 0;
            } else if (var2 == Items.diamond) {
                return 1;
            } else if (var2 == Items.gold_ingot) {
                return 3;
            } else if (var2 == Items.iron_ingot) {
                return 4;
            } else {
                return var2 instanceof ItemBlock ? 5 : 2;
            }
        }
    }

    private void refreshGameMode() {
        if (this.smartMode.bool() && !this.kpl && ClientUtils.isInWorld()) {
            int var1 = ClientUtils.getBedWarsBoardType();
            if (var1 == -1) {
                this.inBedwars = false;
                this.VMx = "";
            } else {
                this.inBedwars = var1 == 2;
                if (this.isSoloBedWars()) {
                    this.VMx = "BEDWARS_EIGHT_ONE";
                } else if (this.inBedwars) {
                    this.VMx = "BEDWARS_LOCAL";
                }
            }
        }
    }

    private boolean isSoloBedWars() {
        for (String var2 : ClientUtils.PxSw4()) {
            String var3 = ClientUtils.zaUnpz(var2).toUpperCase();
            if (var3.contains("SOLO")) {
                return true;
            }
        }

        return false;
    }

    private void requestLocraw(boolean var1) {
        long var2 = System.currentTimeMillis();
        if (this.locrawRequested && var2 - this.sRd > 5000L) {
            this.locrawRequested = false;
        }

        if (ClientUtils.isInWorld() && !this.locrawRequested) {
            if (!var1 || var2 - this.sRd >= 1500L) {
                this.locrawRequested = true;
                this.sRd = var2;
                mc.thePlayer.sendChatMessage("/locraw");
            }
        }
    }

    private boolean isLocrawResponse(String var1) {
        if (var1 == null) {
            return false;
        } else {
            String var2 = var1.trim();
            return var2.startsWith("{") && var2.endsWith("}") && var2.contains("\"server\"") && var2.contains("\"gametype\"") && var2.contains(":");
        }
    }

    private void parseLocrawResponse(String var1) {
        try {
            JsonObject var2 = new JsonParser().parse(var1).getAsJsonObject();
            this.kpl = true;
            if (var2.has("gametype") && "BEDWARS".equalsIgnoreCase(var2.get("gametype").getAsString())) {
                this.inBedwars = true;
                this.VMx = var2.has("mode") ? var2.get("mode").getAsString() : "";
            } else {
                this.inBedwars = false;
                this.VMx = "";
            }
        } catch (Exception var3) {
            this.kpl = false;
            this.inBedwars = false;
            this.VMx = "";
        }
    }

    @Override
    public String suffix() {
        if (this.smartMode.bool()) {
            return "Smart";
        }
        return this.chests.bool() && this.enderChests.bool() ? "Chests, Ender Chests" : this.chests.bool() ? "Chests" : this.enderChests.bool() ? "Ender Chests" : "";
    }

    public static Minecraft getMinecraftInstance() {
        return mc;
    }

    public static int JOFP(Deposit var0, ItemStack var1) {
        return var0.getDepositPriority(var1);
    }

    public static Minecraft getMc() {
        return mc;
    }
}
