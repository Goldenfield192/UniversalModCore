package cam72cam.mod.render;

import cam72cam.mod.ModCore;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.client.renderer.texture.atlas.SpriteSources;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.inventory.InventoryMenu;

public class UMCSpriteSource implements SpriteSource {
    private static final Codec<UMCSpriteSource> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(ResourceLocation.CODEC.fieldOf("main").forGetter((source) -> source.self))
                                .apply(instance, UMCSpriteSource::new)
    );
    private static final SpriteSourceType UMC = SpriteSources.register(ModCore.MODID, CODEC);

    public static final ResourceLocation MAIN = InventoryMenu.BLOCK_ATLAS;
    ResourceLocation self;

    public UMCSpriteSource(ResourceLocation self) {
        this.self = self;
    }

    @Override
    public void run(ResourceManager p_261770_, Output p_261757_) {
    }

    @Override
    public SpriteSourceType type() {
        return UMC;
    }
}
