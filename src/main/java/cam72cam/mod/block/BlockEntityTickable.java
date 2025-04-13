package cam72cam.mod.block;

import cam72cam.mod.block.tile.TileEntity;
import cam72cam.mod.block.tile.TileEntityTickable;
import cam72cam.mod.resource.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Wraps BlockEntity and exposes an update function which is called every tick */
public abstract class BlockEntityTickable extends BlockEntity {
    /** Called every tick */
    public abstract void update();

    @Override
    public TileEntity supplier(Identifier id, BlockPos pos, BlockState state) {
        return new TileEntityTickable(id, pos, state);
    }
}
