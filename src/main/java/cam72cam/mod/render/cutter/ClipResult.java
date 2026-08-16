package cam72cam.mod.render.cutter;

import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public final class ClipResult {
    public final Polygon polygon;
    public final List<Pair<ClipVertex, ClipVertex>> intersections;

    public ClipResult(Polygon polygon, List<Pair<ClipVertex, ClipVertex>> intersections) {
        this.polygon = polygon;
        this.intersections = intersections;
    }
}
