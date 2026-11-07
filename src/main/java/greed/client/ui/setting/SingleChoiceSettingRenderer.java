package greed.client.ui.setting;

import greed.client.ui.ClickGuiStyle;
import greed.client.render.FontText;
import greed.client.ui.UiTheme;

import greed.client.render.FontFace;
import greed.client.render.Render2D;
import greed.client.setting.Setting;
import greed.client.setting.SingleChoiceSetting;

import java.util.List;

public final class SingleChoiceSettingRenderer implements SettingRenderer {
   @Override
   public boolean supports(Setting<?> local678_1) {
      return local678_1 instanceof SingleChoiceSetting;
   }

   private float computeFloat(SettingRenderContext settingRenderContext, SingleChoiceSetting singleChoiceSetting, int local679_3, float local679_4) {
      float local679_5 = settingRenderContext.computeFloat2("seg:" + System.identityHashCode(singleChoiceSetting) + ":" + local679_3, singleChoiceSetting.getSelectedIndex() == local679_3);
      return Math.min(
         local679_4,
         FontText.computeFloat(FontFace.SF, singleChoiceSetting.getOptions().get(local679_3), 6.0F * settingRenderContext.scale)
            + 8.6F * settingRenderContext.scale
            + local679_5 * 6.2F * settingRenderContext.scale
      );
   }

   @Override
   public float render(
      SettingRenderContext settingRenderContext, Setting<?> local680_2, float local680_3, float local680_4, float local680_5, float local680_6, float[] floatArray
   ) {
      SingleChoiceSetting singleChoiceSetting = (SingleChoiceSetting)local680_2;
      float local680_9 = settingRenderContext.scale;
      float local680_10 = Math.min(FontText.computeFloat(FontFace.SF, singleChoiceSetting.getSelectedOption(), 6.0F * local680_9), local680_6 * 0.5F);
      settingRenderContext.processData15(
         singleChoiceSetting.name,
         local680_4,
         local680_5,
         6.0F * local680_9,
         UiTheme.computeInt2(ClickGuiStyle.intVal10, ClickGuiStyle.intVal11, local680_3),
         local680_6 - local680_10 - 4.0F * local680_9
      );
      settingRenderContext.processData17(singleChoiceSetting.description, local680_4, local680_5 - 1.0F * local680_9, local680_6 - local680_10 - 4.0F * local680_9, 8.0F * local680_9);
      settingRenderContext.processData15(
         singleChoiceSetting.getSelectedOption(),
         local680_4 + local680_6 - local680_10,
         local680_5,
         6.0F * local680_9,
         UiTheme.computeInt2(ClickGuiStyle.intVal28, ClickGuiStyle.intVal27, local680_3),
         local680_6 * 0.5F
      );
      float local680_11 = local680_4;
      float local680_12 = local680_5 + 9.55F * local680_9;
      List<String> options = singleChoiceSetting.getOptions();

      for (int index = 0; index < options.size(); index++) {
         float local680_15 = this.computeFloat(settingRenderContext, singleChoiceSetting, index, local680_6);
         if (local680_11 + local680_15 > local680_4 + local680_6 + 0.01F && local680_11 > local680_4) {
            local680_11 = local680_4;
            local680_12 += 11.320001F * local680_9;
         }

         float local680_16 = settingRenderContext.computeFloat2("seg:" + System.identityHashCode(singleChoiceSetting) + ":" + index, singleChoiceSetting.getSelectedIndex() == index);
         Render2D.drawRect(
            local680_11,
            local680_12,
            local680_15,
            9.77F * local680_9,
            2.0F * local680_9,
            UiTheme.computeInt2(ClickGuiStyle.intVal25, ClickGuiStyle.intVal23, local680_16)
         );
         Render2D.drawOutline(
            local680_11,
            local680_12,
            local680_15,
            9.77F * local680_9,
            2.0F * local680_9,
            0.5F,
            UiTheme.computeInt2(ClickGuiStyle.intVal26, ClickGuiStyle.intVal24, local680_16)
         );
         if (local680_16 > 0.01F) {
            Render2D.drawImageSized(
               "assets/greed/textures/gui/clickgui/check.png",
               local680_11 + 3.9F * local680_9,
               local680_12 + (9.77F * local680_9 - 4.7F * local680_9) / 2.0F,
               4.7F * local680_9,
               4.7F * local680_9,
               0.0F,
               UiTheme.computeInt(ClickGuiStyle.intVal27, local680_16)
            );
         }

         settingRenderContext.processData15(
            options.get(index),
            local680_11 + 4.3F * local680_9 + local680_16 * 6.2F * local680_9,
            local680_12 + 1.4F * local680_9,
            6.0F * local680_9,
            UiTheme.computeInt2(ClickGuiStyle.intVal28, ClickGuiStyle.intVal27, local680_16),
            local680_15 - 8.6F * local680_9 - local680_16 * 6.2F * local680_9
         );
         int index2 = index;
         settingRenderContext.processData7(local680_11, local680_12, local680_15, 9.77F * local680_9, floatArray, () -> singleChoiceSetting.selectIndex(index2));
         local680_11 += local680_15 + 1.55F * local680_9;
      }

      return 7.0F * local680_9 + 2.55F * local680_9 + this.computeFloat2(settingRenderContext, singleChoiceSetting, local680_6);
   }

   @Override
   public float getHeight(SettingRenderContext settingRenderContext, Setting<?> singleChoiceSetting, float local682_3) {
      return 7.0F * settingRenderContext.scale + 2.55F * settingRenderContext.scale + this.computeFloat2(settingRenderContext, (SingleChoiceSetting)singleChoiceSetting, local682_3);
   }

   private float computeFloat2(SettingRenderContext settingRenderContext, SingleChoiceSetting singleChoiceSetting, float local683_3) {
      float local683_4 = settingRenderContext.scale;
      float local683_5 = 1.0F;
      float local683_6 = 0.0F;

      for (int index = 0; index < singleChoiceSetting.getOptions().size(); index++) {
         if (local683_6 + this.computeFloat(settingRenderContext, singleChoiceSetting, index, local683_3) > local683_3 + 0.01F && local683_6 > 0.0F) {
            local683_5++;
            local683_6 = 0.0F;
         }

         local683_6 += this.computeFloat(settingRenderContext, singleChoiceSetting, index, local683_3) + 1.55F * local683_4;
      }

      return local683_5 * 9.77F * local683_4 + (local683_5 - 1.0F) * 1.55F * local683_4;
   }
}
