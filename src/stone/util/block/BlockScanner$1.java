package stone.util.block;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;

public interface BlockScanner$1 {
    void onBlockChanged(BlockPos var1, IBlockState var2);

    void onChunkLoaded(int var1, int var2);

    void onChunkUnloaded(int var1, int var2);

    void cKj73();
}
