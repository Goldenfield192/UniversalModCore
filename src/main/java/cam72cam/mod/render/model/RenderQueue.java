package cam72cam.mod.render.model;

import cam72cam.mod.render.opengl.RenderState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RenderQueue {
    private static boolean IMMEDIATE = true;

    private static final List<DrawCall> OPAQUE = new ArrayList<>();
    private static final List<DrawCall> TRANSPARENT = new ArrayList<>();

    static void submitOpaque(ModelRenderer render, String variant, int lod, RenderState state, Collection<String> groups) {
        DrawCall call = new DrawCall(render, variant, lod, state, groups);
        OPAQUE.add(call);
        if (IMMEDIATE) {
            flush();
        }
    }

    static void submitTransparent(ModelRenderer render, String variant, int lod, RenderState state, Collection<String> groups) {
        DrawCall call = new DrawCall(render, variant, lod, state, groups);
        TRANSPARENT.add(call);
        if (IMMEDIATE) {
            flush();
        }
    }

    public static void flush() {
        OPAQUE.forEach(DrawCall::draw);
        TRANSPARENT.forEach(DrawCall::draw);
        OPAQUE.clear();
        TRANSPARENT.clear();
    }

    private static class DrawCall {
        final ModelRenderer render;
        final String variant;
        final int lod;
        final RenderState state;
        final Collection<String> groups;

        DrawCall(ModelRenderer render, String variant, int lod, RenderState state, Collection<String> groups) {
            this.render = render;
            this.variant = variant;
            this.lod = lod;
            this.state = state;
            this.groups = groups;
        }

        void draw() {
            render.draw(variant, lod, state, groups);
        }
    }
}
