package cam72cam.mod.mixin.feat.vectorlized_collision;

import cam72cam.mod.entity.boundingbox.BoundingBox;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.math.Vec3d;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalDoubleRef;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.math.AxisAlignedBB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(Particle.class)
public abstract class MixinParticle {
    @Shadow
    public abstract AxisAlignedBB getBoundingBox();

    @Shadow
    public abstract void setBoundingBox(AxisAlignedBB bb);

    @Shadow
    private AxisAlignedBB boundingBox;

    @Inject(method = "move", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/World;getCollisionBoxes(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/AxisAlignedBB;)Ljava/util/List;"))
    public void handleVectorMovement(double x, double y, double z, CallbackInfo ci, @Local List<AxisAlignedBB> list,
                                     @Local(argsOnly = true, ordinal = 0) LocalDoubleRef x1,
                                     @Local(argsOnly = true, ordinal = 1) LocalDoubleRef y1,
                                     @Local(argsOnly = true, ordinal = 2) LocalDoubleRef z1) {
        IBoundingBox box = IBoundingBox.from(this.getBoundingBox());
        Vec3d iter = new Vec3d(x, y, z);
        for (AxisAlignedBB axisAlignedBB : new ArrayList<>(list)) {
            if (axisAlignedBB instanceof BoundingBox) {
                BoundingBox boundingBox = (BoundingBox) axisAlignedBB;
                iter = boundingBox.internal.adjustMovement(box, iter);
                list.remove(axisAlignedBB);
            }
        }
        x1.set(iter.x);
        y1.set(iter.y);
        z1.set(iter.z);
    }
}
