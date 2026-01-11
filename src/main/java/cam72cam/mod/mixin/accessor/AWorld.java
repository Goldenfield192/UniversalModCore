package cam72cam.mod.mixin.accessor;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.Collections;
import java.util.Set;

public interface AWorld {
    Set<Integer> tickedEntities();

    static Set<Integer> getTicked(World reference) {
        if (reference instanceof ServerWorld
                || (reference.isRemote && reference instanceof ClientWorld)) {
            return ((AWorld) reference).tickedEntities();
        }
        return Collections.emptySet();
    }
}
