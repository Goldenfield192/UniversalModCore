package cam72cam.mod.mixin.feat.vectorlized_collision;

import cam72cam.mod.entity.boundingbox.BoundingBox;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.math.Vec3d;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalDoubleRef;
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

    @Inject(method = "move", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/World;getCollisionBoxes(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/AxisAlignedBB;)Ljava/util/List;", ordinal = 3))
    public void inj(MoverType type, double x, double y, double z, CallbackInfo ci,
                    @Local List<AxisAlignedBB> list,
                    @Local(argsOnly = true, ordinal = 0) LocalDoubleRef x1,
                    @Local(argsOnly = true, ordinal = 1) LocalDoubleRef y1,
                    @Local(argsOnly = true, ordinal = 2) LocalDoubleRef z1,
                    @Share("xR") LocalDoubleRef xR, @Share("zR") LocalDoubleRef zR) {
        IBoundingBox box = IBoundingBox.from(this.getEntityBoundingBox());
        Vec3d iter = new Vec3d(x, y, z);
        boolean flag = false;
        for (AxisAlignedBB axisAlignedBB : new ArrayList<>(list)) {
            if (axisAlignedBB instanceof BoundingBox) {
                BoundingBox boundingBox = (BoundingBox) axisAlignedBB;
                iter = boundingBox.internal.adjustMovement(box, iter);
                list.remove(axisAlignedBB);
                flag = true;
            }
        }
        x1.set(iter.x);
        //TODO stuck in ground
        y1.set(flag ? 0 : iter.y);
        z1.set(iter.z);
        xR.set(iter.x);
        zR.set(iter.z);
    }

    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/Profiler;endSection()V", ordinal = 0))
    public void inj3(MoverType type, double x, double y, double z, CallbackInfo ci,
                     @Local(ordinal = 6) LocalDoubleRef d2, @Local(ordinal = 8) LocalDoubleRef d4,
                     @Share("xR") LocalDoubleRef xR, @Share("zR") LocalDoubleRef zR) {
        d2.set(xR.get());
        d4.set(zR.get());
    }

    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getEntityBoundingBox()Lnet/minecraft/util/math/AxisAlignedBB;", ordinal = 13))
    public void inj4(MoverType type, double x, double y, double z, CallbackInfo ci, @Local List<AxisAlignedBB> list) {
        for (AxisAlignedBB axisAlignedBB : new ArrayList<>(list)) {
            if (axisAlignedBB instanceof BoundingBox) {
                list.remove(axisAlignedBB);
            }
        }
    }

    @Definition(id = "d2", local = @Local(type = double.class, ordinal = 6))
    @Definition(id = "x", local = @Local(argsOnly = true, type = double.class, ordinal = 0))
    @Expression("d2 != x")
    @ModifyExpressionValue(method = "move", at = @At(value = "MIXINEXTRAS:EXPRESSION", ordinal = 2))
    public boolean inj1(boolean original, @Local(ordinal = 6) double d2, @Share("xR") LocalDoubleRef xR) {
        return original && Math.abs(xR.get() - d2) > 1E-4;
    }

    @Definition(id = "d4", local = @Local(type = double.class, ordinal = 8))
    @Definition(id = "z", local = @Local(argsOnly = true, type = double.class, ordinal = 2))
    @Expression("d4 != z")
    @ModifyExpressionValue(method = "move", at = @At(value = "MIXINEXTRAS:EXPRESSION", ordinal = 2))
    public boolean inj2(boolean original, @Local(ordinal = 8) double d4, @Share("zR") LocalDoubleRef zR) {
        return original && Math.abs(zR.get() - d4) > 1E-4;
    }
}
