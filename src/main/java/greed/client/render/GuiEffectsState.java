package greed.client.render;

public final class GuiEffectsState {
   private static float floatVal = 24.0F;
   private static float floatVal2 = 1.0F;
   private static boolean flag;
   private static boolean flag2;

   public static void processData(boolean local6638_0) {
      flag = local6638_0;
   }

   public static boolean checkState() {
      return flag2;
   }

   public static void processData2(float local6640_0) {
      floatVal = Math.max(0.0F, local6640_0);
   }

   public static void processData3(boolean local6641_0) {
      flag2 = local6641_0;
   }

   public static float computeFloat() {
      return floatVal2;
   }

   private GuiEffectsState() {
   }

   public static boolean checkState2() {
      return flag;
   }

   public static float computeFloat2() {
      return floatVal;
   }

   public static void processData4(float local6647_0) {
      floatVal2 = Math.max(0.0F, Math.min(1.0F, local6647_0));
   }
}
