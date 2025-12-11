package cam72cam.mod.world;

import cam72cam.mod.entity.ModdedEntity;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityEvent;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class UMCEntityManager {
    private static final int HORIZONTAL_SEARCH_RADIUS = 3;
    private static final int VERTICAL_SEARCH_RADIUS = 2;
    private final Map<Long, Set<ModdedEntity>> entitySections = new Long2ObjectOpenHashMap<>();
    private final FastUtilMultiMap scanningRange = new FastUtilMultiMap();

    public void join(ModdedEntity entity) {
        Vec3 vec3d = entity.getEyePosition();
        int x = (int) vec3d.x;
        int y = (int) vec3d.y;
        int z = (int) vec3d.z;
        long sec = SectionPos.asLong(x, y, z);
        synchronized (this) {
            Set<ModdedEntity> moddedEntities = entitySections.get(sec);
            if (moddedEntities == null) {
                //Newly added
                moddedEntities = new ObjectArraySet<>();
                entitySections.put(sec, moddedEntities);


                for (int i = x - HORIZONTAL_SEARCH_RADIUS; i <= x + HORIZONTAL_SEARCH_RADIUS; i++) {
                    for (int j = z - HORIZONTAL_SEARCH_RADIUS; j <= z + HORIZONTAL_SEARCH_RADIUS; j++) {
                        for (int k = y - VERTICAL_SEARCH_RADIUS; k <= y + VERTICAL_SEARCH_RADIUS; k++) {
                            scanningRange.put(sec, SectionPos.asLong(i, k, j));
                        }
                    }
                }
            }
            moddedEntities.add(entity);
        }
    }

    public void remove(ModdedEntity entity) {
        Vec3 vec3d = entity.getEyePosition();
        long sec = SectionPos.asLong((int) vec3d.x, (int) vec3d.y, (int) vec3d.z);
        if (!entitySections.containsKey(sec)) {
            return;
        }
        synchronized (this) {
            Collection<ModdedEntity> moddedEntities = entitySections.get(sec);
            moddedEntities.remove(entity);
            if (moddedEntities.isEmpty()) {
                entitySections.remove(sec);
                scanningRange.removeKey(sec);
            }
        }
    }

    public void move(EntityEvent.EnteringSection event) {
        long oldPos = event.getPackedOldPos();
        long newPos = event.getPackedNewPos();
        ModdedEntity entity = (ModdedEntity) event.getEntity();
        synchronized (this) {
            Set<ModdedEntity> moddedEntities = entitySections.get(oldPos);
            if (moddedEntities != null) {
                moddedEntities.remove(entity);
                if (moddedEntities.isEmpty()) {
                    entitySections.remove(oldPos);
                    scanningRange.removeKey(oldPos);
                }
            }

            Vec3 vec3d = entity.getEyePosition();
            int x = (int) vec3d.x;
            int y = (int) vec3d.y;
            int z = (int) vec3d.z;
            moddedEntities = entitySections.get(newPos);
            if (moddedEntities == null) {
                //Newly added
                moddedEntities = new ObjectArraySet<>();
                entitySections.put(newPos, moddedEntities);


                for (int i = x - HORIZONTAL_SEARCH_RADIUS; i <= x + HORIZONTAL_SEARCH_RADIUS; i++) {
                    for (int j = z - HORIZONTAL_SEARCH_RADIUS; j <= z + HORIZONTAL_SEARCH_RADIUS; j++) {
                        for (int k = y - VERTICAL_SEARCH_RADIUS; k <= y + VERTICAL_SEARCH_RADIUS; k++) {
                            scanningRange.put(newPos, SectionPos.asLong(i, k, j));
                        }
                    }
                }
            }
            moddedEntities.add(entity);
        }
    }

    public Collection<Long> queryPotentialPos(long pos) {
        Collection<Long> collection;
        synchronized (this) {
            collection = scanningRange.getKeys(pos);
        }
        return collection;
    }
}
