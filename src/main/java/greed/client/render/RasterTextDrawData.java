package greed.client.render;

public record RasterTextDrawData(
   String fontName,
   String text,
   float x,
   float y,
   float size,
   int colorTopLeft,
   int colorTopRight,
   int colorBottomRight,
   int colorBottomLeft,
   float rotationDegrees,
   float rotationOriginX,
   float rotationOriginY,
   boolean fadeLeft,
   boolean fadeRight,
   float fadeLeftX,
   float fadeRightX,
   float fadeWidth,
   float fadeLeftStrength,
   float fadeRightStrength,
   boolean snapOrigin
) {
   public boolean visible() {
      return this.text != null
         && !this.text.isEmpty()
         && this.size > 0.0F
         && ((this.colorTopLeft | this.colorTopRight | this.colorBottomRight | this.colorBottomLeft) >>> 24 & 0xFF) > 0;
   }

   public RasterTextDrawData withHorizontalFade(float local546_1, float local546_2, float local546_3, float local546_4, float local546_5) {
      return new RasterTextDrawData(
         this.fontName,
         this.text,
         this.x,
         this.y,
         this.size,
         this.colorTopLeft,
         this.colorTopRight,
         this.colorBottomRight,
         this.colorBottomLeft,
         this.rotationDegrees,
         this.rotationOriginX,
         this.rotationOriginY,
         local546_4 > 0.001F,
         local546_5 > 0.001F,
         local546_1,
         local546_2,
         local546_3,
         clamp01(local546_4),
         clamp01(local546_5),
         this.snapOrigin
      );
   }

   public RasterTextDrawData withoutPixelSnap() {
      return new RasterTextDrawData(
         this.fontName,
         this.text,
         this.x,
         this.y,
         this.size,
         this.colorTopLeft,
         this.colorTopRight,
         this.colorBottomRight,
         this.colorBottomLeft,
         this.rotationDegrees,
         this.rotationOriginX,
         this.rotationOriginY,
         this.fadeLeft,
         this.fadeRight,
         this.fadeLeftX,
         this.fadeRightX,
         this.fadeWidth,
         this.fadeLeftStrength,
         this.fadeRightStrength,
         false
      );
   }

   public RasterTextDrawData withHorizontalFade(float local563_1, float local563_2, float local563_3, boolean local563_4, boolean local563_5) {
      return this.withHorizontalFade(local563_1, local563_2, local563_3, local563_4 ? 1.0F : 0.0F, local563_5 ? 1.0F : 0.0F);
   }

   public RasterTextDrawData(String local564_1, String local564_2, float local564_3, float local564_4, float local564_5, int local564_6) {
      this(local564_1, local564_2, local564_3, local564_4, local564_5, local564_6, local564_6, local564_6, local564_6);
   }

   public RasterTextDrawData(
      String local568_1,
      String local568_2,
      float local568_3,
      float local568_4,
      float local568_5,
      int local568_6,
      int local568_7,
      int local568_8,
      int local568_9
   ) {
      this(
         local568_1,
         local568_2,
         local568_3,
         local568_4,
         local568_5,
         local568_6,
         local568_7,
         local568_8,
         local568_9,
         0.0F,
         local568_3,
         local568_4,
         false,
         false,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         true
      );
   }

   public boolean hasHorizontalFade() {
      return (this.fadeLeftStrength > 0.001F || this.fadeRightStrength > 0.001F) && this.fadeWidth > 0.0F && this.fadeRightX > this.fadeLeftX;
   }

   private static float clamp01(float local575_0) {
      return Math.max(0.0F, Math.min(1.0F, local575_0));
   }
}
