package cam72cam.mod.model.common;

import cam72cam.mod.resource.Identifier;

public abstract class AbstractModelLoader {
    public abstract Geometry load(Identifier stream);
    public abstract String computeCacheKey(Identifier ident, double scale);
}
