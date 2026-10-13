package stone.util.raytrace;

import stone.util.ClientUtils;
import stone.util.rotation.AngleUtils;
import com.google.common.base.Predicates;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;

public final class RayTracer {
    private final Minecraft mc;

    public RayTracer(Minecraft var1) {
        this.mc = var1;
    }

    public MovingObjectPosition traceBlocks(double var1, float var3, float var4, boolean var5, boolean var6, boolean var7) {
        Vec3 var8 = this.mc.thePlayer.getPositionEyes(1.0F);
        Vec3 var9 = extendRay(var8, AngleUtils.getLookVector(var3, var4), var1);
        return this.mc.theWorld.rayTraceBlocks(var8, var9, var5, var6, var7);
    }

    public MovingObjectPosition juhtohK(double var1, float var3, float var4) {
        MovingObjectPosition var5 = this.traceBlocks(var1, var3, var4, false, false, false);
        return var5 != null && var5.typeOfHit == MovingObjectType.BLOCK ? var5 : null;
    }

    public MovingObjectPosition jttAk(double var1, float var3, float[] var4, EntityLivingBase var5) {
        if (var5 != null) {
            MovingObjectPosition var6 = this.traceBlocksAndEntity(var1, var3, var4, var5);
            if (var6 != null) {
                return var6;
            }
        }

        float[] var23 = var4 == null ? new float[]{this.mc.thePlayer.rotationYaw, this.mc.thePlayer.rotationPitch} : var4;
        double var7 = this.mc.playerController.extendedReach() ? 6.0 : var1;
        Vec3 var9 = this.mc.thePlayer.getPositionEyes(var3);
        Vec3 var10 = AngleUtils.getLookVector(var23[0], var23[1]);
        MovingObjectPosition var11 = this.traceBlocks(var1, var23[0], var23[1], false, false, true);
        double var12 = this.mc.playerController.extendedReach() ? 6.0 : var1;
        if (var11 != null) {
            var12 = var9.distanceTo(var11.hitVec);
        }

        Vec3 var14 = extendRay(var9, var10, var7);
        ClosestHitSelector var15 = new ClosestHitSelector(var12);

        for (Entity var17 : this.collectEntitiesAlongRay(var10, var7)) {
            AxisAlignedBB var18 = HitVecUtils.fejS(var17);
            MovingObjectPosition var19 = var18.calculateIntercept(var9, var14);
            if (var18.isVecInside(var9)) {
                var15.MuJa(var17, var19 == null ? var9 : var19.hitVec);
            } else if (var19 != null) {
                double var20 = var9.distanceTo(var19.hitVec);
                boolean var22 = var17 == this.mc.thePlayer.ridingEntity;
                var15.ydMynT(var17, var19.hitVec, var20, var22, !var22 || ClientUtils.canRiderInteract(this.mc.thePlayer));
            }
        }

        return var15.hasValidHit() ? new MovingObjectPosition((Entity)var15.getHitEntity(), (Vec3)var15.getHitPosition()) : null;
    }

    public MovingObjectPosition traceBlocksAndEntity(double var1, float var3, float[] var4, EntityLivingBase var5) {
        MovingObjectPosition var6 = this.traceBlocks(var1, var4[0], var4[1], false, false, true);
        Vec3 var7 = this.mc.thePlayer.getPositionEyes(var3);
        double var8 = var6 == null ? var1 : var7.distanceTo(var6.hitVec);
        if (var5 != null) {
            Vec3 var10 = AngleUtils.getLookVector(var4[0], var4[1]);
            Vec3 var11 = extendRay(var7, var10, var1);
            AxisAlignedBB var12 = HitVecUtils.fejS(var5);
            MovingObjectPosition var13 = var12.calculateIntercept(var7, var11);
            if (var12.isVecInside(var7)) {
                return new MovingObjectPosition(var5, var7);
            }

            if (var13 != null && var7.distanceTo(var13.hitVec) < var8) {
                return new MovingObjectPosition(var5, var13.hitVec);
            }
        }

        return var6;
    }

    private List<Entity> collectEntitiesAlongRay(Vec3 var1, double var2) {
        AxisAlignedBB var4 = this.mc
            .thePlayer
            .getEntityBoundingBox()
            .addCoord(var1.xCoord * var2, var1.yCoord * var2, var1.zCoord * var2)
            .expand(1.0, 1.0, 1.0);
        return this.mc
            .theWorld
            .getEntitiesInAABBexcluding(this.mc.thePlayer, var4, Predicates.and(EntitySelectors.NOT_SPECTATING, Entity::canBeCollidedWith));
    }

    private static Vec3 extendRay(Vec3 var0, Vec3 var1, double var2) {
        return var0.addVector(var1.xCoord * var2, var1.yCoord * var2, var1.zCoord * var2);
    }
}
