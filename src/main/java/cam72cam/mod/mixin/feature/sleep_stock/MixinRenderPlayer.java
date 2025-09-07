package cam72cam.mod.mixin.feature.sleep_stock;

import cam72cam.mod.entity.SeatEntity;
import cam72cam.mod.entity.custom.IRidable;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderPlayer.class)
public class MixinRenderPlayer {
    @Redirect(method = "applyRotations(Lnet/minecraft/client/entity/AbstractClientPlayer;FFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/AbstractClientPlayer;isPlayerSleeping()Z"))
    public boolean redirect(AbstractClientPlayer instance) {
        return instance.isPlayerSleeping()
                || (instance.getRidingEntity() instanceof SeatEntity
                    && ((SeatEntity) instance.getRidingEntity()).state == IRidable.PlayerState.LYING);
    }

    @Redirect(method = "applyRotations(Lnet/minecraft/client/entity/AbstractClientPlayer;FFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/AbstractClientPlayer;getBedOrientationInDegrees()F"))
    public float redirectBed(AbstractClientPlayer instance) {
        if (instance.isPlayerSleeping()){
            return instance.getBedOrientationInDegrees();
        } else {
            if(((SeatEntity) instance.getRidingEntity()).getParent() != null){
                return instance.getRotationYawHead();
            } else {
                return 0F;
            }
        }
    }
}
