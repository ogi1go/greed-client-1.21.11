package greed.client.render;

public record TexturedQuadDrawData(
   float x,
   float y,
   float width,
   float height,
   float radiusTL,
   float radiusTR,
   float radiusBR,
   float radiusBL,
   float smoothness,
   int colorTopLeft,
   int colorTopRight,
   int colorBottomRight,
   int colorBottomLeft,
   float u0,
   float v0,
   float u1,
   float v1,
   float rotationDegrees,
   float rotationOriginX,
   float rotationOriginY
) {
   public TexturedQuadDrawData(
      float local6063_1,
      float local6063_2,
      float local6063_3,
      float local6063_4,
      float local6063_5,
      int local6063_6,
      int local6063_7,
      int local6063_8,
      int local6063_9
   ) {
      this(
         local6063_1,
         local6063_2,
         local6063_3,
         local6063_4,
         local6063_5,
         local6063_5,
         local6063_5,
         local6063_5,
         0.0F,
         local6063_6,
         local6063_7,
         local6063_8,
         local6063_9,
         0.0F,
         0.0F,
         1.0F,
         1.0F,
         0.0F,
         0.0F,
         0.0F
      );
   }
}
