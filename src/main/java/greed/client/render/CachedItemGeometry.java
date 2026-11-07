package greed.client.render;

import java.util.List;

public record CachedItemGeometry(List<ItemGeometryQuad> quads, boolean animated, boolean specialRenderer) {
}
