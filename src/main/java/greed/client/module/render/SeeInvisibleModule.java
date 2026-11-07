package greed.client.module.render;

import greed.client.setting.LegacyNumberSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;

@ModuleInfo(name = "SeeInvisible", description = "Показывает невидимых игроков и мобов с настраиваемой прозрачностью.", category = ModuleCategory.RENDER)
public final class SeeInvisibleModule extends Module {
   private static SeeInvisibleModule seeInvisibleModule;
   public LegacyNumberSetting legacyNumberSetting = this.addSetting(new LegacyNumberSetting("Прозрачность", "Насколько плотно рисовать невидимых.", 0.55, 0.05, 1.0, 0.05));

   public static SeeInvisibleModule getInstance() {
      if (seeInvisibleModule == null) {
         seeInvisibleModule = new SeeInvisibleModule();
      }

      return seeInvisibleModule;
   }

   private SeeInvisibleModule() {
   }
}
