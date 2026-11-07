package greed.client.module.combat;

import greed.client.setting.BooleanSetting;
import greed.client.event.EventListener;
import greed.client.util.ItemCooldownAccess;
import greed.client.setting.KeybindSetting;
import greed.mixin.LivingEntityMixin;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.MovementInputEvent;
import greed.client.event.PacketEvent;
import greed.client.event.PlayerTickEvent;
import greed.client.util.InventoryUtils;
import greed.client.util.InventoryActionQueue;
import greed.client.ui.hud.NotificationHud;
import greed.client.util.InventorySlotManager;
import greed.client.RotationAngles;
import greed.client.RotationManager;
import greed.client.RotationSpeed;

import java.util.Locale;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@ModuleInfo(name = "MaceHelper", description = "Взлёт на заряде ветра под смэш булавой по бинду.", category = ModuleCategory.COMBAT)
public final class MaceHelperModule extends Module {
   private static MaceHelperModule maceHelperModule;
   public KeybindSetting keybindSetting;
   public BooleanSetting damageBoost;
   public BooleanSetting autoTakeMace;
   public BooleanSetting assistAuraAim;
   public BooleanSetting bypassObstacleFromAbove;
   private boolean keybindPressed;
   private boolean windChargeActive;
   private boolean windChargeReady;
   private int jumpDelayTicks = -1;
   private long lastWindChargeMillis;
   private boolean shouldSwapWindCharge;
   private boolean swappedWindCharge;
   private boolean maceEquipped;
   private int originalMaceSlot = -1;
   private int originalHotbarSlot = -1;
   private boolean isPerformingMaceJump;
   private boolean maceJumpPending;
   private boolean needsMaceSwap;
   private boolean restoringSlot;
   private long cooldownTimer;
   private long groundTimer;
   private long actionTimeout;
   private long resetDelayTimer;
   private final EventListener<PlayerTickEvent> maceTickListener = playerTickEvent -> {
      if (mc.player != null && mc.interactionManager != null && mc.world != null) {
         boolean local2701_2 = this.keybindSetting.isBound() && this.keybindSetting.getBinding().isPressed(mc.getWindow().getHandle());
         if (local2701_2 && !this.keybindPressed && mc.currentScreen == null) {
            this.windChargeActive = !this.windChargeActive;
            this.actionTimeout = System.currentTimeMillis() + 1500L;
         }

         this.keybindPressed = local2701_2;
         if (!mc.player.isOnGround()) {
            this.groundTimer = 0L;
         } else if (this.groundTimer == 0L) {
            this.groundTimer = System.currentTimeMillis();
         }

         if (mc.player.isOnGround()
            && this.groundTimer != 0L
            && System.currentTimeMillis() - this.groundTimer > 500L
            && !this.windChargeReady
            && this.jumpDelayTicks < 0
            && System.currentTimeMillis() >= this.resetDelayTimer) {
            this.isPerformingMaceJump = false;
            this.maceJumpPending = false;
            this.shouldSwapWindCharge = false;
            this.swappedWindCharge = false;
            if (this.maceEquipped
               && mc.currentScreen == null
               && !InventorySlotManager.getInstance().isLocked()
               && !InventoryActionQueue.getInstance().hasPendingActions()) {
               this.restorePreviousSlot();
            }
         }

         if (this.shouldSwapWindCharge) {
            if (!mc.player.isOnGround()) {
               this.swappedWindCharge = true;
            } else if (this.swappedWindCharge) {
               this.shouldSwapWindCharge = false;
               this.swappedWindCharge = false;
            }
         }

         if (this.isPerformingMaceJump) {
            if (!mc.player.isOnGround()) {
               this.maceJumpPending = true;
            } else if (this.maceJumpPending) {
               this.isPerformingMaceJump = false;
               this.maceJumpPending = false;
               this.needsMaceSwap = false;
            }
         }

         if (this.maceEquipped) {
            if (!mc.player.isOnGround()) {
               this.restoringSlot = true;
            } else if (this.restoringSlot
               && mc.currentScreen == null
               && !InventorySlotManager.getInstance().isLocked()
               && !InventoryActionQueue.getInstance().hasPendingActions()) {
               this.restorePreviousSlot();
            }
         }

         if (this.needsMaceSwap && !InventorySlotManager.getInstance().isLocked() && !InventoryActionQueue.getInstance().hasPendingActions()) {
            this.needsMaceSwap = false;
            this.takeMace();
         }

         if (this.windChargeReady && (System.currentTimeMillis() > this.actionTimeout || InventoryUtils.findInventorySlot(Items.WIND_CHARGE) == -1)) {
            this.cancelMaceAction("бросок сорвался");
         }

         if (this.windChargeActive && System.currentTimeMillis() > this.actionTimeout) {
            this.cancelMaceAction("не вышло кинуть");
         }

         if (this.windChargeReady) {
            RotationManager.getRotationManager()
               .setRotationSimple(new RotationAngles(RotationAngles.getCameraYaw(), 90.0F), new RotationSpeed(360.0F, 360.0F, 44.0F, 44.0F));
         }

         if (this.jumpDelayTicks > 0) {
            this.jumpDelayTicks--;
         }

         if (this.windChargeActive) {
            int local2701_3 = InventoryUtils.findInventorySlot(Items.WIND_CHARGE);
            if (local2701_3 == -1) {
               this.windChargeActive = false;
               NotificationHud.notifyWithText(
                  this.category.getIcon(), "Заряд ветра", Text.literal("нет в инвентаре").setStyle(Style.EMPTY.withColor(Formatting.RED)), true
               );
            } else if (mc.player.getItemCooldownManager().isCoolingDown(mc.player.getInventory().getStack(local2701_3))) {
               this.windChargeActive = false;
               NotificationHud.notifyWithText(
                  this.category.getIcon(),
                  "Заряд ветра",
                  Text.literal("в кд ещё ")
                     .setStyle(Style.EMPTY.withColor(Formatting.GRAY))
                     .append(
                        Text.literal(
                              String.format(
                                 Locale.US,
                                 "%.1fс",
                                 ((ItemCooldownAccess)mc.player.getItemCooldownManager())
                                       .remainingCooldownTicks(mc.player.getInventory().getStack(local2701_3))
                                    / 20.0F
                              )
                           )
                           .setStyle(Style.EMPTY.withColor(Formatting.RED))
                     ),
                  true
               );
            } else if (this.bypassObstacleFromAbove.isChecked()
               && !mc.world.isSpaceEmpty(mc.player, mc.player.getBoundingBox().stretch(0.0, 3.0, 0.0))) {
               this.windChargeActive = false;
               NotificationHud.notifyWithText(
                  this.category.getIcon(), "Заряд ветра", Text.literal("сверху блоки").setStyle(Style.EMPTY.withColor(Formatting.RED)), true
               );
            } else {
               RotationManager.getRotationManager()
                  .setRotationSimple(new RotationAngles(RotationAngles.getCameraYaw(), 90.0F), new RotationSpeed(360.0F, 360.0F, 44.0F, 44.0F));
               if (!(mc.player.getPitch() < 85.0F)) {
                  if (mc.player.isOnGround() && System.currentTimeMillis() - this.groundTimer >= 40L) {
                     this.windChargeActive = false;
                     this.windChargeReady = true;
                     this.actionTimeout = System.currentTimeMillis() + 800L;
                     this.groundTimer = System.currentTimeMillis();
                     int local2701_4 = InventoryUtils.getSelectedSlot();
                     this.isPerformingMaceJump = true;
                     this.maceJumpPending = false;
                     if (this.assistAuraAim.isChecked()) {
                        this.shouldSwapWindCharge = true;
                        this.swappedWindCharge = false;
                     }

                     this.needsMaceSwap = true;
                     InventorySlotManager.getInstance().lockSlot(mc.player.getInventory().getStack(local2701_3).copy());
                  }
               }
            }
         }
      }
   };
   private final EventListener<MovementInputEvent> maceMovementListener = movementInputEvent -> {
      if (mc.player != null) {
         if (this.windChargeActive || this.windChargeReady || this.jumpDelayTicks > 0) {
            movementInputEvent.setJumping(false);
         }

         if (this.windChargeReady || this.jumpDelayTicks > 0) {
            movementInputEvent.setForward(0.0F);
            movementInputEvent.setSideways(0.0F);
         }

         if (this.jumpDelayTicks == 0) {
            this.jumpDelayTicks = -1;
            ((LivingEntityMixin)mc.player).setNoJumpDelay(0);
            movementInputEvent.setJumping(true);
         }
      }
   };
   private final EventListener<PacketEvent> macePacketListener = packetEvent -> {
      if (packetEvent.isOutgoing()
         && mc.player != null
         && System.currentTimeMillis() < this.cooldownTimer
         && packetEvent.getPacket() instanceof PlayerInteractItemC2SPacket playerInteractItemC2SPacket) {
         packetEvent.setPacket(new PlayerInteractItemC2SPacket(playerInteractItemC2SPacket.getHand(), playerInteractItemC2SPacket.getSequence(), playerInteractItemC2SPacket.getYaw(), 90.0F));
         this.cooldownTimer = 0L;
      }

      if (packetEvent.isOutgoing()
         && mc.player != null
         && packetEvent.getPacket() instanceof PlayerInteractItemC2SPacket playerInteractItemC2SPacket2
         && (mc.player.getStackInHand(playerInteractItemC2SPacket2.getHand()).isOf(Items.WIND_CHARGE) || this.windChargeReady)) {
         this.windChargeReady = false;
         this.jumpDelayTicks = 1;
         this.resetDelayTimer = System.currentTimeMillis() + 600L;
      }

      if (packetEvent.isIncoming()
         && mc.player != null
         && packetEvent.getPacket() instanceof ExplosionS2CPacket explosionS2CPacket
         && explosionS2CPacket.playerKnockback().isPresent()
         && explosionS2CPacket.center().squaredDistanceTo(mc.player.getEntityPos()) < 64.0) {
         this.resetDelayTimer = 0L;
      }

      if (packetEvent.isIncoming() && packetEvent.getPacket() instanceof PlayerPositionLookS2CPacket) {
         this.lastWindChargeMillis = System.currentTimeMillis();
      }
   };

   public boolean isMaceEquipped() {
      return this.maceEquipped;
   }

   public static MaceHelperModule getInstance() {
      if (maceHelperModule == null) {
         maceHelperModule = new MaceHelperModule();
      }

      return maceHelperModule;
   }

   private void takeMace() {
      int local2700_1 = InventoryUtils.findHotbarSlot(Items.MACE);
      int local2700_2 = local2700_1 != -1 ? local2700_1 : InventoryUtils.findInventorySlot(Items.MACE);
      if (this.autoTakeMace.isChecked() && mc.player != null && !this.maceEquipped && local2700_2 != -1) {
         if (mc.player.getItemCooldownManager().isCoolingDown(mc.player.getInventory().getStack(local2700_2))) {
            NotificationHud.notifyWithText(
               this.category.getIcon(),
               "Булава",
               Text.literal("в кд ещё ")
                  .setStyle(Style.EMPTY.withColor(Formatting.GRAY))
                  .append(
                     Text.literal(
                           String.format(
                              Locale.US,
                              "%.1fс",
                              ((ItemCooldownAccess)mc.player.getItemCooldownManager())
                                    .remainingCooldownTicks(mc.player.getInventory().getStack(local2700_2))
                                 / 20.0F
                           )
                        )
                        .setStyle(Style.EMPTY.withColor(Formatting.RED))
                  ),
               true
            );
         } else {
            this.maceEquipped = true;
            this.restoringSlot = false;
            if (!mc.player.getMainHandStack().isOf(Items.MACE)) {
               this.originalHotbarSlot = InventoryUtils.getSelectedSlot();
               this.originalMaceSlot = local2700_2;
               if (local2700_2 > 8) {
                  InventoryActionQueue.getInstance().queueSlotSwap(local2700_2, InventoryUtils.getSelectedSlot(), 2);
               } else {
                  InventoryUtils.setSelectedSlot(local2700_2, false);
               }
            }
         }
      }
   }

   private void cancelMaceAction(String local2702_1) {
      this.windChargeActive = false;
      this.windChargeReady = false;
      this.cooldownTimer = 0L;
      this.resetDelayTimer = 0L;
      if (mc.player.isOnGround()) {
         this.needsMaceSwap = false;
         this.isPerformingMaceJump = false;
         this.maceJumpPending = false;
         NotificationHud.notifyWithText(
            this.category.getIcon(), "Заряд ветра", Text.literal(local2702_1).setStyle(Style.EMPTY.withColor(Formatting.RED)), true
         );
      }
   }

   private MaceHelperModule() {
      this.keybindSetting = this.addSetting(new KeybindSetting("Кинуть заряд ветра", "Кидает заряд ветра под себя с прыжком для максимального взлёта."));
      this.damageBoost = this.addSetting(
         new BooleanSetting("Усиление урона", "Держит удар булавой до самого приземления, если падаешь на цель смэш пройдёт с максимальным падением.", true)
      );
      this.autoTakeMace = this.addSetting(
         new BooleanSetting("Автовзятие булавы", "После взлёта сама переключается на булаву и держит её до приземления.", true)
      );
      this.assistAuraAim = this.addSetting(
         new BooleanSetting("Помогать AttackAura в наведении", "После взлёта расширяет радиус прицеливания булавы до приземления.", true)
      );
      this.bypassObstacleFromAbove = this.addSetting(
         new BooleanSetting("Обходить препятствие сверху", "Не кидает, если над головой блоки и взлёт упрётся в потолок.", true)
      );
   }

   private void restorePreviousSlot() {
      if (this.maceEquipped && mc.player != null) {
         this.maceEquipped = false;
         if (this.originalMaceSlot > 8) {
            InventoryActionQueue.getInstance().queueSlotSwap(this.originalHotbarSlot, this.originalMaceSlot, 2);
         } else if (this.originalHotbarSlot != -1) {
            InventoryUtils.setSelectedSlot(this.originalHotbarSlot, false);
         }

         this.originalMaceSlot = -1;
         this.originalHotbarSlot = -1;
      }
   }

   public boolean isWindChargePending() {
      return this.windChargeActive || this.windChargeReady;
   }

   @Override
   protected void onDisable() {
      this.restorePreviousSlot();
      this.windChargeActive = false;
      this.windChargeReady = false;
      this.jumpDelayTicks = -1;
      this.shouldSwapWindCharge = false;
      this.swappedWindCharge = false;
      this.isPerformingMaceJump = false;
      this.maceJumpPending = false;
      this.needsMaceSwap = false;
      this.restoringSlot = false;
      this.cooldownTimer = 0L;
      this.groundTimer = 0L;
      this.actionTimeout = 0L;
      this.resetDelayTimer = 0L;
   }

   public boolean isWindChargeJumping() {
      return this.isPerformingMaceJump;
   }
}
