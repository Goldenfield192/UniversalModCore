package cam72cam.mod.model.common;

public class Material {
    public final String name;

    public final String texture;
    public final String normal;
    public final String specular;

    public final float colorR;
    public final float colorG;
    public final float colorB;
    public final float colorA;
    public boolean used;

    public int copiesU;
    public int copiesV;

    public Material(String name, String texKd, String texBump, String texNs, Float kdR, Float kdG, Float kdB, Float kdA) {
        this.name = name;
        this.texture = texKd;
        this.normal = texBump;
        this.specular = texNs;
        colorR = kdR == null ? 1 : kdR;
        colorG = kdG == null ? 1 : kdG;
        colorB = kdB == null ? 1 : kdB;
        colorA = kdA == null ? 1 : kdA;
        copiesU = 1;
        copiesV = 1;
        used = false;
    }

    public boolean hasTexture() {
        return texture != null;
    }
}
