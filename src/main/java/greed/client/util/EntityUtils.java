package greed.client.util;

import greed.client.module.combat.AttackAuraModule;
import greed.client.module.combat.MaceHelperModule;
import greed.client.util.MathUtils;
import greed.client.MinecraftAccess;
import greed.client.RotationAngles;
import greed.client.RotationSpeed;
import greed.client.module.misc.YTMODEModule;

import java.util.ArrayList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.PointedDripstoneBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public final class EntityUtils implements MinecraftAccess {
   private EntityUtils() {
   }

   public static boolean isObstructingBlock(BlockState blockState, BlockPos blockPos) {
      if (mc.player == null) {
         return false;
      } else {
         BlockPos blockPos2 = BlockPos.ofFloored(mc.player.getEyePos());
         if (!blockPos2.equals(blockPos) && (!mc.player.isInPose(EntityPose.SWIMMING) || !blockPos2.up().equals(blockPos))) {
            Block block = blockState.getBlock();
            return block instanceof FenceBlock
               || block instanceof FenceGateBlock
               || block instanceof WallBlock
               || block instanceof PaneBlock
               || block instanceof SlabBlock
               || block instanceof StairsBlock
               || block instanceof TrapdoorBlock
               || block instanceof PointedDripstoneBlock;
         } else {
            return true;
         }
      }
   }

   public static float getTotalHealth(LivingEntity livingEntity) {
      if (mc.world != null) {
         for (ScoreboardObjective scoreboardObjective : mc.world.getScoreboard().getObjectives()) {
            ReadableScoreboardScore score = mc.world.getScoreboard().getScore(livingEntity, scoreboardObjective);
            if (score != null) {
               return score.getScore();
            }
         }
      }

      return livingEntity.getHealth() + livingEntity.getAbsorptionAmount();
   }

   public static boolean intersectsPlayer(LivingEntity livingEntity, float expansion) {
      Box boundingBox = mc.player.getBoundingBox();
      Box box = livingEntity.getBoundingBox().stretch(expansion, 0.0, expansion);
      return boundingBox.intersects(box);
   }

   public static float calculateFovDistance(float angle, float factor, float minValue, float maxValue) {
      return MathUtils.computeFloat12(minValue, maxValue, Math.abs(angle) * MathUtils.clampUnit(factor));
   }

   public static boolean canSeeVector(Vec3d vec3d, Vec3d vec3d2) {
      return mc.world != null && mc.player != null ? BlockView.raycast(vec3d, vec3d2, null, (blockView, blockPos) -> {
         BlockState blockState = mc.world.getBlockState(blockPos);
         if (isObstructingBlock(blockState, blockPos)) {
            return null;
         }

         VoxelShape collisionShape = blockState.getCollisionShape(mc.world, blockPos);
         return !collisionShape.isEmpty() && collisionShape.raycast(vec3d, vec3d2, blockPos) != null ? Boolean.FALSE : null;
      }, missBlockView -> Boolean.TRUE) : true;
   }

   private static boolean canSeeVectorOffset(Vec3d vec3d, Vec3d vec3d2) {
      Vec3d vec3d3 = vec3d2.add(vec3d.subtract(vec3d2).normalize().multiply(0.05));
      return canSeeVector(vec3d, vec3d3);
   }

   public static boolean isDonationTotem(ItemStack itemStack) {
      return itemStack.getItem() == Items.TOTEM_OF_UNDYING
         && !itemStack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT)
            .copyNbt()
            .getCompoundOrEmpty("PublicBukkitValues")
            .getString("minecraft:don-item", "")
            .isEmpty();
   }

   public static float[] calculateRotations(double targetX, double targetY, double targetZ, double sourceX, double sourceY, double sourceZ) {
      double[] doubleArray = new double[]{targetX - sourceX, targetY - sourceY, targetZ - sourceZ};
      doubleArray = new double[]{
         doubleArray[0], doubleArray[1], doubleArray[2], Math.sqrt(doubleArray[0] * doubleArray[0] + doubleArray[2] * doubleArray[2])
      };
      return new float[]{
         (float)Math.atan2(doubleArray[2], doubleArray[0]) * (float) (180.0 / Math.PI) - 90.0F,
         (float)Math.atan2(doubleArray[1], doubleArray[3]) * (float) (-180.0 / Math.PI)
      };
   }

   public static float calculateYawSpeed(RotationAngles rotationAngles, float minYawSpeed, float maxYawSpeed) {
      return mc.player == null
         ? 360.0F
         : Math.max(
            MouseSensitivity.getSensitivityDelta(),
            Math.abs(MathHelper.wrapDegrees(rotationAngles.getYaw() - mc.player.getYaw()))
               / Math.max(1.0F, MathUtils.computeFloat10(minYawSpeed, maxYawSpeed))
         );
   }

   public static boolean isIntersecting(LivingEntity livingEntity) {
      return intersectsPlayer(livingEntity, 0.0F);
   }

   public static boolean matchesCriteria(Vec3d vec3d, Vec3d vec3d2, Box box, double reach) {
      double tMin = 0.0;
      double tMax = reach;
      double originX = vec3d.x;
      double originY = vec3d.y;
      double originZ = vec3d.z;

      for (int index = 0; index < 3; index++) {
         double direction = index == 0 ? vec3d2.x : (index == 1 ? vec3d2.y : vec3d2.z);
         double axisOrigin = index == 0 ? originX : (index == 1 ? originY : originZ);
         double boxMin = index == 0 ? box.minX : (index == 1 ? box.minY : box.minZ);
         double boxMax = index == 0 ? box.maxX : (index == 1 ? box.maxY : box.maxZ);
         if (Math.abs(direction) < 1.0E-9) {
            if (axisOrigin < boxMin || axisOrigin > boxMax) {
               return false;
            }
         } else {
            double invDirection = 1.0 / direction;
            double tNear = (boxMin - axisOrigin) * invDirection;
            double tFar = (boxMax - axisOrigin) * invDirection;
            if (tNear > tFar) {
               double swapValue = tNear;
               tNear = tFar;
               tFar = swapValue;
            }

            tMin = Math.max(tMin, tNear);
            tMax = Math.min(tMax, tFar);
            if (tMin > tMax) {
               return false;
            }
         }
      }

      return tMax >= 0.0 && tMin <= reach;
   }

   public static float computeFloat(float minBound, float maxBound) {
      return Math.max(MouseSensitivity.getSensitivityDelta(), MouseSensitivity.quantizeSensitivity(MathUtils.computeFloat10(minBound, maxBound)));
   }

   public static float calculatePitchSpeed(RotationAngles rotationAngles, float minPitchSpeed, float maxPitchSpeed) {
      return mc.player == null
         ? 360.0F
         : Math.max(
            MouseSensitivity.getSensitivityDelta(),
            Math.abs(rotationAngles.getPitch() - mc.player.getPitch()) / Math.max(1.0F, MathUtils.computeFloat10(minPitchSpeed, maxPitchSpeed))
         );
   }

   public static boolean isLookingAtEntity(Entity entity, double lookDistance, float lookYaw, float lookPitch) {
      if (entity != null && mc.player != null) {
         Vec3d eyePos = mc.player.getEyePos();
         return entity.getBoundingBox().expand(entity.getTargetingMargin()).contains(eyePos)
            || entity.getBoundingBox()
               .expand(entity.getTargetingMargin())
               .raycast(eyePos, eyePos.add(getLookVector(lookYaw, lookPitch).multiply(lookDistance)))
               .isPresent();
      } else {
         return false;
      }
   }

   public static String formatHealth(LivingEntity livingEntity) {
      if (YTMODEModule.getInstance().isEnabled() && livingEntity.isInvisible()) {
         return "1000";
      } else {
         return Math.round(getTotalHealth(livingEntity)) == 1000 ? "?" : String.valueOf(Math.round(getTotalHealth(livingEntity)));
      }
   }

   public static float calculateSensitivityStep(int minSteps, int maxSteps) {
      return Math.max(1, Math.round((float)MathUtils.computeLong(minSteps, maxSteps))) * MouseSensitivity.getSensitivityDelta();
   }

   public static float calculatePitch(double deltaX, double deltaY, double deltaZ) {
      return (float)Math.atan2(deltaY, Math.sqrt(deltaX * deltaX + deltaZ * deltaZ)) * (float) (-180.0 / Math.PI);
   }

   public static float lerpDistance(Entity entity, float startValue, float endValue, float maxDistance) {
      return MathUtils.lerp(
         startValue, endValue, MathUtils.clampUnit((float)(getDistanceToBox(entity) / Math.max(0.01F, maxDistance)))
      );
   }

   public static boolean canHitEntity(LivingEntity livingEntity, double hitDistance) {
      if (mc.player != null && mc.world != null && livingEntity != null) {
         Vec3d eyePos = mc.player.getEyePos();
         Box box = livingEntity.getBoundingBox().expand(livingEntity.getTargetingMargin());
         if (box.contains(eyePos)) {
            return true;
         }

         Vec3d vec3d = box.raycast(
               eyePos, eyePos.add(getLookVector(mc.player.getYaw(), mc.player.getPitch()).multiply(hitDistance))
            )
            .orElse(null);
         return vec3d != null && canSeeVectorOffset(eyePos, vec3d);
      } else {
         return true;
      }
   }

   public static RotationSpeed calculateRotationSpeed(RotationAngles rotationAngles, float minRotationSpeed, float maxRotationSpeed, float maxYawSpeedValue, float maxPitchSpeedValue) {
      return new RotationSpeed(
         MouseSensitivity.quantizeSensitivity(calculateYawSpeed(rotationAngles, minRotationSpeed, maxRotationSpeed)),
         MouseSensitivity.quantizeSensitivity(calculatePitchSpeed(rotationAngles, minRotationSpeed, maxRotationSpeed)),
         maxYawSpeedValue,
         maxPitchSpeedValue
      );
   }

   public static Vec3d getLookVector(float inputYaw, float inputPitch) {
      float yawRadians = -inputYaw * (float) (Math.PI / 180.0) - (float) Math.PI;
      float pitchRadians = -inputPitch * (float) (Math.PI / 180.0);
      return new Vec3d(
         (float)Math.sin(yawRadians) * -((float)Math.cos(pitchRadians)),
         (float)Math.sin(pitchRadians),
         (float)Math.cos(yawRadians) * -((float)Math.cos(pitchRadians))
      );
   }

   public static boolean hasLineOfSight(Vec3d vec3d, Vec3d vec3d2) {
      return mc.world != null && mc.player != null
         ? mc.world.raycast(new RaycastContext(vec3d, vec3d2, ShapeType.COLLIDER, FluidHandling.NONE, mc.player)).getType()
            == Type.MISS
         : true;
   }

   public static float calculateAngle(double x, double y) {
      return (float)Math.atan2(y, x);
   }

   public static float[] calculateRotationsToVec(Vec3d vec3d, Vec3d vec3d2) {
      double[] doubleArray = new double[]{
         vec3d.getX() - vec3d2.getX(), vec3d.getY() - vec3d2.getY(), vec3d.getZ() - vec3d2.getZ()
      };
      doubleArray = new double[]{doubleArray[0], doubleArray[1], doubleArray[2], Math.sqrt(doubleArray[0] * doubleArray[0] + doubleArray[2] * doubleArray[2])};
      return new float[]{
         (float)Math.atan2(doubleArray[2], doubleArray[0]) * (float) (180.0 / Math.PI) - 90.0F,
         (float)Math.atan2(doubleArray[1], doubleArray[3]) * (float) (-180.0 / Math.PI)
      };
   }

   public static RotationAngles adjustRotationForWall(RotationAngles rotationAngles) {
      AttackAuraModule attackAuraModule = AttackAuraModule.getInstance();
      if (!attackAuraModule.isEnabled() || attackAuraModule.getTarget() == null || !attackAuraModule.wallAttackMode.isSelected("Неполноценные")) {
         return rotationAngles;
      }

      if (mc.player == null || mc.world == null) {
         return rotationAngles;
      }

      if (!MaceHelperModule.getInstance().isWindChargePending() && !attackAuraModule.canPerformMaceSmash()) {
         LivingEntity livingEntity = attackAuraModule.getTarget();
         Vec3d vec3d = mc.player.getEyePos();
         Box boundingBox = livingEntity.getBoundingBox();
         double effectiveRange = attackAuraModule.getEffectiveRange();
         Vec3d vec3d2 = boundingBox.raycast(
               vec3d, vec3d.add(getLookVector(rotationAngles.getYaw(), rotationAngles.getPitch()).normalize().multiply(effectiveRange + 1.0))
            )
            .orElse(null);
         if (vec3d2 != null && !canSeeVectorOffset(vec3d, vec3d2)) {
            ArrayList<Vec3d> candidatePoints = new ArrayList<>();
            Vec3d vec3d3 = Vec3d.ZERO;

            for (int index = 0; index <= 8; index++) {
               for (int index2 = 0; index2 <= 8; index2++) {
                  for (int index3 = 0; index3 <= 8; index3++) {
                     if (index == 0 || index == 8 || index2 == 0 || index2 == 8 || index3 == 0 || index3 == 8) {
                        Vec3d vec3d4 = new Vec3d(
                           MathHelper.lerp(index / 8.0, boundingBox.minX, boundingBox.maxX),
                           MathHelper.lerp(index2 / 8.0, boundingBox.minY, boundingBox.maxY),
                           MathHelper.lerp(index3 / 8.0, boundingBox.minZ, boundingBox.maxZ)
                        );
                        if (!(vec3d.squaredDistanceTo(vec3d4) > effectiveRange * effectiveRange) && canSeeVectorOffset(vec3d, vec3d4)) {
                           candidatePoints.add(vec3d4);
                           vec3d3 = vec3d3.add(vec3d4);
                        }
                     }
                  }
               }
            }

            if (candidatePoints.isEmpty()) {
               return rotationAngles;
            }

            Vec3d vec3d5 = vec3d3.multiply(1.0 / candidatePoints.size());
            Vec3d vec3d6 = candidatePoints.get(0);

            for (Vec3d vec3d7 : candidatePoints) {
               if (vec3d7.squaredDistanceTo(vec3d5) < vec3d6.squaredDistanceTo(vec3d5)) {
                  vec3d6 = vec3d7;
               }
            }

            float[] floatArray = calculateRotationsToVec(vec3d6, vec3d);
            return new RotationAngles(floatArray[0], floatArray[1]);
         } else {
            return rotationAngles;
         }
      } else {
         return rotationAngles;
      }
   }

   public static double getDistanceToBox(Entity entity) {
      if (entity != null && mc.player != null) {
         Vec3d eyePos = mc.player.getEyePos();
         Box boundingBox = entity.getBoundingBox();
         double distanceX = Math.max(Math.max(boundingBox.minX - eyePos.x, eyePos.x - boundingBox.maxX), 0.0);
         double distanceY = Math.max(Math.max(boundingBox.minY - eyePos.y, eyePos.y - boundingBox.maxY), 0.0);
         double distanceZ = Math.max(Math.max(boundingBox.minZ - eyePos.z, eyePos.z - boundingBox.maxZ), 0.0);
         return Math.sqrt(distanceX * distanceX + distanceY * distanceY + distanceZ * distanceZ);
      } else {
         return Double.MAX_VALUE;
      }
   }

   public static double getAngleDistance(LivingEntity livingEntity) {
      if (livingEntity != null && mc.player != null) {
         Vec3d eyePos = mc.player.getEyePos();
         Vec3d vec3d = livingEntity.getEntityPos()
            .add(0.0, MathHelper.clamp(eyePos.y - livingEntity.getY(), 0.0, livingEntity.getHeight() * 0.8), 0.0)
            .subtract(eyePos);
         float yawDelta = MathHelper.wrapDegrees((float)Math.toDegrees(Math.atan2(-vec3d.x, vec3d.z)) - RotationAngles.getCameraYaw());
         float pitchDelta = (float)(-Math.toDegrees(Math.atan2(vec3d.y, Math.hypot(vec3d.x, vec3d.z)))) - RotationAngles.getCameraPitch();
         return Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);
      } else {
         return 0.0;
      }
   }
}
