package stone.util.block;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;

public interface BlockFilter {
    boolean matches(IBlockState var1);

    default void zykH() {
    }

    default boolean isActive() {
        return true;
    }

    default boolean matchesAt(BlockPos var1, IBlockState var2) {
        return this.matches(var2);
    }
}
