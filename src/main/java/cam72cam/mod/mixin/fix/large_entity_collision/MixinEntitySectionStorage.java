package cam72cam.mod.mixin.fix.large_entity_collision;

import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.world.UMCEntityManager;
import cam72cam.mod.world.World;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import net.minecraft.core.SectionPos;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.Set;


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

    @Shadow
    @Final
    private LongSortedSet sectionIds;

    @Inject(method = "forEachAccessibleNonEmptySection", at = @At("HEAD"))
    public void init(AABB p_188363_, AbortableIterationConsumer<EntitySection<T>> p_261588_, CallbackInfo ci,
                     @Share("level")LocalRef<UMCEntityManager> levelLocalRef, @Share("pos")LocalRef<Set<Long>> setLocalRef) {
        //Try to get corresponding level of the search
        Optional<Long2ObjectMap.Entry<EntitySection<T>>> any = this.sections.long2ObjectEntrySet().stream().filter(
                entry -> !entry.getValue().storage.isEmpty()).findAny();
        any.flatMap(entitySectionEntry -> entitySectionEntry.getValue().getEntities().filter(
                e -> e instanceof Entity).findFirst()).ifPresent(e -> {
                    levelLocalRef.set(World.get(((Entity)e).level()).tracker);
                    setLocalRef.set(new LongArraySet());
        });
    }

    @Inject(method = "forEachAccessibleNonEmptySection", at = @At(value = "INVOKE_ASSIGN", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;get(J)Ljava/lang/Object;",
            shift = At.Shift.BY, by = 2)) //To capture the EntitySection
    public void capture(AABB p_188363_, AbortableIterationConsumer<EntitySection<T>> p_261588_, CallbackInfo ci,
                        @Share("level")LocalRef<UMCEntityManager> levelLocalRef, @Share("pos")LocalRef<Set<Long>> setLocalRef,
                        @Local LocalRef<EntitySection<T>> sectionLocalRef, @Local(ordinal = 2) long k2) {
        if (levelLocalRef.get() != null) {
            EntitySection<T> section = sectionLocalRef.get();
            if (section != null && section.getStatus().isAccessible() && !section.isEmpty()) {
                setLocalRef.get().addAll(levelLocalRef.get().queryPotentialPos(k2));
            }
        }
    }

    @Inject(method = "forEachAccessibleNonEmptySection", at = @At("RETURN"))
    public void finish(AABB p_188363_, AbortableIterationConsumer<EntitySection<T>> p_261588_, CallbackInfo ci,
        @Share("level")LocalRef<UMCEntityManager> levelLocalRef, @Share("pos")LocalRef<Set<Long>> setLocalRef) {
        if (levelLocalRef.get() != null) {
            for (long l1 : setLocalRef.get()) {
                if(!sectionIds.contains(l1)) {
                    EntitySection<T> tEntitySection = sections.get(l1);
                    if (tEntitySection != null && !tEntitySection.getStatus().isAccessible() && !tEntitySection.isEmpty()) {
                        p_261588_.accept(tEntitySection);
                    }
                }
            }
        }
    }
}
