package greed.client.ui.setting;

import greed.client.ui.ClickGuiStyle;
import greed.client.render.FontText;
import greed.client.ui.UiTheme;

import greed.client.setting.ActionSetting;
import greed.client.render.FontFace;
import greed.client.render.Render2D;
import greed.client.setting.Setting;

public final class ActionSettingRenderer implements SettingRenderer {
   @Override
   public float render(
      SettingRenderContext settingRenderContext, Setting<?> local3590_2, float local3590_3, float local3590_4, float local3590_5, float local3590_6, float[] floatArray
   ) {
      ActionSetting actionSetting = (ActionSetting)local3590_2;
      float local3590_9 = settingRenderContext.scale;
      float local3590_10 = Math.max(
         18.0F * local3590_9, FontText.computeFloat(FontFace.SF, actionSetting.getButtonLabel(), 6.0F * local3590_9) + 9.5F * local3590_9
      );
      settingRenderContext.processData15(
         actionSetting.name,
         local3590_4,
         local3590_5 + 1.3F * local3590_9,
         6.0F * local3590_9,
         UiTheme.computeInt2(ClickGuiStyle.intVal10, ClickGuiStyle.intVal11, local3590_3),
         local3590_6 - local3590_10 - 4.0F * local3590_9
      );
      settingRenderContext.processData17(actionSetting.description, local3590_4, local3590_5, local3590_6 * 0.5F, 9.6F * local3590_9);
      float local3590_11 = local3590_4 + local3590_6 - local3590_10;
      float local3590_12 = settingRenderContext.computeFloat2(
         "btn:" + System.identityHashCode(actionSetting),
         UiTheme.matchesCriteria(settingRenderContext.mouseX, settingRenderContext.mouseY, local3590_11, local3590_5, local3590_10, 9.57F * local3590_9)
      );
      Render2D.drawRect(
         local3590_11,
         local3590_5,
         local3590_10,
         9.57F * local3590_9,
         2.5F * local3590_9,
         UiTheme.computeInt2(
            UiTheme.computeInt(ClickGuiStyle.intVal, 0.55F + 0.45F * local3590_3),
            UiTheme.computeInt2(ClickGuiStyle.intVal, UiTheme.intVal, 0.12F),
            local3590_12
         )
      );
      Render2D.drawOutline(local3590_11, local3590_5, local3590_10, 9.57F * local3590_9, 2.5F * local3590_9, 0.5F, ClickGuiStyle.intVal38);
      FontText.processData3(
         FontFace.SF,
         actionSetting.getButtonLabel(),
         local3590_11 + (local3590_10 - FontText.computeFloat(FontFace.SF, actionSetting.getButtonLabel(), 6.0F * local3590_9)) / 2.0F,
         local3590_5 + 1.8F * local3590_9,
         6.0F * local3590_9,
         UiTheme.intVal
      );
      settingRenderContext.processData7(local3590_11, local3590_5, local3590_10, 9.57F * local3590_9, floatArray, actionSetting::runAction);
      return 9.6F * local3590_9;
   }

   @Override
   public float getHeight(SettingRenderContext settingRenderContext, Setting<?> local3591_2, float local3591_3) {
      return 9.6F * settingRenderContext.scale;
   }

   @Override
   public boolean supports(Setting<?> local3592_1) {
      return local3592_1 instanceof ActionSetting;
   }
}
