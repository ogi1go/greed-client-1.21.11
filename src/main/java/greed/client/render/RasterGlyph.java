package greed.client.render;

import greed.client.render.FontTexture;

record RasterGlyph(
   FontTexture page,
   float u0,
   float v0,
   float u1,
   float v1,
   float xOffset,
   float yOffset,
   float width,
   float height,
   float advance
) {
   boolean drawable() {
      return this.page != null && this.width > 0.0F && this.height > 0.0F;
   }

   static RasterGlyph empty(float local2722_0) {
      return new RasterGlyph(null, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, local2722_0);
   }

   RasterGlyph withAdvance(float local2726_1) {
      return new RasterGlyph(
         this.page, this.u0, this.v0, this.u1, this.v1, this.xOffset, this.yOffset, this.width, this.height, local2726_1
      );
   }
}
