package stone.module.other.anticheat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;

public class AnticheatPlayerState {
    public double horizontalSpeed;
    public int GefiE;
    public int autoblockTicks;
    public double deltaZ;
    public int scaffoldFlagCount;
    public int sprintUseItemTicks;
    public boolean GVTx;
    public double QSdcI;

    public void updateFromPlayer(EntityPlayer var1) {
        this.updateMovementDelta(var1);
        if (AnticheatMath.BfnuQ(this.horizontalSpeed)) {
            this.GefiE = AnticheatMath.QgxOo(this.GefiE);
        } else {
            this.GefiE = 0;
        }

        boolean var3 = var1.isSneaking();
        this.autoblockTicks = AnticheatMath.nextStreakCount(this.autoblockTicks, var1.isSwingInProgress && var1.isBlocking());
        this.sprintUseItemTicks = AnticheatMath.nextStreakCount(this.sprintUseItemTicks, var1.isSprinting() && var1.isUsingItem());
        boolean var4 = var1.getHeldItem() != null && var1.getHeldItem().getItem() instanceof ItemBlock;
        this.scaffoldFlagCount = AnticheatMath.pFoo(this.scaffoldFlagCount, var1.rotationPitch, var4, var1.swingProgressInt, this.GVTx, var3);
    }

    private void updateMovementDelta(EntityPlayer var1) {
        this.QSdcI = var1.posX - var1.lastTickPosX;
        this.deltaZ = var1.posZ - var1.lastTickPosZ;
        this.horizontalSpeed = AnticheatMath.horizontalSpeed(this.QSdcI, this.deltaZ);
    }

    public void recordSneakingState(EntityPlayer var1) {
        this.GVTx = var1.isSneaking();
    }
}
