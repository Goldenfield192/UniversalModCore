package cam72cam.mod.model.common;

public class Face {
    //Only triangles
//    public Vertex[] vertices = new Vertex[3];
    public int[] indices = new int[3];

    public String materialName;

    public Face(int v1, int v2, int v3, String materialName){
        this.indices[0] = v1;
        this.indices[1] = v2;
        this.indices[2] = v3;
        this.materialName = materialName;
    }

//    public Face(Vertex v1, Vertex v2, Vertex v3, String materialName){
//        this.vertices[0] = v1;
//        this.vertices[1] = v2;
//        this.vertices[2] = v3;
//        this.materialName = materialName;
//    }
}
