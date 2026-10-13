package stone.module.render;

import stone.core.Color;
import stone.event.Phase;
import stone.event.RenderWorldLastEvent;
import stone.event.Subscribe;
import stone.event.TickEndEvent;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.ModuleManager;
import stone.module.Subcategory;
import stone.ui.Theme;
import stone.module.Setting;
import stone.module.combat.AimAssist;
import stone.util.ClientUtils;
import stone.util.render.RenderUtils;
import stone.util.render.RenderUtils$1;
import java.util.List;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

public class TargetESP extends Module {
    private static final String[] COLOR_MODE_LABELS = new String[]{"Custom", "Theme"};
    private static final int COLOR_MODE_CUSTOM = 0;
    private static final int COLOR_MODE_THEME = 1;
    private static final float[] xsS = new float[64];
    private static final float[] SvY = new float[64];
    private final Setting colorMode;
    private final Setting color;
    private final Setting themeOpacity;
    private final Setting thickness;
    private final double[] ringVerticalOffsets = new double[28];
    private final double[] Kr8 = new double[28];
    private final float[] ringAlphas = new float[28];
    private EntityPlayer entityPlayer;

    public TargetESP() {
        super("TargetESP", "Marks the current combat target", Category.VISUALS, Subcategory.ADVANCED);
        this.colorMode = addChoice("Color mode", "", List.of(COLOR_MODE_LABELS), 0);
        this.color = addColour("Color", "", Color.rgb(0x43E886, 1.0F));
        this.themeOpacity = addNumber("Theme opacity", "", 100.0F, 0.0F, 100.0F, 0);
        this.thickness = addNumber("Thickness", "", 2.0F, 1.0F, 5.0F, 1);
        this.color.visibleWhen(() -> this.getColorModeIndex() == COLOR_MODE_CUSTOM);
        this.themeOpacity.visibleWhen(() -> this.getColorModeIndex() == COLOR_MODE_THEME);
    }

    @Override
    public void onDisable(Context context) {
        this.entityPlayer = null;
    }

    @Subscribe(priority = Subscribe.Priority.HIGHEST)
    public void onTickEnd(TickEndEvent var1) {
        if (var1.phase == Phase.END && ClientUtils.isInWorld()) {
            AimAssist var2 = ModuleManager.get(AimAssist.class);
            this.entityPlayer = var2.GHPA();
        } else {
            this.entityPlayer = null;
        }
    }

    @Subscribe(priority = Subscribe.Priority.HIGH)
    public void onRenderWorldLast(RenderWorldLastEvent var1) {
        EntityPlayer var2 = this.entityPlayer;
        if (!this.isValidTarget(var2)) {
            this.entityPlayer = null;
        } else {
            this.qaQbf(var2, var1.partialTicks, this.getRenderColor());
        }
    }

    private boolean isValidTarget(EntityPlayer var1) {
        return var1 != null && ClientUtils.isInWorld() && var1.worldObj == mc.theWorld && var1.deathTime == 0 && !var1.isDead;
    }

    private int getColorModeIndex() {
        return this.colorMode.choice() == COLOR_MODE_THEME ? COLOR_MODE_THEME : COLOR_MODE_CUSTOM;
    }

    private int vDd3() {
        return this.getColorModeIndex() == 0 ? Math.round(this.color.colour().a() * 255.0F) : Math.max(0, Math.min(255, (int)Math.round(this.themeOpacity.number() * 2.55)));
    }

    private int getRenderColor() {
        int var1 = this.getColorModeIndex() == 1 ? (Theme.accent().argb() & 0xFFFFFF) : (this.color.colour().argb() & 0xFFFFFF);
        return ClientUtils.YVVZ(var1, this.vDd3());
    }

    private void qaQbf(EntityPlayer var1, float var2, int var3) {
        if (RenderUtils.isEntityInView(var1) && (var3 >>> 24 & 0xFF) != 0) {
            RenderManager var4 = mc.getRenderManager();
            double var5 = var1.lastTickPosX + (var1.posX - var1.lastTickPosX) * var2 - var4.viewerPosX;
            double var7 = var1.lastTickPosY + (var1.posY - var1.lastTickPosY) * var2 - var4.viewerPosY;
            double var9 = var1.lastTickPosZ + (var1.posZ - var1.lastTickPosZ) * var2 - var4.viewerPosZ;
            double var11 = Math.max(0.1, var1.getEntityBoundingBox().maxY - var1.getEntityBoundingBox().minY);
            double var13 = var1.width * 0.5 + 0.2;
            double var15 = System.currentTimeMillis() % 1800L / 1800.0 * Math.PI * 2.0;
            float var17 = (var3 >> 16 & 0xFF) / 255.0F;
            float var18 = (var3 >> 8 & 0xFF) / 255.0F;
            float var19 = (var3 & 0xFF) / 255.0F;
            float var20 = (var3 >>> 24 & 0xFF) / 255.0F;

            for (int var21 = 0; var21 < 28; var21++) {
                double var22 = var15 - var21 * 0.04;
                double var24 = (Math.sin(var22) + 1.0) * 0.5 * var11;
                float var26 = var21 / 27.0F;
                float var27 = 1.0F - var26;
                this.ringVerticalOffsets[var21] = var24;
                this.Kr8[var21] = var13 + Math.sin(var24 / var11 * Math.PI) * 0.05;
                this.ringAlphas[var21] = var27 * var27 * var20 * 0.72F;
            }

            GL11.glPushMatrix();
            GL11.glPushAttrib(1048575);
            RenderUtils$1 var38 = null;

            try {
                var38 = RenderUtils.uyB6();
                GL11.glTranslated(var5, var7, var9);
                GL11.glEnable(3042);
                GL11.glBlendFunc(770, 771);
                GL11.glDisable(3553);
                GL11.glDisable(2929);
                GL11.glDisable(2884);
                GL11.glDepthMask(false);
                GL11.glEnable(2848);
                GL11.glHint(3154, 4354);
                GL11.glShadeModel(7425);
                GL11.glBegin(4);

                for (int var39 = 0; var39 < 27; var39++) {
                    double var23 = this.ringVerticalOffsets[var39];
                    double var25 = this.ringVerticalOffsets[var39 + 1];
                    double var41 = this.Kr8[var39];
                    double var29 = this.Kr8[var39 + 1];
                    float var31 = this.ringAlphas[var39];
                    float var32 = this.ringAlphas[var39 + 1];

                    for (int var33 = 0; var33 < 64; var33++) {
                        int var34 = var33 + 1 == 64 ? 0 : var33 + 1;
                        this.CWWkP(var33, var23, var41, var17, var18, var19, var31);
                        this.CWWkP(var34, var23, var41, var17, var18, var19, var31);
                        this.CWWkP(var34, var25, var29, var17, var18, var19, var32);
                        this.CWWkP(var33, var23, var41, var17, var18, var19, var31);
                        this.CWWkP(var34, var25, var29, var17, var18, var19, var32);
                        this.CWWkP(var33, var25, var29, var17, var18, var19, var32);
                    }
                }

                GL11.glEnd();
                GL11.glLineWidth((float)this.thickness.number());
                GL11.glColor4f(var17, var18, var19, var20);
                GL11.glBegin(2);

                for (int var40 = 0; var40 < 64; var40++) {
                    GL11.glVertex3d(xsS[var40] * this.Kr8[0], this.ringVerticalOffsets[0], SvY[var40] * this.Kr8[0]);
                }

                GL11.glEnd();
            } finally {
                RenderUtils.restoreLightmapState(var38);
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                GL11.glLineWidth(1.0F);
                GL11.glPopAttrib();
                GL11.glPopMatrix();
            }
        }
    }

    private void CWWkP(int var1, double var2, double var4, float var6, float var7, float var8, float var9) {
        GL11.glColor4f(var6, var7, var8, var9);
        GL11.glVertex3d(xsS[var1] * var4, var2, SvY[var1] * var4);
    }

    static {
        for (int var0 = 0;
            var0 < 64;
            var0++
        ) {
            double var1 = (Math.PI * 2) * var0 / 64.0;
            xsS[var0] = (float)Math.cos(var1);
            SvY[var0] = (float)Math.sin(var1);
        }
    }
}
