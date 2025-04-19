package cam72cam.mod.model.common;

public class Face {
    //Only triangles
    public Vertex[] vertices = new Vertex[3];

    public String materialName;

    public Face(Vertex v1, Vertex v2, Vertex v3, String materialName){
        this.vertices[0] = v1;
        this.vertices[1] = v2;
        this.vertices[2] = v3;
        this.materialName = materialName;
    }
}
