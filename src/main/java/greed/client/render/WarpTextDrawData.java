package greed.client.render;

import net.minecraft.util.Identifier;

public record WarpTextDrawData(
   float x,
   float y,
   float width,
   float height,
   float[] glyphQuads,
   float mouseLocalX,
   float mouseLocalY,
   float influenceRadiusPx,
   float cellPx,
   float pxRange,
   Identifier atlasTexture,
   int color
) {
}
