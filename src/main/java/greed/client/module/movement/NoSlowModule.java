package greed.client.module.movement;

import greed.client.event.EventListener;
import greed.client.setting.ModeSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.PlayerSlowdownEvent;
import greed.client.event.PlayerTickEvent;

import net.minecraft.item.CrossbowItem;

@ModuleInfo(name = "NoSlow", description = "Убирает замедление при использовании предметов.", category = ModuleCategory.MOVEMENT)
public final class NoSlowModule extends Module {
   private static NoSlowModule noSlowModule;
   private final ModeSetting modeSetting = this.addSetting(
      new ModeSetting("Режим", "Способ обхода замедления.", "Grim Ticks", "Vanilla", "Grim Ticks", "Арбалет")
   );
   private int intVal;
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (mc.player != null) {
         if (this.modeSetting.isSelected("Grim Ticks")) {
            if (mc.player.isUsingItem()) {
               this.intVal++;
            } else {
               this.intVal = 0;
            }
         }
      }
   };
   private final EventListener<PlayerSlowdownEvent> playerSlowdownListener = playerSlowdownEvent -> {
      if (mc.player != null) {
         mc.player.setSprinting(mc.player.input.hasForwardMovement());
         if (this.modeSetting.isSelected("Grim Ticks")) {
            if (this.intVal > 1) {
               playerSlowdownEvent.cancel();
               this.intVal = 0;
            }
         } else if (this.modeSetting.isSelected("Арбалет")) {
            if (mc.player.getMainHandStack().getItem() instanceof CrossbowItem) {
               playerSlowdownEvent.cancel();
            }
         } else if (this.modeSetting.isSelected("Vanilla")) {
            playerSlowdownEvent.cancel();
         }
      }
   };

   private NoSlowModule() {
   }

   public static NoSlowModule getInstance() {
      if (noSlowModule == null) {
         noSlowModule = new NoSlowModule();
      }

      return noSlowModule;
   }
}
