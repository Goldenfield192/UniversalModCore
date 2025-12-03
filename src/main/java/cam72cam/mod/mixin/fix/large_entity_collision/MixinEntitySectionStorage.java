package cam72cam.mod.mixin.fix.large_entity_collision;

import cam72cam.mod.entity.ModdedEntity;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;


/**
 * Fixes collision detection and ray tracing for entities that span multiple chunks.
 * <p>
 * Since Minecraft 1.17, the game only checks for entities within the chunks that the
 * given bounding box intersects. This causes issues with large entities that extend beyond
 * their primary chunk, as parts of them in other chunks may be ignored during collision
 * detection and ray tracing operations.
 * <p>
 * This mixin adds a check specifically for our {@link ModdedEntity} instances
 * to ensure that all relevant entity sections are considered, restoring proper functionality
 * for large entities that cross chunk boundaries.
 */
@Mixin(EntitySectionStorage.class)
public class MixinEntitySectionStorage<T extends EntityAccess>  {
    @Shadow
    @Final
    private Long2ObjectMap<EntitySection<T>> sections;

//    @ModifyArg(method = "forEachAccessibleNonEmptySection", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/SectionPos;posToSectionCoord(D)I", ordinal = 0))
//    private  double mod0(double p_175553_) {
//        return p_175553_ - 32;
//    }
//
//    @ModifyArg(method = "forEachAccessibleNonEmptySection", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/SectionPos;posToSectionCoord(D)I", ordinal = 1))
//    private  double mod1(double p_175553_) {
//        return p_175553_ - 16;
//    }
//
//    @ModifyArg(method = "forEachAccessibleNonEmptySection", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/SectionPos;posToSectionCoord(D)I", ordinal = 2))
//    private  double mod2(double p_175553_) {
//        return p_175553_ - 32;
//    }
//
//    @ModifyArg(method = "forEachAccessibleNonEmptySection", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/SectionPos;posToSectionCoord(D)I", ordinal = 3))
//    private  double mod3(double p_175553_) {
//        return p_175553_ + 32;
//    }
//
//    @ModifyArg(method = "forEachAccessibleNonEmptySection", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/SectionPos;posToSectionCoord(D)I", ordinal = 4))
//    private  double mod4(double p_175553_) {
//        return p_175553_ + 16;
//    }
//
//    @ModifyArg(method = "forEachAccessibleNonEmptySection", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/SectionPos;posToSectionCoord(D)I", ordinal = 5))
//    private  double mod5(double p_175553_) {
//        return p_175553_ + 32;
//    }

    @Inject(method = "forEachAccessibleNonEmptySection", at = @At("RETURN"))
    public void inject(AABB p_188363_, AbortableIterationConsumer<EntitySection<T>> p_261588_, CallbackInfo ci) {
        this.sections.values().stream()
                     .filter(e -> e.getStatus().isAccessible())
                     .filter(e -> {
                         Collection<ModdedEntity> moddedEntities = e.storage.find(ModdedEntity.class);
                         return !moddedEntities.isEmpty() && moddedEntities.stream().anyMatch(en -> en.getBoundingBox().intersects(p_188363_));
                     })
                     .forEach(p_261588_::accept);
    }
}
