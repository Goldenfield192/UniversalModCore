package cam72cam.mod.entity.boundingbox;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.Util;
import net.minecraft.core.AxisCycle;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.CubeVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BBVoxelShape extends VoxelShape {
    //Though I don't understand what doe this mean...
    private static final VoxelShape FULL_CUBE1 = Util.make(() -> {
        DiscreteVoxelShape lvt_0_1_ = new BitSetDiscreteVoxelShape(200, 200, 200);
        for (int i = -100; i <= 99; i++) {
            for (int i1 = -100; i1 <= 99; i1++) {
                for (int i2 = -100; i2 <= 99; i2++) {
                    lvt_0_1_.fill(1, i1, i2);
                }
            }
        }
        return new CubeVoxelShape(lvt_0_1_);
    });

    private BoundingBox bb;

    public BBVoxelShape(BoundingBox boundingBox) {
        super(FULL_CUBE1.shape);
        this.bb = boundingBox;
    }

    public BoundingBox getBb() {
        return bb;
    }

    @Override
    protected double collideX(AxisCycle movementAxis, AABB collisionBox, double desiredOffset) {
        if (this.isEmpty()) {
            return desiredOffset;
        } else if (Math.abs(desiredOffset) < 1.0E-7D) {
            return 0.0D;
        } else {
            boolean colliding = bb.intersects(collisionBox.minX, collisionBox.minY, collisionBox.minZ, collisionBox.maxX, collisionBox.maxY, collisionBox.maxZ);
            boolean willZCollide = !colliding
                    && bb.intersects(collisionBox.minX, collisionBox.minY, collisionBox.minZ + desiredOffset, collisionBox.maxX, collisionBox.maxY, collisionBox.maxZ + desiredOffset);
            boolean willXCollide = !colliding
                    && bb.intersects(collisionBox.minX + desiredOffset, collisionBox.minY, collisionBox.minZ, collisionBox.maxX + desiredOffset, collisionBox.maxY, collisionBox.maxZ);
            switch (movementAxis) {
                case FORWARD: //Z
                case NONE: //X
                    if (willXCollide || willZCollide) {
                        return 0;
                    } else {
                        return desiredOffset;
                    }
                case BACKWARD: //Y
                default:
                    //Add a small offset so jump won't get blocked
                    return bb.internal.calculateYOffset(IBoundingBox.from(collisionBox), desiredOffset) + 0.01;
            }
        }
    }

    @Override
    protected DoubleList getCoords(Direction.Axis axis) {
        switch(axis) {
            case X:
                return DoubleArrayList.wrap(Arrays.copyOf(new double[]{bb.minX, bb.maxX}, 200));
            case Y:
                return DoubleArrayList.wrap(Arrays.copyOf(new double[]{bb.minY, bb.maxY}, 200));
            case Z:
                return DoubleArrayList.wrap(Arrays.copyOf(new double[]{bb.minZ, bb.maxZ}, 200));
            default:
                throw new IllegalArgumentException();
        }
    }
}
