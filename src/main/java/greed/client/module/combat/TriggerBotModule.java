package greed.client.module.combat;

import greed.client.setting.BooleanSetting;
import greed.client.event.EventListener;
import greed.client.storage.FriendStore;
import greed.client.setting.LegacyNumberSetting;
import greed.client.setting.ModeSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.MovementInputEvent;
import greed.client.setting.MultiSelectSetting;
import greed.client.event.PacketEvent;
import greed.client.event.PlayerTickEvent;
import greed.client.event.PostAttackEvent;
import greed.client.util.InventoryUtils;
import greed.client.module.combat.CriticalHitUtils;
import greed.client.module.combat.AttackController;
import greed.client.util.InventoryActionQueue;
import greed.client.util.EntityUtils;
import greed.client.util.InventorySlotManager;
import greed.client.module.player.ServerHelperModule;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(
   name = "TriggerBot",
   description = "Бьёт того, на кого уже наведён прицел, таймингами ауры и без своего наведения.",
   category = ModuleCategory.COMBAT
)
public final class TriggerBotModule extends Module {
   private static TriggerBotModule triggerBotModule;
   public LegacyNumberSetting attackRange;
   public MultiSelectSetting targetTypes;
   public ModeSetting wallAttackMode;
   public MultiSelectSetting pauseConditions;
   public MultiSelectSetting extraOptions;
   public BooleanSetting autoMace;
   private LivingEntity triggerTarget;
   private boolean triggerAttackPending;
   private boolean triggerMaceEquipped;
   private int triggerPrevSlot = -1;
   private int triggerMaceSlot = -1;
   private long movementPauseDeadline;
   private final EventListener<PostAttackEvent> triggerPostAttackListener = postAttackEvent -> {
      if (mc.player != null && this.extraOptions.matchesOption("Кип спринт")) {
         mc.player.setSprinting(true);
      }
   };
   private final EventListener<PacketEvent> triggerPacketListener = packetEvent -> {
      if (mc.player != null && packetEvent.isOutgoing() && System.currentTimeMillis() < this.movementPauseDeadline) {
         if (packetEvent.getPacket() instanceof PositionAndOnGround || packetEvent.getPacket() instanceof Full) {
            packetEvent.cancel();
         }
      }
   };
   private final EventListener<MovementInputEvent> triggerMovementListener = movementInputEvent -> {
      if (mc.player != null && System.currentTimeMillis() < this.movementPauseDeadline) {
         movementInputEvent.setForward(0.0F);
         movementInputEvent.setSideways(0.0F);
         movementInputEvent.setJumping(false);
         movementInputEvent.setSneaking(false);
      } else if (this.triggerTarget != null && this.isSmartCrits()) {
         if (!(EntityUtils.getDistanceToBox(this.triggerTarget) >= this.getEffectiveRange())
            && AttackController.shouldResetSprint(this.triggerTarget, this.getRangeParameters(), "Легитный")) {
            movementInputEvent.setForward(0.0F);
            movementInputEvent.setSideways(0.0F);
         }
      }
   };
   private final EventListener<PlayerTickEvent> triggerTickListener = playerTickEvent -> {
      this.triggerTarget = null;
      if (mc.player != null && mc.world != null && mc.player.isAlive()) {
         if (System.currentTimeMillis() < this.movementPauseDeadline) {
            mc.player.setVelocity(Vec3d.ZERO);
         }

         this.updateAutoMace();
         if (!this.pauseConditions.matchesOption("Открыт инвентарь") || mc.currentScreen == null) {
            if (this.pauseConditions.matchesOption("Ешь") && mc.player.isUsingItem()) {
               ItemStack activeItem = mc.player.getActiveItem();
               if (!activeItem.isEmpty() && (activeItem.get(DataComponentTypes.FOOD) != null || activeItem.get(DataComponentTypes.POTION_CONTENTS) != null)) {
                  return;
               }
            }

            this.triggerTarget = this.findTarget();
            if (this.triggerTarget != null) {
               AttackController.updateBurstAttack(this.triggerTarget, true, true, true);
               if (AttackController.canAttackTarget(this.triggerTarget, true, true, this.isSmartCrits(), 0L, this.getRangeParameters())) {
                  AttackController.performAttack(this.triggerTarget, null, null, Hand.MAIN_HAND, false);
               }
            }
         }
      }
   };

   private TriggerBotModule() {
      this.attackRange = this.addSetting(new LegacyNumberSetting("Радиус удара", "На какой дистанции наносить удар.", 3.0, 3.0, 6.0, 0.1));
      this.targetTypes = this.addSetting(
         new MultiSelectSetting("Кого атаковать", "Каких существ считать целью.", new String[]{"Игроки", "Голые", "Мобы"}, "Игроки", "Голые")
      );
      this.wallAttackMode = this.addSetting(new ModeSetting("Бить через блоки", "Разрешить удар сквозь блоки.", "Нет", "Нет", "Обычные", "Неполноценные"));
      this.pauseConditions = this.addSetting(
         new MultiSelectSetting("Не бить если", "Условия паузы удара.", new String[]{"Ешь", "Открыт инвентарь"}, "Ешь", "Открыт инвентарь")
      );
      this.extraOptions = this.addSetting(
         new MultiSelectSetting("Настройки", "Дополнительные опции удара.", new String[]{"Умные криты", "Кип спринт"}, "Умные криты")
      );
      this.autoMace = this.addSetting(
         new BooleanSetting("Авто-булава", "Сама берёт булаву, бьёт смэшем при падении на цель и возвращает прежний предмет.", false)
      );
      this.attackRange
         .automaticWhen(
            () -> ServerHelperModule.getInstance().isEnabled()
               && ServerHelperModule.getInstance().modeSetting.isSelected("SpookyTime")
               && this.attackRange.getValue() > 3.65F
         );
   }

   public boolean isSmartCrits() {
      return this.extraOptions.matchesOption("Умные криты");
   }

   private LivingEntity findTarget() {
      LivingEntity entity = null;

      for (Entity entity2 : mc.world.getEntities()) {
         if (entity2 instanceof LivingEntity entity3
            && this.isValidTarget(entity3, this.attackRange.getValue())
            && AttackController.isLookingAtTarget(entity3, this.attackRange.getValue())
            && (entity == null || EntityUtils.getDistanceToBox(entity3) < EntityUtils.getDistanceToBox(entity))) {
            entity = entity3;
         }
      }

      return entity;
   }

   private double getEffectiveRange() {
      return mc.player != null && mc.player.getMainHandStack().isOf(Items.MACE) ? 3.0 : Math.max(3.0, this.attackRange.getValue());
   }

   public boolean isMaceFalling() {
      return this.isMaceEquippedOrPending() && mc.player != null && !mc.player.isOnGround();
   }

   public boolean canMaceSmash() {
      return this.isMaceFalling() && MaceItem.shouldDealAdditionalDamage(mc.player);
   }

   public boolean isMaceEquippedOrPending() {
      return this.autoMace.isChecked() && this.triggerAttackPending || mc.player != null && mc.player.getMainHandStack().isOf(Items.MACE);
   }

   private void restorePreMaceSlot() {
      if (this.triggerAttackPending && mc.player != null) {
         this.triggerAttackPending = false;
         this.triggerMaceEquipped = true;
         if (this.triggerPrevSlot > 8) {
            InventoryActionQueue.getInstance().queueSlotSwap(this.triggerMaceSlot, this.triggerPrevSlot, 1);
         } else if (this.triggerMaceSlot != -1) {
            InventoryUtils.setSelectedSlot(this.triggerMaceSlot, ServerHelperModule.getInstance().modeSetting.matchesOption("SpookyTime"));
         }

         this.triggerPrevSlot = -1;
         this.triggerMaceSlot = -1;
      }
   }

   private boolean isFallingTowardsTarget() {
      if (this.triggerAttackPending && !mc.player.isOnGround()) {
         return true;
      }

      if (mc.player.isOnGround()) {
         return false;
      }

      Vec3d vec3d = CriticalHitUtils.getLastAttackedPos();
      return vec3d == null
         ? mc.world.isSpaceEmpty(mc.player, mc.player.getBoundingBox().stretch(0.0, -3.5, 0.0))
         : mc.player.getY() + mc.player.getVelocity().y > vec3d.y && mc.player.getY() - vec3d.y > 3.5;
   }

   private void updateAutoMace() {
      if (!InventorySlotManager.getInstance().isLocked() && !InventoryActionQueue.getInstance().hasPendingActions()) {
         if (!this.autoMace.isChecked()) {
            this.restorePreMaceSlot();
         } else {
            if (mc.player.isOnGround() || mc.player.getVelocity().y >= 0.0) {
               this.triggerMaceEquipped = false;
            }

            if (!this.isFallingTowardsTarget()) {
               this.restorePreMaceSlot();
            } else {
               int local7752_1 = InventoryUtils.findHotbarSlot(Items.MACE);
               int local7752_2 = local7752_1 != -1 ? local7752_1 : InventoryUtils.findInventorySlot(Items.MACE);
               if (local7752_2 != -1
                  && !this.triggerAttackPending
                  && !this.triggerMaceEquipped
                  && !mc.player.getItemCooldownManager().isCoolingDown(mc.player.getInventory().getStack(local7752_2))) {
                  this.triggerAttackPending = true;
                  if (!mc.player.getMainHandStack().isOf(Items.MACE)) {
                     if (ServerHelperModule.getInstance().modeSetting.matchesOption("SpookyTime")
                        && !this.isFallingTowardsTarget()
                        && !MaceHelperModule.getInstance().isMaceEquipped()
                        && !MaceHelperModule.getInstance().isWindChargeJumping()
                        && mc.world.isSpaceEmpty(mc.player, mc.player.getBoundingBox().stretch(0.0, -3.0, 0.0))) {
                        this.movementPauseDeadline = System.currentTimeMillis() + 200L;
                     }

                     this.triggerMaceSlot = InventoryUtils.getSelectedSlot();
                     this.triggerPrevSlot = local7752_2;
                     if (local7752_2 > 8) {
                        InventoryActionQueue.getInstance().queueSlotSwap(local7752_2, InventoryUtils.getSelectedSlot(), 1);
                     } else {
                        InventoryUtils.setSelectedSlot(local7752_2, ServerHelperModule.getInstance().modeSetting.matchesOption("SpookyTime"));
                     }
                  }
               }
            }
         }
      }
   }

   public static TriggerBotModule getInstance() {
      if (triggerBotModule == null) {
         triggerBotModule = new TriggerBotModule();
      }

      return triggerBotModule;
   }

   @Override
   protected void onDisable() {
      this.triggerTarget = null;
      this.movementPauseDeadline = 0L;
      this.restorePreMaceSlot();
   }

   private float[] getRangeParameters() {
      return new float[]{(float)this.getEffectiveRange(), 0.0F, (float)this.getEffectiveRange()};
   }

   private boolean isValidTarget(LivingEntity livingEntity, float local7758_2) {
      if (livingEntity != mc.player && livingEntity.age >= 1 && !(livingEntity instanceof ArmorStandEntity)) {
         if (mc.player.squaredDistanceTo(livingEntity) > local7758_2 * local7758_2) {
            return false;
         }

         if (this.wallAttackMode.isSelected("Нет") && !EntityUtils.hasLineOfSight(mc.player.getEyePos(), livingEntity.getEyePos())) {
            return false;
         }

         if (this.wallAttackMode.isSelected("Неполноценные") && !EntityUtils.canSeeVector(mc.player.getEyePos(), livingEntity.getEyePos())) {
            return false;
         }

         if (livingEntity instanceof PlayerEntity) {
            if (FriendStore.isFriend(((PlayerEntity)livingEntity).getGameProfile().name())) {
               return false;
            }

            if (!this.targetTypes.matchesOption("Игроки")) {
               return false;
            }

            if (livingEntity.getArmor() == 0 && !this.targetTypes.matchesOption("Голые")) {
               return false;
            }
         } else if (!this.targetTypes.matchesOption("Мобы")) {
            return false;
         }

         return !livingEntity.isInvulnerable() && livingEntity.isAlive();
      } else {
         return false;
      }
   }

   public LivingEntity getTarget() {
      return this.triggerTarget;
   }
}
