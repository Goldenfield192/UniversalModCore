package cam72cam.mod.render.opengl;

import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.render.ShaderHelper;
import cam72cam.mod.render.api.RenderCtx;
import cam72cam.mod.util.With;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import util.Matrix4;

import java.nio.FloatBuffer;
import java.util.*;

import static cam72cam.mod.render.opengl.Texture.NO_TEXTURE;

public class GlRenderContext extends RenderCtx {
    private FloatBuffer internalBuffer;

    public GlRenderContext() {
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
            internalBuffer = BufferUtils.createFloatBuffer(16);
        }
    }

    public With apply(RenderState state) {
        List<Runnable> restore = new ArrayList<>();

        if (state.model_view != null || state.projection != null) {
            //int oldMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
            //restore.add(() -> GL11.glMatrixMode(oldMode));
            // Sane default and removes a few % on render overhead
            restore.add(() -> GL11.glMatrixMode(GL11.GL_MODELVIEW));
        }
        if (state.model_view != null) {
            GL11.glPushMatrix();
            mulMatrix(state.model_view.copy().transpose());
            restore.add(GL11::glPopMatrix);
        }
        if (state.projection != null) {
            // Since we use the projection matrix so little, we assume that we are always defaulted to MODELVIEW
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPushMatrix();
            mulMatrix(state.projection.copy().transpose());
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            restore.add(() -> {
                GL11.glMatrixMode(GL11.GL_PROJECTION);
                GL11.glPopMatrix();
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
            });
        }

        if (state.texture != null || state.lightmap != null || state.normals != null || state.specular != null) {
            int oldActive = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            restore.add(() -> GL13.glActiveTexture(oldActive));
        }

        if (state.shader != null) {
            With bound = state.shader.bind();
            restore.add(bound::restore);
        }

        boolean shaderActive = ShaderHelper.isShaderPackEnabled();

        if (state.lightmap != null) {
            float block = state.lightmap[0];
            float sky = state.lightmap[1];
            boolean vanillaEmissive = block == 1 && sky == 1 && !shaderActive;
            if (vanillaEmissive) {
                state.lighting(false);
                GL13.glActiveTexture(OpenGlHelper.lightmapTexUnit);
                boolean oldTexEnabled = GL11.glGetBoolean(GL11.GL_TEXTURE_2D);
                applyBool(GL11.GL_TEXTURE_2D, false);
                restore.add(() -> {
                    GL13.glActiveTexture(OpenGlHelper.lightmapTexUnit);
                    applyBool(GL11.GL_TEXTURE_2D, oldTexEnabled);
                });
            } else {
                int i = ((int)(sky * 15)) << 20 | ((int)(block*15)) << 4;
                int x = i % 65536;
                int y = i / 65536;
                float oldX = OpenGlHelper.lastBrightnessX;
                float oldY = OpenGlHelper.lastBrightnessY;
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, x, y);
                restore.add(() -> OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, oldX, oldY));
            }
        }

        if (state.texture != null) {
            GL13.glActiveTexture(OpenGlHelper.defaultTexUnit);
            boolean oldTexEnabled = GL11.glGetBoolean(GL11.GL_TEXTURE_2D);

            if (state.texture == NO_TEXTURE) {
                applyBool(GL11.GL_TEXTURE_2D, false);
                restore.add(() -> {
                    GL13.glActiveTexture(OpenGlHelper.defaultTexUnit);
                    applyBool(GL11.GL_TEXTURE_2D, oldTexEnabled);
                });
            } else {
                applyBool(GL11.GL_TEXTURE_2D, true);

                int oldTex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, state.texture.getId());
                restore.add(() -> {
                    GL13.glActiveTexture(OpenGlHelper.defaultTexUnit);
                    applyBool(GL11.GL_TEXTURE_2D, oldTexEnabled);
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, oldTex);
                });
            }
        }

        if (shaderActive) {

            if (state.normals != null) {
                // Normals
                GL13.glActiveTexture(GL13.GL_TEXTURE2);
                boolean oldNormalEnabled = GL11.glGetBoolean(GL11.GL_TEXTURE_2D);

                if (state.normals == NO_TEXTURE) {
                    applyBool(GL11.GL_TEXTURE_2D, false);
                    restore.add(() -> {
                        GL13.glActiveTexture(GL13.GL_TEXTURE2);
                        applyBool(GL11.GL_TEXTURE_2D, oldNormalEnabled);
                    });
                } else {
                    applyBool(GL11.GL_TEXTURE_2D, true);

                    int oldNorm = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, state.normals.getId());
                    restore.add(() -> {
                        GL13.glActiveTexture(GL13.GL_TEXTURE2);
                        applyBool(GL11.GL_TEXTURE_2D, oldNormalEnabled);
                        GL11.glBindTexture(GL11.GL_TEXTURE_2D, oldNorm);
                    });
                }
                GL13.glActiveTexture(GL13.GL_TEXTURE0);
            }
            if (state.specular != null) {
                // Specular
                GL13.glActiveTexture(GL13.GL_TEXTURE3);
                boolean oldSpecularEnalbed = GL11.glGetBoolean(GL11.GL_TEXTURE_2D);

                if (state.specular == NO_TEXTURE) {
                    applyBool(GL11.GL_TEXTURE_2D, false);
                    restore.add(() -> {
                        GL13.glActiveTexture(GL13.GL_TEXTURE3);
                        applyBool(GL11.GL_TEXTURE_2D, oldSpecularEnalbed);
                    });
                } else {
                    applyBool(GL11.GL_TEXTURE_2D, true);

                    int oldSpec = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, state.specular.getId());
                    restore.add(() -> {
                        GL13.glActiveTexture(GL13.GL_TEXTURE3);
                        applyBool(GL11.GL_TEXTURE_2D, oldSpecularEnalbed);
                        GL11.glBindTexture(GL11.GL_TEXTURE_2D, oldSpec);
                    });
                }
                GL13.glActiveTexture(GL13.GL_TEXTURE0);

            }
        }


        if (state.color != null) {
            boolean oldColorMaterial = GL11.glGetBoolean(GL11.GL_COLOR_MATERIAL);
            applyBool(GL11.GL_COLOR_MATERIAL, true);
            GL11.glGetFloat(GL11.GL_CURRENT_COLOR, internalBuffer);
            float[] oldColor = new float[] {internalBuffer.get(0), internalBuffer.get(1), internalBuffer.get(2), internalBuffer.get(3)};
            GL11.glColor4f(state.color[0], state.color[1], state.color[2], state.color[3]);
            restore.add(() -> {
                GL11.glColor4f(oldColor[0], oldColor[1], oldColor[2], oldColor[3]);
                applyBool(GL11.GL_COLOR_MATERIAL, oldColorMaterial);
            });
        }

        if (state.lighting != null) {
            boolean oldValue = GL11.glGetBoolean(GL11.GL_LIGHTING);
            applyBool(GL11.GL_LIGHTING, state.lighting);
            restore.add(() -> applyBool(GL11.GL_LIGHTING, oldValue));
        }

        if (state.alpha_test != null) {
            boolean oldValue = GL11.glGetBoolean(GL11.GL_ALPHA_TEST);
            applyBool(GL11.GL_ALPHA_TEST, state.alpha_test);
            restore.add(() -> applyBool(GL11.GL_ALPHA_TEST, oldValue));
        }

        if (state.depth_test != null) {
            boolean oldValue = GL11.glGetBoolean(GL11.GL_DEPTH_TEST);
            applyBool(GL11.GL_DEPTH_TEST, state.depth_test);
            restore.add(() -> applyBool(GL11.GL_DEPTH_TEST, oldValue));
        }

        if (state.rescale_normal != null) {
            boolean oldValue = GL11.glGetBoolean(GL12.GL_RESCALE_NORMAL);
            applyBool(GL12.GL_RESCALE_NORMAL, state.rescale_normal);
            restore.add(() -> applyBool(GL12.GL_RESCALE_NORMAL, oldValue));
        }

        if (state.cull_face != null) {
            boolean oldValue = GL11.glGetBoolean(GL11.GL_CULL_FACE);
            applyBool(GL11.GL_CULL_FACE, state.cull_face);
            restore.add(() -> applyBool(GL11.GL_CULL_FACE, oldValue));
        }

        if (state.depth_mask != null) {
            boolean oldDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            GL11.glDepthMask(state.depth_mask);
            restore.add(() -> GL11.glDepthMask(oldDepthMask));
        }

        if (state.smooth_shading != null) {
            int oldShading = GL11.glGetInteger(GL11.GL_SHADE_MODEL);
            GL11.glShadeModel(state.smooth_shading ? GL11.GL_SMOOTH : GL11.GL_FLAT);
            restore.add(() -> GL11.glShadeModel(oldShading));
        }

        if (state.scissor_test != null) {
            boolean oldValue = GL11.glGetBoolean(GL11.GL_SCISSOR_TEST);
            applyBool(GL11.GL_SCISSOR_TEST, state.scissor_test);
            if (state.scissor_test && state.scissor_range != null) {
                int scaleFactor = new ScaledResolution(Minecraft.getMinecraft()).getScaleFactor();
                int screenHeight = GUIHelpers.getScreenHeight() * scaleFactor;

                int x = (int) state.scissor_range.getMinX() * scaleFactor;
                int y = (int) state.scissor_range.getMinY() * scaleFactor;
                int width = (int) state.scissor_range.getWidth() * scaleFactor;
                int height = (int) state.scissor_range.getHeight() * scaleFactor;

                //We set origin point at Top-Left corner but OpenGL takes Bottom-Left corner, so wraps y
                GL11.glScissor(x, screenHeight - y - height, width, height);
            }
            restore.add(() -> applyBool(GL11.GL_SCISSOR_TEST, oldValue));
        }

        if (state.blend != null) {
            restore.add(state.blend.apply());
        }

        Collections.reverse(restore);
        return () -> restore.forEach(Runnable::run);
    }

    private void mulMatrix(Matrix4 matrix) {
        internalBuffer.position(0);
        internalBuffer.put(new float[]{
                (float) matrix.m00, (float) matrix.m01, (float) matrix.m02, (float) matrix.m03,
                (float) matrix.m10, (float) matrix.m11, (float) matrix.m12, (float) matrix.m13,
                (float) matrix.m20, (float) matrix.m21, (float) matrix.m22, (float) matrix.m23,
                (float) matrix.m30, (float) matrix.m31, (float) matrix.m32, (float) matrix.m33
        });
        internalBuffer.flip();
        GL11.glMultMatrix(internalBuffer);
    }

    static void applyBool(int glOptCode, boolean state) {
        if (state) {
            GL11.glEnable(glOptCode);
        } else {
            GL11.glDisable(glOptCode);
        }
    }
}
