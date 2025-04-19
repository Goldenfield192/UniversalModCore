package cam72cam.mod.model.common;

import cam72cam.mod.model.obj.Buffers;
import cam72cam.mod.model.obj.VertexBuffer;
import org.lwjgl.BufferUtils;

import java.util.ArrayList;
import java.util.HashMap;

public class ElementBufferGenerator {
    public static void genEBO(VertexBuffer buffer){
        float[] data = buffer.data;
        ArrayList<Vertex> origVertex = new ArrayList<>();
        if(buffer.hasNormals){
            for (int i = 0; i < buffer.data.length; i += 12) {
                origVertex.add(new Vertex(data[i], data[i+1], data[i+2])
                                       .uv(data[i+3], data[i+4])
                                       .color(data[i+5], data[i+6], data[i+7], data[i+8])
                                       .normal(data[i+9], data[i+10], data[i+11]));
            }
        } else {
            for (int i = 0; i < buffer.data.length; i += 9) {
                origVertex.add(new Vertex(data[i], data[i+1], data[i+2])
                                       .uv(data[i+3], data[i+4])
                                       .color(data[i+5], data[i+6], data[i+7], data[i+8]));
            }
        }
        Buffers.FloatBuffer vbo = new Buffers.FloatBuffer(1024);
        Buffers.IntBuffer ebo = new Buffers.IntBuffer(buffer.data.length / buffer.stride);
        HashMap<String, Integer> vertices = new HashMap<>();

        int i = 0;
        for (Vertex vertex : origVertex){
            String key = vertex.toString();
            if(!vertices.containsKey(key)) {
                vertex.writeToFloatBuffer(vbo, buffer.hasNormals);
                vertices.put(vertex.toString(), i);
                i++;
            }
            ebo.add(vertices.get(key));
        }

        buffer.vbo = vbo.array();
        buffer.ebo = ebo.array();
        buffer.hasEbo = true;
    }
}
