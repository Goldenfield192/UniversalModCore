package cam72cam.mod.entity.boundingbox;

import cam72cam.mod.math.Matrix3;
import cam72cam.mod.math.Vec3d;

/**
 * Extension interface for detection and OBB-specific access.
 * Any bounding box implementing this is an oriented box;
 * everything else is treated as an AABB.
 */
public interface IOrientedBB {
    /**
     * Reference point of the box in world space. This is NOT necessarily the geometric
     * center: the box extends asymmetrically around it (like an entity whose reference
     * position is at its feet).
     */
    Vec3d center();

    /** Distance from {@link #center()} to the negative face along each local axis. */
    Vec3d extentNeg();

    /** Distance from {@link #center()} to the positive face along each local axis. */
    Vec3d extentPos();

    /** Rotation matrix (columns = right, up, forward) */
    Matrix3 rotation();
}
