package stone.util.block;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

public final class WorldUtils {
    private WorldUtils() {
    }

    public static Block getBlock(World var0, BlockPos var1) {
        return getBlockState(var0, var1).getBlock();
    }

    public static IBlockState getBlockState(World var0, BlockPos var1) {
        return BlockBoundsUtils.getBlockStateSafe(var0, var1);
    }

    public static AxisAlignedBB getSelectionBounds(World var0, BlockPos var1) {
        return BlockBoundsUtils.sBdmu(var0, var1);
    }

    public static AxisAlignedBB APOS(World var0, BlockPos var1) {
        return BlockBoundsUtils.getCollisionBounds(var0, var1, true);
    }
}
