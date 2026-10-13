package stone.module.render;

import stone.core.Log;
import stone.core.Color;
import stone.event.Phase;
import stone.event.RenderLivingPreEvent;
import stone.event.RenderWorldLastEvent;
import stone.event.Subscribe;
import stone.event.TickEndEvent;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.ModuleManager;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.module.other.AntiBot;
import stone.module.other.Anticheat;
import stone.module.render.nametags.MinecraftFont;
import stone.module.render.nametags.NametagData;
import stone.module.render.nametags.NametagFormatter;
import stone.module.render.nametags.WarningIconRenderer;
import stone.util.ClientUtils;
import stone.util.font.FontManager;
import stone.util.font.IFont;
import stone.util.render.RenderUtils;
import stone.util.render.RenderUtils$1;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class Nametags extends Module {
    private static final boolean VVzU = Boolean.getBoolean("jade.debugNametagGl");
    private static final Comparator<NametagData> comparator = Nametags::compareByDistance;
    private static final String[] mRl4 = new String[]{"Hearts", "Health"};
    private static final int SKIN_SIZE = 10;
    private Setting scale;
    private Setting autoScale;
    private Setting background;
    private Setting onlyShowName;
    private Setting backgroundOpacity;
    private Setting backgroundBorder;
    private Setting seperateSections;
    private Setting rounding;
    private Setting showHealth;
    private Setting healthBar;
    private Setting healthDisplay;
    private Setting showHeartSymbol;
    private Setting textShadow;
    private Setting showDistance;
    private Setting showSkin;
    private Setting showArmor;
    private Setting showVanilla;
    private Setting maxDistance;
    private Setting friendColor;
    private Setting enemyColor;
    private final List<NametagData> nametagDatas = new ArrayList<>();
    private IFont iFont;
    private int ysuU = 0;

    public Nametags() {
        super("Nametags", "Custom nametags with health, distance and armor", Category.VISUALS, Subcategory.ADVANCED);
        this.scale = addNumber("Scale", "", 1.0F, 0.1F, 2.0F, 1);
        this.autoScale = addBoolean("Auto Scale", "", false);
        this.background = addBoolean("Background", "", true);
        this.backgroundOpacity = addNumber("Background Opacity", "", 0.5F, 0.0F, 1.0F, 2);
        this.backgroundBorder = addBoolean("Background Border", "", false);
        this.seperateSections = addBoolean("Seperate Sections", "", false);
        this.rounding = addNumber("Rounding", "", 0.0F, 0.0F, 6.0F, 1);
        this.onlyShowName = addBoolean("Only Show Name", "", false);
        this.showHealth = addBoolean("Show Health", "", false);
        this.healthBar = addBoolean("Health Bar", "", false);
        this.healthDisplay = addChoice("Health display", "", List.of(mRl4), 0);
        this.showHeartSymbol = addBoolean("Show Heart Symbol", "", true);
        this.textShadow = addBoolean("Text Shadow", "", false);
        this.showDistance = addBoolean("Show Distance", "", false);
        this.showSkin = addBoolean("Show Skin", "", false);
        this.showArmor = addBoolean("Show Armor", "", false);
        this.showVanilla = addBoolean("Show Vanilla", "", true);
        this.maxDistance = addNumber("Max distance", "", 96.0F, 16.0F, 256.0F, 0);
        this.friendColor = addColour("Friend color", "", Color.rgb(0x55FFFF));
        this.enemyColor = addColour("Enemy color", "", Color.rgb(0xFF5555));
    }

    @Override
    public void guiUpdate() {
        boolean var1 = this.background.bool();
        this.backgroundOpacity.setVisible(var1);
        this.backgroundBorder.setVisible(var1);
        this.seperateSections.setVisible(var1);
        this.rounding.setVisible(var1);
        this.healthBar.setVisible(var1);
        boolean var2 = this.showHealth.bool();
        this.healthDisplay.setVisible(var2);
        this.showHeartSymbol.setVisible(var2 && this.healthDisplay.choice() == 0);
    }

    @Subscribe
    public void onTickEnd(TickEndEvent var1) {
        if (var1.phase == Phase.END) {
            if (ClientUtils.isInWorld() && mc.theWorld != null) {
                this.updateNametagEntries();
            } else {
                this.ysuU = 0;
            }
        }
    }

    @Subscribe(priority = Subscribe.Priority.LOWEST)
    public void onRenderWorldLast(RenderWorldLastEvent var1) {
        if (ClientUtils.isInWorld()) {
            this.renderNametags(var1.partialTicks);
        }
    }

    @Subscribe
    public void onRenderLivingPre(RenderLivingPreEvent var1) {
        if (!this.showVanilla.bool()) {
            if (var1.entityLivingBase instanceof EntityPlayer) {
                EntityPlayer var2 = (EntityPlayer)var1.entityLivingBase;
                if (this.isValidNametagPlayer(var2)) {
                    var1.setCancelled(true);
                }
            }
        }
    }

    private void updateNametagEntries() {
        IFont var1 = this.getNametagFont();
        IFont var2 = this.getMinecraftFont(mc.fontRendererObj);
        Entity var3 = mc.getRenderViewEntity();
        if (var3 == null) {
            this.ysuU = 0;
        } else {
            boolean var4 = this.showDistance.bool();
            boolean var5 = this.showArmor.bool();
            boolean var6 = this.showHealth.bool();
            boolean var7 = this.showSkin.bool();
            boolean var8 = FontManager.isMinecraftFont(this.getSelectedFontName());
            float var9 = this.getBaseScale();
            double var10 = this.maxDistance.number() * this.maxDistance.number();
            this.ysuU = 0;

            for (EntityPlayer var13 : mc.theWorld.playerEntities) {
                if (this.isValidNametagPlayer(var13)) {
                    double var14 = var13.posX - var3.posX;
                    double var16 = var13.posY - var3.posY;
                    double var18 = var13.posZ - var3.posZ;
                    double var20 = var14 * var14 + var16 * var16 + var18 * var18;
                    if (!(var20 > var10)) {
                        float var22 = (float)Math.sqrt(var20);
                        String var23 = this.getNametagText(var13);
                        Anticheat var24 = ModuleManager.get(Anticheat.class);
                        boolean var25 = var24.FnlAevK(var13);
                        boolean var26 = var25 && !var8;
                        if (var25 && var8) {
                            var23 = "§6⚠§r " + var23;
                        }

                        String var27 = var6 ? this.formatHealthText(var13) : "";
                        String var28 = var4 ? this.formatDistanceText(var22) : "";
                        int var29 = var1.getStringWidth(var23);
                        int var30 = this.getHealthTextWidth(var1, var2, var27);
                        int var31 = var1.getStringWidth(var28);
                        int var32 = this.wemjfS1(var7, var29, var30, var31, var26 ? 9 : 0);
                        int var33 = ClientUtils.oCoqd(var13);
                        int var34 = this.tUp8(var13, var33);
                        int[] var35 = this.computeHighlightRange(var23, var13.getName());
                        ItemStack var36 = null;
                        ItemStack var37 = null;
                        ItemStack var38 = null;
                        ItemStack var39 = null;
                        ItemStack var40 = null;
                        int var41 = 0;
                        if (var5) {
                            var36 = var13.getEquipmentInSlot(0);
                            if (var36 != null) {
                                var41++;
                            }

                            var37 = var13.getEquipmentInSlot(1);
                            if (var37 != null) {
                                var41++;
                            }

                            var38 = var13.getEquipmentInSlot(2);
                            if (var38 != null) {
                                var41++;
                            }

                            var39 = var13.getEquipmentInSlot(3);
                            if (var39 != null) {
                                var41++;
                            }

                            var40 = var13.getEquipmentInSlot(4);
                            if (var40 != null) {
                                var41++;
                            }
                        }

                        if (this.ysuU >= this.nametagDatas.size()) {
                            this.nametagDatas.add(new NametagData());
                        }

                        this.nametagDatas
                            .get(this.ysuU++)
                            .KUOUyrO(
                                var13,
                                var23,
                                var27,
                                var28,
                                var32,
                                var29,
                                var30,
                                var33,
                                var34,
                                var26,
                                var35[0],
                                var35[1],
                                var20,
                                var9,
                                (var13.isSneaking() ? var13.height - 0.3F : var13.height) + 0.3F,
                                var36,
                                var37,
                                var38,
                                var39,
                                var40,
                                var41
                            );
                    }
                }
            }

            if (this.ysuU > 1) {
                this.nametagDatas.subList(0, this.ysuU).sort(comparator);
            }
        }
    }

    private void renderNametags(float var1) {
        RenderManager var2 = mc.getRenderManager();
        FontRenderer var3 = mc.fontRendererObj;
        IFont var4 = this.getNametagFont();
        if (var2 != null && var3 != null && this.ysuU != 0) {
            if (this.hasVisibleNametags()) {
                IFont var5 = this.getMinecraftFont(var3);
                int var6 = GL11.glGetInteger(2976);
                RenderUtils$1 var7 = null;
                this.pushMatrixState();

                try {
                    var7 = RenderUtils.uyB6();
                    this.logGlErrors("before draw");

                    for (int var8 = 0; var8 < this.ysuU; var8++) {
                        NametagData var9 = this.nametagDatas.get(var8);
                        if (var9.entityPlayer != null && RenderUtils.isEntityInView(var9.entityPlayer)) {
                            this.renderNametagInWorld(var9, var1, var2, var4, var5, var3);
                        }
                    }

                    this.logGlErrors("after draw");
                } finally {
                    try {
                        RenderUtils.restoreLightmapState(var7);
                    } finally {
                        this.restoreMatrixState(var6);
                    }

                    this.logGlErrors("after restore");
                }
            }
        }
    }

    private void pushMatrixState() {
        GlStateManager.pushAttrib();
        GL11.glMatrixMode(5889);
        GL11.glPushMatrix();
        GL11.glMatrixMode(5888);
        GL11.glPushMatrix();
    }

    private void restoreMatrixState(int var1) {
        GL11.glMatrixMode(5888);
        GL11.glPopMatrix();
        GL11.glMatrixMode(5889);
        GL11.glPopMatrix();
        GlStateManager.popAttrib();
        this.restoreGlState();
        GL11.glMatrixMode(var1);
    }

    private void restoreGlState() {
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.enableTexture2D();
        GlStateManager.disableAlpha();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(519, 0.0F);
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.disableDepth();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.disableLighting();
        GlStateManager.enableRescaleNormal();
        GlStateManager.disableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.disableBlend();
        GlStateManager.blendFunc(1, 0);
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glTexEnvi(8960, 8704, 8448);
        GlStateManager.color(0.0F, 0.0F, 0.0F, 0.0F);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glMatrixMode(5888);
    }

    private void logGlErrors(String var1) {
        if (VVzU) {
            int var2;
            while ((var2 = GL11.glGetError()) != 0) {
                Log.warn("Nametags", "GL error %d at %s", var2, var1);
            }
        }
    }

    private boolean hasVisibleNametags() {
        for (int var1 = 0; var1 < this.ysuU; var1++) {
            NametagData var2 = this.nametagDatas.get(var1);
            if (var2.entityPlayer != null && RenderUtils.isEntityInView(var2.entityPlayer)) {
                return true;
            }
        }

        return false;
    }

    private boolean isValidNametagPlayer(EntityPlayer var1) {
        if (var1 == null) {
            return false;
        } else if (var1 == mc.thePlayer) {
            return false;
        } else {
            return !var1.isDead && var1.deathTime <= 0 ? !AntiBot.shouldHideEntity(var1) : false;
        }
    }

    public boolean shouldHideVanillaNametag(EntityPlayer var1) {
        if (this.enabled() && !this.showVanilla.bool() && this.isValidNametagPlayer(var1)) {
            Entity var2 = mc.getRenderViewEntity();
            if (var2 == null) {
                return false;
            } else {
                double var3 = var1.posX - var2.posX;
                double var5 = var1.posY - var2.posY;
                double var7 = var1.posZ - var2.posZ;
                double var9 = this.maxDistance.number() * this.maxDistance.number();
                return var3 * var3 + var5 * var5 + var7 * var7 <= var9;
            }
        } else {
            return false;
        }
    }

    private String getNametagText(EntityPlayer var1) {
        if (!this.onlyShowName.bool()) {
            return var1.getDisplayName().getFormattedText();
        } else {
            String var2 = ClientUtils.getFirstColorCode(var1.getDisplayName().getFormattedText());
            String var3 = var2.length() >= 2 && var2.charAt(0) == 167 ? var2 : "";
            return var3 + var1.getName();
        }
    }

    @Override
    public void onDisable(Context context) {
        this.ysuU = 0;
    }

    private String formatDistanceText(float var1) {
        return NametagFormatter.formatDistance(var1);
    }

    private int tUp8(EntityPlayer var1, int var2) {
        if (ClientUtils.isFriend(var1)) {
            return var2 != -1 ? var2 : this.friendColor.colour().argb();
        } else {
            return ClientUtils.isEnemy(var1) ? this.enemyColor.colour().argb() : -1;
        }
    }

    private float getBaseScale() {
        return (float)this.scale.number() * 0.02F;
    }

    private float computeNametagScale(float var1, boolean var2) {
        return NametagFormatter.scaleByDistance(this.getBaseScale(), var1, var2, 5.0F);
    }

    private void renderNametagInWorld(NametagData var1, float var2, RenderManager var3, IFont var4, IFont var5, FontRenderer var6) {
        EntityPlayer var7 = var1.entityPlayer;
        if (var7 != null && !var7.isDead && var7.deathTime <= 0) {
            double var8 = var7.lastTickPosX + (var7.posX - var7.lastTickPosX) * var2 - var3.viewerPosX;
            double var10 = var7.lastTickPosY + (var7.posY - var7.lastTickPosY) * var2 - var3.viewerPosY;
            double var12 = var7.lastTickPosZ + (var7.posZ - var7.lastTickPosZ) * var2 - var3.viewerPosZ;
            float var14 = var1.scale;
            if (this.autoScale.bool()) {
                var14 = this.computeNametagScale((float)Math.sqrt(var8 * var8 + var10 * var10 + var12 * var12), true);
            }

            GlStateManager.pushMatrix();

            try {
                RenderUtils.resetLightmapState();
                GlStateManager.translate((float)var8, (float)var10 + var1.LVUo, (float)var12);
                GlStateManager.rotate(-var3.playerViewY, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(var3.playerViewX, 1.0F, 0.0F, 0.0F);
                GlStateManager.scale(-var14, -var14, var14);
                GlStateManager.disableLighting();
                GlStateManager.depthMask(false);
                GlStateManager.disableDepth();
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                GlStateManager.translate(0.0F, -10.0F, 0.0F);
                if (this.background.bool() && this.backgroundOpacity.number() > 0.01 || this.background.bool() && this.backgroundBorder.bool() || var1.healthColor != -1) {
                    this.drawNametagBackground(var1, 1.0F, var1.teamColor, var1.healthColor, var4);
                    this.resetRenderState();
                }

                if (this.background.bool() && this.healthBar.bool()) {
                    this.drawHealthBar(var1);
                    this.resetRenderState();
                }

                this.dfUy6(var1, var4, var5);
                this.resetRenderState();
                if (var1.equipmentCount > 0) {
                    int var15 = -(var1.equipmentCount * 14) / 2;
                    byte var16 = -25;
                    if (var1.heldItem != null) {
                        this.renderNametagItem(var1.heldItem, var15, var16, var6);
                        var15 += 14;
                    }

                    if (var1.UMkYt != null) {
                        this.renderNametagItem(var1.UMkYt, var15, var16, var6);
                        var15 += 14;
                    }

                    if (var1.chestplateItem != null) {
                        this.renderNametagItem(var1.chestplateItem, var15, var16, var6);
                        var15 += 14;
                    }

                    if (var1.leggingsItem != null) {
                        this.renderNametagItem(var1.leggingsItem, var15, var16, var6);
                        var15 += 14;
                    }

                    if (var1.bootsItem != null) {
                        this.renderNametagItem(var1.bootsItem, var15, var16, var6);
                    }
                }

                AntiInvis var20 = ModuleManager.get(AntiInvis.class);
                if (var20.isHiddenPlayer(var7) && var20.shouldHideVanillaNametag(var7)) {
                    this.resetRenderState();
                    int var21 = var1.equipmentCount > 0 ? -46 : -25;
                    AntiInvis.xiJ8(-9, var21);
                }
            } finally {
                GlStateManager.enableDepth();
                GlStateManager.depthMask(true);
                GlStateManager.disableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                GlStateManager.popMatrix();
            }
        }
    }

    private void resetRenderState() {
        RenderUtils.resetLightmapState();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glTexEnvi(8960, 8704, 8448);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void dfUy6(NametagData var1, IFont var2, IFont var3) {
        float var4 = -var1.XEwQp / 2.0F;
        int var5 = this.AHAp();
        if (var1.showWarningIcon) {
            WarningIconRenderer.VyqlhA(var4, 1.0F);
            var4 += 9 + var5;
        }

        if (this.showSkin.bool()) {
            this.PcVz(var1.entityPlayer, var4, 0.0F, 10.0F);
            this.resetRenderState();
            var4 += 10 + var5;
        }

        this.drawNameText(var1, var2, var4);
        var4 += var1.iZl;
        if (var1.healthText.length() > 0) {
            var4 += var5;
            this.drawHealthText(var1.healthText, var4, 1.0F, var2, var3);
            var4 += var1.urQ76;
        }

        if (var1.distanceText.length() > 0) {
            var4 += var5;
            var2.drawString(var1.distanceText, var4, 1.0F, -1, this.textShadow.bool());
            var4 += var2.getStringWidth(var1.distanceText);
        }
    }

    private void drawNameText(NametagData var1, IFont var2, float var3) {
        if (var1.healthColor != -1 && var1.ReXi >= 0 && var1.GXWXOB > var1.ReXi) {
            int[] var4 = new int[]{0};
            var2.drawGlyphString(var1.nameText, var3, 1.0F, (recoveredArg0, recoveredArg1, recoveredArg2, recoveredArg3) -> Nametags.getHealthGlyphColor(var4, var1, recoveredArg0, recoveredArg1, recoveredArg2, (java.lang.Integer) recoveredArg3), this.textShadow.bool());
        } else {
            var2.drawString(var1.nameText, var3, 1.0F, -1, this.textShadow.bool());
        }
    }

    private int getHealthTextWidth(IFont var1, IFont var2, String var3) {
        if (var3.length() != 0 && this.isHeartDisplay()) {
            int var4 = 0;
            String[] var5 = var3.split("❤", -1);

            for (int var6 = 0; var6 < var5.length; var6++) {
                var4 += var1.getStringWidth(var5[var6]);
                if (var6 < var5.length - 1) {
                    var4 += var2.getStringWidth("❤");
                }
            }

            return var4;
        } else {
            return var1.getStringWidth(var3);
        }
    }

    private void drawHealthText(String var1, float var2, float var3, IFont var4, IFont var5) {
        if (!this.isHeartDisplay()) {
            var4.drawString(var1, var2, var3, -1, this.textShadow.bool());
        } else {
            String[] var6 = var1.split("❤", -1);
            float var7 = var2;

            for (int var8 = 0; var8 < var6.length; var8++) {
                String var9 = var6[var8];
                if (var9.length() > 0) {
                    var4.drawString(var9, var7, var3, -1, this.textShadow.bool());
                    var7 += var4.getStringWidth(var9);
                }

                if (var8 < var6.length - 1) {
                    var5.drawString(this.getFormattingPrefix(var9) + "❤", var7, var3, -1, this.textShadow.bool());
                    var7 += var5.getStringWidth("❤");
                }
            }
        }
    }

    private String getFormattingPrefix(String var1) {
        return FontRenderer.getFormatFromString(var1);
    }

    private boolean isHeartDisplay() {
        return this.showHeartSymbol.bool() && this.healthDisplay.choice() == 0;
    }

    private int wemjfS1(boolean var1, int var2, int var3, int var4, int var5) {
        return NametagFormatter.getTotalWidth(var1 ? 10 : 0, this.AHAp(), var2, var3, var4, var5);
    }

    private int AHAp() {
        return this.background.bool() && this.seperateSections.bool() ? 8 : 4;
    }

    private void drawHealthBar(NametagData var1) {
        float var2 = Math.max(0.0F, var1.entityPlayer.getAbsorptionAmount());
        float var3 = Math.max(1.0F, var1.entityPlayer.getMaxHealth() + var2);
        float var4 = Math.max(0.0F, var1.entityPlayer.getHealth() + var2);
        float var5 = Math.max(0.0F, Math.min(1.0F, var4 / var3));
        float var6 = -var1.XEwQp / 2.0F - 3.0F;
        float var7 = var1.XEwQp / 2.0F + 3.0F;
        float var8 = this.getBackgroundTop(1.0F, this.getNametagFont());
        float var9 = var6 + (var7 - var6) * var5;
        RenderUtils.XNRNki(var6, var8, var7, var8 + 0.5F, 1711276032);
        if (var9 > var6) {
            RenderUtils.XNRNki(var6, var8, var9, var8 + 0.5F, this.fNtd(var5));
        }
    }

    private int fNtd(float var1) {
        return NametagFormatter.getHealthColor(var1);
    }

    private void PcVz(EntityPlayer var1, float var2, float var3, float var4) {
        if (var1 instanceof AbstractClientPlayer) {
            ResourceLocation var5 = ((AbstractClientPlayer)var1).getLocationSkin();
            if (var5 != null) {
                mc.getTextureManager().bindTexture(var5);
                boolean var6 = GL11.glIsEnabled(2884);
                GlStateManager.enableTexture2D();
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(770, 771);
                GlStateManager.disableCull();
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                this.taoPsh(var2, var3, var4, 0.125F, 0.125F, 0.125F);
                this.taoPsh(var2, var3, var4, 0.625F, 0.125F, 0.125F);
                if (var6) {
                    GlStateManager.enableCull();
                }
            }
        }
    }

    private void taoPsh(float var1, float var2, float var3, float var4, float var5, float var6) {
        float var7 = 3.0F;
        byte var8 = 8;
        GL11.glBegin(6);
        this.iPir(var1 + var3 * 0.5F, var2 + var3 * 0.5F, var1, var2, var3, var4, var5, var6);
        this.drawCornerArc(var1, var2, var3, var7, var4, var5, var6, var1 + var7, var2 + var7, 180.0F, 270.0F, var8);
        this.drawCornerArc(var1, var2, var3, var7, var4, var5, var6, var1 + var3 - var7, var2 + var7, 270.0F, 360.0F, var8);
        this.drawCornerArc(var1, var2, var3, var7, var4, var5, var6, var1 + var3 - var7, var2 + var3 - var7, 0.0F, 90.0F, var8);
        this.drawCornerArc(var1, var2, var3, var7, var4, var5, var6, var1 + var7, var2 + var3 - var7, 90.0F, 180.0F, var8);
        this.iPir(var1, var2 + var7, var1, var2, var3, var4, var5, var6);
        GL11.glEnd();
    }

    private void drawCornerArc(
        float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11, int var12
    ) {
        for (int var13 = 0; var13 <= var12; var13++) {
            float var14 = (float)Math.toRadians(var10 + (var11 - var10) * var13 / var12);
            this.iPir(var8 + (float)Math.cos(var14) * var4, var9 + (float)Math.sin(var14) * var4, var1, var2, var3, var5, var6, var7);
        }
    }

    private void iPir(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8) {
        float var9 = Math.max(0.0F, Math.min(var5, var1 - var3));
        float var10 = Math.max(0.0F, Math.min(var5, var2 - var4));
        GL11.glTexCoord2f(var6 + var8 * (var9 / var5), var7 + var8 * (var10 / var5));
        GL11.glVertex2f(var1, var2);
    }

    private void drawNametagBackground(NametagData var1, float var2, int var3, int var4, IFont var5) {
        float var6 = this.getBackgroundTop(var2, var5);
        float var7 = var2 + var5.getTextBottomOffset() + 2.0F;
        if (this.background.bool() && this.seperateSections.bool()) {
            this.ucaV(var1, var6, var7, var3, var4, var5);
        } else {
            this.drawBackgroundRect(-var1.XEwQp / 2.0F - 3.0F, var6, var1.XEwQp / 2.0F + 3.0F, var7, var3, var4);
        }
    }

    private void ucaV(NametagData var1, float var2, float var3, int var4, int var5, IFont var6) {
        float var7 = -var1.XEwQp / 2.0F;
        if (var1.showWarningIcon) {
            this.drawBackgroundSegment(var7, 9.0F, var2, var3, var4, var5);
            var7 += 17.0F;
        }

        if (this.showSkin.bool()) {
            this.drawBackgroundSegment(var7, 10.0F, var2, var3, var4, var5);
            var7 += 18.0F;
        }

        this.drawBackgroundSegment(var7, var1.iZl, var2, var3, var4, var5);
        var7 += var1.iZl;
        if (var1.healthText.length() > 0) {
            var7 += 8.0F;
            this.drawBackgroundSegment(var7, var1.urQ76, var2, var3, var4, var5);
            var7 += var1.urQ76;
        }

        if (var1.distanceText.length() > 0) {
            var7 += 8.0F;
            this.drawBackgroundSegment(var7, var6.getStringWidth(var1.distanceText), var2, var3, var4, var5);
        }
    }

    private void drawBackgroundSegment(float var1, float var2, float var3, float var4, int var5, int var6) {
        this.drawBackgroundRect(var1 - 3.0F, var3, var1 + var2 + 3.0F, var4, var5, var6);
    }

    private void drawBackgroundRect(float var1, float var2, float var3, float var4, int var5, int var6) {
        GlStateManager.disableTexture2D();
        Tessellator var7 = Tessellator.getInstance();
        WorldRenderer var8 = var7.getWorldRenderer();
        float var9 = (float)this.backgroundOpacity.number();
        boolean var10 = this.background.bool() && var9 > 0.01F;
        float var11 = (float)this.rounding.number();
        int var12 = var6 != -1 ? var6 : var5;
        boolean var13 = this.background.bool() && this.backgroundBorder.bool() || var6 != -1;
        int var14 = var12 == -1 ? -6710887 : 0xFF000000 | var12;
        int var15 = (int)(Math.max(0.0F, Math.min(1.0F, var9)) * 255.0F) << 24;
        if (var11 > 0.0F && var10) {
            if (var13) {
                int var26 = var6 != -1 ? var15 >>> 24 : 255;
                RenderUtils.jxyoE(var1 - 1.0F, var2 - 1.0F, var3 + 1.0F, var4 + 1.0F, var11 + 1.0F, var26 << 24 | var14 & 16777215);
            }

            RenderUtils.jxyoE(var1, var2, var3, var4, var11, var15);
            GlStateManager.disableTexture2D();
        } else {
            if (var10) {
                var8.begin(7, DefaultVertexFormats.POSITION_COLOR);
                var8.pos(var1, var2, 0.0).color(0.0F, 0.0F, 0.0F, var9).endVertex();
                var8.pos(var1, var4, 0.0).color(0.0F, 0.0F, 0.0F, var9).endVertex();
                var8.pos(var3, var4, 0.0).color(0.0F, 0.0F, 0.0F, var9).endVertex();
                var8.pos(var3, var2, 0.0).color(0.0F, 0.0F, 0.0F, var9).endVertex();
                var7.draw();
            }

            if (var13) {
                float var16;
                float var17;
                float var18;
                if (var12 != -1) {
                    var16 = (var12 >> 16 & 0xFF) / 255.0F;
                    var17 = (var12 >> 8 & 0xFF) / 255.0F;
                    var18 = (var12 & 0xFF) / 255.0F;
                } else {
                    var16 = 0.6F;
                    var17 = 0.6F;
                    var18 = 0.6F;
                }

                float var19 = 1.0F;
                float var20 = var6 != -1 ? var9 : 1.0F;
                float var21 = var1 - var19;
                float var22 = var3 + var19;
                float var23 = var2 - var19;
                float var24 = var4 + var19;
                float var25 = -0.001F;
                var8.begin(7, DefaultVertexFormats.POSITION_COLOR);
                var8.pos(var21, var23, var25).color(var16, var17, var18, var20).endVertex();
                var8.pos(var21, var2, var25).color(var16, var17, var18, var20).endVertex();
                var8.pos(var22, var2, var25).color(var16, var17, var18, var20).endVertex();
                var8.pos(var22, var23, var25).color(var16, var17, var18, var20).endVertex();
                var8.pos(var21, var4, var25).color(var16, var17, var18, var20).endVertex();
                var8.pos(var21, var24, var25).color(var16, var17, var18, var20).endVertex();
                var8.pos(var22, var24, var25).color(var16, var17, var18, var20).endVertex();
                var8.pos(var22, var4, var25).color(var16, var17, var18, var20).endVertex();
                var8.pos(var21, var2, var25).color(var16, var17, var18, var20).endVertex();
                var8.pos(var21, var4, var25).color(var16, var17, var18, var20).endVertex();
                var8.pos(var1, var4, var25).color(var16, var17, var18, var20).endVertex();
                var8.pos(var1, var2, var25).color(var16, var17, var18, var20).endVertex();
                var8.pos(var3, var2, var25).color(var16, var17, var18, var20).endVertex();
                var8.pos(var3, var4, var25).color(var16, var17, var18, var20).endVertex();
                var8.pos(var22, var4, var25).color(var16, var17, var18, var20).endVertex();
                var8.pos(var22, var2, var25).color(var16, var17, var18, var20).endVertex();
                var7.draw();
            }

            GlStateManager.enableTexture2D();
        }
    }

    private float getBackgroundTop(float var1, IFont var2) {
        return var1 + var2.getTextTopOffset() - 3.0F;
    }

    private int[] computeHighlightRange(String var1, String var2) {
        return NametagFormatter.findVisibleTextRange(var1, var2);
    }

    private String getSelectedFontName() {
        return FontManager.getDefaultHudFontName();
    }

    private IFont getNametagFont() {
        return FontManager.getNametagRenderer(this.getSelectedFontName());
    }

    private IFont getMinecraftFont(FontRenderer var1) {
        if (this.iFont == null) {
            this.iFont = new MinecraftFont(var1);
        }

        return this.iFont;
    }

    private String formatHealthText(EntityPlayer var1) {
        return NametagFormatter.formatHealthText(var1.getHealth(), var1.getMaxHealth(), var1.getAbsorptionAmount(), this.healthDisplay.choice() == 0, this.showHeartSymbol.bool());
    }

    private void renderNametagItem(ItemStack var1, int var2, int var3, FontRenderer var4) {
        if (var1 != null) {
            RenderUtils.renderItemStack(var1, var2, var3);
            RenderUtils.resetLightmapState();
            GlStateManager.disableDepth();
            if (var1.stackSize > 1) {
                String var5 = String.valueOf(var1.stackSize);
                var4.drawStringWithShadow(var5, var2 + 17 - var4.getStringWidth(var5), var3 + 9, 16777215);
            }

            GlStateManager.enableDepth();
        }
    }

    private static int getHealthGlyphColor(int[] var0, NametagData var1, char var2, float var3, float var4, Integer var5) {
        int var6 = var0[0]++;
        if (var6 >= var1.ReXi && var6 < var1.GXWXOB) {
            return var1.healthColor;
        } else {
            return var5 != null ? var5 : -1;
        }
    }

    private static int compareByDistance(NametagData var0, NametagData var1) {
        return Double.compare(var1.fQq, var0.fQq);
    }
}
