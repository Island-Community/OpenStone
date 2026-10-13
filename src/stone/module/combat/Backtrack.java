package stone.module.combat;

import stone.component.combat.TargetComponent;
import stone.core.Color;
import stone.event.DisconnectEvent;
import stone.event.LoadWorldEvent;
import stone.event.PacketReceiveEvent;
import stone.event.Phase;
import stone.event.PostWalkingUpdateEvent;
import stone.event.RenderTickEvent;
import stone.event.RenderWorldLastEvent;
import stone.event.Subscribe;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.ModuleManager;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.module.combat.AutoClicker;
import stone.module.combat.TriggerBot;
import stone.util.animation.Animation;
import stone.util.animation.Easing;
import stone.util.packet.BlinkComponent;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class Backtrack extends Module {
    public static AxisAlignedBB realTargetBoundingBox;
    public EntityLivingBase targetEntity;
    public boolean isActive;

    private Vec3 realTargetPosition = new Vec3(0.0, 0.0, 0.0);
    private final Animation xAnimation = new Animation(Easing.LINEAR, 50L);
    private final Animation yAnimation = new Animation(Easing.LINEAR, 50L);
    private final Animation zAnimation = new Animation(Easing.LINEAR, 50L);

    private final Setting maxPingSpoof = addNumber("Max Ping Spoof", "Maximum ping spoof hold duration in ms",
            1000, 50, 10000, 0);
    private final Setting renderRealLocation = addBoolean("Render Real Location", "Renders target's true server position", true);
    private final Setting colour = addColour("Color", "Box color; alpha is 50 as in OpenRise",
            new Color(0.9f, 0.2f, 0.2f, 1.0f));
    private final Setting reachDisplay = addBoolean("Reach Display", "Display backtracked reach distance below crosshair", true);

    private double displayedReach;
    private long reachDisplayUntil;

    public Backtrack() {
        super("Backtrack", "Delays incoming packets so you hit where targets were", Category.COMBAT, Subcategory.EXTREME);
    }

    @Override
    public String suffix() {
        return (int) maxPingSpoof.number() + "ms";
    }

    @Override
    public void onEnable(Context context) {
        this.isActive = true;
        this.targetEntity = null;
        this.realTargetPosition = new Vec3(0.0, 0.0, 0.0);
        realTargetBoundingBox = null;
        this.displayedReach = 0.0;
        this.reachDisplayUntil = 0L;
        BlinkComponent.disable();
        BlinkComponent.dispatch();
    }

    @Override
    public void onDisable(Context context) {
        this.targetEntity = null;
        this.realTargetPosition = new Vec3(0.0, 0.0, 0.0);
        realTargetBoundingBox = null;
        this.displayedReach = 0.0;
        this.reachDisplayUntil = 0L;
        BlinkComponent.disable();
        BlinkComponent.dispatch();
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        this.targetEntity = null;
        this.realTargetPosition = new Vec3(0.0, 0.0, 0.0);
        realTargetBoundingBox = null;
        this.displayedReach = 0.0;
        this.reachDisplayUntil = 0L;
        BlinkComponent.clear();
    }

    @Subscribe
    public void onLoadWorld(LoadWorldEvent event) {
        this.targetEntity = null;
        this.realTargetPosition = new Vec3(0.0, 0.0, 0.0);
        realTargetBoundingBox = null;
        this.displayedReach = 0.0;
        this.reachDisplayUntil = 0L;
        BlinkComponent.clear();
    }

    public boolean isLagging() {
        return enabled() && BlinkComponent.isBlinking();
    }

    private double distanceToPlayer(Vec3 pos) {
        if (mc.thePlayer == null) {
            return 0.0;
        }
        double dx = pos.xCoord - mc.thePlayer.posX;
        double dy = pos.yCoord - mc.thePlayer.posY;
        double dz = pos.zCoord - mc.thePlayer.posZ;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Subscribe
    public void onPostWalkingUpdate(PostWalkingUpdateEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        List<EntityLivingBase> list = TargetComponent.getTargets(9.0);
        if (list.isEmpty()) {
            this.isActive = true;
            this.targetEntity = null;
            realTargetBoundingBox = null;
            BlinkComponent.disable();
            BlinkComponent.dispatch();
            return;
        }

        EntityLivingBase entity = list.get(0);
        if (entity != this.targetEntity) {
            this.targetEntity = entity;
            this.realTargetPosition = new Vec3(entity.posX, entity.posY, entity.posZ);
            this.xAnimation.setValue(entity.posX);
            this.xAnimation.setDestinationValue(entity.posX);
            this.yAnimation.setValue(entity.posY);
            this.yAnimation.setDestinationValue(entity.posY);
            this.zAnimation.setValue(entity.posZ);
            this.zAnimation.setDestinationValue(entity.posZ);
        }

        if (this.targetEntity != null && (this.targetEntity.isDead || this.targetEntity.getHealth() <= 0.0f)) {
            this.targetEntity = null;
            realTargetBoundingBox = null;
            BlinkComponent.disable();
            BlinkComponent.dispatch();
            return;
        }

        if (this.targetEntity != null && isAttacking()) {
            double d0 = this.targetEntity.width / 2.0;
            double d1 = this.targetEntity.height;
            realTargetBoundingBox = new AxisAlignedBB(
                this.realTargetPosition.xCoord - d0,
                this.realTargetPosition.yCoord,
                this.realTargetPosition.zCoord - d0,
                this.realTargetPosition.xCoord + d0,
                this.realTargetPosition.yCoord + d1,
                this.realTargetPosition.zCoord + d0
            );

            double d2 = distanceToPlayer(this.realTargetPosition);
            double d3 = this.targetEntity.getDistanceToEntity(mc.thePlayer);
            boolean flag1 = d2 > d3 && d2 > 2.5 && d2 < 6.0;

            if (flag1) {
                BlinkComponent.hold((int) this.maxPingSpoof.number(), true, true, false, true);
            } else {
                BlinkComponent.disable();
                BlinkComponent.dispatch();
            }
        }
    }

    @Subscribe(priority = Subscribe.Priority.HIGHEST)
    public void onPacketReceive(PacketReceiveEvent event) {
        Packet<?> packet = event.getPacket();
        if (this.targetEntity != null) {
            if (packet instanceof S14PacketEntity s14) {
                if (s14.getEntity(mc.theWorld) == this.targetEntity || s14.entityId == this.targetEntity.getEntityId()) {
                    this.realTargetPosition = this.realTargetPosition.addVector(
                        s14.func_149062_c() / 32.0,
                        s14.func_149061_d() / 32.0,
                        s14.func_149064_e() / 32.0
                    );
                }
            } else if (packet instanceof S18PacketEntityTeleport teleport) {
                if (teleport.getEntityId() == this.targetEntity.getEntityId()) {
                    this.realTargetPosition = new Vec3(
                        teleport.getX() / 32.0,
                        teleport.getY() / 32.0,
                        teleport.getZ() / 32.0
                    );
                }
            }
        }
    }

    @Subscribe
    public void onRenderTick(RenderTickEvent event) {
        if (event.phase != Phase.END || mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) {
            return;
        }
        if (!this.reachDisplay.bool()) {
            return;
        }

        if (this.targetEntity != null && isAttacking()) {
            double d0 = distanceToPlayer(this.realTargetPosition);
            double d1 = this.targetEntity.getDistanceToEntity(mc.thePlayer);
            boolean flag = d0 > d1 && d0 > 2.5 && d0 < 6.0;
            if (flag && d0 > 3.0) {
                this.displayedReach = d0;
                this.reachDisplayUntil = System.currentTimeMillis() + 600L;
            }
        }

        if (System.currentTimeMillis() < this.reachDisplayUntil && this.displayedReach > 0.0) {
            ScaledResolution sr = new ScaledResolution(mc);
            String text = String.format(Locale.ROOT, "Reach: %.2f", this.displayedReach);
            float x = sr.getScaledWidth() / 2.0f;
            float y = sr.getScaledHeight() / 2.0f + 20.0f;
            float textWidth = mc.fontRendererObj.getStringWidth(text);
            mc.fontRendererObj.drawStringWithShadow(text, x - textWidth / 2.0f, y, -1);
        }
    }

    @Subscribe
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (this.targetEntity == null || !this.renderRealLocation.bool() || mc.thePlayer == null || mc.getRenderManager() == null) {
            return;
        }

        this.xAnimation.setDuration(150L);
        this.yAnimation.setDuration(150L);
        this.zAnimation.setDuration(150L);

        this.xAnimation.run(this.realTargetPosition.xCoord);
        this.yAnimation.run(this.realTargetPosition.yCoord);
        this.zAnimation.run(this.realTargetPosition.zCoord);

        double x = this.xAnimation.getValue();
        double y = this.yAnimation.getValue();
        double z = this.zAnimation.getValue();
        double w = this.targetEntity.width / 2.0;
        double h = this.targetEntity.height;

        double expand = 0.14;
        AxisAlignedBB box = new AxisAlignedBB(
            x - w, y, z - w,
            x + w, y + h, z + w
        ).expand(expand, expand, expand);

        drawBoundingBox(box, this.colour.colour());
    }

    public static void drawBoundingBox(AxisAlignedBB box, Color c) {
        if (mc.getRenderManager() == null) {
            return;
        }

        double vx = mc.getRenderManager().viewerPosX;
        double vy = mc.getRenderManager().viewerPosY;
        double vz = mc.getRenderManager().viewerPosZ;

        AxisAlignedBB bb = box.offset(-vx, -vy, -vz);

        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_DEPTH_TEST);

            GL11.glColor4f(c.r(), c.g(), c.b(), 50.0f / 255.0f);
            GL11.glBegin(GL11.GL_QUADS);

            GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
            GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
            GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
            GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);

            GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
            GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
            GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
            GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);

            GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
            GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
            GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
            GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);

            GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
            GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
            GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
            GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);

            GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
            GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
            GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
            GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);

            GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
            GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
            GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
            GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);

            GL11.glEnd();

            GL11.glColor4f(c.r(), c.g(), c.b(), 180.0f / 255.0f);
            GL11.glLineWidth(1.5f);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glBegin(GL11.GL_LINES);

            GL11.glVertex3d(bb.minX, bb.minY, bb.minZ); GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
            GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ); GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
            GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ); GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
            GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ); GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);

            GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ); GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
            GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ); GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
            GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ); GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
            GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ); GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);

            GL11.glVertex3d(bb.minX, bb.minY, bb.minZ); GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
            GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ); GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
            GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ); GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
            GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ); GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);

            GL11.glEnd();
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
        } finally {
            GL11.glDepthMask(true);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }

    private static boolean isAttacking() {
        if (mc.thePlayer != null && mc.thePlayer.isSwingInProgress) {
            return true;
        }
        if (mc.currentScreen == null && Mouse.isButtonDown(0)) {
            return true;
        }
        if (mc.gameSettings != null && mc.gameSettings.keyBindAttack.isKeyDown()) {
            return true;
        }
        AutoClicker clicker = ModuleManager.get(AutoClicker.class);
        if (clicker != null && clicker.enabled()) {
            return true;
        }
        TriggerBot trigger = ModuleManager.get(TriggerBot.class);
        if (trigger != null && trigger.enabled()) {
            return true;
        }
        return false;
    }
}
