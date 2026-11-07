package greed.client.ui.setting;

import greed.client.ui.ClickGuiStyle;
import greed.client.setting.ColorSetting;
import greed.client.ui.UiTheme;

import greed.client.render.Render2D;
import greed.client.setting.Setting;

import java.awt.Color;

public final class ColorSettingRenderer implements SettingRenderer {
   @Override
   public float getHeight(SettingRenderContext settingRenderContext, Setting<?> local3123_2, float local3123_3) {
      return 7.0F * settingRenderContext.scale;
   }

   @Override
   public float render(
      SettingRenderContext settingRenderContext, Setting<?> local3126_2, float local3126_3, float local3126_4, float local3126_5, float local3126_6, float[] floatArray
   ) {
      ColorSetting currentColorSetting = (ColorSetting)local3126_2;
      float local3126_9 = settingRenderContext.scale;
      settingRenderContext.processData15(
         currentColorSetting.name,
         local3126_4,
         local3126_5,
         6.0F * local3126_9,
         UiTheme.computeInt2(ClickGuiStyle.intVal10, ClickGuiStyle.intVal11, local3126_3),
         local3126_6 - 22.0F * local3126_9
      );
      settingRenderContext.processData17(currentColorSetting.description, local3126_4, local3126_5 - 1.0F * local3126_9, local3126_6 * 0.5F, 8.0F * local3126_9);
      float[] floatArray2 = settingRenderContext.computeFloat5(currentColorSetting);
      int local3126_11 = computeInt(Color.HSBtoRGB(floatArray2[0], floatArray2[1], floatArray2[2]));
      float local3126_12 = local3126_4 + local3126_6 - 3.72F * local3126_9;
      float local3126_13 = local3126_5 + 3.0F * local3126_9;
      Render2D.drawImageSized(
         "assets/greed/textures/gui/clickgui/picker.png",
         local3126_12 - 13.31F * local3126_9,
         local3126_13 - 3.05F * local3126_9,
         6.1F * local3126_9,
         6.1F * local3126_9,
         0.0F,
         UiTheme.computeInt(ClickGuiStyle.intVal, 0.35F)
      );
      Render2D.drawCircle(local3126_12, local3126_13, 2.81F * local3126_9, local3126_11);
      Render2D.drawCircleOutline(local3126_12, local3126_13, 3.72F * local3126_9, 0.5F * local3126_9, UiTheme.computeInt(local3126_11, 0.4F));
      settingRenderContext.processData7(
         local3126_4 + local3126_6 / 2.0F,
         local3126_5 - 1.5F * local3126_9,
         local3126_6 / 2.0F,
         9.0F * local3126_9,
         floatArray,
         () -> settingRenderContext.object = settingRenderContext.object == currentColorSetting ? null : currentColorSetting
      );
      int local3126_14 = System.identityHashCode(currentColorSetting);
      boolean local3126_15 = settingRenderContext.object == currentColorSetting;
      float local3126_16 = settingRenderContext.computeFloat7("colorpop:" + local3126_14, local3126_15 ? 1.0F : 0.0F);
      if (local3126_16 < 0.01F) {
         return 7.0F * local3126_9;
      }

      float local3126_17 = local3126_12 + 31.6F * local3126_9;
      float local3126_18 = local3126_5 - 2.0F * local3126_9;
      settingRenderContext.list2
         .add(
            () -> {
               float[] floatArray3 = settingRenderContext.computeFloat5(currentColorSetting);
               float local3125_9 = settingRenderContext.computeFloat7("colorS:" + local3126_14, floatArray3[1]);
               float local3125_10 = settingRenderContext.computeFloat7("colorB:" + local3126_14, floatArray3[2]);
               float local3125_11 = settingRenderContext.computeFloat7("colorH:" + local3126_14, floatArray3[0]);
               float local3125_12 = 4.0F * local3126_9;
               float local3125_13 = 58.0F * local3126_9;
               float local3125_14 = 40.0F * local3126_9;
               float local3125_15 = 3.5F * local3126_9;
               float local3125_16 = local3125_13 + local3125_12 * 2.0F;
               float local3125_17 = local3125_12 + local3125_14 + local3125_12 + local3125_15 + local3125_12;
               float local3125_18 = local3126_17 - local3125_16;
               float local3125_19 = Math.max(4.0F * local3126_9, local3126_18 - local3125_17) + (1.0F - local3126_16) * 5.0F * local3126_9;
               if (local3126_15) {
                  settingRenderContext.processData7(-100000.0F, -100000.0F, 200000.0F, 200000.0F, null, () -> settingRenderContext.object = null);
               }

               Render2D.pushAlpha(local3126_16);
               Render2D.drawBlurSmooth(
                  local3125_18 + 0.4F * local3126_9,
                  local3125_19 + 0.4F * local3126_9,
                  local3125_16 - 0.8F * local3126_9,
                  local3125_17 - 0.8F * local3126_9,
                  4.1F * local3126_9,
                  15.0F,
                  1.0F,
                  -1
               );
               Render2D.drawRect(local3125_18, local3125_19, local3125_16, local3125_17, 4.5F * local3126_9, ClickGuiStyle.intVal32);
               Render2D.drawOutline(local3125_18, local3125_19, local3125_16, local3125_17, 4.5F * local3126_9, 0.5F, ClickGuiStyle.intVal33);
               float local3125_20 = local3125_18 + local3125_12;
               float local3125_21 = local3125_19 + local3125_12;
               int local3125_22 = computeInt(Color.HSBtoRGB(local3125_11, 1.0F, 1.0F));
               Render2D.drawGradientRect(
                  local3125_20,
                  local3125_21,
                  local3125_13,
                  local3125_14,
                  2.5F * local3126_9,
                  UiTheme.intVal,
                  local3125_22,
                  local3125_22,
                  UiTheme.intVal
               );
               Render2D.drawGradientRect(local3125_20, local3125_21, local3125_13, local3125_14, 2.5F * local3126_9, 0, 0, -16777216, -16777216);
               Render2D.drawOutline(local3125_20, local3125_21, local3125_13, local3125_14, 2.5F * local3126_9, 0.5F, 1023410176);
               float local3125_23 = local3125_20 + local3125_9 * local3125_13;
               float local3125_24 = local3125_21 + (1.0F - local3125_10) * local3125_14;
               Render2D.drawCircle(
                  local3125_23, local3125_24, 2.2F * local3126_9, computeInt(Color.HSBtoRGB(local3125_11, local3125_9, local3125_10))
               );
               Render2D.drawCircleOutline(local3125_23, local3125_24, 2.6F * local3126_9, 0.8F * local3126_9, UiTheme.intVal);
               float local3125_25 = local3125_21 + local3125_14 + local3125_12;
               Render2D.drawHueSlider(local3125_20, local3125_25, local3125_13, local3125_15, 1.0F);
               float local3125_26 = local3125_20 + local3125_11 * local3125_13;
               Render2D.drawCircle(local3125_26, local3125_25 + local3125_15 / 2.0F, 2.2F * local3126_9, local3125_22);
               Render2D.drawCircleOutline(
                  local3125_26, local3125_25 + local3125_15 / 2.0F, 2.6F * local3126_9, 0.8F * local3126_9, UiTheme.intVal
               );
               Render2D.popAlpha();
               if (local3126_15) {
                  settingRenderContext.processData7(local3125_20, local3125_21, local3125_13, local3125_14, null, () -> {
                     settingRenderContext.colorSetting = currentColorSetting;
                     settingRenderContext.intVal2 = 0;
                  });
                  settingRenderContext.processData7(local3125_20, local3125_25 - 2.0F * local3126_9, local3125_13, local3125_15 + 4.0F * local3126_9, null, () -> {
                     settingRenderContext.colorSetting = currentColorSetting;
                     settingRenderContext.intVal2 = 1;
                  });
                  settingRenderContext.map8
                     .put(
                        currentColorSetting,
                        new float[]{local3125_20, local3125_21, local3125_13, local3125_14, local3125_20, local3125_25, local3125_13, local3125_15}
                     );
               }
            }
         );
      return 7.0F * local3126_9;
   }

   @Override
   public boolean supports(Setting<?> local3130_1) {
      return local3130_1 instanceof ColorSetting;
   }

   private static int computeInt(int local3131_0) {
      return 0xFF000000 | local3131_0 & 16777215;
   }
}
