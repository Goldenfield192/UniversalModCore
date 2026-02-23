package cam72cam.mod.loading;

public class Dependency {
    public final String modid;
    public final String acceptableVersion;
    public final Order order;
    public final Dist dist;
    public final Type dependencyType;

    public Dependency(String modid, String acceptableVersion, Order order, Dist dist, Type dependencyType) {
        this.modid = modid;
        this.acceptableVersion = acceptableVersion;
        this.order = order;
        this.dist = dist;
        this.dependencyType = dependencyType;
    }

    public enum Order {
        BEFORE,
        AFTER
    }

    public enum Dist {
        DEDICATED_SERVER,
        CLIENT
    }

    public enum Type {
        REQUIRED,
        OPTIONAL,
        INCOMPATIBLE
    }
}
