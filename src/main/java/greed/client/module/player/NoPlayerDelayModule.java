package greed.client.module.player;

import greed.client.event.EventListener;
import greed.mixin.LivingEntityMixin;
import greed.mixin.MinecraftClientMixin3;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.setting.MultiSelectSetting;
import greed.client.event.PlayerTickEvent;

import net.minecraft.item.BlockItem;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.GlassBottleItem;

@ModuleInfo(name = "NoPlayerDelay", description = "Убирает выбранные задержки действий игрока.", category = ModuleCategory.PLAYER)
public final class NoPlayerDelayModule extends Module {
   private static NoPlayerDelayModule noPlayerDelayModule;
   public MultiSelectSetting multiSelectSetting;
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (mc.player != null) {
         if (this.multiSelectSetting.matchesOption("Прыжок")) {
            ((LivingEntityMixin)mc.player).setNoJumpDelay(0);
         }

         if (mc.player.getMainHandStack().getItem() instanceof BlockItem && this.multiSelectSetting.matchesOption("Ставка блоков")
            || mc.player.getMainHandStack().getItem() instanceof ExperienceBottleItem && this.multiSelectSetting.matchesOption("Пузырьки опыта")
            || mc.player.getMainHandStack().getItem() instanceof GlassBottleItem && this.multiSelectSetting.matchesOption("Бутыльки без воды")) {
            ((MinecraftClientMixin3)mc).setRightClickDelay(0);
         }
      }
   };

   private NoPlayerDelayModule() {
      this.multiSelectSetting = this.addSetting(
         new MultiSelectSetting("Задержки", "Где отключать задержку.", new String[]{"Ставка блоков", "Прыжок", "Пузырьки опыта", "Бутыльки без воды"})
      );
   }

   public static NoPlayerDelayModule getInstance() {
      if (noPlayerDelayModule == null) {
         noPlayerDelayModule = new NoPlayerDelayModule();
      }

      return noPlayerDelayModule;
   }
}
