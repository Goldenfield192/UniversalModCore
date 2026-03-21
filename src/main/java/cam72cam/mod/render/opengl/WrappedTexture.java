package cam72cam.mod.render.opengl;

import cam72cam.mod.resource.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.resources.IResourceManager;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Internal, don't use
 */
public class WrappedTexture extends AbstractTexture {
    public final Identifier loc;
    private static final AtomicInteger counter = new AtomicInteger(0);

    public WrappedTexture(int id) {
        this.glTextureId = id;
        this.loc = new Identifier("umc_wrapped/" + counter.getAndIncrement());
        Minecraft.getMinecraft().getTextureManager().loadTexture(loc.internal, this);
    }

    @Override
    public void loadTexture(IResourceManager resourceManager) throws IOException {
    }
}
