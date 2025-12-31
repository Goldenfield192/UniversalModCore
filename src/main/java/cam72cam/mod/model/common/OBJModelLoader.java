package cam72cam.mod.model.common;

import cam72cam.mod.resource.Identifier;

public class OBJModelLoader extends AbstractModelLoader {
    public static final OBJModelLoader INSTANCE = new OBJModelLoader();

    @Override
    public Geometry load(Identifier stream) {
        return null;
    }

    @Override
    public String computeCacheKey(Identifier ident, double scale) {
        return String.join("-", ident.toString(), String.valueOf(scale));
    }
}
