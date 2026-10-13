package stone.module.render;

import stone.event.Phase;
import stone.event.RenderWorldLastEvent;
import stone.event.Subscribe;
import stone.event.TickEndEvent;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.ModuleManager;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.module.minigames.BedwarsUtils;
import stone.module.player.Freecam;
import stone.module.render.itemesp.ItemEspMode;
import stone.util.ClientUtils;
import stone.util.font.FontManager;
import stone.util.font.IFont;
import stone.util.item.ItemColorList;
import stone.util.item.ItemMatcher;
import stone.util.render.RenderUtils;
import stone.util.render.RenderUtils$1;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

public class ItemESP extends Module {
    private final Setting mode;
    private final ItemColorList items;
    private final Setting outline;
    private final Setting opacity;
    private final Setting scale;
    private final Setting maxDistance;
    private final ArrayList<ItemESP$2> RBWpjH = new ArrayList<>();
    private final Map<String, Integer> Ceor = new LinkedHashMap<>();
    private final Map<String, ItemESP$3> bfefrB = new LinkedHashMap<>();
    private int activeEntryCount = 0;

    public ItemESP() {
        super("ItemESP", "Highlights dropped items", Category.VISUALS, Subcategory.ADVANCED);
        this.mode = addChoice("Mode", "", List.of(ItemEspMode.labels()), ItemEspMode.BOX.ordinal());
        this.items = new ItemColorList();
        this.outline = addBoolean("Outline", "", true);
        this.opacity = addNumber("Opacity", "", 12.0F, 0.0F, 100.0F, 0);
        this.scale = addNumber("Scale", "", 1.0F, 0.5F, 2.0F, 2);
        this.maxDistance = addNumber("Max distance", "", 128.0F, 32.0F, 256.0F, 0);
        this.addDefaultItems();
    }

    @Override
    public void guiUpdate() {
        this.outline.setVisible(this.getItemEspMode() == ItemEspMode.BOX);
    }

    public boolean isNametagMode() {
        return this.getItemEspMode() == ItemEspMode.NAMETAG;
    }

    @Override
    public void onDisable(Context context) {
        this.clearTrackedItems();
    }

    @Subscribe
    public void onTickEnd(TickEndEvent var1) {
        if (var1.phase == Phase.END) {
            this.updateTrackedItems();
        }
    }

    @Subscribe
    public void onRenderWorldLast(RenderWorldLastEvent var1) {
        if (ClientUtils.isInWorld() && (this.activeEntryCount != 0 || !this.bfefrB.isEmpty())) {
            float var2 = var1.partialTicks;
            Object var3 = Freecam.cameraEntity == null ? mc.thePlayer : Freecam.cameraEntity;
            if (var3 != null) {
                if (this.getItemEspMode() != ItemEspMode.NAMETAG) {
                    for (int var22 = 0; var22 < this.activeEntryCount; var22++) {
                        ItemESP$2 var23 = this.RBWpjH.get(var22);
                        EntityItem var6 = ItemESP$2.getEntityItem(var23);
                        if (var6 != null && !var6.isDead && var6.getEntityItem() != null && var6.getEntityItem().stackSize != 0 && RenderUtils.isEntityInView(var6)) {
                            Integer var7 = this.Ceor.get(ItemESP$2.getItemKey(var23));
                            if (var7 != null) {
                                double var8 = var6.lastTickPosX + (var6.posX - var6.lastTickPosX) * var2;
                                double var10 = var6.lastTickPosY + (var6.posY - var6.lastTickPosY) * var2;
                                double var12 = var6.lastTickPosZ + (var6.posZ - var6.lastTickPosZ) * var2;
                                double var14 = ((EntityPlayer)var3).lastTickPosX + (((EntityPlayer)var3).posX - ((EntityPlayer)var3).lastTickPosX) * var2 - var8;
                                double var16 = ((EntityPlayer)var3).lastTickPosY + (((EntityPlayer)var3).posY - ((EntityPlayer)var3).lastTickPosY) * var2 - var10;
                                double var18 = ((EntityPlayer)var3).lastTickPosZ + (((EntityPlayer)var3).posZ - ((EntityPlayer)var3).lastTickPosZ) * var2 - var12;
                                double var20 = MathHelper.sqrt_double(var14 * var14 + var16 * var16 + var18 * var18);
                                this.renderWorldItemEsp(ItemESP$1.PinDj(ItemESP$2.ZEth(var23)), var7, var8, var10, var12, var20);
                            }
                        }
                    }
                } else {
                    for (ItemESP$3 var5 : this.bfefrB.values()) {
                        this.renderNametagPanel(var5, var2, (EntityPlayer)var3);
                    }
                }
            }

        }
    }

    private void updateTrackedItems() {
        this.clearTrackedItems();
        if (ClientUtils.isInWorld() && mc.theWorld != null && !this.items.getItems().isEmpty()) {
            double var1 = this.maxDistance.number() * this.maxDistance.number();
            boolean var3 = this.getItemEspMode() == ItemEspMode.NAMETAG;

            for (Entity var5 : mc.theWorld.loadedEntityList) {
                if (var5 instanceof EntityItem && RenderUtils.HVp0(var5, var1) && var5.ticksExisted >= 3) {
                    EntityItem var6 = (EntityItem)var5;
                    ItemStack var7 = var6.getEntityItem();
                    if (var7 != null && var7.stackSize != 0 && var7.getItem() != null) {
                        String var8 = this.findMatchingItemName(var7);
                        if (var8 != null) {
                            String var9 = ItemMatcher.getStackItemId(var7);
                            String var10 = var7.getDisplayName();
                            String var11 = var9 != null ? var9 : var8;
                            ItemESP$1 var12 = this.createItemColors(var11, var7);
                            String var13 = null;
                            if (var3) {
                                BedwarsUtils var14 = ModuleManager.get(BedwarsUtils.class);
                                var13 = var14.getItemNametagText(var6);
                            }

                            String var18 = this.getStackGroupKey(var6);
                            if (var13 != null) {
                                var18 = var18 + "|generator";
                            }

                            String var15 = var18 + "|" + var11;
                            Integer var16 = this.Ceor.get(var15);
                            this.Ceor.put(var15, (var16 == null ? 0 : var16) + var7.stackSize);
                            if (this.activeEntryCount >= this.RBWpjH.size()) {
                                this.RBWpjH.add(new ItemESP$2());
                            }

                            ItemESP$2.populateEntry(this.RBWpjH.get(this.activeEntryCount++), var6, var12, var15);
                            if (var3) {
                                ItemESP$3 var17 = this.bfefrB.get(var18);
                                if (var17 == null) {
                                    var17 = new ItemESP$3(var6);
                                    this.bfefrB.put(var18, var17);
                                }

                                ItemESP$3.addEntry(var17, var11, var10, var7.stackSize, var12, var13);
                            }
                        }
                    }
                }
            }
        }
    }

    private ItemEspMode getItemEspMode() {
        return ItemEspMode.fromSetting(this.mode.choice());
    }

    private void clearTrackedItems() {
        this.activeEntryCount = 0;
        this.Ceor.clear();
        this.bfefrB.clear();
    }

    @Override
    public String suffix() {
        return this.getItemEspMode().getLabel();
    }

    private String findMatchingItemName(ItemStack var1) {
        String var2 = ItemMatcher.getStackItemId(var1);
        String var3 = var2 != null ? ItemMatcher.toCanonicalItemId(var2) : null;
        String var4 = null;

        for (String var6 : this.items.getItems()) {
            if (ItemMatcher.matches(var6, var1)) {
                if (var6.equals(var2) || var6.equals(var3)) {
                    return var6;
                }

                if (var4 == null) {
                    var4 = var6;
                }
            }
        }

        return var4;
    }

    private ItemESP$1 createItemColors(String var1, ItemStack var2) {
        String var3 = var1;
        if (!this.items.getItems().contains(var1)) {
            String var4 = this.findMatchingItemName(var2);
            if (var4 != null) {
                var3 = var4;
            }
        }

        return ItemESP$1.ofOpaqueRgb(this.items.getPrimaryColor(var3), this.items.getSecondaryColor(var3));
    }

    private String getStackGroupKey(EntityItem var1) {
        int var2 = MathHelper.floor_double(var1.posX / 2.0);
        int var3 = MathHelper.floor_double(var1.posY / 1.5);
        int var4 = MathHelper.floor_double(var1.posZ / 2.0);
        return var2 + ":" + var3 + ":" + var4;
    }

    private void renderWorldItemEsp(int var1, int var2, double var3, double var5, double var7, double var9) {
        double var11 = var3 - mc.getRenderManager().viewerPosX;
        double var13 = var5 - mc.getRenderManager().viewerPosY;
        double var15 = var7 - mc.getRenderManager().viewerPosZ;
        float var17 = Math.min(Math.max(0.2F, (float)(0.01 * var9)), 0.4F);
        AxisAlignedBB var18 = new AxisAlignedBB(var11 - var17, var13, var15 - var17, var11 + var17, var13 + var17 * 2.0F, var15 + var17);
        this.drawItemBox(var18, withAlpha(var1, 180), var1, this.getBackgroundAlpha());
        this.drawStackCountText(var2, var11, var13, var15, var17, var9, var1);
    }

    private void drawItemBox(AxisAlignedBB var1, int var2, int var3, int var4) {
        float var5 = (var2 >> 24 & 0xFF) / 255.0F;
        float var6 = (var2 >> 16 & 0xFF) / 255.0F;
        float var7 = (var2 >> 8 & 0xFF) / 255.0F;
        float var8 = (var2 & 0xFF) / 255.0F;
        float var9 = (var3 >> 16 & 0xFF) / 255.0F;
        float var10 = (var3 >> 8 & 0xFF) / 255.0F;
        float var11 = (var3 & 0xFF) / 255.0F;
        float var12 = var4 / 255.0F;
        GL11.glPushMatrix();
        GL11.glPushAttrib(1048575);
        RenderUtils$1 var13 = null;

        try {
            var13 = RenderUtils.uyB6();
            GL11.glBlendFunc(770, 771);
            GL11.glEnable(3042);
            GL11.glDisable(3553);
            GL11.glDisable(2929);
            GL11.glDepthMask(false);
            GL11.glEnable(2848);
            GL11.glLineWidth(2.0F);
            if (var4 > 0) {
                RenderUtils.drawFilledAabb(var1, var9, var10, var11, var12);
            }

            if (this.outline.bool()) {
                GL11.glColor4f(var6, var7, var8, var5);
                RenderUtils.drawBoxOutline(var1);
            }
        } finally {
            RenderUtils.restoreLightmapState(var13);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glLineWidth(1.0F);
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }

    private static int withAlpha(int var0, int var1) {
        return MathHelper.clamp_int(var1, 0, 255) << 24 | var0 & 16777215;
    }

    private void drawStackCountText(int var1, double var2, double var4, double var6, float var8, double var9, int var11) {
        GlStateManager.pushMatrix();
        RenderUtils$1 var12 = null;

        try {
            var12 = RenderUtils.uyB6();
            GlStateManager.translate((float)var2, (float)var4 + var8, (float)var6);
            GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
            float var13 = mc.gameSettings.thirdPersonView == 2 ? -1.0F : 1.0F;
            GlStateManager.rotate(mc.getRenderManager().playerViewX, var13, 0.0F, 0.0F);
            float var14 = Math.min(Math.max(0.02266667F, (float)(0.0015 * var9)), 0.07F) * this.jWsed();
            GlStateManager.scale(-var14, -var14, 1.0F);
            GlStateManager.disableDepth();
            String var15 = String.valueOf(var1);
            IFont var16 = this.getNametagFont();
            var16.drawString(var15, -var16.getStringWidth(var15) / 2.0F, -var16.getFontHeight() / 2.0F, var11, true);
            GlStateManager.enableDepth();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        } finally {
            RenderUtils.restoreLightmapState(var12);
            GlStateManager.popMatrix();
        }
    }

    private void renderNametagPanel(ItemESP$3 var1, float var2, EntityPlayer var3) {
        EntityItem var4 = ItemESP$3.getDroppedItem(var1);
        if (var4 != null && !var4.isDead && !ItemESP$3.getEntriesByName(var1).isEmpty()) {
            double var5 = var4.lastTickPosX + (var4.posX - var4.lastTickPosX) * var2;
            double var7 = var4.lastTickPosY + (var4.posY - var4.lastTickPosY) * var2;
            double var9 = var4.lastTickPosZ + (var4.posZ - var4.lastTickPosZ) * var2;
            double var11 = var3.lastTickPosX + (var3.posX - var3.lastTickPosX) * var2 - var5;
            double var13 = var3.lastTickPosY + (var3.posY - var3.lastTickPosY) * var2 - var7;
            double var15 = var3.lastTickPosZ + (var3.posZ - var3.lastTickPosZ) * var2 - var9;
            double var17 = MathHelper.sqrt_double(var11 * var11 + var13 * var13 + var15 * var15);
            double var19 = var5 - mc.getRenderManager().viewerPosX;
            double var21 = var7 - mc.getRenderManager().viewerPosY;
            double var23 = var9 - mc.getRenderManager().viewerPosZ;
            IFont var25 = this.getNametagFont();
            ArrayList var26 = new ArrayList(ItemESP$3.getEntriesByName(var1).values());
            int var27 = 0;

            for (ItemESP$4 var29 : (java.lang.Iterable<ItemESP$4>) (java.lang.Iterable<?>) (var26)) {
                var27 = Math.max(var27, this.getRowWidth(var25, var29));
            }

            int var40 = Math.max(1, var25.getLineHeight());
            float var41 = var26.size() * var40;
            float var30 = this.getHorizontalPadding(var25);
            float var31 = this.HZAi(var25);
            float var32 = Math.min(Math.max(0.025F, (float)(0.0018 * var17)), 0.075F) * this.jWsed();
            GlStateManager.pushMatrix();
            RenderUtils$1 var33 = null;

            try {
                var33 = RenderUtils.uyB6();
                GlStateManager.translate((float)var19, (float)var21 + 0.75F, (float)var23);
                GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate((mc.gameSettings.thirdPersonView == 2 ? -1 : 1) * mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
                GlStateManager.scale(-var32, -var32, var32);
                GlStateManager.disableLighting();
                GlStateManager.disableDepth();
                GlStateManager.depthMask(false);
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                int var34 = this.getBackgroundAlpha();
                if (var34 > 0) {
                    RenderUtils.XNRNki(-var27 / 2.0 - var30, -var41 / 2.0 - var31, var27 / 2.0 + var30, var41 / 2.0 + var31, var34 << 24);
                }

                GlStateManager.enableBlend();
                GlStateManager.enableTexture2D();
                float var35 = -var41 / 2.0F + (var40 - var25.getFontHeight()) / 2.0F + var25.getTextTopOffset();

                for (int var36 = 0; var36 < var26.size(); var36++) {
                    this.drawItemRow(var25, (ItemESP$4)var26.get(var36), var35 + var36 * var40);
                }
            } finally {
                RenderUtils.restoreLightmapState(var33);
                GlStateManager.enableDepth();
                GlStateManager.depthMask(true);
                GlStateManager.disableLighting();
                GlStateManager.enableTexture2D();
                GlStateManager.enableAlpha();
                GlStateManager.disableBlend();
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                GlStateManager.popMatrix();
            }
        }
    }

    private int getRowWidth(IFont var1, ItemESP$4 var2) {
        String var3 = ItemESP$4.getGeneratorTag(var2) == null ? "" : ItemESP$4.getGeneratorTag(var2) + " ";
        String var4 = " x" + ItemESP$4.getTotalCount(var2);
        return var1.getStringWidth(var3) + var1.getStringWidth(ItemESP$4.getDisplayName(var2)) + var1.getStringWidth(var4);
    }

    private void drawItemRow(IFont var1, ItemESP$4 var2, float var3) {
        String var4 = " x" + ItemESP$4.getTotalCount(var2);
        int var5 = this.getRowWidth(var1, var2);
        int var6 = Math.max(1, var1.getStringWidth(ItemESP$4.getDisplayName(var2)));
        float var7 = -var5 / 2.0F;
        if (ItemESP$4.getGeneratorTag(var2) != null) {
            String var8 = ItemESP$4.getGeneratorTag(var2) + " ";
            var1.drawString(var8, var7, var3, -1, true);
            var7 += var1.getStringWidth(var8);
        }

        if (ItemESP$1.PinDj(ItemESP$4.getColorPair(var2)) != ItemESP$1.getSecondaryColor(ItemESP$4.getColorPair(var2))) {
            ItemESP$1 var12 = ItemESP$4.getColorPair(var2);
            float var10 = (float)(System.currentTimeMillis() % 2200L) / 2200.0F;
            var7 += var1.drawGlyphString(ItemESP$4.getDisplayName(var2), var7, var3, (recoveredArg0, recoveredArg1, recoveredArg2, recoveredArg3) -> ItemESP.getGlyphColor(var6, var10, var12, recoveredArg0, recoveredArg1, recoveredArg2, (java.lang.Integer) recoveredArg3), true);
        } else {
            var1.drawString(ItemESP$4.getDisplayName(var2), var7, var3, ItemESP$1.PinDj(ItemESP$4.getColorPair(var2)), true);
            var7 += var6;
        }

        var1.drawString(var4, var7, var3, -1, true);
    }

    private IFont getNametagFont() {
        return FontManager.getNametagRenderer(FontManager.getDefaultHudFontName());
    }

    private int getBackgroundAlpha() {
        return MathHelper.clamp_int((int)Math.round(this.opacity.number() * 2.55), 0, 255);
    }

    private float jWsed() {
        return (float)MathHelper.clamp_double(this.scale.number(), 0.5, 2.0);
    }

    private float getHorizontalPadding(IFont var1) {
        return Math.max(3.0F, (float)Math.ceil(var1.getFontHeight() * 0.45F));
    }

    private float HZAi(IFont var1) {
        return Math.max(2.0F, (float)Math.ceil(var1.getFontHeight() * 0.25F));
    }

    private static int lerpRgb(int var0, int var1, float var2) {
        int var3 = var0 >> 16 & 0xFF;
        int var4 = var0 >> 8 & 0xFF;
        int var5 = var0 & 0xFF;
        int var6 = var1 >> 16 & 0xFF;
        int var7 = var1 >> 8 & 0xFF;
        int var8 = var1 & 0xFF;
        int var9 = var3 + Math.round((var6 - var3) * var2);
        int var10 = var4 + Math.round((var7 - var4) * var2);
        int var11 = var5 + Math.round((var8 - var5) * var2);
        return 0xFF000000 | var9 << 16 | var10 << 8 | var11;
    }

    private void addDefaultItems() {
        this.KtGxa("minecraft:iron_ingot");
        this.KtGxa("minecraft:gold_ingot");
        this.KtGxa("minecraft:diamond");
        this.KtGxa("minecraft:emerald");
    }

    private void KtGxa(String var1) {
        if (!this.items.getItems().contains(var1)) {
            this.items.addItem(var1);
        }
    }

    private static int getGlyphColor(int var0, float var1, ItemESP$1 var2, char var3, float var4, float var5, Integer var6) {
        float var7 = ((var4 + var5 * 0.5F) / var0 + var1) % 1.0F;
        var7 = 1.0F - Math.abs(var7 * 2.0F - 1.0F);
        return lerpRgb(ItemESP$1.PinDj(var2), ItemESP$1.getSecondaryColor(var2), var7);
    }
}
