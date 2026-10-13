package stone.module.other;

import stone.event.PacketReceiveEvent;
import stone.event.Phase;
import stone.event.RenderTickEvent;
import stone.event.Subscribe;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.util.ClientUtils;
import stone.util.font.FontManager;
import stone.util.font.IFont;
import stone.util.render.RenderUtils;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D.Float;
import java.awt.image.BufferedImage;
import java.util.List;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

public class LagDetect extends Module {
    private Setting highLatency;
    private Setting fontScale;
    private Setting ignoreLimbo;
    private static float ANRCZ = 0.5F;
    private static float positionYFraction = 0.08F;
    private static float alertX;
    private static float alertY;
    private static ResourceLocation resourceLocation;
    private static boolean AzjytO = true;
    private long lastPacketTimeMillis = 0L;
    private long jfQ = 0L;
    private long stallStartMillis = 0L;
    private long Znb = 0L;
    private long lastDurationRefreshMillis = 0L;

    public LagDetect() {
        super("LagDetect", "Detects packet loss", Category.OTHER, Subcategory.BASIC);
        this.highLatency = addNumber("High latency", "", 0.5F, 0.1F, 5.0F, 1);
        this.fontScale = addNumber("Font scale", "", 1.0F, 0.5F, 2.0F, 1);
        this.fontScale.setVisible(false);
        this.ignoreLimbo = addBoolean("Ignore limbo", "", true);
    }

    @Override
    public void onDisable(Context context) {
        this.lastPacketTimeMillis = 0L;
        this.resetLagState();
    }

    @Subscribe
    public void onPacketReceive(PacketReceiveEvent var1) {
        long var2 = System.currentTimeMillis();
        if (this.jfQ > 0L && var2 - this.lastPacketTimeMillis < this.QqQn()) {
            this.stallStartMillis = this.stallStartMillis == 0L ? var2 : this.stallStartMillis;
        } else {
            this.stallStartMillis = 0L;
        }

        this.lastPacketTimeMillis = System.currentTimeMillis();
    }

    @Override
    public void onUpdate() {
        if (!ClientUtils.isInWorld()) {
            this.lastPacketTimeMillis = System.currentTimeMillis();
            this.resetLagState();
        } else {
            if (mc.isSingleplayer() || this.ignoreLimbo.bool() && this.isInLimbo()) {
                this.lastPacketTimeMillis = System.currentTimeMillis();
                this.resetLagState();
            }
        }
    }

    @Subscribe
    public void onRenderTick(RenderTickEvent var1) {
        if (var1.phase == Phase.END && !mc.gameSettings.showDebugInfo && ClientUtils.isInWorld()) {
            if (!mc.isSingleplayer() && (!this.ignoreLimbo.bool() || !this.isInLimbo())) {
                long var2 = System.currentTimeMillis();
                long var4 = var2 - this.lastPacketTimeMillis;
                long var6 = this.QqQn();
                if (var4 >= var6 && this.jfQ == 0L) {
                    this.jfQ = this.lastPacketTimeMillis;
                    this.stallStartMillis = 0L;
                }

                if (this.stallStartMillis > 0L && var2 - this.stallStartMillis >= var6) {
                    this.resetLagState();
                }

                if (this.jfQ != 0L) {
                    long var8 = var2 - this.jfQ;
                    if (this.lastDurationRefreshMillis == 0L || var2 - this.lastDurationRefreshMillis >= 100L) {
                        this.Znb = roundUpToDisplayStep(var8);
                        this.lastDurationRefreshMillis = var2;
                    }

                    ScaledResolution var10 = new ScaledResolution(mc);
                    GKEA(var10);
                    long var11 = this.Znb > 0L ? this.Znb : roundUpToDisplayStep(var8);
                    this.drawAlert(alertX, alertY, var11, AzjytO);

                }
            }
        }
    }

    @Override
    public void onEnable(Context context) {
        this.lastPacketTimeMillis = System.currentTimeMillis();
        this.resetLagState();
    }

    @Override
    public String suffix() {
        return (int)Math.round(this.highLatency.number() * 1000.0) + "ms";
    }

    private IFont getHudFont() {
        return FontManager.getHudRenderer(FontManager.getDefaultHudFontName(), 1.0F);
    }

    private long QqQn() {
        return (long)(this.highLatency.number() * 1000.0);
    }

    private void resetLagState() {
        this.jfQ = 0L;
        this.stallStartMillis = 0L;
        this.Znb = 0L;
        this.lastDurationRefreshMillis = 0L;
    }

    private static long roundUpToDisplayStep(long var0) {
        return var0 <= 0L ? 0L : (var0 + 100L - 1L) / 100L * 100L;
    }

    public float[] drawAlertAt(float var1, float var2) {
        EVsgL(var1, var2);
        return this.drawAlert(var1, var2, 500L, false);
    }

    private float[] drawAlert(float var1, float var2, long var3, boolean var5) {
        IFont var6 = this.getHudFont();
        float var7 = (float)Math.max(0.5, Math.min(2.0, this.fontScale.number()));
        String var8 = "Lag Detected! (" + var3 + "ms)";
        float var9 = var6.getFontHeight();
        float var10 = Math.max(15.0F, var9 + 4.0F);
        float var11 = var2 + (var9 - var10) / 2.0F + 1.0F;
        float var12 = var2 + (var10 - var9) / 2.0F;
        float var13 = Math.max(6.0F, var10 * 0.32F);
        float var14 = var10 + var13 + var6.getStringWidth(var8);
        float var15 = Math.max(var10, var9);
        float var16 = var5 ? var1 - var14 * var7 / 2.0F : var1;
        GlStateManager.pushMatrix();
        GlStateManager.translate(var16, var2, 0.0F);
        GlStateManager.scale(var7, var7, 1.0F);
        drawAlarmIcon(0.0F, var11 - var2, var10, -45229);
        var6.drawString(var8, var10 + var13, var12 - var2, -41374, true);
        GlStateManager.popMatrix();
        return new float[]{var16, var2, var16 + var14 * var7, var2 + var15 * var7};
    }

    public static void EVsgL(float var0, float var1) {
        setAlertPosition(var0, var1, new ScaledResolution(mc));
    }

    private static void GKEA(ScaledResolution var0) {
        alertX = ANRCZ * var0.getScaledWidth();
        alertY = positionYFraction * var0.getScaledHeight();
    }

    private static void setAlertPosition(float var0, float var1, ScaledResolution var2) {
        alertX = var0;
        alertY = var1;
        ANRCZ = var0 / Math.max(1.0F, (float)var2.getScaledWidth());
        positionYFraction = var1 / Math.max(1.0F, (float)var2.getScaledHeight());
        AzjytO = false;
    }

    private static void drawAlarmIcon(float var0, float var1, float var2, int var3) {
        RenderUtils.drawIconTexture(getAlarmTexture(), var0 + 1.0F, var1 + 1.0F, Math.round(var2), -1610612736);
        RenderUtils.drawIconTexture(getAlarmTexture(), var0, var1, Math.round(var2), var3);
    }

    private static ResourceLocation getAlarmTexture() {
        if (resourceLocation != null) {
            return resourceLocation;
        } else {
            BufferedImage var0 = new BufferedImage(256, 256, 2);
            Graphics2D var1 = var0.createGraphics();
            var1.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            var1.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            var1.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            float var2 = 8.0F;
            float var3 = 256.0F - var2 * 2.0F;
            float var6 = 128.0F;
            var1.setColor(Color.WHITE);
            var1.fill(new Float(var2, var2, var3, var3));
            var1.setComposite(AlphaComposite.Clear);
            float var7 = var3 * 0.1F;
            var1.fill(new Float(var2 + var7, var2 + var7, var3 - var7 * 2.0F, var3 - var7 * 2.0F));
            var1.setComposite(AlphaComposite.SrcOver);
            var1.setColor(Color.WHITE);
            float var8 = var3 * 0.1F;
            float var9 = var6 - var8 / 2.0F;
            var1.fill(new java.awt.geom.RoundRectangle2D.Float(var9, var2 + var3 * 0.2F, var8, var3 * 0.37F, var8 * 0.35F, var8 * 0.35F));
            float var10 = var3 * 0.13F;
            var1.fill(new Float(var6 - var10 / 2.0F, var2 + var3 * 0.68F, var10, var10));
            var1.dispose();
            resourceLocation = mc.getTextureManager().getDynamicTextureLocation("jade_lag_detect_alarm", new DynamicTexture(var0));
            return resourceLocation;
        }
    }

    public boolean isInLimbo() {
        if (!ClientUtils.isInWorld()) {
            return false;
        } else {
            List var1 = ClientUtils.PxSw4();
            return var1.isEmpty() ? mc.theWorld.provider.getDimensionName().equals("The End") : false;
        }
    }
}
