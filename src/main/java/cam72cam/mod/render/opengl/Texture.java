package cam72cam.mod.render.opengl;

import cam72cam.mod.resource.Identifier;

public interface Texture {
    Texture NO_TEXTURE = Texture.wrap(new Identifier("universalmodcore", "null"));

    Identifier getName();

    static Texture wrap(Identifier id) {
        return new MinecraftTexture(id);
    }
}
