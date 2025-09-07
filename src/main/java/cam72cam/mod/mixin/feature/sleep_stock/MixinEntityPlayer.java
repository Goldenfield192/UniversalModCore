package cam72cam.mod.mixin.feature.sleep_stock;

import cam72cam.mod.entity.SeatEntity;
import cam72cam.mod.entity.custom.IRidable;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityPlayer.class)
public class MixinEntityPlayer {
    @Redirect(method = "getEyeHeight", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;isPlayerSleeping()Z"))
    public boolean red1(EntityPlayer instance) {
        return isPlayerLying(instance);
    }

    @Redirect(method = "updateSize", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;isPlayerSleeping()Z"))
    public boolean red2(EntityPlayer instance) {
        return isPlayerLying(instance);
    }

    private boolean isPlayerLying(EntityPlayer instance) {
        return instance.isPlayerSleeping()
                || (instance.getRidingEntity() instanceof SeatEntity
                && ((SeatEntity) instance.getRidingEntity()).state == IRidable.PlayerState.LYING);    }
}
