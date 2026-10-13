package stone.module.combat;

import stone.event.MoveStateUpdateEvent;
import stone.event.PacketSendEvent;
import stone.event.RotationEvent;
import stone.event.Subscribe;
import stone.event.TickStartEvent;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.module.shared.DisabledOrNestedCondition;
import stone.module.shared.TargetFinder;
import stone.util.ClientUtils;
import stone.util.item.ItemList;
import stone.util.packet.PacketDirection;
import stone.util.packet.PacketListenerRegistration;
import stone.util.packet.QueuedPacketDispatcher;
import stone.util.rotation.RotationUtils;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.BlockPos;
import org.lwjgl.input.Mouse;

public class Fisherman extends Module {
    private final Setting delay;
    private final Setting blink;
    private final Setting hasKnockback;
    private final Setting itemWhitelist;
    private final ItemList whitelistedItems;
    private final Setting disableOnBreak;
    private boolean attackPhaseToggle = false;
    private boolean hasValidTarget = false;
    private boolean knockbackItemHeld = false;
    private boolean ULFM = false;
    private boolean blinkActive = false;
    private int YNS;
    private final Map<Integer, Integer> lastAttackTicks = new HashMap<>();
    private PacketListenerRegistration blinkPacketListener;

    public Fisherman() {
        super("Fisherman", "Item conditions", Category.COMBAT, Subcategory.ADVANCED);
        this.delay = addNumber("Delay", "", 0.0F, 0.0F, 500.0F, 0);
        this.blink = addBoolean("Blink", "", false);
        this.hasKnockback = addBoolean("Has knockback", "", false);
        this.itemWhitelist = addBoolean("Item whitelist", "", false);
        this.whitelistedItems = new ItemList();
        this.disableOnBreak = addBoolean("Disable on break", "", false);
    }

    @Override
    public void guiUpdate() {
    }

    @Override
    public String suffix() {
        return (int)Math.round(this.delay.number()) + "ms";
    }

    @Override
    public void onEnable(Context context) {
        this.attackPhaseToggle = false;
        this.hasValidTarget = false;
        this.knockbackItemHeld = false;
        this.ULFM = false;
        this.blinkActive = false;
        this.YNS = 0;
        this.lastAttackTicks.clear();
        this.releaseBlinkQueue();
    }

    @Override
    public void onDisable(Context context) {
        this.hasValidTarget = false;
        this.ULFM = false;
        this.blinkActive = false;
        this.lastAttackTicks.clear();
        this.releaseBlinkQueue();
    }

    private static int DWnTbl(double var0) {
        return var0 <= 0.0 ? 0 : (int)Math.ceil(var0 / 50.0);
    }

    private boolean isMovementKeyHeld() {
        return mc.gameSettings.keyBindForward.isKeyDown()
            || mc.gameSettings.keyBindBack.isKeyDown()
            || mc.gameSettings.keyBindLeft.isKeyDown()
            || mc.gameSettings.keyBindRight.isKeyDown();
    }

    private void lUiyxcs() {
        if (mc.theWorld == null) {
            this.lastAttackTicks.clear();
        } else {
            Iterator var1 = this.lastAttackTicks.entrySet().iterator();

            while (var1.hasNext()) {
                Entry var2 = (Entry)var1.next();
                Entity var3 = mc.theWorld.getEntityByID((Integer)var2.getKey());
                if (!(var3 instanceof EntityPlayer) || var3.isDead || ((EntityPlayer)var3).deathTime != 0) {
                    var1.remove();
                }
            }
        }
    }

    private boolean FubFo(EntityPlayer var1, int var2) {
        int var3 = var1.getEntityId();
        Integer var4 = this.lastAttackTicks.get(var3);
        if (var4 != null && var2 - var4 < 10) {
            int var5 = DWnTbl(this.delay.number());
            if (var5 <= 0) {
                return true;
            } else {
                int var6 = var2 - var4;
                return var6 >= var5;
            }
        } else {
            this.lastAttackTicks.put(var3, var2);
            return true;
        }
    }

    private void releaseBlinkQueue() {
        if (this.blinkPacketListener != null) {
            this.blinkPacketListener.getPacketHandler().forceOpen();
            this.blinkPacketListener = null;
        }
    }

    @Subscribe(priority = Subscribe.Priority.HIGHEST)
    public void onTickStart(TickStartEvent var1) {
        if (this.blinkActive) {
            this.releaseBlinkQueue();
            this.blinkActive = false;
        }
    }

    @Subscribe(priority = Subscribe.Priority.LOWEST)
    public void onMoveStateUpdate(MoveStateUpdateEvent var1) {
        if (this.hasValidTarget && this.attackPhaseToggle && !this.knockbackItemHeld) {
            if (this.isMovementKeyHeld()) {
                mc.thePlayer.movementInput.moveForward = 1.0F;
            }
        }
    }

    @Subscribe(priority = Subscribe.Priority.HIGH)
    public void onPacketSend(PacketSendEvent var1) {
        if (this.blink.bool() && this.hasValidTarget && this.attackPhaseToggle && !this.blinkActive) {
            if (var1.getPacket() instanceof C03PacketPlayer) {
                if (this.blinkPacketListener == null) {
                    this.blinkPacketListener = new PacketListenerRegistration(PacketDirection.ONLY_OUTBOUND, new DisabledOrNestedCondition(this));
                    QueuedPacketDispatcher.INSTANCE.JUlwlNu(this.blinkPacketListener);
                    this.blinkActive = true;
                }
            }
        }
    }

    @Subscribe(priority = Subscribe.Priority.LOWEST)
    public void onRotation(RotationEvent var1) {
        if (!ClientUtils.isInWorld()) {
            this.hasValidTarget = false;
            this.ULFM = false;
        } else {
            if (this.disableOnBreak.bool() && Mouse.isButtonDown(0) && mc.objectMouseOver != null) {
                BlockPos var2 = mc.objectMouseOver.getBlockPos();
                if (var2 != null) {
                    Block var3 = mc.theWorld.getBlockState(var2).getBlock();
                    if (var3 != Blocks.air && !(var3 instanceof BlockLiquid)) {
                        this.hasValidTarget = false;
                        this.attackPhaseToggle = false;
                        this.ULFM = false;
                        return;
                    }
                }
            }

            this.YNS++;
            this.lUiyxcs();
            if (this.hasKnockback.bool() || this.itemWhitelist.bool()) {
                boolean var5 = !this.hasKnockback.bool() || EnchantmentHelper.getKnockbackModifier(mc.thePlayer) > 0;
                boolean var7 = !this.itemWhitelist.bool() || this.whitelistedItems.matches(mc.thePlayer.getHeldItem());
                if (!var5 && !var7) {
                    this.hasValidTarget = false;
                    this.attackPhaseToggle = false;
                    this.ULFM = false;
                    return;
                }
            }

            EntityPlayer var6 = null;
            if (Mouse.isButtonDown(0)) {
                var6 = TargetFinder.findNearestTarget(9.0);
            }

            boolean var8 = EnchantmentHelper.getKnockbackModifier(mc.thePlayer) > 0;
            this.hasValidTarget = var6 != null && (var8 || this.isMovementKeyHeld());
            if (!this.hasValidTarget) {
                this.attackPhaseToggle = false;
                this.ULFM = false;
            } else {
                this.knockbackItemHeld = var8;
                this.attackPhaseToggle = !this.attackPhaseToggle;
                if (this.attackPhaseToggle && !this.FubFo(var6, this.YNS)) {
                    this.attackPhaseToggle = false;
                    this.ULFM = false;
                } else {
                    if (!this.attackPhaseToggle && this.ULFM) {
                        int var4 = mc.gameSettings.keyBindAttack.getKeyCode();
                        if (var4 != 0) {
                            KeyBinding.onTick(var4);
                        }
                    }

                    this.ULFM = this.attackPhaseToggle;
                    if (this.attackPhaseToggle) {
                        var1.setYaw(RotationUtils.lastSentRotation[0] + 180.0F, 30);
                    }
                }
            }
        }
    }

}
