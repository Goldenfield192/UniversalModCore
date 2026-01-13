package cam72cam.mod.entity.boundingbox;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.Util;
import net.minecraft.core.AxisCycle;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BBVoxelShape extends VoxelShape {
    private BoundingBox bb;

    public BBVoxelShape(BoundingBox boundingBox) {
        super(Shapes.block().shape);
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
        return switch (axis) {
            case X -> DoubleArrayList.wrap(Arrays.copyOf(new double[]{bb.minX, bb.maxX},
                                                         Shapes.block().shape.getSize(Direction.Axis.X) + 1));
            case Y -> DoubleArrayList.wrap(Arrays.copyOf(new double[]{bb.minY, bb.maxY},
                                                         Shapes.block().shape.getSize(Direction.Axis.Y) + 1));
            case Z -> DoubleArrayList.wrap(Arrays.copyOf(new double[]{bb.minZ, bb.maxZ},
                                                         Shapes.block().shape.getSize(Direction.Axis.Z) + 1));
        };
    }
}
