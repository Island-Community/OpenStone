package stone.module.minigames;

import stone.event.Phase;
import stone.event.RenderWorldLastEvent;
import stone.event.Subscribe;
import stone.event.TickEndEvent;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.module.minigames.murdermystery.MurderMysteryUtils;
import stone.module.minigames.murdermystery.MurderWeapons;
import stone.module.other.AntiBot;
import stone.util.ClientUtils;
import stone.util.render.RenderUtils;
import stone.util.item.ItemList;
import stone.util.item.ItemMatcher;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;

public class MurderMystery extends Module {
    private final ItemList murderWeapons;
    private final Setting alertMurderer;
    private final Setting alertBow;
    private final Setting highlightMurderer;
    private final Setting highlightBow;
    private final Setting highlightInnocent;
    private final Setting highlightDead;
    private final Setting goldEsp;
    private final List<EntityPlayer> Bzdo = new ArrayList<>();
    private final List<EntityPlayer> Ebh = new ArrayList<>();
    private final List<EntityItem> fuQceN = new ArrayList<>();
    private boolean inMurderMystery;

    public MurderMystery() {
        super("MurderMystery", "Finds the murderer and the bow in Murder Mystery", Category.MINIGAMES,
                Subcategory.ADVANCED);
        this.murderWeapons = new ItemList();

        for (Item var4 : MurderWeapons.getDefaultWeapons()) {
            this.GKbJ9(var4);
        }

        this.alertMurderer = addBoolean("Alert murderer", "", true);
        this.alertBow = addBoolean("Alert bow", "", true);
        this.highlightMurderer = addBoolean("Highlight murderer", "", true);
        this.highlightBow = addBoolean("Highlight bow", "", true);
        this.highlightInnocent = addBoolean("Highlight innocent", "", true);
        this.highlightDead = addBoolean("Highlight dead", "", true);
        this.goldEsp = addBoolean("Gold ESP", "", true);
    }

    @Override
    public void onDisable(Context context) {
        this.clearTrackedEntities();
    }

    @Subscribe
    public void onTickEnd(TickEndEvent var1) {
        if (var1.phase == Phase.END && ClientUtils.isInWorld()) {
            this.inMurderMystery = this.detectMurderMysteryGame();
            if (!this.inMurderMystery) {
                this.clearTrackedEntities();
            } else {
                this.scanPlayers();
            }
        }
    }

    @Subscribe(priority = Subscribe.Priority.HIGH)
    public void onRenderWorldLast(RenderWorldLastEvent var1) {
        if (ClientUtils.isInWorld() && this.inMurderMystery) {
            for (EntityPlayer var4 : mc.theWorld.playerEntities) {
                if (var4 != mc.thePlayer && !var4.isInvisible() && (!AntiBot.shouldHideEntity(var4) || this.highlightDead.bool())) {
                    int var5 = Color.green.getRGB();
                    if (this.Bzdo.contains(var4) && this.highlightMurderer.bool()) {
                        var5 = Color.red.getRGB();
                    } else if (this.Ebh.contains(var4) && this.highlightBow.bool()) {
                        var5 = Color.orange.getRGB();
                    } else if (!this.highlightInnocent.bool()) {
                        continue;
                    }

                    if (this.highlightDead.bool() || !(MurderMysteryUtils.BNAB(var4.getEntityBoundingBox()) <= 0.009)) {
                        RenderUtils.drawEntityBox(var4, var5, 2.0F, var1.partialTicks);
                    }
                }
            }

            if (this.goldEsp.bool()) {
                int var6 = -331703;

                for (EntityItem var8 : this.fuQceN) {
                    if (var8 != null && !var8.isDead && var8.getEntityItem() != null && var8.getEntityItem().stackSize != 0) {
                        RenderUtils.drawEntityBox(var8, var6, 2.0F, var1.partialTicks);
                    }
                }
            }
        }
    }

    private void scanPlayers() {
        this.fuQceN.clear();

        for (EntityPlayer var2 : mc.theWorld.playerEntities) {
            if (var2 != mc.thePlayer && !var2.isInvisible() && (!AntiBot.shouldHideEntity(var2) || this.highlightDead.bool())) {
                ItemStack var3 = var2.getHeldItem();
                if (var3 != null) {
                    Item var4 = var3.getItem();
                    if (this.murderWeapons.matches(var3)) {
                        if (!this.Bzdo.contains(var2)) {
                            this.Bzdo.add(var2);
                            if (this.alertMurderer.bool()) {
                                mc.thePlayer.playSound("note.pling", 1.0F, 1.0F);
                                ClientUtils.sendTitledMessage("Stone", "&b" + var2.getName() + " &7is the &cmurderer&7! (&f" + (int)mc.thePlayer.getDistanceToEntity(var2) + "m&7)");
                            }
                        }
                    } else if (var4 instanceof ItemBow) {
                        if (!this.Ebh.contains(var2)) {
                            this.Ebh.add(var2);
                            if (this.alertBow.bool()) {
                                mc.thePlayer.playSound("note.pling", 1.0F, 1.25F);
                                ClientUtils.sendTitledMessage("Stone", "&b" + var2.getName() + " &7has a &6bow&7! (&f" + (int)mc.thePlayer.getDistanceToEntity(var2) + "m&7)");
                            }
                        }
                    } else {
                        this.Ebh.remove(var2);
                    }
                } else {
                    this.Ebh.remove(var2);
                }
            }
        }

        if (this.goldEsp.bool()) {
            for (Entity var6 : mc.theWorld.loadedEntityList) {
                if (var6 instanceof EntityItem && var6.ticksExisted >= 3) {
                    EntityItem var7 = (EntityItem)var6;
                    ItemStack var8 = var7.getEntityItem();
                    if (var8 != null && var8.stackSize != 0 && var8.getItem() == Items.gold_ingot) {
                        this.fuQceN.add(var7);
                    }
                }
            }
        }
    }

    private boolean detectMurderMysteryGame() {
        if (ClientUtils.isOnHypixel() && mc.thePlayer.getWorldScoreboard() != null && mc.thePlayer.getWorldScoreboard().getObjectiveInDisplaySlot(1) != null) {
            ArrayList var1 = new ArrayList();

            for (String var3 : ClientUtils.getSidebarLines()) {
                var1.add(ClientUtils.AOAtn(var3));
            }

            return MurderMysteryUtils.isMurderMysteryGame(mc.thePlayer.getWorldScoreboard().getObjectiveInDisplaySlot(1).getDisplayName(), var1);
        } else {
            return false;
        }
    }

    private void clearTrackedEntities() {
        this.inMurderMystery = false;
        this.Bzdo.clear();
        this.Ebh.clear();
        this.fuQceN.clear();
    }

    private void GKbJ9(Item var1) {
        if (var1 != null) {
            String var2 = ItemMatcher.getItemId(var1);
            if (var2 != null) {
                this.murderWeapons.addItem(ItemMatcher.hasMultipleVariants(var1) ? var2 + ":*" : var2);
            }
        }
    }

}
