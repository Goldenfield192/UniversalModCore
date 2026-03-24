package cam72cam.mod.render;

import cam72cam.mod.ModCore;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import util.Matrix4;

import javax.vecmath.Vector3f;
import java.util.*;

/**
 * Internal class to scale an existing Baked Model
 *
 * Do not use directly
 */
class BakedScaledModel implements IBakedModel {
    // I know this is evil and I love it :D

    private final Matrix4 transform;
    private final IBakedModel source;
    private final Map<EnumFacing, List<BakedQuad>> quadCache = new HashMap<>();

    public BakedScaledModel(IBakedModel source, Matrix4 transform) {
        this.source = source;
        this.transform = transform;
    }

    public BakedScaledModel(IBakedModel source, float height) {
        this.source = source;
        transform = new Matrix4().scale(1, height, 1);
    }

    /**
     * Hack source model and adjust vertex to fit topFacing
     * */
    public BakedScaledModel(IBakedModel source, float height, Vec3i basePos, Vec3d topFacing) {
        this.source = source;
        this.transform = new Matrix4().translate(basePos.x, basePos.y, basePos.z).scale(1, height, 1);

        if (topFacing == null) return;

        float[][] topVertexes = {{0,height,0},{1,height,0},{0,height,1},{1,height,1}};
        float minTopY = Float.MAX_VALUE;
        float BottomY = 0;

        double centerX = 0.5, centerZ = 0.5;
        double dx = topFacing.x, dy = topFacing.y, dz = topFacing.z;
        if (Math.abs(dy) < 1e-5) { // flat
            return;
        }

        double d0 = dx * centerX + dy * height + dz * centerZ;

        for (float[] v : topVertexes) {
            double y = (d0 - dx * v[0] - dz * v[2]) / dy;
            if (y < minTopY) minTopY = (float) y;
        }

        BottomY = Math.min(minTopY, BottomY);

        quadCache.clear();
        for (EnumFacing side : EnumFacing.values()) {
            List<BakedQuad> sideQuads = source.getQuads(null, side, 0);
            if (sideQuads.isEmpty()) continue;

            List<BakedQuad> transformed = new ArrayList<>();
            for (BakedQuad quad : sideQuads) {
                VertexFormat format = quad.getFormat();
                int[] originalData = quad.getVertexData();
                int[] newData = Arrays.copyOf(originalData, originalData.length);

                for (int i = 0; i < 4; i++) {
                    int offset = format.getIntegerSize() * i;

                    float origX = Float.intBitsToFloat(originalData[offset]);
                    float origY = Float.intBitsToFloat(originalData[offset + 1]);
                    float origZ = Float.intBitsToFloat(originalData[offset + 2]);

                    float finalY = origY;
                    if (Math.abs(origY - 1.0f) < 1e-5) {//top
                        double newY = (d0 - dx * origX - dz * origZ) / dy;
                        finalY = (float) newY;
                    } else if (Math.abs(origY) < 1e-5) {//bottom
                        finalY = BottomY;
                    } else {//unexpected
                        ModCore.error("invalid IBakedModel:" + source);
                    }

                    newData[offset] = Float.floatToRawIntBits(basePos.x + origX);
                    newData[offset + 1] = Float.floatToRawIntBits(basePos.y + finalY);
                    newData[offset + 2] = Float.floatToRawIntBits(basePos.z + origZ);
                }

                transformed.add(new BakedQuad(newData, quad.getTintIndex(), quad.getFace(),
                        quad.getSprite(), quad.shouldApplyDiffuseLighting(), format));
            }
            quadCache.put(side, transformed);
        }
    }

    private int[] transformVertexData(int[] originalData, VertexFormat format, Matrix4 transform) {
        int[] newData = Arrays.copyOf(originalData, originalData.length);
        for (int i = 0; i < 4; i++) {
            int offset = format.getIntegerSize() * i;
            Vector3f vec = new Vector3f(
                    Float.intBitsToFloat(newData[offset]),
                    Float.intBitsToFloat(newData[offset + 1]),
                    Float.intBitsToFloat(newData[offset + 2])
            );
            transform.apply(vec);
            newData[offset] = Float.floatToRawIntBits(vec.x);
            newData[offset + 1] = Float.floatToRawIntBits(vec.y);
            newData[offset + 2] = Float.floatToRawIntBits(vec.z);
        }
        return newData;
    }

    @Override
    public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
        if (quadCache.get(side) == null) {
            List<BakedQuad> quads = source.getQuads(state, side, rand);
            quadCache.put(side, new ArrayList<>());
            for (BakedQuad quad : quads) {
                int[] newData = transformVertexData(quad.getVertexData(), quad.getFormat(), transform);
                quadCache.get(side).add(new BakedQuad(newData, quad.getTintIndex(), quad.getFace(), quad.getSprite(), quad.shouldApplyDiffuseLighting(), quad.getFormat()));
            }
        }
        return quadCache.get(side);
    }

    @Override
    public boolean isAmbientOcclusion() {
        return source.isAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return source.isGui3d();
    }

    @Override
    public boolean isBuiltInRenderer() {
        return source.isBuiltInRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return source.getParticleTexture();
    }

    @Override
    public ItemOverrideList getOverrides() {
        return source.getOverrides();
    }

}