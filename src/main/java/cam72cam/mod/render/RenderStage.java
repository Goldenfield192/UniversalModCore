package cam72cam.mod.render;

public class RenderStage {
    public static Stage stage = Stage.NONE;

//    public static void setEntityStage() {
//        RenderSystem.assertThread(RenderSystem::isOnRenderThreadOrInit);
//        RenderStage.stage = Stage.ENTITY;
//    }
//
//    public static Stage clearStage() {
//        RenderSystem.assertThread(RenderSystem::isOnRenderThread);
//        RenderStage.stage = Stage.NONE;
//    }

    public enum Stage{
        BLOCK,
        ITEM,
        ENTITY,
        GUI,
        NONE
    }
}
