package stone.module.render;

import stone.core.Color;
import stone.event.EntityJoinWorldEvent;
import stone.event.RenderWorldLastEvent;
import stone.event.Subscribe;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.module.render.bedplates.BedLocator;
import stone.module.render.bedplates.BedPlateCache;
import stone.module.render.bedplates.BedPlateEntry;
import stone.module.render.bedplates.BedPlateShapes;
import stone.module.render.bedplates.BedPlateTracker;
import stone.util.ClientUtils;
import stone.util.block.BlockScanner;
import stone.util.font.FontManager;
import stone.util.font.IFont;
import stone.util.render.RenderUtils;
import stone.util.render.RenderUtils$1;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import org.lwjgl.opengl.GL11;

public class BedPlates extends Module {
    private Setting range;
    private Setting rounding;
    private Setting opacity;
    private Setting exposedOutline;
    private Setting exposedColor;
    private Setting outlineExposedLayers;
    private Setting defenceHud;
    private Setting blockCount;
    private Setting scale;
    private Setting autoScale;
    private final List<BlockPos[]> Izc = new ArrayList<>();
    private final List<BlockPos[]> YKxI = new ArrayList<>();
    private boolean lvZ;
    private final BedPlateTracker bedPlateTracker = new BedPlateTracker();

    public BedPlates() {
        super("BedPlates", "Shows the blocks defending each bed", Category.VISUALS, Subcategory.ADVANCED);
        this.range = addNumber("Range", "", 10.0F, 2.0F, 200.0F, 0);
        this.rounding = addNumber("Rounding", "", 4.0F, 0.0F, 10.0F, 1);
        this.opacity = addNumber("Opacity", "", 55.0F, 5.0F, 100.0F, 0);
        this.exposedOutline = addBoolean("Exposed Outline", "", true);
        this.exposedColor = addColour("Exposed Color", "", Color.rgb(0x50FF78, 0.353F));
        this.outlineExposedLayers = addBoolean("Outline Exposed Layers", "", true);
        this.defenceHud = addBoolean("Defence HUD", "", false);
        this.blockCount = addBoolean("Block count", "", true);
        this.scale = addNumber("Scale", "", 1.0F, 0.1F, 2.0F, 2);
        this.autoScale = addBoolean("Auto Scale", "", false);
    }

    @Override
    public void guiUpdate() {
        this.exposedColor.setVisible(this.exposedOutline.bool());
        boolean var1 = this.defenceHud.bool();
        this.opacity.setVisible(var1);
        this.outlineExposedLayers.setVisible(this.exposedOutline.bool());
        this.blockCount.setVisible(var1);
        this.scale.setVisible(var1);
        this.autoScale.setVisible(var1);
    }

    @Override
    public void onEnable(Context context) {
        this.setScanActive(dMne());
    }

    @Override
    public void onDisable(Context context) {
        this.setScanActive(false);
        this.YKxI.clear();
        this.Izc.clear();
        this.bedPlateTracker.VPpl();
    }

    @Subscribe
    public void onEntityJoinWorld(EntityJoinWorldEvent var1) {
        if (var1.entity == mc.thePlayer) {
            this.setScanActive(false);
            this.YKxI.clear();
            this.Izc.clear();
            this.bedPlateTracker.VPpl();
        }
    }

    public int getScanPriority() {
        return this.enabled() && this.lvZ ? 8 : 0;
    }

    @Override
    public void onUpdate() {
        boolean var1 = dMne();
        this.setScanActive(var1);
        if (!var1) {
            this.YKxI.clear();
            this.Izc.clear();
            this.bedPlateTracker.VPpl();
        } else {
            BlockScanner var2 = BlockScanner.getInstance();
            double var3 = this.range.number() * this.range.number();
            double var5 = mc.thePlayer.posX;
            double var7 = mc.thePlayer.posY;
            double var9 = mc.thePlayer.posZ;
            List var11 = BedLocator.QBREh(var2, mc.theWorld, var5, var7, var9, var3);
            this.YKxI.clear();

            for (BlockPos[] var13 : (java.lang.Iterable<BlockPos[]>) (java.lang.Iterable<?>) (var11)) {
                this.YKxI.add(BedLocator.copyBedPositions(var13));
            }

            if (!this.defenceHud.bool()) {
                this.bedPlateTracker.VPpl();
            } else {
                this.bedPlateTracker.WUpzQ(mc, var11);
            }
        }
    }

    @Override
    public String suffix() {
        if (!dMne()) {
            return "";
        } else {
            int var1 = BlockScanner.getInstance().getBedPlateCount();
            return var1 > 0 ? String.valueOf(var1) : "";
        }
    }

    @Subscribe(priority = Subscribe.Priority.HIGHEST)
    public void onRenderWorldLast(RenderWorldLastEvent var1) {
        if (this.enabled() && dMne()) {
            float var2 = this.WXeb();
            double var3 = this.range.number() * this.range.number();
            double var5 = mc.thePlayer.posX;
            double var7 = mc.thePlayer.posY;
            double var9 = mc.thePlayer.posZ;
            ArrayList var11 = new ArrayList();
            HashSet var12 = new HashSet();

            for (BlockPos[] var14 : this.YKxI) {
                BlockPos var15 = var14[0];
                AxisAlignedBB var16 = BedLocator.createBedBoundingBox(var14[0], var14[1], var2);
                if (RenderUtils.XRxsYw(var16) && var12.add(var15)) {
                    var11.add(BedLocator.copyBedPositions(var14));
                }
            }

            for (BlockPos[] var27 : new ArrayList<>(this.Izc)) {
                if (var27 != null && var27.length >= 2) {
                    BlockPos var30 = var27[0];
                    BlockPos var31 = var27[1];
                    if (!var12.contains(var30) && BedLocator.euy0(mc.theWorld, var27)) {
                        double var17 = var30.getX() + 0.5 - var5;
                        double var19 = var30.getY() + 0.5 - var7;
                        double var21 = var30.getZ() + 0.5 - var9;
                        if (!(var17 * var17 + var19 * var19 + var21 * var21 > var3)) {
                            AxisAlignedBB var23 = BedLocator.createBedBoundingBox(var30, var31, var2);
                            if (RenderUtils.XRxsYw(var23)) {
                                var11.add(BedLocator.copyBedPositions(var27));
                                var12.add(var30);
                            }
                        }
                    }
                }
            }

            for (BlockPos[] var28 : (java.lang.Iterable<BlockPos[]>) (java.lang.Iterable<?>) (var11)) {
                this.renderExposedLayerOutlines(var28, var2);
                if (this.defenceHud.bool() && BedLocator.hasFootPartFirst(mc.theWorld, var28)) {
                    this.XJAmO(var28, var2);
                }

            }

            this.Izc.clear();

            for (BlockPos[] var29 : (java.lang.Iterable<BlockPos[]>) (java.lang.Iterable<?>) (var11)) {
                this.Izc.add(BedLocator.copyBedPositions(var29));
            }
        }
    }

    private static boolean dMne() {
        return ClientUtils.getBedWarsBoardType() == 2;
    }

    private void setScanActive(boolean var1) {
        if (this.lvZ != var1) {
            BlockScanner var2 = BlockScanner.getInstance();
            this.lvZ = var1;
            if (var1) {
                var2.addListener(this.bedPlateTracker);
                var2.enableBedPlateScan();
                var2.queueLoadedChunks();
            } else {
                var2.removeListener(this.bedPlateTracker);
                var2.disableBedPlateScan();
            }
        }
    }

    private void renderExposedLayerOutlines(BlockPos[] var1, float var2) {
        List var3 = this.getAirLayerPositions(var1);
        int var4 = this.getLayerBlockCount(var1);
        boolean var5 = var4 > 0 && var3.size() > var4 / 2;
        if (this.exposedOutline.bool() && this.outlineExposedLayers.bool() && !var3.isEmpty() && !var5) {
            RenderUtils$1 var6 = null;

            try {
                var6 = RenderUtils.uyB6();
                GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                GlStateManager.disableTexture2D();
                GlStateManager.disableDepth();
                GlStateManager.disableCull();
                GlStateManager.depthMask(false);
                GL11.glLineWidth(2.0F);
                int var7 = this.exposedColor.colour().argb();
                float var8 = (var7 >> 24 & 0xFF) / 255.0F;
                float var9 = (var7 >> 16 & 0xFF) / 255.0F;
                float var10 = (var7 >> 8 & 0xFF) / 255.0F;
                float var11 = (var7 & 0xFF) / 255.0F;

                for (BlockPos var13 : (java.lang.Iterable<BlockPos>) (java.lang.Iterable<?>) (var3)) {
                    this.drawColoredBlockOutline(var13, var9, var10, var11, var8);
                }
            } finally {
                RenderUtils.restoreLightmapState(var6);
                GL11.glLineWidth(1.0F);
                this.restoreItemRenderState();
            }
        }
    }

    private void drawColoredBlockOutline(BlockPos var1, float var2, float var3, float var4, float var5) {
        double var6 = var1.getX() - mc.getRenderManager().viewerPosX;
        double var8 = var1.getY() - mc.getRenderManager().viewerPosY;
        double var10 = var1.getZ() - mc.getRenderManager().viewerPosZ;
        GlStateManager.color(var2, var3, var4, Math.max(var5, 0.35F));
        RenderUtils.drawBoxOutline(new AxisAlignedBB(var6, var8, var10, var6 + 1.0, var8 + 1.0, var10 + 1.0));
    }

    private boolean hasExposedLayers(BlockPos[] var1) {
        return !this.getAirLayerPositions(var1).isEmpty();
    }

    private int getLayerBlockCount(BlockPos[] var1) {
        if (var1 != null && var1.length >= 2) {
            List var2 = BedPlateShapes.getLayersForDirection(var1[0], var1[1]);
            if (var2 != null && !var2.isEmpty()) {
                HashSet var3 = new HashSet();

                for (BlockPos var5 : (java.lang.Iterable<BlockPos>) (java.lang.Iterable<?>) ((List)var2.get(0))) {
                    var3.add(var1[0].add(var5).toLong());
                }

                return var3.size();
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    private List<BlockPos> getAirLayerPositions(BlockPos[] var1) {
        ArrayList var2 = new ArrayList();
        if (var1 != null && var1.length >= 2 && mc.theWorld != null) {
            List var3 = BedPlateShapes.getLayersForDirection(var1[0], var1[1]);
            if (var3 != null && !var3.isEmpty()) {
                HashSet var4 = new HashSet();

                for (BlockPos var6 : (java.lang.Iterable<BlockPos>) (java.lang.Iterable<?>) ((List)var3.get(0))) {
                    BlockPos var7 = var1[0].add(var6);
                    if (mc.theWorld.getBlockState(var7).getBlock() == Blocks.air && var4.add(var7.toLong())) {
                        var2.add(var7);
                    }
                }

                return var2;
            } else {
                return var2;
            }
        } else {
            return var2;
        }
    }

    private void XJAmO(BlockPos[] var1, float var2) {
        BedPlateCache var3 = this.bedPlateTracker.getCache(var1[0], var1[1]);
        if (var3 != null && !var3.getEntries().isEmpty()) {
            RenderManager var4 = mc.getRenderManager();
            if (var4 != null) {
                AxisAlignedBB var5 = BedLocator.createBedBoundingBox(var1[0], var1[1], var2);
                double var6 = (var5.minX + var5.maxX) * 0.5 - var4.viewerPosX;
                double var8 = var5.maxY + 0.6 - var4.viewerPosY;
                double var10 = (var5.minZ + var5.maxZ) * 0.5 - var4.viewerPosZ;
                float var12 = this.getBaseScale();
                if (this.autoScale.bool()) {
                    float var13 = (float)Math.sqrt(var6 * var6 + var8 * var8 + var10 * var10);
                    var12 = this.computeAutoScale(var13);
                }

                IFont var31 = this.getBlockCountFont();
                if (var31 != null) {
                    List var14 = var3.getEntries();
                    int var15 = var14.size() * 18 - 2;
                    int var16 = -var15 / 2;
                    byte var17 = -8;
                    int var18 = var16 - 3;
                    int var19 = var17 - 3;
                    int var20 = var16 + var15 + 3;
                    int var21 = var17 + 16 + 3;
                    boolean var22 = this.exposedOutline.bool() && this.hasExposedLayers(var1);
                    GlStateManager.pushMatrix();
                    RenderUtils$1 var23 = null;

                    try {
                        var23 = RenderUtils.uyB6();
                        GlStateManager.translate(var6, var8, var10);
                        GlStateManager.rotate(-var4.playerViewY, 0.0F, 1.0F, 0.0F);
                        GlStateManager.rotate(var4.playerViewX, 1.0F, 0.0F, 0.0F);
                        GlStateManager.scale(-var12, -var12, var12);
                        GlStateManager.disableLighting();
                        GlStateManager.depthMask(false);
                        GlStateManager.disableDepth();
                        GlStateManager.disableCull();
                        GlStateManager.enableBlend();
                        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                        this.BtLcv(var18, var19, var20, var21, var22);
                        this.setupItemRenderState();

                        for (int var24 = 0; var24 < var14.size(); var24++) {
                            BedPlateEntry var25 = (BedPlateEntry)var14.get(var24);
                            int var26 = var16 + var24 * 18;
                            this.drawEntryIcon(var25, var26, var17);
                            this.setupItemRenderState();
                            if (this.blockCount.bool() && var25.GULE() > 1) {
                                String var27 = String.valueOf(var25.GULE());
                                var31.drawString(var27, var26 + 17 - var31.getStringWidth(var27), var17 + 9, -1, true);
                                this.setupItemRenderState();
                            }
                        }
                    } finally {
                        RenderUtils.restoreLightmapState(var23);
                        GlStateManager.popMatrix();
                        this.restoreItemRenderState();
                    }
                }
            }
        }
    }

    private void drawEntryIcon(BedPlateEntry var1, int var2, int var3) {
        if (var1 != null) {
            if (var1.hasItem()) {
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                RenderUtils.renderItemStackWithClear(var1.getItemStack(), var2, var3, false);
                RenderUtils.resetLightmapState();
            } else {
                if (var1.hasSprite()) {
                    this.leru(var1.getSprite(), var2, var3);
                }
            }
        }
    }

    private void leru(TextureAtlasSprite var1, int var2, int var3) {
        if (var1 != null) {
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
            GlStateManager.enableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            Tessellator var4 = Tessellator.getInstance();
            WorldRenderer var5 = var4.getWorldRenderer();
            var5.begin(7, DefaultVertexFormats.POSITION_TEX);
            var5.pos(var2, var3 + 16, 0.0).tex(var1.getMinU(), var1.getMaxV()).endVertex();
            var5.pos(var2 + 16, var3 + 16, 0.0).tex(var1.getMaxU(), var1.getMaxV()).endVertex();
            var5.pos(var2 + 16, var3, 0.0).tex(var1.getMaxU(), var1.getMinV()).endVertex();
            var5.pos(var2, var3, 0.0).tex(var1.getMinU(), var1.getMinV()).endVertex();
            var4.draw();
        }
    }

    private void BtLcv(int var1, int var2, int var3, int var4, boolean var5) {
        float var6 = (float)Math.max(0.0, Math.min(1.0, this.opacity.number() / 100.0));
        float var7 = Math.min((float)this.rounding.number(), Math.min((var3 - var1) / 2.0F, (var4 - var2) / 2.0F));
        this.PXxSlx(var1, var2, var3, var4, var7, ClientUtils.YVVZ(0, Math.round(var6 * 255.0F)));
        if (var5 && this.outlineExposedLayers.bool()) {
            int var8 = this.exposedColor.colour().argb();
            int var9 = Math.max(90, var8 >> 24 & 0xFF);
            this.drawStenciledOutline(var1, var2, var3, var4, var7, ClientUtils.YVVZ(var8, var9));
        }

        GlStateManager.enableTexture2D();
    }

    private void drawStenciledOutline(int var1, int var2, int var3, int var4, float var5, int var6) {
        float var7 = 1.25F;

        try {
            GL11.glClear(1024);
            GL11.glEnable(2960);
            GL11.glStencilMask(255);
            GL11.glColorMask(false, false, false, false);
            GL11.glDepthMask(false);
            GL11.glStencilFunc(519, 1, 255);
            GL11.glStencilOp(7681, 7681, 7681);
            this.PXxSlx(var1, var2, var3, var4, var5, -1);
            GL11.glColorMask(true, true, true, true);
            GL11.glStencilFunc(517, 1, 255);
            GL11.glStencilOp(7680, 7680, 7680);
            this.PXxSlx(var1 - var7, var2 - var7, var3 + var7, var4 + var7, var5 + var7, var6);
        } finally {
            GL11.glColorMask(true, true, true, true);
            GL11.glStencilMask(255);
            GL11.glDisable(2960);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private void PXxSlx(float var1, float var2, float var3, float var4, float var5, int var6) {
        if (!(var3 <= var1) && !(var4 <= var2)) {
            float var7 = Math.max(0.0F, Math.min(var5, Math.min((var3 - var1) / 2.0F, (var4 - var2) / 2.0F)));
            GlStateManager.disableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GL11.glDisable(2884);
            GL11.glColor4f((var6 >> 16 & 0xFF) / 255.0F, (var6 >> 8 & 0xFF) / 255.0F, (var6 & 0xFF) / 255.0F, (var6 >> 24 & 0xFF) / 255.0F);
            GL11.glBegin(9);
            this.emitArcVertices(var3 - var7, var4 - var7, var7, 0, 90);
            this.emitArcVertices(var1 + var7, var4 - var7, var7, 90, 180);
            this.emitArcVertices(var1 + var7, var2 + var7, var7, 180, 270);
            this.emitArcVertices(var3 - var7, var2 + var7, var7, 270, 360);
            GL11.glEnd();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private void emitArcVertices(float var1, float var2, float var3, int var4, int var5) {
        if (var3 <= 0.0F) {
            GL11.glVertex2f(var1, var2);
        } else {
            for (int var6 = var4; var6 <= var5; var6 += 6) {
                double var7 = Math.toRadians(var6);
                GL11.glVertex2f(var1 + (float)Math.cos(var7) * var3, var2 + (float)Math.sin(var7) * var3);
            }
        }
    }

    private void setupItemRenderState() {
        RenderUtils.resetLightmapState();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.disableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glTexEnvi(8960, 8704, 8448);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void restoreItemRenderState() {
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.enableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glTexEnvi(8960, 8704, 8448);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private float WXeb() {
        return 0.5625F;
    }

    private float getBaseScale() {
        return (float)this.scale.number() * 0.02F;
    }

    private float computeAutoScale(float var1) {
        float var2 = this.getBaseScale();
        float var3 = Math.max(1.0F, var1);
        float var4 = var2 * (var3 / 8.0F);
        return Math.max(var2, var4);
    }

    private String getBlockCountFontName() {
        return FontManager.getDefaultHudFontName();
    }

    private IFont getBlockCountFont() {
        return FontManager.getNametagRenderer(this.getBlockCountFontName());
    }
}
