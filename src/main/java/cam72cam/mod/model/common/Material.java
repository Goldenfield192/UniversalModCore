package cam72cam.mod.model.common;

import cam72cam.mod.render.obj.OBJTextureSheet;
import cam72cam.mod.render.opengl.Texture;
import cam72cam.mod.resource.Identifier;

public class Material {
    public final String name;

    public final Texture colorMap;
    public final Texture normalMap;
    public final Texture specularMap;

    public final byte KdR;
    public final byte KdG;
    public final byte KdB;
    public final byte KdA;
    public boolean used;

    public int copiesU;
    public int copiesV;

    public Material(String name, Identifier colorMap) {
        this(name, colorMap, getSpecular(colorMap), getNormal(colorMap), 0xFFFFFFFF);
    }

    public Material(String name, Identifier colorMap, Identifier specular, Identifier normal) {
        this(name, colorMap, specular, normal, 0xFFFFFFFF);
    }

    public Material(String name, Identifier colorMap, Identifier specular, Identifier normal, int color) {
        this.name = name;
        this.colorMap = Texture.wrap(colorMap);
        this.normalMap = Texture.wrap(normal);
        this.specularMap = Texture.wrap(specular);
        //color : 0xAARRGGBB
        this.KdA = (byte) (color >> 24 & 255);
        this.KdR = (byte) (color >> 16 & 255);
        this.KdG = (byte) (color >> 8 & 255);
        this.KdB = (byte) (color & 255);
        copiesU = 1;
        copiesV = 1;
        used = false;
    }

    public boolean hasTexture() {
        return colorMap != null;
    }

    private static Identifier getSpecular(Identifier color) {
        String[] fileName = color.getPath().split("/")[color.getPath().split("/").length - 1].split("\\.");
        return color.getRelative(fileName[0] + "_s." + fileName[1]);
    }

    private static Identifier getNormal(Identifier color) {
        String[] fileName = color.getPath().split("/")[color.getPath().split("/").length - 1].split("\\.");
        return color.getRelative(fileName[0] + "_n." + fileName[1]);
    }
}
