package cam72cam.mod.render.obj;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.ElementBuffer;
import cam72cam.mod.model.obj.OBJGroup;
import cam72cam.mod.model.obj.OBJModel;
import cam72cam.mod.render.opengl.EBO;
import cam72cam.mod.render.opengl.RenderContext;
import cam72cam.mod.util.With;
import cam72cam.mod.render.opengl.RenderState;
import org.lwjgl.opengl.GL32;
import util.Matrix4;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class OBJRender extends EBO {
    public final OBJModel model;
    public final Supplier<ElementBuffer> buffer;

    public OBJRender(OBJModel model, Supplier<ElementBuffer> buffer) {
        super(buffer, s -> {});
        this.model = model;
        this.buffer = buffer;
    }

    public Binding bind(RenderState state) {
        return bind(state, false);
    }
    public Binding bind(RenderState state, boolean waitForLoad) {
        return new Binding(state, waitForLoad);
    }

    public class Binding extends EBO.Binding {
        protected Binding(RenderState state, boolean wait) {
            super(state, wait);
        }

        public void draw(Collection<String> groups, Consumer<RenderState> mod) {
            if (!isLoaded()) {
                return;
            }
            try (With pus = super.push(mod)) {
                draw(groups);
            }
        }

        /**
         * Draw these groups in the VB
         */
        public void draw(Collection<String> groups) {
            if (!isLoaded()) {
                return;
            }
            RenderContext.checkError();
            List<String> sorted = new ArrayList<>(groups);
            sorted.sort(Comparator.naturalOrder());
            int start = -1;
            int stop = -1;
            for (String group : sorted) {
                OBJGroup info = model.groups.get(group);
                if (start == stop) {
                    start = info.faceStart;
                    stop = info.faceStop + 1;
                } else if (info.faceStart == stop) {
                    stop = info.faceStop + 1;
                } else {
                    GL32.glDrawElements(GL32.GL_TRIANGLES, (stop - start) * 3, GL32.GL_UNSIGNED_INT, (long) start * 3 * buffer.get().stride * Float.BYTES);
                    start = info.faceStart;
                    stop = info.faceStop + 1;
                }
            }
            if (start != stop) {
                GL32.glDrawElements(GL32.GL_TRIANGLES, (stop - start) * 3, GL32.GL_UNSIGNED_INT, (long) start * 3 * buffer.get().stride * Float.BYTES);
            }
            RenderContext.checkError();
        }
    }

    public class Builder {
        private final Consumer<RenderState> settings;
        private final List<Consumer<Buffer>> actions = new ArrayList<>();

        private Builder(Consumer<RenderState> settings) {
            this.settings = settings;
        }

        private class Buffer {
            private ElementBuffer eb;
            private float[] vertex;
            private int[] indices;
            private int builtIdx;

            private Buffer() {
                this.eb = buffer.get();
                this.vertex = new float[eb.getVBO().array().length];
                this.indices = new int[eb.getEBO().array().length];
                this.builtIdx = 0;
            }

            private void require(int size) {
                while (vertex.length <= builtIdx + size) {
                    float[] tmp = new float[vertex.length * 2];
                    System.arraycopy(vertex, 0, tmp, 0, builtIdx);
                    vertex = tmp;
                }
            }

            private void add(float[] buff, Matrix4 m) {
                require(buff.length);

                if (m != null) {
                    for (int i = 0; i < buff.length; i += eb.stride) {
                        float x = buff[i+0];
                        float y = buff[i+1];
                        float z = buff[i+2];
                        Vec3d v = m.apply(new Vec3d(x, y, z));
                        buff[i+0] = (float) v.x;
                        buff[i+1] = (float) v.y;
                        buff[i+2] = (float) v.z;
                    }
                }

                System.arraycopy(buff, 0, vertex, builtIdx, buff.length);
                builtIdx += buff.length;
            }

            public void draw(Matrix4 m) {
                if (m == null) {
                    add(eb.data, null);
                } else {
                    float[] buff = new float[eb.data.length];
                    System.arraycopy(eb.data, 0, buff, 0, eb.data.length);
                    add(buff, m);
                }
            }

            public void draw(Collection<String> groups, Matrix4 m) {
                for (String group : groups) {
                    OBJGroup info = model.groups.get(group);

                    int start = info.faceStart * eb.vertsPerFace * eb.stride;
                    int stop = (info.faceStop + 1) * eb.vertsPerFace * eb.stride;

                    float[] buff = new float[stop - start];
                    System.arraycopy(eb.data, start, buff, 0, stop - start);
                    add(buff, m);
                }
            }

            public ElementBuffer build() {
                float[] out = new float[builtIdx];
                System.arraycopy(vertex, 0, out, 0, builtIdx);
                boolean hasNormals = eb.hasNormals;
                eb = null;
                vertex = null;
                return new ElementBuffer(out, hasNormals);
            }
        }

        public void draw() {
            draw((Matrix4) null);
        }

        public void draw(Matrix4 m) {
            actions.add(b -> b.draw(m));
        }

        public void draw(Collection<String> groups) {
            draw(groups, null);
        }

        public void draw(Collection<String> groups, Matrix4 m) {
            actions.add(b -> b.draw(groups, m));
        }

        public EBO build() {
            List<Consumer<Buffer>> actions = new ArrayList<>(this.actions); // Snapshot
            return new EBO(() -> {
                Buffer buff = new Buffer();
                actions.forEach(c -> c.accept(buff));
                return buff.build();
            }, settings);
        }
    }

    public Builder subModel(Consumer<RenderState> settings) {
        return new Builder(settings);
    }
}
