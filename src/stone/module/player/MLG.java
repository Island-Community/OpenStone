package stone.module.player;

import stone.event.PreUpdateEvent;
import stone.event.RenderWorldLastEvent;
import stone.event.RotationEvent;
import stone.event.Subscribe;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.ModuleManager;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.module.player.mlg.MlgState;
import stone.module.player.mlg.MlgUtils;
import stone.util.ClientUtils;
import net.minecraft.init.Items;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class MLG extends Module {
    public Setting pickUp;
    public Setting swapSlot;
    private final MlgState mlgState = new MlgState();

    public MLG() {
        super("MLG", "Places water under you to cancel fall damage", Category.PLAYER, Subcategory.ADVANCED);
        this.pickUp = addBoolean("Pick Up", "", true);
        this.swapSlot = addBoolean("Swap Slot", "", true);
    }

    @Override
    public void onDisable(Context context) {
        this.restorePreviousSlot();
        this.mlgState.reset();
    }

    @Subscribe
    public void onRenderWorldLast(RenderWorldLastEvent var1) {
        if (ClientUtils.isInWorld() && !mc.isGamePaused() && !mc.thePlayer.capabilities.isFlying && !mc.thePlayer.capabilities.isCreativeMode) {
            if (this.LvPq()) {
                MovingObjectPosition var2 = ClientUtils.rayTraceBlocksWithRotations(mc.playerController.getBlockReachDistance(), mc.thePlayer.rotationYaw, 90.0F);
                if (var2 != null && var2.typeOfHit == MovingObjectType.BLOCK && var2.sideHit == EnumFacing.UP) {
                    long var3 = System.currentTimeMillis();
                    if (this.mlgState.isReadyToRefill(var3)) {
                        if (!MlgUtils.EReKjl(mc.thePlayer.getHeldItem(), Items.water_bucket) && this.swapSlot.bool()) {
                            this.swapToWaterBucket();
                        }

                        if (MlgUtils.EReKjl(mc.thePlayer.getHeldItem(), Items.water_bucket)) {
                            this.mlgState.onWaterPlaced(var3, this.pickUp.bool());
                            this.sendItemUsePacket();
                            if (!this.pickUp.bool()) {
                                this.restorePreviousSlot();
                            }

                        }
                    }
                }
            }
        }
    }

    @Subscribe
    public void onPreUpdate(PreUpdateEvent var1) {
        if (!mc.isGamePaused()) {
            if (this.mlgState.shouldPickUpWater(System.currentTimeMillis()) && MlgUtils.EReKjl(mc.thePlayer.getHeldItem(), Items.bucket)) {
                this.mlgState.TcFaeva();
                this.sendItemUsePacket();
                this.restorePreviousSlot();
            }
        }
    }

    @Subscribe
    public void onRotation(RotationEvent var1) {
        if (ModuleManager.get(BedNuker.class) == null || !ModuleManager.get(BedNuker.class).shouldOverridePointedObject()) {
            if (ModuleManager.get(BridgeNuker.class) == null || !ModuleManager.get(BridgeNuker.class).shouldOverridePointedObject()) {
                if ((this.LvPq() || this.mlgState.isRecentlyPlaced(System.currentTimeMillis())) && this.findWaterBucketSlot() != -1) {
                    var1.setRotation(mc.thePlayer.rotationYaw, 90.0F, 45);
                }
            }
        }
    }

    private void swapToWaterBucket() {
        int var1 = this.findWaterBucketSlot();
        if (var1 != -1) {
            this.mlgState.NOMpoo(mc.thePlayer.inventory.currentItem);
            ClientUtils.setHeldSlot(var1, true);
        }
    }

    private void restorePreviousSlot() {
        int var1 = this.mlgState.consumeSavedSlot();
        if (var1 != -1 && mc.thePlayer != null) {
            ClientUtils.setHeldSlot(var1, true);
        }
    }

    private int findWaterBucketSlot() {
        return MlgUtils.KMqC(mc.thePlayer.inventory, Items.water_bucket);
    }

    private void sendItemUsePacket() {
        mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
    }

    private boolean LvPq() {
        return MlgUtils.isFallingDangerously(mc.thePlayer);
    }

}
