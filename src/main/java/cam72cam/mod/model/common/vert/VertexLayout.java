package cam72cam.mod.model.common.vert;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VertexLayout {
    private final List<VertAttrElement> elements = new ArrayList<>();
    private final IntArrayList offset = new IntArrayList();
    private int vertSize = 0;

    public VertexLayout addElement(VertAttrElement element) {
        elements.add(element);
        offset.add(vertSize);
        vertSize += element.length;
        return this;
    }

    public VertAttrElement getElement(int i) {
        return elements.get(i);
    }

    public int getOffset(int i) {
        return offset.getInt(i);
    }

    public int getVertSize() {
        return vertSize;
    }
}
