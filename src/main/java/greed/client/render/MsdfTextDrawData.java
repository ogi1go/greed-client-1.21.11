package greed.client.render;

public record MsdfTextDrawData(
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
   float fadeLeftX,
   float fadeRightX,
   float fadeWidth,
   float fadeLeftStrength,
   float fadeRightStrength
) {
   private static int alpha(int local6770_0) {
      return local6770_0 >>> 24 & 0xFF;
   }

   private static float clamp01(float local6772_0) {
      return Math.max(0.0F, Math.min(1.0F, local6772_0));
   }

   public boolean hasHorizontalFade() {
      return (this.fadeLeftStrength > 0.001F || this.fadeRightStrength > 0.001F) && this.fadeWidth > 0.0F && this.fadeRightX > this.fadeLeftX;
   }

   public MsdfTextDrawData(
      String local6783_1,
      String local6783_2,
      float local6783_3,
      float local6783_4,
      float local6783_5,
      int local6783_6,
      int local6783_7,
      int local6783_8,
      int local6783_9,
      float local6783_10,
      float local6783_11,
      float local6783_12
   ) {
      this(
         local6783_1,
         local6783_2,
         local6783_3,
         local6783_4,
         local6783_5,
         local6783_6,
         local6783_7,
         local6783_8,
         local6783_9,
         local6783_10,
         local6783_11,
         local6783_12,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F
      );
   }

   public MsdfTextDrawData(
      String local6785_1,
      String local6785_2,
      float local6785_3,
      float local6785_4,
      float local6785_5,
      int local6785_6,
      float local6785_7,
      float local6785_8,
      float local6785_9
   ) {
      this(
         local6785_1,
         local6785_2,
         local6785_3,
         local6785_4,
         local6785_5,
         local6785_6,
         local6785_6,
         local6785_6,
         local6785_6,
         local6785_7,
         local6785_8,
         local6785_9,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F
      );
   }

   public MsdfTextDrawData(
      String local6789_1,
      String local6789_2,
      float local6789_3,
      float local6789_4,
      float local6789_5,
      int local6789_6,
      int local6789_7,
      int local6789_8,
      int local6789_9
   ) {
      this(
         local6789_1,
         local6789_2,
         local6789_3,
         local6789_4,
         local6789_5,
         local6789_6,
         local6789_7,
         local6789_8,
         local6789_9,
         0.0F,
         local6789_3,
         local6789_4,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F
      );
   }

   public MsdfTextDrawData withHorizontalFade(float local6792_1, float local6792_2, float local6792_3, float local6792_4, float local6792_5) {
      return new MsdfTextDrawData(
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
         local6792_1,
         local6792_2,
         local6792_3,
         clamp01(local6792_4),
         clamp01(local6792_5)
      );
   }

   public MsdfTextDrawData(String local6793_1, String local6793_2, float local6793_3, float local6793_4, float local6793_5, int local6793_6) {
      this(local6793_1, local6793_2, local6793_3, local6793_4, local6793_5, local6793_6, local6793_6, local6793_6, local6793_6);
   }

   public boolean visible() {
      return this.text != null && !this.text.isEmpty() && !(this.size <= 0.0F)
         ? alpha(this.colorTopLeft) > 0 || alpha(this.colorTopRight) > 0 || alpha(this.colorBottomRight) > 0 || alpha(this.colorBottomLeft) > 0
         : false;
   }
}
