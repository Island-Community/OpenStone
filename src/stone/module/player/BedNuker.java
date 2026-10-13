package stone.module.player;

import stone.event.ChangeCurrentItemEvent;
import stone.event.ClickMouseEvent;
import stone.event.MouseEvent;
import stone.event.PrePlayerInteractEvent;
import stone.event.RenderWorldLastEvent;
import stone.event.RotationEvent;
import stone.event.SetCurrentItemEvent;
import stone.event.Subscribe;
import stone.event.TickStartEvent;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.module.minigames.BedwarsSpawnTracker;
import stone.module.player.bednuker.BedFinder;
import stone.module.player.bednuker.BedFinder$2;
import stone.module.player.bednuker.BreakProgressColor;
import stone.module.player.bednuker.BreakTarget;
import stone.module.player.bednuker.TargetRayTracer;
import stone.module.player.shared.BreakDamageHelper;
import stone.module.shared.ProgressBarSource;
import stone.util.ClientUtils;
import stone.util.block.BlockUtils;
import stone.util.raytrace.PointedObjectOverrider;
import stone.util.render.RenderUtils;
import stone.util.rotation.RotationHandler;
import stone.util.rotation.RotationUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class BedNuker extends Module implements ProgressBarSource, PointedObjectOverrider {
    private final Setting fov;
    private final Setting range;
    private final Setting breakSpeed;
    private final Setting ignoreMiningFatigue;
    private final Setting groundSpoof;
    private final Setting blockZapper;
    private final Setting zeroTickBlockZapper;
    private final Setting prioritizeKillAura;
    private final Setting switchBackWhenDone;
    private final Setting renderOutline;
    private final BedFinder bedFinder = new BedFinder();
    private int Cnu;
    private BlockPos blockPos;
    private Vec3 vec3;
    private EnumFacing enumFacing;
    private boolean MIJ;
    private int VkxQ;
    private int sxnS5 = -1;
    private int slotSwitchDepth;
    private boolean Wjmxhu;
    private int FMJYpd = -1;
    private int kJp = -1;
    private int qb5 = -1;
    private int MxT = -1;
    private C08PacketPlayerBlockPlacement c08PacketPlayerBlockPlacement;
    private final BreakProgressColor JsY = new BreakProgressColor();
    private final BedFinder$2 bedBlockFilter = new BedFinder$2() {
        @Override
        public boolean isBreakable(Block var1) {
            return BedNuker.canBreakBlock(BedNuker.this, var1);
        }

        @Override
        public float getBreakSpeed(Block var1) {
            return BedNuker.getMaxBreakSpeed(BedNuker.this, var1);
        }
    };

    public BedNuker() {
        super("BedNuker", "Breaks beds within reach, through walls", Category.PLAYER, Subcategory.EXTREME);
        this.breakSpeed = addNumber("Break speed", "", 1.0F, 1.0F, 2.0F, 2);
        this.ignoreMiningFatigue = addBoolean("Ignore mining fatigue", "", false);
        this.groundSpoof = addBoolean("Ground spoof", "", false);
        this.range = addNumber("Range", "", 4.5F, 2.0F, 6.0F, 1);
        this.fov = addNumber("FOV", "", 180.0F, 30.0F, 360.0F, 0);
        this.blockZapper = addBoolean("Block zapper", "", false);
        this.zeroTickBlockZapper = addBoolean("Zero tick block zapper", "", false);
        this.prioritizeKillAura = addBoolean("Prioritize KillAura", "", false);
        this.switchBackWhenDone = addBoolean("Switch back when done", "", true);
        this.switchBackWhenDone.setBool(true);
        this.switchBackWhenDone.setVisible(false);
        this.renderOutline = addBoolean("Render outline", "", true);
    }

    @Override
    public void guiUpdate() {
        this.zeroTickBlockZapper.setVisible(this.blockZapper.bool());
        if (!this.switchBackWhenDone.bool()) {
            this.switchBackWhenDone.setBool(true);
        }
    }

    @Override
    public void onDisable(Context context) {
        this.Jucmm();
        this.bedFinder.clearCandidates();
        this.Cnu = 0;
        this.VkxQ = 0;
        this.sxnS5 = -1;
    }

    @Subscribe(priority = Subscribe.Priority.LOWEST)
    public void onTickStart(TickStartEvent var1) {
        this.VkxQ++;
    }

    @Subscribe(priority = Subscribe.Priority.LOWEST)
    public void onPrePlayerInteract(PrePlayerInteractEvent var1) {
        this.updateMiningAction();
    }

    @Subscribe(priority = Subscribe.Priority.HIGH)
    public void onMouse(MouseEvent var1) {
        if (this.shouldSuppressInput()) {
            if (var1.button == 0 || var1.button == 1) {
                var1.setCancelled(true);
            }
        }
    }

    @Subscribe(priority = Subscribe.Priority.HIGH)
    public void onClickMouse(ClickMouseEvent var1) {
        if (this.shouldSuppressInput()) {
            var1.setCancelled(true);
        }
    }

    @Subscribe(priority = Subscribe.Priority.HIGH)
    public void onChangeCurrentItem(ChangeCurrentItemEvent var1) {
        if (this.shouldSuppressInput()) {
            int var2 = this.FMJYpd;
            this.resetNukeState(false);
            if (var2 != -1 && ClientUtils.isInWorld()) {
                mc.thePlayer.inventory.currentItem = var2;
            }
        }
    }

    @Subscribe(priority = Subscribe.Priority.HIGH)
    public void onSetCurrentItem(SetCurrentItemEvent var1) {
        if (this.shouldSuppressInput() && this.slotSwitchDepth <= 0) {
            this.resetNukeState(false);
        }
    }

    private boolean shouldSuppressInput() {
        return this.isAimedThisTick();
    }

    public void updateMiningAction() {
        if (this.canMine() && (!this.MIJ || this.isTargetBreakable()) && !this.iEjyDt()) {
            if (!this.isAimedThisTick()) {
                this.restoreKeyStates();
            } else {
                int var1 = mc.gameSettings.keyBindAttack.getKeyCode();
                int var2 = mc.gameSettings.keyBindUseItem.getKeyCode();
                KeyBinding.setKeyBindState(var1, false);
                KeyBinding.setKeyBindState(var2, false);
                this.Wjmxhu = true;
                if (this.gxScaEc() && this.c08PacketPlayerBlockPlacement == null) {
                    if (this.blockZapper.bool() && this.isHoldingBlockZapper()) {
                        KeyBinding.setKeyBindState(var2, true);
                    } else {
                        KeyBinding.setKeyBindState(var1, true);
                    }
                }
            }
        } else {
            if (this.MIJ) {
                this.Jucmm();
            }
        }
    }

    public boolean isNukingTarget() {
        return this.MIJ && this.enabled() && ClientUtils.isInWorld() && mc.currentScreen == null && this.canMine() && this.isTargetBreakable() && !this.iEjyDt();
    }

    public boolean gxScaEc() {
        return this.isAimedThisTick() && this.rayTraceTarget(this.blockPos) != null;
    }

    private boolean isAimedThisTick() {
        return this.sxnS5 == this.VkxQ && this.isNukingTarget() && this.vec3 != null && this.enumFacing != null;
    }

    private MovingObjectPosition rayTraceTarget(BlockPos var1) {
        if (var1 != null && ClientUtils.isInWorld()) {
            RotationHandler var2 = RotationHandler.getInstance();
            Float var3 = var2.getTargetYaw();
            Float var4 = var2.cvZx();
            return TargetRayTracer.rayTraceWithReportedCheck(
                BlockUtils.getSelectedBounds(var1),
                mc.thePlayer.getPositionEyes(1.0F),
                this.range.number(),
                var3 == null ? mc.thePlayer.rotationYaw : var3,
                var4 == null ? mc.thePlayer.rotationPitch : var4,
                mc.thePlayer.lastReportedYaw,
                mc.thePlayer.lastReportedPitch
            );
        } else {
            return null;
        }
    }

    public boolean isNuking() {
        return this.isNukingTarget();
    }

    public float xskLk() {
        return this.IKijtw();
    }

    public float TnokwE() {
        return this.isGroundSpoofing() ? 5.0F : 1.0F;
    }

    private float IKijtw() {
        float var1 = (float)this.breakSpeed.number();
        if (this.ignoreMiningFatigue.bool() && mc.thePlayer != null && mc.thePlayer.isPotionActive(Potion.digSlowdown)) {
            ItemStack var2 = mc.thePlayer.getHeldItem();
            Item var3 = var2 != null ? var2.getItem() : null;
            if (var3 != Items.wooden_axe && var3 != Items.wooden_pickaxe) {
                var1 = 2.5F;
            } else {
                var1 = 1.8F;
            }
        }

        return var1 > 1.0F ? var1 : 1.0F;
    }

    private boolean isGroundSpoofing() {
        return this.groundSpoof.bool() && mc.thePlayer != null && !mc.thePlayer.onGround;
    }

    public void QLkR() {
        if (this.gxScaEc() && mc.inGameHasFocus && mc.playerController != null && this.blockPos != null) {
            BlockPos var2 = mc.playerController.currentBlock;
            if (mc.playerController.isHittingBlock && var2 != null && this.blockPos.equals(var2)) {
                float var3 = this.getBreakDamagePerTick();
                if (!(var3 <= 0.0F)) {
                    float var4 = mc.playerController.curBlockDamageMP;
                    float var5 = BreakDamageHelper.oxOn(var4, var3);
                    if (var5 > var4) {
                        mc.playerController.curBlockDamageMP = var5;
                    }
                }
            }
        }
    }

    public float getBreakDamagePerTick() {
        return BreakDamageHelper.GeX06(this.xskLk());
    }

    public float getBreakProgress() {
        if (this.canMine() && this.MIJ && mc.playerController != null) {
            BlockPos var2 = mc.playerController.currentBlock;
            return this.blockPos != null && var2 != null && this.blockPos.equals(var2) ? mc.playerController.curBlockDamageMP : 0.0F;
        } else {
            return 0.0F;
        }
    }

    @Override
    public boolean isProgressActive() {
        return this.isNukingTarget();
    }

    @Override
    public float getProgressFraction() {
        return Math.max(0.0F, Math.min(1.0F, this.getBreakProgress()));
    }

    @Override
    public String getProgressLabel() {
        return "Bed Nuker";
    }

    @Override
    public boolean shouldOverridePointedObject() {
        return this.gxScaEc();
    }

    @Override
    public void applyPointedObjectOverride(float var1) {
        if (this.shouldOverridePointedObject()) {
            if (mc.getRenderViewEntity() != null) {
                MovingObjectPosition var2 = this.rayTraceTarget(this.blockPos);
                if (var2 != null) {
                    MovingObjectPosition var3 = new MovingObjectPosition(var2.hitVec, var2.sideHit, this.blockPos);
                    mc.objectMouseOver = var3;
                    mc.pointedEntity = null;
                    EntityRenderer var4 = mc.entityRenderer;
                    var4.pointedEntity = null;
                }
            }
        }
    }

    @Subscribe(priority = Subscribe.Priority.LOWEST)
    public void onRotation(RotationEvent var1) {
        if (!this.enabled() || !ClientUtils.isInWorld() || mc.currentScreen != null || !this.canMine()) {
            this.Jucmm();
        } else if (this.iEjyDt()) {
            this.Jucmm();
        } else if (var1.suppressRotations) {
            this.Jucmm();
        } else {
            double var2 = this.range.number();
            double var4 = var2 * var2;
            if (--this.Cnu <= 0) {
                this.Cnu = Math.max(1, 1);
                this.BcVn(var2 + 1.0);
            }

            if (this.bedFinder.isCandidateListEmpty()) {
                this.Jucmm();
            } else {
                BreakTarget var6 = this.findCurrentTarget(var4);
                if (var6 == null) {
                    var6 = this.findBestTarget(var4);
                }

                if (var6 == null) {
                    this.Jucmm();
                } else {
                    this.clearPendingPlacement();
                    BreakTarget var7 = this.getZeroTickSwapTarget(var6);
                    boolean var8 = var7 != null;
                    if (var8) {
                        var6 = var7;
                    }

                    this.blockPos = var6.getBlockPos();
                    this.vec3 = var6.getHitVec();
                    this.enumFacing = var6.getSide();
                    this.MIJ = true;
                    if (!var8) {
                        this.selectToolForBlock(BlockUtils.iepjdt(this.blockPos));
                    }

                    float var9 = var1.yaw != null ? var1.yaw : RotationUtils.lastSentRotation[0];
                    float[] var10 = TargetRayTracer.LynS(BlockUtils.getSelectedBounds(this.blockPos), mc.thePlayer.getPositionEyes(1.0F), var9);
                    this.sxnS5 = var1.setRotation(var10[0], var10[1], 45) ? this.VkxQ : -1;
                }
            }
        }
    }

    @Subscribe
    public void onRenderWorldLast(RenderWorldLastEvent var1) {
        if (this.enabled() && this.renderOutline.bool() && this.MIJ && this.isTargetBreakable() && ClientUtils.isInWorld() && this.canMine()) {
            IBlockState var2 = mc.theWorld.getBlockState(this.blockPos);
            Block var3 = var2.getBlock();
            if (var3 != null && var3 != Blocks.air) {
                this.renderTargetOutline(this.blockPos, this.getOutlineColor());
            }
        }
    }

    private int getOutlineColor() {
        return this.JsY.getOutlineColor(this.blockPos, this.isMiningTargetBlock(), this.getBreakProgress());
    }

    private boolean isMiningTargetBlock() {
        if (this.blockPos != null && mc.playerController != null) {
            BlockPos var1 = mc.playerController.currentBlock;
            return var1 != null && this.blockPos.equals(var1);
        } else {
            return false;
        }
    }

    private void renderTargetOutline(BlockPos var1, int var2) {
        AxisAlignedBB var3 = BlockUtils.getSelectedBounds(var1);
        if (var3 != null) {
            double var4 = mc.getRenderManager().viewerPosX;
            double var6 = mc.getRenderManager().viewerPosY;
            double var8 = mc.getRenderManager().viewerPosZ;
            AxisAlignedBB var10 = var3.offset(-var4, -var6, -var8).expand(0.002, 0.002, 0.002);
            float var11 = (var2 >> 24 & 0xFF) / 255.0F;
            float var12 = (var2 >> 16 & 0xFF) / 255.0F;
            float var13 = (var2 >> 8 & 0xFF) / 255.0F;
            float var14 = (var2 & 0xFF) / 255.0F;
            GlStateManager.pushMatrix();
            GL11.glPushAttrib(1048575);

            try {
                GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                GlStateManager.disableTexture2D();
                GlStateManager.disableDepth();
                GlStateManager.disableCull();
                GlStateManager.depthMask(false);
                RenderUtils.drawFilledAabb(var10, var12, var13, var14, var11);
            } finally {
                GL11.glPopAttrib();
                GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
                GlStateManager.enableTexture2D();
                GlStateManager.enableDepth();
                GlStateManager.enableCull();
                GlStateManager.depthMask(true);
                GlStateManager.disableBlend();
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                GlStateManager.popMatrix();
            }
        }
    }

    private void Jucmm() {
        this.resetNukeState(true);
    }

    private void resetNukeState(boolean var1) {
        this.MIJ = false;
        this.sxnS5 = -1;
        this.clearPendingPlacement();
        this.kJp = -1;
        if (var1 && this.FMJYpd != -1 && ClientUtils.isInWorld()) {
            this.PSfg(this.FMJYpd);
        }

        this.restoreKeyStates();
        this.slotSwitchDepth = 0;
        this.blockPos = null;
        this.vec3 = null;
        this.enumFacing = null;
        this.JsY.reset();
        this.FMJYpd = -1;
    }

    private void restoreKeyStates() {
        if (this.Wjmxhu) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), Mouse.isButtonDown(0));
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), Mouse.isButtonDown(1));
            this.Wjmxhu = false;
        }
    }

    public void processPendingPlacement() {
        if (!ClientUtils.isInWorld() || mc.playerController == null) {
            this.kJp = -1;
            this.clearPendingPlacement();
        } else if (this.c08PacketPlayerBlockPlacement == null) {
            int var4 = this.kJp;
            this.kJp = -1;
            this.switchToSlot(var4);
        } else if (this.gxScaEc() && this.rayTraceTarget(this.c08PacketPlayerBlockPlacement.getPosition()) != null) {
            int var1 = this.qb5;
            int var2 = this.MxT;
            C08PacketPlayerBlockPlacement var3 = this.c08PacketPlayerBlockPlacement;
            this.kJp = -1;
            this.clearPendingPlacement();
            this.switchToSlot(var1);
            mc.thePlayer.sendQueue.addToSendQueue(var3);
            if (var2 >= 0) {
                this.switchToSlot(var2);
            }
        }
    }

    private void BcVn(double var1) {
        BlockPos var3 = BedwarsSpawnTracker.getSpawnBlockPos();
        if (var3 == null || ClientUtils.getBedWarsBoardType() != 2 || mc.thePlayer.getDistanceSq(var3) > 800.0) {
            var3 = null;
        }

        this.bedFinder.NNLvsH(mc.theWorld, mc.thePlayer, var1, (float)this.fov.number(), var3);
    }

    private BreakTarget findBestTarget(double var1) {
        return this.bedFinder.findBestReachableTarget(mc.theWorld, mc.thePlayer, var1, mc.playerController.curBlockDamageMP, mc.playerController.currentBlock, this.bedBlockFilter);
    }

    private BreakTarget findCurrentTarget(double var1) {
        return this.MIJ && this.isTargetBreakable() ? this.bedFinder.findTargetAtPos(mc.theWorld, mc.thePlayer, this.blockPos, var1, this.bedBlockFilter) : null;
    }

    private void selectToolForBlock(Block var1) {
        int var2 = -1;
        if (this.blockZapper.bool()) {
            var2 = this.findBlockZapperSlot();
        }

        if (var2 < 0) {
            var2 = this.findBestToolSlot(var1);
        }

        if (var2 >= 0) {
            if (this.FMJYpd == -1 && var2 != mc.thePlayer.inventory.currentItem) {
                this.FMJYpd = mc.thePlayer.inventory.currentItem;
            }

            if (var2 != mc.thePlayer.inventory.currentItem) {
                this.PSfg(var2);
            }
        }
    }

    private int findBlockZapperSlot() {
        if (mc.thePlayer == null) {
            return -1;
        } else {
            for (int var1 = 0; var1 < InventoryPlayer.getHotbarSize(); var1++) {
                ItemStack var2 = mc.thePlayer.inventory.getStackInSlot(var1);
                if (var2 != null
                    && var2.getItem() == Items.prismarine_shard
                    && var2.hasDisplayName()
                    && ClientUtils.AOAtn(var2.getDisplayName()).equals("Block Zapper")) {
                    return var1;
                }
            }

            return -1;
        }
    }

    private boolean isHoldingBlockZapper() {
        if (mc.thePlayer == null) {
            return false;
        } else {
            ItemStack var1 = mc.thePlayer.getHeldItem();
            if (var1 == null || var1.getItem() != Items.prismarine_shard) {
                return false;
            } else {
                return !var1.hasDisplayName() ? false : ClientUtils.AOAtn(var1.getDisplayName()).equals("Block Zapper");
            }
        }
    }

    private float computeMaxBreakSpeed(Block var1) {
        if (mc.thePlayer == null) {
            return 0.0F;
        } else {
            float var2 = this.requiresPickaxe(var1) ? 0.0F : BlockUtils.getBreakSpeed(var1, null, this.ignoreMiningFatigue.bool(), this.groundSpoof.bool());

            for (int var3 = 0; var3 < InventoryPlayer.getHotbarSize(); var3++) {
                ItemStack var4 = mc.thePlayer.inventory.getStackInSlot(var3);
                if (this.isEffectiveTool(var4, var1)) {
                    float var5 = BlockUtils.getBreakSpeed(var1, var4, this.ignoreMiningFatigue.bool(), this.groundSpoof.bool());
                    if (var5 > var2) {
                        var2 = var5;
                    }
                }
            }

            return var2;
        }
    }

    private int findBestToolSlot(Block var1) {
        if (mc.thePlayer == null) {
            return -1;
        } else {
            int var2 = -1;
            float var3 = this.requiresPickaxe(var1) ? 0.0F : BlockUtils.getBreakSpeed(var1, null, this.ignoreMiningFatigue.bool(), this.groundSpoof.bool());

            for (int var4 = 0; var4 < InventoryPlayer.getHotbarSize(); var4++) {
                ItemStack var5 = mc.thePlayer.inventory.getStackInSlot(var4);
                if (this.isEffectiveTool(var5, var1)) {
                    float var6 = BlockUtils.getBreakSpeed(var1, var5, this.ignoreMiningFatigue.bool(), this.groundSpoof.bool());
                    if (var6 > var3) {
                        var3 = var6;
                        var2 = var4;
                    }
                }
            }

            return var2;
        }
    }

    private boolean IdcX(Block var1) {
        return !this.requiresPickaxe(var1) ? true : this.findBestToolSlot(var1) >= 0;
    }

    private boolean isEffectiveTool(ItemStack var1, Block var2) {
        return var1 == null ? false : !this.requiresPickaxe(var2) || var1.getItem() instanceof ItemPickaxe;
    }

    private boolean requiresPickaxe(Block var1) {
        if (var1 != null && !(var1 instanceof BlockBed)) {
            Material var2 = var1.getMaterial();
            return var2 == Material.iron || var2 == Material.anvil || var2 == Material.rock;
        } else {
            return false;
        }
    }

    private BreakTarget getZeroTickSwapTarget(BreakTarget var1) {
        if (this.blockZapper.bool() && this.zeroTickBlockZapper.bool()) {
            Block var2 = BlockUtils.iepjdt(var1.getBlockPos());
            if (var2 instanceof BlockBed) {
                return null;
            } else {
                int var3 = this.findBlockZapperSlot();
                if (var3 < 0) {
                    return null;
                } else {
                    BlockPos var4 = this.bedFinder.findAdjacentBedPart(var1.getBlockPos());
                    if (var4 == null) {
                        return null;
                    } else {
                        AxisAlignedBB var5 = BlockUtils.getSelectedBounds(var4);
                        if (var5 == null) {
                            return null;
                        } else {
                            Vec3 var6 = mc.thePlayer.getPositionEyes(1.0F);
                            float[] var7 = TargetRayTracer.LynS(var5, var6, RotationUtils.lastSentRotation[0]);
                            if (TargetRayTracer.rayTraceFromRotation(BlockUtils.getSelectedBounds(var1.getBlockPos()), var6, this.range.number(), var7[0], var7[1]) == null) {
                                return null;
                            } else {
                                if (this.FMJYpd == -1 && mc.thePlayer.inventory.currentItem != var3) {
                                    this.FMJYpd = mc.thePlayer.inventory.currentItem;
                                }

                                ItemStack var8 = mc.thePlayer.inventory.getStackInSlot(var3);
                                this.qb5 = var3;
                                this.c08PacketPlayerBlockPlacement = new C08PacketPlayerBlockPlacement(
                                    var1.getBlockPos(),
                                    var1.getSide().getIndex(),
                                    var8,
                                    (float)(var1.getHitVec().xCoord - var1.getBlockPos().getX()),
                                    (float)(var1.getHitVec().yCoord - var1.getBlockPos().getY()),
                                    (float)(var1.getHitVec().zCoord - var1.getBlockPos().getZ())
                                );
                                int var9 = this.findBestToolSlot(BlockUtils.iepjdt(var4));
                                this.MxT = var9;
                                this.kJp = -1;
                                Vec3 var10 = RotationUtils.ONLLQYf(var5, var6);
                                EnumFacing var11 = BlockUtils.getFacingTowardPoint(var4, var10);
                                return new BreakTarget(var4, var10, var11);
                            }
                        }
                    }
                }
            }
        } else {
            return null;
        }
    }

    private void PSfg(int var1) {
        if (var1 >= 0) {
            this.kJp = var1 == mc.thePlayer.inventory.currentItem ? -1 : var1;
        }
    }

    private void switchToSlot(int var1) {
        if (var1 != -1 && var1 != mc.thePlayer.inventory.currentItem) {
            this.slotSwitchDepth++;

            try {
                mc.thePlayer.inventory.currentItem = var1;
                mc.playerController.syncCurrentPlayItem();
            } finally {
                this.slotSwitchDepth--;
            }
        }
    }

    private void clearPendingPlacement() {
        this.qb5 = -1;
        this.MxT = -1;
        this.c08PacketPlayerBlockPlacement = null;
    }

    private boolean canMine() {
        return mc.thePlayer != null
            && !mc.thePlayer.isDead
            && mc.thePlayer.getHealth() > 0.0F
            && mc.thePlayer.capabilities.allowEdit
            && !mc.thePlayer.capabilities.isCreativeMode
            && !mc.thePlayer.isSpectator();
    }

    private boolean isTargetBreakable() {
        if (this.blockPos != null && mc.theWorld != null) {
            Block var1 = BlockUtils.iepjdt(this.blockPos);
            return var1 != null && var1 != Blocks.air && var1.getBlockHardness(mc.theWorld, this.blockPos) >= 0.0F;
        } else {
            return false;
        }
    }

    private boolean iEjyDt() {
        return false;
    }

    public static boolean canBreakBlock(BedNuker var0, Block var1) {
        return var0.IdcX(var1);
    }

    public static float getMaxBreakSpeed(BedNuker var0, Block var1) {
        return var0.computeMaxBreakSpeed(var1);
    }
}
