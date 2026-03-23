package cam72cam.mod.render;

import cam72cam.mod.math.Vec3d;
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

    public BakedScaledModel(IBakedModel source, Matrix4 transform, Vec3d topFacing) {
        this.source = source;
        this.transform = transform;

        if (topFacing == null) return;

        this.quadCache.clear();

        // apply transform, calculate boundary
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        Map<EnumFacing, List<BakedQuad>> tempCache = new HashMap<>();

        for (EnumFacing side : EnumFacing.values()) {
            List<BakedQuad> sideQuads = source.getQuads(null, side, 0);
            if (sideQuads.isEmpty()) continue;

            List<BakedQuad> transformed = new ArrayList<>();
            for (BakedQuad quad : sideQuads) {
                int[] data = Arrays.copyOf(quad.getVertexData(), quad.getVertexData().length);
                VertexFormat format = quad.getFormat();

                for (int i = 0; i < 4; i++) {
                    int offset = format.getIntegerSize() * i;
                    Vector3f vec = new Vector3f(
                            Float.intBitsToFloat(data[offset]),
                            Float.intBitsToFloat(data[offset + 1]),
                            Float.intBitsToFloat(data[offset + 2])
                    );
                    transform.apply(vec);
                    data[offset] = Float.floatToRawIntBits(vec.x);
                    data[offset + 1] = Float.floatToRawIntBits(vec.y);
                    data[offset + 2] = Float.floatToRawIntBits(vec.z);
                    if (vec.y < minY) minY = vec.y;
                    if (vec.y > maxY) maxY = vec.y;
                    if (vec.x < minX) minX = vec.x;
                    if (vec.x > maxX) maxX = vec.x;
                    if (vec.z < minZ) minZ = vec.z;
                    if (vec.z > maxZ) maxZ = vec.z;
                }

                transformed.add(new BakedQuad(data, quad.getTintIndex(), quad.getFace(),
                        quad.getSprite(), quad.shouldApplyDiffuseLighting(), format));
            }
            tempCache.put(side, transformed);
        }

        if (maxY == -Float.MAX_VALUE) {
            quadCache.putAll(tempCache);
            return;
        }

        // top face center
        float centerX = (minX + maxX) / 2f;
        float centerZ = (minZ + maxZ) / 2f;

        // flat
        double dx = topFacing.x, dy = topFacing.y, dz = topFacing.z;
        if (Math.abs(dy) < 1e-5) {
            quadCache.putAll(tempCache);
            return;
        }
        double d0 = dx * centerX + dy * maxY + dz * centerZ;

        // make top ace tilted, find the lowest point
        float minTopY = Float.MAX_VALUE;
        for (List<BakedQuad> quads : tempCache.values()) {
            for (BakedQuad quad : quads) {
                VertexFormat format = quad.getFormat();
                int[] data = quad.getVertexData();
                for (int i = 0; i < 4; i++) {
                    int offset = format.getIntegerSize() * i;
                    float y = Float.intBitsToFloat(data[offset + 1]);
                    if (Math.abs(y - maxY) < 1e-5) {
                        float x = Float.intBitsToFloat(data[offset]);
                        float z = Float.intBitsToFloat(data[offset + 2]);
                        double newY = (d0 - dx * x - dz * z) / dy;
                        data[offset + 1] = Float.floatToRawIntBits((float) newY);
                        if ((float) newY < minTopY) minTopY = (float) newY;
                    }
                }
            }
        }

        // judge bottom if needed
        if (minTopY < minY) {
            for (List<BakedQuad> quads : tempCache.values()) {
                for (BakedQuad quad : quads) {
                    VertexFormat format = quad.getFormat();
                    int[] data = quad.getVertexData();
                    for (int i = 0; i < 4; i++) {
                        int offset = format.getIntegerSize() * i;
                        float y = Float.intBitsToFloat(data[offset + 1]);
                        if (Math.abs(y - minY) < 1e-5) {
                            data[offset + 1] = Float.floatToRawIntBits(minTopY);
                        }
                    }
                }
            }
        }
        //TODO: top face shading

        quadCache.putAll(tempCache);
    }

    @Override
    public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
        if (quadCache.get(side) == null) {
            List<BakedQuad> quads = source.getQuads(state, side, rand);
            quadCache.put(side, new ArrayList<>());
            for (BakedQuad quad : quads) {
                int[] newData = Arrays.copyOf(quad.getVertexData(), quad.getVertexData().length);

                VertexFormat format = quad.getFormat();

                for (int i = 0; i < 4; ++i) {
                    int j = format.getIntegerSize() * i;
                    Vector3f vec = new Vector3f(
                            Float.intBitsToFloat(newData[j + 0]),
                            Float.intBitsToFloat(newData[j + 1]),
                            Float.intBitsToFloat(newData[j + 2])
                    );
                    transform.apply(vec);

                    newData[j + 0] = Float.floatToRawIntBits(vec.x);
                    newData[j + 1] = Float.floatToRawIntBits(vec.y);
                    newData[j + 2] = Float.floatToRawIntBits(vec.z);
                }

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