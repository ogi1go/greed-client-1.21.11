package greed.client.ui.setting;

import greed.client.ui.KeybindSettingWidget;
import greed.client.setting.Setting;
import greed.client.ui.TextSettingWidget;

public final class SettingRendererRegistry {
   private static final SettingRenderer[] recoveredClass0164Array = new SettingRenderer[]{
      new BooleanSettingRenderer(),
      new NumberSettingRenderer(),
      new MultiChoiceSettingRenderer(),
      new SingleChoiceSettingRenderer(),
      new ColorSettingRenderer(),
      new KeybindSettingWidget(),
      new TextSettingWidget(),
      new ActionSettingRenderer()
   };

   private static SettingRenderer getSettingRenderer(Setting<?> local1274_0) {
      for (SettingRenderer settingRenderer : recoveredClass0164Array) {
         if (settingRenderer.supports(local1274_0)) {
            return settingRenderer;
         }
      }

      return null;
   }

   public static float computeFloat(
      SettingRenderContext settingRenderContext, Setting<?> local1275_1, float local1275_2, float local1275_3, float local1275_4, float local1275_5, float[] floatArray
   ) {
      SettingRenderer settingRenderer = getSettingRenderer(local1275_1);
      return settingRenderer != null
         ? settingRenderer.render(settingRenderContext, local1275_1, local1275_2, local1275_3, local1275_4, local1275_5, floatArray)
         : Math.max(settingRenderContext.computeFloat6(local1275_1.description, local1275_5), 14.0F * settingRenderContext.scale);
   }

   public static float computeFloat2(SettingRenderContext settingRenderContext, Setting<?> local1276_1, float local1276_2) {
      SettingRenderer settingRenderer = getSettingRenderer(local1276_1);
      return settingRenderer != null
         ? settingRenderer.getHeight(settingRenderContext, local1276_1, local1276_2)
         : Math.max(settingRenderContext.computeFloat6(local1276_1.description, local1276_2), 14.0F * settingRenderContext.scale);
   }

   private SettingRendererRegistry() {
   }
}
