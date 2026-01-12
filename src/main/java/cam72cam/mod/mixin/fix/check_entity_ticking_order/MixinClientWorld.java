package cam72cam.mod.mixin.fix.check_entity_ticking_order;

import cam72cam.mod.mixin.accessor.AWorld;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ClientWorld.class)
public class MixinClientWorld implements AWorld {
    private static ThreadLocal<Set<Integer>> tickedEntities = new ThreadLocal<>();

    @Override
    public Set<Integer> tickedEntities() {
        return tickedEntities.get();
    }

    @Inject(method = "tickEntities", at = @At(value = "HEAD"))
    public void tickStart(CallbackInfo ci) {
        tickedEntities.set(new IntAVLTreeSet());
    }

    @Inject(method = "tickEntities", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/ints/Int2ObjectMap$Entry;getValue()Ljava/lang/Object;", remap = false))
    public void capture(CallbackInfo ci, @Local LocalRef<Int2ObjectMap.Entry<Entity>> entry) {
        tickedEntities.get().add(entry.get().getIntKey());
    }

    @Inject(method = "tickEntities", at = @At(value = "RETURN"))
    public void tickEnd(CallbackInfo ci) {
        tickedEntities.remove();
    }
}
