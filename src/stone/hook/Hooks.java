package stone.hook;

import stone.event.AttackEntityEvent;
import stone.event.ChangeCurrentItemEvent;
import stone.event.ChatReceivedEvent;
import stone.event.ClickMouseEvent;
import stone.event.DisconnectEvent;
import stone.event.EntityJoinWorldEvent;
import stone.event.EventBus;
import stone.event.GameLoopEvent;
import stone.event.GuiOpenEvent;
import stone.event.JumpEvent;
import stone.event.LeftClickEvent;
import stone.event.LoadWorldEvent;
import stone.event.MouseEvent;
import stone.event.MoveFlyingEvent;
import stone.event.MoveInputEvent;
import stone.event.MoveStateUpdateEvent;
import stone.event.PacketReceiveEvent;
import stone.event.PacketSendEvent;
import stone.event.Phase;
import stone.event.PlayerAttackEvent;
import stone.event.PostUpdateEvent;
import stone.event.PostWalkingUpdateEvent;
import stone.event.PrePlayerInteractEvent;
import stone.event.PreUpdateEvent;
import stone.event.RenderLivingPreEvent;
import stone.event.RenderPlayerPostEvent;
import stone.event.RenderPlayerPreEvent;
import stone.event.RenderTickEvent;
import stone.event.RenderWorldLastEvent;
import stone.event.RightClickEvent;
import stone.event.SetCurrentItemEvent;
import stone.event.TickEndEvent;
import stone.event.TickStartEvent;
import stone.event.UpdateWalkingPlayerEvent;
import stone.event.UseItemEvent;
import stone.event.VelocityEvent;
import stone.game.RotationEngine;
import stone.module.ModuleManager;
import stone.module.combat.AutoClicker;
import stone.module.combat.KeepSprint;
import stone.module.combat.Piercing;
import stone.module.minigames.AutoChest;
import stone.module.minigames.BedwarsUtils;
import stone.module.minigames.NukeDefend;
import stone.module.player.BedNuker;
import stone.module.player.BridgeNuker;
import stone.module.player.Freecam;
import stone.module.player.GhostHand;
import stone.module.player.InventoryManager;
import stone.module.render.AntiDebuff;
import stone.module.render.AntiInvis;
import stone.module.render.Cape;
import stone.module.render.FreeLook;
import stone.module.render.Nametags;
import stone.module.render.NoCameraClip;
import stone.ui.Client;
import stone.util.ClientUtils;
import stone.util.packet.PacketUtils;
import stone.util.raytrace.PointedObjectOverrider;
import stone.util.render.FakePlayerRenderer;
import stone.util.rotation.EntityRotationOverride;
import stone.util.rotation.RotationHandler;
import stone.util.rotation.RotationUtils;
import io.netty.channel.ChannelHandlerContext;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovementInputFromOptions;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import org.lwjgl.input.Mouse;

public final class Hooks {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final EntityRotationOverride freeLookRotation = new EntityRotationOverride();
    private static boolean freeLookRotated;
    private static float blockDamageBefore;
    private static double attackMotionX;
    private static double attackMotionZ;
    private static float walkingYaw;
    private static float walkingPitch;
    private static boolean walkingSneak;

    private Hooks() {}

    public static void tickStart() {
        EventBus.post(new TickStartEvent());
        EventBus.post(new TickEndEvent(Phase.START));
    }

    public static void targetRotation() {
        RotationHandler.getInstance().updateTargetRotation();
    }

    public static void tick() {
        Client.tick();
    }

    public static void controllerUpdated() {
        ModuleManager.get(BedNuker.class).processPendingPlacement();
    }

    public static void preInteract() {
        EventBus.post(new PrePlayerInteractEvent());
    }

    public static void blockClickSent() {
        BedNuker nuker = ModuleManager.get(BedNuker.class);
        if (nuker.isNuking()) {
            nuker.QLkR();
        }
    }

    public static void changeCurrentItem(InventoryPlayer inventory, int direction) {
        if (!EventBus.post(new ChangeCurrentItemEvent(direction, inventory.currentItem)).cancelled()) {
            inventory.changeCurrentItem(direction);
        }
    }

    public static void setThirdPersonView(GameSettings settings, int view) {
        if (FreeLook.DnH) {
            ModuleManager.get(FreeLook.class).disableFreeLook();
        } else {
            settings.thirdPersonView = view;
        }
    }

    public static void setCurrentItem(InventoryPlayer inventory, int slot) {
        if (!EventBus.post(new SetCurrentItemEvent(slot)).cancelled()) {
            inventory.currentItem = slot;
        }
    }

    public static void tickEnd() {
        EventBus.post(new TickEndEvent(Phase.END));
        if (ClientUtils.isInWorld()) {
            ModuleManager.update();
        }
    }

    public static void gameLoop() {
        EventBus.post(new GameLoopEvent());
        EventBus.post(new RenderTickEvent(Phase.START, mc.timer.renderPartialTicks));
    }

    public static void frame() {
        Client.frame();
    }

    public static boolean leftClick() {
        return EventBus.post(new MouseEvent(0, true, Mouse.getX(), Mouse.getY(), 0, 0, 0)).cancelled()
                || EventBus.post(new ClickMouseEvent(mc.objectMouseOver)).cancelled()
                || EventBus.post(new LeftClickEvent()).cancelled();
    }

    public static boolean rightClick() {
        if (Mouse.isButtonDown(1)
                && EventBus.post(new MouseEvent(1, true, Mouse.getX(), Mouse.getY(), 0, 0, 0)).cancelled()) {
            return true;
        }
        return EventBus.post(new RightClickEvent()).cancelled();
    }

    public static void loadWorld(WorldClient world, String message) {
        EventBus.post(new LoadWorldEvent(world));
    }

    public static boolean openGui(GuiScreen screen) {
        return EventBus.post(new GuiOpenEvent(screen)).cancelled();
    }

    public static boolean useItem(EntityPlayer player, World world, ItemStack stack) {
        return Freecam.cameraEntity != null || EventBus.post(new UseItemEvent(stack)).cancelled();
    }

    public static boolean placeBlock() {
        return ModuleManager.get(NukeDefend.class).shouldBlockDuplicatePlacement();
    }

    public static boolean attackEntity(EntityPlayer player, Entity target) {
        KeepSprint keepSprint = ModuleManager.get(KeepSprint.class);
        return Freecam.cameraEntity != null
                || keepSprint.enabled() && keepSprint.shouldCancelAttack(player, target)
                || EventBus.post(new AttackEntityEvent(target, player, true)).cancelled();
    }

    public static boolean clickBlock() {
        return Freecam.cameraEntity != null;
    }

    public static void clickHitDelay(PlayerControllerMP controller) {
        shortenHitDelay(controller);
    }

    public static boolean damageBlock(PlayerControllerMP controller) {
        blockDamageBefore = controller.curBlockDamageMP;
        return Freecam.cameraEntity != null;
    }

    public static void creativeHitDelay(PlayerControllerMP controller) {
        shortenHitDelay(controller);
    }

    public static void breakHitDelay(PlayerControllerMP controller) {
        shortenHitDelay(controller);
    }

    public static void blockDamaged(PlayerControllerMP controller) {
        float gained = controller.curBlockDamageMP - blockDamageBefore;
        float multiplier = breakSpeedMultiplier();
        if (gained > 0.0F && multiplier > 1.0F) {
            controller.curBlockDamageMP = Math.min(1.0F, blockDamageBefore + gained * multiplier);
        }
    }

    private static float breakSpeedMultiplier() {
        BedNuker bedNuker = ModuleManager.get(BedNuker.class);
        if (bedNuker.isNuking()) {
            return bedNuker.TnokwE();
        }
        BridgeNuker bridgeNuker = ModuleManager.get(BridgeNuker.class);
        return bridgeNuker.PfjH() ? bridgeNuker.getBreakSpeedMultiplier() : 1.0F;
    }

    private static void shortenHitDelay(PlayerControllerMP controller) {
        BridgeNuker bridgeNuker = ModuleManager.get(BridgeNuker.class);
        if (ModuleManager.get(BedNuker.class).isNuking()) {
            controller.blockHitDelay = 0;
        } else if (bridgeNuker.PfjH() && bridgeNuker.getBreakDelayTicks() < 5) {
            controller.blockHitDelay = bridgeNuker.getBreakDelayTicks();
        }
    }

    public static boolean turn(Entity entity, float yaw, float pitch) {
        return entity == mc.thePlayer && Freecam.applyCameraAngles(yaw, pitch);
    }

    public static boolean moveFlying(Entity entity, float strafe, float forward, float friction) {
        if (entity != mc.thePlayer) {
            return false;
        }
        MoveFlyingEvent event = EventBus.post(new MoveFlyingEvent(strafe, forward, friction, entity.rotationYaw));
        float moveStrafe = event.getStrafe();
        float moveForward = event.getForward();
        float speed = moveStrafe * moveStrafe + moveForward * moveForward;
        if (speed >= 1.0E-4F) {
            speed = event.getFriction() / Math.max(1.0F, MathHelper.sqrt_float(speed));
            moveStrafe *= speed;
            moveForward *= speed;
            float yaw = event.getYaw() * (float) Math.PI / 180.0F;
            float sin = MathHelper.sin(yaw);
            float cos = MathHelper.cos(yaw);
            entity.motionX += moveStrafe * cos - moveForward * sin;
            entity.motionZ += moveForward * cos + moveStrafe * sin;
        }
        return true;
    }

    public static boolean jump(EntityLivingBase entity) {
        if (entity != mc.thePlayer) {
            return false;
        }
        JumpEvent event = EventBus.post(
                new JumpEvent(entity, entity.getJumpUpwardsMotion(), entity.rotationYaw, entity.isSprinting()));
        if (event.cancelled()) {
            return true;
        }
        entity.motionY = event.getMotionY();
        if (entity.isPotionActive(Potion.jump)) {
            entity.motionY += (entity.getActivePotionEffect(Potion.jump).getAmplifier() + 1) * 0.1F;
        }
        if (event.isSprinting()) {
            float yaw = event.getYaw() * (float) Math.PI / 180.0F;
            entity.motionX -= MathHelper.sin(yaw) * 0.2F;
            entity.motionZ += MathHelper.cos(yaw) * 0.2F;
        }
        entity.isAirBorne = true;
        return true;
    }

    public static void preUpdate() {
        EventBus.post(new PreUpdateEvent());
    }

    public static void postUpdate(EntityPlayerSP player) {
        if (player.worldObj.isBlockLoaded(new BlockPos(player.posX, 0.0, player.posZ))) {
            EventBus.post(new PostUpdateEvent());
        }
    }

    public static void preMotion(EntityPlayerSP player) {
        RotationEngine.preMotion(player);
        walkingYaw = player.rotationYaw;
        walkingPitch = player.rotationPitch;
        walkingSneak = player.movementInput.sneak;
        boolean sneaking = player.isSneaking();
        UpdateWalkingPlayerEvent event = EventBus.post(new UpdateWalkingPlayerEvent(walkingYaw, walkingPitch, sneaking));
        player.rotationYaw = event.getYaw();
        player.rotationPitch = event.getPitch();
        if (event.isSneaking() != sneaking) {
            player.movementInput.sneak = event.isSneaking();
        }
        RotationUtils.lastSentRotation[0] = event.getYaw();
        RotationUtils.lastSentRotation[1] = event.getPitch();
    }

    public static void postMotion(EntityPlayerSP player) {
        player.rotationYaw = walkingYaw;
        player.rotationPitch = walkingPitch;
        player.movementInput.sneak = walkingSneak;
        EventBus.post(new PostWalkingUpdateEvent());
        RotationEngine.postMotion(player);
    }

    public static boolean motionViewEntity(EntityPlayerSP player) {
        return player.isCurrentViewEntity() || Freecam.cameraEntity != null;
    }

    public static boolean inputViewEntity(EntityPlayerSP player) {
        return player.isCurrentViewEntity() || Freecam.cameraEntity != null;
    }

    public static void livingUpdate(EntityPlayerSP player) {
        KeepSprint keepSprint = ModuleManager.get(KeepSprint.class);
        if (keepSprint.enabled()) {
            keepSprint.applyPredictedSprintState(player);
        }
    }

    public static void closeScreen() {
        ModuleManager.get(InventoryManager.class).recoverCursorOnScreenClose("EntityPlayerSP.closeScreen");
    }

    public static void moveInput(MovementInputFromOptions input) {
        MoveInputEvent event = EventBus.post(
                new MoveInputEvent(input.moveForward, input.moveStrafe, input.jump, input.sneak));
        input.moveForward = event.getMoveForward();
        input.moveStrafe = event.getMoveStrafe();
        input.jump = event.isJumping();
        input.sneak = event.isSneaking();
    }

    public static void moveInputDone() {
        EventBus.post(new MoveStateUpdateEvent());
    }

    public static boolean sendPacket(Packet<?> packet) {
        NukeDefend nukeDefend = ModuleManager.get(NukeDefend.class);
        if (packet instanceof C08PacketPlayerBlockPlacement && nukeDefend.MuoLy()) {
            PacketUtils.consumeSilentlySent(packet);
            return true;
        }
        if (!PacketUtils.consumeSilentlySent(packet) && EventBus.post(new PacketSendEvent(packet)).cancelled()) {
            return true;
        }
        if (packet instanceof C03PacketPlayer) {
            nukeDefend.clearPendingPlacement();
        }
        return false;
    }

    public static boolean receivePacket(ChannelHandlerContext context, Packet<?> packet) {
        return EventBus.post(new PacketReceiveEvent(packet)).cancelled();
    }

    public static boolean chat(S02PacketChat packet) {
        return mc.isCallingFromMinecraftThread()
                && EventBus.post(new ChatReceivedEvent(packet.getType(), packet.getChatComponent())).cancelled();
    }

    public static void disconnect() {
        EventBus.post(new DisconnectEvent());
    }

    public static boolean velocity(S12PacketEntityVelocity packet) {
        return mc.isCallingFromMinecraftThread() && EventBus.post(new VelocityEvent(packet)).cancelled();
    }

    public static void entityJoined(World world, Entity entity) {
        if (world.isRemote) {
            EventBus.post(new EntityJoinWorldEvent(entity, world));
        }
    }

    public static boolean playerAttack(EntityPlayer player, Entity target) {
        return EventBus.post(new PlayerAttackEvent(player, target)).cancelled();
    }

    public static void knockback(EntityPlayer player) {
        attackMotionX = player.motionX;
        attackMotionZ = player.motionZ;
    }

    public static void attackSprint(EntityPlayer player, boolean sprinting) {
        KeepSprint keepSprint = ModuleManager.get(KeepSprint.class);
        if (keepSprint.enabled() && player == mc.thePlayer) {
            player.motionX = attackMotionX;
            player.motionZ = attackMotionZ;
            keepSprint.applySprintSlowdown(player);
        } else {
            player.setSprinting(sprinting);
        }
    }

    public static void renderLiving(EntityLivingBase entity, double x, double y, double z, float yaw,
                                    float partialTicks) {
        if (!FakePlayerRenderer.isRendering() && EventBus.hasListeners(RenderLivingPreEvent.class)) {
            EventBus.post(new RenderLivingPreEvent(entity, x, y, z, partialTicks));
        }
    }

    public static boolean modelInvisible(EntityLivingBase entity) {
        return entity.isInvisible() && !ModuleManager.get(AntiInvis.class).isForceVisibleTarget(entity);
    }

    public static boolean canRenderName(EntityLivingBase entity) {
        return hidesVanillaName(entity);
    }

    public static boolean nameInvisible(EntityLivingBase entity, EntityPlayer viewer) {
        return entity.isInvisibleToPlayer(viewer) && !ModuleManager.get(AntiInvis.class).isForceVisibleTarget(entity);
    }

    public static boolean renderName(EntityLivingBase entity, double x, double y, double z) {
        return hidesVanillaName(entity);
    }

    private static boolean hidesVanillaName(EntityLivingBase entity) {
        return FakePlayerRenderer.isRendering() || entity instanceof EntityPlayer player
                && ModuleManager.get(Nametags.class).shouldHideVanillaNametag(player);
    }

    public static void renderPlayer(RenderPlayer renderer, AbstractClientPlayer player, double x, double y, double z,
                                    float yaw, float partialTicks) {
        if (!FakePlayerRenderer.isRendering() && EventBus.hasListeners(RenderPlayerPreEvent.class)) {
            EventBus.post(new RenderPlayerPreEvent(player, x, y, z, partialTicks, renderer.getMainModel()));
        }
    }

    public static void playerRendered(RenderPlayer renderer, AbstractClientPlayer player, double x, double y,
                                      double z, float yaw, float partialTicks) {
        if (!FakePlayerRenderer.isRendering() && EventBus.hasListeners(RenderPlayerPostEvent.class)) {
            EventBus.post(new RenderPlayerPostEvent(player, x, y, z, partialTicks, renderer.getMainModel()));
        }
    }

    public static void renderInfoCached(RenderManager manager) {
        if (FreeLook.DnH && ModuleManager.get(FreeLook.class).enabled()) {
            manager.playerViewX = FreeLook.savedPitch;
            manager.playerViewY = FreeLook.savedYaw;
        }
    }

    public static ResourceLocation capeTexture(NetworkPlayerInfo info) {
        if (mc.thePlayer != null && info.getGameProfile().getId().equals(mc.thePlayer.getUniqueID())) {
            ResourceLocation cape = Cape.texture();
            if (cape != null) {
                return cape;
            }
        }
        return info.getLocationCape();
    }

    public static void containerDrawn(GuiContainer gui) {
        ModuleManager.get(AutoClicker.class).setInventoryTarget(gui, gui.theSlot);
    }

    public static boolean containerClick() {
        return inventoryBusy();
    }

    public static boolean containerDrag() {
        return inventoryBusy();
    }

    public static boolean containerRelease() {
        return inventoryBusy();
    }

    public static boolean slotClick(Slot slot, int slotId, int button, int type) {
        return inventoryBusy()
                || ModuleManager.get(BedwarsUtils.class).handleQuickShopSlotClick(slot, slotId, button, type);
    }

    private static boolean inventoryBusy() {
        return ModuleManager.get(InventoryManager.class).isExecutingPlan();
    }

    public static void screenMouseInput() {
        int wheel = Mouse.getEventDWheel();
        AutoChest autoChest = ModuleManager.get(AutoChest.class);
        if (wheel != 0 && autoChest.enabled()) {
            autoChest.handleScrollTransfer(wheel);
        }
    }

    public static void pickStart() {
        RotationHandler rotations = RotationHandler.getInstance();
        Entity viewer = mc.getRenderViewEntity();
        if (rotations.renderRotationApplied || viewer == null || !rotations.abxpJn()) {
            return;
        }
        Float yaw = rotations.getTargetYaw();
        Float pitch = rotations.cvZx();
        if (yaw != null && pitch != null && !yaw.isNaN() && !pitch.isNaN()) {
            rotations.mNwrQ(viewer, yaw, pitch, true);
            rotations.renderRotationApplied = true;
        }
    }

    public static void pointedObject(float partialTicks) {
        for (PointedObjectOverrider overrider : List.<PointedObjectOverrider>of(ModuleManager.get(BedNuker.class),
                ModuleManager.get(BridgeNuker.class), ModuleManager.get(GhostHand.class),
                ModuleManager.get(Piercing.class))) {
            if (overrider.shouldOverridePointedObject()) {
                overrider.applyPointedObjectOverride(partialTicks);
                return;
            }
        }
    }

    public static void pickEnd() {
        RotationHandler rotations = RotationHandler.getInstance();
        if (rotations.renderRotationApplied) {
            Entity viewer = mc.getRenderViewEntity();
            if (viewer != null) {
                rotations.restoreEntityRotation(viewer);
            }
            rotations.renderRotationApplied = false;
        }
    }

    public static void cameraStart() {
        if (FreeLook.DnH && ModuleManager.get(FreeLook.class).enabled()) {
            freeLookRotation.applyRotation(mc.getRenderViewEntity(), FreeLook.savedYaw, FreeLook.savedPitch, true);
            freeLookRotated = true;
        }
    }

    public static void cameraEnd() {
        if (freeLookRotated) {
            freeLookRotation.oIpxn(mc.getRenderViewEntity());
            freeLookRotated = false;
        }
    }

    public static double cameraClip(Vec3 from, Vec3 to) {
        return ModuleManager.get(NoCameraClip.class).enabled() ? 4.0 : from.distanceTo(to);
    }

    public static boolean fogPotion(EntityLivingBase entity, Potion potion) {
        return !ModuleManager.get(AntiDebuff.class).blocksBlindness(potion) && entity.isPotionActive(potion);
    }

    public static boolean fogColorPotion(EntityLivingBase entity, Potion potion) {
        return !ModuleManager.get(AntiDebuff.class).blocksBlindness(potion) && entity.isPotionActive(potion);
    }

    public static boolean nauseaPotion(EntityPlayerSP player, Potion potion) {
        return !ModuleManager.get(AntiDebuff.class).blocksConfusion(potion) && player.isPotionActive(potion);
    }

    public static boolean mouseTurn(Minecraft minecraft) {
        FreeLook freeLook = ModuleManager.get(FreeLook.class);
        return minecraft.inGameHasFocus && (!freeLook.enabled() || freeLook.mouseTurnsPlayer());
    }

    public static void renderTickEnd(float partialTicks, long finishTime) {
        EventBus.post(new RenderTickEvent(Phase.END, partialTicks));
    }

    public static void renderWorldLast(int pass, float partialTicks, long finishTime) {
        if (EventBus.hasListeners(RenderWorldLastEvent.class)) {
            EventBus.post(new RenderWorldLastEvent(partialTicks));
        }
    }
}
