package cam72cam.mod.model.common.vert;

public class VertAttrElement {
    public static final VertAttrElement POSITION = new VertAttrElement("Pos", Type.FLOAT, Usage.POSITION, 3);
    public static final VertAttrElement COLOR = new VertAttrElement("Col", Type.FLOAT, Usage.COLOR, 4);
    public static final VertAttrElement UV = new VertAttrElement("Uv", Type.FLOAT, Usage.UV, 2);
    public static final VertAttrElement NORMAL = new VertAttrElement("Nor", Type.FLOAT, Usage.NORMAL, 3);
    public static final VertAttrElement PADDING = new VertAttrElement("Padding", Type.BYTE, Usage.PADDING, 1);

    public final String name;
    public final Type dataType;
    public final Usage usage;
    public final int length;

    public VertAttrElement(String name, Type type, Usage usage, int length) {
        this.name = name;
        this.dataType = type;
        this.usage = usage;
        this.length = length;
    }

    public enum Type {
        SHORT(2),
        UNSIGNED_SHORT(2),
        BYTE(1),
        UNSIGNED_BYTE(1),
        FLOAT(4),
        HALF_FLOAT(2),
        ;

        public final int size;

        Type(int size) {
            this.size = size;
        }
    }

    public enum Usage {
        POSITION,
        UV,
        COLOR,
        NORMAL,
        PADDING,
        OTHERS,
        ;
    }
}
