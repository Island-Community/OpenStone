package stone.module.render.blockesp;

import stone.module.Module;
import stone.module.Setting;
import stone.util.block.BlockColorList;

public final class BlockEspSettings {
    public final BlockColorList blocks;
    public final Setting outline;
    public final Setting shade;
    public final Setting range;
    public final Setting searchSpeed;
    public final Setting maxBlocksRendered;
    public final Setting maxPerChunk;
    public final Setting gameCheck;

    public BlockEspSettings(Module var1) {
        this.blocks = new BlockColorList();
        this.outline = var1.addBoolean("Outline", "", true);
        this.shade = var1.addBoolean("Shade", "", false);
        this.range = var1.addNumber("Range", "", 64.0F, 8.0F, 256.0F, 0);
        this.searchSpeed = var1.addNumber("Search Speed", "", 8.0F, 1.0F, 64.0F, 0);
        this.maxBlocksRendered = var1.addNumber("Max Blocks Rendered", "", 2048.0F, 128.0F, 8192.0F, 0);
        this.maxPerChunk = var1.addNumber("Max Per Chunk", "", 50.0F, 1.0F, 50.0F, 0);
        this.gameCheck = var1.addBoolean("Game check", "", false);
    }

}
