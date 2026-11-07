package greed.client.module.render;

import greed.client.event.CameraFovEvent;
import greed.client.event.EventListener;
import greed.client.setting.ModeSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.setting.NumberSetting;

@ModuleInfo(name = "AspectRatio", description = "Меняет соотношение сторон картинки мира с плавной анимацией перехода.", category = ModuleCategory.RENDER)
public final class AspectRatioModule extends Module {
   private static AspectRatioModule aspectRatioModule;
   public ModeSetting modeSetting;
   public NumberSetting numberSetting;
   private float floatVal;
   private long timeMillis;
   private final EventListener<CameraFovEvent> cameraFovListener = cameraFovEvent -> {
      if (mc.getWindow() != null) {
         float local1414_2 = (float)mc.getWindow().getFramebufferWidth() / Math.max(1, mc.getWindow().getFramebufferHeight());

         float local1414_3 = switch (this.modeSetting.getSelectedOption()) {
            case "21:9" -> 2.3333333F;
            case "16:9" -> 1.7777778F;
            case "16:10" -> 1.6F;
            case "5:4" -> 1.25F;
            case "1:1" -> 1.0F;
            case "9:16" -> 0.5625F;
            case "Кастом" -> this.numberSetting.floatValue();
            default -> 1.3333334F;
         };
         long local1414_4 = System.currentTimeMillis();
         if (this.floatVal == 0.0F) {
            this.floatVal = local1414_2;
         }

         this.floatVal = this.floatVal + (local1414_3 - this.floatVal) * (1.0F - (float)Math.exp(-Math.min(100L, local1414_4 - this.timeMillis) / 160.0));
         this.timeMillis = local1414_4;
         cameraFovEvent.processData(this.floatVal);
         cameraFovEvent.processData(true);
      }
   };

   @Override
   protected void onEnable() {
      this.floatVal = 0.0F;
      this.timeMillis = System.currentTimeMillis();
   }

   public int computeInt(int local1415_1) {
      return this.isEnabled() && !(this.floatVal <= 0.0F) && mc.getWindow() != null
         ? Math.max(1, Math.round(mc.getWindow().getFramebufferHeight() * this.floatVal))
         : local1415_1;
   }

   private AspectRatioModule() {
      this.modeSetting = this.addSetting(
         new ModeSetting("Соотношение", "Готовые пресеты соотношения сторон.", "4:3", "21:9", "16:9", "16:10", "4:3", "5:4", "1:1", "9:16", "Кастом")
      );
      this.numberSetting = this.addSetting(
         new NumberSetting("Своё соотношение", "Ширина к высоте для пресета Кастом.")
            .withRange(0.4F, 3.2F)
            .withStep(0.01F)
            .setValue(1.78F)
            .withSuffix(":1")
      );
   }

   public static AspectRatioModule getInstance() {
      if (aspectRatioModule == null) {
         aspectRatioModule = new AspectRatioModule();
      }

      return aspectRatioModule;
   }
}
