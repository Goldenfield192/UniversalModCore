package cam72cam.mod.render.model;

import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.render.obj.OBJTextureSheet;
import cam72cam.mod.render.opengl.RenderState;

import java.util.*;

public class ModelRenderer {
    private static final Map<Model, ModelRenderer> renders = new HashMap<>();

    private final Model model;
    private VBOHolder vbo;
    private long lastUsed;

    public static ModelRenderer getOrCreate(Model model) {
        synchronized (renders) {
            return renders.computeIfAbsent(model, ModelRenderer::new);
        }
    }

    private ModelRenderer(Model model) {
        this.model = model;
    }

    /** Frees ModelRenderers that haven't been drawn for 30 seconds (client tick). */
    public static void registerClientEvents() {
        ClientEvents.TICK.subscribe(() -> {
            synchronized (renders) {
                List<Model> toRemove = new ArrayList<>();
                for (Map.Entry<Model, ModelRenderer> entry : renders.entrySet()) {
                    if (entry.getValue().vbo != null && entry.getValue().vbo.isAllocated()
                            && System.currentTimeMillis() - entry.getValue().lastUsed > 30 * 1000) {
                        entry.getValue().free();
                        toRemove.add(entry.getKey());
                    }
                }
                toRemove.forEach(renders.keySet()::remove);
            }
        });
    }

    public Binding binding() {
        return new Binding();
    }

    public class Binding {
        private int lod = -1;
        private String variant = "";
        private RenderState state = new RenderState();

        public Binding lod(int lod) {
            this.lod = lod;
            return this;
        }

        public Binding texture(String variant) {
            this.variant = variant;
            return this;
        }

        public Binding state(RenderState state) {
            this.state = state.clone();
            return this;
        }

        public void opaque() {
            opaque(Collections.emptySet());
        }

        public void opaque(Collection<String> groups) {
            RenderQueue.submitOpaque(ModelRenderer.this, variant, lod, state, groups);
        }

        public void transparent() {
            transparent(Collections.emptySet());
        }

        public void transparent(Collection<String> groups) {
            RenderQueue.submitTransparent(ModelRenderer.this, variant, lod, state, groups);
        }
    }

    void draw(String variant, int lod, RenderState state, Collection<String> groups) {


        lastUsed = System.currentTimeMillis();
        RenderState s = state.clone().smooth_shading(model.isSmoothShading).cull_face(false);
        applyTextures(s, variant, lod);
        vbo().draw(s, groups);
    }

    private VBOHolder vbo() {
        if (vbo == null) {
            vbo = new VBOHolder(model);
        }
        return vbo;
    }

    private void applyTextures(RenderState s, String variant, int lod) {
        OBJTextureSheet albedo = pick(mapFor(model.getTextures(), variant), lod);
        if (albedo != null) {
            s.texture(albedo);
        }
        if (model.hasSpecular) {
            OBJTextureSheet spec = pick(mapFor(model.getSpeculars(), variant), lod);
            if (spec != null) {
                s.specular(spec);
            }
        }
        if (model.hasNormal) {
            OBJTextureSheet norm = pick(mapFor(model.getNormals(), variant), lod);
            if (norm != null) {
                s.normals(norm);
            }
        }
    }

    private static Map<Integer, OBJTextureSheet> mapFor(Map<String, Map<Integer, OBJTextureSheet>> all, String variant) {
        Map<Integer, OBJTextureSheet> map = all.get(variant);
        return map != null ? map : all.get("");
    }

    /** Largest sheet with size <= target, or the smallest when all are larger. */
    private static OBJTextureSheet pick(Map<Integer, OBJTextureSheet> lodMap, int target) {
        if (lodMap == null || lodMap.isEmpty()) {
            return null;
        }
        if (target <= 0) {
            return lodMap.get(Collections.max(lodMap.keySet()));
        }
        Integer best = null;
        for (Integer size : lodMap.keySet()) {
            if (size <= target && (best == null || size > best)) {
                best = size;
            }
        }
        return lodMap.get(best != null ? best : Collections.min(lodMap.keySet()));
    }

    public void free() {
        synchronized (renders) {
            if (vbo != null) {
                vbo.free();
                vbo = null;
            }
        }
    }
}
