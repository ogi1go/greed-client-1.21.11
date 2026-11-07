package greed.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;
import org.joml.Matrix3x2f;

public final class GuiScale {
   private static final Matrix3x2f matrix = new Matrix3x2f();

   public static float computeFloat() {
      return 2.0F;
   }

   public static Matrix3x2f renderElement(DrawContext drawContext) {
      Matrix3x2f matrix3x2f = matrix.set(drawContext.getMatrices());
      processData(matrix3x2f);
      return matrix3x2f;
   }

   public static float computeFloat2() {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient == null) {
         return 0.0F;
      }

      Window window = minecraftClient.getWindow();
      return window == null ? 0.0F : window.getFramebufferHeight();
   }

   public static int computeInt() {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient == null) {
         return 1;
      }

      Window window = minecraftClient.getWindow();
      return window == null ? 1 : Math.max(1, window.getScaleFactor());
   }

   public static int computeInt2(float local1068_0) {
      return Math.round(computeFloat4(local1068_0));
   }

   private GuiScale() {
   }

   public static float computeFloat3() {
      return 2.0F / computeInt();
   }

   public static float computeFloat4(float local1072_0) {
      return local1072_0 * computeFloat3();
   }

   public static void processData(Matrix3x2f matrix3x2f) {
      float local1073_1 = computeFloat3();
      if (local1073_1 != 1.0F) {
         matrix3x2f.scale(local1073_1);
      }
   }
}
