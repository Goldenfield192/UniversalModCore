package cam72cam.mod.render.opengl;

import cam72cam.mod.Config;
import cam72cam.mod.ModCore;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.model.obj.ImageUtils;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.util.With;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public abstract class CustomTexture implements Texture {
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
    private Identifier textureID;
    private volatile boolean isLoaded;

    private static final List<CustomTexture> textures = new ArrayList<>();
    private int counter = 0;

    public static void registerClientEvents() {
        // free unused textures
        ClientEvents.TICK.subscribe(() -> {
            try {
                synchronized (textures) {
                    for (CustomTexture texture : textures) {
                        if (texture.isLoaded && System.currentTimeMillis() - texture.lastUsed > texture.cacheSeconds * 1000L && (texture.loader == null || !texture.loader.isDone())) {
                            texture.dealloc();
                        }
                    }
                }
            } catch (Exception ex) {
                ModCore.catching(ex);
            }
        });
    }


    public CustomTexture(int width, int height, int cacheSeconds) {
        synchronized (textures) {
            textures.add(this);
        }
        this.width = width;
        this.height = height;
        this.cacheSeconds = cacheSeconds;
    }

    protected abstract ByteBuffer getData();

    private void createTexture(ByteBuffer buffer) {
        BufferedImage image = ImageUtils.fromRGBA(buffer.array(), width, height);
        DynamicTexture texture = new DynamicTexture(image);
        if (this.textureID == null) {
            this.textureID = new Identifier(Minecraft.getMinecraft().getTextureManager()
                                                     .getDynamicTextureLocation("umc_generated_" + counter, texture));
            counter++;
        }
    }

    private void threadedLoader() {
        synchronized (textures) {
            if (loader != null) {
                if (loader.isDone()) {
                    try {
                        createTexture(loader.get());
                        this.isLoaded = true;
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
        this.isLoaded = true;
    }

    public Texture synchronous(boolean sync) {
        lastUsed = System.currentTimeMillis();

        if (!isLoaded) {
            if (sync) {
                directLoader();
            } else {
                return this;
            }
        }
        return () -> textureID;
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    @Override
    public Identifier getId() {
        lastUsed = System.currentTimeMillis();

        if (!isLoaded) {
            if (Config.ThreadedTextureLoading) {
                threadedLoader();
            } else {
                directLoader();
            }
        }
        return !isLoaded ? NO_TEXTURE.getId() : this.textureID;
    }

    public void dealloc() {
        synchronized (textures) {
            if (this.isLoaded) {
                Minecraft.getMinecraft().getTextureManager().deleteTexture(this.textureID.internal);
                this.isLoaded = false;
                this.loader = null;
            }
        }
    }
}
