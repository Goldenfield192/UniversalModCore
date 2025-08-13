package cam72cam.mod.render.opengl;

import cam72cam.mod.Config;
import cam72cam.mod.ModCore;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.resource.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public abstract class CustomTexture implements Texture {
    private Identifier name;
    private final int width;
    private final int height;
    private final int cacheSeconds;

    private static final ExecutorService pool = Executors.newFixedThreadPool(1, runnable -> {
        Thread thread = new Thread(runnable);
        thread.setName("UMC-TextureLoader");
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });

    private Future<ByteBuffer> loader = null;
    private long lastUsed;
    private Integer textureID;

    private static final List<CustomTexture> textures = new ArrayList<>();

    public static void registerClientEvents() {
        // free unused textures
        ClientEvents.TICK.subscribe(() -> {
            try {
                synchronized (textures) {
                    for (CustomTexture texture : textures) {
                        if (texture.textureID != null && System.currentTimeMillis() - texture.lastUsed > texture.cacheSeconds * 1000 && (texture.loader == null || !texture.loader.isDone())) {
                            texture.dealloc();
                        }
                    }
                }
            } catch (Exception ex) {
                ModCore.catching(ex);
            }
        });
    }


    public CustomTexture(int width, int height, int cacheSeconds, Identifier name) {
        synchronized (textures) {
            textures.add(this);
        }
        this.width = width;
        this.height = height;
        this.cacheSeconds = cacheSeconds;
        this.name = name;
    }

    protected abstract ByteBuffer getData();

    private void createTexture(ByteBuffer buffer) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] data = buffer.asIntBuffer().array();
        //Wrap back to ARGB
        for(int i = 0; i < width * height; i++){
            int c_argb = data[i];
            int r = c_argb >> 24 & 255;
            int g = c_argb >> 16 & 255;
            int b = c_argb >> 8 & 255;
            int a = c_argb & 255;
            data[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
        image.setRGB(0, 0, width, height, data, 0, width);
        DynamicTexture texture = new DynamicTexture(image);
        Minecraft.getMinecraft().renderEngine.loadTexture(name.internal, texture);
        textureID = Minecraft.getMinecraft().renderEngine.getTexture(name.internal).getGlTextureId();
    }

    private void threadedLoader() {
        synchronized (textures) {
            if (loader != null) {
                if (loader.isDone()) {
                    try {
                        createTexture(loader.get());
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                    loader = null;
                }
            } else {
                // Start thread
                loader = pool.submit(this::getData);
            }
        }
    }

    private void directLoader() {
        createTexture(getData());
    }

    public Texture synchronous(boolean sync) {
        lastUsed = System.currentTimeMillis();

        if (textureID == null) {
            if (sync) {
                directLoader();
            } else {
                return this;
            }
        }
        return () -> textureID;
    }

    public boolean isLoaded() {
        return textureID != null;
    }

    @Override
    public int getId() {
        lastUsed = System.currentTimeMillis();

        if (textureID == null) {
            if (Config.ThreadedTextureLoading) {
                threadedLoader();
            } else {
                directLoader();
            }
        }
        return textureID == null ? NO_TEXTURE.getId() : this.textureID;
    }

    public Identifier getName() {
        return name;
    }

    public void dealloc() {
        synchronized (textures) {
            if (this.textureID != null) {
                Minecraft.getMinecraft().renderEngine.deleteTexture(name.internal);
                this.name = null;
                this.textureID = null;
                this.loader = null;
            }
        }
    }
}
