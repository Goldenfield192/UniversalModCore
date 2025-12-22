package cam72cam.mod.render.opengl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import java.awt.image.BufferedImage;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class RuntimeTexture implements Texture {
    private final String name;
    private final Supplier<BufferedImage> data;
    private ResourceLocation location;
    private int id;

    private boolean allowUpdate = true;
    private final Lock lock = new ReentrantLock();

    public RuntimeTexture(String name, Supplier<BufferedImage> data) {
        this.name = name;
        this.data = data;
        this.location = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation(name, new DynamicTexture(data.get()));
        this.id = Minecraft.getMinecraft().getTextureManager().getTexture(location).getGlTextureId();
    }

    public void update(BufferedImage image) {
        if (!allowUpdate) {
            throw new IllegalStateException();
        }
        synchronized (lock){
            free();
            this.location = Minecraft.getMinecraft().getTextureManager()
                                     .getDynamicTextureLocation(name, new DynamicTexture(data.get()));
            this.id = Minecraft.getMinecraft().getTextureManager().getTexture(location).getGlTextureId();
        }
    }

    public void free() {
        synchronized (lock) {
            Minecraft.getMinecraft().getTextureManager().deleteTexture(location);
            allowUpdate = false;
        }
    }

    @Override
    public int getId() {
        synchronized (lock) {
            return id;
        }
    }
}
