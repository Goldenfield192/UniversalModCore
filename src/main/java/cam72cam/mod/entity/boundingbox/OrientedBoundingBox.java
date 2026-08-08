package cam72cam.mod.entity.boundingbox;

import cam72cam.mod.math.Matrix3;
import cam72cam.mod.math.Vec3d;

public class OrientedBoundingBox implements IBoundingBox, IOrientedBB {
    private static final double EPSILON = 1.0E-6;

    private final Vec3d center;
    /** Distance from {@link #center} to the negative face along each local axis. */
    private final Vec3d extentNeg;
    /** Distance from {@link #center} to the positive face along each local axis. */
    private final Vec3d extentPos;
    private final Matrix3 rotation;

    // Lazy caches. Safe because the box is treated as immutable.
    private Box box;
    private Vec3d cachedWorldExtent;
    private Vec3d cachedMin;
    private Vec3d cachedMax;

    public OrientedBoundingBox(Vec3d center, Vec3d extentNeg, Vec3d extentPos, Matrix3 rotation) {
        this.center = center;
        this.extentNeg = extentNeg;
        this.extentPos = extentPos;
        this.rotation = rotation.copy();
    }

    /** Convenience constructor for a symmetric box centered on {@code center}. */
    public OrientedBoundingBox(Vec3d center, Vec3d extent, Matrix3 rotation) {
        this(center, extent, extent, rotation);
    }

    public OrientedBoundingBox(IOrientedBB other) {
        this(other.center(), other.extentNeg(), other.extentPos(), other.rotation());
    }

    /** Build an OBB snapshot of any box; AABBs get an identity rotation. */
    public static OrientedBoundingBox from(IBoundingBox bb) {
        if (bb instanceof IOrientedBB) {
            return new OrientedBoundingBox((IOrientedBB) bb);
        }
        Vec3d min = bb.min();
        Vec3d max = bb.max();
        Vec3d center = new Vec3d((min.x + max.x) * 0.5, (min.y + max.y) * 0.5, (min.z + max.z) * 0.5);
        Vec3d extent = new Vec3d((max.x - min.x) * 0.5, (max.y - min.y) * 0.5, (max.z - min.z) * 0.5);
        return new OrientedBoundingBox(center, extent, extent, new Matrix3());
    }

    public static OrientedBoundingBox from(Vec3d extent, Vec3d center) {
        return new OrientedBoundingBox(center, extent, extent, new Matrix3());
    }

    // ------------------------------------------------------------------
    // Basic accessors
    // ------------------------------------------------------------------

    @Override
    public Vec3d center() {
        return center;
    }

    @Override
    public Vec3d extentNeg() {
        return extentNeg;
    }

    @Override
    public Vec3d extentPos() {
        return extentPos;
    }

    @Override
    public Matrix3 rotation() {
        return rotation;
    }

    /**
     * World-space geometric center of the box (the point the box is symmetric about):
     * {@code center + rotation * ((extentPos - extentNeg) / 2)}.
     */
    private Vec3d geometricCenter() {
        Vec3d offset = extentPos.subtract(extentNeg).scale(0.5);
        return center.add(rotation.apply(offset));
    }

    /** Symmetric half-extent about the geometric center: {@code (extentNeg + extentPos) / 2}. */
    private Vec3d halfExtent() {
        return new Vec3d(
                (extentNeg.x + extentPos.x) * 0.5,
                (extentNeg.y + extentPos.y) * 0.5,
                (extentNeg.z + extentPos.z) * 0.5);
    }

    @Override
    public Vec3d min() {
        worldExtent();
        return cachedMin;
    }

    @Override
    public Vec3d max() {
        worldExtent();
        return cachedMax;
    }

    public OrientedBoundingBox copy() {
        return new OrientedBoundingBox(center, extentNeg, extentPos, rotation);
    }

    // ------------------------------------------------------------------
    // Shape modifiers
    // ------------------------------------------------------------------

    @Override
    public IBoundingBox expand(Vec3d val) {
        return moveFaces(val, true);
    }

    @Override
    public IBoundingBox contract(Vec3d val) {
        return moveFaces(val, false);
    }

    /**
     * expand() grows the enclosing AABB outwards by {@code val} (sign-dependent per face),
     * contract() is its inverse. Both operate on the world-aligned enclosing box and
     * therefore return a plain AABB.
     */
    private IBoundingBox moveFaces(Vec3d val, boolean expand) {
        Vec3d min = min();
        Vec3d max = max();
        double minX = min.x;
        double minY = min.y;
        double minZ = min.z;
        double maxX = max.x;
        double maxY = max.y;
        double maxZ = max.z;

        if (expand) {
            if (val.x < 0) { minX += val.x; } else { maxX += val.x; }
            if (val.y < 0) { minY += val.y; } else { maxY += val.y; }
            if (val.z < 0) { minZ += val.z; } else { maxZ += val.z; }
        } else {
            if (val.x < 0) { maxX += val.x; } else { minX += val.x; }
            if (val.y < 0) { maxY += val.y; } else { minY += val.y; }
            if (val.z < 0) { maxZ += val.z; } else { minZ += val.z; }
        }

        return IBoundingBox.from(new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ));
    }

    @Override
    public IBoundingBox grow(Vec3d val) {
        return new OrientedBoundingBox(center,
                new Vec3d(Math.max(0, extentNeg.x + val.x), Math.max(0, extentNeg.y + val.y), Math.max(0, extentNeg.z + val.z)),
                new Vec3d(Math.max(0, extentPos.x + val.x), Math.max(0, extentPos.y + val.y), Math.max(0, extentPos.z + val.z)),
                rotation);
    }

    @Override
    public IBoundingBox offset(Vec3d vec3d) {
        return new OrientedBoundingBox(center.add(vec3d), extentNeg, extentPos, rotation);
    }

    // ------------------------------------------------------------------
    // Movement adjustment (3D vectorized collision response)
    // ------------------------------------------------------------------

    @Override
    public Vec3d adjustMovement(IBoundingBox other, Vec3d velocity) {
        //Other with velocity tries to move and this is obstacle, return corrected movement
        return resolveCollision(of(other), box(), velocity);
    }

    @Override
    public double calculateXOffset(IBoundingBox other, double offsetX) {
        return 0;
    }

    @Override
    public double calculateYOffset(IBoundingBox other, double offsetY) {
        return 0;
    }

    @Override
    public double calculateZOffset(IBoundingBox other, double offsetZ) {
        return 0;
    }

    /**
     * Resolve the movement of {@code moving} (displaced by {@code velocity}) against the
     * static obstacle {@code obstacle}. Returns the adjusted displacement vector.
     *
     * <p>The velocity is first decomposed onto the obstacle's local axes, then each axis is
     * resolved sequentially with a binary search (like vanilla {@code calculateX/Y/ZOffset}).
     * Working in the obstacle frame turns the obstacle into an axis-aligned box, so the
     * resolved movement slides along the obstacle's faces. The result on each local axis is
     * clamped to between {@code 0} and the input component, so it never reverses direction
     * or exceeds the requested movement.</p>
     */
    private static Vec3d resolveCollision(Box moving, Box obstacle, Vec3d velocity) {
        // Fixme bad movement
        // Fast path: the destination is fully clear.
        if (!intersects(moving, obstacle, velocity.x, velocity.y, velocity.z)) {
            return velocity;
        }
        // Obstacle's local axes (columns of its rotation matrix).
        Vec3d oRight = new Vec3d(obstacle.rx, obstacle.ry, obstacle.rz);
        Vec3d oUp = new Vec3d(obstacle.ux, obstacle.uy, obstacle.uz);
        Vec3d oForward = new Vec3d(obstacle.fx, obstacle.fy, obstacle.fz);

        // Decompose velocity onto the obstacle's local axes.
        double vLx = oRight.dotProduct(velocity);
        double vLy = oUp.dotProduct(velocity);
        double vLz = oForward.dotProduct(velocity);

        // Resolve each local axis, accumulating world-space displacement.
        double x = binarySearch(moving, obstacle, vLx, oRight, Vec3d.ZERO);
        Vec3d afterX = oRight.scale(x);
        double y = binarySearch(moving, obstacle, vLy, oUp, afterX);
        Vec3d afterY = afterX.add(oUp.scale(y));
        double z = binarySearch(moving, obstacle, vLz, oForward, afterY);

        return afterY.add(oForward.scale(z));
    }

    /**
     * Binary search along one obstacle-local axis for the largest safe displacement of
     * {@code moving} against {@code obstacle}, given the world-space displacement already
     * applied on the previous axes ({@code baseWorld}). The result is clamped to between
     * {@code 0} and {@code displacement}, so it never exceeds the requested input.
     */
    private static double binarySearch(Box moving, Box obstacle,
                                       double displacement,
                                       Vec3d axis, Vec3d baseWorld) {
        if (displacement == 0) {
            return 0;
        }
        // Full movement on this axis is unobstructed.
        Vec3d full = baseWorld.add(axis.scale(displacement));
        if (!intersects(moving, obstacle, full.x, full.y, full.z)) {
            return displacement;
        }
        // Binary search for the maximum safe displacement.
        double lo = 0;
        double hi = displacement;
        for (int i = 0; i < 16; i++) {
            double mid = (lo + hi) * 0.5;
            Vec3d test = baseWorld.add(axis.scale(mid));
            if (intersects(moving, obstacle, test.x, test.y, test.z)) {
                hi = mid;
            } else {
                lo = mid;
            }
        }
        return lo;
    }

    // ------------------------------------------------------------------
    // Intersection tests
    // ------------------------------------------------------------------

    @Override
    public boolean intersects(Vec3d min, Vec3d max) {
        return intersectsAABB(box(), min.x, min.y, min.z, max.x, max.y, max.z);
    }

    @Override
    public boolean intersects(IBoundingBox bounds) {
        if (bounds instanceof IOrientedBB) {
            return intersects(box(), of(bounds), 0, 0, 0);
        }
        Vec3d min = bounds.min();
        Vec3d max = bounds.max();
        return intersectsAABB(box(), min.x, min.y, min.z, max.x, max.y, max.z);
    }

    @Override
    public IBoundingBox expandToFit(IBoundingBox other) {
        Vec3d myMin = min();
        Vec3d myMax = max();
        Vec3d otherMin = other.min();
        Vec3d otherMax = other.max();

        // TODO OBB: currently expands the enclosing AABB, returns an AABB.
        return IBoundingBox.from(
                new Vec3d(Math.min(myMin.x, otherMin.x), Math.min(myMin.y, otherMin.y), Math.min(myMin.z, otherMin.z)),
                new Vec3d(Math.max(myMax.x, otherMax.x), Math.max(myMax.y, otherMax.y), Math.max(myMax.z, otherMax.z)));
    }

    /**
     * Ray vs OBB intersection using the slab method in OBB local space:
     * the segment is transformed into the box frame and tested against the
     * asymmetric slab bounds [-extentNeg, +extentPos] on each local axis.
     */
    @Override
    public boolean intersectsSegment(Vec3d start, Vec3d end) {
        Vec3d ls = toLocal(start);
        Vec3d le = toLocal(end);
        double dx = le.x - ls.x;
        double dy = le.y - ls.y;
        double dz = le.z - ls.z;

        double tMin = 0.0;
        double tMax = 1.0;

        // X slab: box spans [-extentNeg.x, +extentPos.x] in local space
        if (Math.abs(dx) < EPSILON) {
            if (ls.x < -extentNeg.x || ls.x > extentPos.x) return false;
        } else {
            double ood = 1.0 / dx;
            double t1 = (-extentNeg.x - ls.x) * ood;
            double t2 = (extentPos.x - ls.x) * ood;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return false;
        }

        // Y slab
        if (Math.abs(dy) < EPSILON) {
            if (ls.y < -extentNeg.y || ls.y > extentPos.y) return false;
        } else {
            double ood = 1.0 / dy;
            double t1 = (-extentNeg.y - ls.y) * ood;
            double t2 = (extentPos.y - ls.y) * ood;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return false;
        }

        // Z slab
        if (Math.abs(dz) < EPSILON) {
            return !(ls.z < -extentNeg.z) && !(ls.z > extentPos.z);
        } else {
            double ood = 1.0 / dz;
            double t1 = (-extentNeg.z - ls.z) * ood;
            double t2 = (extentPos.z - ls.z) * ood;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            return !(tMin > tMax);
        }
    }

    /** Transform a world-space point into the OBB local frame (rotation is orthonormal). */
    private Vec3d toLocal(Vec3d world) {
        double dx = world.x - center.x;
        double dy = world.y - center.y;
        double dz = world.z - center.z;
        Vec3d r = rotation.right();
        Vec3d u = rotation.up();
        Vec3d f = rotation.forward();
        return new Vec3d(
                r.x * dx + r.y * dy + r.z * dz,
                u.x * dx + u.y * dy + u.z * dz,
                f.x * dx + f.y * dy + f.z * dz);
    }

    @Override
    public boolean contains(Vec3d vec) {
        Vec3d local = toLocal(vec);
        return local.x >= -extentNeg.x && local.x <= extentPos.x
                && local.y >= -extentNeg.y && local.y <= extentPos.y
                && local.z >= -extentNeg.z && local.z <= extentPos.z;
    }

    // ------------------------------------------------------------------
    // Cached world-space enclosing AABB
    // ------------------------------------------------------------------

    /**
     * World-space half-extent of the enclosing AABB: for each world axis the projected
     * half-extent is the sum of the absolute dot products of the local axes with it.
     */
    private Vec3d worldExtent() {
        if (cachedWorldExtent != null) {
            return cachedWorldExtent;
        }
        Box b = box();
        Vec3d result = new Vec3d(
                Math.abs(b.rx) * b.ex + Math.abs(b.ux) * b.ey + Math.abs(b.fx) * b.ez,
                Math.abs(b.ry) * b.ex + Math.abs(b.uy) * b.ey + Math.abs(b.fy) * b.ez,
                Math.abs(b.rz) * b.ex + Math.abs(b.uz) * b.ey + Math.abs(b.fz) * b.ez);
        cachedWorldExtent = result;
        // Enclosing AABB is centered on the geometric center (b.cx..), not the ref center.
        cachedMin = new Vec3d(b.cx - result.x, b.cy - result.y, b.cz - result.z);
        cachedMax = new Vec3d(b.cx + result.x, b.cy + result.y, b.cz + result.z);
        return result;
    }

    private Box box() {
        if (box == null) {
            box = new Box(geometricCenter(), halfExtent(), rotation);
        }
        return box;
    }

    // ------------------------------------------------------------------
    // SAT helpers
    // ------------------------------------------------------------------

    /**
     * Test whether the two oriented boxes intersect, with {@code a} displaced by
     * ({@code dx}, {@code dy}, {@code dz}) relative to its stored center.
     */
    private static boolean intersects(Box a, Box b, double dx, double dy, double dz) {
        double tx = a.cx + dx - b.cx;
        double ty = a.cy + dy - b.cy;
        double tz = a.cz + dz - b.cz;

        // Face axes of a.
        if (separated(a, b, tx, ty, tz, a.rx, a.ry, a.rz)) return false;
        if (separated(a, b, tx, ty, tz, a.ux, a.uy, a.uz)) return false;
        if (separated(a, b, tx, ty, tz, a.fx, a.fy, a.fz)) return false;
        // Face axes of b.
        if (separated(a, b, tx, ty, tz, b.rx, b.ry, b.rz)) return false;
        if (separated(a, b, tx, ty, tz, b.ux, b.uy, b.uz)) return false;
        if (separated(a, b, tx, ty, tz, b.fx, b.fy, b.fz)) return false;
        // Edge cross-product axes (a_i x b_j).
        return !separatedCross(a, b, tx, ty, tz, a.rx, a.ry, a.rz, b.rx, b.ry, b.rz)
                && !separatedCross(a, b, tx, ty, tz, a.rx, a.ry, a.rz, b.ux, b.uy, b.uz)
                && !separatedCross(a, b, tx, ty, tz, a.rx, a.ry, a.rz, b.fx, b.fy, b.fz)
                && !separatedCross(a, b, tx, ty, tz, a.ux, a.uy, a.uz, b.rx, b.ry, b.rz)
                && !separatedCross(a, b, tx, ty, tz, a.ux, a.uy, a.uz, b.ux, b.uy, b.uz)
                && !separatedCross(a, b, tx, ty, tz, a.ux, a.uy, a.uz, b.fx, b.fy, b.fz)
                && !separatedCross(a, b, tx, ty, tz, a.fx, a.fy, a.fz, b.rx, b.ry, b.rz)
                && !separatedCross(a, b, tx, ty, tz, a.fx, a.fy, a.fz, b.ux, b.uy, b.uz)
                && !separatedCross(a, b, tx, ty, tz, a.fx, a.fy, a.fz, b.fx, b.fy, b.fz);
    }

    /**
     * Single-axis OBB vs OBB separation test.
     * {@code t} is the vector from b's center to a's (displaced) center.
     *
     * <p>The axis is normalized first so the {@link #EPSILON} allowance is uniform:
     * without it, the tolerance of cross-product axes would scale with their length.</p>
     */
    private static boolean separated(Box a, Box b, double tx, double ty, double tz,
                                     double lx, double ly, double lz) {
        double lenSq = lx * lx + ly * ly + lz * lz;
        if (Math.abs(lenSq - 1.0) > 1e-4) {
            if (lenSq < EPSILON) {
                return false; // Degenerate axis cannot separate.
            }
            double inv = 1.0 / Math.sqrt(lenSq);
            lx *= inv;
            ly *= inv;
            lz *= inv;
        }

        double ra = Math.abs(lx * a.rx + ly * a.ry + lz * a.rz) * a.ex
                + Math.abs(lx * a.ux + ly * a.uy + lz * a.uz) * a.ey
                + Math.abs(lx * a.fx + ly * a.fy + lz * a.fz) * a.ez;
        double rb = Math.abs(lx * b.rx + ly * b.ry + lz * b.rz) * b.ex
                + Math.abs(lx * b.ux + ly * b.uy + lz * b.uz) * b.ey
                + Math.abs(lx * b.fx + ly * b.fy + lz * b.fz) * b.ez;
        double d = Math.abs(tx * lx + ty * ly + tz * lz);
        return d > ra + rb + EPSILON;
    }

    /** Compute a cross-product axis and test separation; parallel axes are ignored. */
    private static boolean separatedCross(Box a, Box b, double tx, double ty, double tz,
                                          double ax, double ay, double az,
                                          double bx, double by, double bz) {
        double lx = ay * bz - az * by;
        double ly = az * bx - ax * bz;
        double lz = ax * by - ay * bx;
        if (lx * lx + ly * ly + lz * lz < EPSILON) {
            return false; // Parallel edges: not a valid separating axis.
        }
        return separated(a, b, tx, ty, tz, lx, ly, lz);
    }

    /**
     * OBB vs AABB SAT. The AABB is described by its two corners; the 15 candidate axes are
     * the 6 face normals (3 OBB + 3 world) and the 9 cross products of OBB axes with world axes.
     */
    private static boolean intersectsAABB(Box a, double minX, double minY, double minZ,
                                          double maxX, double maxY, double maxZ) {
        // OBB face normals.
        if (separatedAABB(a, minX, minY, minZ, maxX, maxY, maxZ, a.rx, a.ry, a.rz)) return false;
        if (separatedAABB(a, minX, minY, minZ, maxX, maxY, maxZ, a.ux, a.uy, a.uz)) return false;
        if (separatedAABB(a, minX, minY, minZ, maxX, maxY, maxZ, a.fx, a.fy, a.fz)) return false;
        // World (AABB) face normals.
        if (separatedAABB(a, minX, minY, minZ, maxX, maxY, maxZ, 1, 0, 0)) return false;
        if (separatedAABB(a, minX, minY, minZ, maxX, maxY, maxZ, 0, 1, 0)) return false;
        if (separatedAABB(a, minX, minY, minZ, maxX, maxY, maxZ, 0, 0, 1)) return false;
        // Cross products: OBB axis x world axis.
        if (separatedAABB(a, minX, minY, minZ, maxX, maxY, maxZ, 0, a.rz, -a.ry)) return false;
        if (separatedAABB(a, minX, minY, minZ, maxX, maxY, maxZ, -a.rz, 0, a.rx)) return false;
        if (separatedAABB(a, minX, minY, minZ, maxX, maxY, maxZ, a.ry, -a.rx, 0)) return false;
        if (separatedAABB(a, minX, minY, minZ, maxX, maxY, maxZ, 0, a.uz, -a.uy)) return false;
        if (separatedAABB(a, minX, minY, minZ, maxX, maxY, maxZ, -a.uz, 0, a.ux)) return false;
        if (separatedAABB(a, minX, minY, minZ, maxX, maxY, maxZ, a.uy, -a.ux, 0)) return false;
        if (separatedAABB(a, minX, minY, minZ, maxX, maxY, maxZ, 0, a.fz, -a.fy)) return false;
        if (separatedAABB(a, minX, minY, minZ, maxX, maxY, maxZ, -a.fz, 0, a.fx)) return false;
        if (separatedAABB(a, minX, minY, minZ, maxX, maxY, maxZ, a.fy, -a.fx, 0)) return false;
        return true;
    }

    /**
     * Single-axis OBB vs AABB separation test. The OBB projects to a radius around its center
     * projection; the AABB projects to the interval spanned by its two corners.
     *
     * <p>The axis is normalized first so the {@link #EPSILON} allowance is uniform across
     * the 6 face normals and the 9 cross-product axes.</p>
     */
    private static boolean separatedAABB(Box a, double minX, double minY, double minZ,
                                         double maxX, double maxY, double maxZ,
                                         double lx, double ly, double lz) {
        double lenSq = lx * lx + ly * ly + lz * lz;
        if (Math.abs(lenSq - 1.0) > 1e-4) {
            if (lenSq < EPSILON) {
                return false; // Degenerate axis cannot separate.
            }
            double inv = 1.0 / Math.sqrt(lenSq);
            lx *= inv;
            ly *= inv;
            lz *= inv;
        }

        double ra = Math.abs(lx * a.rx + ly * a.ry + lz * a.rz) * a.ex
                + Math.abs(lx * a.ux + ly * a.uy + lz * a.uz) * a.ey
                + Math.abs(lx * a.fx + ly * a.fy + lz * a.fz) * a.ez;
        double oc = lx * a.cx + ly * a.cy + lz * a.cz;
        double p1 = lx * minX + ly * minY + lz * minZ;
        double p2 = lx * maxX + ly * maxY + lz * maxZ;
        double aMin = Math.min(p1, p2);
        double aMax = Math.max(p1, p2);
        return aMin > oc + ra + EPSILON || oc - ra > aMax + EPSILON;
    }

    // ------------------------------------------------------------------
    // Geometry snapshot
    // ------------------------------------------------------------------

    /**
     * Allocation-friendly flattened snapshot of an oriented box's geometry.
     * Basis vectors are the columns of the rotation matrix (right/up/forward).
     */
    private static final class Box {
        final double cx, cy, cz;   // center
        final double ex, ey, ez;   // half-extents
        final double rx, ry, rz;   // right
        final double ux, uy, uz;   // up
        final double fx, fy, fz;   // forward

        Box(Vec3d center, Vec3d extent, Matrix3 rot) {
            this.cx = center.x;
            this.cy = center.y;
            this.cz = center.z;
            this.ex = extent.x;
            this.ey = extent.y;
            this.ez = extent.z;
            Vec3d r = rot.right();
            this.rx = r.x;
            this.ry = r.y;
            this.rz = r.z;
            Vec3d u = rot.up();
            this.ux = u.x;
            this.uy = u.y;
            this.uz = u.z;
            Vec3d f = rot.forward();
            this.fx = f.x;
            this.fy = f.y;
            this.fz = f.z;
        }

        /** Build a box from an AABB (identity orientation). */
        Box(double cx, double cy, double cz, double ex, double ey, double ez) {
            this.cx = cx;
            this.cy = cy;
            this.cz = cz;
            this.ex = ex;
            this.ey = ey;
            this.ez = ez;
            this.rx = 1;
            this.ry = 0;
            this.rz = 0;
            this.ux = 0;
            this.uy = 1;
            this.uz = 0;
            this.fx = 0;
            this.fy = 0;
            this.fz = 1;
        }
    }

    /** Convert any box to the flattened representation (geometric center + half-extent). */
    private static Box of(IBoundingBox bb) {
        if (bb instanceof IOrientedBB) {
            IOrientedBB o = (IOrientedBB) bb;
            Vec3d en = o.extentNeg();
            Vec3d ep = o.extentPos();
            Vec3d half = new Vec3d((en.x + ep.x) * 0.5, (en.y + ep.y) * 0.5, (en.z + ep.z) * 0.5);
            Vec3d offset = new Vec3d((ep.x - en.x) * 0.5, (ep.y - en.y) * 0.5, (ep.z - en.z) * 0.5);
            Vec3d geometric = o.center().add(o.rotation().apply(offset));
            return new Box(geometric, half, o.rotation());
        }
        Vec3d min = bb.min();
        Vec3d max = bb.max();
        return new Box((min.x + max.x) * 0.5, (min.y + max.y) * 0.5, (min.z + max.z) * 0.5,
                (max.x - min.x) * 0.5, (max.y - min.y) * 0.5, (max.z - min.z) * 0.5);
    }
}
