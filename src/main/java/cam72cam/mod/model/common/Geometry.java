package cam72cam.mod.model.common;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.FaceAccessor;
import cam72cam.mod.model.obj.VertexBuffer;

import java.util.Map;

public class Geometry {
    protected VertexBuffer buffer;
    protected Map<String, Material> materials;
    protected Map<String, ModelGroup> groups;

    public ModelGroup getGroup(String s) {
        return groups.get(s);
    }

    public Vec3d minOfGroups(Iterable<String> groups) {
        Vec3d min = new Vec3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
        for (String s : groups) {
            min = min.min(this.groups.get(s).min);
        }
        return min;
    }

    public Vec3d maxOfGroups(Iterable<String> groups) {
        Vec3d max = new Vec3d(Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE);
        for (String s : groups) {
            max = max.max(this.groups.get(s).max);
        }
        return max;
    }

    public Vec3d centerOfGroups(Iterable<String> groups) {
        return minOfGroups(groups).add(maxOfGroups(groups)).scale(0.5);
    }
    public double lengthOfGroups(Iterable<String> groups) {
        return maxOfGroups(groups).subtract(minOfGroups(groups)).x;
    }
    public double heightOfGroups(Iterable<String> groups) {
        return maxOfGroups(groups).subtract(minOfGroups(groups)).y;
    }
    public double widthOfGroups(Iterable<String> groups) {
        return maxOfGroups(groups).subtract(minOfGroups(groups)).z;
    }

    public FaceAccessor getAccessor() { //TODO provide native and randomized accessor
        return new FaceAccessor();
    }

    public/*TODO*/ void getRenderer();

    public void free();
}
