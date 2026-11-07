package greed.client.render;

import java.util.ArrayDeque;

public final class RoundedClipStack {
   private static float floatVal;
   private static float floatVal2;
   private static float floatVal3;
   private static float floatVal4;
   private static float floatVal5;
   private static float floatVal6;
   private static float floatVal7;
   private static float floatVal8;
   private static boolean flag;
   private static final ArrayDeque<float[]> arrayDeque = new ArrayDeque<>();

   public static float computeFloat() {
      return floatVal6;
   }

   public static float computeFloat2() {
      return floatVal4;
   }

   public static float computeFloat3() {
      return floatVal5;
   }

   public static float computeFloat4() {
      return floatVal2;
   }

   public static void updateState() {
      arrayDeque.clear();
      flag = false;
   }

   public static float computeFloat5() {
      return floatVal7;
   }

   public static float computeFloat6() {
      return floatVal3;
   }

   public static float computeFloat7() {
      return floatVal8;
   }

   public static float computeFloat8(float local8029_0, float local8029_1) {
      if (!flag) {
         return 1.0F;
      }

      float local8029_2 = floatVal + floatVal3 * 0.5F;
      float local8029_3 = floatVal2 + floatVal4 * 0.5F;
      boolean local8029_4 = local8029_0 < local8029_2;
      boolean local8029_5 = local8029_1 < local8029_3;
      float local8029_6 = local8029_5 ? (local8029_4 ? floatVal5 : floatVal6) : (local8029_4 ? floatVal8 : floatVal7);
      float local8029_7 = Math.abs(local8029_0 - local8029_2) - (floatVal3 * 0.5F - local8029_6);
      float local8029_8 = Math.abs(local8029_1 - local8029_3) - (floatVal4 * 0.5F - local8029_6);
      float local8029_9 = Math.max(local8029_7, 0.0F);
      float local8029_10 = Math.max(local8029_8, 0.0F);
      float local8029_11 = Math.min(Math.max(local8029_7, local8029_8), 0.0F)
         + (float)Math.sqrt(local8029_9 * local8029_9 + local8029_10 * local8029_10)
         - local8029_6;
      return Math.max(0.0F, Math.min(1.0F, -local8029_11));
   }

   private RoundedClipStack() {
   }

   public static float computeFloat9() {
      return floatVal;
   }

   public static int computeInt(int local8033_0, float local8033_1, float local8033_2) {
      float local8033_3 = computeFloat8(local8033_1, local8033_2);
      if (local8033_3 >= 0.999F) {
         return local8033_0;
      }

      if (local8033_3 <= 0.001F) {
         return local8033_0 & 16777215;
      }

      int local8033_4 = Math.round((local8033_0 >>> 24 & 0xFF) * local8033_3);
      return local8033_4 << 24 | local8033_0 & 16777215;
   }

   public static void processData(
      float local8034_0, float local8034_1, float local8034_2, float local8034_3, float local8034_4, float local8034_5, float local8034_6, float local8034_7
   ) {
      if (flag) {
         arrayDeque.push(new float[]{floatVal, floatVal2, floatVal3, floatVal4, floatVal5, floatVal6, floatVal7, floatVal8});
      }

      floatVal = local8034_0;
      floatVal2 = local8034_1;
      floatVal3 = local8034_2;
      floatVal4 = local8034_3;
      floatVal5 = local8034_4;
      floatVal6 = local8034_5;
      floatVal7 = local8034_6;
      floatVal8 = local8034_7;
      flag = true;
   }

   public static boolean checkState() {
      return flag;
   }

   public static void updateState2() {
      float[] floatArray = arrayDeque.poll();
      if (floatArray != null) {
         floatVal = floatArray[0];
         floatVal2 = floatArray[1];
         floatVal3 = floatArray[2];
         floatVal4 = floatArray[3];
         floatVal5 = floatArray[4];
         floatVal6 = floatArray[5];
         floatVal7 = floatArray[6];
         floatVal8 = floatArray[7];
      } else {
         flag = false;
      }
   }
}
