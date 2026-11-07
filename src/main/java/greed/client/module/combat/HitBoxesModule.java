package greed.client.module.combat;

import greed.client.setting.BooleanSetting;
import greed.client.event.EntityYawEvent;
import greed.client.event.EventListener;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.setting.NumberSetting;

@ModuleInfo(name = "HitBoxes", description = "Раздувает хитбоксы сущностей, по которым попадают удары.", category = ModuleCategory.COMBAT)
public final class HitBoxesModule extends Module {
   private static HitBoxesModule hitBoxesModule;
   public NumberSetting numberSetting;
   public BooleanSetting booleanSetting;
   public boolean isDebugRenderContext;
   public final EventListener<EntityYawEvent> hitboxListener = entityYawEvent -> {
      if (!this.isDebugRenderContext || this.booleanSetting.isChecked()) {
         if (entityYawEvent.getMargin() < this.numberSetting.floatValue()) {
            entityYawEvent.setMargin(this.numberSetting.floatValue());
         }
      }
   };

   private HitBoxesModule() {
      this.numberSetting = this.addSetting(
         new NumberSetting("Установите размер бокса", "На сколько блоков раздуть бокс по горизонтали.")
            .withRange(0.0F, 5.0F)
            .withStep(0.1F)
            .setValue(0.3F)
      );
      this.booleanSetting = this.addSetting(new BooleanSetting("Отображать в F3+B", "Рисовать раздутый бокс в дебаг-рендере хитбоксов.", true));
   }

   public static HitBoxesModule getInstance() {
      if (hitBoxesModule == null) {
         hitBoxesModule = new HitBoxesModule();
      }

      return hitBoxesModule;
   }
}
