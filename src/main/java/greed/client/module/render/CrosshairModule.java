package greed.client.module.render;

import greed.client.setting.BooleanSetting;
import greed.client.util.Colors;
import greed.client.event.EventListener;
import greed.client.event.HudRenderEvent;
import greed.client.setting.LegacyNumberSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.util.GuiScale;
import greed.client.setting.ColorSetting;
import greed.client.render.Render2D;

import net.minecraft.util.hit.HitResult.Type;

@ModuleInfo(
   name = "Crosshair",
   description = "Свой прицел вместо ванильного: крест с плавным кулдауном удара и краснением по цели.",
   category = ModuleCategory.RENDER
)
public final class CrosshairModule extends Module {
   private static CrosshairModule crosshairModule;
   public ColorSetting colorSetting;
   public LegacyNumberSetting gap;
   public LegacyNumberSetting length;
   public LegacyNumberSetting thickness;
   public BooleanSetting cooldown;
   public LegacyNumberSetting cooldownStrength;
   public BooleanSetting outline;
   public BooleanSetting tShapeMode;
   public BooleanSetting centerDot;
   public LegacyNumberSetting dotSize;
   private float floatVal;
   private float floatVal2;
   private final EventListener<HudRenderEvent> hudRenderListener = hudRenderEvent -> {
      if (mc.player != null
         && mc.world != null
         && mc.options.getPerspective().isFirstPerson()
         && !CameraFeatureModule.getInstance().checkState4()) {
         float local4902_2 = hudRenderEvent.computeInt2() / 2.0F * GuiScale.computeInt() / GuiScale.computeFloat();
         float local4902_3 = hudRenderEvent.computeInt() / 2.0F * GuiScale.computeInt() / GuiScale.computeFloat();
         this.floatVal = this.floatVal
            + ((this.cooldown.isChecked() ? 1.0F - mc.player.getAttackCooldownProgress(hudRenderEvent.computeFloat()) : 0.0F) - this.floatVal) * 0.3F;
         this.floatVal2 = this.floatVal2
            + ((mc.crosshairTarget != null && mc.crosshairTarget.getType() == Type.ENTITY ? 1.0F : 0.0F) - this.floatVal2) * 0.3F;
         int local4902_4 = Colors.computeInt2(this.colorSetting.getValue(), -51401, this.floatVal2);
         float local4902_5 = this.gap.getValue() + this.cooldownStrength.getValue() * this.floatVal;
         if (!this.tShapeMode.isChecked()) {
            this.processData(
               local4902_2 - this.thickness.getValue() / 2.0F,
               local4902_3 - local4902_5 - this.length.getValue(),
               this.thickness.getValue(),
               this.length.getValue(),
               local4902_4
            );
         }

         this.processData(
            local4902_2 - this.thickness.getValue() / 2.0F,
            local4902_3 + local4902_5,
            this.thickness.getValue(),
            this.length.getValue(),
            local4902_4
         );
         this.processData(
            local4902_2 - local4902_5 - this.length.getValue(),
            local4902_3 - this.thickness.getValue() / 2.0F,
            this.length.getValue(),
            this.thickness.getValue(),
            local4902_4
         );
         this.processData(
            local4902_2 + local4902_5,
            local4902_3 - this.thickness.getValue() / 2.0F,
            this.length.getValue(),
            this.thickness.getValue(),
            local4902_4
         );
         if (this.centerDot.isChecked()) {
            this.processData(
               local4902_2 - this.dotSize.getValue() / 2.0F,
               local4902_3 - this.dotSize.getValue() / 2.0F,
               this.dotSize.getValue(),
               this.dotSize.getValue(),
               local4902_4
            );
         }
      }
   };

   private CrosshairModule() {
      this.colorSetting = this.addSetting(new ColorSetting("Цвет", "Цвет прицела, при наведении на энтити краснеет."));
      this.gap = this.addSetting(new LegacyNumberSetting("Отступ", "Просвет от центра до линий.", 2.0, 0.0, 20.0, 1.0));
      this.length = this.addSetting(new LegacyNumberSetting("Длина", "Длина линий креста.", 5.0, 1.0, 20.0, 1.0));
      this.thickness = this.addSetting(new LegacyNumberSetting("Толщина", "Толщина линий.", 2.0, 1.0, 5.0, 1.0));
      this.cooldown = this.addSetting(new BooleanSetting("Кулдаун", "Показывать перезарядку удара: крест плавно расходится и сходится.", true));
      this.cooldownStrength = this.addSetting(new LegacyNumberSetting("Сила кулдауна", "Насколько крест расходится во время перезарядки.", 8.0, 1.0, 20.0, 1.0));
      this.cooldownStrength.withVisibility(() -> this.cooldown.isChecked());
      this.outline = this.addSetting(new BooleanSetting("Обводка", "Тёмная обводка вокруг линий.", true));
      this.tShapeMode = this.addSetting(new BooleanSetting("T-режим", "Не рисовать верхнюю линию.", false));
      this.centerDot = this.addSetting(new BooleanSetting("Точка в центре", "Точка в центре прицела.", false));
      this.dotSize = this.addSetting(new LegacyNumberSetting("Размер точки", "Размер центральной точки.", 2.0, 1.0, 10.0, 0.5));
      this.dotSize.withVisibility(() -> this.centerDot.isChecked());
   }

   public static CrosshairModule getInstance() {
      if (crosshairModule == null) {
         crosshairModule = new CrosshairModule();
      }

      return crosshairModule;
   }

   private void processData(float local4904_1, float local4904_2, float local4904_3, float local4904_4, int local4904_5) {
      if (this.outline.isChecked()) {
         Render2D.drawRectSharp(local4904_1 - 0.5F, local4904_2 - 0.5F, local4904_3 + 1.0F, local4904_4 + 1.0F, -1275068416);
      }

      Render2D.drawRectSharp(local4904_1, local4904_2, local4904_3, local4904_4, local4904_5);
   }
}
