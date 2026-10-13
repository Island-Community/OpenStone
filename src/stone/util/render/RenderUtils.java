package stone.util.render;

import stone.core.Assets;
import stone.util.IMinecraft;
import stone.util.block.BlockUtils;
import stone.util.block.StairOutlineRenderer;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

public class RenderUtils implements IMinecraft {
    private static Frustum frustum = new Frustum();
    private static final FloatBuffer lht = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer jgivP = BufferUtils.createFloatBuffer(16);
    private static final Map<String, ResourceLocation> iconTextureCache = new HashMap<>();

    public static RenderUtils$1 uyB6() {
        boolean var0 = GL11.glIsEnabled(2896);
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        boolean var1 = GL11.glIsEnabled(3553);
        ((Buffer)jgivP).clear();
        GL11.glGetFloat(2819, jgivP);
        RenderUtils$1 var2 = new RenderUtils$1(var0, var1, jgivP.get(0), jgivP.get(1));
        resetLightmapState();
        return var2;
    }

    public static void resetLightmapState() {
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.enableTexture2D();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.disableLighting();
    }

    public static void restoreLightmapState(RenderUtils$1 var0) {
        if (var0 == null) {
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        } else {
            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, RenderUtils$1.getLightmapU(var0), RenderUtils$1.getLightmapV(var0));
            if (RenderUtils$1.bGlc(var0)) {
                GlStateManager.enableTexture2D();
            } else {
                GlStateManager.disableTexture2D();
            }

            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            if (RenderUtils$1.isLightingEnabled(var0)) {
                GlStateManager.enableLighting();
            } else {
                GlStateManager.disableLighting();
            }
        }
    }

    public static void drawBlockBoundsBox(BlockPos var0, int var1, boolean var2, boolean var3) {
        iPds(var0.getX(), var0.getY(), var0.getZ(), 1.0, 1.0, 1.0, var1, var2, var3);
    }

    public static boolean isEntityInView(Entity var0) {
        return var0 == null ? false : XRxsYw(var0.getEntityBoundingBox()) || var0.ignoreFrustumCheck;
    }

    public static boolean XRxsYw(AxisAlignedBB var0) {
        if (var0 == null) {
            return false;
        } else {
            Entity var1 = mc.getRenderViewEntity();
            if (var1 == null) {
                return true;
            } else {
                frustum.setPosition(var1.posX, var1.posY, var1.posZ);
                return frustum.isBoundingBoxInFrustum(var0);
            }
        }
    }

    public static boolean HVp0(Entity var0, double var1) {
        if (var0 == null) {
            return false;
        } else {
            Entity var3 = mc.getRenderViewEntity();
            return var3 == null ? false : var0.getDistanceSqToEntity(var3) <= var1;
        }
    }

    public static void XNRNki(double var0, double var2, double var4, double var6, int var8) {
        float var9 = (var8 >> 24 & 0xFF) / 255.0F;
        float var10 = (var8 >> 16 & 0xFF) / 255.0F;
        float var11 = (var8 >> 8 & 0xFF) / 255.0F;
        float var12 = (var8 & 0xFF) / 255.0F;
        GlStateManager.pushMatrix();
        Tessellator var13 = Tessellator.getInstance();
        WorldRenderer var14 = var13.getWorldRenderer();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(var10, var11, var12, var9);
        var14.begin(7, DefaultVertexFormats.POSITION);
        var14.pos(var0, var6, 0.0).endVertex();
        var14.pos(var4, var6, 0.0).endVertex();
        var14.pos(var4, var2, 0.0).endVertex();
        var14.pos(var0, var2, 0.0).endVertex();
        var13.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    public static void iPds(double var0, double var2, double var4, double var6, double var8, double var10, int var12, boolean var13, boolean var14) {
        double var15 = var0 - mc.getRenderManager().viewerPosX;
        double var17 = var2 - mc.getRenderManager().viewerPosY;
        double var19 = var4 - mc.getRenderManager().viewerPosZ;
        GL11.glPushMatrix();
        GL11.glPushAttrib(1048575);

        try {
            GL11.glBlendFunc(770, 771);
            GL11.glEnable(3042);
            GL11.glLineWidth(2.0F);
            GL11.glDisable(3553);
            GL11.glDisable(2929);
            GL11.glDepthMask(false);
            float var21 = (var12 >> 24 & 0xFF) / 255.0F;
            float var22 = (var12 >> 16 & 0xFF) / 255.0F;
            float var23 = (var12 >> 8 & 0xFF) / 255.0F;
            float var24 = (var12 & 0xFF) / 255.0F;
            GL11.glColor4f(var22, var23, var24, var21);
            AxisAlignedBB var25 = new AxisAlignedBB(var15, var17, var19, var15 + var6, var17 + var8, var19 + var10);
            if (var13) {
                drawBoxOutline(var25);
            }

            if (var14) {
                drawFilledBox(var25, var22, var23, var24);
            }
        } finally {
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glPopAttrib();
            GL11.glPopMatrix();
            resetGlPipelineState();
        }
    }

    private static void addColoredVertex(WorldRenderer var0, double var1, double var3, double var5, int var7) {
        var0.pos(var1, var3, var5).color(var7 >> 16 & 0xFF, var7 >> 8 & 0xFF, var7 & 0xFF, var7 >> 24 & 0xFF).endVertex();
    }

    private static void addFaceVertices(WorldRenderer var0, EnumFacing var1, AxisAlignedBB var2, int var3, int var4) {
        switch (var1) {
            case UP:
                addColoredVertex(var0, var2.minX, var2.maxY, var2.maxZ, var3);
                addColoredVertex(var0, var2.maxX, var2.maxY, var2.maxZ, var4);
                addColoredVertex(var0, var2.maxX, var2.maxY, var2.minZ, var3);
                addColoredVertex(var0, var2.minX, var2.maxY, var2.minZ, var4);
                break;
            case DOWN:
                addColoredVertex(var0, var2.maxX, var2.minY, var2.maxZ, var3);
                addColoredVertex(var0, var2.minX, var2.minY, var2.maxZ, var4);
                addColoredVertex(var0, var2.minX, var2.minY, var2.minZ, var3);
                addColoredVertex(var0, var2.maxX, var2.minY, var2.minZ, var4);
                break;
            case NORTH:
                addColoredVertex(var0, var2.maxX, var2.maxY, var2.minZ, var3);
                addColoredVertex(var0, var2.maxX, var2.minY, var2.minZ, var4);
                addColoredVertex(var0, var2.minX, var2.minY, var2.minZ, var3);
                addColoredVertex(var0, var2.minX, var2.maxY, var2.minZ, var4);
                break;
            case SOUTH:
                addColoredVertex(var0, var2.minX, var2.maxY, var2.maxZ, var3);
                addColoredVertex(var0, var2.minX, var2.minY, var2.maxZ, var4);
                addColoredVertex(var0, var2.maxX, var2.minY, var2.maxZ, var3);
                addColoredVertex(var0, var2.maxX, var2.maxY, var2.maxZ, var4);
                break;
            case EAST:
                addColoredVertex(var0, var2.maxX, var2.maxY, var2.minZ, var3);
                addColoredVertex(var0, var2.maxX, var2.maxY, var2.maxZ, var4);
                addColoredVertex(var0, var2.maxX, var2.minY, var2.maxZ, var3);
                addColoredVertex(var0, var2.maxX, var2.minY, var2.minZ, var4);
                break;
            case WEST:
                addColoredVertex(var0, var2.minX, var2.maxY, var2.maxZ, var3);
                addColoredVertex(var0, var2.minX, var2.maxY, var2.minZ, var4);
                addColoredVertex(var0, var2.minX, var2.minY, var2.minZ, var3);
                addColoredVertex(var0, var2.minX, var2.minY, var2.maxZ, var4);
        }
    }

    public static void drawBlockFace(AxisAlignedBB var0, EnumFacing var1, int var2, int var3, boolean var4, boolean var5) {
        Tessellator var6 = Tessellator.getInstance();
        WorldRenderer var7 = var6.getWorldRenderer();
        if (var4) {
            var7.begin(7, DefaultVertexFormats.POSITION_COLOR);
            addFaceVertices(var7, var1, var0, var2, var2);
            var6.draw();
        }

        if (var5) {
            var7.begin(2, DefaultVertexFormats.POSITION_COLOR);
            addFaceVertices(var7, var1, var0, var3, var3);
            var6.draw();
        }
    }

    public static void IfIae(BlockPos var0, IBlockState var1, int var2, boolean var3, boolean var4, Set<EnumFacing> var5) {
        AxisAlignedBB var6 = BlockUtils.getSelectedBounds(var0);
        if (var6 != null) {
            double var7 = mc.getRenderManager().viewerPosX;
            double var9 = mc.getRenderManager().viewerPosY;
            double var11 = mc.getRenderManager().viewerPosZ;
            int var13 = var2 & 16777215 | 1056964608;
            int var14 = var2 | 0xFF000000;
            GL11.glPushMatrix();
            GL11.glPushAttrib(1048575);
            RenderUtils$1 var15 = null;

            try {
                var15 = uyB6();
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                GL11.glLineWidth(2.0F);
                GlStateManager.disableTexture2D();
                GlStateManager.disableDepth();
                GlStateManager.disableCull();
                GlStateManager.depthMask(false);
                if (var1.getBlock() instanceof BlockStairs) {
                    StairOutlineRenderer.renderStairOutline(var0, var1, var6, null, var7, var9, var11, var13, var14, var14, var14, var4, var3, (recoveredArg0, recoveredArg1, recoveredArg2, recoveredArg3, recoveredArg4, recoveredArg5, recoveredArg6, recoveredArg7) -> RenderUtils.renderBlockFaceQuad(var13, var14, (net.minecraft.util.AxisAlignedBB) recoveredArg0, (net.minecraft.util.EnumFacing) recoveredArg1, recoveredArg2, recoveredArg3, recoveredArg4, recoveredArg5, recoveredArg6, recoveredArg7));
                } else {
                    AxisAlignedBB var16 = var6.offset(-var7, -var9, -var11);

                    for (EnumFacing var18 : var5) {
                        drawBlockFace(var16, var18, var13, var14, var4, var3);
                    }
                }
            } finally {
                restoreLightmapState(var15);
                GL11.glPopAttrib();
                GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
                GL11.glEnable(3553);
                GlStateManager.enableTexture2D();
                GlStateManager.enableDepth();
                GlStateManager.enableCull();
                GlStateManager.depthMask(true);
                GlStateManager.disableBlend();
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                GL11.glPopMatrix();
            }
        }
    }

    public static void drawBoxOutline(AxisAlignedBB var0) {
        Tessellator var1 = Tessellator.getInstance();
        WorldRenderer var2 = var1.getWorldRenderer();
        RenderUtils$1 var3 = uyB6();

        try {
            var2.begin(1, DefaultVertexFormats.POSITION);
            addLineSegment(var2, var0.minX, var0.minY, var0.minZ, var0.maxX, var0.minY, var0.minZ);
            addLineSegment(var2, var0.maxX, var0.minY, var0.minZ, var0.maxX, var0.minY, var0.maxZ);
            addLineSegment(var2, var0.maxX, var0.minY, var0.maxZ, var0.minX, var0.minY, var0.maxZ);
            addLineSegment(var2, var0.minX, var0.minY, var0.maxZ, var0.minX, var0.minY, var0.minZ);
            addLineSegment(var2, var0.minX, var0.maxY, var0.minZ, var0.maxX, var0.maxY, var0.minZ);
            addLineSegment(var2, var0.maxX, var0.maxY, var0.minZ, var0.maxX, var0.maxY, var0.maxZ);
            addLineSegment(var2, var0.maxX, var0.maxY, var0.maxZ, var0.minX, var0.maxY, var0.maxZ);
            addLineSegment(var2, var0.minX, var0.maxY, var0.maxZ, var0.minX, var0.maxY, var0.minZ);
            addLineSegment(var2, var0.minX, var0.minY, var0.minZ, var0.minX, var0.maxY, var0.minZ);
            addLineSegment(var2, var0.maxX, var0.minY, var0.minZ, var0.maxX, var0.maxY, var0.minZ);
            addLineSegment(var2, var0.maxX, var0.minY, var0.maxZ, var0.maxX, var0.maxY, var0.maxZ);
            addLineSegment(var2, var0.minX, var0.minY, var0.maxZ, var0.minX, var0.maxY, var0.maxZ);
            var1.draw();
        } finally {
            restoreLightmapState(var3);
        }
    }

    public static void drawOffsetBoxOutline(AxisAlignedBB var0, double var1, double var3, double var5) {
        drawBoxOutline(var0.offset(-var1, -var3, -var5));
    }

    public static void drawEntityBox(Entity var0, int var1, float var2, float var3) {
        double var4 = var0.lastTickPosX + (var0.posX - var0.lastTickPosX) * var3 - mc.getRenderManager().viewerPosX;
        double var6 = var0.lastTickPosY + (var0.posY - var0.lastTickPosY) * var3 - mc.getRenderManager().viewerPosY;
        double var8 = var0.lastTickPosZ + (var0.posZ - var0.lastTickPosZ) * var3 - mc.getRenderManager().viewerPosZ;
        AxisAlignedBB var10 = var0.getEntityBoundingBox().offset(var4 - var0.posX, var6 - var0.posY, var8 - var0.posZ);
        float var11 = (var1 >>> 24 & 0xFF) / 255.0F;
        float var12 = (var1 >> 16 & 0xFF) / 255.0F;
        float var13 = (var1 >> 8 & 0xFF) / 255.0F;
        float var14 = (var1 & 0xFF) / 255.0F;
        GL11.glPushMatrix();
        GL11.glPushAttrib(1048575);
        RenderUtils$1 var15 = null;

        try {
            var15 = uyB6();
            GL11.glBlendFunc(770, 771);
            GL11.glEnable(3042);
            GL11.glDisable(3553);
            GL11.glDisable(2929);
            GL11.glDepthMask(false);
            drawFilledAabb(var10, var12, var13, var14, 0.3F * var11);
            GL11.glEnable(2848);
            GL11.glLineWidth(Math.max(1.0F, var2));
            GL11.glColor4f(var12, var13, var14, var11);
            drawBoxOutline(var10);
        } finally {
            restoreLightmapState(var15);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glLineWidth(1.0F);
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }

    private static void addLineSegment(WorldRenderer var0, double var1, double var3, double var5, double var7, double var9, double var11) {
        var0.pos(var1, var3, var5).endVertex();
        var0.pos(var7, var9, var11).endVertex();
    }

    public static void drawFilledBox(AxisAlignedBB var0, float var1, float var2, float var3) {
        drawFilledAabb(var0, var1, var2, var3, 0.25F);
    }

    public static void drawFilledAabb(AxisAlignedBB var0, float var1, float var2, float var3, float var4) {
        Tessellator var5 = Tessellator.getInstance();
        WorldRenderer var6 = var5.getWorldRenderer();
        RenderUtils$1 var7 = uyB6();

        try {
            var6.begin(7, DefaultVertexFormats.POSITION_COLOR);
            addQuadWithColor(
                var6,
                var0.minX,
                var0.minY,
                var0.minZ,
                var0.maxX,
                var0.minY,
                var0.minZ,
                var0.maxX,
                var0.maxY,
                var0.minZ,
                var0.minX,
                var0.maxY,
                var0.minZ,
                var1,
                var2,
                var3,
                var4
            );
            addQuadWithColor(
                var6,
                var0.maxX,
                var0.minY,
                var0.maxZ,
                var0.minX,
                var0.minY,
                var0.maxZ,
                var0.minX,
                var0.maxY,
                var0.maxZ,
                var0.maxX,
                var0.maxY,
                var0.maxZ,
                var1,
                var2,
                var3,
                var4
            );
            addQuadWithColor(
                var6,
                var0.minX,
                var0.minY,
                var0.maxZ,
                var0.minX,
                var0.minY,
                var0.minZ,
                var0.minX,
                var0.maxY,
                var0.minZ,
                var0.minX,
                var0.maxY,
                var0.maxZ,
                var1,
                var2,
                var3,
                var4
            );
            addQuadWithColor(
                var6,
                var0.maxX,
                var0.minY,
                var0.minZ,
                var0.maxX,
                var0.minY,
                var0.maxZ,
                var0.maxX,
                var0.maxY,
                var0.maxZ,
                var0.maxX,
                var0.maxY,
                var0.minZ,
                var1,
                var2,
                var3,
                var4
            );
            addQuadWithColor(
                var6,
                var0.minX,
                var0.maxY,
                var0.minZ,
                var0.maxX,
                var0.maxY,
                var0.minZ,
                var0.maxX,
                var0.maxY,
                var0.maxZ,
                var0.minX,
                var0.maxY,
                var0.maxZ,
                var1,
                var2,
                var3,
                var4
            );
            addQuadWithColor(
                var6,
                var0.minX,
                var0.minY,
                var0.maxZ,
                var0.maxX,
                var0.minY,
                var0.maxZ,
                var0.maxX,
                var0.minY,
                var0.minZ,
                var0.minX,
                var0.minY,
                var0.minZ,
                var1,
                var2,
                var3,
                var4
            );
            var5.draw();
        } finally {
            restoreLightmapState(var7);
        }
    }

    private static void addQuadWithColor(
        WorldRenderer var0,
        double var1,
        double var3,
        double var5,
        double var7,
        double var9,
        double var11,
        double var13,
        double var15,
        double var17,
        double var19,
        double var21,
        double var23,
        float var25,
        float var26,
        float var27,
        float var28
    ) {
        var0.pos(var1, var3, var5).color(var25, var26, var27, var28).endVertex();
        var0.pos(var7, var9, var11).color(var25, var26, var27, var28).endVertex();
        var0.pos(var13, var15, var17).color(var25, var26, var27, var28).endVertex();
        var0.pos(var19, var21, var23).color(var25, var26, var27, var28).endVertex();
    }

    public static void drawTracerToEntity(Entity var0, int var1, float var2, float var3) {
        if (var0 != null && mc.getRenderManager() != null) {
            Entity var4 = mc.getRenderViewEntity();
            if (var4 == null) {
                var4 = mc.thePlayer;
            }

            if (var4 != null) {
                double var5 = var0.lastTickPosX + (var0.posX - var0.lastTickPosX) * var3 - mc.getRenderManager().viewerPosX;
                double var7 = var0.lastTickPosY
                    + (var0.posY - var0.lastTickPosY) * var3
                    - mc.getRenderManager().viewerPosY
                    + var0.getEyeHeight()
                    + (var0.isSneaking() ? -0.125 : 0.0);
                double var9 = var0.lastTickPosZ + (var0.posZ - var0.lastTickPosZ) * var3 - mc.getRenderManager().viewerPosZ;
                double var11 = 0.0;
                double var13 = var4.getEyeHeight();
                double var15 = 0.0;
                if (var4 == mc.thePlayer && mc.gameSettings.thirdPersonView == 0) {
                    float var17 = ((Entity)var4).rotationYaw;
                    float var18 = ((Entity)var4).rotationPitch;
                    double var19 = -Math.sin(Math.toRadians(var17)) * Math.cos(Math.toRadians(var18));
                    double var21 = -Math.sin(Math.toRadians(var18));
                    double var23 = Math.cos(Math.toRadians(var17)) * Math.cos(Math.toRadians(var18));
                    var11 = var19;
                    var13 += var21;
                    var15 = var23;
                }

                float var28 = (var1 >> 24 & 0xFF) / 255.0F;
                float var29 = (var1 >> 16 & 0xFF) / 255.0F;
                float var30 = (var1 >> 8 & 0xFF) / 255.0F;
                float var20 = (var1 & 0xFF) / 255.0F;
                GL11.glPushMatrix();
                RenderUtils$1 var31 = null;

                try {
                    var31 = uyB6();
                    GL11.glEnable(3042);
                    GL11.glBlendFunc(770, 771);
                    GL11.glEnable(2848);
                    GL11.glDisable(3553);
                    GL11.glDisable(2929);
                    GL11.glDepthMask(false);
                    GL11.glLineWidth(var2);
                    GL11.glColor4f(var29, var30, var20, var28);
                    GL11.glBegin(1);
                    GL11.glVertex3d(var11, var13, var15);
                    GL11.glVertex3d(var5, var7, var9);
                    GL11.glEnd();
                    GL11.glLineWidth(1.0F);
                    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                    GL11.glDepthMask(true);
                    GL11.glEnable(2929);
                    GL11.glEnable(3553);
                    GL11.glDisable(2848);
                    GL11.glDisable(3042);
                } finally {
                    restoreLightmapState(var31);
                    GL11.glPopMatrix();
                }
            }
        }
    }

    public static void GIWbDs(int var0) {
        GL11.glColor4f((var0 >> 16 & 0xFF) / 255.0F, (var0 >> 8 & 0xFF) / 255.0F, (var0 & 0xFF) / 255.0F, (var0 >> 24 & 0xFF) / 255.0F);
    }

    public static void setAlphaThreshold(float var0) {
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, (float)(var0 * 0.01));
    }

    public static void lTbf() {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void resetGlPipelineState() {
        int var0 = GL11.glGetInteger(34016);
        resetTextureUnitState(OpenGlHelper.defaultTexUnit);
        resetTextureUnitState(OpenGlHelper.lightmapTexUnit);
        rebindActiveTexture(var0);
        boolean var1 = GL11.glIsEnabled(3008);
        int var2 = GL11.glGetInteger(3009);
        float var3 = GL11.glGetFloat(3010);
        boolean var4 = GL11.glIsEnabled(2929);
        boolean var5 = GL11.glGetBoolean(2930);
        boolean var6 = GL11.glIsEnabled(2896);
        boolean var7 = GL11.glIsEnabled(32826);
        boolean var8 = GL11.glIsEnabled(3042);
        int var9 = GL11.glGetInteger(32969);
        int var10 = GL11.glGetInteger(32968);
        int var11 = GL11.glGetInteger(32971);
        int var12 = GL11.glGetInteger(32970);
        ((Buffer)lht).clear();
        GL11.glGetFloat(2816, lht);
        float var13 = lht.get(0);
        float var14 = lht.get(1);
        float var15 = lht.get(2);
        float var16 = lht.get(3);
        applyAlphaState(var1);
        GlStateManager.alphaFunc(519, 0.0F);
        GlStateManager.alphaFunc(var2, var3);
        applyDepthState(var4);
        GlStateManager.depthMask(!var5);
        GlStateManager.depthMask(var5);
        applyLightingState(var6);
        cIv778(var7);
        QvSfy6(var8);
        GlStateManager.tryBlendFuncSeparate(1, 0, 1, 0);
        GlStateManager.tryBlendFuncSeparate(var9, var10, var11, var12);
        GlStateManager.color(var13 == 0.0F ? 1.0F : 0.0F, var14 == 0.0F ? 1.0F : 0.0F, var15 == 0.0F ? 1.0F : 0.0F, var16 == 0.0F ? 1.0F : 0.0F);
        GlStateManager.color(var13, var14, var15, var16);
    }

    private static void resetTextureUnitState(int var0) {
        rebindActiveTexture(var0);
        boolean var1 = GL11.glIsEnabled(3553);
        int var2 = GL11.glGetInteger(32873);
        if (var1) {
            GlStateManager.disableTexture2D();
            GlStateManager.enableTexture2D();
        } else {
            GlStateManager.enableTexture2D();
            GlStateManager.disableTexture2D();
        }

        if (var2 != 0) {
            GlStateManager.bindTexture(0);
        }

        GlStateManager.bindTexture(var2);
    }

    private static void rebindActiveTexture(int var0) {
        int var1 = var0 == OpenGlHelper.defaultTexUnit ? OpenGlHelper.lightmapTexUnit : OpenGlHelper.defaultTexUnit;
        GlStateManager.setActiveTexture(var1);
        GlStateManager.setActiveTexture(var0);
    }

    private static void applyAlphaState(boolean var0) {
        if (var0) {
            GlStateManager.disableAlpha();
            GlStateManager.enableAlpha();
        } else {
            GlStateManager.enableAlpha();
            GlStateManager.disableAlpha();
        }
    }

    private static void applyDepthState(boolean var0) {
        if (var0) {
            GlStateManager.disableDepth();
            GlStateManager.enableDepth();
        } else {
            GlStateManager.enableDepth();
            GlStateManager.disableDepth();
        }
    }

    private static void applyLightingState(boolean var0) {
        if (var0) {
            GlStateManager.disableLighting();
            GlStateManager.enableLighting();
        } else {
            GlStateManager.enableLighting();
            GlStateManager.disableLighting();
        }
    }

    private static void cIv778(boolean var0) {
        if (var0) {
            GlStateManager.disableRescaleNormal();
            GlStateManager.enableRescaleNormal();
        } else {
            GlStateManager.enableRescaleNormal();
            GlStateManager.disableRescaleNormal();
        }
    }

    private static void QvSfy6(boolean var0) {
        if (var0) {
            GlStateManager.disableBlend();
            GlStateManager.enableBlend();
        } else {
            GlStateManager.enableBlend();
            GlStateManager.disableBlend();
        }
    }

    public static RenderUtils$2 captureProjectionMatrices(RenderUtils$2 var0, int var1) {
        if (var0 == null) {
            var0 = new RenderUtils$2();
        }

        RenderUtils$2.setGuiScale(var0, var1);
        ((Buffer)RenderUtils$2.Aqnrs(var0)).clear();
        ((Buffer)RenderUtils$2.getProjectionMatrix(var0)).clear();
        ((Buffer)RenderUtils$2.getViewportBuffer(var0)).clear();
        GL11.glGetFloat(2982, RenderUtils$2.Aqnrs(var0));
        GL11.glGetFloat(2983, RenderUtils$2.getProjectionMatrix(var0));
        GL11.glGetInteger(2978, RenderUtils$2.getViewportBuffer(var0));
        ((Buffer)RenderUtils$2.Aqnrs(var0)).rewind();
        ((Buffer)RenderUtils$2.getProjectionMatrix(var0)).rewind();
        ((Buffer)RenderUtils$2.getViewportBuffer(var0)).rewind();
        return var0;
    }

    public static boolean projectToScreen(RenderUtils$2 var0, double var1, double var3, double var5, double[] var7) {
        if (var0 != null && var7 != null && var7.length >= 3) {
            ((Buffer)RenderUtils$2.getProjectedCoords(var0)).clear();
            boolean var8 = GLU.gluProject(
                (float)var1, (float)var3, (float)var5, RenderUtils$2.Aqnrs(var0), RenderUtils$2.getProjectionMatrix(var0), RenderUtils$2.getViewportBuffer(var0), RenderUtils$2.getProjectedCoords(var0)
            );
            if (!var8) {
                return false;
            } else {
                var7[0] = RenderUtils$2.getProjectedCoords(var0).get(0) / RenderUtils$2.getGuiScale(var0);
                var7[1] = (Display.getHeight() - RenderUtils$2.getProjectedCoords(var0).get(1)) / RenderUtils$2.getGuiScale(var0);
                var7[2] = RenderUtils$2.getProjectedCoords(var0).get(2);
                return true;
            }
        } else {
            return false;
        }
    }

    public static void jxyoE(float var0, float var1, float var2, float var3, float var4, int var5) {
        if (!(var2 <= var0)) {
            float var6 = var2 - var0;
            if (var6 < 3.0F) {
                var4 = Math.min(var4, var6 / 2.0F);
            }

            var0 = (float)(var0 * 2.0);
            var1 = (float)(var1 * 2.0);
            var2 = (float)(var2 * 2.0);
            var3 = (float)(var3 * 2.0);
            GL11.glPushMatrix();
            GL11.glPushAttrib(1048575);
            GL11.glScaled(0.5, 0.5, 0.5);
            GL11.glEnable(3042);
            GL11.glDisable(3553);
            GL11.glEnable(2848);
            GL11.glBegin(9);
            GIWbDs(var5);

            for (byte var7 = 0; var7 <= 90; var7 += 3) {
                double var8 = var7 * (float) (Math.PI / 180.0);
                GL11.glVertex2d(var0 + var4 + Math.sin(var8) * var4 * -1.0, var1 + var4 + Math.cos(var8) * var4 * -1.0);
            }

            for (int var14 = 90; var14 <= 180; var14 += 3) {
                double var17 = var14 * (float) (Math.PI / 180.0);
                GL11.glVertex2d(var0 + var4 + Math.sin(var17) * var4 * -1.0, var3 - var4 + Math.cos(var17) * var4 * -1.0);
            }

            if (var2 - var0 >= 4.5) {
                for (int var15 = 0; var15 <= 90; var15++) {
                    double var18 = var15 * (float) (Math.PI / 180.0);
                    GL11.glVertex2d(var2 - var4 + Math.sin(var18) * var4, var3 - var4 + Math.cos(var18) * var4);
                }

                for (int var16 = 90; var16 <= 180; var16++) {
                    double var19 = var16 * (float) (Math.PI / 180.0);
                    GL11.glVertex2d(var2 - var4 + Math.sin(var19) * var4, var1 + var4 + Math.cos(var19) * var4);
                }
            }

            GL11.glEnd();
            GL11.glEnable(3553);
            GL11.glDisable(3042);
            GL11.glDisable(2848);
            GL11.glEnable(3553);
            GL11.glPopAttrib();
            GL11.glPopMatrix();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    public static void renderItemStack(ItemStack var0, int var1, int var2) {
        renderItemStackWithClear(var0, var1, var2, true);
    }

    public static void renderItemStackWithClear(ItemStack var0, int var1, int var2, boolean var3) {
        if (var0 != null) {
            GlStateManager.pushMatrix();
            float var4 = mc.getRenderItem().zLevel;

            try {
                beginItemRenderState();
                if (var3) {
                    GlStateManager.depthMask(true);
                    GlStateManager.clear(256);
                } else {
                    GlStateManager.disableDepth();
                    GlStateManager.depthMask(false);
                }

                RenderHelper.enableStandardItemLighting();
                GlStateManager.pushMatrix();
                GlStateManager.scale(1.0F, 1.0F, -0.01F);
                mc.getRenderItem().zLevel = -150.0F;
                mc.getRenderItem().renderItemAndEffectIntoGUI(var0, var1, var2);
                GlStateManager.popMatrix();
                RenderHelper.disableStandardItemLighting();
                finishItemRenderState();
                GlStateManager.disableBlend();
            } finally {
                mc.getRenderItem().zLevel = var4;
                GlStateManager.popMatrix();
            }
        }
    }

    public static void finishItemRenderState() {
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GL11.glEnable(3553);
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void beginItemRenderState() {
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.disableLighting();
        GL11.glEnable(3553);
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static ResourceLocation loadRecoloredIcon(String var0, String var1, ResourceLocation var2) {
        try (InputStream var3 = Assets.open(var0)) {
            if (var3 != null) {
                BufferedImage var24 = ImageIO.read(var3);
                int var6 = var24.getWidth();
                int var7 = var24.getHeight();
                BufferedImage var8 = new BufferedImage(var6, var7, 2);

                for (int var9 = 0; var9 < var7; var9++) {
                    for (int var10 = 0; var10 < var6; var10++) {
                        int var11 = var24.getRGB(var10, var9) >>> 24 & 0xFF;
                        if (var11 > 0) {
                            var8.setRGB(var10, var9, var11 << 24 | 16777215);
                        }
                    }
                }

                return mc.getTextureManager().getDynamicTextureLocation(var1, new DynamicTexture(var8));
            } else {
                return var2;
            }
        } catch (Exception var23) {
            var23.printStackTrace();
            return var2;
        }
    }

    public static ResourceLocation getIconTexture(String var0) {
        ResourceLocation var1 = iconTextureCache.get(var0);
        if (var1 != null) {
            return var1;
        } else {
            String var2 = "jade_icon_" + var0.hashCode();
            ResourceLocation var3 = loadRecoloredIcon(var0, var2, null);
            if (var3 != null) {
                iconTextureCache.put(var0, var3);
            }

            return var3;
        }
    }

    public static void drawIconTexture(ResourceLocation var0, float var1, float var2, int var3, int var4) {
        if (var0 != null) {
            boolean var5 = GL11.glIsEnabled(2929);
            boolean var6 = GL11.glIsEnabled(3042);
            boolean var7 = GL11.glGetBoolean(2930);
            finishItemRenderState();
            mc.getTextureManager().bindTexture(var0);
            GL11.glTexParameteri(3553, 10241, 9729);
            GL11.glTexParameteri(3553, 10240, 9729);
            float var8 = (var4 >>> 24 & 0xFF) / 255.0F;
            float var9 = (var4 >> 16 & 0xFF) / 255.0F;
            float var10 = (var4 >> 8 & 0xFF) / 255.0F;
            float var11 = (var4 & 0xFF) / 255.0F;
            GlStateManager.color(var9, var10, var11, var8);
            GL11.glPushMatrix();
            GL11.glTranslatef(var1, var2, 0.0F);
            Gui.drawModalRectWithCustomSizedTexture(0, 0, 0.0F, 0.0F, var3, var3, var3, var3);
            GL11.glPopMatrix();
            restoreBlendDepthState(var5, var6, var7);
        }
    }

    public static void restoreBlendDepthState(boolean var0, boolean var1, boolean var2) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        if (var1) {
            GlStateManager.enableBlend();
        } else {
            GlStateManager.disableBlend();
        }

        if (var0) {
            GlStateManager.enableDepth();
        } else {
            GlStateManager.disableDepth();
        }

        GlStateManager.depthMask(var2);
    }

    private static void renderBlockFaceQuad(int var0, int var1, AxisAlignedBB var2, EnumFacing var3, int var4, int var5, int var6, int var7, boolean var8, boolean var9) {
        drawBlockFace(var2, var3, var0, var1, var8, var9);
    }
}
