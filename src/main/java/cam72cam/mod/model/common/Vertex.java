package cam72cam.mod.model.common;

public class Vertex {
    public float posX;
    public float posY;
    public float posZ;

    public float u;
    public float v;

    public float r;
    public float g;
    public float b;
    public float a;

    public float normalX;
    public float normalY;
    public float normalZ;

    public Vertex(Position position){
        this(position, UV.NONE, Normal.NONE);
    }

    public Vertex(Position position, UV uv){
        this(position, uv, Normal.NONE);
    }

    public Vertex(Position position, UV uv, Normal normal){
        this.posX = position.posX;
        this.posY = position.posY;
        this.posZ = position.posZ;

        this.u = uv.u;
        this.v = uv.v;

        this.normalX = normal.normalX;
        this.normalY = normal.normalY;
        this.normalZ = normal.normalZ;
    }

    public Vertex(float posX, float posY, float posZ) {
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
    }

    public Vertex color(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        return this;
    }

    public Vertex uv(float u, float v) {
        this.u = u;
        this.v = v;
        return this;
    }

    public Vertex normal(float normalX, float normalY, float normalZ) {
        this.normalX = normalX;
        this.normalY = normalY;
        this.normalZ = normalZ;
        return this;
    }

    public static class Position{
        public float posX;
        public float posY;
        public float posZ;

        public Position(String x, String y, String z, float scale){
            this(Float.parseFloat(x) * scale,
                 Float.parseFloat(y) * scale,
                 Float.parseFloat(z) * scale);
        }

        public Position(float posX, float posY, float posZ) {
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
        }
    }

    public static class UV{
        public static final UV NONE = new UV(0,0);

        public float u;
        public float v;

        public UV(String u, String v){
            this(Float.parseFloat(u), Float.parseFloat(v));
        }

        public UV(float u, float v) {
            this.u = u;
            this.v = v;
        }
    }

    public static class Normal{
        protected static final Normal NONE = new Normal(0,0,0);

        public float normalX;
        public float normalY;
        public float normalZ;

        public Normal(String x, String y, String z){
            this(Float.parseFloat(x), Float.parseFloat(y), Float.parseFloat(z));
        }

        public Normal(float normalX, float normalY, float normalZ) {
            this.normalX = normalX;
            this.normalY = normalY;
            this.normalZ = normalZ;
        }
    }
}
