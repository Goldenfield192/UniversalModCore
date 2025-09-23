package cam72cam.mod.model.obj;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.Buffers.*;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class OBJParser {
    public static final float UNSPECIFIED = Float.MIN_VALUE;

    // 3 floats per vertex (x, y, z)
    private final FloatBuffer vertices = new FloatBuffer(1024);
    // 3 floats per normal (x, y, z)
    private final FloatBuffer vertexNormals = new FloatBuffer(1024);
    // 2 floats per normal (u, v)
    private final FloatBuffer vertexTextures = new FloatBuffer(1024);
    // 3 ints per vert, 3 verts per face (v1, vt1, vn1, v2, vt2, vn2, v3, vt3, vn3)
    private final IntBuffer faceVerts = new IntBuffer(1024);
    private final List<String> faces = new ArrayList<>();
    // 1 int per face, which face to use (mtlLookup)
    private final List<String> faceMaterials = new ArrayList<>();
    // List of material files to load as part of this obj
    private final List<String> materialLibraries = new ArrayList<>();
    // Group -> Face # ranges
    private final List<OBJGroup> groups = new ArrayList<>();

    private final float[] vbo;
    private final int[] ebo;

    private final float scale;

    private final List<OBJGroup> correctedGroups;
    private VertexBuffer buffer;
    private final ElementBuffer elementBuffer;
    private final String[] correctedFaceMaterials;
    private boolean smoothShading = false;

    private String currentMaterial = null;
    private int currentGroupStart = 0;
    private String currentGroupName = "defaultName";
    private boolean hasNormals = true;

    public OBJParser(InputStream stream, float scale) throws IOException {
        this.scale = scale;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }
                if (line.length() == 0) {
                    continue;
                }
                String[] args = line.split(" ");
                String cmd = args[0];
                switch (cmd) {
                    case "mtllib":
                        addMaterialLibrary(args[1]);
                        break;
                    case "usemtl":
                        if (args.length >= 2) {
                            String mtlName = args[1];
                            for (int i = 2; i < args.length; i++) {
                                mtlName += " " + args[i];
                            }
                            setCurrentMTL(mtlName);
                        } else {
                            setCurrentMTL("undefined");
                        }
                        break;
                    case "o":
                    case "g":
                        String groupName = args[1];
                        for (int i = 2; i < args.length; i++) {
                            groupName += " " + args[i];
                        }
                        addGroup(groupName);
                        break;
                    case "v":
                        addVertex(args[1], args[2], args[3]);
                        break;
                    case "vn":
                        addVertexNormal(args[1], args[2], args[3]);
                        break;
                    case "vt":
                        addVertexTexture(args[1], args[2]);
                        break;
                    case "f":
                        if (args.length == 4) {
                            addFace(args[1], args[2], args[3]);
                        } else if (args.length == 5) {
                            addFace(args[1], args[2], args[3]);
                            addFace(args[3], args[4], args[1]);
                        } else {
                            for (int i = 2; i < args.length - 1; i++) {
                                addFace(args[1], args[i], args[i + 1]);
                            }
                        }
                        break;
                    case "s":
                        if (args.length == 2 && args[1].equals("1")) {
                            // Technically this should be for every group, but this is a close enough approximation
                            this.smoothShading = true;
                        }
                        break;
                    case "l":
                        // Ignore
                        // TODO might be able to use this for details
                        break;
                    default:
                        //System.out.println("OBJ: ignored line '" + line + "'");
                        break;
                }
            }
            addGroup(null); // Finalize last group
        }

        groups.sort(Comparator.comparing(a -> a.name));
        this.correctedGroups = new ArrayList<>();

        float[] vertices = this.vertices.array();
        float[] vertexNormals = this.vertexNormals.array();
        float[] vertexTextures = this.vertexTextures.array();
        int[] faceVerts = this.faceVerts.array();
        this.correctedFaceMaterials = new String[faceMaterials.size()];

        FloatBuffer vbo = new FloatBuffer(1024);
        int currentVert = 0;
        IntBuffer ebo = new IntBuffer(1024);
        Map<String, Integer> deduplicate = new Object2IntOpenHashMap<>();

        this.buffer = new VertexBuffer(faceMaterials.size(), hasNormals);

        int faceCount = 0;

        for (OBJGroup group : groups) {
            int startFace = faceCount;
            List<Vec3d> points = new ArrayList<>();
            // primitive array here only takes up maybe 1-2MB at worst
            boolean[] usedVerts = new boolean[this.vertices.size()/3];
            for (int face = group.faceStart; face <= group.faceStop; face++) {
                correctedFaceMaterials[faceCount] = faceMaterials.get(face);
                for (int point = 0; point < 3; point++) {
                    //Parse vert
                    int faceVertexIndex = face * 3 + point;
                    String pointStr = faces.get(faceVertexIndex);
                    if(!deduplicate.containsKey(pointStr)) {
                        int[] p = parsePoint(pointStr, 0);
                        int vert = p[0] * 3;
                        vbo.add(vertices[vert]);  //x
                        vbo.add(vertices[vert+1]);//y
                        vbo.add(vertices[vert+2]);//z
                        int uv = p[1] * 2;
                        if (uv >= 0) {
                            vbo.add(vertexTextures[uv]);    //u
                            vbo.add(vertexTextures[uv + 1]);//v
                        } else {
                            vbo.add(UNSPECIFIED);
                            vbo.add(UNSPECIFIED);
                        }
                        vbo.add(1);//r
                        vbo.add(1);//g
                        vbo.add(1);//b
                        vbo.add(1);//a
                        if (hasNormals) {
                            int norm = p[2] * 3;
                            vbo.add(vertexNormals[norm]);  //nx
                            vbo.add(vertexNormals[norm+1]);//ny
                            vbo.add(vertexNormals[norm+2]);//nz
                        }
                        deduplicate.put(pointStr, currentVert);
                        currentVert++;
                    }
                    ebo.add(deduplicate.get(pointStr));
                    int[] p = parsePoint(pointStr, 0);
                    int vert = p[0] * 3;
                    points.add(new Vec3d(vertices[vert], vertices[vert+1], vertices[vert+2]));
                }
                faceCount++;
            }

            Vec3d first = points.get(0);
            Vec3d groupMin = points.stream().reduce(first, Vec3d::min);
            Vec3d groupMax = points.stream().reduce(first, Vec3d::max);
            Vec3d center = groupMax.add(groupMin).scale(0.5);

            Vec3d min = first;
            Vec3d max = first;
            // Furthest from center
            for (Vec3d point : points) {
                if (max.distanceToSquared(center) < point.distanceToSquared(center)) {
                    max = point;
                }
            }
            //
            for (Vec3d point : points) {
                if (min.distanceToSquared(max) < point.distanceToSquared(max)) {
                    min = point;
                }
            }
            Vec3d finalMin = min.lengthSquared() < max.lengthSquared() ? min : max;
            Vec3d finalMax = min.lengthSquared() < max.lengthSquared() ? max : min;
            List<Vec3d> minG = points.stream().filter(p -> p.distanceToSquared(finalMin) < p.distanceToSquared(finalMax)).collect(Collectors.toList());
            List<Vec3d> maxG = points.stream().filter(p -> p.distanceToSquared(finalMin) > p.distanceToSquared(finalMax)).collect(Collectors.toList());
            Vec3d minN = minG.stream().reduce(Vec3d.ZERO, Vec3d::add).scale(1. / minG.size());
            Vec3d maxN = maxG.stream().reduce(Vec3d.ZERO, Vec3d::add).scale(1. / maxG.size());
            Vec3d normal = maxN.subtract(minN).normalize();

            correctedGroups.add(new OBJGroup(group.name, startFace, faceCount-1, groupMin, groupMax, normal));
        }
        this.vbo = vbo.array();
        this.ebo = ebo.array();
        this.buffer = new VertexBuffer(this.vbo, hasNormals);
        this.elementBuffer = new ElementBuffer(this.buffer, this.ebo);
    }
    public VertexBuffer getVertexBuffer() {
        return buffer;
    }
    public ElementBuffer getElementBuffer() {
        return elementBuffer;
    }
    public List<OBJGroup> getGroups() {
        return correctedGroups;
    }
    public List<String> getMaterialLibraries() {
        return materialLibraries;
    }
    public String[] getFaceMaterials() {
        return correctedFaceMaterials;
    }
    public boolean isSmoothShading() {
        return smoothShading;
    }

    private void addMaterialLibrary(String lib) {
        materialLibraries.add(lib);
    }

    private void setCurrentMTL(String name) {
        currentMaterial = name.intern();
    }

    private void addGroup(String name) {
        if (currentGroupStart != faceMaterials.size()) {
            groups.add(new OBJGroup(currentGroupName, currentGroupStart, faceMaterials.size() - 1, null, null, null));
        }
        currentGroupName = name;
        currentGroupStart = faceMaterials.size();
    }

    private void addVertex(String x, String y, String z) {
        vertices.add(Float.parseFloat(x) * scale);
        vertices.add(Float.parseFloat(y) * scale);
        vertices.add(Float.parseFloat(z) * scale);
    }
    private void addVertexTexture(String u, String v) {
        vertexTextures.add(Float.parseFloat(u));
        vertexTextures.add(Float.parseFloat(v));
    }
    private void addVertexNormal(String x, String y, String z) {
        vertexNormals.add(Float.parseFloat(x));
        vertexNormals.add(Float.parseFloat(y));
        vertexNormals.add(Float.parseFloat(z));
    }

    private void addFace(String a, String b, String c) {
        parsePoint(a);
        parsePoint(b);
        parsePoint(c);
        faces.add(a);
        faces.add(b);
        faces.add(c);
        faceMaterials.add(currentMaterial);
    }

    private int[] parsePoint(String point, int i) {
        String[] sp = point.split("/");
        int[] res = new int[3];
        for (int i = 0; i < 3; i++) {
            if (i < sp.length && !sp[i].equals("")) {
                res[i] = (Integer.parseInt(sp[i]) - 1);
            } else {
                res[i] = (-1);
                if (i == 2) {
                    //VN
                    this.hasNormals = false;
                }
            }
        }
        return res;
    }

    private void parsePoint(String point) {
        String[] sp = point.split("/");
        for (int i = 0; i < 3; i++) {
            if (i < sp.length && !sp[i].equals("")) {
                faceVerts.add(Integer.parseInt(sp[i]) - 1);
            } else {
                faceVerts.add(-1);
                if (i == 2) {
                    //VN
                    this.hasNormals = false;
                }
            }
        }
    }
}
