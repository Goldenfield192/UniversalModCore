package cam72cam.mod.render.opengl;

import cam72cam.mod.ModCore;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.util.With;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;
import util.Matrix4;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static cam72cam.mod.render.opengl.Texture.NO_TEXTURE;

public class RenderContext {
    public static PoseStack STACK;
    public static float partialTick;
    public static ModdedEntity entity;

    private RenderContext() {
    }

    public static With apply(RenderState state) {
        if(getCurrentStage() == RenderStage.ENTITY) {
            return applyEntity(state);
        }
        RenderContext.checkError();
        List<Runnable> restore = new ArrayList<>();

        if (getCurrentStage() == RenderStage.GUI && state.texture != null) {
            float[] oldColor = RenderSystem.getShaderColor();
            RenderSystem.setShaderTexture(0, state.texture.getId());
            if (state.color != null) {
                ShaderInstance oldShader = RenderSystem.getShader();
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.setShaderColor(state.color[0], state.color[1], state.color[2], state.color[3]);
                restore.add(() -> RenderSystem.setShader(() -> oldShader));
            }
            restore.add(() -> RenderSystem.setShaderColor(oldColor[0], oldColor[1], oldColor[2], oldColor[3]));
            return () -> restore.forEach(Runnable::run);
        }

        ShaderInstance shader = RenderSystem.getShader();
        if (state.model_view != null) {
            Matrix4f oldModelView = RenderSystem.getModelViewMatrix().copy();
            restore.add(() -> RenderSystem.getModelViewMatrix().load(oldModelView));
            Matrix4 model_view = state.model_view;
            Matrix4f target = new Matrix4f(new float[]{
                    (float) model_view.m00,
                    (float) model_view.m01,
                    (float) model_view.m02,
                    (float) model_view.m03,
                    (float) model_view.m10,
                    (float) model_view.m11,
                    (float) model_view.m12,
                    (float) model_view.m13,
                    (float) model_view.m20,
                    (float) model_view.m21,
                    (float) model_view.m22,
                    (float) model_view.m23,
                    (float) model_view.m30,
                    (float) model_view.m31,
                    (float) model_view.m32,
                    (float) model_view.m33
            });

            shader.MODEL_VIEW_MATRIX.set(target);

            RenderSystem.getModelViewMatrix().load(target);

        }
        if (state.projection != null) {
            Matrix4f oldProjection = RenderSystem.getProjectionMatrix().copy();
            restore.add(() -> RenderSystem.getProjectionMatrix().load(oldProjection));
            Matrix4 projection = state.projection;
            Matrix4f target = new Matrix4f(new float[]{
                    (float) projection.m00,
                    (float) projection.m01,
                    (float) projection.m02,
                    (float) projection.m03,
                    (float) projection.m10,
                    (float) projection.m11,
                    (float) projection.m12,
                    (float) projection.m13,
                    (float) projection.m20,
                    (float) projection.m21,
                    (float) projection.m22,
                    (float) projection.m23,
                    (float) projection.m30,
                    (float) projection.m31,
                    (float) projection.m32,
                    (float) projection.m33
            });
            shader.PROJECTION_MATRIX.set(target);
            RenderSystem.getProjectionMatrix().load(target);
        }

        if (state.texture != NO_TEXTURE && state.texture != null) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, state.texture.getId());
            shader.setSampler("Sampler0", state.texture.getId());
            // TODO normal and spec
            int oldTexture = RenderSystem.getShaderTexture(0);
            restore.add(() -> RenderSystem.setShaderTexture(0, oldTexture));
            RenderSystem.setShaderTexture(0, state.texture.getId());
        }

        if (state.color != null && shader.COLOR_MODULATOR != null) {
            shader.COLOR_MODULATOR.set(state.color);
            float[] oldColor = RenderSystem.getShaderColor();

            RenderSystem.setShaderColor(state.color[0], state.color[1], state.color[2], state.color[3]);
            restore.add(() -> RenderSystem.setShaderColor(oldColor[0], oldColor[1], oldColor[2], oldColor[3]));
        }
        /* TODO 1.17.1
        state.bools.forEach((glId, value) -> {
            boolean oldValue = GL11.glGetBoolean(glId);
            applyBool(glId, value);
            restore.add(() -> applyBool(glId, oldValue));
        });
        if (state.depth_mask != null) {
            boolean oldDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            GL11.glDepthMask(state.depth_mask);
            restore.add(() -> GL11.glDepthMask(oldDepthMask));
        }

        if (state.smooth_shading != null) {
            int oldShading = GL11.glGetInteger(GL11.GL_SHADE_MODEL);
            GL11.glShadeModel(state.smooth_shading ? GL11.GL_SMOOTH : GL11.GL_FLAT);
            restore.add(() -> GL11.glShadeModel(oldShading));
        }*/

        shader.apply();
        checkError();


        if (state.blend != null) {
            restore.add(() -> state.blend.apply().run());
        }
        return () -> restore.forEach(Runnable::run);
    }

    private static With applyEntity(RenderState state) {
        RenderContext.checkError();
        List<Runnable> restore = new ArrayList<>();
        int currentProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        restore.add(()->GL20.glUseProgram(currentProgram));

        ShaderInstance CURRENT_SHADER_INSTANCE = GameRenderer.getRendertypeEntitySolidShader();
        int entitySolidProgram = CURRENT_SHADER_INSTANCE.getId();
        GL20.glUseProgram(entitySolidProgram);

        if (state.model_view != null) {
            Matrix4f oldModelView = RenderSystem.getModelViewMatrix().copy();
            restore.add(() -> RenderSystem.getModelViewMatrix().load(oldModelView));
            Matrix4 model_view = state.model_view;
            Matrix4f target = new Matrix4f(new float[]{
                    (float) model_view.m00, (float) model_view.m01, (float) model_view.m02, (float) model_view.m03,
                    (float) model_view.m10, (float) model_view.m11, (float) model_view.m12, (float) model_view.m13,
                    (float) model_view.m20, (float) model_view.m21, (float) model_view.m22, (float) model_view.m23,
                    (float) model_view.m30, (float) model_view.m31, (float) model_view.m32, (float) model_view.m33
            });
            CURRENT_SHADER_INSTANCE.MODEL_VIEW_MATRIX.set(target);
            RenderSystem.getModelViewMatrix().load(target);
        }
        if (state.projection != null) {
            Matrix4f oldProjection = RenderSystem.getProjectionMatrix().copy();
            restore.add(() -> RenderSystem.getProjectionMatrix().load(oldProjection));
            Matrix4 projection = state.projection;
            Matrix4f target = new Matrix4f(new float[]{
                    (float) projection.m00, (float) projection.m01, (float) projection.m02, (float) projection.m03,
                    (float) projection.m10, (float) projection.m11, (float) projection.m12, (float) projection.m13,
                    (float) projection.m20, (float) projection.m21, (float) projection.m22, (float) projection.m23,
                    (float) projection.m30, (float) projection.m31, (float) projection.m32, (float) projection.m33
            });
            CURRENT_SHADER_INSTANCE.PROJECTION_MATRIX.set(target);
            RenderSystem.getProjectionMatrix().load(target);
        }

        CURRENT_SHADER_INSTANCE.FOG_START.set(RenderSystem.getShaderFogStart());
        CURRENT_SHADER_INSTANCE.FOG_START.upload();

        CURRENT_SHADER_INSTANCE.FOG_END.set(RenderSystem.getShaderFogEnd());
        CURRENT_SHADER_INSTANCE.FOG_END.upload();

        CURRENT_SHADER_INSTANCE.FOG_COLOR.set(RenderSystem.getShaderFogColor());
        CURRENT_SHADER_INSTANCE.FOG_COLOR.upload();

        if (state.color != null && CURRENT_SHADER_INSTANCE.COLOR_MODULATOR != null) {
            CURRENT_SHADER_INSTANCE.COLOR_MODULATOR.set(state.color);
            float[] oldColor = RenderSystem.getShaderColor();

            RenderSystem.setShaderColor(state.color[0], state.color[1], state.color[2], state.color[3]);
            restore.add(() -> RenderSystem.setShaderColor(oldColor[0], oldColor[1], oldColor[2], oldColor[3]));
        }

//        GL20.glUniform1i(GL20.glGetUniformLocation(entitySolidProgram, "Sampler0"), 0);
//        GL20.glUniform1i(GL20.glGetUniformLocation(entitySolidProgram, "Sampler1"), 1);
//        GL20.glUniform1i(GL20.glGetUniformLocation(entitySolidProgram, "Sampler2"), 2);

        RenderSystem.setupShaderLights(CURRENT_SHADER_INSTANCE);
        Vector3f LIGHT0_DIRECTION = new Vector3f(CURRENT_SHADER_INSTANCE.LIGHT0_DIRECTION.getFloatBuffer().get(0), CURRENT_SHADER_INSTANCE.LIGHT0_DIRECTION.getFloatBuffer().get(1), CURRENT_SHADER_INSTANCE.LIGHT0_DIRECTION.getFloatBuffer().get(2));
        Vector3f LIGHT1_DIRECTION = new Vector3f(CURRENT_SHADER_INSTANCE.LIGHT1_DIRECTION.getFloatBuffer().get(0), CURRENT_SHADER_INSTANCE.LIGHT1_DIRECTION.getFloatBuffer().get(1), CURRENT_SHADER_INSTANCE.LIGHT1_DIRECTION.getFloatBuffer().get(2));

        Vector3f light0Direction = LIGHT0_DIRECTION.copy();
        Vector3f light1Direction = LIGHT1_DIRECTION.copy();
        STACK.pushPose();
        STACK.mulPose(new Quaternion(0.0F, Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot()), 0.0F, true));
//        RenderedGltfModel.CURRENT_POSE = STACK.last().pose();
        Matrix3f currentNormal = STACK.last().normal();
        STACK.popPose();
        light0Direction.transform(currentNormal);
        light1Direction.transform(currentNormal);
        CURRENT_SHADER_INSTANCE.LIGHT0_DIRECTION.set(light0Direction);
        CURRENT_SHADER_INSTANCE.LIGHT1_DIRECTION.set(light1Direction);
//        CURRENT_SHADER_INSTANCE.LIGHT0_DIRECTION.upload();
//        CURRENT_SHADER_INSTANCE.LIGHT1_DIRECTION.upload();

//        if (state.texture != NO_TEXTURE && state.texture != null) {
//            GL11.glBindTexture(GL11.GL_TEXTURE_2D, state.texture.getId());
//            shader.setSampler("Sampler0", state.texture.getId());
//            // TODO normal and spec
//            int oldTexture = RenderSystem.getShaderTexture(0);
//            restore.add(() -> RenderSystem.setShaderTexture(0, oldTexture));
//            RenderSystem.setShaderTexture(0, state.texture.getId());
//        }
        /* TODO 1.17.1
        state.bools.forEach((glId, value) -> {
            boolean oldValue = GL11.glGetBoolean(glId);
            applyBool(glId, value);
            restore.add(() -> applyBool(glId, oldValue));
        });
        if (state.depth_mask != null) {
            boolean oldDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            GL11.glDepthMask(state.depth_mask);
            restore.add(() -> GL11.glDepthMask(oldDepthMask));
        }

        if (state.smooth_shading != null) {
            int oldShading = GL11.glGetInteger(GL11.GL_SHADE_MODEL);
            GL11.glShadeModel(state.smooth_shading ? GL11.GL_SMOOTH : GL11.GL_FLAT);
            restore.add(() -> GL11.glShadeModel(oldShading));
        }*/

//        shader.apply();
        checkError();


//        if (state.blend != null) {
//            restore.add(() -> state.blend.apply().run());
//        }
        CURRENT_SHADER_INSTANCE.apply();
        return () -> restore.forEach(Runnable::run);
    }

    public static void applyBool(int opt, boolean currState) {
        if (currState) {
            GL32.glEnable(opt);
        } else {
            GL32.glDisable(opt);
        }
    }


    public static void checkError() {
        int err = GL32.glGetError();
        if (err != 0) {
            ModCore.error("We broke something: %s", err);
        }
    }
    //Internal
    private static final Deque<RenderStage> stages = new ArrayDeque<>();
    public static int[] defaultLights;

    public static void pushStage(RenderStage stage) {
        stages.push(stage);
    }

    public static void popStage() {
        RenderStage stage = stages.pop();
        if(stage == RenderStage.ENTITY) {
            defaultLights = null;
        }
    }

    public static RenderStage getCurrentStage() {
        return stages.isEmpty() ? RenderStage.NONE : stages.peek();
    }

    //In ENTITY phase
    public static void setDefaultLights(int j, int k) {
        if (getCurrentStage() == RenderStage.ENTITY) {
            defaultLights = new int[]{j, k};
        }
    }

    public enum RenderStage {
        BLOCK,
        ENTITY,
        ITEM,
        GUI,
        OVERLAY,
        NONE
    }
}
