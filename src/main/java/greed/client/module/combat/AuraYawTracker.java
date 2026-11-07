package greed.client.module.combat;

import greed.client.util.EntityUtils;

import greed.client.util.Easings;
import greed.client.util.MathUtils;
import greed.client.MinecraftAccess;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class AuraYawTracker implements MinecraftAccess {
   private float currentYawOffset;
   private float targetYawThreshold = 0.6F;
   private float aimTrackingTicks;
   private boolean isTargetLocked;
   private long cooldownExpiryMillis;

   public void scheduleCooldown() {
      this.cooldownExpiryMillis = System.currentTimeMillis() + (long)MathUtils.randomFloat(200.0F, 250.0F);
   }

   public static float calculateAttackProgressFactor() {
      float attackCooldownProgress = mc.player.getAttackCooldownProgress(1.0F);
      double attributeValue = mc.player.getAttributeValue(EntityAttributes.ATTACK_SPEED);
      if (attributeValue > 1.6) {
         attackCooldownProgress *= (float)(1.6 / attributeValue);
      }

      return MathHelper.clamp(MathHelper.sin((float)(Easings.EASE_OUT_BACK.apply(attackCooldownProgress) * Math.PI)), 0.12F, 1.0F);
   }

   public float calculateYawOffset(LivingEntity livingEntity, float tickDelta, float playerYaw, float playerPitch, boolean isDirectAim) {
      if (livingEntity != null && mc.player != null) {
         Vec3d vec3d = new Vec3d(
            livingEntity.getX() - livingEntity.lastRenderX - mc.player.getX() + mc.player.lastRenderX,
            0.0,
            livingEntity.getZ() - livingEntity.lastRenderZ - mc.player.getZ() + mc.player.lastRenderZ
         );
         Vec3d cameraPosVec = mc.player.getCameraPosVec(tickDelta);
         Box box = livingEntity.getBoundingBox()
            .offset(livingEntity.getLerpedPos(tickDelta).subtract(livingEntity.getEntityPos()).add(vec3d.multiply(4.0)));
         box = box.contract(box.getLengthX() * 0.25, 0.0, box.getLengthZ() * 0.25);
         Vec3d vec3d2 = cameraPosVec.add(EntityUtils.getLookVector(playerYaw, playerPitch).multiply(cameraPosVec.distanceTo(box.getCenter())));
         double dx = MathHelper.clamp(vec3d2.x, box.minX, box.maxX) - cameraPosVec.x;
         double dz = MathHelper.clamp(vec3d2.z, box.minZ, box.maxZ) - cameraPosVec.z;
         float deltaYaw = MathHelper.wrapDegrees((float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F - playerYaw)
            + MathHelper.sin(mc.player.age * 0.21F) * 0.35F;
         this.aimTrackingTicks = Math.abs(deltaYaw) < 1.5F ? Math.min(this.aimTrackingTicks + 1.0F, 240.0F) : 0.0F;
         if (Math.abs(deltaYaw) < this.targetYawThreshold) {
            this.isTargetLocked = true;
            this.currentYawOffset = 0.0F;
            return 0.0F;
         }

         if (this.isTargetLocked) {
            this.isTargetLocked = false;
            this.targetYawThreshold = MathUtils.randomFloat(0.4F, 0.9F);
         }

         float speedMultiplier = isDirectAim
            ? 1.0F
            : (1.0F - MathHelper.clamp((this.aimTrackingTicks - 60.0F) / 72.0F, 0.0F, 1.0F))
               * (mc.player.handSwingTicks > MathUtils.randomFloat(0.2F, 0.4F) ? 0.5F : 1.0F)
               * (EntityUtils.isIntersecting(livingEntity) ? 0.5F : 1.0F)
               * (System.currentTimeMillis() < this.cooldownExpiryMillis ? 0.35F : 1.0F);
         float maxTurn = isDirectAim ? 4.5F : 3.0F;
         float maxStep = isDirectAim ? 0.35F : 0.22F;
         this.currentYawOffset = this.currentYawOffset
            + MathHelper.clamp(
               MathHelper.clamp(deltaYaw * (isDirectAim ? 0.2F : 0.14F * calculateAttackProgressFactor()) * speedMultiplier, -maxTurn, maxTurn) - this.currentYawOffset,
               -maxStep,
               maxStep
            );
         return this.currentYawOffset;
      } else {
         return 0.0F;
      }
   }

   public void reset() {
      this.currentYawOffset = this.aimTrackingTicks = 0.0F;
      this.isTargetLocked = false;
      this.cooldownExpiryMillis = 0L;
   }
}
