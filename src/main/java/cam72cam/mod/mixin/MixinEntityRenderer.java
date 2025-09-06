package cam72cam.mod.mixin;

import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {
    @ModifyConstant(method = "updateRenderer", constant = @Constant(floatValue = 4.0F))
    public float inject1(float constant) {
        return 4f;
    }

    @ModifyConstant(method = "orientCamera", constant = @Constant(floatValue = 4.0F))
    public float inject2(float constant) {
        return 4f;
    }
}
