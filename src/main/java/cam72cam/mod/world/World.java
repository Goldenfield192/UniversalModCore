package cam72cam.mod.world;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.ModCore;
import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.block.BlockType;
import cam72cam.mod.block.tile.TileEntity;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Living;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.BoundingBox;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.fluid.ITank;
import cam72cam.mod.item.IInventory;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import cam72cam.mod.serialization.TagCompound;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.apache.commons.lang3.NotImplementedException;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/** Wraps both ClientLevel and ServerLevel */
public class World {

    /* Static access to loaded worlds */
    private static final Map<String, World> clientWorlds = new HashMap<>();
    private static final Map<String, World> serverWorlds = new HashMap<>();
    private static final List<Consumer<World>> onTicks = new ArrayList<>();

    /** Internal, do not use */
    public final Level internal;
    /** isClient == world.isRemote */
    public final boolean isClient;
    /** isServer != world.isRemote */
    public final boolean isServer;

    private final Map<Integer, Entity> entityByID = new HashMap<>();
    private final Map<UUID, Entity> entityByUUID = new HashMap<>();
    private final Map<Class<?>, List<Entity>> entitiesByClass = new HashMap<>();

    /* World Initialization */

    private World(Level world) {
        internal = world;
        isClient = world.isClientSide;
        isServer = !world.isClientSide;
    }

    /** Helper function to get a world map (client or server) */
    private static Map<String, World> getWorldMap(Level world) {
        return world.isClientSide ? clientWorlds : serverWorlds;
    }
    /** Helper function to get a world in it's respective map */
    private static World getWorld(Level world){
        return getWorldMap(world).get(world.dimension().location().toString());
    }

    /** Load world hander, sets up maps and internal handlers */
    private static void loadWorld(Level world) {
        if (getWorld(world) == null) {
            World worldWrap = new World(world);
            getWorldMap(world).put(worldWrap.getId(), worldWrap);
        }
    }

    /** Called from Event system, wires into common world events */
    public static void registerEvents() {
        CommonEvents.Entity.JOIN.subscribe((world, entity) -> {
            get(world).onEntityAdded(entity);
            return true;
        });

        CommonEvents.World.LOAD.subscribe(World::loadWorld);

        CommonEvents.World.UNLOAD.subscribe(world -> getWorldMap(world).remove(world.dimension().location().toString()));

        CommonEvents.World.TICK.subscribe(world -> onTicks.forEach(fn -> fn.accept(get(world))));

        CommonEvents.World.TICK.subscribe(world -> get(world).checkLoadedEntities());
    }

    public static void registerClientEvnets() {
        ClientEvents.TICK.subscribe(() -> {
            if (MinecraftClient.isReady()) {
                MinecraftClient.getPlayer().getWorld().checkLoadedEntities();
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    private Iterable<net.minecraft.world.entity.Entity> clientEntities() {
        return internal instanceof ClientLevel ? ((ClientLevel) internal).entitiesForRendering() : ((ServerLevel) internal).getEntities().getAll();
    }

    @OnlyIn(Dist.DEDICATED_SERVER)
    private Iterable<net.minecraft.world.entity.Entity> serverEntities() {
        return ((ServerLevel) internal).getEntities().getAll();
    }

    private void checkLoadedEntities() {
        Iterable<net.minecraft.world.entity.Entity> internalEntities = DistExecutor.runForDist(
                () -> this::clientEntities,
                () -> this::serverEntities
        );

        // Once a tick scan entities that may have de-sync'd with the UMC world
        for (net.minecraft.world.entity.Entity entity : internalEntities) {
            Entity found = this.entityByID.get(entity.getId());
            if (found == null) {
                ModCore.debug("Adding entity that was not wrapped correctly %s - %s", entity.getUUID(), entity);
                this.onEntityAdded(entity);
            } else if (found.internal != entity) {
                // For some reason, this can happen on the client.  I'm guessing entities pop in and out of render distance
                ModCore.debug("Mismatching world entity %s - %s", entity.getId(), entity);
                this.onEntityRemoved(found.internal);
                this.onEntityAdded(entity);
            }
        }
        for (Entity entity : new ArrayList<>(this.entityByID.values())) {
            if (internal.getEntity(entity.getId()) == null) {
                ModCore.debug("Dropping entity that was not removed correctly %s - %s", entity.getUUID(), entity);
                this.onEntityRemoved(entity.internal);
            }
        }
    }

    /** Turn a MC world into a UMC world */
    public static World get(Level world) {
        if (world == null) {
            return null;
        }
        if (getWorld(world) == null) {
            // WTF forge
            // I should NOT need to do this
            loadWorld(world);
        }

        return getWorld(world);
    }

    /** Based on dim/isRemote get the corresponding UMC world.  Not recommended for general use. */
    public static World get(String registryKey, boolean isClient) {
        return (isClient ? clientWorlds : serverWorlds).get(registryKey);
    }

    /** Add tick handler */
    public static void onTick(Consumer<World> fn) {
        onTicks.add(fn);
    }

    /** World's internal ID, Not recommended for general use. */
    public String getId() {
        return internal.dimension().location().toString();
    }

    /* Event Methods */

    /**
     * Handle tracking entities that have been added to the internal world.
     * Wiring from WorldEventListener
     */
    void onEntityAdded(net.minecraft.world.entity.Entity entityIn) {
        if (entityByID.containsKey(entityIn.getId())) {
            // Dupe
            return;
        }

        Entity entity;
        if (entityIn instanceof ModdedEntity) {
            entity = ((ModdedEntity) entityIn).getSelf();
        } else if (entityIn instanceof net.minecraft.world.entity.player.Player) {
            entity = new Player((net.minecraft.world.entity.player.Player) entityIn);
        } else if (entityIn instanceof LivingEntity) {
            entity = new Living((LivingEntity) entityIn);
        } else {
            entity = new Entity(entityIn);
        }
        entitiesByClass.putIfAbsent(entity.getClass(), new ArrayList<>());
        entitiesByClass.get(entity.getClass()).add(entity);
        entityByID.put(entityIn.getId(), entity);
        entityByUUID.put(entity.getUUID(), entity);
    }

    /**
     * Handle tracking entities that have been removed from the internal world.
     * Wiring from WorldEventListener
     */
    void onEntityRemoved(net.minecraft.world.entity.Entity entity) {
        if(entity == null) {
            ModCore.warn("Somehow removed a null entity?");
            return;
        }
        for (List<Entity> value : entitiesByClass.values()) {
            value.removeIf(inner -> inner.getUUID().equals(entity.getUUID()));
        }
        entityByID.remove(entity.getId());
        entityByUUID.remove(entity.getUUID());
    }

    /* Entity Methods */

    /** Find a UMC entity by MC entity */
    public Entity getEntity(net.minecraft.world.entity.Entity entity) {
        return getEntity(entity.getUUID(), Entity.class);
    }

    /** Find a UMC entity by MC ID and Entity class */
    public <T extends Entity> T getEntity(int id, Class<T> type) {
        Entity ent = entityByID.get(id);
        if (ent == null) {
            return null;
        }
        if (!type.isInstance(ent)) {
            ModCore.warn("When looking for entity %s by id %s, we instead got a %s", type, id, ent.getClass());
            return null;
        }
        return (T) ent;
    }

    /** Find UMC entity by MC Entity, assuming type */
    public <T extends Entity> T getEntity(UUID id, Class<T> type) {
        Entity ent = entityByUUID.get(id);
        if (ent == null) {
            return null;
        }
        if (!type.isInstance(ent)) {
            ModCore.warn("When looking for entity %s by id %s, we instead got a %s", type, id, ent.getClass());
            return null;
        }
        return (T) ent;
    }

    /** Find UMC entities by type */
    public <T extends Entity> List<T> getEntities(Class<T> type) {
        return getEntities((T val) -> true, type);
    }

    /** Find UMC Entities which match the filter and are of the given type */
    public <T extends Entity> List<T> getEntities(Predicate<T> filter, Class<T> type) {
        List<T> list = new ArrayList<>();
        for (Class<?> key : entitiesByClass.keySet()) {
            if (type.isAssignableFrom(key)) {
                for (Entity entity : entitiesByClass.get(key)) {
                    T as = entity.as(type);
                    if (as != null) {
                        if (filter.test(as)) {
                            list.add(as);
                        }
                    }
                }
            }
        }
        return list;
    }

    /** Add a constructed entity to the world */
    public boolean spawnEntity(Entity ent) {
        return internal.addFreshEntity(ent.internal);
    }

    /** Kill an entity */
    public void removeEntity(Entity entity) {
        entity.internal.remove(!isClient ? net.minecraft.world.entity.Entity.RemovalReason.KILLED : net.minecraft.world.entity.Entity.RemovalReason.DISCARDED); // TODO MAYBE BORK
    }

    /** Force a chunk for up to 5s */
    public void keepLoaded(Vec3i pos) {
        ChunkManager.flagEntityPos(this, pos);
    }

    /** Internal, do not use */
    public <T extends net.minecraft.world.level.block.entity.BlockEntity> T getTileEntity(Vec3i pos, Class<T> cls) {
        net.minecraft.world.level.block.entity.BlockEntity ent = internal.getBlockEntity(pos.internal());

        if (cls.isInstance(ent)) {
            return (T) ent;
        }
        return null;
    }

    /** Get all block entities of the given type */
    public <T extends BlockEntity> List<T> getBlockEntities(Class<T> cls) {
        /*
        return internal.blockEntityList.stream()
                .filter(x -> x instanceof TileEntity && ((TileEntity) x).isLoaded() && cls.isInstance(((TileEntity) x).instance()))
                .map(x -> (T) ((TileEntity) x).instance())
                .collect(Collectors.toList());*/
        throw new NotImplementedException("TODO remove from UMC!");
    }

    /** Get a block entity at the position, assuming type */
    public <T extends BlockEntity> T getBlockEntity(Vec3i pos, Class<T> cls) {
        TileEntity te = getTileEntity(pos, TileEntity.class);
        if (te == null) {
            return null;
        }
        BlockEntity instance = te.instance();
        if (cls.isInstance(instance)) {
            return (T) instance;
        }
        return null;
    }

    /** Does this block have a block entity of the given type? */
    public <T extends BlockEntity> boolean hasBlockEntity(Vec3i pos, Class<T> cls) {
        TileEntity te = getTileEntity(pos, TileEntity.class);
        if (te == null) {
            return false;
        }
        return cls.isInstance(te.instance());
    }

    /**
     * Turn the given data back into a block
     *
     * @see BlockEntity#getData
     */
    public BlockEntity reconstituteBlockEntity(TagCompound datain) {
        TagCompound data = TileEntity.legacyConverter(datain);
        // TODO 1.16 null state
        BlockPos blockpos = new BlockPos(data.internal.getInt("x"), data.internal.getInt("y"), data.internal.getInt("z"));
        TileEntity te = (TileEntity) TileEntity.loadStatic(blockpos, null, data.internal);
        if (te == null) {
            ModCore.warn("BAD TE DATA " + data);
            return null;
        }
        te.setLevel(internal);
        if (te.instance() == null) {
            ModCore.warn("Loaded " + te.isLoaded() + " " + data);
        }
        return te.instance();
    }

    /** Set the block entity at pos to given entity */
    public void setBlockEntity(Vec3i pos, BlockEntity entity) {
        if (entity != null) {
            entity.internal.setPos(pos.internal());
            internal.setBlockEntity(entity.internal);
            entity.markDirty();
        } else {
            internal.removeBlockEntity(pos.internal());
        }
    }

    /** World time in ticks (Day/Night cycle time)*/
    public long getTime() {
        return internal.getDayTime();
    }

    /** Time since world was originally created (updated when loaded) */
    public long getTicks() {
        return internal.getGameTime();
    }

    /** Ticks per second (with up to N samples) */
    public double getTPS(int sampleSize) {
        if (internal.getServer() == null) {
            return 20;
        }

        long[] ttl = internal.getServer().tickTimes;

        sampleSize = Math.min(sampleSize, ttl.length);
        double ttus = 0;
        for (int i = 0; i < sampleSize; i++) {
            ttus += ttl[ttl.length - 1 - i] / (double) sampleSize;
        }

        if (ttus == 0) {
            ttus = 0.01;
        }

        double ttms = ttus * 1.0E-6D;
        return Math.min(1000.0 / ttms, 20);
    }

    /** Height of the ground for precipitation purposes at the given block */
    public Vec3i getPrecipitationHeight(Vec3i pos) {
        return new Vec3i(internal.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, pos.internal()));
    }

    /** Set the given pos to air */
    public void setToAir(Vec3i pos) {
        internal.removeBlockEntity(pos.internal());
        internal.removeBlock(pos.internal(), false);
    }

    /** If the block at pos is air */
    public boolean isAir(Vec3i ph) {
        return internal.isEmptyBlock(ph.internal());
    }

    /** Set the snow level to the given depth (1-8) */
    public void setSnowLevel(Vec3i ph, int snowDown) {
        snowDown = Math.max(1, Math.min(8, snowDown));
        if (snowDown == 8) {
            internal.setBlockAndUpdate(ph.internal(), Blocks.SNOW_BLOCK.defaultBlockState());
        } else {
            internal.setBlockAndUpdate(ph.internal(), Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, snowDown));
        }
    }

    /** Get the snow level (1-8) */
    public int getSnowLevel(Vec3i ph) {
        BlockState state = internal.getBlockState(ph.internal());
        if (state.getBlock() == Blocks.SNOW) {
            return state.getValue(SnowLayerBlock.LAYERS);
        }
        if (state.getBlock() == Blocks.SNOW_BLOCK) {
            return 8;
        }
        return 0;
    }

    /** If this block is snow or snow layers */
    public boolean isSnow(Vec3i ph) {
        Block block = internal.getBlockState(ph.internal()).getBlock();
        return block == Blocks.SNOW || block == Blocks.SNOW_BLOCK;
    }

    /** If it is snowing or raining */
    public boolean isPrecipitating() {
        return internal.isRaining();
    }

    /** If it is is raining */
    public boolean isRaining(Vec3i position) {
        return isPrecipitating() && internal.getBiome(position.internal()).value().getPrecipitationAt(position.internal()) == Biome.Precipitation.RAIN;
    }

    /** If it is snowing */
    public boolean isSnowing(Vec3i position) {
        return isPrecipitating() && internal.getBiome(position.internal()).value().getPrecipitationAt(position.internal()) == Biome.Precipitation.SNOW;
    }

    /** Temp in celsius */
    public float getTemperature(Vec3i pos) {
        // TODO 1.18.2 float mctemp = internal.getBiome(pos.internal()).value().getTemperature(pos.internal());
        float mctemp = internal.getBiome(pos.internal()).value().getBaseTemperature();
        //https://www.reddit.com/r/Minecraft/comments/3eh7yu/the_rl_temperature_of_minecraft_biomes_revealed/ctex050/
        return (13.6484805403f * mctemp) + 7.0879687222f;
    }

    /** Drop a stack on the ground at pos */
    public void dropItem(ItemStack stack, Vec3i pos) {
        dropItem(stack, new Vec3d(pos));
    }

    /** Drop a stack on the ground at pos */
    public void dropItem(ItemStack stack, Vec3d pos) {
        internal.addFreshEntity(new ItemEntity(internal, pos.x, pos.y, pos.z, stack.internal()));
    }

    /** Check if the block is currently in a loaded chunk */
    public boolean isBlockLoaded(Vec3i parent) {
        ChunkAccess chunk = internal.getChunkSource().getChunk(parent.x >> 4, parent.z >> 4, ChunkStatus.EMPTY, false);
        return (chunk != null && chunk.getStatus() == ChunkStatus.FULL)
                && internal.isLoaded(parent.internal());
    }

    /** Check if block at pos collides with a BB */
    public boolean doesBlockCollideWith(Vec3i bp, IBoundingBox bb) {
        IBoundingBox bbb = IBoundingBox.from(internal.getBlockState(bp.internal()).getShape(internal, bp.internal()).bounds().move(bp.internal().below()));
        return bb.intersects(bbb);
    }

    public List<Vec3i> blocksInBounds(IBoundingBox bb) {
        return StreamSupport.stream(internal.getBlockCollisions(null, BoundingBox.from(bb)).spliterator(), false)
                .map(VoxelShape::bounds)
                .filter(blockBox -> bb.intersects(IBoundingBox.from(blockBox)))
                .map(blockBox -> new Vec3i(blockBox.minX, blockBox.minY, blockBox.minZ))
                .collect(Collectors.toList());
    }

    /** Break block (with in-world drops) */
    public void breakBlock(Vec3i pos) {
        this.breakBlock(pos, true);
    }

    /** Break block with sound effecnts, particles and optional drops */
    public void breakBlock(Vec3i pos, boolean drop) {
        internal.destroyBlock(pos.internal(), drop);
    }

    /** If block is the given type */
    public boolean isBlock(Vec3i pos, BlockType block) {
        return internal.getBlockState(pos.internal()).getBlock() == block.internal;
    }

    /** Set block to a given block type */
    public void setBlock(Vec3i pos, BlockType block) {
        internal.setBlockAndUpdate(pos.internal(), block.internal.defaultBlockState());
    }

    /** Set a block to given stack (best guestimate) */
    public void setBlock(Vec3i pos, ItemStack stack) {
        Block state = Block.byItem(stack.internal().getItem()); //TODO 1.14.4 .getStateFromMeta(stack.internal.getMetadata());
        internal.setBlockAndUpdate(pos.internal(), state.defaultBlockState());
    }

    /** Is the top of the block solid?  Based on some AABB nonsense */
    public boolean isTopSolid(Vec3i pos) {
        return Block.canSupportCenter(internal, pos.internal(), Direction.UP);
    }

    /** How hard is the block? */
    public float getBlockHardness(Vec3i pos) {
        return internal.getBlockState(pos.internal()).getDestroySpeed(internal, pos.internal());
    }

    /** Get max redstone power surrounding this block */
    public int getRedstone(Vec3i pos) {
        int power = 0;
        for (Facing facing : Facing.values()) {
            power = Math.max(power, internal.getSignal(pos.offset(facing).internal(), facing.internal));
        }
        return power;
    }

    /** If the sky is visible at this position */
    public boolean canSeeSky(Vec3i position) {
        return internal.canSeeSky(position.internal());
    }

    /**
     * Some generic rules for if a block is replaceable
     *
     * This mainly relies on Block.isReplaceable, but not all mod authors hook into it correctly.
     */
    public boolean isReplaceable(Vec3i pos) {
        if (isAir(pos)) {
            return true;
        }

        Block block = internal.getBlockState(pos.internal()).getBlock();

        if (internal.getBlockState(pos.internal()).canBeReplaced()) {
            return true;
        }
        if (block instanceof BushBlock) {
            return true;
        }
        if (block instanceof IPlantable) {
            return true;
        }
        if (block instanceof LiquidBlock) {
            return true;
        }
        if (block instanceof SnowLayerBlock || block == Blocks.SNOW_BLOCK) {
            return true;
        }
        if (block instanceof LeavesBlock) {
            return true;
        }
        return false;
    }

    /* Capabilities */

    /** Get the inventory at this block (accessed from any side) */
    public IInventory getInventory(Vec3i offset) {
        for (Facing value : Facing.values()) {
            IInventory inv = getInventory(offset, value);
            if (inv != null) {
                return inv;
            }
        }
        return getInventory(offset, null);
    }

    /** Get the inventory at this block (accessed from given side) */
    public IInventory getInventory(Vec3i offset, Facing dir) {
        net.minecraft.world.level.block.entity.BlockEntity te = internal.getBlockEntity(offset.internal());
        Direction face = dir != null ? dir.internal : null;
        if (te != null && te.getCapability(ForgeCapabilities.ITEM_HANDLER, face).isPresent()) {
            IItemHandler inv = te.getCapability(ForgeCapabilities.ITEM_HANDLER, face).orElse(null);
            if (inv instanceof IItemHandlerModifiable) {
                return IInventory.from((IItemHandlerModifiable) inv);
            }
        }
        return null;
    }

    /** Get the tank at this block (accessed from any side) */
    public List<ITank> getTank(Vec3i offset) {
        for (Facing value : Facing.values()) {
            List<ITank> tank = getTank(offset, value);
            if (tank != null) {
                return tank;
            }
        }
        return getTank(offset, null);
    }

    /** Get the tank at this block (accessed from given side) */
    public List<ITank> getTank(Vec3i offset, Facing dir) {
        net.minecraft.world.level.block.entity.BlockEntity te = internal.getBlockEntity(offset.internal());
        Direction face = dir != null ? dir.internal : null;
        if (te != null && te.getCapability(ForgeCapabilities.FLUID_HANDLER, face).isPresent()) {
            IFluidHandler tank = te.getCapability(ForgeCapabilities.FLUID_HANDLER, face).orElse(null);
            if (tank != null) {
                return ITank.getTank(tank);
            }
        }
        return null;
    }

    /** Get stack equiv of block at pos (Unreliable!) */
    public ItemStack getItemStack(Vec3i pos) {
        BlockState state = internal.getBlockState(pos.internal());
        try {
            return new ItemStack(state.getBlock().getCloneItemStack(internal, pos.internal(), state));
        } catch (Exception ex) {
            return new ItemStack(new net.minecraft.world.item.ItemStack(state.getBlock()));
        }
    }

    /** Get dropped items within the given area */
    public List<ItemStack> getDroppedItems(IBoundingBox bb) {
        List<ItemEntity> items = internal.getEntitiesOfClass(ItemEntity.class, BoundingBox.from(bb));
        return items.stream().map((ItemEntity::getItem)).map(ItemStack::new).collect(Collectors.toList());
    }

    /** Get a BlockInfo that can be used to overwrite a block in the future.  Does not currently include TE data */
    public BlockInfo getBlock(Vec3i pos) {
        return new BlockInfo(internal.getBlockState(pos.internal()));
    }

    /** Overwrite the block at pos from the given info */
    public void setBlock(Vec3i pos, BlockInfo info) {
        internal.removeBlockEntity(pos.internal());
        internal.setBlockAndUpdate(pos.internal(), info.internal);
    }

    /** Opt in collision overriding */
    public boolean canEntityCollideWith(Vec3i bp, String damageType) {
        Block block = internal.getBlockState(bp.internal()).getBlock();
        return ! (block instanceof IConditionalCollision) ||
                ((IConditionalCollision) block).canCollide(internal, bp.internal(), internal.getBlockState(bp.internal()), new DamageSource(Holder.direct(new DamageType(damageType, DamageScaling.NEVER, /*(float) damage*/ 0f))));
    }

    /** Spawn a particle */
    public void createParticle(ParticleType type, Vec3d position, Vec3d velocity) {
        internal.addParticle(type.internal, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
    }

    /**
     *
     * Updates the blocks around the position.
     * Value updateObservers will be ignored in some MC versions.
     *
     * @param pos
     * @param blockType
     * @param updateObservers
     */
    public void notifyNeighborsOfStateChange(Vec3i pos, BlockType blockType, boolean updateObservers){
        this.internal.updateNeighborsAt(pos.internal(), blockType.internal);
    }

    public enum ParticleType {
        SMOKE(ParticleTypes.SMOKE),
        // Incomplete
        ;

        private final SimpleParticleType internal;

        ParticleType(SimpleParticleType internal) {
            this.internal = internal;
        }
    }

    public float getBlockLightLevel(Vec3i pos) {
        return internal.getBrightness(LightLayer.BLOCK, pos.internal()) / 15f;
    }

    public float getSkyLightLevel(Vec3i pos) {
        return internal.getBrightness(LightLayer.SKY, pos.internal()) / 15f;
    }
}
