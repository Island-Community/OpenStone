package stone.module.render;

import stone.core.Color;
import stone.event.Phase;
import stone.event.RenderWorldLastEvent;
import stone.event.Subscribe;
import stone.event.TickEndEvent;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.module.other.AntiBot;
import stone.util.ClientUtils;
import stone.util.render.RenderUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public class Tracers extends Module {
    public Setting lineWidth;
    public Setting maxDistance;
    public Setting color;
    public Setting teamColor;
    public Setting offScreenOnly;
    public Setting hideTeammates;
    public Setting onlyInGame;
    private boolean rVdeD;
    private final ArrayList<Entity> entitys = new ArrayList<>();
    private int entityCount = 0;

    public Tracers() {
        super("Tracers", "Lines from the crosshair to players", Category.VISUALS, Subcategory.ADVANCED);
        this.lineWidth = addNumber("Line Width", "", 1.0F, 1.0F, 5.0F, 0);
        this.maxDistance = addNumber("Max distance", "", 128.0F, 10.0F, 512.0F, 0);
        this.color = addColour("Color", "", Color.rgb(0x00FF00));
        this.teamColor = addBoolean("Team color", "", false);
        this.offScreenOnly = addBoolean("Off-screen only", "", false);
        this.hideTeammates = addBoolean("Hide teammates", "", false);
        this.onlyInGame = addBoolean("Only in-game", "", false);
    }

    @Override
    public void onEnable(Context context) {
        this.rVdeD = mc.gameSettings.viewBobbing;
        if (this.rVdeD) {
            mc.gameSettings.viewBobbing = false;
        }
    }

    @Override
    public void onDisable(Context context) {
        mc.gameSettings.viewBobbing = this.rVdeD;
    }

    @Override
    public void onUpdate() {
        if (mc.gameSettings.viewBobbing) {
            mc.gameSettings.viewBobbing = false;
        }
    }

    @Subscribe
    public void onTickEnd(TickEndEvent var1) {
        if (var1.phase == Phase.END) {
            this.abeK();
        }
    }

    @Subscribe
    public void onRenderWorldLast(RenderWorldLastEvent var1) {
        if (ClientUtils.isInWorld() && this.entityCount != 0) {
            int var2 = this.color.colour().argb();

            for (int var3 = 0; var3 < this.entityCount; var3++) {
                Entity var4 = this.entitys.get(var3);
                if (var4 != null && (!this.offScreenOnly.bool() || !RenderUtils.isEntityInView(var4))) {
                    int var5 = this.applyTeamColor(var4, var2);
                    RenderUtils.drawTracerToEntity(var4, var5, (float)this.lineWidth.number(), var1.partialTicks);

                }
            }
        }
    }

    private int applyTeamColor(Entity var1, int var2) {
        if (this.teamColor.bool() && var1 instanceof EntityPlayer) {
            int var3 = ClientUtils.oCoqd(var1);
            if (var3 != -1) {
                int var4 = var2 >> 24 & 0xFF;
                return var4 << 24 | var3 & 16777215;
            }
        }

        return var2;
    }

    private void abeK() {
        this.entityCount = 0;
        if (ClientUtils.isInWorld() && mc.theWorld != null) {
            if (!this.onlyInGame.bool() || isInBedwarsGame()) {
                double var1 = this.maxDistance.number();
                double var3 = var1 * var1;
                for (EntityPlayer var6 : mc.theWorld.playerEntities) {
                    if (var6 != mc.thePlayer
                        && var6.deathTime == 0
                        && !AntiBot.shouldHideEntity(var6)
                        && (!this.hideTeammates.bool() || !ClientUtils.isTeammate(var6))
                        && !(mc.thePlayer.getDistanceSqToEntity(var6) > var3)) {
                        this.addEntity(var6);
                    }
                }

            }
        }
    }

    private void addEntity(Entity var1) {
        if (this.entityCount >= this.entitys.size()) {
            this.entitys.add(var1);
        } else {
            this.entitys.set(this.entityCount, var1);
        }

        this.entityCount++;
    }

    private static boolean isInBedwarsGame() {
        List var0 = ClientUtils.PxSw4();
        if (var0.size() < 7) {
            return false;
        } else if (!ClientUtils.zaUnpz((String)var0.get(0)).startsWith("BED WARS")) {
            return false;
        } else {
            String var1 = ClientUtils.zaUnpz((String)var0.get(1));
            String[] var2 = var1.split("  ");
            if (var2.length > 1) {
                String var3 = var2[1];
                if (var3.endsWith("]")) {
                    var3 = var3.split(" ")[0];
                }

                if (var3.startsWith("L")) {
                    return false;
                }
            }

            if (ClientUtils.zaUnpz((String)var0.get(5)).startsWith("R Red:") && ClientUtils.zaUnpz((String)var0.get(6)).startsWith("B Blue:")) {
                return true;
            } else {
                String var4 = ClientUtils.zaUnpz((String)var0.get(6));
                return !var4.equals("Waiting...") && !var4.startsWith("Starting in") ? false : false;
            }
        }
    }

}
