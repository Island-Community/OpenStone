package stone.module.player;

import stone.event.EntityJoinWorldEvent;
import stone.event.MoveInputEvent;
import stone.event.PrePlayerInteractEvent;
import stone.event.RotationEvent;
import stone.event.Subscribe;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.ModuleManager;
import stone.module.Subcategory;
import stone.module.player.autodeflect.AutoDeflectSettings;
import stone.module.player.autodeflect.ClickScheduler;
import stone.module.player.autodeflect.DeflectConditions;
import stone.module.player.autodeflect.FireballAim;
import stone.module.player.autodeflect.FireballFinder;
import stone.util.ClientUtils;
import stone.util.InputHookManager;
import stone.util.rotation.RotationUtils;
import java.util.HashSet;
import java.util.Random;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class AutoDeflect extends Module {
    private final AutoDeflectSettings autoDeflectSettings;
    public EntityFireball entityFireball;
    private final HashSet<Entity> entitys = new HashSet<>();
    private final ClickScheduler TGLfo = new ClickScheduler();
    private final Random random = new Random();

    public AutoDeflect() {
        super("AutoDeflect", "Hits incoming fireballs back", Category.PLAYER, Subcategory.ADVANCED);
        this.autoDeflectSettings = new AutoDeflectSettings(this);
    }

    @Override
    public void onDisable(Context context) {
        this.entitys.clear();
        this.entityFireball = null;
        this.TGLfo.resetSchedule();
    }

    @Override
    public void onUpdate() {
        if (ClientUtils.isInWorld() && mc.currentScreen == null) {
            this.entityFireball = this.BrlC();
        } else {
            this.entityFireball = null;
        }
    }

    @Subscribe
    public void onMoveInput(MoveInputEvent var1) {
        if (ClientUtils.isInWorld()) {
            if (DeflectConditions.shouldHoldSneak(this.entityFireball != null, this.autoDeflectSettings.sneak.bool(), mc.thePlayer.isRiding(), mc.thePlayer.capabilities.isFlying)) {
                var1.setSneaking(true);
            }
        }
    }

    @Subscribe
    public void onRotation(RotationEvent var1) {
        if (ClientUtils.isInWorld() && mc.currentScreen == null) {
            if (ModuleManager.get(BedNuker.class) == null || !ModuleManager.get(BedNuker.class).shouldOverridePointedObject()) {
                if (ModuleManager.get(BridgeNuker.class) == null || !ModuleManager.get(BridgeNuker.class).shouldOverridePointedObject()) {
                    if (DeflectConditions.canAimAtFireball(true, true, this.autoDeflectSettings.onGround.bool(), mc.thePlayer.onGround, this.entityFireball != null)) {
                        float var2 = var1.yaw != null ? var1.yaw : RotationUtils.lastSentRotation[0];
                        float var3 = var1.pitch != null ? var1.pitch : RotationUtils.lastSentRotation[1];
                        float[] var4 = FireballAim.computeAimAngles(mc, this.entityFireball, var2, var3);
                        if (var4 != null) {
                            float[] var5 = RotationUtils.smoothAngles(var2, var3, var4[0], var4[1], (int)this.autoDeflectSettings.aimSpeed.number());
                            var1.setRotation(var5[0], var5[1], 45);
                        }
                    }
                }
            }
        }
    }

    @Subscribe
    public void onPrePlayerInteract(PrePlayerInteractEvent var1) {
        if (ClientUtils.isInWorld() && mc.currentScreen == null) {
            if (DeflectConditions.canAimAtFireball(true, true, this.autoDeflectSettings.onGround.bool(), mc.thePlayer.onGround, this.entityFireball != null)) {
                MovingObjectPosition var2 = mc.objectMouseOver;
                if (var2 != null && var2.typeOfHit == MovingObjectType.ENTITY && var2.entityHit == this.entityFireball) {
                    long var3 = System.currentTimeMillis();
                    int var5 = mc.gameSettings.keyBindAttack.getKeyCode();
                    int var6 = this.TGLfo.consumeDueClicks(var3, this::nextClickDelayMillis);

                    for (int var7 = 0; var7 < var6; var7++) {
                        KeyBinding.setKeyBindState(var5, true);
                        KeyBinding.onTick(var5);
                        InputHookManager.simulateMouseButton(0, true);
                    }
                } else {
                    this.TGLfo.resetSchedule();
                    InputHookManager.simulateMouseButton(0, false);
                }
            }
        }
    }

    private EntityFireball BrlC() {
        return FireballFinder.findClosestFireball(mc, this.entitys, this.autoDeflectSettings.range.number(), (float)this.autoDeflectSettings.fov.number(), this.autoDeflectSettings.onGround.bool());
    }

    @Subscribe
    public void onEntityJoinWorld(EntityJoinWorldEvent var1) {
        if (ClientUtils.isInWorld()) {
            if (var1.entity == mc.thePlayer) {
                this.entitys.clear();
            } else if (DeflectConditions.isTrackableEntity(false, var1.entity instanceof EntityFireball, mc.thePlayer.getDistanceSqToEntity(var1.entity))) {
                this.entitys.add(var1.entity);
            }
        }
    }

    private long nextClickDelayMillis() {
        double var1 = this.autoDeflectSettings.cps.number();
        return ClickScheduler.getDelayMillis(var1, this.random.nextInt(ClickScheduler.getJitterBound(var1)));
    }

}
