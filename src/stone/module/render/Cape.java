package stone.module.render;

import stone.core.Assets;
import stone.module.Category;
import stone.module.Module;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.ui.Theme;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

public class Cape extends Module {
    private static Cape instance;
    public static Setting cape;
    private String[] capeModes;
    private List<String> capeEntries = new ArrayList<>();
    public static List<List<ResourceLocation>> capeFrames = new ArrayList<>();
    private int vnO = -1;
    private DynamicTexture dynamicTexture;
    private int[] capePixels;
    private int[] animatedPixelIndices;
    private int[] ru4;
    private int capeTextureWidth;
    private long lastAnimationMillis = -1L;

    public Cape() {
        super("Cape", "Gives you a cape", Category.VISUALS, Subcategory.BASIC);
        instance = this;
        this.KBIxnC();
        cape = addChoice("Cape", "", List.of(this.capeModes), 0);
    }

    private void KBIxnC() {
        ArrayList var1 = new ArrayList();

        for (String var5 : new String(Assets.read("assets/jade/textures/capes/capes.txt"), StandardCharsets.UTF_8).split("\n")) {
            var5 = var5.trim();
            if (!var5.isEmpty()) {
                var1.add(var5);
            }
        }

        this.capeEntries = var1;
        ArrayList var21 = new ArrayList();
        var21.add("None");

        for (String var31 : this.capeEntries) {
            int var35 = var31.indexOf(47);
            if (var35 != -1) {
                var21.add(var31.substring(0, var35));
            } else {
                int var39 = var31.lastIndexOf(46);
                var21.add(var39 > 0 ? var31.substring(0, var39) : var31);
            }
        }

        this.capeModes = (String[]) var21.toArray(new String[0]);
    }

    public static ResourceLocation texture() {
        if (instance == null || !instance.enabled() || cape.choice() <= 0) {
            return null;
        }

        if (capeFrames.isEmpty()) {
            instance.loadCapeTextures();
        }

        int var0 = cape.choice() - 1;
        if (var0 >= capeFrames.size()) {
            return null;
        }

        if (var0 == instance.vnO) {
            instance.updateJadeCapeAnimation();
        }

        List<ResourceLocation> var1 = capeFrames.get(var0);
        if (var1.isEmpty()) {
            return null;
        }

        return var1.size() == 1 ? var1.get(0) : var1.get((int)(System.currentTimeMillis() / 67L % var1.size()));
    }

    public void loadCapeTextures() {
        capeFrames.clear();
        this.vnO = -1;
        this.dynamicTexture = null;
        this.capePixels = null;
        this.animatedPixelIndices = null;
        this.ru4 = null;
        this.capeTextureWidth = 0;
        this.lastAnimationMillis = -1L;
        String var1 = "/assets/jade/textures/capes/";

        try {
            for (String var3 : this.capeEntries) {
                int var4 = var3.indexOf(47);
                if (var4 != -1) {
                    String var16 = var3.substring(0, var4);
                    String var17 = var3.substring(var4 + 1);
                    String[] var18 = var17.split(",");
                    ArrayList var19 = new ArrayList();

                    for (String var23 : var18) {
                        var23 = var23.trim();
                        if (!var23.isEmpty()) {
                            InputStream var25 = Assets.open(var1 + var16 + "/" + var23);
                            if (var25 != null) {
                                BufferedImage var26 = ImageIO.read(var25);
                                var25.close();
                                if (var26 != null) {
                                    var19.add(mc.getTextureManager().getDynamicTextureLocation(var16 + "_" + var23, new DynamicTexture(var26)));
                                }
                            }
                        }
                    }

                    capeFrames.add(var19);
                } else {
                    String var5 = var3.substring(0, var3.lastIndexOf(46));
                    InputStream var6 = Assets.open(var1 + var3);
                    if (var6 == null) {
                        capeFrames.add(new ArrayList<>());
                    } else {
                        BufferedImage var7 = ImageIO.read(var6);
                        var6.close();
                        if (var7 == null) {
                            capeFrames.add(new ArrayList<>());
                        } else {
                            int var8 = var7.getWidth();
                            int var9 = var7.getHeight();
                            int var10 = Math.max(1, var8 / 2);
                            ArrayList var11 = new ArrayList();
                            if ("Jade".equals(var5)) {
                                this.vnO = capeFrames.size();
                                var11.add(this.createJadeCapeTexture(var7));
                            } else if (var9 > var10 && var9 % var10 == 0) {
                                int var12 = var9 / var10;

                                for (int var13 = 0; var13 < var12; var13++) {
                                    BufferedImage var14 = var7.getSubimage(0, var13 * var10, var8, var10);
                                    var11.add(mc.getTextureManager().getDynamicTextureLocation(var5 + "_f" + var13, new DynamicTexture(var14)));
                                }
                            } else {
                                var11.add(mc.getTextureManager().getDynamicTextureLocation(var5, new DynamicTexture(var7)));
                            }

                            capeFrames.add(var11);
                        }
                    }
                }
            }
        } catch (Exception var15) {
            var15.printStackTrace();
        }
    }

    private ResourceLocation createJadeCapeTexture(BufferedImage var1) {
        BufferedImage var2 = new BufferedImage(var1.getWidth(), var1.getHeight(), 2);
        Graphics var3 = var2.getGraphics();
        var3.drawImage(var1, 0, 0, null);
        var3.dispose();
        this.capeTextureWidth = var2.getWidth();
        int var4 = var2.getHeight();
        this.capePixels = new int[this.capeTextureWidth * var4];
        ArrayList var5 = new ArrayList();
        ArrayList var6 = new ArrayList();
        var2.getRGB(0, 0, this.capeTextureWidth, var4, this.capePixels, 0, this.capeTextureWidth);

        for (int var7 = 0; var7 < this.capePixels.length; var7++) {
            int var8 = this.capePixels[var7];
            int var9 = var8 >>> 24 & 0xFF;
            int var10 = var8 >>> 16 & 0xFF;
            int var11 = var8 >>> 8 & 0xFF;
            int var12 = var8 & 0xFF;
            int var13 = Math.max(var10, Math.max(var11, var12));
            if (var9 > 0 && var13 > 26) {
                int var14 = Math.min(255, Math.max(0, (var13 - 24) * 255 / 231));
                var5.add(var7);
                var6.add(var14);
                this.capePixels[var7] = var9 << 24 | 1572864 | 6144 | 24;
            }
        }

        this.animatedPixelIndices = new int[var5.size()];
        this.ru4 = new int[var6.size()];

        for (int var15 = 0; var15 < var5.size(); var15++) {
            this.animatedPixelIndices[var15] = (Integer)var5.get(var15);
            this.ru4[var15] = (Integer)var6.get(var15);
        }

        this.dynamicTexture = new DynamicTexture(var2);
        return mc.getTextureManager().getDynamicTextureLocation("jade_dynamic_cape", this.dynamicTexture);
    }

    private void updateJadeCapeAnimation() {
        if (this.dynamicTexture != null && this.capePixels != null && this.animatedPixelIndices != null && this.ru4 != null && this.capeTextureWidth > 0) {
            long var1 = System.currentTimeMillis();
            if (this.lastAnimationMillis < 0L || var1 - this.lastAnimationMillis >= 100L) {
                this.lastAnimationMillis = var1;
                int[] var3 = this.dynamicTexture.getTextureData();
                if (var3.length == this.capePixels.length) {
                    for (int var4 = 0; var4 < this.animatedPixelIndices.length; var4++) {
                        int var5 = this.ru4[var4];
                        int var6 = this.animatedPixelIndices[var4];
                        int var7 = this.capePixels[var6];
                        int var8 = var7 >>> 24 & 0xFF;
                        int var10 = Theme.accent().argb();
                        int var11 = 255 - var5;
                        int var12 = ((var7 >>> 16 & 0xFF) * var11 + (var10 >>> 16 & 0xFF) * var5) / 255;
                        int var13 = ((var7 >>> 8 & 0xFF) * var11 + (var10 >>> 8 & 0xFF) * var5) / 255;
                        int var14 = ((var7 & 0xFF) * var11 + (var10 & 0xFF) * var5) / 255;
                        var3[var6] = var8 << 24 | var12 << 16 | var13 << 8 | var14;
                    }

                    this.dynamicTexture.updateDynamicTexture();
                }
            }
        }
    }
}
