package greed.client.render;

public record ArcColorSpan(float startDegrees, float endDegrees, int color) {
   public static ArcColorSpan of(float local1853_0, float local1853_1, int local1853_2) {
      return new ArcColorSpan(local1853_0, local1853_1, local1853_2);
   }
}
