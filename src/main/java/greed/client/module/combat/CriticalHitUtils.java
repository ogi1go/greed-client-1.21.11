package greed.client.module.combat;

import greed.client.util.EntityUtils;
import greed.client.util.InventoryUtils;
import greed.client.module.movement.SimulatedPlayerInput;

import greed.client.module.combat.AttackAuraModule;
import greed.client.MinecraftAccess;

import java.util.Collections;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

public final class CriticalHitUtils implements MinecraftAccess {
   private static final List<VoxelShape> list = Collections.emptyList();
   private static Vec3d vec3d3;
   private static int intVal = -1;
   private static int intVal2;
   private static int intVal3;
   private static double doubleVal;
   private static int intVal4 = -1;

   public static boolean isSafeCritTarget(LivingEntity livingEntity, boolean local1312_1) {
      if (local1312_1 && !isMaceEquipped() && !isMaceSmashAvailable()) {
         Vec3d vec3d = getLastAttackedPos();
         return vec3d == null || livingEntity == null || vec3d.distanceTo(livingEntity.getEntityPos()) > 2.0;
      } else {
         return true;
      }
   }

   public static boolean canPredictCrit() {
      if (mc.player != null && !(mc.player.fallDistance <= 1.0)) {
         SimulatedPlayerInput simulatedPlayerInput = SimulatedPlayerInput.getSimulatedPlayerInput(1);
         return simulatedPlayerInput.flag4 && mc.player.fallDistance > 1.5 || simulatedPlayerInput.flag7;
      } else {
         return false;
      }
   }

   private CriticalHitUtils() {
   }

   private static double calculateSimulatedFallDistance() {
      if (mc.player == null || mc.world == null || mc.player.isOnGround()) {
         return 0.0;
      }

      if (mc.player.getAbilities().flying || mc.player.isGliding() || mc.player.hasVehicle() || mc.player.isClimbing()) {
         return 0.0;
      }

      if (!mc.player.hasStatusEffect(StatusEffects.LEVITATION) && !mc.player.isTouchingWater() && !mc.player.isInLava()) {
         SimulatedPlayerInput simulatedPlayerInput = SimulatedPlayerInput.getSimulatedPlayerInput4(SimulatedPlayerInput.SimulatedPlayerInputState.fromClientPlayer(mc.player.input.playerInput));
         double local1315_1 = mc.player.fallDistance;

         for (int index = 0; index < 60; index++) {
            simulatedPlayerInput.updateState();
            local1315_1 = Math.max(local1315_1, simulatedPlayerInput.floatVal3);
            if (simulatedPlayerInput.flag4 || simulatedPlayerInput.flag7) {
               break;
            }
         }

         return local1315_1;
      } else {
         return 0.0;
      }
   }

   public static boolean isFallingForCrit() {
      return canPerformCrit() && mc.player != null && mc.player.fallDistance > 1.3F && intVal3 <= 0;
   }

   public static float getPredictedFallDistance(int local1317_0) {
      return mc.player == null ? 0.0F : SimulatedPlayerInput.getSimulatedPlayerInput(local1317_0).floatVal3;
   }

   public static boolean hasCritDelay() {
      return intVal3 > 0;
   }

   private static boolean isInLiquid(Box box) {
      for (BlockPos blockPos : BlockPos.iterate(
         BlockPos.ofFloored(box.minX, box.minY, box.minZ), BlockPos.ofFloored(box.maxX, box.maxY, box.maxZ)
      )) {
         if (mc.world.getBlockState(blockPos).getFluidState().isIn(FluidTags.WATER)
            || mc.world.getBlockState(blockPos).getFluidState().isIn(FluidTags.LAVA)) {
            return true;
         }
      }

      return false;
   }

   public static void update() {
      if (intVal3 > 0) {
         intVal3--;
      }
   }

   public static boolean canPerformCrit() {
      if (mc.player == null) {
         return false;
      } else if (mc.player.getMainHandStack().isOf(Items.MACE)) {
         return true;
      } else {
         return mc.player.fallDistance < 1.0
            ? false
            : AttackAuraModule.getInstance().autoMace.isChecked() && InventoryUtils.findInventorySlot(Items.MACE) != -1;
      }
   }

   private static boolean matchesCriteria(Box box) {
      for (BlockPos blockPos : BlockPos.iterate(
         BlockPos.ofFloored(box.minX, box.minY, box.minZ), BlockPos.ofFloored(box.maxX, box.maxY, box.maxZ)
      )) {
         BlockState blockState = mc.world.getBlockState(blockPos);
         if (!blockState.isOf(Blocks.COBWEB) && !blockState.isOf(Blocks.SWEET_BERRY_BUSH)) {
            if (!blockState.getFluidState().isIn(FluidTags.WATER) && !blockState.getFluidState().isIn(FluidTags.LAVA)) {
               continue;
            }

            return true;
         }

         return true;
      }

      return false;
   }

   public static double getFallDistance() {
      Vec3d vec3d = getLastAttackedPos();
      return vec3d != null && mc.player != null ? mc.player.getY() - vec3d.y : -1.0;
   }

   public static boolean isMaceSmashAvailable() {
      if (mc.player != null && mc.world != null) {
         double finalGravity = mc.player.getFinalGravity();
         double local1325_2 = 0.0;
         Vec3d velocity = mc.player.getVelocity();

         for (int index = 0; index < 2; index++) {
            double local1325_6 = (velocity.y - finalGravity) * 0.9799995613681012;
            Box box = mc.player.getBoundingBox().offset(0.0, local1325_2, 0.0);
            Vec3d vec3d = Entity.adjustMovementForCollisions(
               mc.player, new Vec3d(0.0, local1325_6, 0.0), box, mc.world, list
            );
            if (local1325_6 < 0.0 && matchesCriteria(box.offset(vec3d.x, vec3d.y, vec3d.z))) {
               return true;
            }

            local1325_2 += vec3d.y;
            velocity = new Vec3d(velocity.x, vec3d.y, velocity.z);
         }

         return false;
      } else {
         return false;
      }
   }

   public static boolean isMaceEquipped() {
      if (mc.player != null && mc.world != null && !mc.player.isOnGround()) {
         double finalGravity = mc.player.getFinalGravity();
         double local1326_2 = 0.0;
         Vec3d velocity = mc.player.getVelocity();

         for (int index = 0; index < 2; index++) {
            double local1326_6 = (velocity.y - finalGravity) * 0.9799995613681012;
            Vec3d vec3d = Entity.adjustMovementForCollisions(
               mc.player,
               new Vec3d(0.0, local1326_6, 0.0),
               mc.player.getBoundingBox().offset(0.0, local1326_2, 0.0),
               mc.world,
               list
            );
            if (local1326_6 < 0.0 && (index == 0 ? vec3d.y != local1326_6 : vec3d.y > local1326_6 * 0.5)) {
               return true;
            }

            local1326_2 += vec3d.y;
            velocity = new Vec3d(velocity.x, vec3d.y, velocity.z);
         }

         return false;
      } else {
         return false;
      }
   }

   public static void reset() {
      intVal2 = 0;
      intVal3 = 0;
   }

   private static boolean matchesCriteria2(LivingEntity livingEntity) {
      if (livingEntity != null && mc.player != null) {
         double local1328_1 = livingEntity.getY() - mc.player.getY();
         return local1328_1 > 0.0 && local1328_1 <= 1.5 && livingEntity.getY() - livingEntity.lastRenderY < 0.0;
      } else {
         return false;
      }
   }

   public static double getEstimatedFallDistance() {
      if (mc.player == null) {
         return 0.0;
      }

      if (mc.player.age == intVal4) {
         return doubleVal;
      }

      intVal4 = mc.player.age;
      doubleVal = calculateSimulatedFallDistance();
      return doubleVal;
   }

   public static Vec3d getLastAttackedPos() {
      if (mc.player == null) {
         return null;
      }

      if (mc.player.age == intVal) {
         return vec3d3;
      }

      intVal = mc.player.age;
      vec3d3 = getVec3d();
      return vec3d3;
   }

   public static void onAttack() {
      if (mc.player != null && mc.player.getMainHandStack().isOf(Items.MACE)) {
         intVal3 = 20;
      }
   }

   public static boolean isInvalidCritTarget(LivingEntity livingEntity) {
      if (mc.player != null && canPerformCrit()) {
         if (getEstimatedFallDistance() <= 1.5) {
            intVal2 = 0;
            return false;
         }

         if (!canPredictCrit() && !matchesCriteria2(livingEntity)) {
            if (getPredictedFallDistance(1) >= 1.0F && !matchesCriteria3(livingEntity)) {
               intVal2 = 0;
               return false;
            } else if (++intVal2 >= 10) {
               intVal2 = 0;
               return false;
            } else {
               return true;
            }
         } else {
            intVal2 = 0;
            return false;
         }
      } else {
         intVal2 = 0;
         return false;
      }
   }

   private static boolean matchesCriteria3(LivingEntity livingEntity) {
      if (livingEntity != null && mc.player != null && mc.world != null) {
         SimulatedPlayerInput simulatedPlayerInput = SimulatedPlayerInput.getSimulatedPlayerInput(1);
         Vec3d vec3d = simulatedPlayerInput.vec3d7.add(0.0, mc.player.getStandingEyeHeight(), 0.0);
         Vec3d vec3d2 = new Vec3d(
            (livingEntity.getX() - livingEntity.lastRenderX) * 2.0,
            (livingEntity.getY() - livingEntity.lastRenderY) * 2.0,
            (livingEntity.getZ() - livingEntity.lastRenderZ) * 2.0
         );
         Box box = livingEntity.getBoundingBox().offset(vec3d2);
         if (!EntityUtils.hasLineOfSight(vec3d, box.getCenter())) {
            return false;
         }

         double local1333_5 = AttackAuraModule.getInstance().getEffectiveRange();
         double local1333_7 = Math.max(Math.max(box.minX - vec3d.x, vec3d.x - box.maxX), 0.0);
         double local1333_9 = Math.max(Math.max(box.minY - vec3d.y, vec3d.y - box.maxY), 0.0);
         double local1333_11 = Math.max(Math.max(box.minZ - vec3d.z, vec3d.z - box.maxZ), 0.0);
         return local1333_7 * local1333_7 + local1333_9 * local1333_9 + local1333_11 * local1333_11 < local1333_5 * local1333_5;
      } else {
         return false;
      }
   }

   private static Vec3d getVec3d() {
      if (mc.player == null || mc.world == null || mc.player.isOnGround()) {
         return null;
      }

      if (mc.player.getAbilities().flying || mc.player.isGliding() || mc.player.hasVehicle() || mc.player.isClimbing()) {
         return null;
      }

      if (mc.player.hasStatusEffect(StatusEffects.LEVITATION)) {
         return null;
      }

      if (!mc.player.isTouchingWater() && !mc.player.isInLava()) {
         double finalGravity = mc.player.getFinalGravity();
         double local1334_2 = 0.0;
         double local1334_4 = 0.0;
         double local1334_6 = 0.0;
         Box boundingBox = mc.player.getBoundingBox();
         Vec3d velocity = mc.player.getVelocity();

         for (int index = 0; index < 400; index++) {
            double local1334_11 = velocity.x * 0.9799995613681012;
            double local1334_13 = (velocity.y - finalGravity) * 0.9799995613681012;
            double local1334_15 = velocity.z * 0.9799995613681012;
            Vec3d vec3d = new Vec3d(local1334_11, local1334_13, local1334_15);
            Vec3d vec3d2 = Entity.adjustMovementForCollisions(
               mc.player, vec3d, boundingBox.offset(local1334_2, local1334_4, local1334_6), mc.world, list
            );
            if (vec3d.y < -9.999995680744685E-5 && vec3d2.y > vec3d.y + 9.999995999686725E-5) {
               return mc.player.getEntityPos().add(local1334_2 + vec3d2.x, local1334_4 + vec3d2.y, local1334_6 + vec3d2.z);
            }

            local1334_2 += vec3d2.x;
            local1334_4 += vec3d2.y;
            local1334_6 += vec3d2.z;
            if (isInLiquid(boundingBox.offset(local1334_2, local1334_4, local1334_6))) {
               return mc.player.getEntityPos().add(local1334_2, local1334_4, local1334_6);
            }

            if (Math.abs(vec3d2.x - vec3d.x) > 9.999995999686725E-5) {
               local1334_11 = 0.0;
            }

            if (Math.abs(vec3d2.y - vec3d.y) > 9.999995999686725E-5) {
               local1334_13 = 0.0;
            }

            if (Math.abs(vec3d2.z - vec3d.z) > 9.999995999686725E-5) {
               local1334_15 = 0.0;
            }

            velocity = new Vec3d(local1334_11, local1334_13, local1334_15);
            if (velocity.lengthSquared() < 1.0000000090069629E-12 && vec3d.y >= -9.999995680744685E-5) {
               break;
            }
         }

         return null;
      } else {
         return null;
      }
   }
}
