package greed.client.ui.setting;

import greed.client.ui.ClickGuiStyle;
import greed.client.render.FontText;
import greed.client.ui.UiTheme;

import greed.client.render.FontFace;
import greed.client.setting.MultiChoiceSetting;
import greed.client.render.Render2D;
import greed.client.setting.Setting;

public final class MultiChoiceSettingRenderer implements SettingRenderer {
   @Override
   public boolean supports(Setting<?> local1725_1) {
      return local1725_1 instanceof MultiChoiceSetting;
   }

   @Override
   public float render(
      SettingRenderContext settingRenderContext, Setting<?> local1727_2, float local1727_3, float local1727_4, float local1727_5, float local1727_6, float[] floatArray
   ) {
      MultiChoiceSetting multiChoiceSetting = (MultiChoiceSetting)local1727_2;
      float local1727_9 = settingRenderContext.scale;
      settingRenderContext.processData15(
         multiChoiceSetting.name,
         local1727_4,
         local1727_5,
         6.0F * local1727_9,
         UiTheme.computeInt2(ClickGuiStyle.intVal10, ClickGuiStyle.intVal11, local1727_3),
         local1727_6 - 8.0F * local1727_9
      );
      settingRenderContext.processData17(multiChoiceSetting.description, local1727_4, local1727_5 - 1.0F * local1727_9, local1727_6 * 0.6F, 8.0F * local1727_9);
      FontText.processData3(
         FontFace.SF,
         "Выбрано: " + multiChoiceSetting.getSelectedCount(),
         local1727_4,
         local1727_5 + 7.5F * local1727_9,
         5.5F * local1727_9,
         UiTheme.computeInt2(ClickGuiStyle.intVal13, ClickGuiStyle.intVal14, local1727_3)
      );
      float local1727_10 = settingRenderContext.computeFloat7("mspop:" + System.identityHashCode(multiChoiceSetting), settingRenderContext.object == multiChoiceSetting ? 1.0F : 0.0F);
      float local1727_11 = local1727_4 + local1727_6 - 2.2F * local1727_9;
      float local1727_12 = local1727_5 + 6.0F * local1727_9;
      Render2D.drawImageRotated(
         "assets/greed/textures/gui/clickgui/chevright.png",
         local1727_11 - 2.75F * local1727_9,
         local1727_12 - 2.75F * local1727_9,
         5.5F * local1727_9,
         0.0F,
         180.0F - local1727_10 * 180.0F,
         local1727_11,
         local1727_12,
         UiTheme.computeInt2(ClickGuiStyle.intVal8, ClickGuiStyle.intVal9, local1727_3)
      );
      settingRenderContext.processData7(
         local1727_4,
         local1727_5 - 1.5F * local1727_9,
         local1727_6,
         15.0F * local1727_9,
         floatArray,
         () -> settingRenderContext.object = settingRenderContext.object == multiChoiceSetting ? null : multiChoiceSetting
      );
      if (local1727_10 > 0.01F) {
         SettingOptionPopup.processData(
            settingRenderContext,
            local1727_4 + local1727_6,
            local1727_5,
            multiChoiceSetting.getOptions(),
            multiChoiceSetting::isIndexSelected,
            multiChoiceSetting::toggleIndex,
            false,
            local1727_10,
            settingRenderContext.object == multiChoiceSetting
         );
      }

      return 13.5F * local1727_9;
   }

   @Override
   public float getHeight(SettingRenderContext settingRenderContext, Setting<?> local1728_2, float local1728_3) {
      return 13.5F * settingRenderContext.scale;
   }
}
