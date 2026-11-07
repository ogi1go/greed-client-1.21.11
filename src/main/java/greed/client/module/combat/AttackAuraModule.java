package greed.client.module.combat;

import greed.client.module.combat.attackaura.HvhAuraRotation;
import greed.client.module.combat.attackaura.OreMineAuraRotation;
import greed.client.setting.BooleanSetting;
import greed.mixin.BossBarHudMixin;
import greed.client.event.EventListener;
import greed.client.storage.FriendStore;
import greed.client.setting.LegacyNumberSetting;
import greed.client.module.movement.MaceTestModule;
import greed.client.setting.ModeSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.MovementInputEvent;
import greed.client.setting.MultiSelectSetting;
import greed.client.event.PacketEvent;
import greed.client.event.PlayerTickEvent;
import greed.client.util.InventoryUtils;
import greed.client.scripting.LuaRotationScripts;
import greed.client.event.SubTickEvent;
import greed.client.setting.NumericTextSetting;
import greed.client.util.InventoryActionQueue;
import greed.client.util.EntityUtils;
import greed.client.util.ValueClamps;
import greed.client.util.InventorySlotManager;
import greed.client.RotationAngles;
import greed.client.RotationManager;
import greed.client.module.player.ServerHelperModule;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@ModuleInfo(name = "AttackAura", description = "Автоатака по ближайшей цели с человечными ротациями.", category = ModuleCategory.COMBAT)
public final class AttackAuraModule extends Module {
   private static AttackAuraModule attackAuraModule;
   public ModeSetting rotationType;
   public LegacyNumberSetting attackRange;
   public LegacyNumberSetting aimRange;
   public MultiSelectSetting targetTypes;
   public ModeSetting targetSorting;
   public ModeSetting sprintResetType;
   public BooleanSetting freeMovementCorrection;
   public ModeSetting wallAttackMode;
   public MultiSelectSetting pauseConditions;
   public MultiSelectSetting extraOptions;
   public BooleanSetting autoMace;
   public BooleanSetting maxDamageSwap;
   public NumericTextSetting swapItemName;
   private final CooldownAuraRotation cooldownAuraRotation = new CooldownAuraRotation();
   private final TimedAuraRotation timedAuraRotation = new TimedAuraRotation();
   private final ShieldBreaker shieldBreaker = new ShieldBreaker();
   private final AdaptiveAuraRotation adaptiveAuraRotation = new AdaptiveAuraRotation();
   private final PredictiveAuraRotation predictiveAuraRotation = new PredictiveAuraRotation();
   private final SmoothAuraRotation smoothAuraRotation = new SmoothAuraRotation();
   private final HvhAuraRotation hvhAuraRotation = new HvhAuraRotation();
   private final OreMineAuraRotation oreMineAuraRotation = new OreMineAuraRotation();


   private static final Pattern BOSS_TIMER_PATTERN = Pattern.compile("(\\d+)\\s*сек");
   private LivingEntity target;
   public int hitCounter;
   private boolean maceEquipped;
   private boolean maceHitPending;
   private LivingEntity maceTarget;
   private LivingEntity previousTarget;
   private int previousSlot = -1;
   private int maceSlot = -1;
   private int auraTicks;
   private boolean maceHitDone;
   private long pauseMovementUntil;
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (mc.player != null && mc.world != null) {
         if (!mc.player.isAlive()) {
            this.toggle();
         } else {
            if (System.currentTimeMillis() < this.pauseMovementUntil) {
               mc.player.setVelocity(Vec3d.ZERO);
            }

            CriticalHitUtils.update();
            this.updateAutoMace();
            if (MaceHelperModule.getInstance().isWindChargeJumping()
               && MaceHelperModule.getInstance().autoTakeMace.isChecked()
               && this.previousTarget != null
               && this.previousTarget.isAlive()
               && !this.previousTarget.isRemoved()) {
               this.target = this.previousTarget;
            } else if (this.target == null || !this.isValidTarget(this.target)) {
               this.findTarget();
            }

            if (!MaceHelperModule.getInstance().isWindChargeJumping() && this.target != null) {
               this.previousTarget = this.target;
            }

            this.updateAuraRotation();
            if (this.getRotationStrategy() == null || !this.getRotationStrategy().isRotating(this.target, (float)this.getEffectiveRange())) {
               this.performAttack();
            }
         }
      }
   };
   private final EventListener<SubTickEvent> subTickListener = mouseScrollEvent -> {
      if (this.getRotationStrategy() != null) {
         this.getRotationStrategy().applyRotation(this.target, mouseScrollEvent.getTickProgress());
      }
   };
   private final EventListener<MovementInputEvent> movementInputListener = movementInputEvent -> {
      if (mc.player != null && System.currentTimeMillis() < this.pauseMovementUntil) {
         movementInputEvent.setForward(0.0F);
         movementInputEvent.setSideways(0.0F);
         movementInputEvent.setJumping(false);
         movementInputEvent.setSneaking(false);
      } else {
         if (mc.player != null && this.freeMovementCorrection.isChecked() && RotationManager.getRotationManager().isRotating()) {
            RotationManager.correctMovementStraight(movementInputEvent, RotationAngles.getCameraYaw());
         }

         if (this.target != null) {
            if (EntityUtils.getDistanceToBox(this.target) < this.getEffectiveRange()
               && AttackController.shouldResetSprint(this.target, this.getRangeParameters(), this.sprintResetType.getSelectedOption())) {
               movementInputEvent.setJumping(false);
               movementInputEvent.setForward(0.0F);
               movementInputEvent.setSideways(0.0F);
            } else {
               if (!this.freeMovementCorrection.isChecked() && RotationManager.getRotationManager().isRotating()) {
                  RotationManager.correctMovement(
                     movementInputEvent,
                     mc.player.getYaw(),
                     MathHelper.wrapDegrees(
                        (float)Math.toDegrees(Math.atan2(this.target.getZ() - mc.player.getZ(), this.target.getX() - mc.player.getX()))
                           - 90.0F
                     )
                  );
               }
            }
         }
      }
   };
   private final EventListener<PacketEvent> packetListener = packetEvent -> {
      if (mc.player != null && packetEvent.isOutgoing()) {
         if (System.currentTimeMillis() < this.pauseMovementUntil) {
            if (packetEvent.getPacket() instanceof PositionAndOnGround || packetEvent.getPacket() instanceof Full) {
               packetEvent.cancel();
            }
         }
      }
   };

   private boolean shouldMaceSmash() {
      if (mc.player.isTouchingWater() || mc.player.isInLava()) {
         return false;
      } else if (this.maceEquipped && !mc.player.isOnGround()) {
         return true;
      } else {
         return mc.player.isOnGround() ? false : CriticalHitUtils.getEstimatedFallDistance() >= 3.5;
      }
   }

   private void updateAuraRotation() {
      if (this.target != null) {
         if (!MaceHelperModule.getInstance().isWindChargePending()) {
            boolean local1360_1 = AttackController.canAttackTarget(this.target, false, true, true, 0L, this.getRangeParameters());
            if (this.getRotationStrategy() != null) {
               this.getRotationStrategy().update(this.target, local1360_1, (float)this.getEffectiveRange());
            } else if (this.rotationType.isSelected("FunTime")) {
               this.cooldownAuraRotation.update(this.target, local1360_1, (float)this.getEffectiveRange());
            } else if (this.rotationType.isSelected("SpookyTime")) {
               if (this.isPvpArenaOrBoss()) {
                  this.shieldBreaker.update(this.target, local1360_1, (float)this.getEffectiveRange());
               } else {
                  this.timedAuraRotation.update(this.target, local1360_1, (float)this.getEffectiveRange());
               }
            } else if (this.rotationType.isSelected("HVH")) {
               hvhAuraRotation.update(this.target, local1360_1, (float)this.getEffectiveRange());
            } else if (this.rotationType.isSelected("OreMine")) {
               oreMineAuraRotation.update(this.target, local1360_1, (float)this.getEffectiveRange());
            } else if (!this.rotationType.isSelected("Legit")) {
               LuaRotationScripts.executeRotationScript(this.rotationType.getSelectedOption(), this.target, local1360_1, (float)this.getEffectiveRange());
            }
         }
      }
   }

   public float getSwingProgress() {
      return MathHelper.clamp(mc.player.handSwingTicks / this.randomRange(8.0F, 12.0F), 0.0F, 1.0F);
   }

   public boolean shouldCrit() {
      return (this.canPerformMaceSmash() || CriticalHitUtils.canPerformCrit() && mc.player != null && !mc.player.isOnGround())
         && MaceItem.shouldDealAdditionalDamage(mc.player);
   }

   private void updateAutoMace() {
      if (!InventorySlotManager.getInstance().isLocked() && !InventoryActionQueue.getInstance().hasPendingActions()) {
         if (this.autoMace.isChecked() && mc.player != null) {
            if (mc.player.isOnGround() || mc.player.getVelocity().y >= 0.0) {
               this.maceHitPending = false;
            }

            this.swapToMace();
            if (this.shouldMaceSmash()) {
               int local1366_1 = InventoryUtils.findHotbarSlot(Items.MACE);
               int local1366_2 = local1366_1 != -1 ? local1366_1 : InventoryUtils.findInventorySlot(Items.MACE);
               if (local1366_2 != -1
                  && !this.maceEquipped
                  && !this.maceHitPending
                  && !mc.player.getItemCooldownManager().isCoolingDown(mc.player.getInventory().getStack(local1366_2))) {
                  this.maceEquipped = true;
                  if (mc.player.getMainHandStack().isOf(Items.MACE)) {
                     return;
                  }

                  if (ServerHelperModule.getInstance().modeSetting.matchesOption("SpookyTime")
                     && !MaceHelperModule.getInstance().isWindChargeJumping()
                     && mc.world.isSpaceEmpty(mc.player, mc.player.getBoundingBox().stretch(0.0, -3.0, 0.0))
                     && !mc.world.isSpaceEmpty(mc.player, mc.player.getBoundingBox().stretch(0.0, -8.0, 0.0))) {
                     this.pauseMovementUntil = System.currentTimeMillis() + 200L;
                  }

                  this.maceSlot = InventoryUtils.getSelectedSlot();
                  this.previousSlot = local1366_2;
                  if (local1366_2 > 8) {
                     InventoryActionQueue.getInstance().queueSlotSwap(local1366_2, InventoryUtils.getSelectedSlot(), 1);
                  } else {
                     InventoryUtils.setSelectedSlot(local1366_2, ServerHelperModule.getInstance().modeSetting.matchesOption("SpookyTime"));
                  }
               }
            } else {
               this.restoreMaceSlot();
            }
         } else {
            this.maceTarget = null;
            this.restoreMaceSlot();
         }
      }
   }

   @Override
   protected void onDisable() {
      this.target = null;
      this.previousTarget = null;
      this.hitCounter = 0;
      this.maceHitDone = false;
      this.pauseMovementUntil = 0L;
      CriticalHitUtils.reset();
      this.restoreMaceSlot();
      this.timedAuraRotation.reset();
      this.shieldBreaker.reset();
      this.resetRotations();
      this.predictiveAuraRotation.reset();
      this.smoothAuraRotation.reset();
   }

   public void updateLegitAim() {
      if (this.isEnabled() && this.rotationType.isSelected("Legit") && this.target != null) {
         this.smoothAuraRotation.applySmoothRotation(this.target);
      }
   }

   private void restoreMaceSlot() {
      if (this.maceEquipped && mc.player != null) {
         this.maceEquipped = false;
         this.maceHitPending = true;
         if (this.previousSlot > 8) {
            InventoryActionQueue.getInstance().queueSlotSwap(this.maceSlot, this.previousSlot, 1);
         } else if (this.maceSlot != -1) {
            InventoryUtils.setSelectedSlot(this.maceSlot, ServerHelperModule.getInstance().modeSetting.matchesOption("SpookyTime"));
         }

         this.previousSlot = -1;
         this.maceSlot = -1;
      }
   }

   public LivingEntity getTarget() {
      return this.target;
   }

   public float[] getRangeParameters() {
      if (MaceHelperModule.getInstance().isWindChargeJumping()) {
         return new float[]{3.0F, 11.0F, 14.0F};
      }

      if (!this.canPerformMaceSmash() || !MaceHelperModule.getInstance().assistAuraAim.isChecked() && !MaceTestModule.getInstance().assistAuraHit.isChecked()) {
         if (this.canPerformMaceSmash()) {
            return CriticalHitUtils.getFallDistance() > 2.0 ? new float[]{3.0F, 25.0F, 28.0F} : new float[]{3.0F, 15.0F, 18.0F};
         } else {
            return this.autoMace.isChecked() && InventoryUtils.findInventorySlot(Items.MACE) != -1 && CriticalHitUtils.getFallDistance() > 2.0
               ? new float[]{
                  this.attackRange.getValue(), this.aimRange.getValue() + 10.0F, this.attackRange.getValue() + this.aimRange.getValue() + 10.0F
               }
               : new float[]{this.attackRange.getValue(), this.aimRange.getValue(), this.attackRange.getValue() + this.aimRange.getValue()};
         }
      } else {
         return CriticalHitUtils.getFallDistance() > 2.0 ? new float[]{3.0F, 21.0F, 24.0F} : new float[]{3.0F, 11.0F, 14.0F};
      }
   }

   private void performAttack() {
      if (this.target != null) {
         if (!(EntityUtils.getDistanceToBox(this.target) >= this.getEffectiveRange())) {
            if (this.pauseConditions.matchesOption("Ешь") && mc.player.isUsingItem()) {
               ItemStack activeItem = mc.player.getActiveItem();
               if (!activeItem.isEmpty() && (activeItem.get(DataComponentTypes.FOOD) != null || activeItem.get(DataComponentTypes.POTION_CONTENTS) != null)) {
                  return;
               }
            }

            if (!this.pauseConditions.matchesOption("Открыт инвентарь") || mc.currentScreen == null) {
               AttackController.updateBurstAttack(this.target, false, true, false);
               if (System.currentTimeMillis() >= this.pauseMovementUntil) {
                  if (AttackController.canAttackTarget(this.target, true, true, true, 0L, this.getRangeParameters())) {
                     if (!this.wallAttackMode.isSelected("Неполноценные") || EntityUtils.canHitEntity(this.target, this.getEffectiveRange())) {
                        if (this.wallAttackMode.isSelected("Shard")
                           && mc.player.networkHandler != null
                           && !EntityUtils.hasLineOfSight(mc.player.getEyePos(), this.target.getEyePos())) {
                           ArrayList<BlockPos> local1379_1 = new ArrayList<>();
                           Vec3d eyePos = mc.player.getEyePos();
                           Vec3d vec3d = EntityUtils.getLookVector(mc.player.getYaw(), mc.player.getPitch()).normalize();
                           double local1379_4 = this.target
                              .getBoundingBox()
                              .raycast(
                                 eyePos,
                                 eyePos.add(
                                    vec3d.multiply(Math.max(mc.player.distanceTo(this.target), this.attackRange.getValue() + 2.0F))
                                 )
                              )
                              .map(eyePos::distanceTo)
                              .orElse(EntityUtils.getDistanceToBox(this.target));

                           for (double local1379_6 = 0.0; local1379_6 <= local1379_4; local1379_6 += 0.1) {
                              BlockPos blockPos = BlockPos.ofFloored(eyePos.add(vec3d.multiply(local1379_6)));
                              if (!local1379_1.contains(blockPos)) {
                                 local1379_1.add(blockPos);
                                 if (!mc.world.getBlockState(blockPos).isAir()
                                    && !mc.world.getBlockState(blockPos).getCollisionShape(mc.world, blockPos).isEmpty()) {
                                    BlockHitResult blockHitResult = mc.world
                                       .raycast(
                                          new RaycastContext(
                                             eyePos.add(vec3d.multiply(local1379_6 - 0.2)),
                                             eyePos.add(vec3d.multiply(local1379_6 + 0.2)),
                                             ShapeType.COLLIDER,
                                             FluidHandling.NONE,
                                             mc.player
                                          )
                                       );
                                    Direction direction = blockHitResult.getType() == Type.BLOCK ? blockHitResult.getSide() : Direction.UP;
                                    mc.player
                                       .networkHandler
                                       .sendPacket(new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, blockPos, direction));
                                    mc.player
                                       .networkHandler
                                       .sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, blockPos, direction));
                                 }
                              }
                           }
                        }

                        int local1379_1 = this.maxDamageSwap.isChecked() && !this.swapItemName.getText().isBlank()
                           ? InventoryUtils.findOffhandSlot(
                              itemStack -> !itemStack.isEmpty()
                                 && itemStack.getName().getString().toLowerCase().contains(this.swapItemName.getText().toLowerCase().trim())
                           )
                           : -1;
                        if (AttackController.performAttack(
                           this.target,
                           local1379_1 == -1 ? null : () -> InventoryUtils.swapSlotWithHand(local1379_1, Hand.OFF_HAND),
                           local1379_1 == -1 ? null : () -> {
                              mc.player.currentScreenHandler.nextRevision();
                              InventoryUtils.swapSlotWithHand(local1379_1, Hand.OFF_HAND);
                           },
                           Hand.MAIN_HAND,
                           false
                        )) {
                           this.hitCounter++;
                           CriticalHitUtils.onAttack();
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private boolean canAttackEntity(LivingEntity playerEntity, float local1380_2) {
      if (playerEntity != mc.player && playerEntity.age >= 1 && !(playerEntity instanceof ArmorStandEntity)) {
         if (mc.player.squaredDistanceTo(playerEntity) > local1380_2 * local1380_2) {
            return false;
         }

         if (this.wallAttackMode.isSelected("Нет") && !EntityUtils.hasLineOfSight(mc.player.getEyePos(), playerEntity.getEyePos())) {
            return false;
         }

         if (playerEntity instanceof PlayerEntity) {
            if (FriendStore.isFriend(((PlayerEntity)playerEntity).getGameProfile().name())) {
               return false;
            }

            if (this.isBot((PlayerEntity)playerEntity)) {
               return this.targetTypes.matchesOption("Боты") && !playerEntity.isInvulnerable() && playerEntity.isAlive();
            }

            if (!this.targetTypes.matchesOption("Игроки")) {
               return false;
            }

            if (playerEntity.getArmor() == 0 && !this.targetTypes.matchesOption("Голые")) {
               return false;
            }
         } else if (!this.targetTypes.matchesOption("Мобы")) {
            return false;
         }

         return !playerEntity.isInvulnerable() && playerEntity.isAlive();
      } else {
         return false;
      }
   }

   public float randomRange(float local1383_1, float local1383_2) {
      return ValueClamps.lerpFloat(local1383_2, local1383_1, new SecureRandom().nextFloat());
   }

   private AuraRotationStrategy getRotationStrategy() {
      return switch (this.rotationType.getSelectedOption()) {
         case "Aim" -> this.adaptiveAuraRotation;
         default -> null;
      };
   }

   public boolean isSmartCrits() {
      return this.extraOptions.matchesOption("Умные криты");
   }

   private AttackAuraModule() {
      this.rotationType = this.addSetting(new ModeSetting("Тип наведения", "Стиль движения прицела к цели.", "FunTime", "FunTime", "SpookyTime", "Legit","HVH","OreMine"));
      this.attackRange = this.addSetting(new LegacyNumberSetting("Радиус удара", "На какой дистанции наносить удар.", 3.0, 3.0, 6.0, 0.1));
      this.aimRange = this.addSetting(
         new LegacyNumberSetting("Радиус прицеливания", "На сколько блоков дальше радиуса удара вести цель.", 5.0, 0.0, 10.0, 0.1)
      );
      this.targetTypes = this.addSetting(
         new MultiSelectSetting("Кого атаковать", "Каких существ считать целью.", new String[]{"Игроки", "Голые", "Мобы", "Боты"}, "Игроки", "Голые")
      );
      this.targetSorting = this.addSetting(
         new ModeSetting(
            "Сортировка целей", "По какому признаку выбирать цель из подходящих.", "Наводка", "Наводка", "Адаптивный", "Дистанция", "Здоровье"
         )
      );
      this.sprintResetType = this.addSetting(new ModeSetting("Тип спринта", "Как гасить спринт перед ударом для крита.", "Легитный", "Легитный", "Быстрый"));
      this.freeMovementCorrection = this.addSetting(new BooleanSetting("Свободная коррекция движения", "WASD ведёт туда, куда смотришь ты, а не ротация.", true));
      this.wallAttackMode = this.addSetting(
         new ModeSetting("Бить через блоки", "Разрешить наведение сквозь блоки.", "Нет", "Нет", "Обычные", "Неполноценные", "Shard")
      );
      this.pauseConditions = this.addSetting(new MultiSelectSetting("Не бить если", "Условия паузы автоатаки.", new String[]{"Ешь", "Открыт инвентарь"}));
      this.extraOptions = this.addSetting(new MultiSelectSetting("Настройки", "Дополнительные опции удара.", new String[]{"Умные криты"}));
      this.autoMace = this.addSetting(
         new BooleanSetting("Авто-булава", "Сама берёт булаву, бьёт смэшем при падении на цель и возвращает прежний предмет.", false)
      );
      this.maxDamageSwap = this.addSetting(
         new BooleanSetting("Максимальный урон", "На момент удара уводит выбранный предмет в левую руку и сразу возвращает обратно.", false)
      );
      this.swapItemName = this.addSetting(
         new NumericTextSetting("Предмет для свапа", "Часть названия предмета, который уходит в левую руку на удар.", "карател", 32)
      );
      this.swapItemName.withVisibility(() -> this.maxDamageSwap.isChecked());
      this.attackRange
         .automaticWhen(
            () -> ServerHelperModule.getInstance().isEnabled()
               && ServerHelperModule.getInstance().modeSetting.isSelected("SpookyTime")
               && this.attackRange.getValue() > 3.65F
         );
   }

   private double getAngleDistance(LivingEntity livingEntity) {
      return Math.hypot(
         MathHelper.wrapDegrees(EntityUtils.calculateRotationsToVec(livingEntity.getEyePos(), mc.player.getEyePos())[0] - mc.player.getYaw()),
         EntityUtils.calculateRotationsToVec(livingEntity.getEyePos(), mc.player.getEyePos())[1] - mc.player.getPitch()
      );
   }

   public double getEffectiveRange() {
      return mc.player != null && mc.player.getMainHandStack().isOf(Items.MACE) ? 3.0 : Math.max(3.0, this.attackRange.getValue());
   }

   private boolean isBot(PlayerEntity playerEntity) {
      if (mc.getNetworkHandler() == null) {
         return false;
      }

      PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(playerEntity.getUuid());
      String local1391_3 = playerEntity.getGameProfile().name();
      return playerListEntry == null || local1391_3 == null || !local1391_3.matches("\\w{3,16}") || !local1391_3.equals(playerListEntry.getProfile().name());
   }

   private boolean isValidTarget(LivingEntity playerEntity) {
      return this.canAttackEntity(playerEntity, this.getRangeParameters()[2]);
   }

   public static AttackAuraModule getInstance() {
      if (attackAuraModule == null) {
         attackAuraModule = new AttackAuraModule();
      }

      return attackAuraModule;
   }

   private boolean isPvpArenaOrBoss() {
      if (mc.player != null && mc.world != null) {
         ScoreboardObjective objectiveForSlot = mc.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
         if (objectiveForSlot != null) {
            String[] stringArray = objectiveForSlot.getDisplayName().getString().split("-");
            if (stringArray.length > 1 && stringArray[1].replaceAll("\\D", "").matches("70[24]")) {
               return true;
            }
         }

         if (!mc.world.getRegistryKey().getValue().getPath().equals("arenas")) {
            return false;
         }

         for (ClientBossBar clientBossBar : ((BossBarHudMixin)mc.inGameHud.getBossBarHud()).getEvents().values()) {
            Matcher matcher = BOSS_TIMER_PATTERN.matcher(clientBossBar.getName().getString().replaceAll("(?i)§[0-9a-fk-or]", ""));
            if (clientBossBar.getPercent() > 0.05F && matcher.find() && Integer.parseInt(matcher.group(1)) / clientBossBar.getPercent() < 60.0F) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private void findTarget() {
      this.target = null;
      ArrayList<LivingEntity> local1398_1 = new ArrayList<>();

      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof LivingEntity playerEntity && this.isValidTarget(playerEntity)) {
            local1398_1.add(playerEntity);
         }
      }

      if (!local1398_1.isEmpty()) {
         local1398_1.sort(
            this.targetSorting.isSelected("Адаптивный")
               ? Comparator.<LivingEntity>comparingDouble(livingEntity -> -livingEntity.getArmor())
                  .thenComparingDouble(livingEntity2 -> this.getAngleDistance(livingEntity2))
                  .thenComparingDouble(entity2 -> EntityUtils.getDistanceToBox(entity2))
                  .thenComparingDouble(livingEntity3 -> EntityUtils.getTotalHealth(livingEntity3))
               : (
                  this.targetSorting.isSelected("Дистанция")
                     ? Comparator.<LivingEntity>comparingDouble(entity3 -> EntityUtils.getDistanceToBox(entity3))
                        .thenComparingDouble(livingEntity4 -> EntityUtils.getTotalHealth(livingEntity4))
                     : (
                        this.targetSorting.isSelected("Здоровье")
                           ? Comparator.<LivingEntity>comparingDouble(livingEntity5 -> EntityUtils.getTotalHealth(livingEntity5))
                              .thenComparingDouble(entity4 -> EntityUtils.getDistanceToBox(entity4))
                           : Comparator.<LivingEntity>comparingDouble(livingEntity6 -> this.getAngleDistance(livingEntity6))
                              .thenComparingDouble(entity5 -> EntityUtils.getDistanceToBox(entity5))
                              .thenComparingDouble(livingEntity7 -> EntityUtils.getTotalHealth(livingEntity7))
                              .thenComparingDouble(livingEntity8 -> -livingEntity8.getArmor())
                     )
               )
         );
         this.target = local1398_1.get(0);
      }

      this.timedAuraRotation.reset();
      this.shieldBreaker.reset();
      this.resetRotations();
      this.predictiveAuraRotation.reset();
      this.smoothAuraRotation.reset();
   }

   private void swapToMace() {
      this.maceTarget = null;
      double local1399_1 = Double.MAX_VALUE;

      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof LivingEntity playerEntity
            && this.canAttackEntity(playerEntity, 64.0F)
            && !playerEntity.isBlocking()
            && !(Math.hypot(playerEntity.getX() - mc.player.getX(), playerEntity.getZ() - mc.player.getZ()) > 6.0)
            && mc.player.squaredDistanceTo(playerEntity) < local1399_1) {
            local1399_1 = mc.player.squaredDistanceTo(playerEntity);
            this.maceTarget = playerEntity;
         }
      }
   }

   private void resetRotations() {
      this.adaptiveAuraRotation.reset();
   }

   public boolean isMaceFalling() {
      return this.autoMace.isChecked() && this.maceEquipped
         || MaceHelperModule.getInstance().isMaceEquipped()
         || mc.player != null && mc.player.getMainHandStack().isOf(Items.MACE);
   }

   @Override
   protected void onEnable() {
      this.timedAuraRotation.reset();
      this.shieldBreaker.reset();
      this.resetRotations();
      this.predictiveAuraRotation.reset();
      this.smoothAuraRotation.reset();
   }

   public boolean canPerformMaceSmash() {
      return this.isMaceFalling() && mc.player != null && !mc.player.isOnGround();
   }
}
