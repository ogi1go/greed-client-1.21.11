package greed.client.render;

public record MsdfGlyph(
   boolean drawable,
   float advance,
   float planeLeft,
   float planeTop,
   float planeRight,
   float planeBottom,
   float u0,
   float v0,
   float u1,
   float v1
) {
   public static MsdfGlyph nonDrawable(float local7363_0) {
      return new MsdfGlyph(false, local7363_0, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
   }
}
