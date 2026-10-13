package stone.module.render;

import stone.event.RenderWorldLastEvent;
import stone.event.Subscribe;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.Subcategory;
import stone.module.render.blockesp.BedWarsDetector;
import stone.module.render.blockesp.BlockEspRenderer;
import stone.module.render.blockesp.BlockEspRenderer$0;
import stone.module.render.blockesp.BlockEspScanLimits;
import stone.module.render.blockesp.BlockEspSettings;
import stone.util.ClientUtils;
import stone.util.block.BlockScanner;

public class BlockESP extends Module {
    private final BlockEspSettings blockEspSettings = new BlockEspSettings(this);
    private int lastEntryHash;

    public BlockESP() {
        super("BlockESP", "Highlights chosen blocks", Category.VISUALS, Subcategory.EXTREME);
    }

    @Override
    public void onEnable(Context context) {
        BlockScanner var1 = BlockScanner.getInstance();
        var1.hokXq4(this.blockEspSettings.blocks);
        this.lastEntryHash = this.blockEspSettings.blocks.getEntries().hashCode();
        if (!this.blockEspSettings.blocks.getEntries().isEmpty()) {
            var1.Ziwu();
        }
    }

    @Override
    public void onDisable(Context context) {
        BlockScanner.getInstance().vUnwA();
    }

    @Override
    public void onUpdate() {
        if (ClientUtils.isInWorld()) {
            int var1 = this.blockEspSettings.blocks.getEntries().hashCode();
            if (var1 != this.lastEntryHash) {
                this.lastEntryHash = var1;
                BlockScanner.getInstance().Ziwu();
            }
        }
    }

    public int getSearchSpeed() {
        boolean var1 = !this.blockEspSettings.gameCheck.bool() || BedWarsDetector.isBedWarsGame();
        return BlockEspScanLimits.resolveBlocksPerTick(this.enabled(), !this.blockEspSettings.blocks.getEntries().isEmpty(), var1, this.blockEspSettings.searchSpeed.number());
    }

    @Override
    public String suffix() {
        int var1 = BlockScanner.getInstance().getTrackedBlockCount();
        return var1 > 0 ? String.valueOf(var1) : "";
    }

    @Subscribe
    public void onRenderWorldLast(RenderWorldLastEvent var1) {
        BlockScanner var2 = BlockScanner.getInstance();
        if (var2.isScanningActive() && ClientUtils.isInWorld()) {
            if (!this.blockEspSettings.gameCheck.bool() || BedWarsDetector.isBedWarsGame()) {
                double var3 = this.blockEspSettings.range.number();
                BlockEspRenderer$0 var5 = new BlockEspRenderer$0(
                    var3 * var3, (int)this.blockEspSettings.maxBlocksRendered.number(), this.getMaxPerChunk(), this.blockEspSettings.outline.bool(), this.blockEspSettings.shade.bool()
                );
                BlockEspRenderer.renderNearbyBlocks(mc, var2, this.blockEspSettings.blocks, var5);
            }
        }
    }

    private int getMaxPerChunk() {
        return BlockEspScanLimits.resolveMaxPerChunk(this.blockEspSettings.maxPerChunk.number());
    }
}
