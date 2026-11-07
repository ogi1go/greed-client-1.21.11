package greed.client.module.combat;

import greed.client.util.EntityUtils;
import greed.client.util.InventoryUtils;

import greed.client.module.combat.AimAssistModule;
import greed.client.module.movement.AirStuckModule;
import greed.client.module.combat.AttackAuraModule;
import greed.client.module.combat.MaceHelperModule;
import greed.client.util.MathUtils;
import greed.client.MinecraftAccess;
import greed.client.util.Stopwatch;
import greed.client.module.combat.TriggerBotModule;

import java.util.Arrays;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public final class AttackController implements MinecraftAccess {
   private static final Stopwatch stopwatch = new Stopwatch();
   private static int ticksSinceLastAttack;
   private static long totalAttackCount;
   private static boolean isBurstAttacking;
   private static int remainingBurstHits;
   private static float attackJitter = MathUtils.getSecureRandom().nextBoolean() ? 0.0F : 0.1F;

   public static Stopwatch getStopwatch() {
      return stopwatch;
   }

   public static boolean canAttackNow() {
      return hasAttackCooldown(0L);
   }

   public static boolean performAttack(LivingEntity livingEntity, Runnable runnable, Runnable runnable2, Hand hand, boolean local1584_4) {
      if (livingEntity != null && mc.interactionManager != null) {
         if (runnable != null) {
            runnable.run();
         }

         mc.interactionManager.attackEntity(mc.player, livingEntity);
         if (local1584_4) {
            incrementAttackCount();
         } else {
            resetAttackCount();
         }

         ticksSinceLastAttack = 0;
         attackJitter = MathUtils.getSecureRandom().nextBoolean() ? 0.0F : 0.1F;
         if (hand != null) {
            mc.player.swingHand(hand);
         }

         if (runnable2 != null) {
            runnable2.run();
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean isRandomAttackTick() {
      return totalAttackCount % 7L == 3L;
   }

   public static boolean isWithinDistance(LivingEntity livingEntity, float local1587_1) {
      return livingEntity != null && mc.player != null ? mc.player.squaredDistanceTo(livingEntity) <= local1587_1 * local1587_1 : true;
   }

   public static void incrementAttackTicks() {
      ticksSinceLastAttack++;
   }

   public static void resetAttackCount() {
      totalAttackCount = 0L;
   }

   public static float getAttackChargeRatio() {
      return Math.min(ticksSinceLastAttack / 10.0F, 1.0F);
   }

   public static long getAttackCooldownTimeMillis() {
      return ticksSinceLastAttack * 50L;
   }

   public static void resetBurstAttack() {
      isBurstAttacking = false;
      remainingBurstHits = 0;
   }

   public static boolean canAttackTarget(
      LivingEntity livingEntity, boolean local1593_1, boolean local1593_2, boolean local1593_3, long local1593_4, float[] floatArray
   ) {
      float local1593_7 = floatArray.length > 2 ? floatArray[2] : floatArray[0];
      if (local1593_2 && livingEntity != null && !isWithinDistance(livingEntity, local1593_7)) {
         return false;
      }

      if (!hasAttackCooldown(local1593_4)) {
         return false;
      }

      boolean local1593_8 = canPerformCriticalHit(local1593_3);
      if (local1593_8 && local1593_1 && !isLookingAtTarget(livingEntity, floatArray[0])) {
         local1593_8 = false;
      }

      return local1593_8;
   }

   private static int getMaxBurstHits() {
      return 3;
   }

   public static double calculateFallDistanceToGround(int local1596_0) {
      if (mc.world == null) {
         return 1.0;
      }

      Vec3d eyePos = mc.player.getEyePos();
      double local1596_2 = local1596_0 * 2.0;
      double local1596_4 = 255.0;
      double local1596_6 = -64.0;
      float local1596_8 = mc.player.getWidth() / 2.0F - 0.01F;

      for (Vec3d vec3d : Arrays.asList(
         eyePos.add(-local1596_8, 0.0, -local1596_8),
         eyePos.add(local1596_8, 0.0, local1596_8),
         eyePos.add(local1596_8, 0.0, -local1596_8),
         eyePos.add(-local1596_8, 0.0, local1596_8)
      )) {
         double local1596_11 = mc.world
            .raycast(new RaycastContext(vec3d, vec3d.add(0.0, -local1596_0, 0.0), ShapeType.VISUAL, FluidHandling.ANY, mc.player))
            .getPos()
            .y;
         double local1596_13 = mc.world
            .raycast(new RaycastContext(vec3d, vec3d.add(0.0, local1596_0, 0.0), ShapeType.VISUAL, FluidHandling.ANY, mc.player))
            .getPos()
            .y;
         if (local1596_4 > local1596_13) {
            local1596_4 = local1596_13;
         }

         if (local1596_6 < local1596_11) {
            local1596_6 = local1596_11;
         }

         if (local1596_2 > local1596_4 - local1596_6) {
            local1596_2 = local1596_4 - local1596_6;
         }
      }

      return local1596_2 - mc.player.getHeight();
   }

   public static void incrementAttackCount() {
      totalAttackCount++;
   }

   public static double getEffectiveFallDistance() {
      double local1599_0 = mc.player.fallDistance;
      if (mc.world != null
         && !mc.player.isOnGround()
         && mc.player.getVelocity().y < -0.0784000015258789
         && !mc.world.getBlockState(mc.player.getBlockPos()).getFluidState().isIn(FluidTags.WATER)
         && !mc.world.getBlockState(mc.player.getBlockPos().up()).getFluidState().isIn(FluidTags.WATER)) {
         local1599_0 = -mc.player.getVelocity().y;
      }

      return local1599_0;
   }

   public static boolean canAttackTargetSimple(LivingEntity livingEntity, boolean local1600_1, boolean local1600_2, long local1600_3, float[] floatArray) {
      return canAttackTarget(livingEntity, local1600_1, true, local1600_2, local1600_3, floatArray);
   }

   public static boolean isLookingAtTarget(LivingEntity entity, double local1601_1) {
      return entity != null && EntityUtils.isLookingAtEntity(entity, local1601_1, mc.player.getYaw(), mc.player.getPitch());
   }

   public static void updateBurstAttack(LivingEntity livingEntity, boolean local1602_1, boolean local1602_2, boolean local1602_3) {
      if (livingEntity == null || remainingBurstHits == 0 || !local1602_3 || livingEntity.hurtTime != 0) {
         resetBurstAttack();
      }

      if (local1602_3
         && livingEntity != null
         && !(livingEntity instanceof PlayerEntity)
         && !mc.player.isUsingItem()
         && hasElapsedMillis(isRandomAttackTick() ? 250L : 150L)
         && mc.player.handSwinging) {
         if (!isBurstAttacking && remainingBurstHits == 0 && livingEntity.hurtTime == 0) {
            isBurstAttacking = true;
            remainingBurstHits = getMaxBurstHits();
         }

         if (isBurstAttacking
            && remainingBurstHits > 0
            && (!local1602_2 || isLookingAtTarget(livingEntity, 6.0))
            && performAttack(livingEntity, () -> {}, () -> {}, Hand.MAIN_HAND, local1602_1)) {
            remainingBurstHits--;
         }
      }
   }

   private static LivingEntity getCurrentCombatTarget() {
      return AttackAuraModule.getInstance().getTarget() != null ? AttackAuraModule.getInstance().getTarget() : TriggerBotModule.getInstance().getTarget();
   }

   private static boolean isMaceOrCritReady() {
      return AttackAuraModule.getInstance().isMaceFalling() || TriggerBotModule.getInstance().isMaceEquippedOrPending() || CriticalHitUtils.canPerformCrit();
   }

   public static boolean canPerformCriticalHit(boolean local1605_0) {
      if (isMaceOrCritReady()
         && mc.player != null
         && !mc.player.isOnGround()
         && InventoryUtils.findInventorySlot(Items.MACE) != -1
         && !mc.player.getItemCooldownManager().isCoolingDown(mc.player.getInventory().getStack(InventoryUtils.findInventorySlot(Items.MACE)))) {
         if (CriticalHitUtils.isInvalidCritTarget(getCurrentCombatTarget())) {
            return false;
         }

         if (MaceItem.shouldDealAdditionalDamage(mc.player) && mc.player.getVelocity().y < 0.0) {
            return true;
         }
      }

      if (!local1605_0) {
         return true;
      }

      boolean local1605_1 = AttackAuraModule.getInstance().isEnabled() && AttackAuraModule.getInstance().isSmartCrits()
         || TriggerBotModule.getInstance().isEnabled() && TriggerBotModule.getInstance().isSmartCrits()
         || AimAssistModule.getInstance().isEnabled() && AimAssistModule.getInstance().shouldAssist();
      FluidState fluidState = mc.world.getFluidState(BlockPos.ofFloored(mc.player.getEyePos()));
      boolean local1605_3 = !mc.player.hasStatusEffect(StatusEffects.LEVITATION)
         && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
         && !fluidState.isIn(FluidTags.WATER)
         && !fluidState.isIn(FluidTags.LAVA)
         && !mc.player.getAbilities().flying
         && !mc.player.isGliding()
         && !mc.player.isClimbing()
         && !mc.player.hasVehicle();
      return local1605_3 && mc.player.fallDistance > (AirStuckModule.getInstance().isFrozenInAir() ? 0.0 : getEffectiveFallDistance()) && !mc.player.isOnGround()
         || local1605_1 && mc.player.isOnGround() && !mc.player.input.playerInput.jump()
         || !local1605_3;
   }

   private static boolean isDamageBoostEnabled() {
      return MaceHelperModule.getInstance().damageBoost.isChecked();
   }

   private AttackController() {
   }

   public static boolean hasAttackCooldown(long local1608_0) {
      if (mc.player == null) {
         return false;
      } else if (AttackAuraModule.getInstance().shouldCrit() || TriggerBotModule.getInstance().canMaceSmash()) {
         return !mc.player.getItemCooldownManager().isCoolingDown(mc.player.getMainHandStack());
      } else if (mc.player.fallDistance > 1.5) {
         return !mc.player.getItemCooldownManager().isCoolingDown(mc.player.getMainHandStack()) && ticksSinceLastAttack > 3L + local1608_0 / 50L;
      } else {
         return mc.player.getMainHandStack().isOf(Items.MACE)
            ? !mc.player.getItemCooldownManager().isCoolingDown(mc.player.getMainHandStack())
               && mc.player.getAttackCooldownProgress(0.5F + (float)Math.max(0L, -local1608_0) / 50.0F) >= 0.9F
            : mc.player.getAttackCooldownProgress(0.5F + (float)Math.max(0L, -local1608_0) / 50.0F) >= 0.9F && ticksSinceLastAttack >= 10L + local1608_0 / 50L;
      }
   }

   public static boolean shouldResetSprint(LivingEntity livingEntity, float[] floatArray, String local1609_2) {
      if ("Быстрый".equals(local1609_2)) {
         if (livingEntity != null
            && canAttackTarget(livingEntity, false, false, false, -50L, floatArray)
            && !mc.player.isOnGround()
            && !mc.player.isSubmergedInWater()
            && mc.player.getVelocity().y <= 0.0030162615090425808) {
            return true;
         }
      } else if ("Легитный".equals(local1609_2)
         && livingEntity != null
         && canAttackTarget(livingEntity, false, false, false, -150L, floatArray)
         && !mc.player.isOnGround()
         && !mc.player.isSubmergedInWater()
         && mc.player.getVelocity().y <= 0.16477328182606651) {
         return true;
      }

      return false;
   }

   public static boolean hasElapsedMillis(long local1610_0) {
      return ticksSinceLastAttack * 50L >= local1610_0;
   }
}
