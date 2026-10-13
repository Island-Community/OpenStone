package stone.module.render;

import stone.event.RenderWorldLastEvent;
import stone.event.Subscribe;
import stone.module.Category;
import stone.module.Module;
import stone.module.ModuleManager;
import stone.module.Subcategory;
import stone.util.render.RenderUtils;
import stone.util.render.RenderUtils$1;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;

public class AntiInvis extends Module {
    private static final ResourceLocation resourceLocation = new ResourceLocation(
        "textures/gui/container/inventory.png"
    );

    public AntiInvis() {
        super("AntiInvis", "Shows invisible players", Category.VISUALS, Subcategory.EXTREME);
    }

    public boolean isForceVisibleTarget(EntityLivingBase var1) {
        return this.enabled() && var1 instanceof EntityPlayer && var1 != mc.thePlayer;
    }

    public boolean isHiddenPlayer(EntityPlayer var1) {
        return this.isForceVisibleTarget(var1) && var1.isInvisible() && !var1.isDead && var1.deathTime <= 0;
    }

    public boolean shouldHideVanillaNametag(EntityPlayer var1) {
        Nametags var2 = ModuleManager.get(Nametags.class);
        return var2.shouldHideVanillaNametag(var1);
    }

    @Subscribe
    public void onRenderWorldLast(RenderWorldLastEvent var1) {
        if (mc.theWorld != null && mc.thePlayer != null && mc.getRenderManager() != null) {
            for (EntityPlayer var3 : mc.theWorld.playerEntities) {
                if (this.isHiddenPlayer(var3) && !this.shouldHideVanillaNametag(var3) && RenderUtils.isEntityInView(var3)) {
                    this.renderHiddenMarker(var3, var1.partialTicks);
                }
            }
        }
    }

    private void renderHiddenMarker(EntityPlayer var1, float var2) {
        RenderManager var3 = mc.getRenderManager();
        double var4 = var1.lastTickPosX + (var1.posX - var1.lastTickPosX) * var2 - var3.viewerPosX;
        double var6 = var1.lastTickPosY + (var1.posY - var1.lastTickPosY) * var2 - var3.viewerPosY;
        double var8 = var1.lastTickPosZ + (var1.posZ - var1.lastTickPosZ) * var2 - var3.viewerPosZ;
        float var10 = var1.height + (var1.isSneaking() ? 0.85F : 1.05F);
        GlStateManager.pushMatrix();
        RenderUtils$1 var11 = null;

        try {
            var11 = RenderUtils.uyB6();
            GlStateManager.translate((float)var4, (float)var6 + var10, (float)var8);
            GlStateManager.rotate(-var3.playerViewY, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(var3.playerViewX, 1.0F, 0.0F, 0.0F);
            GlStateManager.scale(-0.025F, -0.025F, 0.025F);
            GlStateManager.disableDepth();
            GlStateManager.depthMask(false);
            GlStateManager.enableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            xiJ8(-9, -9);
        } finally {
            RenderUtils.restoreLightmapState(var11);
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
        }

    }

    public static void xiJ8(int var0, int var1) {
        int var2 = Potion.invisibility.getStatusIconIndex();
        mc.getTextureManager().bindTexture(resourceLocation);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Gui.drawModalRectWithCustomSizedTexture(var0, var1, var2 % 8 * 18, 198 + var2 / 8 * 18, 18, 18, 256.0F, 256.0F);
    }
}
