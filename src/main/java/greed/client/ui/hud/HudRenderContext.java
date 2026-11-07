package greed.client.ui.hud;

import greed.client.util.GuiScale;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;

public final class HudRenderContext {
   private static boolean flag;
   private static boolean flag2;
   private static float floatVal;
   private static float floatVal2;
   private static long timeMillis;
   private static long timeMillis2 = System.currentTimeMillis();
   private static int intVal;
   private static DrawContext drawContext2;

   public static void renderElement(DrawContext drawContext) {
      drawContext2 = drawContext;
   }

   static void processData(MinecraftClient minecraftClient, float local7300_1) {
      flag = minecraftClient.currentScreen instanceof ChatScreen;
      long local7300_2 = System.currentTimeMillis();
      if (flag != flag2) {
         floatVal2 = floatVal;
         timeMillis = local7300_2;
         flag2 = flag;
      }

      float local7300_4 = Math.min(1.0F, (float)(local7300_2 - timeMillis) / (flag ? 180.0F : 120.0F));
      floatVal = floatVal2 + ((flag ? 1.0F : 0.0F) - floatVal2) * (1.0F - (float)Math.pow(1.0F - local7300_4, 3.0));
      if (local7300_2 - timeMillis2 >= 1000L) {
         timeMillis2 = local7300_2;
         intVal++;
      }
   }

   public static void processData2(float local7301_0, float local7301_1, float local7301_2, float local7301_3) {
      if (drawContext2 != null) {
         float local7301_4 = GuiScale.computeFloat4(local7301_0);
         float local7301_5 = GuiScale.computeFloat4(local7301_1);
         drawContext2.getMatrices().pushMatrix();
         drawContext2.getMatrices().translate(local7301_4, local7301_5);
         drawContext2.getMatrices().scale(local7301_2, local7301_3);
         drawContext2.getMatrices().translate(-local7301_4, -local7301_5);
      }
   }

   public static void processData3(float local7302_0, float local7302_1, float local7302_2) {
      processData2(local7302_0, local7302_1, local7302_2, local7302_2);
   }

   public static void updateState() {
      if (drawContext2 != null) {
         drawContext2.getMatrices().popMatrix();
      }
   }

   public static float computeFloat() {
      return floatVal;
   }

   public static int computeInt() {
      return intVal;
   }

   public static DrawContext getDrawContext() {
      return drawContext2;
   }

   public static void processData4(float local7308_0, float local7308_1, float local7308_2) {
      processData3(local7308_0, local7308_1, 1.0F);
   }

   public static boolean checkState() {
      return floatVal > 0.01F;
   }

   public static boolean checkState2() {
      return flag;
   }

   private HudRenderContext() {
   }
}
