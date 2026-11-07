package greed.client.module.render;

import greed.client.module.combat.AttackAuraModule;
import greed.client.setting.BooleanSetting;
import greed.client.render.HandOutlineRenderer;
import greed.client.setting.LegacyNumberSetting;
import greed.client.setting.ModeSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.ui.UiTheme;
import greed.client.setting.ColorSetting;
import greed.client.setting.SectionSetting;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;

@ModuleInfo(name = "SwordAnimations", description = "Позволяет изменять анимацию удара.", category = ModuleCategory.RENDER)
public final class SwordAnimationsModule extends Module {
   private static SwordAnimationsModule swordAnimationsModule;
   public ModeSetting modeSetting = this.addSetting(
           new ModeSetting("Тип", "Вариант анимации удара.", "Мод 1", "Нету", "Мод 1", "Мод 2", "Мод 3", "Мод 4", "Мод 5", "Мод 6", "Мод 7", "Мод 8")
   );
   public ModeSetting shaderMode;
   public LegacyNumberSetting legacyNumberSetting = this.addSetting(new LegacyNumberSetting("Размер правой руки", "Масштаб предмета в правой руке.", 1.0, 0.5, 1.5, 0.05));
   public LegacyNumberSetting legacyNumberSetting2 = this.addSetting(new LegacyNumberSetting("Размер левой руки", "Масштаб предмета в левой руке.", 1.0, 0.5, 1.5, 0.05));
   public LegacyNumberSetting legacyNumberSetting3 = this.addSetting(new LegacyNumberSetting("Сила наклона", "Амплитуда наклона предмета при ударе.", 3.0, 1.0, 10.0, 1.0));
   public LegacyNumberSetting legacyNumberSetting4 = this.addSetting(new LegacyNumberSetting("Сила", "Амплитуда провала руки вниз.", 15.0, 1.0, 15.0, 0.05));
   public LegacyNumberSetting legacyNumberSetting5 = this.addSetting(new LegacyNumberSetting("Скорость удара", "Длительность взмаха руки в тиках.", 6.0, 3.0, 10.0, 1.0));
   public BooleanSetting booleanSetting = this.addSetting(
           new BooleanSetting("Только с аурой", "Анимация работает только при включённой AttackAura.", false)
   );
   public BooleanSetting applyShader;
   public LegacyNumberSetting outline;
   public LegacyNumberSetting fill;
   public LegacyNumberSetting highlight;
   public BooleanSetting customColor;
   public ColorSetting colorSetting;

   public int computeInt() {
      int local2653_1 = (int)this.legacyNumberSetting5.getValue();
      if (mc.player.hasStatusEffect(StatusEffects.HASTE)) {
         return local2653_1 - (1 + mc.player.getStatusEffect(StatusEffects.HASTE).getAmplifier());
      } else {
         return mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)
                 ? local2653_1 + (1 + mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) * 2
                 : local2653_1;
      }
   }

   @Override
   protected void onDisable() {
      HandOutlineRenderer.getInstance().updateState4();
   }

   private Quaternionf getQuaternionf(int local2655_1, float local2655_2) {
      return this.getRotationAxis(local2655_1).rotationDegrees(local2655_2);
   }

   public boolean checkState() {
      return !this.booleanSetting.isChecked() || AttackAuraModule.getInstance().isEnabled();
   }

   public void renderElement(MatrixStack matrixStack, int local2657_2, float local2657_3) {
      float local2657_4 = this.legacyNumberSetting3.getValue() * 10.0F;
      float local2657_5 = MathHelper.sin(local2657_3 * (float) Math.PI);
      float local2657_6 = local2657_4 / 10.0F / 10.0F * 40.0F;
      switch (this.modeSetting.getSelectedOption()) {
         case "Мод 1":
            matrixStack.translate(local2657_2 * 0.1F, 0.0F, -0.2F);
            matrixStack.translate(local2657_2 * 0.5F, -0.4F, -0.82F);
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(local2657_2 * 90.0F));
            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(local2657_2 * -60.0F));
            matrixStack.multiply(this.getRotationAxis(local2657_2).rotationDegrees(local2657_2 * (-80.0F - local2657_4 * local2657_5)));
            break;
         case "Мод 2":
            matrixStack.translate(local2657_2 * 0.56F, -0.52F, -0.72F);
            matrixStack.multiply(
                    RotationAxis.POSITIVE_Y.rotationDegrees(local2657_2 * (45.0F + MathHelper.sin(local2657_3 * local2657_3 * (float) Math.PI) / 4.0F * -20.0F))
            );
            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(local2657_2 * MathHelper.sin(MathHelper.sqrt(local2657_3) * (float) Math.PI) * -20.0F));
            matrixStack.multiply(this.getQuaternionf(local2657_2, local2657_2 * local2657_5 * -local2657_4));
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(local2657_2 * -45.0F));
            break;
         case "Мод 3":
            matrixStack.translate(local2657_2 * 0.1F, 0.0F, -0.2F);
            matrixStack.translate(local2657_2 * 0.5F, -0.4F, -0.82F);
            matrixStack.multiply(this.getQuaternionf(local2657_2, local2657_2 * -120.0F));
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(local2657_2 * (-local2657_5 * local2657_4 + 30.0F)));
            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(local2657_2 * 90.0F));
            break;
         case "Мод 4":
            matrixStack.translate(local2657_2 * 0.1F, 0.0F, -0.2F);
            matrixStack.translate(local2657_2 * 0.5F, -0.4F, -0.82F);
            matrixStack.multiply(this.getQuaternionf(local2657_2, local2657_2 * -120.0F));
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(local2657_2 * -local2657_5 * local2657_4));
            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(local2657_2 * 90.0F));
            break;
         case "Мод 5":
            matrixStack.translate(local2657_2 * 0.3F, 0.0F, -0.3F);
            matrixStack.translate(local2657_2 * 0.6F, -0.6F, -0.9F);
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(local2657_2 * 45.0F));
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(local2657_2 * local2657_5 * -(local2657_6 / 2.0F)));
            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(local2657_2 * local2657_5 * -(local2657_6 / 2.0F)));
            matrixStack.multiply(this.getQuaternionf(local2657_2, local2657_2 * local2657_5 * -local2657_6));
            matrixStack.translate(local2657_2 * -0.5F, 0.2F, 0.0F);
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(local2657_2 * 30.0F));
            matrixStack.multiply(this.getQuaternionf(local2657_2, local2657_2 * -80.0F));
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(local2657_2 * 60.0F));
            break;
         case "Мод 6":
            matrixStack.translate(local2657_2 * 0.3F, 0.0F, -0.3F);
            matrixStack.translate(local2657_2 * 0.6F, -0.6F, -0.9F);
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(local2657_2 * 45.0F));
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(local2657_2 * local2657_5 * local2657_6));
            matrixStack.multiply(this.getQuaternionf(local2657_2, local2657_2 * local2657_5 * -local2657_6));
            matrixStack.translate(local2657_2 * -0.5F, 0.2F, 0.0F);
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(local2657_2 * 30.0F));
            matrixStack.multiply(this.getQuaternionf(local2657_2, local2657_2 * -80.0F));
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(local2657_2 * 60.0F));
            break;
         case "Мод 7":
            matrixStack.translate(local2657_2 * 0.56F, -0.52F, -0.72F);
            matrixStack.translate(
                    0.0F, MathHelper.lerp(local2657_5, 0.0F, -0.8F) * MathHelper.clamp((this.legacyNumberSetting4.getValue() - 1.0F) / 14.0F, 0.0F, 1.0F), 0.0F
            );
            break;
         case "Мод 8":
            matrixStack.translate(local2657_2 * 0.72F, -0.5F, -1.0F);
            matrixStack.multiply(this.getQuaternionf(local2657_2, local2657_2 * 20.0F));
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(local2657_2 * -90.0F));
            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(local2657_2 * 60.0F));
            matrixStack.multiply(this.getQuaternionf(local2657_2, local2657_2 * 30.0F));
            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(local2657_2 * local2657_4 * 0.3F));
            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(local2657_2 * local2657_4 * local2657_5));
      }
   }

   public float computeFloat(Arm arm) {
      return !this.isEnabled() ? 1.0F : (arm == Arm.RIGHT ? this.legacyNumberSetting : this.legacyNumberSetting2).getValue();
   }

   public boolean checkState2() {
      return this.isEnabled() && !this.checkState3();
   }

   public boolean checkState3() {
      return this.modeSetting.isSelected("Нету");
   }

   private RotationAxis getRotationAxis(int local2662_1) {
      return local2662_1 == -1 ? RotationAxis.NEGATIVE_X : RotationAxis.POSITIVE_X;
   }

   public boolean checkState4() {
      return this.isEnabled() && this.applyShader.isChecked();
   }

   public int computeInt2() {
      return this.customColor.isChecked() ? this.colorSetting.getValue() : UiTheme.intVal7;
   }

   public boolean matchesCriteria(Arm arm) {
      return this.isEnabled() && !this.checkState3() && this.checkState() && mc.player != null && arm == mc.player.getMainArm();
   }

   public static SwordAnimationsModule getInstance() {
      if (swordAnimationsModule == null) {
         swordAnimationsModule = new SwordAnimationsModule();
      }

      return swordAnimationsModule;
   }

   private SwordAnimationsModule() {
      this.addSetting(new SectionSetting("Применить шейдер"));
      this.applyShader = this.addSetting(new BooleanSetting("Применить шейдер", "Обводка и тень вокруг предмета в руке.", false));
      this.shaderMode = this.addSetting(new ModeSetting("Тип шейдера", "Вариант отрисовки эффекта на руке.", "Обводка", "Обводка", "Красивый"));
      this.outline = this.addSetting(new LegacyNumberSetting("Оутлайн", "Сила обводки.", 0.8, 0.0, 1.0, 0.05));
      this.fill = this.addSetting(new LegacyNumberSetting("Заливка", "Заливка предмета цветом.", 0.0, 0.0, 1.0, 0.05));
      this.highlight = this.addSetting(new LegacyNumberSetting("Подсветка", "Сила свечения вокруг предмета.", 0.55, 0.0, 3.0, 0.05));
      this.customColor = this.addSetting(new BooleanSetting("Кастомный цвет", "Свой цвет вместо акцента темы.", false));
      this.colorSetting = this.addSetting(new ColorSetting("Цвет", "Цвет обводки и тени."));
      this.legacyNumberSetting3.withVisibility(() -> !this.modeSetting.isSelected("Мод 7") && !this.checkState3());
      this.legacyNumberSetting5.withVisibility(() -> !this.checkState3());
      this.legacyNumberSetting4.withVisibility(() -> this.modeSetting.isSelected("Мод 7"));
      this.shaderMode.withVisibility(() -> this.applyShader.isChecked());
      this.outline.withVisibility(() -> this.applyShader.isChecked());
      this.fill.withVisibility(() -> this.applyShader.isChecked());
      this.highlight.withVisibility(() -> this.applyShader.isChecked());
      this.customColor.withVisibility(() -> this.applyShader.isChecked());
      this.colorSetting.withVisibility(() -> this.applyShader.isChecked() && this.customColor.isChecked());
   }
}