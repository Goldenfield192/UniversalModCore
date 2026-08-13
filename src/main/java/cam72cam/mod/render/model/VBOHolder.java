package cam72cam.mod.render.model;

import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.model.common.mesh.ModelGroup;
import cam72cam.mod.model.common.mesh.VAOLayout;
import cam72cam.mod.render.opengl.RenderContext;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.util.With;
import net.minecraft.client.renderer.GLAllocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;
import java.util.*;

/** Owns the GL buffer for a Model and draws it (whole or by groups). All GL calls live here. */
class VBOHolder {
    private final Model model;
    private int vbo = -1;
    private int vertexCount;

    VBOHolder(Model model) {
        this.model = model;
    }

    void draw(RenderState s, Collection<String> groups) {
        ensureVbo();

        VAOLayout layout = model.getLayout();
        int oldVbo = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT);

        try (With ctx = RenderContext.apply(s).and(() -> {
            GL11.glPopClientAttrib();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, oldVbo);
        })) {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            layout.setup();
            drawArrays(groups);
            layout.restore();
        }
    }

    /** Draws the whole model, or only the requested groups with contiguous face ranges batched. */
    private void drawArrays(Collection<String> groups) {
        if (groups == null || groups.isEmpty()) {
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, vertexCount);
            return;
        }
        List<String> sorted = new ArrayList<>(groups);
        sorted.sort(Comparator.naturalOrder());
        int start = -1;
        int stop = -1;
        for (String name : sorted) {
            ModelGroup group = model.getGroups().get(name);
            if (group == null) {
                continue;
            }
            if (start == -1) {
                start = group.faceStart;
                stop = group.faceEnd;
            } else if (group.faceStart == stop) {
                stop = group.faceEnd;
            } else {
                GL11.glDrawArrays(GL11.GL_TRIANGLES, start * 3, (stop - start) * 3);
                start = group.faceStart;
                stop = group.faceEnd;
            }
        }
        if (start != -1) {
            GL11.glDrawArrays(GL11.GL_TRIANGLES, start * 3, (stop - start) * 3);
        }
    }

    private void ensureVbo() {
        if (vbo == -1) {
            int oldVbo = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);

            float[] data = model.getVboData();
            vbo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            FloatBuffer buffer = GLAllocation.createDirectFloatBuffer(data.length);
            buffer.put(data);
            buffer.flip();
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
            vertexCount = data.length / (model.getLayout().getStride() / 4);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, oldVbo);
        }
    }

    boolean isAllocated() {
        return vbo != -1;
    }

    void free() {
        if (vbo != -1) {
            GL15.glDeleteBuffers(vbo);
            vbo = -1;
        }
    }
}
