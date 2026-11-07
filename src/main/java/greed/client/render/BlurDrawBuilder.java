package greed.client.render;

public final class BlurDrawBuilder {
   private float x;
   private float y;
   private float width;
   private float height;
   private float radiusTopLeft;
   private float radiusTopRight;
   private float radiusBottomRight;
   private float radiusBottomLeft;
   private float smoothness = 1.0F;
   private float blurRadius = 16.0F;
   private int color = -1;

   public BlurDrawBuilder withSmoothness(float newSmoothness) {
      this.smoothness = newSmoothness;
      return this;
   }

   public BlurDrawBuilder withRoundedBounds(float newX, float newY, float newWidth, float newHeight, float newRadiusTopLeft) {
      return this.withBounds(newX, newY, newWidth, newHeight).withRadius(newRadiusTopLeft);
   }

   public BlurDrawBuilder withBlurRadius(float newBlurRadius) {
      this.blurRadius = newBlurRadius;
      return this;
   }

   public BlurDrawBuilder withColor(int newColor) {
      this.color = newColor;
      return this;
   }

   public BlurDrawData build() {
      return new BlurDrawData(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.smoothness,
         this.blurRadius,
         this.color
      );
   }

   public BlurDrawBuilder withRadius(float newRadiusTopLeft) {
      this.radiusTopLeft = newRadiusTopLeft;
      this.radiusTopRight = newRadiusTopLeft;
      this.radiusBottomRight = newRadiusTopLeft;
      this.radiusBottomLeft = newRadiusTopLeft;
      return this;
   }

   public BlurDrawBuilder withRadii(float newRadiusTopLeft, float newRadiusTopRight, float newRadiusBottomRight, float newRadiusBottomLeft) {
      this.radiusTopLeft = newRadiusTopLeft;
      this.radiusTopRight = newRadiusTopRight;
      this.radiusBottomRight = newRadiusBottomRight;
      this.radiusBottomLeft = newRadiusBottomLeft;
      return this;
   }

   public BlurDrawBuilder withBounds(float newX, float newY, float newWidth, float newHeight) {
      this.x = newX;
      this.y = newY;
      this.width = newWidth;
      this.height = newHeight;
      return this;
   }
}
