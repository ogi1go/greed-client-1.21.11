package greed.client.ui.setting;

import greed.client.ui.ClickGuiStyle;
import greed.client.render.RectDrawData;
import greed.client.ui.UiTheme;

import greed.client.setting.BooleanSetting;
import greed.client.render.Render2D;
import greed.client.setting.Setting;

public final class BooleanSettingRenderer implements SettingRenderer {
   @Override
   public float getHeight(SettingRenderContext settingRenderContext, Setting<?> local3031_2, float local3031_3) {
      return 7.0F * settingRenderContext.scale;
   }

   @Override
   public boolean supports(Setting<?> local3032_1) {
      return local3032_1 instanceof BooleanSetting;
   }

   @Override
   public float render(
      SettingRenderContext settingRenderContext, Setting<?> local3033_2, float local3033_3, float local3033_4, float local3033_5, float local3033_6, float[] floatArray
   ) {
      BooleanSetting booleanSetting = (BooleanSetting)local3033_2;
      float local3033_9 = settingRenderContext.scale;
      float local3033_10 = settingRenderContext.computeFloat2("cb:" + System.identityHashCode(booleanSetting), booleanSetting.isChecked());
      settingRenderContext.processData15(
         booleanSetting.name,
         local3033_4,
         local3033_5,
         6.0F * local3033_9,
         UiTheme.computeInt2(ClickGuiStyle.intVal10, ClickGuiStyle.intVal11, local3033_3),
         local3033_6 - 15.0F * local3033_9
      );
      settingRenderContext.processData17(booleanSetting.description, local3033_4, local3033_5 - 1.0F * local3033_9, local3033_6 - 15.0F * local3033_9, 8.0F * local3033_9);
      float local3033_11 = 10.74F * local3033_9;
      float local3033_12 = 6.64F * local3033_9;
      float local3033_13 = local3033_4 + local3033_6 - local3033_11;
      float local3033_14 = local3033_5;
      int local3033_15 = UiTheme.computeInt2(ClickGuiStyle.intVal18, ClickGuiStyle.intVal20, local3033_3);
      int local3033_16 = UiTheme.computeInt2(ClickGuiStyle.intVal22, ClickGuiStyle.intVal, local3033_3);
      Render2D.drawRectData(
         new RectDrawData(
               local3033_13,
               local3033_14,
               local3033_11,
               local3033_12,
               local3033_12 / 2.0F,
               local3033_12 / 2.0F,
               local3033_12 / 2.0F,
               local3033_12 / 2.0F,
               Render2D.applyAlpha(UiTheme.computeInt2(local3033_15, local3033_16, local3033_10))
            )
            .withSmoothness(0.5F)
      );
      float local3033_17 = 4.88F * local3033_9;
      float local3033_18 = local3033_13 + 0.93F * local3033_9 + local3033_10 * (local3033_11 - local3033_17 - 1.86F * local3033_9);
      Render2D.drawCircleSmooth(
         local3033_18 + local3033_17 / 2.0F,
         local3033_14 + local3033_12 / 2.0F,
         local3033_17 / 2.0F,
         0.5F,
         Render2D.applyAlpha(
            UiTheme.computeInt2(
               UiTheme.computeInt2(ClickGuiStyle.intVal19, ClickGuiStyle.intVal21, local3033_3),
               UiTheme.intVal,
               local3033_10
            )
         )
      );
      settingRenderContext.processData7(
         local3033_13 - 2.0F * local3033_9,
         local3033_14 - 2.0F * local3033_9,
         local3033_11 + 4.0F * local3033_9,
         local3033_12 + 4.0F * local3033_9,
         floatArray,
         booleanSetting::toggleValue
      );
      return 7.0F * local3033_9;
   }
}
