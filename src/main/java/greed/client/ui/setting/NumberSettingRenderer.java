package greed.client.ui.setting;

import greed.client.ui.ClickGuiStyle;
import greed.client.render.FontText;
import greed.client.ui.UiTheme;

import greed.client.render.FontFace;
import greed.client.setting.NumberSetting;
import greed.client.render.Render2D;
import greed.client.setting.Setting;

public final class NumberSettingRenderer implements SettingRenderer {
   @Override
   public float render(
      SettingRenderContext settingRenderContext, Setting<?> local577_2, float local577_3, float local577_4, float local577_5, float local577_6, float[] floatArray
   ) {
      NumberSetting currentNumberSetting = (NumberSetting)local577_2;
      float local577_9 = settingRenderContext.scale;
      String displayValue = currentNumberSetting.getDisplayValue();
      settingRenderContext.processData15(
         currentNumberSetting.name,
         local577_4,
         local577_5,
         6.0F * local577_9,
         UiTheme.computeInt2(ClickGuiStyle.intVal10, ClickGuiStyle.intVal11, local577_3),
         local577_6 - FontText.computeFloat(FontFace.SF, displayValue, 6.0F * local577_9) - 4.0F * local577_9
      );
      settingRenderContext.processData17(currentNumberSetting.description, local577_4, local577_5 - 1.0F * local577_9, local577_6 * 0.6F, 8.0F * local577_9);
      FontText.processData3(
         FontFace.SF,
         displayValue,
         local577_4 + local577_6 - FontText.computeFloat(FontFace.SF, displayValue, 6.0F * local577_9),
         local577_5,
         6.0F * local577_9,
         UiTheme.computeInt2(
            UiTheme.computeInt2(ClickGuiStyle.intVal12, ClickGuiStyle.intVal, local577_3),
            -43691,
            settingRenderContext.computeFloat7("sliderDanger:" + System.identityHashCode(currentNumberSetting), currentNumberSetting.isAutomatic() ? 1.0F : 0.0F)
         )
      );
      float local577_11 = local577_5 + 9.65F * local577_9;
      float local577_12 = 1.63F * local577_9;
      Render2D.drawRect(
         local577_4,
         local577_11,
         local577_6,
         local577_12,
         local577_12 / 2.0F,
         UiTheme.computeInt2(ClickGuiStyle.intVal29, ClickGuiStyle.intVal31, local577_3)
      );
      float local577_13 = settingRenderContext.computeFloat7("slider:" + System.identityHashCode(currentNumberSetting), currentNumberSetting.getFraction());
      float local577_14 = Math.max(3.0F * local577_9, local577_13 * local577_6);
      Render2D.drawRect(
         local577_4,
         local577_11,
         local577_14,
         local577_12,
         local577_12 / 2.0F,
         UiTheme.computeInt2(ClickGuiStyle.intVal30, ClickGuiStyle.intVal, local577_3)
      );
      float local577_15 = 2.97F * local577_9;
      Render2D.drawRect(
         local577_4 + local577_14 - local577_15 / 2.0F - 0.5F * local577_9,
         local577_11 + local577_12 / 2.0F - local577_15 / 2.0F,
         local577_15,
         local577_15,
         local577_15 / 2.0F,
         UiTheme.intVal
      );
      settingRenderContext.map7.put(currentNumberSetting, new float[]{local577_4, local577_6});
      settingRenderContext.processData6(
         local577_4,
         local577_11 - 2.5F * local577_9,
         local577_6,
         local577_12 + 5.0F * local577_9,
         floatArray,
         () -> settingRenderContext.numberSetting = currentNumberSetting,
         () -> currentNumberSetting.setValue(settingRenderContext.getT(currentNumberSetting, currentNumberSetting.floatValue()))
      );
      return 13.3F * local577_9;
   }

   @Override
   public float getHeight(SettingRenderContext settingRenderContext, Setting<?> local579_2, float local579_3) {
      return 13.3F * settingRenderContext.scale;
   }

   @Override
   public boolean supports(Setting<?> local581_1) {
      return local581_1 instanceof NumberSetting;
   }
}
