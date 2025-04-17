package cam72cam.mod.render.opengl;

import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.model.obj.ElementBuffer;
import cam72cam.mod.util.With;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL32;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class EBO {
    private static final List<EBO> ebos = new ArrayList<>();
    public static void registerClientEvents() {
        // free unused textures
        ClientEvents.TICK.subscribe(() -> {
            synchronized (ebos) {
                for (EBO ebo : ebos) {
                    if (ebo.ebo != -1 && System.currentTimeMillis() - ebo.lastUsed > 30 * 1000) {
                        ebo.free();
                    }
                }
            }
        });
    }

    private final Supplier<ElementBuffer> buffer;
    private final Consumer<RenderState> settings;

    private int vao;
    private int vbo;
    private int ebo;
    private int length;
    private long lastUsed;
    private ElementBuffer ebInfo;

    private static final ExecutorService pool = new ThreadPoolExecutor(0, Runtime.getRuntime().availableProcessors(),
                                                                       5L, TimeUnit.SECONDS,
                                                                       new LinkedBlockingQueue<>(),
                                                                       runnable -> {
                                                                           Thread thread = new Thread(runnable);
                                                                           thread.setName("UMC-ElementBufferLoader");
                                                                           thread.setPriority(Thread.MIN_PRIORITY);
                                                                           return thread;
                                                                       });
    private Future<Pair<FloatBuffer, IntBuffer>> loader = null;

    public EBO(Supplier<ElementBuffer> buffer, Consumer<RenderState> settings) {
        this.buffer = buffer;
        this.vao = -1;
        this.ebo = -1;
        this.settings = settings;

        synchronized (ebos) {
            ebos.add(this);
        }
    }

    private void init() {
        if (loader != null) {
            if (loader.isDone()) {
                try {
                    int oldVao = GL32.glGetInteger(GL32.GL_VERTEX_ARRAY_BUFFER_BINDING);
                    int oldVbo = GL32.glGetInteger(GL32.GL_ARRAY_BUFFER_BINDING);
                    int oldEbo = GL32.glGetInteger(GL32.GL_ELEMENT_ARRAY_BUFFER_BINDING);

                    vao = GL32.glGenVertexArrays();
                    GL32.glBindVertexArray(vao);

                    vbo = GL32.glGenBuffers();
                    GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, vbo);
                    GL32.glBufferData(GL32.GL_ARRAY_BUFFER, loader.get().getKey(), GL32.GL_STATIC_DRAW);

                    ebo = GL32.glGenBuffers();
                    GL32.glBindBuffer(GL32.GL_ELEMENT_ARRAY_BUFFER, ebo);
                    GL32.glBufferData(GL32.GL_ELEMENT_ARRAY_BUFFER, loader.get().getValue(), GL32.GL_STATIC_DRAW);

                    GL32.glBindVertexArray(oldVao);
                    GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, oldVbo);
                    GL32.glBindBuffer(GL32.GL_ELEMENT_ARRAY_BUFFER, oldEbo);
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
                loader = null;
            }
        } else {
            // Start thread
            loader = pool.submit(() -> {
                ElementBuffer eb = buffer.get();
                this.ebInfo = new ElementBuffer(0, eb.hasNormals);
                eb.genData();
                return Pair.of(eb.getVBO(), eb.getEBO());
            });
        }
    }

    public EBO.Binding bind(RenderState state) {
        return bind(state, false);
    }

    public EBO.Binding bind(RenderState state, boolean waitForLoad) {
        return new EBO.Binding(state, waitForLoad);
    }

    public class Binding implements With {
        private final With restore;
        private final RenderState state;

        public boolean isLoaded() {
            return ebo != -1;
        }


        protected Binding(RenderState state, boolean wait) {
            if (!isLoaded()) {
                init();
            }
            RenderContext.checkError();
            lastUsed = System.currentTimeMillis();

            if (!wait) {
                if (!isLoaded()) {
                    restore = () -> {
                    };
                    this.state = null;
                    return;
                }
            } else {
                while (!isLoaded()) {
                    init();
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            settings.accept(state);
            this.state = state;

            ShaderInstance oldShader = RenderSystem.getShader();
            //int oldVao = GL32.glGetInteger(GL32.GL_VERTEX_ARRAY_BUFFER_BINDING);
            //int oldVbo = GL32.glGetInteger(GL32.GL_ARRAY_BUFFER_BINDING);


            /*
            GL32.glEnableClientState(GL32.GL_VERTEX_ARRAY);
            GL32.glEnableClientState(GL32.GL_TEXTURE_COORD_ARRAY);
            GL32.glEnableClientState(GL32.GL_COLOR_ARRAY);
            if (vbInfo.hasNormals) {
                GL32.glEnableClientState(GL32.GL_NORMAL_ARRAY);
            } else {
                GL32.glDisableClientState(GL32.GL_NORMAL_ARRAY);
            }*/

            ShaderInstance shader = GameRenderer.getRendertypeCutoutShader();
            RenderSystem.setShader(() -> shader);
            GL32.glBindVertexArray(vao);
            GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, vbo);
            GL32.glBindBuffer(GL32.GL_ELEMENT_ARRAY_BUFFER, ebo);

            int stride = ebInfo.stride * Float.BYTES;

            ImmutableList<VertexFormatElement> elements = shader.getVertexFormat().getElements();
            for (int i = 0; i < elements.size(); i++) {
                VertexFormatElement element = elements.get(i);
                switch (element.getUsage()) {
                    case POSITION -> {
                        //element.setupBufferState(i, (long) vbInfo.vertexOffset * Float.BYTES, stride);
                        GL32.glEnableVertexAttribArray(i);
                        GL32.glVertexAttribPointer(i, 3, GL32.GL_FLOAT, false, stride, (long) ebInfo.vertexOffset * Float.BYTES);
                    }
                    case NORMAL -> {
                        if (ebInfo.hasNormals) {
                            GL32.glEnableVertexAttribArray(i);
                            GL32.glVertexAttribPointer(i, 3, GL32.GL_FLOAT, true, stride, (long) ebInfo.normalOffset * Float.BYTES);
                        }
                    }
                    case COLOR -> {
                        GL32.glEnableVertexAttribArray(i);
                        GL32.glVertexAttribPointer(i, 4, GL32.GL_FLOAT, true, stride, (long) ebInfo.colorOffset * Float.BYTES);
                    }
                    case UV -> {
                        for (Map.Entry<String, VertexFormatElement> entry : shader.getVertexFormat().getElementMapping().entrySet()) {
                            if (entry.getValue() == element) {
                                if (entry.getKey().equals("UV0")) {
                                    GL32.glEnableVertexAttribArray(i);
                                    GL32.glVertexAttribPointer(i, 2, GL32.GL_FLOAT, false, stride, (long) ebInfo.textureOffset * Float.BYTES);
                                } else if (entry.getKey().equals("UV1")) {
                                    // TODO
                                } else if (entry.getKey().equals("UV2")) {
                                    GL32.glDisableVertexAttribArray(i);
                                    int x = 255;
                                    int y = 255;
                                    if (state.lightmap != null) {
                                        x = (int) (state.lightmap[0] * 255);
                                        y = (int) (state.lightmap[1] * 255);
                                    }
                                    GL32.glVertexAttribI2i(i, x, y);
                                }
                            }
                        }
                    }
                }
            }
            RenderContext.checkError();

            this.restore = RenderContext.apply(state).and(() -> {
                RenderContext.checkError();
                shader.getVertexFormat().clearBufferState();

                RenderContext.checkError();

                //GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, oldVbo);
                //GL32.glBindBuffer(GL32.GL_ELEMENT_ARRAY_BUFFER, 0);
                //GL32.glBindVertexArray(oldVao);
                RenderSystem.setShader(() -> oldShader);
                BufferUploader.reset();
            });
        }

        @Override
        public void restore() {
            restore.close();
        }

        protected With push(Consumer<RenderState> mod) {
            if (!isLoaded()) {
                return () -> {};
            }
            RenderState state = this.state.clone();
            mod.accept(state);
            return RenderContext.apply(state);
        }

        /**
         * Draw the entire VB
         */
        public void draw() {
            if (!isLoaded()) {
                return;
            }
            GL32.glDrawElements(GL32.GL_TRIANGLES, 0, GL32.GL_UNSIGNED_INT, 0);
            RenderContext.checkError();
        }
    }

    /**
     * Clear this VB from standard and GPU memory
     */
    public void free() {
        synchronized (ebos) {
            if (ebo != -1) {
                GL32.glDeleteBuffers(vbo);
                GL32.glDeleteBuffers(ebo);
                GL32.glDeleteVertexArrays(vao);
                ebo = -1;
            }
        }
    }
}
