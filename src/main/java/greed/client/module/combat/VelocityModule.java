package greed.client.module.combat;

import greed.client.setting.BooleanSetting;
import greed.client.event.EventListener;
import greed.client.event.EventPriority;
import greed.client.setting.LegacyNumberSetting;
import greed.client.setting.ModeSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.MovementInputEvent;
import greed.client.setting.MultiSelectSetting;
import greed.client.event.PacketEvent;

import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.sound.SoundEvents;

@ModuleInfo(name = "Velocity", description = "Убирает откидывание от ударов и взрывов.", category = ModuleCategory.COMBAT)
public final class VelocityModule extends Module {
   private static VelocityModule velocityModule;
   public MultiSelectSetting multiSelectSetting;
   public ModeSetting modeSetting;
   public BooleanSetting booleanSetting;
   public LegacyNumberSetting legacyNumberSetting;
   private boolean isVerticalVelocityOnly;
   private int jumpResetDelayCounter;
   private final EventListener<PacketEvent> velocityPacketListener = packetEvent -> {
      if (mc.player != null && packetEvent.isIncoming()) {
         if (packetEvent.getPacket() instanceof ExplosionS2CPacket explosionS2CPacket) {
            boolean local2249_3 = explosionS2CPacket.explosionSound().value() == SoundEvents.ENTITY_WIND_CHARGE_WIND_BURST.value();
            if (local2249_3 ? this.multiSelectSetting.matchesOption("Заряд ветра") : this.multiSelectSetting.matchesOption("Взрывы")) {
               packetEvent.cancel();
            }
         } else if (this.multiSelectSetting.matchesOption("Удары")) {
            if (packetEvent.getPacket() instanceof EntityVelocityUpdateS2CPacket entityVelocityUpdateS2CPacket && entityVelocityUpdateS2CPacket.getEntityId() == mc.player.getId()) {
               if (this.modeSetting.isSelected("Обычный")) {
                  packetEvent.cancel();
               } else {
                  this.isVerticalVelocityOnly = entityVelocityUpdateS2CPacket.getVelocity().x == 0.0 && entityVelocityUpdateS2CPacket.getVelocity().z == 0.0 && entityVelocityUpdateS2CPacket.getVelocity().y < 0.0;
               }
            }
         }
      }
   };
   @EventPriority(100)
   private final EventListener<MovementInputEvent> velocityMovementListener = movementInputEvent -> {
      if (mc.player != null
         && this.multiSelectSetting.matchesOption("Удары")
         && this.modeSetting.isSelected("JumpReset")
         && AttackAuraModule.getInstance().isEnabled()) {
         if (!mc.player.isTouchingWater() && !mc.player.isSubmergedInWater() && !mc.player.isInLava()) {
            if (mc.player.hurtTime != 9
               || !mc.player.isOnGround()
               || !mc.player.isSprinting()
               || this.isVerticalVelocityOnly
               || this.booleanSetting.isChecked() && this.jumpResetDelayCounter < (int)this.legacyNumberSetting.getValue()) {
               this.jumpResetDelayCounter++;
            } else {
               movementInputEvent.setJumping(true);
               this.jumpResetDelayCounter = 0;
            }
         }
      }
   };

   @Override
   protected void onDisable() {
      this.isVerticalVelocityOnly = false;
      this.jumpResetDelayCounter = 0;
   }

   private VelocityModule() {
      this.multiSelectSetting = this.addSetting(
         new MultiSelectSetting("Отменять", "От чего убирать откидывание.", new String[]{"Удары", "Взрывы", "Заряд ветра"}, "Удары")
      );
      this.modeSetting = this.addSetting(new ModeSetting("Тип", "Как гасить откидывание от ударов.", "Обычный", "Обычный", "JumpReset"));
      this.modeSetting.withVisibility(() -> this.multiSelectSetting.matchesOption("Удары"));
      this.booleanSetting = this.addSetting(new BooleanSetting("Прыгать с задержкой тиков", "Прыгать только после паузы между джамп-ресетами.", true));
      this.legacyNumberSetting = this.addSetting(new LegacyNumberSetting("Тиков между прыжками", "Сколько тиков ждать между джамп-ресетами.", 2.0, 0.0, 20.0, 1.0));
      this.booleanSetting.withVisibility(() -> this.multiSelectSetting.matchesOption("Удары") && this.modeSetting.isSelected("JumpReset"));
      this.legacyNumberSetting.withVisibility(() -> this.multiSelectSetting.matchesOption("Удары") && this.modeSetting.isSelected("JumpReset") && this.booleanSetting.isChecked());
   }

   public static VelocityModule getInstance() {
      if (velocityModule == null) {
         velocityModule = new VelocityModule();
      }

      return velocityModule;
   }
}
