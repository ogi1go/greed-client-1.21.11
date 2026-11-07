package greed.client.module.render;

import greed.client.module.combat.AttackAuraModule;
import greed.client.setting.BooleanSetting;
import greed.client.event.EventListener;
import greed.client.event.HeldItemRenderEvent;
import greed.client.setting.LegacyNumberSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.setting.SectionSetting;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.util.Arm;

@ModuleInfo(name = "ViewModel", description = "Позиция рук в кадре и поведение вьюмодели.", category = ModuleCategory.RENDER)
public final class ViewModelModule extends Module {
   private static ViewModelModule viewModelModule;
   public LegacyNumberSetting rightX;
   public LegacyNumberSetting rightY;
   public LegacyNumberSetting rightZ;
   public LegacyNumberSetting leftX;
   public LegacyNumberSetting leftY;
   public LegacyNumberSetting leftZ;
   public BooleanSetting removeUseAnimation;
   public BooleanSetting freezeHands;
   public BooleanSetting onlyWithKillaura;
   private final EventListener<HeldItemRenderEvent> heldItemRenderListener = heldItemRenderEvent -> {
      if (this.checkState() && !heldItemRenderEvent.getItemStack().isEmpty() && !heldItemRenderEvent.getItemStack().contains(DataComponentTypes.MAP_ID)) {
         this.renderElement(heldItemRenderEvent.getMatrixStack(), heldItemRenderEvent.getArm());
      }
   };

   private ViewModelModule() {
      this.addSetting(new SectionSetting("Правая рука"));
      this.rightX = this.addSetting(new LegacyNumberSetting("Правая X", "Смещение правой руки по X.", 0.0, -1.0, 1.0, 0.05));
      this.rightY = this.addSetting(new LegacyNumberSetting("Правая Y", "Смещение правой руки по Y.", 0.0, -1.0, 1.0, 0.05));
      this.rightZ = this.addSetting(new LegacyNumberSetting("Правая Z", "Смещение правой руки по Z.", 0.0, -1.0, 1.0, 0.05));
      this.addSetting(new SectionSetting("Левая рука"));
      this.leftX = this.addSetting(new LegacyNumberSetting("Левая X", "Смещение левой руки по X.", 0.0, -1.0, 1.0, 0.05));
      this.leftY = this.addSetting(new LegacyNumberSetting("Левая Y", "Смещение левой руки по Y.", 0.0, -1.0, 1.0, 0.05));
      this.leftZ = this.addSetting(new LegacyNumberSetting("Левая Z", "Смещение левой руки по Z.", 0.0, -1.0, 1.0, 0.05));
      this.removeUseAnimation = this.addSetting(new BooleanSetting("Убрать анимацию юза", "Убирает тряску предмета при еде и питье.", false));
      this.freezeHands = this.addSetting(new BooleanSetting("Не двигать руки", "Отключает отставание рук при повороте камеры.", false));
      this.onlyWithKillaura = this.addSetting(new BooleanSetting("Только с киллаурой", "Смещения рук работают только при включённой AttackAura.", false));
   }

   public boolean checkState() {
      return !this.onlyWithKillaura.isChecked() || AttackAuraModule.getInstance().isEnabled();
   }

   public void renderElement(MatrixStack matrixStack, Arm arm) {
      if (arm == Arm.RIGHT) {
         matrixStack.translate(this.rightX.getValue(), this.rightY.getValue(), this.rightZ.getValue());
      } else {
         matrixStack.translate(-this.leftX.getValue(), this.leftY.getValue(), this.leftZ.getValue());
      }
   }

   public static ViewModelModule getInstance() {
      if (viewModelModule == null) {
         viewModelModule = new ViewModelModule();
      }

      return viewModelModule;
   }

   public boolean checkState2() {
      return this.isEnabled() && this.removeUseAnimation.isChecked();
   }

   public boolean checkState3() {
      return this.isEnabled() && this.freezeHands.isChecked();
   }
}
