package cam72cam.mod.model.common;

public class MeshAttr<T> {
    public final String name;
    public final Class<T> type;

    public MeshAttr(String name, Class<T> type) {
        this.name = name;
        this.type = type;
    }
}
