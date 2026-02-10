package cam72cam.mod.mixin.feat.data_registry;

import cam72cam.mod.event.platform.RegisterBlockTagEvent;
import cam72cam.mod.event.platform.RegisterItemTagEvent;
import cam72cam.mod.item.Fuzzy;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagLoader;
import net.minecraft.tags.TagManager;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mixin(TagLoader.class)
public class MixinTagCollection {

    @Shadow
    @Final
    private String directory;

    @Inject(method = "load", at = @At("RETURN"), remap = false)
    public void onRegisterTag(ResourceManager p_144496_, CallbackInfoReturnable<Map<ResourceLocation, List<TagLoader.EntryWithSource>>> cir) {
        Map<ResourceLocation, List<TagLoader.EntryWithSource>> map = cir.getReturnValue();
        if (this.directory.contains("block")) {
            RegisterBlockTagEvent event = new RegisterBlockTagEvent(map);
            ModLoader.get().postEvent(event);
        } else if (this.directory.contains("item")) {
            Fuzzy.lookup = new HashMap<>();
            RegisterItemTagEvent event = new RegisterItemTagEvent(map);
            ModLoader.get().postEvent(event);
            for (Map.Entry<ResourceLocation, List<TagLoader.EntryWithSource>> entry : map.entrySet()) {
                Fuzzy.lookup.computeIfAbsent(entry.getKey(), k -> new HashSet<>())
                            .addAll(entry.getValue().stream().map(TagLoader.EntryWithSource::toString).collect(Collectors.toSet()));
            }
        }
    }
}
