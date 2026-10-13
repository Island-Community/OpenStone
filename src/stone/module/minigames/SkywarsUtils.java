package stone.module.minigames;

import stone.core.Color;
import stone.event.ChangeCurrentItemEvent;
import stone.event.ChatReceivedEvent;
import stone.event.LoadWorldEvent;
import stone.event.Phase;
import stone.event.RenderWorldLastEvent;
import stone.event.SetCurrentItemEvent;
import stone.event.Subscribe;
import stone.event.TickEndEvent;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.util.ClientUtils;
import stone.util.SkywarsGameState;
import stone.util.render.RenderUtils;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.BlockPos;
import net.minecraft.util.DamageSource;

public class SkywarsUtils extends Module {
    private static final String[] KILL_MESSAGE_SEPARATORS = new String[]{
            " by ",
            " to ",
            " with ",
            " of ",
            " from ",
            " knight ",
            " for ",
            " on ",
            " league "
        };
    private final Setting strengthEsp;
    private final Setting strengthEspColor;
    private final Setting autoEcho;
    private final Setting autoEchoHealth;
    private final Map<String, Long> strengthPlayers = new HashMap<>();
    private int echoState;
    private int clockSlot = -1;
    private int previousSlot = -1;

    public SkywarsUtils() {
        super("SkywarsUtils", "Strength alerts and auto echo for SkyWars", Category.MINIGAMES, Subcategory.BASIC);
        this.strengthEsp = addBoolean("Strength ESP", "", false);
        this.strengthEspColor = addColour("Strength ESP Color", "", Color.rgb(0xFF0000));
        this.autoEcho = addBoolean("Auto Echo", "", false);
        this.autoEchoHealth = addNumber("Auto Echo Health", "", 4.0F, 1.0F, 20.0F, 1);
        this.strengthEspColor.setVisible(false);
        this.autoEchoHealth.setVisible(false);
    }

    @Override
    public void onEnable(Context context) {
        this.strengthPlayers.clear();
        this.vcoEz(true);
    }

    @Override
    public void onDisable(Context context) {
        this.strengthPlayers.clear();
        this.vcoEz(true);
    }

    @Override
    public void guiUpdate() {
        this.strengthEspColor.setVisible(this.strengthEsp.bool());
        this.autoEchoHealth.setVisible(this.autoEcho.bool());
    }

    @Subscribe
    public void onChatReceived(ChatReceivedEvent var1) {
        if (this.strengthEsp.bool() && SkywarsGameState.JYXs() && var1.messageType == 0 && var1.component != null) {
            String var2 = extractKilledPlayerName(ClientUtils.AOAtn(var1.component.getUnformattedText()));
            if (var2 != null) {
                this.strengthPlayers.put(var2, System.currentTimeMillis());
            }
        }
    }

    @Subscribe
    public void onLoadWorld(LoadWorldEvent var1) {
        this.strengthPlayers.clear();
        this.vcoEz(false);
    }

    @Subscribe
    public void onSetCurrentItem(SetCurrentItemEvent var1) {
        if (this.echoState != 0) {
            this.previousSlot = var1.slot;
            var1.setCancelled(true);
        }
    }

    @Subscribe
    public void onChangeCurrentItem(ChangeCurrentItemEvent var1) {
        if (this.echoState != 0) {
            int var2 = Integer.compare(var1.scrollDirection, 0);
            this.previousSlot = Math.floorMod(mc.thePlayer.inventory.currentItem - var2, 9);
            var1.setCancelled(true);
        }
    }

    @Subscribe
    public void onTickEnd(TickEndEvent var1) {
        if (var1.phase == Phase.END) {
            if (this.echoState != 0) {
                this.updateEchoState();
            } else if (this.autoEcho.bool() && this.shouldUseEchoClock()) {
                this.clockSlot = this.findClockSlot();
                if (this.clockSlot != -1) {
                    this.previousSlot = mc.thePlayer.inventory.currentItem;
                    this.bzMc(this.clockSlot);
                    this.echoState = 1;
                }
            }
        }
    }

    @Subscribe(priority = Subscribe.Priority.HIGH)
    public void onRenderWorldLast(RenderWorldLastEvent var1) {
        if (this.strengthEsp.bool() && SkywarsGameState.JYXs() && ClientUtils.isInWorld()) {
            long var3 = System.currentTimeMillis();

            for (EntityPlayer var6 : mc.theWorld.playerEntities) {
                if (var6 != mc.thePlayer && this.pkR7(var6, var3)) {
                    RenderUtils.drawEntityBox(var6, this.strengthEspColor.colour().argb(), 2.0F, var1.partialTicks);
                }
            }
        }
    }

    public boolean isStrengthHighlighted(EntityLivingBase var1) {
        return this.enabled() && this.strengthEsp.bool() && SkywarsGameState.JYXs() && var1 instanceof EntityPlayer && this.pkR7((EntityPlayer)var1, System.currentTimeMillis());
    }

    private boolean pkR7(EntityPlayer var1, long var2) {
        Long var4 = this.strengthPlayers.get(var1.getName());
        if (var4 == null) {
            return false;
        } else if (var2 - var4 > 5000L) {
            this.strengthPlayers.remove(var1.getName());
            return false;
        } else {
            return true;
        }
    }

    private boolean shouldUseEchoClock() {
        if (ClientUtils.isInWorld() && mc.currentScreen == null && mc.inGameHasFocus && !mc.thePlayer.isDead) {
            long var1 = System.currentTimeMillis();
            if (!SkywarsGameState.isTimerElapsed(var1)) {
                return false;
            } else {
                int var3 = this.findGroundY();
                if (var3 == Integer.MIN_VALUE) {
                    return false;
                } else {
                    boolean var4 = mc.thePlayer.getHealth() < this.autoEchoHealth.number() && this.isEnemyNearbyWithMoreHealth();
                    return var4 || this.JWGpwHi(var3, 8.0F);
                }
            }
        } else {
            return false;
        }
    }

    private boolean isEnemyNearbyWithMoreHealth() {
        float var1 = mc.thePlayer.getHealth() + 2.0F;
        double var2 = 25.0;

        for (EntityPlayer var5 : mc.theWorld.playerEntities) {
            if (var5 != mc.thePlayer
                && !var5.isDead
                && !(var5.getHealth() <= 0.0F)
                && !mc.thePlayer.isOnSameTeam(var5)
                && mc.thePlayer.getDistanceSqToEntity(var5) <= var2
                && var5.getHealth() >= var1) {
                return true;
            }
        }

        return false;
    }

    private boolean JWGpwHi(int var1, float var2) {
        if (!mc.thePlayer.onGround && !(mc.thePlayer.motionY >= 0.0)) {
            Block var3 = mc.theWorld.getBlockState(new BlockPos((int)Math.floor(mc.thePlayer.posX), var1, (int)Math.floor(mc.thePlayer.posZ))).getBlock();
            if (var3 != Blocks.water
                && var3 != Blocks.flowing_water
                && var3 != Blocks.web
                && var3 != Blocks.ladder
                && var3 != Blocks.vine
                && (var3 != Blocks.slime_block || mc.thePlayer.isSneaking())) {
                double var4 = Math.max(0.0, mc.thePlayer.posY - (var1 + 1.0));
                float var6 = mc.thePlayer.fallDistance + (float)var4;
                PotionEffect var7 = mc.thePlayer.getActivePotionEffect(Potion.jump);
                float var8 = 3.0F + (var7 == null ? 0.0F : var7.getAmplifier() + 1.0F);
                float var9 = (float)Math.ceil(var6 - var8);
                if (var9 <= 0.0F) {
                    return false;
                } else {
                    int var10 = EnchantmentHelper.getEnchantmentModifierDamage(mc.thePlayer.inventory.armorInventory, DamageSource.fall);
                    var10 = Math.min(20, Math.max(0, var10));
                    var9 *= (25.0F - var10) / 25.0F;
                    if (var3 == Blocks.hay_block) {
                        var9 *= 0.2F;
                    }

                    float var11 = mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount() - var9;
                    return var11 < var2;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private int findGroundY() {
        int var1 = (int)Math.floor(mc.thePlayer.posX);
        int var2 = (int)Math.floor(mc.thePlayer.posZ);
        int var3 = Math.min(255, (int)Math.floor(mc.thePlayer.getEntityBoundingBox().minY - 0.001));

        for (int var4 = var3; var4 >= 0; var4--) {
            Block var5 = mc.theWorld.getBlockState(new BlockPos(var1, var4, var2)).getBlock();
            if (var5 != null && var5 != Blocks.air) {
                return var4;
            }
        }

        return Integer.MIN_VALUE;
    }

    private int findClockSlot() {
        for (int var1 = 0; var1 < 9; var1++) {
            ItemStack var2 = mc.thePlayer.inventory.getStackInSlot(var1);
            if (var2 != null && var2.getItem() == Items.clock) {
                return var1;
            }
        }

        return -1;
    }

    private void updateEchoState() {
        if (!ClientUtils.isInWorld() || mc.currentScreen != null || mc.thePlayer.isDead) {
            this.vcoEz(true);
        } else if (this.echoState == 1) {
            ItemStack var1 = mc.thePlayer.getHeldItem();
            if (mc.thePlayer.inventory.currentItem == this.clockSlot && var1 != null && var1.getItem() == Items.clock) {
                mc.rightClickMouse();
                SkywarsGameState.PqEeqyh(System.currentTimeMillis());
                this.echoState = 2;
            } else {
                this.vcoEz(true);
            }
        } else {
            this.vcoEz(true);
        }
    }

    private void vcoEz(boolean var1) {
        if (var1 && ClientUtils.isInWorld() && this.previousSlot >= 0 && this.previousSlot < 9) {
            this.bzMc(this.previousSlot);
        }

        this.echoState = 0;
        this.clockSlot = -1;
        this.previousSlot = -1;
    }

    private void bzMc(int var1) {
        if (var1 >= 0 && var1 <= 8 && mc.thePlayer.inventory.currentItem != var1) {
            mc.thePlayer.inventory.currentItem = var1;
            mc.playerController.syncCurrentPlayItem();
        }
    }

    private static String extractKilledPlayerName(String var0) {
        if (var0 != null && var0.endsWith(".")) {
            for (String var4 : KILL_MESSAGE_SEPARATORS) {
                int var5 = var0.lastIndexOf(var4);
                if (var5 != -1) {
                    String var6 = var0.substring(var5 + var4.length(), var0.length() - 1).trim();
                    return var6.matches("[A-Za-z0-9_]{1,16}") ? var6 : null;
                }
            }

            return null;
        } else {
            return null;
        }
    }
}
