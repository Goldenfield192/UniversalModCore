package cam72cam.mod.render.opengl;

import cam72cam.mod.resource.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureUtil;

public interface Texture {
    Texture NO_TEXTURE = wrap(new Identifier(Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("missing", TextureUtil.MISSING_TEXTURE)));

    Identifier getId();

    @Deprecated
    static Texture wrap(int id) {
        WrappedTexture texture = new WrappedTexture(id);
        return () -> texture.loc;
    }

    static Texture wrap(Identifier id) {
        return new MinecraftTexture(id);
    }
}
