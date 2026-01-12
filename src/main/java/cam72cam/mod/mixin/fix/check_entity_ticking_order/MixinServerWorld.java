package cam72cam.mod.mixin.fix.check_entity_ticking_order;

import cam72cam.mod.mixin.accessor.AWorld;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import net.minecraft.entity.Entity;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.function.BooleanSupplier;

//TODO 1.14.4 entity ticking order are messed up, capture here and expose by ThreadLocal
@Mixin(ServerWorld.class)
public class MixinServerWorld implements AWorld {
    private static ThreadLocal<Set<Integer>> tickedEntities = new ThreadLocal<>();

    @Override
    public Set<Integer> tickedEntities() {
        return tickedEntities.get();
    }

    @Inject(method = "tick", at = @At(value = "HEAD"))
    public void tickStart(BooleanSupplier p_72835_1_, CallbackInfo ci) {
        tickedEntities.set(new IntAVLTreeSet());
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/ints/Int2ObjectMap$Entry;getValue()Ljava/lang/Object;"))
    public void capture(BooleanSupplier p_72835_1_, CallbackInfo ci, @Local LocalRef<Int2ObjectMap.Entry<Entity>> entry) {
        tickedEntities.get().add(entry.get().getIntKey());
    }

    @Inject(method = "tick", at = @At(value = "RETURN"))
    public void tickEnd(BooleanSupplier p_72835_1_, CallbackInfo ci) {
        tickedEntities.remove();
    }
}
