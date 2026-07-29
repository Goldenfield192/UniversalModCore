package cam72cam.mod.entity.boundingbox;

import cam72cam.mod.math.Matrix3;
import cam72cam.mod.math.Vec3d;

/**
 * Extension interface for detection and OBB-specific access.
 * Any bounding box implementing this is an oriented box;
 * everything else is treated as an AABB.
 */
public interface IOrientedBB {
    Vec3d center();

    /** Half-extent in local space (x, y, z) */
    Vec3d extent();

    /** Rotation matrix (columns = right, up, forward) */
    Matrix3 rotation();
}
