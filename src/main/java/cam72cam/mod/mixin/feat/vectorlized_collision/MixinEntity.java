package cam72cam.mod.mixin.feat.vectorlized_collision;

import cam72cam.mod.entity.boundingbox.BoundingBox;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.math.Vec3d;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalDoubleRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.util.math.AxisAlignedBB;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Debug(export = true)
@Mixin(Entity.class)
public abstract class MixinEntity {
    @Shadow
    public abstract AxisAlignedBB getEntityBoundingBox();

    @Shadow
    public abstract void setEntityBoundingBox(AxisAlignedBB bb);

    @Shadow
    public boolean collided;

    @Shadow
    public boolean onGround;

    @Shadow
    public double motionZ;

    @Shadow
    public double motionY;

    @Shadow
    public double motionX;

    @Inject(method = "move", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/World;getCollisionBoxes(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/AxisAlignedBB;)Ljava/util/List;", ordinal = 3))
    public void checkUMCCollision(MoverType type, double x, double y, double z, CallbackInfo ci,
                                  @Local List<AxisAlignedBB> list,
                                  @Local(argsOnly = true, ordinal = 0) LocalDoubleRef x1,
                                  @Local(argsOnly = true, ordinal = 1) LocalDoubleRef y1,
                                  @Local(argsOnly = true, ordinal = 2) LocalDoubleRef z1,
                                  @Share("velocity")LocalRef<Vec3d> velocity,
                                  @Share("collided")LocalBooleanRef collided) {
        IBoundingBox box = IBoundingBox.from(this.getEntityBoundingBox());
        Vec3d prev;
        Vec3d iter = new Vec3d(x, y, z);
        collided.set(false);

        for (AxisAlignedBB axisAlignedBB : new ArrayList<>(list)) {
            if (axisAlignedBB instanceof BoundingBox) {
                BoundingBox boundingBox = (BoundingBox) axisAlignedBB;
                prev = iter;
                iter = boundingBox.internal.adjustMovement(box, iter);
                list.remove(axisAlignedBB);
                if (iter.equals(prev)) {
                    collided.set(true);
                }
            }
        }

        x1.set(iter.x);
        y1.set(iter.y);
        z1.set(iter.z);
        velocity.set(iter);
    }

    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getEntityBoundingBox()Lnet/minecraft/util/math/AxisAlignedBB;", ordinal = 13))
    public void removeCollided(MoverType type, double x, double y, double z, CallbackInfo ci, @Local List<AxisAlignedBB> list) {
        list.removeIf(aabb -> aabb instanceof BoundingBox);
    }

    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/Profiler;endSection()V", ordinal = 0))
    public void inj3(MoverType type, double x, double y, double z, CallbackInfo ci,
                     @Local(ordinal = 6) LocalDoubleRef d2, @Local(ordinal = 7) LocalDoubleRef d3, @Local(ordinal = 8) LocalDoubleRef d4,
                     @Share("velocity")LocalRef<Vec3d> velocity,
                     @Share("collided")LocalBooleanRef collided) {
        if (d2.get() == x && collided.get()) {
            motionX = (velocity.get().x);
        } else {
            double abs1 = Math.abs(d2.get());
            double abs2 = Math.abs(velocity.get().x);
            motionX = (abs1 < abs2 ? d2.get() : velocity.get().x);
        }
        if (d3.get() == y && collided.get()) {
            motionY = (velocity.get().y);
        } else {
            double abs1 = Math.abs(d3.get());
            double abs2 = Math.abs(velocity.get().y);
            motionY = (abs1 < abs2 ? d3.get() : velocity.get().y);
        }
        if (d4.get() == z && collided.get()) {
            motionZ = velocity.get().z;
        }  else {
            double abs1 = Math.abs(d4.get());
            double abs2 = Math.abs(velocity.get().z);
            motionZ = (abs1 < abs2 ? d4.get() : velocity.get().z);
        }
    }

    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;canTriggerWalking()Z"))
    public void resetVelocity(MoverType type, double x, double y, double z, CallbackInfo ci,
                              @Share("velocity")LocalRef<Vec3d> velocity,
                              @Share("collided")LocalBooleanRef collided) {
        if (collided.get()) {
            onGround = true;
        }
    }

    @Definition(id = "d2", local = @Local(type = double.class, ordinal = 6))
    @Definition(id = "x", local = @Local(argsOnly = true, type = double.class, ordinal = 0))
    @Expression("d2 != x")
    @ModifyExpressionValue(method = "move", at = @At(value = "MIXINEXTRAS:EXPRESSION", ordinal = 2))
    public boolean inj1(boolean original, @Local(ordinal = 6) double d2,
                        @Share("velocity")LocalRef<Vec3d> velocity,
                        @Share("collided")LocalBooleanRef collided) {
        return original && !collided.get() && Math.abs(velocity.get().x - d2) > 1E-4;
    }

    @Definition(id = "d4", local = @Local(type = double.class, ordinal = 8))
    @Definition(id = "z", local = @Local(argsOnly = true, type = double.class, ordinal = 2))
    @Expression("d4 != z")
    @ModifyExpressionValue(method = "move", at = @At(value = "MIXINEXTRAS:EXPRESSION", ordinal = 2))
    public boolean inj2(boolean original, @Local(ordinal = 8) double d4,
                        @Share("velocity")LocalRef<Vec3d> velocity,
                        @Share("collided")LocalBooleanRef collided) {
        return original && !collided.get() && Math.abs(velocity.get().z - d4) > 1E-4;
    }
}
