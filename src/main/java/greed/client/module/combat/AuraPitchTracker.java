package greed.client.module.combat;

import greed.client.MinecraftAccess;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class AuraPitchTracker implements MinecraftAccess {
   private float currentPitchOffset;

   public void reset() {
      this.currentPitchOffset = 0.0F;
   }

   public float calculatePitchOffset(LivingEntity livingEntity, float tickDelta, float playerPitch, boolean isDirectAim) {
      if (livingEntity != null && mc.player != null) {
         Vec3d cameraPosVec = mc.player.getCameraPosVec(tickDelta);
         Box box = livingEntity.getBoundingBox().offset(livingEntity.getLerpedPos(tickDelta).subtract(livingEntity.getEntityPos()));
         double horizontalDist = Math.max(0.6, Math.hypot(box.getCenter().x - cameraPosVec.x, box.getCenter().z - cameraPosVec.z));
         float topPitch = calculatePitchAngle(box.maxY - box.getLengthY() * 0.3 - cameraPosVec.y, horizontalDist);
         float bottomPitch = calculatePitchAngle(box.minY + box.getLengthY() * 0.2 - cameraPosVec.y, horizontalDist);
         float pitchDiff = playerPitch < topPitch ? topPitch - playerPitch : (playerPitch > bottomPitch ? bottomPitch - playerPitch : 0.0F);
         float pitchJitter = MathHelper.sin(mc.player.age * 0.37F) * 0.55F + MathHelper.cos(mc.player.age * 0.11F) * 0.35F;
         float maxPitchTurn = isDirectAim ? 1.8F : 1.1F;
         float maxPitchStep = isDirectAim ? 0.15F : 0.09F;
         this.currentPitchOffset = this.currentPitchOffset
            + MathHelper.clamp(
               MathHelper.clamp((pitchDiff + pitchJitter) * (isDirectAim ? 0.16F : 0.08F * AuraYawTracker.calculateAttackProgressFactor()), -maxPitchTurn, maxPitchTurn)
                  - this.currentPitchOffset,
               -maxPitchStep,
               maxPitchStep
            );
         return this.currentPitchOffset;
      } else {
         return 0.0F;
      }
   }

   private static float calculatePitchAngle(double dy, double dist) {
      return (float)(-Math.toDegrees(Math.atan2(dy, dist)));
   }
}
