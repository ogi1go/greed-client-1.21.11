package greed.client.render;

public record BlobDrawData(
   float x,
   float y,
   float w,
   float h,
   float centerX,
   float centerY,
   float radiusX,
   float radiusY,
   float falloff,
   float feather,
   float clipTL,
   float clipTR,
   float clipBR,
   float clipBL,
   int color
) {
   public boolean visible() {
      return this.w > 0.0F && this.h > 0.0F && this.radiusX > 0.0F && this.radiusY > 0.0F && this.color >>> 24 != 0;
   }
}
