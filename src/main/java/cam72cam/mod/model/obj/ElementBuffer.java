package cam72cam.mod.model.obj;

public class ElementBuffer {
    public final VertexBuffer vbo;
    public final int[] data;

    public ElementBuffer(VertexBuffer vbo, int[] data) {
        this.vbo = vbo;
        this.data = data;
    }
}
