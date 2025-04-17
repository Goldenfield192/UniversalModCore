package cam72cam.mod.model.obj;

import org.apache.commons.lang3.ArrayUtils;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;

public class ElementBuffer {
    public List<Vertex> origVertex;

    public Buffers.FloatBuffer vbo;
    public Buffers.IntBuffer ebo;
    public HashMap<String, Vertex> verts;
    public int currentVertexCount;

    public final int vertsPerFace;
    public final int vertexOffset;
    public final int textureOffset;
    public final int colorOffset;
    public final int normalOffset;
    public final boolean hasNormals;
    public final int stride;

    private boolean built = false;

    private ElementBuffer(float[] vbo, int[] ebo, int faces, boolean hasNormals){
        this.origVertex = new ArrayList<>(2048);

        this.verts = new HashMap<>();
        this.currentVertexCount = 0;
        this.vertexOffset = 0;
        this.vertsPerFace = 3;
        this.textureOffset = vertexOffset + 3;
        this.colorOffset = textureOffset + 2;
        this.normalOffset = colorOffset + 4;
        this.hasNormals = hasNormals;
        this.stride = hasNormals ? normalOffset + 3 : colorOffset + 4;

        if(vbo != null && ebo != null){
            this.vbo = new Buffers.FloatBuffer(vbo.length);
            for (float f : vbo){
                this.vbo.add(f);
            }
            this.ebo = new Buffers.IntBuffer(ebo.length);
            for (int i : ebo){
                this.ebo.add(i);
            }
            this.built = true;
        }
    }

    public ElementBuffer(int faces, boolean hasNormals) {
        this(null, null, faces, hasNormals);
    }

    public ElementBuffer(float[] vbo, int[] ebo, boolean hasNormals){
        this(vbo, ebo, 0, hasNormals);
    }

    public void genData(){
        this.ebo = new Buffers.IntBuffer(1024);
        this.vbo = new Buffers.FloatBuffer(1024);

        for(Vertex vertex : origVertex){
            if(verts.containsKey(vertex.identity)){
                ebo.add(verts.get(vertex.identity).index);
            } else {
                vertex.index = currentVertexCount;
                currentVertexCount++;

                vbo.add(vertex.vx);
                vbo.add(vertex.vy);
                vbo.add(vertex.vz);
                vbo.add(vertex.u);
                vbo.add(vertex.v);
                vbo.add(vertex.r);
                vbo.add(vertex.g);
                vbo.add(vertex.b);
                vbo.add(vertex.a);
                if(hasNormals){
                    vbo.add(vertex.nx);
                    vbo.add(vertex.ny);
                    vbo.add(vertex.nz);
                }

                verts.put(vertex.identity, vertex);
                ebo.add(vertex.index);
            }
        }

        this.built = true;
    }

    public FloatBuffer getVBO(){
        if(!built){
            this.genData();
        }
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vbo.size());
        buffer.put(vbo.array());
        buffer.position(0);
        return buffer;
    }

    public IntBuffer getEBO(){
        if(!built){
            this.genData();
        }
        IntBuffer buffer = BufferUtils.createIntBuffer(ebo.size());
        buffer.put(ebo.array());
        buffer.position(0);
        return buffer;
    }

    public static class Vertex{
        public String identity;
        public int index;

        public float vx;
        public float vy;
        public float vz;

        public float u;
        public float v;

        public float r;
        public float g;
        public float b;
        public float a;

        public float nx;
        public float ny;
        public float nz;

        public Vertex(float vx, float vy, float vz) {
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.identity = Arrays.toString(new float[]{this.vx, this.vy, this.vz, this.u, this.v, this.r, this.g, this.b, this.a, this.nx, this.ny, this.nz});
        }

        public Vertex uv(float u, float v){
            this.u = u;
            this.v = v;
            this.identity = Arrays.toString(new float[]{this.vx, this.vy, this.vz, this.u, this.v, this.r, this.g, this.b, this.a, this.nx, this.ny, this.nz});
            return this;
        }

        public Vertex color(float r, float g, float b, float a) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.identity = Arrays.toString(new float[]{this.vx, this.vy, this.vz, this.u, this.v, this.r, this.g, this.b, this.a, this.nx, this.ny, this.nz});
            return this;
        }

        public Vertex normal(float nx, float ny, float nz){
            this.nx = nx;
            this.ny = ny;
            this.nz = nz;
            this.identity = Arrays.toString(new float[]{this.vx, this.vy, this.vz, this.u, this.v, this.r, this.g, this.b, this.a, this.nx, this.ny, this.nz});
            return this;
        }

        public void setInBuffer(ElementBuffer buffer){
            if(!buffer.hasNormals && (this.nx != 0 || this.ny != 0 || this.nz != 0)){
                throw new RuntimeException();
            }
            buffer.origVertex.add(this);
        }
    }
}
