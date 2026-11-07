package greed.client.render;

public final class CornerRadii {
   private static final CornerRadii cornerRadii = new CornerRadii(0.0F, 0.0F, 0.0F, 0.0F, false);
   private final float topLeft;
   private final float topRight;
   private final float bottomRight;
   private final float bottomLeft;
   private final boolean relative;

   public float getTopRight() {
      return this.topRight;
   }

   public static CornerRadii of(float newTopLeft, float newTopRight, float newBottomRight, float newBottomLeft, boolean newRelative) {
      return new CornerRadii(newTopLeft, newTopRight, newBottomRight, newBottomLeft, newRelative);
   }

   public boolean isRelative() {
      return this.relative;
   }

   public CornerRadii expandedBy(float local6651_1) {
      return of(
         this.topLeft + local6651_1, this.topRight + local6651_1, this.bottomRight + local6651_1, this.bottomLeft + local6651_1, this.relative
      );
   }

   public float getBottomLeft() {
      return this.bottomLeft;
   }

   public float maxRadius() {
      return Math.max(Math.max(this.topLeft, this.topRight), Math.max(this.bottomRight, this.bottomLeft));
   }

   public static CornerRadii none() {
      return cornerRadii;
   }

   public static CornerRadii uniform(float newTopLeft) {
      return of(newTopLeft, newTopLeft, newTopLeft, newTopLeft, false);
   }

   public float getBottomRight() {
      return this.bottomRight;
   }

   public static CornerRadii corners(float newTopLeft, float newTopRight, float newBottomRight, float newBottomLeft) {
      return of(newTopLeft, newTopRight, newBottomRight, newBottomLeft, false);
   }

   private CornerRadii(float initialTopLeft, float initialTopRight, float initialBottomRight, float initialBottomLeft, boolean initialRelative) {
      this.topLeft = initialTopLeft;
      this.topRight = initialTopRight;
      this.bottomRight = initialBottomRight;
      this.bottomLeft = initialBottomLeft;
      this.relative = initialRelative;
   }

   public float getTopLeft() {
      return this.topLeft;
   }

   public static CornerRadii getCornerRadii(float newTopLeft) {
      return of(newTopLeft, newTopLeft, newTopLeft, newTopLeft, true);
   }
}
