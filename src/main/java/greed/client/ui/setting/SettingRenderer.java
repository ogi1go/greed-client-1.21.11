package greed.client.ui.setting;

import greed.client.setting.Setting;

public interface SettingRenderer {
   float render(SettingRenderContext settingRenderContext, Setting<?> var2, float var3, float var4, float var5, float var6, float[] floatArray);

   boolean supports(Setting<?> var1);

   float getHeight(SettingRenderContext settingRenderContext, Setting<?> var2, float var3);
}
