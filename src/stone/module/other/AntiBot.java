package stone.module.other;

import stone.event.LoadWorldEvent;
import stone.event.PacketReceiveEvent;
import stone.event.Subscribe;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.ModuleManager;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.module.player.Freecam;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S13PacketDestroyEntities;
import net.minecraft.network.play.server.S14PacketEntity;

public class AntiBot extends Module {
    private static Setting tabList;
    private static Setting moveCheck;
    private static final Set<Integer> DHoht = ConcurrentHashMap.newKeySet();

    public AntiBot() {
        super("AntiBot", "Filters out the server's fake players", Category.OTHER, Subcategory.BASIC);
        tabList = addBoolean("Tab list", "", false);
        moveCheck = addBoolean("Move Check", "", true);
        setEnabled(true, null);
    }

    @Override
    public void onEnable(Context context) {
        DHoht.clear();
    }

    @Override
    public void onDisable(Context context) {
        DHoht.clear();
    }

    @Subscribe
    public void onLoadWorld(LoadWorldEvent var1) {
        DHoht.clear();
    }

    @Subscribe
    public void onPacketReceive(PacketReceiveEvent var1) {
        Packet var2 = var1.getPacket();
        if (var2 instanceof S14PacketEntity) {
            S14PacketEntity var3 = (S14PacketEntity)var2;
            if (var3.func_149062_c() != 0 || var3.func_149061_d() != 0 || var3.func_149064_e() != 0) {
                DHoht.add(var3.entityId);
            }
        } else if (var2 instanceof S13PacketDestroyEntities) {
            for (int var6 : ((S13PacketDestroyEntities)var2).getEntityIDs()) {
                DHoht.remove(var6);
            }
        }
    }

    public static boolean shouldHideEntity(Entity var0) {
        if (!ModuleManager.get(AntiBot.class).enabled()) {
            return false;
        } else if (Freecam.cameraEntity != null && Freecam.cameraEntity == var0) {
            return true;
        } else if (var0 != null && var0 instanceof EntityPlayer) {
            EntityPlayer var1 = (EntityPlayer)var0;
            if (var1 == mc.thePlayer) {
                return false;
            } else if (moveCheck.bool() && !DHoht.contains(var1.getEntityId())) {
                return true;
            } else if (var1.isDead) {
                return true;
            } else if (var1.getName().isEmpty()) {
                return true;
            } else {
                if (tabList.bool() && !isListedInTab(var1)) {
                    return true;
                } else if (var1.getHealth() != 20.0F && var1.getName().startsWith("§c")) {
                    return true;
                } else {
                    if (var1.maxHurtTime == 0) {
                        if (var1.getHealth() == 20.0F) {
                            String var3 = var1.getDisplayName().getUnformattedText();
                            if (var3.length() == 10 && var3.charAt(0) != 167 && var1.ticksExisted >= 10 && !isListedInTab(var1)) {
                                return true;
                            }

                            if (var3.length() == 12 && var1.isPlayerSleeping() && var3.charAt(0) == 167) {
                                return true;
                            }

                            if (var3.length() >= 7 && var3.charAt(2) == '[' && var3.charAt(3) == 'N' && var3.charAt(6) == ']') {
                                return true;
                            }

                            if (var1.getName().contains(" ")) {
                                return true;
                            }
                        } else if (var1.isInvisible()) {
                            String var4 = var1.getDisplayName().getUnformattedText();
                            if (var4.length() >= 3 && var4.charAt(0) == 167 && var4.charAt(1) == 'c') {
                                return true;
                            }
                        }
                    }

                    return false;
                }
            }
        } else {
            return true;
        }
    }

    private static boolean isListedInTab(EntityPlayer var0) {
        if (mc.getNetHandler() == null) {
            return false;
        }
        NetworkPlayerInfo var1 = mc.getNetHandler().getPlayerInfo(var0.getUniqueID());
        return var1 != null && var1.getGameProfile() != null;
    }
}
