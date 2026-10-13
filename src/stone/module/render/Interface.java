package stone.module.render;

import stone.event.Phase;
import stone.event.RenderTickEvent;
import stone.event.Subscribe;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.ModuleManager;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.module.client.ClickGuiModule;
import stone.render.Icon;
import stone.ui.Theme;
import stone.util.font.FontManager;
import stone.util.font.IFont;
import stone.util.render.RoundedRect;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public final class Interface extends Module {
    public static final class ArrayListEntry {
        public final Module module;
        public float x = 5000.0f;
        public float y = 0.0f;
        public float targetX = 5000.0f;
        public float targetY = 0.0f;
        public float animationTime = 0.0f;
        public float nameWidth = 0.0f;
        public float tagWidth = 0.0f;
        public Color color = Color.WHITE;
        public String displayName = "";
        public String displayTag = "";
        public boolean hasTag = false;

        public ArrayListEntry(Module module) {
            this.module = module;
        }

        public float getTotalWidth() {
            return this.nameWidth + this.tagWidth;
        }
    }

    private static final class PotionEffectEntry {
        final int potionId;
        final String name;
        final String duration;
        final float nameWidth;
        final float durationWidth;

        PotionEffectEntry(int potionId, String name, String duration, float nameWidth, float durationWidth) {
            this.potionId = potionId;
            this.name = name;
            this.duration = duration;
            this.nameWidth = nameWidth;
            this.durationWidth = durationWidth;
        }
    }

    private final Setting modulesToShow;
    private final Setting colorMode;
    private final Setting themeChoice;
    private final Setting suffix;
    private final Setting lowercase;
    private final Setting removeSpaces;
    private final Setting dropShadow;
    private final Setting background;
    private final Setting backgroundAlpha;
    private final Setting potionEffects;

    private final List<ArrayListEntry> allEntries = new ArrayList<>();
    private final List<ArrayListEntry> activeEntries = new ArrayList<>();
    private long lastFrameTime = 0L;
    private final float moduleSpacing = 12.0f;
    private final float edgeOffset = 10.0f;

    public Interface() {
        super("Interface", "Module list and active potion effects", Category.HUD, Subcategory.BASIC);
        setIcon(Icon.MONITOR);

        modulesToShow = addChoice("Modules to Show", "Filter modules in array list", List.of("All", "Exclude render", "Only bound"), 0);
        colorMode = addChoice("Color Mode", "Color mode for array list", List.of("Fade", "Breathe", "Static"), 0);
        themeChoice = addChoice("Theme", "Color palette for HUD", List.of("Blend", "Hyper", "Bubblegum", "Water", "Purple", "Accent"), 0);
        suffix = addBoolean("Suffix", "Display module suffix or mode", true);
        lowercase = addBoolean("Lowercase", "Lowercase module names in array list", false);
        removeSpaces = addBoolean("Remove Spaces", "Remove spaces from module names", false);
        dropShadow = addBoolean("Drop Shadow", "Render font with shadow", true);
        background = addChoice("Background", "Background behind modules", List.of("Normal", "None"), 0);
        backgroundAlpha = addNumber("Background Alpha", "Opacity of module backgrounds", 110.0f, 0.0f, 255.0f, 0);
        potionEffects = addBoolean("Potion Effects", "Display active potion effects", true);

        setEnabled(true, null);
        rebuildEntries();
    }

    private void rebuildEntries() {
        allEntries.clear();
        for (Module mod : ModuleManager.all()) {
            if (mod instanceof ClickGuiModule || mod instanceof Interface) {
                continue;
            }
            ArrayListEntry entry = new ArrayListEntry(mod);
            if (mod.enabled()) {
                entry.animationTime = 10.0f;
            }
            allEntries.add(entry);
        }
    }

    private boolean shouldShow(ArrayListEntry entry, String filter) {
        if (entry.module instanceof ClickGuiModule || entry.module instanceof Interface) {
            return false;
        }
        return switch (filter) {
            case "Exclude render" -> entry.module.category != Category.VISUALS && entry.module.category != Category.HUD;
            case "Only bound" -> entry.module.key() != 0;
            default -> true;
        };
    }

    private Color getThemeColor1() {
        return switch (themeChoice.choiceName()) {
            case "Hyper" -> new Color(236, 110, 173);
            case "Bubblegum" -> new Color(243, 145, 216);
            case "Water" -> new Color(12, 232, 199);
            case "Purple" -> new Color(142, 45, 226);
            case "Accent" -> new Color(Theme.accent().argb());
            default -> new Color(71, 148, 253);
        };
    }

    private Color getThemeColor2() {
        return switch (themeChoice.choiceName()) {
            case "Hyper" -> new Color(52, 148, 230);
            case "Bubblegum" -> new Color(152, 165, 243);
            case "Water" -> new Color(12, 163, 232);
            case "Purple" -> new Color(74, 0, 224);
            case "Accent" -> new Color(Theme.accentDim().argb());
            default -> new Color(71, 253, 160);
        };
    }

    public static Color interpolate(Color c1, Color c2, double factor) {
        factor = Math.max(0.0, Math.min(1.0, factor));
        double inv = 1.0 - factor;
        int r = (int) (c1.getRed() * factor + c2.getRed() * inv);
        int g = (int) (c1.getGreen() * factor + c2.getGreen() * inv);
        int b = (int) (c1.getBlue() * factor + c2.getBlue() * inv);
        int a = (int) (c1.getAlpha() * factor + c2.getAlpha() * inv);
        return new Color(r, g, b, a);
    }

    private static String toRomanNumeral(int n) {
        if (n < 1) return String.valueOf(n);
        if (n > 255) n = 255;
        StringBuilder sb = new StringBuilder();
        int[] values = {100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] numerals = {"C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        for (int i = 0; i < values.length; i++) {
            while (n >= values[i]) {
                sb.append(numerals[i]);
                n -= values[i];
            }
        }
        return sb.toString();
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

    public static void drawTexture(ResourceLocation location, float x, float y, int u, int v, int uW, int vH, int texW, int texH, float width, float height) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(location);
        GL11.glTexParameteri(3553, 10242, 33071);
        GL11.glTexParameteri(3553, 10243, 33071);
        GL11.glTexParameteri(3553, 10241, 9728);
        GL11.glTexParameteri(3553, 10240, 9728);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.enableAlpha();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        float f = (float) u / texW;
        float f1 = (float) v / texH;
        float f2 = (float) (u + uW) / texW;
        float f3 = (float) (v + vH) / texH;
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

    private static void drawPotionIcon(int potionId, float x, float y, float size) {
        if (potionId < 0 || potionId >= Potion.potionTypes.length) return;
        Potion potion = Potion.potionTypes[potionId];
        if (potion == null || !potion.hasStatusIcon()) return;
        ResourceLocation loc = GuiContainer.inventoryBackground;
        int iconIndex = potion.getStatusIconIndex();
        int u = iconIndex % 8 * 18;
        int v = 198 + iconIndex / 8 * 19;
        drawTexture(loc, x, y, u, v, 18, 18, 256, 256, size, size);
    }

    @Override
    public void onRender(Context context) {
        doRender();
    }

    private void doRender() {
        if (mc.gameSettings != null && mc.gameSettings.showDebugInfo) {
            return;
        }

        long now = System.currentTimeMillis();
        float elapsedMs = Math.min(100.0f, lastFrameTime > 0 ? (now - lastFrameTime) : 16.0f);
        lastFrameTime = now;

        ScaledResolution sr = new ScaledResolution(mc);
        float scaledWidth = sr.getScaledWidth();
        float scaledHeight = sr.getScaledHeight();

        stone.render.Renderer.instance().pause();
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
                    GlStateManager.alphaFunc(516, 0.1F);
                    GlStateManager.enableTexture2D();
                    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

                    renderHud(scaledWidth, scaledHeight, now, elapsedMs);
                } finally {
                    GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                    GlStateManager.popMatrix();
                }
            } finally {
                GlStateManager.matrixMode(GL11.GL_PROJECTION);
                GlStateManager.popMatrix();
            }
        } catch (Throwable t) {
            stone.core.Log.error("Interface", t, "Error during interface render");
        } finally {
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            stone.render.Renderer.instance().resume();
        }
    }

    private void renderHud(float scaledWidth, float scaledHeight, long now, float elapsedMs) {

        if (allEntries.size() != ModuleManager.all().size() - 2) {
            rebuildEntries();
        }

        IFont arrayListFont = FontManager.getLineSeedRegular(9.0f);
        IFont regular18Font = FontManager.getLineSeedRegular(9.0f);

        boolean hasSuffix = suffix.bool();
        boolean isLower = lowercase.bool();
        boolean noSpaces = removeSpaces.bool();
        boolean withDropShadow = dropShadow.bool();
        boolean isNormalBg = "Normal".equals(background.choiceName());
        int bgAlpha = (int) backgroundAlpha.number();
        String filter = modulesToShow.choiceName();

        for (ArrayListEntry entry : allEntries) {
            String tag = entry.module.suffix();
            entry.hasTag = hasSuffix && tag != null && !tag.isEmpty();

            String rawName = entry.module.name;
            String name = isLower ? rawName.toLowerCase(Locale.ROOT) : rawName;
            if (noSpaces) name = name.replace(" ", "");

            String rawTag = tag != null ? tag : "";
            String displayTag = isLower ? rawTag.toLowerCase(Locale.ROOT) : rawTag;
            if (noSpaces) displayTag = displayTag.replace(" ", "");

            entry.displayName = name;
            entry.displayTag = displayTag;
            entry.nameWidth = arrayListFont.getStringWidth(name);
            entry.tagWidth = entry.hasTag ? arrayListFont.getStringWidth(displayTag) + 3.0f : 0.0f;
        }

        activeEntries.clear();
        for (ArrayListEntry entry : allEntries) {
            if (!shouldShow(entry, filter)) continue;
            if (entry.animationTime > 0.0f || entry.module.enabled()) {
                activeEntries.add(entry);
            }
        }

        activeEntries.sort((a, b) -> Float.compare(b.getTotalWidth(), a.getTotalWidth()));

        for (ArrayListEntry entry : allEntries) {
            if (entry.module.enabled()) {
                entry.animationTime = Math.min(entry.animationTime + elapsedMs / 100.0f * 10.0f, 10.0f);
            } else {
                entry.animationTime = Math.max(entry.animationTime - elapsedMs / 100.0f * 10.0f, 0.0f);
            }
        }

        float currentY = edgeOffset;
        for (ArrayListEntry entry : activeEntries) {
            if (entry.animationTime <= 0.0f) continue;
            float totalWidth = entry.getTotalWidth();
            if (entry.module.enabled()) {
                entry.targetX = scaledWidth - totalWidth - edgeOffset;
            } else {
                entry.targetX = scaledWidth + totalWidth + edgeOffset;
            }
            entry.targetY = currentY;

            if (entry.module.enabled() || entry.animationTime >= 10.0f) {
                currentY += moduleSpacing;
            }

            if (Math.abs(entry.x - entry.targetX) < 0.5f && Math.abs(entry.y - entry.targetY) < 0.5f
                    && (entry.animationTime == 0.0f || entry.animationTime == 10.0f)) {
                entry.x = entry.targetX;
                entry.y = entry.targetY;
            } else if (entry.x > 3000.0f && entry.module.enabled()) {
                entry.x = entry.targetX;
                entry.y = entry.targetY;
            } else {
                float factor = Math.min(1.0f, 0.015f * elapsedMs);
                entry.x = entry.x + (entry.targetX - entry.x) * factor;
                entry.y = entry.y + (entry.targetY - entry.y) * factor;
            }

            Color theme1 = getThemeColor1();
            Color theme2 = getThemeColor2();
            switch (colorMode.choiceName()) {
                case "Fade" -> {
                    double d = Math.sin(now / 600.0 + entry.y * 0.05) * 0.5 + 0.5;
                    entry.color = interpolate(theme1, theme2, d);
                }
                case "Breathe" -> {
                    double d = Math.sin(now / 600.0) * 0.5 + 0.5;
                    entry.color = interpolate(theme1, theme2, d);
                }
                default -> entry.color = theme1;
            }
        }

        for (ArrayListEntry entry : activeEntries) {
            if (entry.animationTime <= 0.0f) continue;
            float totalWidth = entry.getTotalWidth();

            if (isNormalBg) {
                drawRect(entry.x - 2.0f, entry.y - 3.0f, entry.x + totalWidth + 3.0f, entry.y - 3.0f + moduleSpacing,
                        new Color(0, 0, 0, bgAlpha).getRGB());
            }

            if (withDropShadow) {
                arrayListFont.drawStringWithShadow(entry.displayName, entry.x, entry.y - 0.5f, entry.color.getRGB());
                if (entry.hasTag) {
                    arrayListFont.drawStringWithShadow(entry.displayTag, entry.x + entry.nameWidth + 3.0f, entry.y - 0.5f, 0xFFCCCCCC);
                }
            } else {
                arrayListFont.drawString(entry.displayName, entry.x, entry.y - 0.5f, entry.color.getRGB(), false);
                if (entry.hasTag) {
                    arrayListFont.drawString(entry.displayTag, entry.x + entry.nameWidth + 3.0f, entry.y - 0.5f, 0xFFCCCCCC, false);
                }
            }

        }

        float f3 = scaledHeight - regular18Font.getFontHeight() - 2.0f;

        if (potionEffects.bool() && mc.thePlayer != null) {
            Collection<PotionEffect> effects = mc.thePlayer.getActivePotionEffects();
            if (effects != null && !effects.isEmpty()) {
                IFont potionFontBold = FontManager.getLineSeedBold(8.0f);
                IFont potionFontRegular = FontManager.getLineSeedRegular(9.0f);

                List<PotionEffectEntry> list = new ArrayList<>();
                for (PotionEffect effect : effects) {
                    if (effect.getDuration() > 24000) continue;
                    String numeral = toRomanNumeral(effect.getAmplifier() + 1);
                    String name = I18n.format(effect.getEffectName()) + " " + numeral;
                    String duration = Potion.getDurationString(effect);
                    float nameW = potionFontBold.getStringWidth(name);
                    float durW = potionFontRegular.getStringWidth(" " + duration);
                    list.add(new PotionEffectEntry(effect.getPotionID(), name, duration, nameW, durW));
                }

                list.sort((a, b) -> Float.compare(b.nameWidth + b.durationWidth, a.nameWidth + a.durationWidth));

                for (int i = 0; i < list.size(); i++) {
                    PotionEffectEntry entry = list.get(i);
                    float rowY = f3 - (potionFontRegular.getFontHeight() + 5.0f) * (i + 1);
                    float rowX = scaledWidth - entry.nameWidth - entry.durationWidth - 22.0f;

                    drawPotionIcon(entry.potionId, rowX, rowY - 2.5f, 13.0f);
                    potionFontBold.drawString(entry.name, rowX + 16.0f, rowY, 0xFFCCCCCC, withDropShadow);
                    potionFontRegular.drawString(" " + entry.duration, rowX + 16.0f + entry.nameWidth, rowY, 0xFF999999, withDropShadow);
                }
            }
        }
    }
}
