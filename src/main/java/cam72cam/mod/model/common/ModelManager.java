package cam72cam.mod.model.common;

import cam72cam.mod.resource.Identifier;

import java.util.Map;

public class ModelManager {
    private static Map<Identifier, Geometry> models;
    private static Map<String, AbstractModelLoader> loaders;

    public static Geometry load(Identifier ident, double scale) {
        String ext = ident.getPath().split("\\.")[ident.getPath().split("\\.").length - 1];
        return load(ident, ext, scale);
    }

    public static Geometry load(Identifier ident, String ext, double scale) {
        if (models.containsKey(ident)) {
            return models.get(ident);
        }
        AbstractModelLoader loader = loaders.getOrDefault(ext, OBJModelLoader.INSTANCE);
        String key = loader.computeCacheKey(ident, scale); //TODO cache

        Geometry model = loader.load(ident);
        models.put(ident, model);
        return model;
    }
}
