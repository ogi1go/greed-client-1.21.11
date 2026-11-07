package greed.client.module.combat;

import greed.client.setting.BooleanSetting;
import greed.client.event.EventListener;
import greed.client.storage.FriendStore;
import greed.client.setting.LegacyNumberSetting;
import greed.client.util.MathUtils;
import greed.client.setting.ModeSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.MovementInputEvent;
import greed.client.setting.MultiSelectSetting;
import greed.client.event.PlayerTickEvent;
import greed.client.module.combat.AttackController;
import greed.client.util.EntityUtils;
import greed.client.util.MouseSensitivity;

import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(
   name = "AimAssist",
   description = "Ведёт прицел к цели в пределах FOV, на входе в снап-FOV кидает его в цель резким рывком и бьёт.",
   category = ModuleCategory.COMBAT
)
public final class AimAssistModule extends Module {
   private static AimAssistModule aimAssistModule;
   public ModeSetting assistType;
   public LegacyNumberSetting fov;
   public LegacyNumberSetting range;
   public LegacyNumberSetting horizontalSpeed;
   public LegacyNumberSetting verticalSpeed;
   public LegacyNumberSetting assistStrength;
   public LegacyNumberSetting attackRange;
   public LegacyNumberSetting snapSmoothness;
   public BooleanSetting attackTarget;
   public BooleanSetting smartCrits;
   public MultiSelectSetting targetTypes;
   public ModeSetting targetSorting;
   public ModeSetting throughBlocks;
   private LivingEntity aimTarget;
   private float currentYaw;
   private float currentPitch;
   private float targetYaw;
   private float targetPitch;
   private boolean snapPending = true;
   private long lastAttackTime;
   private final EventListener<MovementInputEvent> aimMovementListener = movementInputEvent -> {
      if (this.aimTarget != null && this.shouldAssist()) {
         if (!(EntityUtils.getDistanceToBox(this.aimTarget) >= this.range.getValue())
            && AttackController.shouldResetSprint(this.aimTarget, new float[]{this.range.getValue(), 0.0F, this.range.getValue()}, "Легитный")) {
            movementInputEvent.setForward(0.0F);
            movementInputEvent.setSideways(0.0F);
         }
      }
   };
   private final EventListener<PlayerTickEvent> aimTickListener = playerTickEvent -> {
      if (mc.player != null && mc.world != null && mc.player.isAlive()) {
         if (this.attackTarget.isChecked()
            && this.aimTarget != null
            && this.getAngleDistance(this.aimTarget) <= this.attackRange.getValue()
            && AttackController.canAttackTarget(
               this.aimTarget, true, true, this.shouldAssist(), 0L, new float[]{this.range.getValue(), 0.0F, this.range.getValue()}
            )) {
            AttackController.performAttack(this.aimTarget, null, null, Hand.MAIN_HAND, false);
         }

         if (this.aimTarget == null || !this.isValidTarget(this.aimTarget)) {
            ArrayList<LivingEntity> local1207_2 = new ArrayList<>();

            for (Entity entity : mc.world.getEntities()) {
               if (entity instanceof LivingEntity livingEntity && this.isValidTarget(livingEntity)) {
                  local1207_2.add(livingEntity);
               }
            }

            if (!local1207_2.isEmpty()) {
               local1207_2.sort(
                  this.targetSorting.isSelected("Адаптивный")
                     ? Comparator.<LivingEntity>comparingDouble(livingEntity2 -> -livingEntity2.getArmor())
                        .thenComparingDouble(livingEntity3 -> this.getAngleDistance(livingEntity3))
                        .thenComparingDouble(entity2 -> EntityUtils.getDistanceToBox(entity2))
                        .thenComparingDouble(livingEntity4 -> EntityUtils.getTotalHealth(livingEntity4))
                     : (
                        this.targetSorting.isSelected("Дистанция")
                           ? Comparator.<LivingEntity>comparingDouble(entity3 -> EntityUtils.getDistanceToBox(entity3))
                              .thenComparingDouble(livingEntity5 -> EntityUtils.getTotalHealth(livingEntity5))
                           : (
                              this.targetSorting.isSelected("Здоровье")
                                 ? Comparator.<LivingEntity>comparingDouble(livingEntity6 -> EntityUtils.getTotalHealth(livingEntity6))
                                    .thenComparingDouble(entity4 -> EntityUtils.getDistanceToBox(entity4))
                                 : Comparator.<LivingEntity>comparingDouble(livingEntity7 -> this.getAngleDistance(livingEntity7))
                                    .thenComparingDouble(entity5 -> EntityUtils.getDistanceToBox(entity5))
                                    .thenComparingDouble(livingEntity8 -> EntityUtils.getTotalHealth(livingEntity8))
                                    .thenComparingDouble(livingEntity9 -> -livingEntity9.getArmor())
                           )
                     )
               );
               this.aimTarget = local1207_2.get(0);
            } else {
               this.aimTarget = null;
            }

            this.currentYaw = this.currentPitch = 0.0F;
            this.targetYaw = this.targetPitch = 0.0F;
            this.snapPending = true;
         }
      } else {
         this.aimTarget = null;
      }
   };

   public boolean shouldAssist() {
      return this.attackTarget.isChecked() && this.smartCrits.isChecked();
   }

   @Override
   protected void onDisable() {
      this.aimTarget = null;
      this.currentYaw = this.currentPitch = 0.0F;
      this.targetYaw = this.targetPitch = 0.0F;
      this.snapPending = true;
   }

   public static AimAssistModule getInstance() {
      if (aimAssistModule == null) {
         aimAssistModule = new AimAssistModule();
      }

      return aimAssistModule;
   }

   private double getAngleDistance(LivingEntity livingEntity) {
      return Math.hypot(
         MathHelper.wrapDegrees(EntityUtils.calculateRotationsToVec(livingEntity.getEyePos(), mc.player.getEyePos())[0] - mc.player.getYaw()),
         EntityUtils.calculateRotationsToVec(livingEntity.getEyePos(), mc.player.getEyePos())[1] - mc.player.getPitch()
      );
   }

   private boolean isValidTarget(LivingEntity livingEntity) {
      if (livingEntity != mc.player && livingEntity.age >= 1 && !(livingEntity instanceof ArmorStandEntity)) {
         if (EntityUtils.getDistanceToBox(livingEntity) > this.range.getValue()) {
            return false;
         }

         if (this.getAngleDistance(livingEntity) > this.fov.getValue()) {
            return false;
         }

         if (this.throughBlocks.isSelected("Нет") && !EntityUtils.hasLineOfSight(mc.player.getEyePos(), livingEntity.getEyePos())) {
            return false;
         }

         if (this.throughBlocks.isSelected("Неполноценные") && !EntityUtils.canSeeVector(mc.player.getEyePos(), livingEntity.getEyePos())) {
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

   public void applyMouseAimAssist(double local1197_1, double local1197_3) {
      if (this.isEnabled() && this.aimTarget != null && mc.player != null && mc.world != null) {
         float local1197_5 = Math.max(1.0F, Math.min((float)(System.currentTimeMillis() - this.lastAttackTime), 100.0F)) / 50.0F;
         this.lastAttackTime = System.currentTimeMillis();
         Vec3d eyePos = mc.player.getEyePos();
         Box box = this.aimTarget
            .getBoundingBox()
            .contract(this.aimTarget.getWidth() * 0.25, this.aimTarget.getHeight() * 0.2, this.aimTarget.getWidth() * 0.25);
         Vec3d vec3d = eyePos.add(
            EntityUtils.getLookVector(mc.player.getYaw(), mc.player.getPitch()).multiply(eyePos.distanceTo(box.getCenter()))
         );
         float[] floatArray = EntityUtils.calculateRotationsToVec(
            new Vec3d(
               MathHelper.clamp(vec3d.x, box.minX, box.maxX),
               MathHelper.clamp(vec3d.y, box.minY, box.maxY),
               MathHelper.clamp(vec3d.z, box.minZ, box.maxZ)
            ),
            eyePos
         );
         float local1197_10 = MathHelper.wrapDegrees(floatArray[0] - mc.player.getYaw());
         float local1197_11 = floatArray[1] - mc.player.getPitch();
         if (Math.hypot(local1197_10, local1197_11) > this.attackRange.getValue()) {
            this.snapPending = true;
         }

         if (EntityUtils.isLookingAtEntity(this.aimTarget, this.range.getValue(), mc.player.getYaw(), mc.player.getPitch())) {
            this.snapPending = false;
         }

         float local1197_12 = MathUtils.clampUnit(
            (this.fov.getValue() - (float)Math.hypot(local1197_10, local1197_11)) / Math.max(1.0F, this.fov.getValue() * 0.25F)
         );
         if (!(local1197_12 <= 0.0F)) {
            if (this.assistType.isSelected("Мышь")) {
               float local1197_13 = (float)Math.hypot(local1197_1 * 0.15, local1197_3 * 0.15);
               float local1197_14 = local1197_13 * this.getFovProgress() * (this.assistStrength.getValue() / 100.0F) * local1197_12;
               this.targetYaw = this.targetYaw + MathHelper.clamp(local1197_10, -local1197_14, local1197_14);
               this.targetPitch = this.targetPitch + MathHelper.clamp(local1197_11, -local1197_14 * 0.6F, local1197_14 * 0.6F);
            } else {
               float local1197_13 = this.assistStrength.getValue() / 100.0F;
               float local1197_14 = MathHelper.clamp(local1197_10 * local1197_13, -this.horizontalSpeed.getValue(), this.horizontalSpeed.getValue()) * local1197_12;
               float local1197_15 = MathHelper.clamp(local1197_11 * local1197_13, -this.verticalSpeed.getValue(), this.verticalSpeed.getValue()) * local1197_12;
               this.currentYaw = this.currentYaw + (local1197_14 - this.currentYaw) * MathUtils.clampUnit(0.4F * local1197_5);
               this.currentPitch = this.currentPitch + (local1197_15 - this.currentPitch) * MathUtils.clampUnit(0.4F * local1197_5);
               this.targetYaw = this.targetYaw + MathHelper.clamp(this.currentYaw * local1197_5, -Math.abs(local1197_10), Math.abs(local1197_10));
               this.targetPitch = this.targetPitch + MathHelper.clamp(this.currentPitch * local1197_5, -Math.abs(local1197_11), Math.abs(local1197_11));
            }

            float local1197_13 = MouseSensitivity.quantizeSensitivity(this.targetYaw);
            float local1197_14 = MouseSensitivity.quantizeSensitivity(this.targetPitch);
            this.targetYaw -= local1197_13;
            this.targetPitch -= local1197_14;
            mc.player.setYaw(mc.player.getYaw() + local1197_13);
            mc.player.setPitch(MathHelper.clamp(mc.player.getPitch() + local1197_14, -90.0F, 90.0F));
            mc.player.lastYaw += local1197_13;
            mc.player.lastPitch = MathHelper.clamp(mc.player.lastPitch + local1197_14, -90.0F, 90.0F);
         }
      }
   }

   private float getFovProgress() {
      float attackCooldownProgress = mc.player.getAttackCooldownProgress(1.0F);
      double attributeValue = mc.player.getAttributeValue(EntityAttributes.ATTACK_SPEED);
      if (attributeValue > 1.6) {
         attackCooldownProgress *= (float)(1.6 / attributeValue);
      }

      float local1199_4;
      if (attributeValue < 2.0) {
         local1199_4 = 2.70158F * attackCooldownProgress * attackCooldownProgress * attackCooldownProgress - 1.70158F * attackCooldownProgress * attackCooldownProgress;
      } else if (attackCooldownProgress < 0.5F) {
         local1199_4 = 8.0F * attackCooldownProgress * attackCooldownProgress * attackCooldownProgress * attackCooldownProgress;
      } else {
         local1199_4 = 1.0F - 8.0F * (attackCooldownProgress - 1.0F) * (attackCooldownProgress - 1.0F) * (attackCooldownProgress - 1.0F) * (attackCooldownProgress - 1.0F);
      }

      return (1.0F + 29.0F * local1199_4) / 50.0F;
   }

   private AimAssistModule() {
      this.assistType = this.addSetting(
         new ModeSetting("Тип доводки", "Скорость ведёт прицел сама, Мышь доворачивает только пока ты крутишь мышкой.", "Скорость", "Скорость", "Мышь")
      );
      this.fov = this.addSetting(new LegacyNumberSetting("FOV", "Максимальный угол от прицела до цели, за которым доводки нет.", 45.0, 5.0, 180.0, 1.0));
      this.range = this.addSetting(new LegacyNumberSetting("Радиус", "На какой дистанции держать цель.", 4.0, 1.0, 8.0, 0.1));
      this.horizontalSpeed = this.addSetting(new LegacyNumberSetting("Скорость по горизонтали", "Сколько градусов за тик выбирать по яву.", 9.0, 0.5, 40.0, 0.5));
      this.verticalSpeed = this.addSetting(new LegacyNumberSetting("Скорость по вертикали", "Сколько градусов за тик выбирать по питчу.", 6.0, 0.0, 40.0, 0.5));
      this.assistStrength = this.addSetting(
         new LegacyNumberSetting(
            "Сила доводки",
            "Какую долю нужного доворота отрабатывать: в Скорость тянет прицел мягче или резче, в Мышь масштабирует твоё движение мышкой.",
            100.0,
            10.0,
            300.0,
            5.0
         )
      );
      this.attackRange = this.addSetting(
         new LegacyNumberSetting("Радиус удара", "Внутри этого угла от прицела цель считается взятой и по ней идёт удар.", 15.0, 0.0, 90.0, 1.0)
      );
      this.snapSmoothness = this.addSetting(
         new LegacyNumberSetting("Плавность снапа", "За сколько миллисекунд рывок дотягивает прицел до хитбокса. 0 — мгновенный бросок.", 60.0, 0.0, 300.0, 5.0)
      );
      this.snapSmoothness.withVisibility(() -> false);
      this.attackTarget = this.addSetting(new BooleanSetting("Бить по цели", "Бить цель по готовности кулдауна, когда она внутри радиуса удара.", true));
      this.smartCrits = this.addSetting(
         new BooleanSetting("Умные криты", "Ждать момент крита и гасить спринт перед ударом, а не бить по готовности кулдауна.", true)
      );
      this.smartCrits.withVisibility(() -> this.attackTarget.isChecked());
      this.targetTypes = this.addSetting(
         new MultiSelectSetting("Кого доводить", "Каких существ считать целью.", new String[]{"Игроки", "Голые", "Мобы"}, "Игроки", "Голые")
      );
      this.targetSorting = this.addSetting(
         new ModeSetting(
            "Сортировка целей", "По какому признаку выбирать цель из подходящих.", "Наводка", "Наводка", "Адаптивный", "Дистанция", "Здоровье"
         )
      );
      this.throughBlocks = this.addSetting(
         new ModeSetting("Доводить через блоки", "Разрешить наведение сквозь блоки.", "Нет", "Нет", "Обычные", "Неполноценные")
      );
      this.horizontalSpeed.withVisibility(() -> this.assistType.isSelected("Скорость"));
      this.verticalSpeed.withVisibility(() -> this.assistType.isSelected("Скорость"));
   }

   public LivingEntity getTarget() {
      return this.aimTarget;
   }
}
