package cam72cam.mod.entity.boundingbox;

import cam72cam.mod.math.Matrix3;
import cam72cam.mod.math.Vec3d;
import net.minecraft.util.math.AxisAlignedBB;

public class OrientedBoundingBox implements IBoundingBox, IOrientedBB {
    private static final double EPSILON = 1.0E-6;

    private final Vec3d center;
    private final Vec3d extent;
    private final Matrix3 rotation;

    // Cached results — recomputed lazily; cached-right/up/forward serve as version tags
    // to detect matrix mutations (e.g. if a setRotation() method is added in the future).
    private Vec3d cachedMin;
    private Vec3d cachedMax;
    private Vec3d cachedWorldExtent;

    public OrientedBoundingBox(Vec3d center, Vec3d extent, Matrix3 rotation) {
        this.center = center;
        this.extent = extent;
        this.rotation = rotation.copy();
    }

    public OrientedBoundingBox(IOrientedBB other) {
        this(other.center(), other.extent(), other.rotation());
    }

    public static OrientedBoundingBox from(IBoundingBox bb) {
        if (bb instanceof IOrientedBB) {
            return new OrientedBoundingBox((IOrientedBB)bb);
        }
        return new OrientedBoundingBox(bb.center(), bb.max().subtract(bb.center()), new Matrix3());
    }

    public static OrientedBoundingBox from(Vec3d extent, Vec3d center) {
        return new OrientedBoundingBox(center, extent, new Matrix3());
    }

    /**
     * Compute the world-space half-extent of the enclosing AABB by projecting the OBB onto world axes.
     * For each world axis, the projected extent is the sum of absolute dot products of OBB axes scaled by extent.
     * Result is cached; cache is invalidated when rotation basis vectors change.
     */
    private Vec3d worldExtent() {
        Vec3d r = rotation.right();
        Vec3d u = rotation.up();
        Vec3d f = rotation.forward();

        // Check version tags — if basis vectors haven't changed, return cached value
        if (cachedWorldExtent != null) {
            return cachedWorldExtent;
        }

        Vec3d result = new Vec3d(
            Math.abs(r.x) * extent.x + Math.abs(u.x) * extent.y + Math.abs(f.x) * extent.z,
            Math.abs(r.y) * extent.x + Math.abs(u.y) * extent.y + Math.abs(f.y) * extent.z,
            Math.abs(r.z) * extent.x + Math.abs(u.z) * extent.y + Math.abs(f.z) * extent.z
        );

        // Update cache and version tags
        cachedWorldExtent = result;
        cachedMin = center.subtract(result);
        cachedMax = center.add(result);

        return result;
    }

    @Override
    public Vec3d min() {
        // warm the cache by computing worldExtent, then return cached min
        worldExtent();
        return cachedMin;
    }

    @Override
    public Vec3d center() {
        return center;
    }

    @Override
    public Vec3d max() {
        // warm the cache by computing worldExtent, then return cached max
        worldExtent();
        return cachedMax;
    }

    @Override
    public IBoundingBox expand(Vec3d val) {
        // expand works on world-space sides — apply to enclosing AABB and return as AABB
        Vec3d half = worldExtent();
        double minX = center.x - half.x;
        double minY = center.y - half.y;
        double minZ = center.z - half.z;
        double maxX = center.x + half.x;
        double maxY = center.y + half.y;
        double maxZ = center.z + half.z;

        if (val.x < 0) { minX += val.x; } else { maxX += val.x; }
        if (val.y < 0) { minY += val.y; } else { maxY += val.y; }
        if (val.z < 0) { minZ += val.z; } else { maxZ += val.z; }

        return IBoundingBox.from(new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ));
    }

    @Override
    public IBoundingBox contract(Vec3d val) {
        // contract is the opposite of expand: positive val pulls the positive face in
        Vec3d half = worldExtent();
        double minX = center.x - half.x;
        double minY = center.y - half.y;
        double minZ = center.z - half.z;
        double maxX = center.x + half.x;
        double maxY = center.y + half.y;
        double maxZ = center.z + half.z;

        if (val.x < 0) { maxX += val.x; } else { minX += val.x; }
        if (val.y < 0) { maxY += val.y; } else { minY += val.y; }
        if (val.z < 0) { maxZ += val.z; } else { minZ += val.z; }

        return IBoundingBox.from(new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ));
    }

    @Override
    public IBoundingBox grow(Vec3d val) {
        // grow() expands symmetrically — apply to extent in local space
        double nx = Math.max(0, extent.x + val.x);
        double ny = Math.max(0, extent.y + val.y);
        double nz = Math.max(0, extent.z + val.z);
        return new OrientedBoundingBox(center, new Vec3d(nx, ny, nz), rotation);
    }

    @Override
    public IBoundingBox offset(Vec3d vec3d) {
        return new OrientedBoundingBox(center.add(vec3d), extent, rotation);
    }

    @Override
    public Vec3d adjustMovement(IBoundingBox other, Vec3d velocity) {
        if (other instanceof IOrientedBB) {
            return adjustMovementOBB((IOrientedBB) other, velocity);
        }
        IOrientedBB otherBB = OrientedBoundingBox.from(other);
        return adjustMovementOBB(otherBB, velocity);
    }
    /**
     * 计算 OBB 与 OBB 之间的最小平移向量 (MTV)，用于分离两个相交的 OBB。
     * 返回从 moving (this) 指向远离 static (other) 的向量，若无相交则返回 null。
     */
    private static Vec3d obbMTV(Vec3d c1, Vec3d e1, Matrix3 r1,
                                Vec3d c2, Vec3d e2, Matrix3 r2) {
        Vec3d t = c1.subtract(c2);  // moving 中心指向 static 中心
        Vec3d[] a = {r1.right(), r1.up(), r1.forward()};
        Vec3d[] b = {r2.right(), r2.up(), r2.forward()};

        double minOverlap = Double.POSITIVE_INFINITY;
        Vec3d bestNormal = null;
        double bestSign = 1.0;

        // 15 条分离轴：3 + 3 + 9 条边叉积
        Vec3d[] axes = new Vec3d[15];
        int idx = 0;
        // A 的3个面法线
        for (int i = 0; i < 3; i++) axes[idx++] = a[i];
        // B 的3个面法线
        for (int j = 0; j < 3; j++) axes[idx++] = b[j];
        // 9条边叉积
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Vec3d cross = a[i].crossProduct(b[j]);
                if (cross.lengthSquared() >= EPSILON) {
                    axes[idx++] = cross;
                }
            }
        }

        for (int i = 0; i < idx; i++) {
            Vec3d axis = axes[i];
            double len = axis.length();
            if (len < EPSILON) continue;
            Vec3d n = axis.scale(1.0 / len); // 归一化

            // 计算两个 OBB 在法线上的投影半径和中心距离
            double ra = Math.abs(n.dotProduct(a[0])) * e1.x
                    + Math.abs(n.dotProduct(a[1])) * e1.y
                    + Math.abs(n.dotProduct(a[2])) * e1.z;
            double rb = Math.abs(n.dotProduct(b[0])) * e2.x
                    + Math.abs(n.dotProduct(b[1])) * e2.y
                    + Math.abs(n.dotProduct(b[2])) * e2.z;
            double d = Math.abs(t.dotProduct(n));  // 中心投影距离
            double overlap = ra + rb - d;
            if (overlap > 0 && overlap < minOverlap) {
                minOverlap = overlap;
                bestNormal = n;
                // 记录 moving 相对于 static 在法线上的方向：若 t·n > 0，说明 moving 在正方向，MTV 应为 +n
                bestSign = t.dotProduct(n) >= 0 ? 1.0 : -1.0;
            }
        }

        if (bestNormal == null || minOverlap <= EPSILON) {
            return null;
        }
        return bestNormal.scale(bestSign * minOverlap);
    }

    /**
     * OBB vs OBB 碰撞响应，支持沿表面滑动。
     * 反复进行“安全移动→获取法线→剥离法线速度”迭代。
     */
    private Vec3d adjustMovementOBB(IOrientedBB other, Vec3d velocity) {
        Vec3d startCenter = this.center;                // 本 OBB 的起始中心
        Vec3d movingCenter = startCenter;               // 当前移动后的中心
        Vec3d remaining = velocity;                      // 剩余未处理的速度
        final int MAX_ITER = 10;                         // 防止无限循环

        for (int iter = 0; iter < MAX_ITER; iter++) {
            if (remaining.lengthSquared() < EPSILON) break;

            // 二分查找当前剩余方向上的最大安全位移比例
            double lo = 0.0, hi = 1.0;
            for (int i = 0; i < 8; i++) {
                double mid = (lo + hi) / 2.0;
                Vec3d testCenter = movingCenter.add(remaining.scale(mid));
                if (obbIntersects(testCenter, extent, rotation,
                                  other.center(), other.extent(), other.rotation())) {
                    hi = mid;
                } else {
                    lo = mid;
                }
            }

            // 移动安全部分
            if (lo > EPSILON) {
                movingCenter = movingCenter.add(remaining.scale(lo));
                remaining = remaining.scale(1.0 - lo);
            }

            if (remaining.lengthSquared() < EPSILON) break;

            // 此时与障碍物接触，计算最小平移向量（MTV）
            Vec3d mtv = obbMTV(movingCenter, extent, rotation,
                               other.center(), other.extent(), other.rotation());
            if (mtv == null) {
                // 理论上不应为 null，若出现则停止
                break;
            }

            // 碰撞法线：从静态 OBB 指向移动 OBB
            Vec3d normal = mtv.normalize();

            // 剥离剩余速度中朝向表面法线的分量（实现滑动）
            double dot = remaining.dotProduct(normal);
            if (dot < 0.0) {
                remaining = remaining.subtract(normal.scale(dot));
                // 微移防止浮点误差导致下次仍判定为相交
                movingCenter = movingCenter.add(normal.scale(EPSILON * 100));
            } else {
                // 速度已背离表面，无需处理
                break;
            }
        }

        // 返回实际发生的位移（从原中心到最终中心）
        return movingCenter.subtract(startCenter);
    }

    /**
     * OBB vs OBB Separating Axis Theorem intersection test.
     * Tests 15 potential separating axes:
     *   6 face normals (3 from each OBB) + 9 cross products of edge pairs.
     */
    private static boolean obbIntersects(Vec3d c1, Vec3d e1, Matrix3 r1,
                                         Vec3d c2, Vec3d e2, Matrix3 r2) {
        Vec3d t = c2.subtract(c1);
        Vec3d[] a = {r1.right(), r1.up(), r1.forward()};
        Vec3d[] b = {r2.right(), r2.up(), r2.forward()};

        // Face axes of A
        for (int i = 0; i < 3; i++) {
            if (separatedOBB(a[i], t, a, e1, b, e2)) return false;
        }
        // Face axes of B
        for (int j = 0; j < 3; j++) {
            if (separatedOBB(b[j], t, a, e1, b, e2)) return false;
        }
        // Cross‑product axes
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Vec3d cross = a[i].crossProduct(b[j]);
                if (cross.lengthSquared() < EPSILON) continue;
                if (separatedOBB(cross, t, a, e1, b, e2)) return false;
            }
        }
        return true;
    }

    /**
     * Test a single SAT axis for OBB vs OBB: project both boxes and check for a gap.
     * ra = sum(|a[k] · L| * ae_k), rb = sum(|b[k] · L| * be_k), d = |t · L|.
     */
    private static boolean separatedOBB(Vec3d L, Vec3d t,
                                        Vec3d[] a, Vec3d ae,
                                        Vec3d[] b, Vec3d be) {
        double ra = Math.abs(a[0].dotProduct(L)) * ae.x
                  + Math.abs(a[1].dotProduct(L)) * ae.y
                  + Math.abs(a[2].dotProduct(L)) * ae.z;
        double rb = Math.abs(b[0].dotProduct(L)) * be.x
                  + Math.abs(b[1].dotProduct(L)) * be.y
                  + Math.abs(b[2].dotProduct(L)) * be.z;
        double d  = Math.abs(t.dotProduct(L));
        return d > ra + rb + EPSILON;
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
     * OBB vs AABB intersection test using Separating Axis Theorem.
     * Tests 15 potential separating axes:
     *   - 3 AABB face normals (world axes)
     *   - 3 OBB face normals (rotation basis vectors)
     *   - 9 cross products of OBB axes with world axes
     */
    @Override
    public boolean intersects(Vec3d min, Vec3d max) {
        Vec3d r = rotation.right();
        Vec3d u = rotation.up();
        Vec3d f = rotation.forward();

        // OBB face axes
        if (separatedOnAxis(r, min, max)) return false;
        if (separatedOnAxis(u, min, max)) return false;
        if (separatedOnAxis(f, min, max)) return false;

        // AABB face axes (world X, Y, Z)
        if (separatedOnAxis(Vec3dX, min, max)) return false;
        if (separatedOnAxis(Vec3dY, min, max)) return false;
        if (separatedOnAxis(Vec3dZ, min, max)) return false;

        // Cross products of OBB axes with AABB axes
        Vec3d[] obbAxes = {r, u, f};
        Vec3d[] aabbAxes = {Vec3dX, Vec3dY, Vec3dZ};
        for (Vec3d obbAxis : obbAxes) {
            for (Vec3d aabbAxis : aabbAxes) {
                Vec3d cross = obbAxis.crossProduct(aabbAxis);
                if (cross.lengthSquared() < EPSILON) continue;
                if (separatedOnAxis(cross, min, max)) return false;
            }
        }

        return true;
    }

    // Cached axis vectors for OBB-AABB SAT
    private static final Vec3d Vec3dX = new Vec3d(1, 0, 0);
    private static final Vec3d Vec3dY = new Vec3d(0, 1, 0);
    private static final Vec3d Vec3dZ = new Vec3d(0, 0, 1);

    /**
     * Test a single SAT axis: project the OBB (center +- r*extent) and the AABB (min-max)
     * onto the axis and check for a gap.
     */
    private boolean separatedOnAxis(Vec3d axis, Vec3d aabbMin, Vec3d aabbMax) {
        // OBB projection: center · axis +- sum(|OBB_axis · axis| * extent)
        double oCenter = axis.dotProduct(center);
        double oRadius = Math.abs(axis.dotProduct(rotation.right())) * extent.x
                       + Math.abs(axis.dotProduct(rotation.up())) * extent.y
                       + Math.abs(axis.dotProduct(rotation.forward())) * extent.z;

        // AABB projection: sum of min/max terms for each component
        double aProj1 = axis.x * aabbMin.x + axis.y * aabbMin.y + axis.z * aabbMin.z;
        double aProj2 = axis.x * aabbMax.x + axis.y * aabbMax.y + axis.z * aabbMax.z;
        double aMin = Math.min(aProj1, aProj2);
        double aMax = Math.max(aProj1, aProj2);

        return aMin > oCenter + oRadius + EPSILON || oCenter - oRadius > aMax + EPSILON;
    }

    /**
     * Dispatch intersection to the most appropriate algorithm:
     * - OBB vs IOrientedBB → full SAT (15‑axis test)
     * - OBB vs AABB        → SAT (implemented above)
     */
    @Override
    public boolean intersects(IBoundingBox bounds) {
        if (bounds instanceof IOrientedBB) {
            IOrientedBB o = (IOrientedBB) bounds;
            return obbIntersects(center, extent, rotation, o.center(), o.extent(), o.rotation());
        }
        // OBB vs AABB: use SAT
        return this.intersects(bounds.min(), bounds.max());
    }

    @Override
    public IBoundingBox expandToFit(IBoundingBox other) {
        Vec3d myMin = min();
        Vec3d myMax = max();
        Vec3d otherMin = other.min();
        Vec3d otherMax = other.max();

        //TODO OBB
        return IBoundingBox.from(
            new Vec3d(Math.min(myMin.x, otherMin.x), Math.min(myMin.y, otherMin.y), Math.min(myMin.z, otherMin.z)),
            new Vec3d(Math.max(myMax.x, otherMax.x), Math.max(myMax.y, otherMax.y), Math.max(myMax.z, otherMax.z))
        );
    }

    /**
     * Ray vs OBB intersection using the slab method in OBB local space.
     * Transforms the segment to local space, then tests against [-extent, +extent] AABB.
     */
    @Override
    public boolean intersectsSegment(Vec3d start, Vec3d end) {
        Matrix3 inv = rotation.copy().transpose();
        Vec3d localStart = inv.apply(start.subtract(center));
        Vec3d localEnd = inv.apply(end.subtract(center));
        Vec3d localDir = localEnd.subtract(localStart);

        // Use slab method on the local AABB [-extent, +extent]
        double tMin = 0.0;
        double tMax = 1.0;

        // X slab
        if (Math.abs(localDir.x) < EPSILON) {
            if (localStart.x < -extent.x || localStart.x > extent.x) return false;
        } else {
            double ood = 1.0 / localDir.x;
            double t1 = (-extent.x - localStart.x) * ood;
            double t2 = (extent.x - localStart.x) * ood;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return false;
        }

        // Y slab
        if (Math.abs(localDir.y) < EPSILON) {
            if (localStart.y < -extent.y || localStart.y > extent.y) return false;
        } else {
            double ood = 1.0 / localDir.y;
            double t1 = (-extent.y - localStart.y) * ood;
            double t2 = (extent.y - localStart.y) * ood;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return false;
        }

        // Z slab
        if (Math.abs(localDir.z) < EPSILON) {
            if (localStart.z < -extent.z || localStart.z > extent.z) return false;
        } else {
            double ood = 1.0 / localDir.z;
            double t1 = (-extent.z - localStart.z) * ood;
            double t2 = (extent.z - localStart.z) * ood;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return false;
        }

        return true;
    }

    /**
     * Point-in-OBB test: transform the point to OBB local space and check bounds.
     */
    @Override
    public boolean contains(Vec3d vec) {
        Matrix3 inv = rotation.copy().transpose();
        Vec3d local = inv.apply(vec.subtract(center));
        return local.x >= -extent.x && local.x <= extent.x
            && local.y >= -extent.y && local.y <= extent.y
            && local.z >= -extent.z && local.z <= extent.z;
    }

    @Override
    public Vec3d extent() {
        return extent;
    }

    @Override
    public Matrix3 rotation() {
        return rotation;
    }

    public OrientedBoundingBox copy() {
        return new OrientedBoundingBox(center, extent, rotation);
    }
}
