package greed.client.module.movement;

import greed.client.setting.BooleanSetting;
import greed.client.event.EventListener;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.PlayerTickEvent;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.UseEffectsComponent;

@ModuleInfo(name = "AutoSprint", description = "Автоспринт: не теряет скорость при ударе и умеет игнорить слепоту.", category = ModuleCategory.MOVEMENT)
public final class AutoSprintModule extends Module {
   private static AutoSprintModule autoSprintModule;
   public BooleanSetting keepSprint;
   public BooleanSetting ignoreBlindness;
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (mc.player != null) {
         mc.player
            .setSprinting(
               mc.player.input.hasForwardMovement()
                  && !mc.player.hasBlindnessEffect()
                  && (
                     mc.player.hasVehicle()
                        ? mc.player.getVehicle().canSprintAsVehicle() && mc.player.getVehicle().isLogicalSideForUpdatingMovement()
                        : mc.player.getHungerManager().canSprint() || mc.player.getAbilities().allowFlying
                  )
                  && (mc.player.getAbilities().flying || !mc.player.isPartlyTouchingWater())
                  && (
                     !mc.player.isUsingItem()
                        || mc.player.getActiveItem().getOrDefault(DataComponentTypes.USE_EFFECTS, UseEffectsComponent.DEFAULT).canSprint()
                  )
                  && (!mc.player.isGliding() || mc.player.isSubmergedInWater())
                  && (!mc.player.shouldSlowDown() || mc.player.isSubmergedInWater())
            );
      }
   };

   private AutoSprintModule() {
      this.keepSprint = this.addSetting(new BooleanSetting("Кип спринт", "Не сбрасывать скорость спринта при ударе.", false));
      this.ignoreBlindness = this.addSetting(new BooleanSetting("Игнор слепоты", "Спринтить даже под эффектом слепоты.", false));
   }

   public static AutoSprintModule getInstance() {
      if (autoSprintModule == null) {
         autoSprintModule = new AutoSprintModule();
      }

      return autoSprintModule;
   }

   public boolean checkState() {
      return this.isEnabled() && this.keepSprint.isChecked();
   }

   public boolean checkState2() {
      return this.isEnabled() && this.ignoreBlindness.isChecked();
   }
}
