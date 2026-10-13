package stone.module.render;

import stone.event.AttackEntityEvent;
import stone.event.Subscribe;
import stone.game.CombatTarget;
import stone.core.Time;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.render.Icon;
import stone.render.Renderer;
import stone.ui.Client;
import stone.ui.Theme;
import stone.util.font.FontManager;
import stone.util.font.IFont;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderSkeleton;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public final class TargetHud extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private final Setting offsetX = addNumber("Offset X", "Pixels right of the screen centre", 0.0f, -600.0f, 600.0f, 0);
    private final Setting offsetY = addNumber("Offset Y", "Pixels below the screen centre", 50.0f, -400.0f, 400.0f, 0);
    private final Setting scale = addNumber("Scale", "Size of the panel", 1.0f, 0.5f, 2.0f, 2);
    private final Setting showOutcome = addBoolean("Win or loss", "Display Winning: or Losing: calculation", true);
    private final Setting particles = addBoolean("Particles", "Spawn blood particles when target takes damage", true);

    private EntityLivingBase target;
    private long lastAttackTime = 0L;
    private final long TIMEOUT_MS = 1000L;

    private double scaleValue = 0.0;
    private double scaleStart = 0.0;
    private double scaleTarget = 0.0;
    private long scaleStartTime = 0L;
    private long scaleDuration = 850L;
    private boolean scaleElastic = true;

    private double healthValue = 0.0;
    private double healthStart = 0.0;
    private double healthTarget = 0.0;
    private long healthStartTime = 0L;
    private final long healthDuration = 250L;

    private long lastAdvantageCalcTime = 0L;
    private float cachedAdvantage = 0.0f;

    private int lastHurtTime = 0;

    private static final class Particle {
        float x, y;
        float vx, vy;
        float alpha = 1.0f;
        float size;

        Particle(float x, float y, float vx, float vy, float size) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
        }

        boolean update(float dt) {
            x += vx * dt * 60.0f;
            y += vy * dt * 60.0f;
            alpha -= dt * 2.2f;
            return alpha > 0.0f;
        }
    }

    private final List<Particle> particleList = new ArrayList<>();

    public TargetHud() {
        super("TargetHud", "OpenRise Modern TargetHUD", Category.VISUALS, Subcategory.BASIC);
        setIcon(Icon.TARGET);
        setEnabled(true, null);
    }

    @Override
    public void onEnable(Context context) {
        scaleValue = 0.0;
        scaleTarget = 0.0;
        healthValue = 0.0;
        target = null;
        particleList.clear();
    }

    @Subscribe
    public void onAttack(AttackEntityEvent event) {
        if (event.entity instanceof EntityLivingBase living) {
            this.target = living;
            this.lastAttackTime = System.currentTimeMillis();
        }
    }

    private void updateScaleAnimation(boolean targetPresent) {
        double dest = targetPresent ? 1.0 : 0.0;
        long now = System.currentTimeMillis();
        if (dest != scaleTarget) {
            scaleTarget = dest;
            scaleStart = scaleValue;
            scaleStartTime = now;
            scaleDuration = targetPresent ? 850L : 400L;
            scaleElastic = targetPresent;
        }
        long elapsed = now - scaleStartTime;
        double progress = Math.min(1.0, Math.max(0.0, (double) elapsed / scaleDuration));
        double factor;
        if (scaleElastic) {

            if (progress <= 0.0) {
                factor = 0.0;
            } else if (progress >= 1.0) {
                factor = 1.0;
            } else {
                factor = Math.pow(2.0, -10.0 * progress) * Math.sin((progress * 10.0 - 0.75) * 2.0943951023931953) * 0.5 + 1.0;
            }
        } else {

            factor = 2.70158 * progress * progress * progress - 1.70158 * progress * progress;
        }
        scaleValue = scaleStart + (scaleTarget - scaleStart) * factor;
        if (progress >= 1.0) {
            scaleValue = scaleTarget;
        }
    }

    private void updateHealthAnimation(double targetHpBarWidth) {
        long now = System.currentTimeMillis();
        if (Math.abs(targetHpBarWidth - healthTarget) > 0.001) {
            healthTarget = targetHpBarWidth;
            healthStart = healthValue;
            healthStartTime = now;
        }
        long elapsed = now - healthStartTime;
        double progress = Math.min(1.0, Math.max(0.0, (double) elapsed / healthDuration));

        double d2 = progress - 1.0;
        double factor = 1.0 + d2 * d2 * d2 * d2 * d2;
        healthValue = healthStart + (healthTarget - healthStart) * factor;
        if (progress >= 1.0) {
            healthValue = healthTarget;
        }
    }

    private float calculateDamage(ItemStack stack, Entity entity, Entity opponent) {
        float f = 1.0F;
        if (stack != null) {
            Item item = stack.getItem();
            if (item instanceof ItemSword sword) {
                f = sword.getDamageVsEntity();
            } else if (item instanceof ItemTool tool) {
                f = tool.getToolMaterial().getDamageVsEntity();
            }

            int i = EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack);
            if (i > 0) {
                f += i * 1.25F;
            }
        }

        if (entity instanceof EntityLivingBase living) {
            if (living.getActivePotionEffect(Potion.damageBoost) != null) {
                int j = living.getActivePotionEffect(Potion.damageBoost).getAmplifier() + 1;
                f += j * 3.0F;
            }
        }

        if (opponent instanceof EntityLivingBase livingOpponent) {
            float totalArmor = livingOpponent.getTotalArmorValue();
            float f2 = Math.min(totalArmor * 0.04F, 0.8F);
            f *= 1.0F - f2;
            int k = getProtectionPoints(livingOpponent);
            int l = Math.min(20, (int) Math.ceil(Math.min(25, k) * 0.75F));
            if (l > 0) {
                f *= 1.0F - l * 0.04F;
            }

            if (livingOpponent.getActivePotionEffect(Potion.resistance) != null) {
                int i1 = livingOpponent.getActivePotionEffect(Potion.resistance).getAmplifier() + 1;
                float f3 = Math.min(i1 * 0.2F, 0.8F);
                f *= 1.0F - f3;
            }
        }

        if (!Float.isFinite(f)) {
            f = 1.0F;
        }
        return Math.max(0.01F, f);
    }

    private int getProtectionPoints(EntityLivingBase living) {
        int i = 0;
        for (int j = 0; j < 4; j++) {
            ItemStack stack = living.getCurrentArmor(j);
            int k = stack != null ? EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, stack) : 0;
            if (k > 0) {
                i += (int) Math.floor((6 + k * k) * 0.75F / 3.0F);
            }
        }
        return i;
    }

    private float getAdvantage(EntityLivingBase currentTarget) {
        long now = System.currentTimeMillis();
        if (now - lastAdvantageCalcTime > 200L) {
            cachedAdvantage = calculateAdvantage(currentTarget);
            lastAdvantageCalcTime = now;
        }
        return cachedAdvantage;
    }

    private float calculateAdvantage(EntityLivingBase currentTarget) {
        if (mc.thePlayer == null || currentTarget == null) {
            return 0.0F;
        }

        float f = Math.max(0.1F, mc.thePlayer.getHealth());
        float f2 = Math.max(0.1F, currentTarget.getHealth());
        float f3 = calculateDamage(mc.thePlayer.getHeldItem(), mc.thePlayer, currentTarget);
        float f4 = calculateDamage(currentTarget.getHeldItem(), currentTarget, mc.thePlayer);
        if (f3 <= 0.01F && f4 <= 0.01F) {
            return 0.0F;
        }

        int i;
        for (i = 0; f > 0.0F && f2 > 0.0F && i < 256; i++) {
            f2 -= f3;
            f -= f4;
        }

        return i >= 256 ? f3 - f4 : f - f2;
    }

    @Override
    public void onRender(Context context) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        boolean guiOpen = mc.currentScreen instanceof GuiChat || Client.gui().visible();
        EntityLivingBase activeTarget = this.target;

        if (activeTarget == null || !activeTarget.isEntityAlive()) {
            if (mc.pointedEntity instanceof EntityLivingBase living && living.isEntityAlive()) {
                activeTarget = living;
                this.target = living;
                this.lastAttackTime = System.currentTimeMillis();
            } else if (mc.objectMouseOver != null && mc.objectMouseOver.entityHit instanceof EntityLivingBase living && living.isEntityAlive()) {
                activeTarget = living;
                this.target = living;
                this.lastAttackTime = System.currentTimeMillis();
            }
        }

        if (guiOpen && (activeTarget == null || !activeTarget.isEntityAlive())) {
            activeTarget = mc.thePlayer;
        }

        boolean targetPresent = activeTarget != null && activeTarget.isEntityAlive()
                && (guiOpen || (System.currentTimeMillis() - lastAttackTime <= 2000L));

        updateScaleAnimation(targetPresent);

        if (scaleValue <= 0.001) {
            return;
        }

        if (particles.bool() && activeTarget != null && activeTarget.hurtTime > 0 && lastHurtTime == 0) {
            for (int i = 0; i < 6; i++) {
                float vx = (float) ((Math.random() - 0.5) * 3.5);
                float vy = (float) ((Math.random() - 0.5) * 3.5);
                particleList.add(new Particle(24.0f, 24.0f, vx, vy, (float) (Math.random() * 2.0 + 1.5)));
            }
        }
        lastHurtTime = activeTarget != null ? activeTarget.hurtTime : 0;

        Iterator<Particle> it = particleList.iterator();
        while (it.hasNext()) {
            if (!it.next().update(context.deltaTime)) {
                it.remove();
            }
        }

        ScaledResolution sr = new ScaledResolution(mc);
        float scaledWidth = sr.getScaledWidth();
        float scaledHeight = sr.getScaledHeight();

        Renderer.instance().pause();
        try {
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.pushMatrix();
            try {
                GlStateManager.loadIdentity();
                GlStateManager.ortho(0.0D, (double) scaledWidth, (double) scaledHeight, 0.0D, 1000.0D, 3000.0D);

                GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                GlStateManager.pushMatrix();
                try {
                    GlStateManager.loadIdentity();
                    GlStateManager.translate(0.0F, 0.0F, -2000.0F);

                    GlStateManager.enableBlend();
                    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                    GlStateManager.enableAlpha();
                    GlStateManager.alphaFunc(516, 0.05F);
                    GlStateManager.enableTexture2D();
                    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

                    renderModernTargetInfo(activeTarget, scaledWidth, scaledHeight);
                } finally {
                    GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                    GlStateManager.popMatrix();
                }
            } finally {
                GlStateManager.matrixMode(GL11.GL_PROJECTION);
                GlStateManager.popMatrix();
            }
        } catch (Throwable t) {
            stone.core.Log.error("TargetHud", t, "Error rendering TargetHud");
        } finally {
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            Renderer.instance().resume();
        }
    }

    private void renderModernTargetInfo(EntityLivingBase entity, float scaledWidth, float scaledHeight) {
        if (entity == null) {
            return;
        }

        float b0 = 32.0f;
        float d8 = entity.hurtTime > 0 ? entity.hurtTime * 0.5f : 0.0f;
        float d9 = d8 / 2.0f;

        IFont lightFont = FontManager.getLineSeedRegular(9.0f);
        IFont mediumFont = FontManager.getLineSeedBold(9.0f);

        String s1 = entity.getName();
        float adv = getAdvantage(entity);
        boolean winning = adv > 0.0f;
        String s2 = winning ? "Winning:" : "Losing:";

        float d2 = mediumFont.getStringWidth(s1);
        float d3 = lightFont.getStringWidth(s2);

        float healthVal = Math.max(0.0f, Math.min(entity.getHealth(), entity.getMaxHealth()));
        String hpText = String.format(Locale.ROOT, "%.1f", healthVal);
        float d5 = mediumFont.getStringWidth(hpText);

        float d6 = Math.max(d3 + d2 + 35.0f - d5, 65.0f);
        double maxHp = entity.getMaxHealth() > 0.0f ? entity.getMaxHealth() : 20.0f;
        updateHealthAnimation((healthVal / maxHp) * d6);
        float d7 = (float) healthValue;

        float d10 = 48.0f + d6 + 4.0f + d5 + 8.0f;
        float d11 = 48.0f;

        float panelScale = scale.number();
        float d0 = (scaledWidth / 2.0f) + offsetX.number() - (d10 * panelScale / 2.0f);
        float d1 = (scaledHeight / 2.0f) + offsetY.number();

        float d12 = (float) scaleValue;

        GlStateManager.pushMatrix();
        try {
            float centerX = d0 + (d10 * panelScale) / 2.0f;
            float centerY = d1 + (d11 * panelScale) / 2.0f;
            GlStateManager.translate(centerX, centerY, 0.0f);
            GlStateManager.scale(d12 * panelScale, d12 * panelScale, 1.0f);
            GlStateManager.translate(-centerX, -centerY, 0.0f);

            drawRoundedRect(d0 - 2.0f, d1 - 2.0f, d10 + 4.0f, d11 + 4.0f, 10.0f, new Color(0, 0, 0, 50).getRGB());
            drawRoundedRect(d0 - 1.0f, d1 - 1.0f, d10 + 2.0f, d11 + 2.0f, 9.0f, new Color(0, 0, 0, 80).getRGB());
            drawRoundedRect(d0, d1, d10 - 1.0f, d11, 8.0f, new Color(13, 15, 20, 205).getRGB());
            drawRoundedOutline(d0, d1, d10 - 1.0f, d11, 8.0f, 1.0f, new Color(255, 255, 255, 22).getRGB());

            float headX = d0 + 8.0f + d9;
            float headY = d1 + 8.0f + d9;
            float headSize = b0 - d8;
            renderTargetHead(entity, headX, headY, headSize, d8);

            int s2Color = showOutcome.bool() ? (winning ? 0xFF55FF55 : 0xFFFF5555) : 0xFFFFFFFF;
            lightFont.drawString(s2, d0 + 8.0f + b0 + 7.0f, d1 + 8.0f + 4.0f + 2.0f, s2Color, true);
            mediumFont.drawString(s1, d0 + 8.0f + b0 + 7.0f + d3 + 3.0f, d1 + 8.0f + 4.0f + 2.5f, 0xFFFFFFFF, true);

            float barX = d0 + 8.0f + b0 + 7.0f;
            float barY = d1 + 8.0f + b0 - 4.0f - 7.0f;
            drawRoundedRect(barX, barY, d6, 6.0f, 3.0f, new Color(22, 25, 34, 160).getRGB());

            if (d7 > 0.5f) {
                int color1 = Theme.accent().packed();
                int color2 = stone.core.Color.mix(Theme.accent(), stone.core.Color.WHITE, 0.35f).packed();
                drawRoundedGradientRect(barX, barY, Math.min(d6, d7), 6.0f, 3.0f, color1, color2);
            }

            mediumFont.drawString(hpText, barX + d6 + 4.0f, d1 + 8.0f + b0 - 4.0f - 8.0f, 0xFFDDDDDD, true);

            if (!particleList.isEmpty()) {
                for (Particle p : particleList) {
                    int pColor = new Color(0.9f, 0.15f, 0.15f, p.alpha * 0.85f).getRGB();
                    drawRect(d0 + 8.0f + p.x, d1 + 8.0f + p.y, d0 + 8.0f + p.x + p.size, d1 + 8.0f + p.y + p.size, pColor);
                }
            }
        } finally {
            GlStateManager.popMatrix();
        }
    }

    private void renderTargetHead(EntityLivingBase entity, float x, float y, float size, float hurtShrink) {
        ResourceLocation skin;
        if (entity instanceof AbstractClientPlayer player) {
            skin = player.getLocationSkin();
        } else {
            skin = new ResourceLocation("textures/entity/steve.png");
        }

        drawRoundedRect(x - 0.5f, y - 0.5f, size + 1.0f, size + 1.0f, 4.0f, new Color(8, 10, 14, 180).getRGB());

        mc.getTextureManager().bindTexture(skin);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.enableAlpha();
        if (hurtShrink > 0.0f) {
            float hurtFactor = Math.min(1.0f, hurtShrink / 4.5f);
            GlStateManager.color(1.0f, 1.0f - hurtFactor * 0.6f, 1.0f - hurtFactor * 0.6f, 1.0f);
        } else {
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        }

        drawTexture(skin, x, y, 8.0F, 8.0F, 8.0F, 8.0F, 64.0F, 64.0F, size, size);

        drawTexture(skin, x, y, 40.0F, 8.0F, 8.0F, 8.0F, 64.0F, 64.0F, size, size);

        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        drawRoundedOutline(x - 0.5f, y - 0.5f, size + 1.0f, size + 1.0f, 4.0f, 1.0f, new Color(0, 0, 0, 70).getRGB());
    }

    public static void drawTexture(ResourceLocation location, float x, float y, float u, float v, float uW, float vH, float texW, float texH, float width, float height) {
        mc.getTextureManager().bindTexture(location);
        GL11.glTexParameteri(3553, 10242, 33071);
        GL11.glTexParameteri(3553, 10243, 33071);
        GL11.glTexParameteri(3553, 10241, 9728);
        GL11.glTexParameteri(3553, 10240, 9728);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.enableAlpha();
        float f = u / texW;
        float f1 = v / texH;
        float f2 = (u + uW) / texW;
        float f3 = (v + vH) / texH;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_TEX);
        wr.pos(x, y + height, 0.0).tex(f, f3).endVertex();
        wr.pos(x + width, y + height, 0.0).tex(f2, f3).endVertex();
        wr.pos(x + width, y, 0.0).tex(f2, f1).endVertex();
        wr.pos(x, y, 0.0).tex(f, f1).endVertex();
        tessellator.draw();
        GlStateManager.disableBlend();
    }

    public static void drawRect(float left, float top, float right, float bottom, int color) {
        if (left < right) {
            float temp = left;
            left = right;
            right = temp;
        }
        if (top < bottom) {
            float temp = top;
            top = bottom;
            bottom = temp;
        }
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);
        wr.begin(7, DefaultVertexFormats.POSITION);
        wr.pos((double) left, (double) bottom, 0.0D).endVertex();
        wr.pos((double) right, (double) bottom, 0.0D).endVertex();
        wr.pos((double) right, (double) top, 0.0D).endVertex();
        wr.pos((double) left, (double) top, 0.0D).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, int color) {
        if (width <= 0.0f || height <= 0.0f) return;
        float maxRadius = Math.min(width, height) / 2.0f;
        radius = Math.min(radius, maxRadius);
        float x2 = x + width;
        float y2 = y + height;

        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);

        GL11.glBegin(GL11.GL_POLYGON);
        for (int i = 0; i <= 90; i += 15) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x2 - radius + Math.sin(rad) * radius, y + radius - Math.cos(rad) * radius);
        }
        for (int i = 90; i <= 180; i += 15) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x2 - radius + Math.sin(rad) * radius, y2 - radius - Math.cos(rad) * radius);
        }
        for (int i = 180; i <= 270; i += 15) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x + radius + Math.sin(rad) * radius, y2 - radius - Math.cos(rad) * radius);
        }
        for (int i = 270; i <= 360; i += 15) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x + radius + Math.sin(rad) * radius, y + radius - Math.cos(rad) * radius);
        }
        GL11.glEnd();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawRoundedGradientRect(float x, float y, float width, float height, float radius, int color1, int color2) {
        if (width <= 0.0f || height <= 0.0f) return;
        float maxRadius = Math.min(width, height) / 2.0f;
        radius = Math.min(radius, maxRadius);
        float x2 = x + width;
        float y2 = y + height;

        float a1 = (float) (color1 >> 24 & 255) / 255.0F;
        float r1 = (float) (color1 >> 16 & 255) / 255.0F;
        float g1 = (float) (color1 >> 8 & 255) / 255.0F;
        float b1 = (float) (color1 & 255) / 255.0F;

        float a2 = (float) (color2 >> 24 & 255) / 255.0F;
        float r2 = (float) (color2 >> 16 & 255) / 255.0F;
        float g2 = (float) (color2 >> 8 & 255) / 255.0F;
        float b2 = (float) (color2 & 255) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glShadeModel(GL11.GL_SMOOTH);

        GL11.glBegin(GL11.GL_POLYGON);
        for (int i = 0; i <= 90; i += 15) {
            double rad = Math.toRadians(i);
            double vx = x2 - radius + Math.sin(rad) * radius;
            double vy = y + radius - Math.cos(rad) * radius;
            float t = (float) ((vx - x) / Math.max(1.0f, width));
            GL11.glColor4f(r1 + (r2 - r1) * t, g1 + (g2 - g1) * t, b1 + (b2 - b1) * t, a1 + (a2 - a1) * t);
            GL11.glVertex2d(vx, vy);
        }
        for (int i = 90; i <= 180; i += 15) {
            double rad = Math.toRadians(i);
            double vx = x2 - radius + Math.sin(rad) * radius;
            double vy = y2 - radius - Math.cos(rad) * radius;
            float t = (float) ((vx - x) / Math.max(1.0f, width));
            GL11.glColor4f(r1 + (r2 - r1) * t, g1 + (g2 - g1) * t, b1 + (b2 - b1) * t, a1 + (a2 - a1) * t);
            GL11.glVertex2d(vx, vy);
        }
        for (int i = 180; i <= 270; i += 15) {
            double rad = Math.toRadians(i);
            double vx = x + radius + Math.sin(rad) * radius;
            double vy = y2 - radius - Math.cos(rad) * radius;
            float t = (float) ((vx - x) / Math.max(1.0f, width));
            GL11.glColor4f(r1 + (r2 - r1) * t, g1 + (g2 - g1) * t, b1 + (b2 - b1) * t, a1 + (a2 - a1) * t);
            GL11.glVertex2d(vx, vy);
        }
        for (int i = 270; i <= 360; i += 15) {
            double rad = Math.toRadians(i);
            double vx = x + radius + Math.sin(rad) * radius;
            double vy = y + radius - Math.cos(rad) * radius;
            float t = (float) ((vx - x) / Math.max(1.0f, width));
            GL11.glColor4f(r1 + (r2 - r1) * t, g1 + (g2 - g1) * t, b1 + (b2 - b1) * t, a1 + (a2 - a1) * t);
            GL11.glVertex2d(vx, vy);
        }
        GL11.glEnd();

        GL11.glShadeModel(GL11.GL_FLAT);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawRoundedOutline(float x, float y, float width, float height, float radius, float lineWidth, int color) {
        if (width <= 0.0f || height <= 0.0f) return;
        float maxRadius = Math.min(width, height) / 2.0f;
        radius = Math.min(radius, maxRadius);
        float x2 = x + width;
        float y2 = y + height;

        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glLineWidth(lineWidth);
        GlStateManager.color(r, g, b, a);

        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i <= 90; i += 15) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x2 - radius + Math.sin(rad) * radius, y + radius - Math.cos(rad) * radius);
        }
        for (int i = 90; i <= 180; i += 15) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x2 - radius + Math.sin(rad) * radius, y2 - radius - Math.cos(rad) * radius);
        }
        for (int i = 180; i <= 270; i += 15) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x + radius + Math.sin(rad) * radius, y2 - radius - Math.cos(rad) * radius);
        }
        for (int i = 270; i <= 360; i += 15) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x + radius + Math.sin(rad) * radius, y + radius - Math.cos(rad) * radius);
        }
        GL11.glEnd();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
}
