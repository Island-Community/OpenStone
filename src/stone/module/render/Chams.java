package stone.module.render;

import stone.event.RenderPlayerPostEvent;
import stone.event.RenderPlayerPreEvent;
import stone.event.RenderWorldLastEvent;
import stone.event.Subscribe;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.Subcategory;
import stone.module.other.AntiBot;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

public class Chams extends Module {
    private boolean polygonOffsetActive;
    private boolean LAAWtE;
    private float savedPolygonOffsetFactor;
    private float IGauQ1;

    public Chams() {
        super("Chams", "Draws players through walls", Category.VISUALS, Subcategory.EXTREME);
    }

    private void applyPolygonOffset() {
        this.LAAWtE = GL11.glIsEnabled(32823);
        this.savedPolygonOffsetFactor = GL11.glGetFloat(32824);
        this.IGauQ1 = GL11.glGetFloat(10752);
        this.polygonOffsetActive = true;
        GL11.glEnable(32823);
        GL11.glPolygonOffset(1.0F, -4000000.0F);
    }

    private void restorePolygonOffset() {
        if (this.polygonOffsetActive) {
            GL11.glPolygonOffset(this.savedPolygonOffsetFactor, this.IGauQ1);
            if (this.LAAWtE) {
                GL11.glEnable(32823);
            } else {
                GL11.glDisable(32823);
            }

            this.polygonOffsetActive = false;
        }
    }

    @Subscribe
    public void onRenderPlayerPre(RenderPlayerPreEvent var1) {
        this.restorePolygonOffset();
        EntityPlayer var2 = var1.entityPlayer;
        if (var2 != mc.thePlayer && !AntiBot.shouldHideEntity(var2)) {
            this.applyPolygonOffset();
        }
    }

    @Subscribe
    public void onRenderPlayerPost(RenderPlayerPostEvent var1) {
        this.restorePolygonOffset();
    }

    @Subscribe
    public void onRenderWorldLast(RenderWorldLastEvent var1) {
        this.restorePolygonOffset();
    }

    @Override
    public void onDisable(Context context) {
        this.restorePolygonOffset();
    }
}
