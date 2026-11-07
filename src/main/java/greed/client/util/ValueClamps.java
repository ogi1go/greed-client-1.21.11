package greed.client.util;


public final class ValueClamps {
   public static int lerpInt(int local4968_0, int local4968_1, double local4968_2) {
      return (int)(local4968_0 + local4968_2 * (local4968_1 - local4968_0));
   }

   public static float lerpFloat(float local4969_0, float local4969_1, double local4969_2) {
      return (float)(local4969_0 + local4969_2 * (local4969_1 - local4969_0));
   }

   public static double lerpDouble(double local4970_0, double local4970_2, double local4970_4) {
      return local4970_0 + local4970_4 * (local4970_2 - local4970_0);
   }

   private ValueClamps() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
