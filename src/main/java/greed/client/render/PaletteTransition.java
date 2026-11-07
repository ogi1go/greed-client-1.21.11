package greed.client.render;

public final class PaletteTransition {
   private static long startedAtMillis = -4611686018427387904L;

   private PaletteTransition() {
   }

   public static float getProgress() {
      float local7053_0 = (float)(System.currentTimeMillis() - startedAtMillis) / 700.0F;
      return !(local7053_0 < 0.0F) && !(local7053_0 > 1.0F) ? local7053_0 : -1.0F;
   }

   public static void start() {
      startedAtMillis = System.currentTimeMillis();
   }
}
