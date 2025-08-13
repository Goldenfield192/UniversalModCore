package cam72cam.mod.render;

import cam72cam.mod.render.opengl.RenderContext;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.Texture;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.util.With;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A custom sprite sheet which can span multiple texture sheets */
public class SpriteSheet {
    public final int spriteSize;
    private final Map<Identifier, SpriteInfo> sprites = new HashMap<>();
    private final List<SpriteInfo> unallocated = new ArrayList<>();
    /** sprite width/height in px */
    public SpriteSheet(int spriteSize) {
        this.spriteSize = spriteSize;
    }

    /** Create new blank sheet and add slots to unallocated */
    private void allocateSheet(Identifier id) {
        int sheetSize = Math.min(1024, GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE));
        BufferedImage image = new BufferedImage(sheetSize, sheetSize, BufferedImage.TYPE_INT_ARGB);
        DynamicTexture texture = new DynamicTexture(image);
        Minecraft.getMinecraft().getTextureManager().loadTexture(id.internal, texture);
        try (With ctx = RenderContext.apply(new RenderState().texture(Texture.wrap(id)))) {
            for (int uPx = 0; uPx < sheetSize; uPx += spriteSize) {
                for (int vPx = 0; vPx < sheetSize; vPx += spriteSize) {
                    float u = uPx / (float) sheetSize;
                    float uMax = (uPx + spriteSize) / (float) sheetSize;
                    float v = vPx / (float) sheetSize;
                    float vMax = (vPx + spriteSize) / (float) sheetSize;
                    unallocated.add(new SpriteInfo(u, uMax, uPx, v, vMax, vPx, id));
                }
            }
        }
    }

    /** Allocate a slot in the sheet and write pixels to it */
    public void setSprite(Identifier id, ByteBuffer pixels) {
        if (!sprites.containsKey(id)) {
            if (unallocated.size() == 0) {
                allocateSheet(id);
            }
            sprites.put(id, unallocated.remove(0));
        }
        SpriteInfo sprite = sprites.get(id);

        try (With ctx = RenderContext.apply(new RenderState().texture(Texture.wrap(sprite.texID)))) {
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, sprite.uPx, sprite.vPx, spriteSize, spriteSize, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, pixels);
        }
    }

    /** Render the sprite represented by id (skip if unknown) */
    public void renderSprite(Identifier id) {
        SpriteInfo sprite = sprites.get(id);
        if (sprite == null) {
            return;
        }
        RenderState state = new RenderState()
                .texture(Texture.wrap(sprite.texID))
                .rotate(180, 1, 0, 0)
                .translate(0, -1, 0);
        try (With ctx = RenderContext.apply(state)) {
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glColor4f(1, 1, 1, 1);
            GL11.glTexCoord2f(sprite.uMin, sprite.vMin);
            GL11.glVertex3f(0, 0, 0);
            GL11.glTexCoord2f(sprite.uMin, sprite.vMax);
            GL11.glVertex3f(0, 1, 0);
            GL11.glTexCoord2f(sprite.uMax, sprite.vMax);
            GL11.glVertex3f(1, 1, 0);
            GL11.glTexCoord2f(sprite.uMax, sprite.vMin);
            GL11.glVertex3f(1, 0, 0);
            GL11.glEnd();
        };
    }

    /** Remove a sprite from the sheet (does not reduce used GPU memory yet) */
    public void freeSprite(Identifier id) {
        unallocated.add(sprites.remove(id));
        // TODO shrink number of sheets?
    }

    private static class SpriteInfo {
        final float uMin;
        final float uMax;
        final int uPx;
        final float vMin;
        final float vMax;
        final int vPx;
        final Identifier texID;

        private SpriteInfo(float u, float uMax, int uPx, float v, float vMax, int vPx, Identifier texID) {
            this.uMin = u;
            this.uMax = uMax;
            this.uPx = uPx;
            this.vMin = v;
            this.vMax = vMax;
            this.vPx = vPx;
            this.texID = texID;
        }
    }
}
