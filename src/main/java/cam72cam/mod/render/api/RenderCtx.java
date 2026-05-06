package cam72cam.mod.render.api;

import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.util.With;

public abstract class RenderCtx {
    private static RenderCtx INSTANCE;

    public static void init(RenderCtx instance) {
        INSTANCE = instance;
    }

    public static RenderCtx getInstance() {
        return INSTANCE;
    }

    public abstract With apply(RenderState state);

    public enum Stage {
        BLOCK,

        ENTITY,

        ITEM_SPRITE_TEX,
        ITEM_IN_WORLD,
        ITEM_IN_GUI,

        GUI,

        OVERLAY,      //Mouseover...
        OVERLAY_TEXT, //Name plates...

        NONE
    }
}
