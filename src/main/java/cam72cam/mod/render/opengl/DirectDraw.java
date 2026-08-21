package cam72cam.mod.render.opengl;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.util.With;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class DirectDraw {
    private final List<VertexBuilder> verts = new ArrayList<>();
    private Mode mode;

    public DirectDraw() {
        this(Mode.QUADS);
    }

    public DirectDraw(Mode mode) {
        this.mode = mode;
    }

    public void draw(RenderState state) {
        try (With ctx = RenderContext.apply(state)) {
            GL11.glBegin(mode.asGlMode());
            for (VertexBuilder vert : verts) {
                vert.draw();
            }
            GL11.glEnd();
        }
    }

    public VertexBuilder vertex(double x, double y, double z) {
        VertexBuilder target = new VertexBuilder(x, y, z);
        verts.add(target);
        return target;
    }

    public VertexBuilder vertex(Vec3d pos) {
        return vertex(pos.x, pos.y, pos.z);
    }

    public static class VertexBuilder {
        private final double x;
        private final double y;
        private final double z;
        private Double u;
        private Double v;
        private Double j;
        private Double k;
        private Double l;
        private Double r;
        private Double g;
        private Double b;
        private Double a;

        private VertexBuilder(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public VertexBuilder uv(double u, double v) {
            this.u = u;
            this.v = v;
            return this;
        }

        public VertexBuilder normal(double j, double k, double l) {
            this.j = j;
            this.k = k;
            this.l = l;
            return this;
        }

        public VertexBuilder color(double r, double g, double b, double a) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            return this;
        }

        private void draw() {
            if (u != null) {
                GL11.glTexCoord2d(u, v);
            }
            if (r != null) {
                GL11.glColor4d(r, g, b, a);
            }
            if (j != null) {
                GL11.glNormal3d(j, k, l);
            }
            GL11.glVertex3d(x, y, z);
        }
    }

    public enum Mode {
        LINES(GL11.GL_LINES),
        LINE_STRIP(GL11.GL_LINE_STRIP),
        TRIANGLES(GL11.GL_TRIANGLES),
        TRIANGLE_STRIP(GL11.GL_TRIANGLE_STRIP),
        TRIANGLE_FAN(GL11.GL_TRIANGLE_FAN),
        QUADS(GL11.GL_QUADS),
        ;

        private final int glMode;

        Mode(int glMode) {
            this.glMode = glMode;
        }

        int asGlMode() {
            return glMode;
        }
    }
}
