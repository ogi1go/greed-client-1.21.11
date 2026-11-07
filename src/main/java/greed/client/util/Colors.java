package greed.client.util;

public final class Colors {
   public static final int intVal = -1;
   public static final int intVal2 = -16777216;
   public static final int intVal3 = -65536;
   public static final int intVal4 = -16711936;
   public static final int intVal5 = -16776961;

   private static int computeInt(int local6166_0) {
      return local6166_0 >>> 8 & 0xFF;
   }

   public static int computeInt2(int local6167_0, int local6167_1, float local6167_2) {
      float local6167_3 = Float.isFinite(local6167_2) ? Math.clamp(local6167_2, 0.0F, 1.0F) : 0.0F;
      int local6167_4 = Math.round(
         computeInt4(computeInt9(local6167_0)) + (computeInt4(computeInt9(local6167_1)) - computeInt4(computeInt9(local6167_0))) * local6167_3
      );
      int local6167_5 = Math.round(computeInt3(local6167_0) + (computeInt3(local6167_1) - computeInt3(local6167_0)) * local6167_3);
      int local6167_6 = Math.round(computeInt(local6167_0) + (computeInt(local6167_1) - computeInt(local6167_0)) * local6167_3);
      int local6167_7 = Math.round(computeInt8(local6167_0) + (computeInt8(local6167_1) - computeInt8(local6167_0)) * local6167_3);
      return computeInt5(local6167_5, local6167_6, local6167_7, local6167_4);
   }

   private static int computeInt3(int local6168_0) {
      return local6168_0 >>> 16 & 0xFF;
   }

   public static int computeInt4(int local6169_0) {
      return local6169_0 >>> 24 & 0xFF;
   }

   public static int computeInt5(int local6170_0, int local6170_1, int local6170_2, int local6170_3) {
      return (local6170_3 & 0xFF) << 24 | (local6170_0 & 0xFF) << 16 | (local6170_1 & 0xFF) << 8 | local6170_2 & 0xFF;
   }

   private Colors() {
   }

   public static int computeInt6(int local6172_0, float local6172_1) {
      if (!Float.isFinite(local6172_1)) {
         local6172_1 = 1.0F;
      }

      return computeInt7(local6172_0, Math.clamp(Math.round(computeInt4(computeInt9(local6172_0)) * local6172_1), 0, 255));
   }

   public static int computeInt7(int local6173_0, int local6173_1) {
      return local6173_0 & 16777215 | (local6173_1 & 0xFF) << 24;
   }

   private static int computeInt8(int local6174_0) {
      return local6174_0 & 0xFF;
   }

   private static int computeInt9(int local6175_0) {
      return (local6175_0 & 0xFF000000) == 0 && (local6175_0 & 16777215) != 0 ? local6175_0 | 0xFF000000 : local6175_0;
   }

   public static int computeInt10(int local6176_0, int local6176_1, int local6176_2) {
      return computeInt5(local6176_0, local6176_1, local6176_2, 255);
   }
}
