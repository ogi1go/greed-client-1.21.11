package greed.client.ui;

import greed.client.render.FontFace;
import greed.client.setting.KeybindSetting;
import greed.client.render.FontText;
import greed.client.ui.setting.SettingRenderer;
import greed.client.ui.UiTheme;
import greed.client.ui.ClickGuiStyle;
import greed.client.ui.setting.SettingRenderContext;
import greed.client.render.Render2D;
import greed.client.setting.Setting;

public final class KeybindSettingWidget implements SettingRenderer {
   @Override
   public boolean supports(Setting<?> local6178_1) {
      return local6178_1 instanceof KeybindSetting;
   }

   @Override
   public float render(
      SettingRenderContext settingRenderContext, Setting<?> local6179_2, float local6179_3, float local6179_4, float local6179_5, float local6179_6, float[] floatArray
   ) {
      KeybindSetting currentKeybindSetting = (KeybindSetting)local6179_2;
      float local6179_9 = settingRenderContext.scale;
      String local6179_10 = settingRenderContext.keybindSetting == currentKeybindSetting ? "Нажми клавишу" : (currentKeybindSetting.isBound() ? currentKeybindSetting.getKeyName() : "—");
      float local6179_11 = FontText.computeFloat(FontFace.SF, local6179_10, 6.0F * local6179_9)
         + (settingRenderContext.keybindSetting == currentKeybindSetting ? 6.6F * local6179_9 : 0.0F);
      settingRenderContext.processData15(
         currentKeybindSetting.name,
         local6179_4,
         local6179_5,
         6.0F * local6179_9,
         UiTheme.computeInt2(ClickGuiStyle.intVal10, ClickGuiStyle.intVal11, local6179_3),
         local6179_6 - local6179_11 - 4.0F * local6179_9
      );
      settingRenderContext.processData17(currentKeybindSetting.description, local6179_4, local6179_5 - 1.0F * local6179_9, local6179_6 * 0.5F, 8.0F * local6179_9);
      if (settingRenderContext.keybindSetting == currentKeybindSetting) {
         String local6179_13 = "Нажми клавишу";
         float local6179_12 = FontText.computeFloat(FontFace.SF, local6179_13, 6.0F * local6179_9);
         FontText.processData3(
            FontFace.SF,
            local6179_13,
            local6179_4 + local6179_6 - local6179_12,
            local6179_5,
            6.0F * local6179_9,
            UiTheme.computeInt2(ClickGuiStyle.intVal13, ClickGuiStyle.intVal14, local6179_3)
         );
         Render2D.drawImageSized(
            "assets/greed/textures/gui/clickgui/bindkey.png",
            local6179_4 + local6179_6 - local6179_12 - 6.6F * local6179_9,
            local6179_5 + 0.4F * local6179_9,
            5.1F * local6179_9,
            5.1F * local6179_9,
            0.0F,
            UiTheme.computeInt2(ClickGuiStyle.intVal13, ClickGuiStyle.intVal14, local6179_3)
         );
      } else if (currentKeybindSetting.isBound()) {
         float local6179_12 = FontText.computeFloat(FontFace.SF, currentKeybindSetting.getKeyName(), 6.0F * local6179_9);
         FontText.processData3(
            FontFace.SF,
            currentKeybindSetting.getKeyName(),
            local6179_4 + local6179_6 - local6179_12,
            local6179_5,
            6.0F * local6179_9,
            UiTheme.computeInt2(ClickGuiStyle.intVal12, ClickGuiStyle.intVal, local6179_3)
         );
      } else {
         float local6179_12 = FontText.computeFloat(FontFace.SF, "—", 6.0F * local6179_9);
         FontText.processData3(
            FontFace.SF,
            "—",
            local6179_4 + local6179_6 - local6179_12,
            local6179_5,
            6.0F * local6179_9,
            UiTheme.computeInt2(ClickGuiStyle.intVal13, ClickGuiStyle.intVal14, local6179_3)
         );
      }

      settingRenderContext.processData6(
         local6179_4 + local6179_6 / 2.0F,
         local6179_5 - 1.5F * local6179_9,
         local6179_6 / 2.0F,
         9.0F * local6179_9,
         floatArray,
         () -> settingRenderContext.keybindSetting = currentKeybindSetting,
         () -> currentKeybindSetting.withKeyCode(-1)
      );
      return 7.0F * local6179_9;
   }

   @Override
   public float getHeight(SettingRenderContext settingRenderContext, Setting<?> local6180_2, float local6180_3) {
      return 7.0F * settingRenderContext.scale;
   }
}
