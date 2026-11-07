package greed.client.module.render;

import greed.client.event.EventListener;
import greed.client.setting.LegacyNumberSetting;
import greed.client.setting.ModeSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.PlayerTickEvent;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

@ModuleInfo(name = "Brightness", description = "Убирает темноту: поднимает гамму мира или вешает ночное зрение.", category = ModuleCategory.RENDER)
public final class BrightnessModule extends Module {
   private static BrightnessModule brightnessModule;
   public ModeSetting modeSetting;
   public LegacyNumberSetting legacyNumberSetting;
   private boolean flag;
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (mc.player != null) {
         if (this.modeSetting.isSelected("Зелье")) {
            mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 300, 0, false, false));
            this.flag = true;
         } else if (this.flag) {
            mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            this.flag = false;
         }
      }
   };

   public static BrightnessModule getInstance() {
      if (brightnessModule == null) {
         brightnessModule = new BrightnessModule();
      }

      return brightnessModule;
   }

   @Override
   protected void onDisable() {
      if (mc.player != null && this.flag) {
         mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
      }

      this.flag = false;
   }

   private BrightnessModule() {
      this.modeSetting = this.addSetting(new ModeSetting("Освещение", "Чем светить в темноте.", "Гамма", "Гамма", "Зелье"));
      this.legacyNumberSetting = this.addSetting(new LegacyNumberSetting("Сила гаммы", "Во сколько раз поднять яркость мира.", 5.0, 1.0, 15.0, 0.1));
      this.legacyNumberSetting.withVisibility(() -> this.modeSetting.isSelected("Гамма"));
   }
}
