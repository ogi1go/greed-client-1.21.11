package greed.client.cosmetic;

import greed.client.cosmetic.CosmeticCategory;
import greed.client.cosmetic.CosmeticEntry;
import greed.client.cosmetic.CosmeticModel;
import greed.client.cosmetic.CosmeticPlacement;
import greed.client.cosmetic.CosmeticStore;
import greed.client.module.misc.GlobalsModule;
import greed.client.module.misc.UnHookModule;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public final class CosmeticMotionTracker {
   private static final CosmeticMotionTracker cosmeticMotionTracker = new CosmeticMotionTracker();
   private final HashMap<UUID, CosmeticMotionTracker.MotionHistory> motionHistories = new HashMap<>();
   private final HashSet<UUID> activePlayers = new HashSet<>();
   private Object lastWorld;

   private boolean updateVerticalMotion(CosmeticMotionTracker.MotionHistory history, double targetY) {
      history.verticalVelocity = Math.min(0.85, history.verticalVelocity + 0.08);
      history.posY = Math.max(targetY, history.posY - history.verticalVelocity);
      if (history.posY > targetY + 0.001) {
         return true;
      }

      history.posY = targetY;
      history.verticalVelocity = 0.0;
      return false;
   }

   private static Vec3d computePetTarget(Vec3d vec3d, float yaw, boolean flying) {
      double yawRadians = Math.toRadians(yaw);
      return flying
         ? new Vec3d(vec3d.x + Math.cos(yawRadians - (Math.PI / 2)) * 1.25, vec3d.y, vec3d.z + Math.sin(yawRadians - (Math.PI / 2)) * 1.25)
         : new Vec3d(vec3d.x + Math.sin(yawRadians) * 1.75, vec3d.y, vec3d.z - Math.cos(yawRadians) * 1.75);
   }

   public static CosmeticMotionTracker getCosmeticMotionTracker() {
      return cosmeticMotionTracker;
   }

   private double raycastGroundY(MinecraftClient minecraftClient, Vec3d vec3d, Vec3d vec3d2) {
      HitResult hitResult = minecraftClient.world
         .raycast(new RaycastContext(vec3d, vec3d2, ShapeType.COLLIDER, FluidHandling.NONE, minecraftClient.player));
      if (hitResult instanceof BlockHitResult blockHitResult && hitResult.getType() != Type.MISS) {
         BlockPos blockPos = blockHitResult.getBlockPos();
         BlockPos blockPos2 = blockPos.up();
         if (minecraftClient.world.getBlockState(blockPos).getCollisionShape(minecraftClient.world, blockPos).isEmpty()) {
            return Double.NaN;
         } else {
            return minecraftClient.world.getBlockState(blockPos2).getCollisionShape(minecraftClient.world, blockPos2).isEmpty()
                  && minecraftClient.world.getBlockState(blockPos2.up()).getCollisionShape(minecraftClient.world, blockPos2.up()).isEmpty()
               ? hitResult.getPos().y
               : Double.NaN;
         }
      } else {
         return Double.NaN;
      }
   }

   private Vec3d findGroundPosition(MinecraftClient minecraftClient, double centerX, double centerZ, double baseY) {
      for (int index = 0; index <= 4; index++) {
         for (int index2 = -index; index2 <= index; index2++) {
            for (int index3 = -index; index3 <= index; index3++) {
               if (index <= 0 || Math.abs(index2) == index || Math.abs(index3) == index) {
                  double candidateX = index == 0 ? centerX : Math.floor(centerX) + index2 + 0.5;
                  double candidateZ = index == 0 ? centerZ : Math.floor(centerZ) + index3 + 0.5;
                  double groundY = this.raycastGroundYNear(minecraftClient, candidateX, candidateZ, baseY);
                  if (!Double.isNaN(groundY)) {
                     return new Vec3d(candidateX, groundY, candidateZ);
                  }
               }
            }
         }
      }

      return null;
   }

   public void processData(MinecraftClient minecraftClient) {
      if (UnHookModule.isUnhooked()) {
         this.motionHistories.clear();
         return;
      }

      if (minecraftClient.player != null && minecraftClient.world != null) {
         if (this.lastWorld != minecraftClient.world) {
            this.motionHistories.clear();
            this.lastWorld = minecraftClient.world;
         }

         HashSet<UUID> activePlayerIds = this.activePlayers;
         activePlayerIds.clear();

         for (AbstractClientPlayerEntity abstractClientPlayerEntity : minecraftClient.world.getPlayers()) {
            CosmeticEntry currentCosmeticEntry = abstractClientPlayerEntity == minecraftClient.player
               ? CosmeticStore.getCosmeticEntry(CosmeticCategory.PET)
               : (
                  !abstractClientPlayerEntity.isSpectator() && !abstractClientPlayerEntity.isInvisible() && GlobalsModule.getInstance().checkState()
                     ? GlobalsModule.getInstance().getCosmeticEntry(abstractClientPlayerEntity.getGameProfile().name(), CosmeticCategory.PET)
                     : null
               );
            if (currentCosmeticEntry != null && currentCosmeticEntry.placement() != null && currentCosmeticEntry.placement().isPet()) {
               activePlayerIds.add(abstractClientPlayerEntity.getUuid());
               CosmeticMotionTracker.MotionHistory history = this.motionHistories.get(abstractClientPlayerEntity.getUuid());
               if (history == null || history.cosmeticEntry.id() != currentCosmeticEntry.id()) {
                  Vec3d vec3d = computePetTarget(abstractClientPlayerEntity.getEntityPos(), abstractClientPlayerEntity.bodyYaw, currentCosmeticEntry.placement().flying());
                  double targetY = currentCosmeticEntry.placement().flying()
                     ? abstractClientPlayerEntity.getY() + 1.3
                     : this.resolveGroundY(minecraftClient, vec3d.x, vec3d.z, abstractClientPlayerEntity.getY());
                  history = new CosmeticMotionTracker.MotionHistory(
                     currentCosmeticEntry,
                     abstractClientPlayerEntity.getUuid().getLeastSignificantBits() ^ currentCosmeticEntry.id(),
                     vec3d.x,
                     targetY,
                     vec3d.z,
                     abstractClientPlayerEntity.bodyYaw
                  );
                  this.motionHistories.put(abstractClientPlayerEntity.getUuid(), history);
               }

               this.updatePet(minecraftClient, abstractClientPlayerEntity, history);
            }
         }

         this.motionHistories.keySet().retainAll(activePlayerIds);
      } else {
         this.motionHistories.clear();
         this.lastWorld = null;
      }
   }

   private double resolveGroundY(MinecraftClient minecraftClient, double x, double z, double fallbackY) {
      double groundY = this.raycastGroundYNear(minecraftClient, x, z, fallbackY);
      return Double.isNaN(groundY) ? fallbackY : groundY;
   }

   private boolean updateMotion(
      MinecraftClient minecraftClient, CosmeticMotionTracker.MotionHistory history, Vec3d vec3d, double maxStep, boolean flying
   ) {
      double deltaX = vec3d.x - history.posX;
      double deltaZ = vec3d.z - history.posZ;
      double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
      boolean verticalMoved = false;
      if (!flying) {
         double groundY = this.raycastGroundYFar(minecraftClient, history.posX, history.posZ, history.posY);
         if (!Double.isNaN(groundY) && groundY < history.posY - 0.02) {
            verticalMoved = this.updateVerticalMotion(history, groundY);
         } else if (!Double.isNaN(groundY)) {
            history.posY = groundY;
            history.verticalVelocity = 0.0;
         }
      }

      if (distance > 15.0) {
         if (flying) {
            history.setPosition(vec3d.x, vec3d.y, vec3d.z);
         } else {
            Vec3d vec3d2 = this.findGroundPosition(minecraftClient, vec3d.x, vec3d.z, vec3d.y);
            if (vec3d2 != null) {
               history.setPosition(vec3d2.x, vec3d2.y, vec3d2.z);
            }
         }

         return false;
      } else {
         boolean moved = false;
         if (distance > 0.12) {
            double step = Math.min(maxStep, distance);
            double dirX = deltaX / distance;
            double dirZ = deltaZ / distance;
            if (flying) {
               history.posX += dirX * step;
               history.posZ += dirZ * step;
               history.yaw = history.yaw
                  + MathHelper.wrapDegrees((float)Math.toDegrees(Math.atan2(-deltaX, deltaZ)) - history.yaw) * 0.28F;
               moved = true;
            } else {
               moved = this.tryMoveTo(
                  minecraftClient, history, history.posX + dirX * step, history.posZ + dirZ * step
               );
               if (!moved) {
                  moved = this.tryMoveTo(
                     minecraftClient,
                     history,
                     history.posX - dirZ * step * history.strafeSign,
                     history.posZ + dirX * step * history.strafeSign
                  );
               }

               if (!moved) {
                  history.strafeSign = -history.strafeSign;
                  moved = this.tryMoveTo(
                     minecraftClient,
                     history,
                     history.posX - dirZ * step * history.strafeSign,
                     history.posZ + dirX * step * history.strafeSign
                  );
               }
            }
         }

         if (flying) {
            history.verticalVelocity = 0.0;
            history.posY = history.posY + MathHelper.clamp(vec3d.y - history.posY, -0.32, 0.32);
         }

         if (!moved && !verticalMoved && distance > 0.12) {
            history.wanderCooldown = Math.min(history.wanderCooldown, 5);
         }

         return moved || verticalMoved;
      }
   }

   public void renderElement(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, MinecraftClient minecraftClient) {
      if (UnHookModule.isUnhooked()) {
         return;
      }

      if (minecraftClient.player != null && minecraftClient.world != null && matrixStack != null && vertexConsumerProvider != null) {
         float tickProgress = minecraftClient.getRenderTickCounter().getTickProgress(false);
         Vec3d cameraPos = minecraftClient.gameRenderer.getCamera().getCameraPos();

         for (CosmeticMotionTracker.MotionHistory history : this.motionHistories.values()) {
            double renderX = MathHelper.lerp(tickProgress, history.prevX, history.posX) - cameraPos.x;
            double renderY = MathHelper.lerp(tickProgress, history.prevY, history.posY) - cameraPos.y;
            double renderZ = MathHelper.lerp(tickProgress, history.prevZ, history.posZ) - cameraPos.z;
            CosmeticModel cosmeticModel = CosmeticModel.getCosmeticModel(history.cosmeticEntry);
            if (cosmeticModel != null) {
               CosmeticPlacement cosmeticPlacement = history.cosmeticEntry.placement();
               float yaw = history.prevYaw + MathHelper.wrapDegrees(history.yaw - history.prevYaw) * tickProgress;
               float scale = Math.max(0.05F, cosmeticPlacement.scale());
               matrixStack.push();
               matrixStack.translate(renderX, renderY, renderZ);
               matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - yaw));
               matrixStack.translate(cosmeticPlacement.x(), cosmeticPlacement.y(), cosmeticPlacement.z());
               matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cosmeticPlacement.rotationX()));
               matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(cosmeticPlacement.rotationY()));
               matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(cosmeticPlacement.rotationZ()));
               matrixStack.scale(scale, scale, scale);
               cosmeticModel.renderElement6(
                  matrixStack,
                  vertexConsumerProvider,
                  15728880,
                  (history.animationTick + tickProgress) / 20.0F,
                  MathHelper.lerp(tickProgress, history.prevAnimationProgress, history.animationProgress)
               );
               matrixStack.pop();
            }
         }
      }
   }

   private double raycastGroundYNear(MinecraftClient minecraftClient, double x, double z, double y) {
      return this.raycastGroundY(minecraftClient, new Vec3d(x, y + 4.0, z), new Vec3d(x, y - 12.0, z));
   }

   private static double distanceSquared(Vec3d vec3d, Vec3d vec3d2) {
      double deltaX = vec3d.x - vec3d2.x;
      double deltaZ = vec3d.z - vec3d2.z;
      return deltaX * deltaX + deltaZ * deltaZ;
   }

   private boolean tryMoveTo(MinecraftClient minecraftClient, CosmeticMotionTracker.MotionHistory history, double x, double z) {
      double groundY = this.raycastGroundYNear(minecraftClient, x, z, history.posY);
      if (Double.isNaN(groundY) || groundY < history.posY - 1.25) {
         groundY = this.raycastGroundYFar(minecraftClient, x, z, history.posY);
      }

      if (!Double.isNaN(groundY) && !(groundY > history.posY + 1.25)) {
         double deltaX = x - history.posX;
         double deltaZ = z - history.posZ;
         history.posX = x;
         history.posZ = z;
         if (groundY < history.posY - 1.25) {
            this.updateVerticalMotion(history, groundY);
         } else {
            history.posY = groundY;
            history.verticalVelocity = 0.0;
         }

         history.yaw = history.yaw
            + MathHelper.wrapDegrees((float)Math.toDegrees(Math.atan2(-deltaX, deltaZ)) - history.yaw) * 0.28F;
         return true;
      } else {
         return false;
      }
   }

   private double raycastGroundYFar(MinecraftClient minecraftClient, double x, double z, double y) {
      return this.raycastGroundY(minecraftClient, new Vec3d(x, y + 0.35, z), new Vec3d(x, y - 64.0, z));
   }

   private Vec3d computeWanderPosition(CosmeticMotionTracker.MotionHistory history, Vec3d vec3d, double minRadius, double maxRadius) {
      if (--history.wanderCooldown <= 0
         || distanceSquared(new Vec3d(history.wanderX, vec3d.y, history.wanderZ), vec3d) > maxRadius * maxRadius) {
         double angle = history.random.nextDouble() * Math.PI * 2.0;
         double radius = minRadius + history.random.nextDouble() * (maxRadius - minRadius);
         history.wanderX = vec3d.x + Math.cos(angle) * radius;
         history.wanderZ = vec3d.z + Math.sin(angle) * radius;
         history.wanderCooldown = 45 + history.random.nextInt(90);
      }

      return new Vec3d(history.wanderX, vec3d.y, history.wanderZ);
   }

   private void updatePet(MinecraftClient minecraftClient, AbstractClientPlayerEntity abstractClientPlayerEntity, CosmeticMotionTracker.MotionHistory history) {
      boolean flying = history.cosmeticEntry.placement().flying();
      Vec3d vec3d = abstractClientPlayerEntity.getEntityPos();
      boolean moved = history.lastPosition != null && distanceSquared(vec3d, history.lastPosition) > 4.0E-4;
      history.lastPosition = vec3d;
      if (moved) {
         history.bodyYaw = abstractClientPlayerEntity.bodyYaw;
      }

      Vec3d vec3d2 = computePetTarget(vec3d, history.bodyYaw, flying);
      if (flying) {
         vec3d2 = new Vec3d(vec3d2.x, vec3d.y + 1.3, vec3d2.z);
      }

      history.prevX = history.posX;
      history.prevY = history.posY;
      history.prevZ = history.posZ;
      history.prevYaw = history.yaw;
      history.prevAnimationProgress = history.animationProgress;
      history.animationTick++;
      double distanceSq = distanceSquared(new Vec3d(history.posX, history.posY, history.posZ), vec3d);
      double farDistance = flying ? 4.0 : 7.0;
      double nearDistance = flying ? 3.5 : 6.0;
      if (distanceSq > farDistance * farDistance) {
         history.moveDirectly = true;
      } else if (distanceSq < nearDistance * nearDistance) {
         history.moveDirectly = false;
      }

      Vec3d vec3d3 = history.moveDirectly ? vec3d2 : this.computeWanderPosition(history, vec3d, flying ? 3.0 : 1.4, flying ? 4.0 : 6.5);
      if (flying) {
         vec3d3 = new Vec3d(vec3d3.x, vec3d.y + 1.3, vec3d3.z);
      }

      boolean movedToTarget = this.updateMotion(minecraftClient, history, vec3d3, history.moveDirectly ? 0.22 : 0.075, flying);
      history.animationProgress = history.animationProgress + ((movedToTarget ? 1.0F : 0.0F) - history.animationProgress) * 0.25F;
   }

   private static final class MotionHistory {
      private final CosmeticEntry cosmeticEntry;
      private final Random random;
      private double posX;
      private double posY;
      private double posZ;
      private double prevX;
      private double prevY;
      private double prevZ;
      private double wanderX;
      private double wanderZ;
      private double verticalVelocity;
      private double strafeSign;
      private float yaw;
      private float prevYaw;
      private float bodyYaw;
      private float animationProgress;
      private float prevAnimationProgress;
      private int wanderCooldown;
      private int animationTick;
      private int unusedInt;
      private final int variantIndex;
      private double unusedDouble1;
      private double unusedDouble2;
      private double unusedDouble3;
      private double unusedDouble4;
      private boolean moveDirectly;
      private boolean unusedFlag;
      private Vec3d lastPosition;

      private void setPosition(double x, double y, double z) {
         this.posX = this.prevX = this.wanderX = x;
         this.posY = this.prevY = y;
         this.posZ = this.prevZ = this.wanderZ = z;
         this.verticalVelocity = 0.0;
      }

      private void savePreviousState() {
         this.prevX = this.posX;
         this.prevY = this.posY;
         this.prevZ = this.posZ;
         this.prevYaw = this.yaw;
         this.prevAnimationProgress = this.animationProgress;
      }

      private MotionHistory(CosmeticEntry initialCosmeticEntry, long seed, double x, double y, double z, float yaw) {
         this.cosmeticEntry = initialCosmeticEntry;
         this.random = new Random(seed);
         this.variantIndex = (int)(seed & 2147483647L) % 4;
         this.posX = this.prevX = this.wanderX = x;
         this.posY = this.prevY = y;
         this.posZ = this.prevZ = this.wanderZ = z;
         this.yaw = this.prevYaw = this.bodyYaw = yaw;
         this.strafeSign = this.random.nextBoolean() ? 1.0 : -1.0;
      }
   }
}
