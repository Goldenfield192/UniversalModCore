package cam72cam.mod.model.common;

import java.util.HashMap;

public class MeshAttrMap {
    private final HashMap<MeshAttr<?>, Object> attrObjectHashMap = new HashMap<>();

    public <T> void put(MeshAttr<T> attr, T value) {
        attrObjectHashMap.put(attr, value);
    }

    public <T> T get(MeshAttr<T> attr) {
        return (T) attrObjectHashMap.get(attr);
    }
}
