package cam72cam.mod.model.obj;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.common.Face;
import cam72cam.mod.model.common.Vertex;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class OBJParser {
    public static final float UNSPECIFIED = Float.MIN_VALUE;

    // 1 object per vertex (x, y, z)
    private final ArrayList<Vertex.Position> vertexPositions = new ArrayList<>(1024);
    // 1 object per normal (x, y, z)
    private final ArrayList<Vertex.Normal> vertexNormals = new ArrayList<>(1024);
    // 1 object per uv (u, v)
    private final ArrayList<Vertex.UV> vertexTextures = new ArrayList<>(1024);
    // 1 entry per vertex for deduplication
    private final HashMap<String, Integer> vertexMap = new HashMap<>(1024);
    private final ArrayList<Vertex> vertices = new ArrayList<>();
    // 1 object per face (3 vert and 1 mtl name)
    private final ArrayList<Face> faces = new ArrayList<>(1024);
    // List of material files to load as part of this obj
    private final List<String> materialLibraries = new ArrayList<>();
    // Group -> Face # ranges
    private final List<OBJGroup> groups = new ArrayList<>();

    private final float scale;

    private final List<OBJGroup> correctedGroups;
    private final VertexBuffer buffer;
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
                if (line.startsWith("#") || line.isEmpty()) {
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
                            String mtlName = line.substring(6).trim();
                            setCurrentMTL(mtlName);
                        } else {
                            setCurrentMTL("undefined");
                        }
                        break;
                    case "o":
                    case "g":
                        String groupName = line.substring(1).trim();
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

        this.correctedFaceMaterials = new String[faces.size()];

        this.buffer = new VertexBuffer(faces.size(), hasNormals);

        int faceCount = 0;
        int vertexOffset = buffer.vertexOffset;
        int normalOffset = buffer.normalOffset;
        int textureOffset = buffer.textureOffset;

        for (OBJGroup group : groups) {
            int startFace = faceCount;
            List<Vec3d> points = new ArrayList<>();
            // primitive array here only takes up maybe 1-2MB at worst
            boolean[] usedVerts = new boolean[faces.size() * 3];
            for (int faceIndex = group.faceStart; faceIndex <= group.faceStop; faceIndex++) {
                Face face = faces.get(faceIndex);
                correctedFaceMaterials[faceCount] = face.materialName;
                for (int point = 0; point < 3; point++) {
                    int faceVertexIdx = faceIndex + point * 3;
                    Vertex vertex = vertices.get(face.indices[point]);

                    float x = vertex.posX;
                    float y = vertex.posY;
                    float z = vertex.posZ;
                    buffer.data[vertexOffset+0] = x;
                    buffer.data[vertexOffset+1] = y;
                    buffer.data[vertexOffset+2] = z;
                    vertexOffset += buffer.stride;

                    if (!usedVerts[faceVertexIdx]) {
                        usedVerts[faceVertexIdx] = true;
                        points.add(new Vec3d(x, y, z));
                    }
                    buffer.data[textureOffset+0] = vertex.u;
                    buffer.data[textureOffset+1] = vertex.v;
                    textureOffset += buffer.stride;

                    if (hasNormals) {
                        buffer.data[normalOffset+0] = vertex.normalX;
                        buffer.data[normalOffset+1] = vertex.normalY;
                        buffer.data[normalOffset+2] = vertex.normalZ;
                        normalOffset += buffer.stride;
                    }
                }
                faceCount++;
            }

            //Setup model normal
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
    }
    public VertexBuffer getBuffer() {
        return buffer;
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
        if (currentGroupStart != faces.size()) {
            groups.add(new OBJGroup(currentGroupName, currentGroupStart, faces.size() - 1, null, null, null));
        }
        currentGroupName = name;
        currentGroupStart = faces.size();
    }

    private void addVertex(String x, String y, String z) {
        vertexPositions.add(new Vertex.Position(x, y, z, scale));
    }
    private void addVertexTexture(String u, String v) {
        vertexTextures.add(new Vertex.UV(u, v));
    }
    private void addVertexNormal(String x, String y, String z) {
        vertexNormals.add(new Vertex.Normal(x, y, z));
    }

    private void addFace(String a, String b, String c) {
        String[] strings = new String[]{a, b, c};
        int[] index = new int[3];

        for (int i = 0; i < 3; i++) {
            if(vertexMap.containsKey(strings[i])){
                index[i] = vertexMap.get(strings[i]);
            } else {
                vertexMap.put(strings[i], vertices.size());
                vertices.add(parsePoint(strings[i]));
            }
        }

        faces.add(new Face(index[0], index[1], index[2], currentMaterial));
    }

    private Vertex parsePoint(String orig){
        String[] sp = orig.split("/");
        int vertIndex = Integer.parseInt(sp[0]) - 1;
        Vertex.Position position = vertexPositions.get(vertIndex);
        return switch (sp.length) {
            case 3 -> {
                //Obj allow pos//normal and skip uv, need judging specially
                Vertex.UV uv = Objects.equals(sp[1], "") ? Vertex.UV.NONE : vertexTextures.get(Integer.parseInt(sp[1]) - 1);
                yield new Vertex(position, uv, vertexNormals.get(Integer.parseInt(sp[2]) - 1));
            }
            case 2 -> {
                //No vn
                this.hasNormals = false;
                yield new Vertex(position, vertexTextures.get(Integer.parseInt(sp[1]) - 1));
            }
            case 1 -> {
                //No uv and vn
                this.hasNormals = false;
                yield new Vertex(position);
            }
            default -> throw new IllegalArgumentException("Invalid vertex format: " + orig + ", must have at least a position");
        };
    }
}
