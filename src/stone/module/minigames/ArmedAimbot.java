package stone.module.minigames;

import stone.event.MouseEvent;
import stone.event.Phase;
import stone.event.PostWalkingUpdateEvent;
import stone.event.PreUpdateEvent;
import stone.event.RenderTickEvent;
import stone.event.RenderWorldLastEvent;
import stone.event.RotationEvent;
import stone.event.Subscribe;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.module.other.AntiBot;
import stone.util.ClientUtils;
import stone.util.rotation.RotationHandler;
import stone.util.rotation.RotationUtils;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

public class ArmedAimbot extends Module {
    private static final String[] pW8 = new String[]{"Health", "FOV", "Hurttime", "Distance"};
    private static final Set<Item> items;
    private Setting speed;
    private Setting multipoint;
    private Setting randomization;
    private Setting fov;
    private Setting prediction;
    private Setting sort;
    private Setting headshot;
    private Setting holdRightClick;
    private Setting keepMoveDirection;
    private EntityPlayer entityPlayer;
    private int ticksOnTarget;
    private boolean ia9;
    private boolean vJb;
    private double MVz;
    private double targetY;
    private double targetZ;
    private boolean pRh;
    private float screenX;
    private float screenY;
    private float screenDepth;
    private boolean hasScreenPosition;
    private final FloatBuffer projectionMatrixBuffer = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer modelViewMatrixBuffer = BufferUtils.createFloatBuffer(16);
    private final IntBuffer intBuffer = BufferUtils.createIntBuffer(16);
    private final FloatBuffer screenCoordsBuffer = BufferUtils.createFloatBuffer(3);

    public ArmedAimbot() {
        super("ArmedAimbot", "Aims the gun in BedWars Armed mode", Category.MINIGAMES, Subcategory.EXTREME);
        this.speed = addNumber("Speed", "", 10.0F, 1.0F, 30.0F, 0);
        this.multipoint = addNumber("Multipoint", "", 0.0F, 0.0F, 100.0F, 0);
        this.randomization = addNumber("Randomization", "", 50.0F, 0.0F, 100.0F, 0);
        this.fov = addNumber("FOV", "", 90.0F, 15.0F, 360.0F, 0);
        this.prediction = addNumber("Prediction", "", 3.0F, 0.0F, 10.0F, 0);
        this.sort = addChoice("Sort", "", List.of(pW8), 1);
        this.headshot = addBoolean("Headshot", "", false);
        this.holdRightClick = addBoolean("Hold Right Click", "", true);
        this.keepMoveDirection = addBoolean("Keep move direction", "", true);
    }

    @Override
    public void guiUpdate() {
        this.multipoint.setVisible(!this.headshot.bool());
    }

    @Override
    public void onEnable(Context context) {
        this.resetState();
    }

    @Override
    public void onDisable(Context context) {
        this.resetState();
    }

    private void resetState() {
        this.entityPlayer = null;
        this.ticksOnTarget = 0;
        this.ia9 = false;
        this.vJb = false;
        this.pRh = false;
        this.hasScreenPosition = false;
    }

    @Subscribe
    public void onRotation(RotationEvent var1) {
        if (!this.areConditionsMet()) {
            this.clearTarget();
        } else {
            EntityPlayer var2 = this.entityPlayer;
            this.entityPlayer = this.findBestTarget(true);
            if (this.entityPlayer != var2) {
                this.ticksOnTarget = 0;
            }

            if (this.entityPlayer == null) {
                this.pRh = false;
            } else {
                this.ticksOnTarget++;
                float[] var3 = this.XxvG(this.entityPlayer, var1);
                if (var3 == null) {
                    this.pRh = false;
                } else {
                    RotationHandler.getInstance().ljYma8(!this.keepMoveDirection.bool());
                    var1.setRotation(var3[0], var3[1], 10);
                    if (this.ticksOnTarget > 1 && !this.vJb && this.isHeldItemUndamaged()) {
                        this.ia9 = true;
                    }
                }
            }
        }
    }

    @Subscribe
    public void onPreUpdate(PreUpdateEvent var1) {
        if (this.ia9) {
            this.ia9 = false;
            ItemStack var2 = mc.thePlayer.getHeldItem();
            if (var2 != null) {
                mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(new BlockPos(-1, -1, -1), 255, var2, 0.0F, 0.0F, 0.0F));
                this.vJb = true;
            }
        }
    }

    @Subscribe
    public void onPostWalkingUpdate(PostWalkingUpdateEvent var1) {
        this.vJb = false;
    }

    @Subscribe
    public void onMouse(MouseEvent var1) {
        if (this.holdRightClick.bool()) {
            if (var1.button == 1 && var1.pressed) {
                if (ClientUtils.isInWorld()) {
                    ItemStack var2 = mc.thePlayer.getHeldItem();
                    if (var2 != null && items.contains(var2.getItem())) {
                        var1.setCancelled(true);
                    }
                }
            }
        }
    }

    @Subscribe
    public void onRenderWorldLast(RenderWorldLastEvent var1) {
        this.hasScreenPosition = false;
        if (this.entityPlayer != null && this.pRh) {
            double var2 = mc.getRenderManager().viewerPosX;
            double var4 = mc.getRenderManager().viewerPosY;
            double var6 = mc.getRenderManager().viewerPosZ;
            GL11.glGetFloat(2982, this.projectionMatrixBuffer);
            GL11.glGetFloat(2983, this.modelViewMatrixBuffer);
            GL11.glGetInteger(2978, this.intBuffer);
            boolean var8 = GLU.gluProject(
                (float)(this.MVz - var2), (float)(this.targetY - var4), (float)(this.targetZ - var6), this.projectionMatrixBuffer, this.modelViewMatrixBuffer, this.intBuffer, this.screenCoordsBuffer
            );
            if (var8) {
                this.screenX = this.screenCoordsBuffer.get(0);
                this.screenY = this.screenCoordsBuffer.get(1);
                this.screenDepth = this.screenCoordsBuffer.get(2);
                this.hasScreenPosition = true;
            }
        }
    }

    @Subscribe
    public void onRenderTick(RenderTickEvent var1) {
        if (var1.phase == Phase.END) {
            if (this.hasScreenPosition && !(this.screenDepth < 0.0F) && !(this.screenDepth >= 1.0003684F)) {
                ScaledResolution var2 = new ScaledResolution(mc);
                int var3 = var2.getScaleFactor();
                float var4 = this.screenX / var3;
                float var5 = (mc.displayHeight - this.screenY) / var3;
                float var6 = 3.0F;
                GL11.glPushMatrix();
                GL11.glEnable(3042);
                GL11.glBlendFunc(770, 771);
                GL11.glDisable(3553);
                GL11.glLineWidth(3.0F);
                GL11.glColor4f(1.0F, 0.0F, 0.0F, 1.0F);
                GL11.glBegin(1);
                GL11.glVertex2f(var4 - var6, var5);
                GL11.glVertex2f(var4 + var6, var5);
                GL11.glEnd();
                GL11.glBegin(1);
                GL11.glVertex2f(var4, var5 - var6);
                GL11.glVertex2f(var4, var5 + var6);
                GL11.glEnd();
                GL11.glEnable(3553);
                GL11.glDisable(3042);
                GL11.glPopMatrix();
            }
        }
    }

    private boolean areConditionsMet() {
        if (mc.currentScreen != null || !mc.inGameHasFocus) {
            return false;
        } else if (!ClientUtils.isInWorld()) {
            return false;
        } else if (!this.isInBedWarsGame()) {
            return false;
        } else {
            return !this.isHoldingAimbotItem() ? false : !this.holdRightClick.bool() || Mouse.isButtonDown(1);
        }
    }

    private void clearTarget() {
        if (this.entityPlayer != null) {
            this.entityPlayer = null;
            this.ticksOnTarget = 0;
        }

        this.pRh = false;
    }

    private boolean isHoldingAimbotItem() {
        ItemStack var1 = mc.thePlayer.getHeldItem();
        return var1 != null && items.contains(var1.getItem());
    }

    private boolean isHeldItemUndamaged() {
        ItemStack var1 = mc.thePlayer.getHeldItem();
        return var1 != null && var1.getItemDamage() == 0;
    }

    private double getWeaponRange() {
        ItemStack var1 = mc.thePlayer.getHeldItem();
        if (var1 != null && items.contains(var1.getItem())) {
            try {
                for (String var4 : var1.getTooltip(mc.thePlayer, false)) {
                    String var5 = ClientUtils.AOAtn(var4);
                    if (var5.contains("Range: ")) {
                        return Double.parseDouble(var5.split("Range: ")[1].trim().split(" ")[0]) - 1.0;
                    }
                }
            } catch (Exception var6) {
            }

            return 0.0;
        } else {
            return 0.0;
        }
    }

    private boolean isInBedWarsGame() {
        List var1 = ClientUtils.PxSw4();
        if (var1 == null || var1.size() < 7) {
            return false;
        } else if (!ClientUtils.AOAtn((String)var1.get(0)).startsWith("BED WARS")) {
            return false;
        } else {
            try {
                String[] var2 = ClientUtils.AOAtn((String)var1.get(1)).split("  ");
                if (var2.length > 1) {
                    String var3 = var2[1];
                    if (var3.endsWith("]")) {
                        var3 = var3.split(" ")[0];
                    }

                    if (var3.charAt(0) == 'L') {
                        return false;
                    }
                }
            } catch (Exception var4) {
            }

            return ClientUtils.AOAtn((String)var1.get(5)).startsWith("R Red:") && ClientUtils.AOAtn((String)var1.get(6)).startsWith("B Blue:");
        }
    }

    private EntityPlayer findBestTarget(boolean var1) {
        double var2 = this.getWeaponRange();
        if (var2 <= 0.0) {
            return null;
        } else {
            double var4 = var2 * var2;
            int var6 = (int)this.fov.number();
            int var7 = (int)this.sort.choice();
            double var8 = this.multipoint.number();
            float var10 = mc.thePlayer.rotationYaw;
            if (var1) {
                Float var11 = RotationHandler.getInstance().getTargetYaw();
                if (var11 != null) {
                    var10 = var11;
                }
            }

            ArrayList var19 = new ArrayList();

            for (EntityPlayer var13 : mc.theWorld.playerEntities) {
                if (var13 != mc.thePlayer && var13.deathTime == 0 && !ClientUtils.isFriend(var13) && !ClientUtils.isTeammate(var13) && !AntiBot.shouldHideEntity(var13)) {
                    String var14 = var13.getUniqueID().toString();
                    if (var14.length() > 14) {
                        char var15 = var14.charAt(14);
                        if (var15 != '4' && var15 != '1') {
                            continue;
                        }
                    }

                    double var20 = RotationUtils.getDistanceSqToEntity(var13);
                    if (!(var20 > var4)) {
                        if (var6 != 360) {
                            float var17 = RotationUtils.ilaZ(var13.posX, var13.posZ);
                            if (!ClientUtils.Bhr95(var10, var6, var17)) {
                                continue;
                            }
                        }

                        if (RotationUtils.XKXgx5(var13, var8, var8, var2, false, true, true)) {
                            double var21 = mc.thePlayer.getDistanceSqToEntity(var13);
                            var19.add(new ArmedAimbot$0(var13, getCandidateSortValue(var13, var7, var21)));
                        }
                    }
                }
            }

            if (var19.isEmpty()) {
                return null;
            } else {
                var19.sort(Comparator.comparingDouble(ArmedAimbot::extractCandidateSortValue));
                return ((ArmedAimbot$0)var19.get(0)).xg9;
            }
        }
    }

    private float[] XxvG(EntityPlayer var1, RotationEvent var2) {
        double var3 = this.getWeaponRange();
        if (var3 <= 0.0) {
            return null;
        } else {
            int var5 = (int)this.prediction.number();
            int var6 = (int)this.speed.number();
            float var7 = (float)this.randomization.number();
            float var8 = var2.yaw != null ? var2.yaw : RotationUtils.lastSentRotation[0];
            float var9 = var2.pitch != null ? var2.pitch : RotationUtils.lastSentRotation[1];
            if (this.headshot.bool()) {
                double[] var13 = this.computeHeadshotPoint(var1, var5);
                this.setAimPoint(var13[0], var13[1], var13[2]);
                float[] var11 = this.computeAnglesToPoint(var13[0], var13[1], var13[2], var8, var9);
                return RotationUtils.smoothAnglesRandomized(var8, var9, var11[0], var11[1], var6, var7);
            } else {
                double var10 = this.multipoint.number();
                if (var5 > 0) {
                    float[] var14 = RotationUtils.Pav87(var1, var5);
                    if (var14 == null) {
                        return null;
                    } else {
                        this.setPredictedAimPoint(var1, var5);
                        return RotationUtils.smoothAnglesRandomized(var8, var9, var14[0], var14[1], var6, var7);
                    }
                } else {
                    float[] var12 = RotationHandler.getInstance().getEventEntityRotationsFull(var1, var2, var6, var10, var10, var7, true, var3, false, true, true);
                    this.setAimPointOnEntity(var1, var10);
                    return var12;
                }
            }
        }
    }

    private double[] computeHeadshotPoint(EntityPlayer var1, int var2) {
        double var3 = (var1.getEntityBoundingBox().minX + var1.getEntityBoundingBox().maxX) / 2.0;
        double var5 = (var1.getEntityBoundingBox().minZ + var1.getEntityBoundingBox().maxZ) / 2.0;
        double var7 = var1.getEntityBoundingBox().maxY - 0.1;
        if (var2 > 0) {
            double var9 = var1.posX - var1.lastTickPosX;
            double var11 = var1.posZ - var1.lastTickPosZ;
            var3 += var9 * var2;
            var5 += var11 * var2;
        }

        return new double[]{var3, var7, var5};
    }

    private float[] computeAnglesToPoint(double var1, double var3, double var5, float var7, float var8) {
        double var9 = var1 - mc.thePlayer.posX;
        double var11 = var5 - mc.thePlayer.posZ;
        double var13 = var3 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double var15 = var9 * var9 + var11 * var11;
        float var17;
        float var18;
        if (var15 < 1.0E-12) {
            var17 = var7;
            var18 = var13 > 0.0 ? -90.0F : 90.0F;
        } else {
            float var19 = (float)(Math.atan2(var11, var9) * (float) (180.0 / Math.PI)) - 90.0F;
            var17 = var7 + MathHelper.wrapAngleTo180_float(var19 - var7);
            double var20 = Math.sqrt(var15);
            float var22 = (float)(-(Math.atan2(var13, var20) * (float) (180.0 / Math.PI)));
            var18 = var8 + MathHelper.wrapAngleTo180_float(var22 - var8);
        }

        var18 = Math.max(-90.0F, Math.min(90.0F, var18));
        return new float[]{var17, var18};
    }

    private void setPredictedAimPoint(EntityPlayer var1, int var2) {
        double var3 = var1.posX - var1.lastTickPosX;
        double var5 = var1.posZ - var1.lastTickPosZ;
        this.MVz = var1.posX + var3 * var2;
        this.targetY = var1.posY + var1.getEyeHeight() * 0.9;
        this.targetZ = var1.posZ + var5 * var2;
        this.pRh = true;
    }

    private void setAimPoint(double var1, double var3, double var5) {
        this.MVz = var1;
        this.targetY = var3;
        this.targetZ = var5;
        this.pRh = true;
    }

    private void setAimPointOnEntity(EntityPlayer var1, double var2) {
        Vec3 var4 = RotationUtils.getEntityHitVec(var1, var2, var2);
        if (var4 != null) {
            this.MVz = var4.xCoord;
            this.targetY = var4.yCoord;
            this.targetZ = var4.zCoord;
            this.pRh = true;
        } else {
            this.pRh = false;
        }
    }

    private static double getCandidateSortValue(EntityPlayer var0, int var1, double var2) {
        switch (var1) {
            case 0:
                return var0.getHealth() + var0.getAbsorptionAmount();
            case 1:
            default:
                return Math.abs(ClientUtils.getYawDifferenceToEntity(var0, false)) + Math.abs(ClientUtils.coPw(var0, false));
            case 2:
                return var0.hurtTime;
            case 3:
                return var2;
        }
    }

    private static double extractCandidateSortValue(ArmedAimbot$0 var0) {
        return var0.eXpzKh;
    }

    static {
        Item[] var10002 = new Item[6];
        var10002[0] = Items.golden_hoe;
        var10002[1] = Items.stone_hoe;
        var10002[2] = Items.diamond_hoe;
        var10002[3] = Items.flint_and_steel;
        var10002[4] = Items.iron_hoe;
        var10002[5] = Items.wooden_hoe;
        items = new HashSet<>(Arrays.asList(var10002));
    }
}
