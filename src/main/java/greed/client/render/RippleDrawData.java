package greed.client.render;

public record RippleDrawData(
   float x,
   float y,
   float width,
   float height,
   float radiusTopLeft,
   float radiusTopRight,
   float radiusBottomRight,
   float radiusBottomLeft,
   float smoothness,
   float centerX,
   float centerY,
   float rippleRadius,
   float rippleSmoothness,
   int sourceColor,
   int targetColor,
   boolean isTextured
) {
   public RippleDrawData(
      float x,
      float y,
      float width,
      float height,
      float cornerRadius,
      float smoothness,
      float centerX,
      float centerY,
      float rippleRadius,
      float rippleSmoothness,
      int sourceColor,
      int targetColor
   ) {
      this(
         x,
         y,
         width,
         height,
         cornerRadius,
         cornerRadius,
         cornerRadius,
         cornerRadius,
         smoothness,
         centerX,
         centerY,
         rippleRadius,
         rippleSmoothness,
         sourceColor,
         targetColor,
         false
      );
   }

   public boolean visible() {
      return this.width > 0.0F && this.height > 0.0F;
   }
}
