package stone.util.font;

import stone.core.Assets;
import stone.ui.Client;
import stone.module.render.nametags.MinecraftFont;
import java.awt.Font;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

public final class FontManager {
    private static final String MINECRAFT = "Minecraft";
    private static final FontManager$7[] BUNDLED_FONTS = new FontManager$7[]{
        new FontManager$7("Modern", "ProductSans-Regular.ttf", 0),
        new FontManager$7("Bold", "ProductSans-Bold.ttf", 1),
        new FontManager$7("Smooth", "Smooth.ttf", 0),
        new FontManager$7("Pixel", "Doto-Black.ttf", 0),
        new FontManager$7("Tahoma", "tahoma.ttf", 0),
        new FontManager$7("LINESeed", "LINESeedKR-Rg.ttf", 0),
        new FontManager$7("LINESeed-Bold", "LINESeedKR-Bd.ttf", 1),
        new FontManager$7("LINESeed-Thin", "LINESeedKR-Th.ttf", 0)
    };
    private static final Map<String, FontManager$7> BUNDLED_FONT_MAP = buildBundledFontMap();
    private static final Map<String, Font> BASE_FONT_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, IFont> FONT_CACHE = new LinkedHashMap<>(16, 0.75F, true);
    private static final Map<String, FontManager$8> CUSTOM_FONT_MAP = new LinkedHashMap<>();
    private static final Map<String, String> BOLD_FAMILY_MAP = new LinkedHashMap<>();
    private static String[] hudFontOptions = buildBundledHudFontOptions();
    private static File fontDirectory;
    private static long nextFontScanMs;
    private static String lastDirectoryFingerprint = "";

    private FontManager() {
    }

    public static void tick() {
        refreshCustomFonts();
    }

    public static File getFontDirectory() {
        if (fontDirectory == null) {
            fontDirectory = Client.directory().resolve("fonts").toFile();
        }

        if (!fontDirectory.exists()) {
            fontDirectory.mkdirs();
        }

        return fontDirectory;
    }

    public static String[] getHudFontOptions() {
        refreshCustomFonts();
        return (String[])hudFontOptions.clone();
    }

    public static String getDefaultHudFontName() {
        String[] var0 = getHudFontOptions();
        return var0.length == 0 ? "Minecraft" : var0[0];
    }

    public static IFont getHudRenderer(String var0, float var1) {
        float var2 = Math.max(0.5F, Math.min(2.0F, var1));
        return getRenderer(var0, 10.0F * var2);
    }

    public static IFont getNametagRenderer(String var0) {
        return getRenderer(var0, 9.0F);
    }

    public static IFont getFont(String name, float size) {
        return getRenderer(name, size);
    }

    public static IFont getLineSeedRegular(float size) {
        return getRenderer("LINESeed", size);
    }

    public static IFont getLineSeedBold(float size) {
        return getRenderer("LINESeed-Bold", size);
    }

    public static IFont getRenderer(String var0, float var1) {
        final float var2 = Math.max(1.0F, var1);
        if (var0 != null && !isMinecraftFont(var0)) {
            final FontManager$7 var3 = BUNDLED_FONT_MAP.get(var0);
            if (var3 != null) {
                String var6 = var0 + "#" + quantizeForCacheKey(var2) + "#" + getUiScale();
                return getCachedRenderer(
                    var6,
                    new Supplier<IFont>() {
                        public IFont get() {
                            try {
                                Font var1x = FontManager.BASE_FONT_CACHE.computeIfAbsent(var3.fileName, var0x -> FontManager.loadBaseFont(var0x));
                                return (IFont)(var1x == null
                                    ? FontManager.getMinecraftRenderer(var2)
                                    : new TrueTypeFont(var1x.deriveFont(var3.style, var2), true));
                            } catch (Throwable t) {
                                return FontManager.getMinecraftRenderer(var2);
                            }
                        }
                    }
                );
            } else {
                final FontManager$8 var4 = getCustomFont(var0);
                if (var4 == null) {
                    return getMinecraftRenderer(var2);
                } else {
                    String var5 = "custom:" + var4.cacheKey + "#" + quantizeForCacheKey(var2) + "#" + getUiScale();
                    return getCachedRenderer(
                        var5,
                        new Supplier<IFont>() {
                            public IFont get() {
                                Font var1 = FontManager.BASE_FONT_CACHE.computeIfAbsent(var4.cacheKey, new Function<String, Font>() {
                                    public Font apply(String var1) {
                                        return FontManager.loadCustomBaseFont(var4.file);
                                    }
                                });
                                return (IFont)(var1 == null
                                    ? FontManager.getMinecraftRenderer(var2)
                                    : new TrueTypeFont(var1.deriveFont(var4.style, var2), true));
                            }
                        }
                    );
                }
            }
        } else {
            return getMinecraftRenderer(var2);
        }
    }

    private static IFont getMinecraftRenderer(float var0) {
        float var1 = Math.max(1.0F, (float)Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT);
        final float var2 = Math.max(0.5F, Math.min(2.0F, var0 / var1));
        String var3 = "Minecraft#" + quantizeForCacheKey(var2);
        return getCachedRenderer(var3, new Supplier<IFont>() {
            public IFont get() {
                return new MinecraftFont(Minecraft.getMinecraft().fontRendererObj, var2);
            }
        });
    }

    public static boolean isMinecraftFont(String var0) {
        return var0 == null || "Minecraft".equalsIgnoreCase(var0);
    }

    private static String[] buildBundledHudFontOptions() {
        String[] var0 = new String[BUNDLED_FONTS.length + 1];

        for (int var1 = 0; var1 < BUNDLED_FONTS.length; var1++) {
            var0[var1] = BUNDLED_FONTS[var1].displayName;
        }

        var0[BUNDLED_FONTS.length] = "Minecraft";
        return var0;
    }

    private static Map<String, FontManager$7> buildBundledFontMap() {
        LinkedHashMap var0 = new LinkedHashMap();

        for (FontManager$7 var4 : BUNDLED_FONTS) {
            var0.put(var4.displayName, var4);
        }

        var0.put("Sf-Regular", var0.get("Modern"));
        var0.put("Sf-Ui", var0.get("Modern"));
        var0.put("Sf-Bold", var0.get("Bold"));
        return var0;
    }

    private static synchronized FontManager$8 getCustomFont(String var0) {
        refreshCustomFonts();
        return CUSTOM_FONT_MAP.get(var0);
    }

    private static synchronized void refreshCustomFonts() {
        long var1 = System.currentTimeMillis();
        if (var1 >= nextFontScanMs) {
            nextFontScanMs = var1 + 2000L;
            File var3 = getFontDirectory();
            File[] var4 = var3.listFiles();
            String var5 = buildDirectoryFingerprint(var4);
            if (!var5.equals(lastDirectoryFingerprint)) {
                lastDirectoryFingerprint = var5;
                LinkedHashMap var6 = new LinkedHashMap();
                LinkedHashMap var7 = new LinkedHashMap();
                if (var4 != null) {
                    Arrays.sort(var4, (var0x, var1x) -> var0x.getName().compareToIgnoreCase(var1x.getName()));

                    for (File var11 : var4) {
                        if (var11.isFile() && isSupportedFontFile(var11)) {
                            String var12 = displayNameForFile(var11);
                            if (var12.length() != 0 && !isReservedDisplayName(var12)) {
                                int var13 = isBoldDisplayName(var12) ? 1 : 0;
                                var6.put(var12, new FontManager$8(var11, var13));
                            }
                        }
                    }
                }

                for (String var15 : (java.lang.Iterable<String>) (java.lang.Iterable<?>) (var6.keySet())) {
                    if (isBoldDisplayName(var15)) {
                        String var16 = regularNameForBoldDisplay(var15);
                        if (var6.containsKey(var16)) {
                            var7.put(var16, var15);
                        }
                    }
                }

                var7.put("Modern", "Bold");
                var7.put("Sf-Regular", "Bold");
                var7.put("Sf-Ui", "Bold");
                var7.put("LINESeed", "LINESeed-Bold");
                CUSTOM_FONT_MAP.clear();
                CUSTOM_FONT_MAP.putAll(var6);
                BOLD_FAMILY_MAP.clear();
                BOLD_FAMILY_MAP.putAll(var7);
                hudFontOptions = buildHudFontOptions(var6.keySet());
            }
        }
    }

    private static String[] buildHudFontOptions(Iterable<String> var0) {
        ArrayList var1 = new ArrayList();

        for (String var5 : buildBundledHudFontOptions()) {
            var1.add(var5);
        }

        for (String var7 : var0) {
            var1.add(var7);
        }

        return (String[]) var1.toArray(new String[0]);
    }

    private static String buildDirectoryFingerprint(File[] var0) {
        if (var0 != null && var0.length != 0) {
            Arrays.sort(var0, (var0x, var1x) -> var0x.getName().compareToIgnoreCase(var1x.getName()));
            StringBuilder var1 = new StringBuilder();

            for (File var5 : var0) {
                if (var5.isFile() && isSupportedFontFile(var5)) {
                    var1.append(var5.getName()).append(':').append(var5.length()).append(':').append(var5.lastModified()).append(';');
                }
            }

            return var1.toString();
        } else {
            return "";
        }
    }

    private static boolean isSupportedFontFile(File var0) {
        String var1 = var0.getName().toLowerCase();
        return var1.endsWith(".ttf") || var1.endsWith(".otf");
    }

    private static boolean isReservedDisplayName(String var0) {
        return "Minecraft".equalsIgnoreCase(var0) || BUNDLED_FONT_MAP.containsKey(var0);
    }

    private static String displayNameForFile(File var0) {
        String var1 = var0.getName();
        int var2 = var1.lastIndexOf(46);
        if (var2 > 0) {
            var1 = var1.substring(0, var2);
        }

        var1 = var1.replace('_', ' ').replace('-', ' ').trim();
        var1 = var1.replaceAll("\\s+", " ");
        if (var1.toLowerCase().endsWith("bold") && !var1.toLowerCase().endsWith(" bold")) {
            var1 = var1.substring(0, var1.length() - 4).trim() + " Bold";
        }

        return var1;
    }

    private static boolean isBoldDisplayName(String var0) {
        return var0 != null && var0.toLowerCase().endsWith(" bold");
    }

    private static String regularNameForBoldDisplay(String var0) {
        return var0.substring(0, var0.length() - " Bold".length()).trim();
    }

    private static Font loadBaseFont(String var0) {
        byte[] var1 = readFontData(var0);
        if (var1 == null) {
            return null;
        } else {
            try {
                return Font.createFont(0, new ByteArrayInputStream(var1));
            } catch (Exception var5) {
                try {
                    return Font.createFont(1, new ByteArrayInputStream(var1));
                } catch (Exception var4) {
                    return null;
                }
            }
        }
    }

    private static Font loadCustomBaseFont(File var0) {
        if (var0 != null && var0.isFile()) {
            try (FileInputStream var1 = new FileInputStream(var0)) {
                return Font.createFont(0, var1);
            } catch (Exception var15) {
                return null;
            }
        } else {
            return null;
        }
    }

    private static int getUiScale() {
        try {
            return Math.max(1, new ScaledResolution(Minecraft.getMinecraft()).getScaleFactor());
        } catch (Exception var1) {
            return 1;
        }
    }

    private static float quantizeForCacheKey(float var0) {
        return Math.round(var0 * 100.0F) / 100.0F;
    }

    private static byte[] readFontData(String var0) {
        try (InputStream var1 = Assets.open("/assets/jade/fonts/" + var0)) {
            if (var1 != null) {
                ByteArrayOutputStream var20 = new ByteArrayOutputStream();
                byte[] var4 = new byte[4096];

                int var5;
                while ((var5 = var1.read(var4)) != -1) {
                    var20.write(var4, 0, var5);
                }

                return var20.toByteArray();
            } else {
                return null;
            }
        } catch (IOException var19) {
            return null;
        }
    }

    private static synchronized IFont getCachedRenderer(String var0, Supplier<IFont> var1) {
        IFont var2 = FONT_CACHE.get(var0);
        if (var2 != null) {
            return var2;
        } else {
            var2 = (IFont)var1.get();
            FONT_CACHE.put(var0, var2);
            trimFontCache();
            return var2;
        }
    }

    private static void trimFontCache() {
        while (FONT_CACHE.size() > 512) {
            Iterator var0 = FONT_CACHE.entrySet().iterator();
            if (!var0.hasNext()) {
                return;
            }

            Entry var1 = (Entry)var0.next();
            var0.remove();
            if (var1.getValue() != null) {
                ((IFont)var1.getValue()).destroy();
            }
        }
    }
}
