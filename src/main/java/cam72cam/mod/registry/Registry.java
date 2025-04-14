package cam72cam.mod.registry;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;

public class Registry {
    private static final HashMap<String, Registry> REGISTRY = new HashMap<>();

    public final DeferredRegister<Block> BLOCK;
    public final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY;
    public final DeferredRegister<Item> ITEM;

    private Registry(String modID){
        this.BLOCK = DeferredRegister.create(ForgeRegistries.BLOCKS, modID);
        this.BLOCK_ENTITY = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, modID);
        this.ITEM = DeferredRegister.create(ForgeRegistries.ITEMS, modID);
        REGISTRY.put(modID, this);
    }

    public static void create(String modID){
        new Registry(modID);
    }

    public static Registry getRegistry(String modID){
        return REGISTRY.get(modID);
    }

    public static void froze(String modID, IEventBus bus){
        REGISTRY.get(modID).BLOCK.register(bus);
        REGISTRY.get(modID).BLOCK_ENTITY.register(bus);
        REGISTRY.get(modID).ITEM.register(bus);
    }

    public static class ClientRegistry{

    }
}
