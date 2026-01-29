package cam72cam.mod.mixin.feat.advencement_register;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementManager;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.FrameType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(AdvancementManager.class)
public class MixinAdvancementManager {
    @Inject(method = "loadCustomAdvancements", at = @At(value = "RETURN", ordinal = 1))
    public void load(CallbackInfoReturnable<Map<ResourceLocation, Advancement.Builder>> cir) {
        Map<ResourceLocation, Advancement.Builder> builderMap = cir.getReturnValue();
//        for (cam72cam.mod.advancement.Advancement.Builder builder : cam72cam.mod.advancement.Advancement.builders) {
//            DisplayInfo info = new DisplayInfo(builder.logo.internal,
//                                               new TextComponentTranslation(builder.translation),
//                                               new TextComponentTranslation(builder.description),
//                                               null,
//                                               FrameType.GOAL,
//                                               true, true, false);
//            Advancement.Builder builder1 = new Advancement.Builder(null, info, builder.reward.internal(), )
//            builderMap.put(builder.ident.internal, builder1);
//        }
    }
}
