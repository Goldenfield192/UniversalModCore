package cam72cam.mod.model.common;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.FaceAccessor;

public class ModelGroup {
    protected final Geometry parent;

    public final String name;
    public final int faceStart;
    public final int faceStop;
    public final Vec3d min;
    public final Vec3d max;
    public final Vec3d normal;

    ModelGroup(Geometry parent, String groupName, int faceStart, int faceStop) {
        this.parent = parent;
        this.name = groupName;
        this.faceStart = faceStart;
        this.faceStop = faceStop;

        FaceAccessor accessor = parent.getAccessor();
        FaceAccessor group = accessor.getSubByGroup(groupName);
        for (FaceAccessor faceAccessor : group) {

        }
    }
}
