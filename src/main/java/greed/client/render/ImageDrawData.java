package greed.client.render;

public record ImageDrawData(
   String texture,
   float x,
   float y,
   float size,
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
   float rotationOriginY,
   float explicitWidth,
   float explicitHeight,
   boolean nearest,
   boolean glow
) {
   public static final int DEFAULT_COLOR = -1;
   public static final float DEFAULT_SMOOTHNESS = 0.0F;

   public boolean visible() {
      return this.texture != null
         && !this.texture.isBlank()
         && (this.size > 0.0F || this.explicitWidth > 0.0F && this.explicitHeight > 0.0F)
         && (
            effectiveAlpha(this.colorTopLeft) > 0
               || effectiveAlpha(this.colorTopRight) > 0
               || effectiveAlpha(this.colorBottomRight) > 0
               || effectiveAlpha(this.colorBottomLeft) > 0
         );
   }

   public ImageDrawData withColors(int... intArray) {
      if (intArray != null && intArray.length != 0) {
         if (intArray.length == 1) {
            return this.withColors(intArray[0], intArray[0], intArray[0], intArray[0]);
         } else if (intArray.length == 2) {
            return this.withColors(intArray[0], intArray[1], intArray[1], intArray[0]);
         } else {
            return intArray.length == 3
               ? this.withColors(intArray[0], intArray[1], intArray[2], intArray[0])
               : this.withColors(intArray[0], intArray[1], intArray[2], intArray[3]);
         }
      } else {
         return this;
      }
   }

   public ImageDrawData withUv(float u0, float v0, float u1, float v1) {
      return new ImageDrawData(
         this.texture,
         this.x,
         this.y,
         this.size,
         this.radiusTL,
         this.radiusTR,
         this.radiusBR,
         this.radiusBL,
         this.smoothness,
         this.colorTopLeft,
         this.colorTopRight,
         this.colorBottomRight,
         this.colorBottomLeft,
         u0,
         v0,
         u1,
         v1,
         this.rotationDegrees,
         this.rotationOriginX,
         this.rotationOriginY,
         this.explicitWidth,
         this.explicitHeight,
         this.nearest,
         this.glow
      );
   }

   private ImageDrawData withColors(int colorTopLeft, int colorTopRight, int colorBottomRight, int colorBottomLeft) {
      return new ImageDrawData(
         this.texture,
         this.x,
         this.y,
         this.size,
         this.radiusTL,
         this.radiusTR,
         this.radiusBR,
         this.radiusBL,
         this.smoothness,
         colorTopLeft,
         colorTopRight,
         colorBottomRight,
         colorBottomLeft,
         this.u0,
         this.v0,
         this.u1,
         this.v1,
         this.rotationDegrees,
         this.rotationOriginX,
         this.rotationOriginY,
         this.explicitWidth,
         this.explicitHeight,
         this.nearest,
         this.glow
      );
   }

   public ImageDrawData(String texture, float x, float y, float size, float radius, int color) {
      this(
         texture,
         x,
         y,
         size,
         radius,
         radius,
         radius,
         radius,
         0.0F,
         color,
         color,
         color,
         color,
         0.0F,
         0.0F,
         1.0F,
         1.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         false,
         false
      );
   }

   public ImageDrawData(
      String texture,
      float x,
      float y,
      float size,
      float radiusTL,
      float radiusTR,
      float radiusBR,
      float radiusBL,
      int colorTopLeft,
      int colorTopRight,
      int colorBottomRight,
      int colorBottomLeft
   ) {
      this(
         texture,
         x,
         y,
         size,
         radiusTL,
         radiusTR,
         radiusBR,
         radiusBL,
         0.0F,
         colorTopLeft,
         colorTopRight,
         colorBottomRight,
         colorBottomLeft,
         0.0F,
         0.0F,
         1.0F,
         1.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         false,
         false
      );
   }

   private static int effectiveAlpha(int color) {
      return color >>> 24 & 0xFF;
   }

   public ImageDrawData(
      String texture,
      float x,
      float y,
      float size,
      float radius,
      int colorTopLeft,
      int colorTopRight,
      int colorBottomRight,
      int colorBottomLeft
   ) {
      this(
         texture,
         x,
         y,
         size,
         radius,
         radius,
         radius,
         radius,
         0.0F,
         colorTopLeft,
         colorTopRight,
         colorBottomRight,
         colorBottomLeft,
         0.0F,
         0.0F,
         1.0F,
         1.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         false,
         false
      );
   }

   public ImageDrawData withRotation(float rotationDegrees, float rotationOriginX, float rotationOriginY) {
      return new ImageDrawData(
         this.texture,
         this.x,
         this.y,
         this.size,
         this.radiusTL,
         this.radiusTR,
         this.radiusBR,
         this.radiusBL,
         this.smoothness,
         this.colorTopLeft,
         this.colorTopRight,
         this.colorBottomRight,
         this.colorBottomLeft,
         this.u0,
         this.v0,
         this.u1,
         this.v1,
         rotationDegrees,
         rotationOriginX,
         rotationOriginY,
         this.explicitWidth,
         this.explicitHeight,
         this.nearest,
         this.glow
      );
   }

   public ImageDrawData(String texture, float x, float y, float size, float radius) {
      this(
         texture,
         x,
         y,
         size,
         radius,
         radius,
         radius,
         radius,
         0.0F,
         -1,
         -1,
         -1,
         -1,
         0.0F,
         0.0F,
         1.0F,
         1.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         false,
         false
      );
   }

   public ImageDrawData withGlow() {
      return this.glow
         ? this
         : new ImageDrawData(
            this.texture,
            this.x,
            this.y,
            this.size,
            this.radiusTL,
            this.radiusTR,
            this.radiusBR,
            this.radiusBL,
            this.smoothness,
            this.colorTopLeft,
            this.colorTopRight,
            this.colorBottomRight,
            this.colorBottomLeft,
            this.u0,
            this.v0,
            this.u1,
            this.v1,
            this.rotationDegrees,
            this.rotationOriginX,
            this.rotationOriginY,
            this.explicitWidth,
            this.explicitHeight,
            this.nearest,
            true
         );
   }

   public ImageDrawData withSmoothness(float smoothness) {
      return new ImageDrawData(
         this.texture,
         this.x,
         this.y,
         this.size,
         this.radiusTL,
         this.radiusTR,
         this.radiusBR,
         this.radiusBL,
         smoothness,
         this.colorTopLeft,
         this.colorTopRight,
         this.colorBottomRight,
         this.colorBottomLeft,
         this.u0,
         this.v0,
         this.u1,
         this.v1,
         this.rotationDegrees,
         this.rotationOriginX,
         this.rotationOriginY,
         this.explicitWidth,
         this.explicitHeight,
         this.nearest,
         this.glow
      );
   }

   public float radius() {
      return this.radiusTL;
   }

   public ImageDrawData(String texture, float x, float y, float explicitWidth, float explicitHeight, float radius, int color) {
      this(
         texture,
         x,
         y,
         0.0F,
         radius,
         radius,
         radius,
         radius,
         0.0F,
         color,
         color,
         color,
         color,
         0.0F,
         0.0F,
         1.0F,
         1.0F,
         0.0F,
         0.0F,
         0.0F,
         Math.max(0.0F, explicitWidth),
         Math.max(0.0F, explicitHeight),
         false,
         false
      );
   }

   public ImageDrawData withNearest() {
      return this.nearest
         ? this
         : new ImageDrawData(
            this.texture,
            this.x,
            this.y,
            this.size,
            this.radiusTL,
            this.radiusTR,
            this.radiusBR,
            this.radiusBL,
            this.smoothness,
            this.colorTopLeft,
            this.colorTopRight,
            this.colorBottomRight,
            this.colorBottomLeft,
            this.u0,
            this.v0,
            this.u1,
            this.v1,
            this.rotationDegrees,
            this.rotationOriginX,
            this.rotationOriginY,
            this.explicitWidth,
            this.explicitHeight,
            true,
            this.glow
         );
   }
}
