package greed.client.render;

import greed.client.ui.UiTheme;

import greed.client.render.Render2D;

import java.awt.Color;

public final class ThemeColors {
   public static int intVal = new Color(15, 14, 17, 184).getRGB();
   public static int intVal2 = new Color(255, 255, 255, 3).getRGB();
   public static int intVal3 = new Color(255, 255, 255, 5).getRGB();
   public static int __ = new Color(255, 255, 255, 10).getRGB();
   public static int intVal4 = new Color(255, 255, 255, 20).getRGB();
   public static int intVal5 = -1;
   public static int intVal6 = new Color(255, 255, 255, 184).getRGB();
   public static int intVal7 = new Color(255, 255, 255, 122).getRGB();
   public static int intVal8 = new Color(255, 255, 255, 61).getRGB();
   public static int intVal9 = -12092417;
   public static int intVal10 = 285212671;

   public static void processData(String local6576_0, float local6576_1, float local6576_2, float local6576_3, int local6576_4) {
      UiTheme.processData7(local6576_0, local6576_1, local6576_2 - 0.5F, local6576_3, local6576_4);
   }

   private ThemeColors() {
   }

   public static void processData2(float local6578_0, float local6578_1, float local6578_2, float local6578_3) {
      Render2D.drawCornerBracket(local6578_0 + local6578_2 - 9.0F, local6578_1 + local6578_3 - 9.0F, 5.0F, 2.5F, 0.6F, 2, intVal10);
   }

   public static void processData3(float local6579_0, float local6579_1, float local6579_2, float local6579_3, float local6579_4) {
      Render2D.drawBackdropBlur(local6579_0, local6579_1, local6579_2, local6579_3, local6579_4);
      Render2D.drawOutline(local6579_0, local6579_1, local6579_2, local6579_3, local6579_4, 0.5F, intVal3);
   }

   public static void processData4(float local6580_0, float local6580_1, float local6580_2) {
      Render2D.drawRect(local6580_0, local6580_1 - local6580_2 / 2.0F, 0.5F, local6580_2, 0.25F, __);
   }

   public static float computeFloat(float local6582_0) {
      return Math.max(0.0F, Math.min(1.0F, local6582_0));
   }

   public static void processData5(float local6583_0, float local6583_1, float local6583_2, float local6583_3, int local6583_4) {
      float local6583_5 = local6583_0 - local6583_2 / 2.0F;
      float local6583_6 = local6583_1 - local6583_2 / 2.0F;
      float local6583_7 = computeFloat(local6583_3);
      if (local6583_7 < 0.004F) {
         Render2D.drawOutlineArcUniformVarargs(local6583_5, local6583_6, local6583_2, local6583_2, local6583_2 / 2.0F, 1.0F, intVal3);
      } else {
         Render2D.drawOutlineArcUniformVarargs(
            local6583_5,
            local6583_6,
            local6583_2,
            local6583_2,
            local6583_2 / 2.0F,
            1.0F,
            intVal3,
            ArcColorSpan.of(90.0F - local6583_7 * 360.0F, 90.0F, local6583_4)
         );
         if (local6583_7 < 0.996F) {
            float local6583_8 = local6583_2 / 2.0F - 0.5F;
            double local6583_9 = Math.toRadians(local6583_7 * 360.0);
            Render2D.drawCircleSmooth(local6583_0, local6583_1 - local6583_8, 0.5F, 0.25F, local6583_4);
            Render2D.drawCircleSmooth(
               local6583_0 + local6583_8 * (float)Math.sin(local6583_9), local6583_1 - local6583_8 * (float)Math.cos(local6583_9), 0.5F, 0.25F, local6583_4
            );
         }
      }
   }
}
