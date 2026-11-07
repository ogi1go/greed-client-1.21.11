package greed.client.render;

import greed.client.util.GuiScale;

public final class PixelSnap {
   static final int intVal = 512;
   static final int intVal2 = 2;
   static final int intVal3 = 1024;

   static int computeInt(float local5383_0) {
      return Math.max(1, Math.round(GuiScale.computeFloat()));
   }

   static float computeFloat(float local5384_0, float local5384_1) {
      return local5384_1 > 1.0E-4F ? Math.round(local5384_0 * local5384_1) / local5384_1 : computeFloat2(local5384_0);
   }

   static int computeInt2(int local5385_0) {
      return Math.max(4, local5385_0 + 2);
   }

   public static float computeFloat2(float local5386_0) {
      float local5386_1 = GuiScale.computeFloat();
      return Math.round(local5386_0 * local5386_1) / local5386_1;
   }

   private PixelSnap() {
   }
}
