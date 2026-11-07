package greed.client.module.combat;

import greed.client.util.EntityUtils;
import greed.client.util.RotationQuantizer;

import greed.client.MinecraftAccess;
import greed.client.RotationAngles;
import greed.client.RotationManager;
import greed.client.RotationSpeed;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class AdaptiveAuraRotation implements MinecraftAccess, AuraRotationStrategy {
   private final AuraYawTracker auraYawTracker = new AuraYawTracker();
   private final AuraPitchTracker auraPitchTracker = new AuraPitchTracker();
   private final RotationQuantizer yawQuantizer = new RotationQuantizer();
   private final RotationQuantizer pitchQuantizer = new RotationQuantizer();
   private int lastRotationTick;
   private boolean requiresRaycast;

   @Override
   public void reset() {
      this.auraYawTracker.reset();
      this.auraPitchTracker.reset();
      this.yawQuantizer.reset();
      this.pitchQuantizer.reset();
   }

   @Override
   public void update(LivingEntity entity, boolean canAttack, float maxRange) {
      if (entity != null && mc.player != null) {
         this.lastRotationTick = mc.player.age;
         this.requiresRaycast = canAttack || AttackController.hasAttackCooldown(-150L);
         if (canAttack && EntityUtils.getDistanceToBox(entity) < maxRange) {
            this.auraYawTracker.scheduleCooldown();
         }

         RotationManager.getRotationManager()
            .setRotation(
               new RotationAngles(mc.player.getYaw(), mc.player.getPitch()),
               new RotationSpeed(0.0F, 0.0F, 60.0F, 24.0F),
               RotationManager.ReturnMode.Empty
            );
      }
   }

   @Override
   public void applyRotation(LivingEntity livingEntity, float tickDelta) {
      if (livingEntity != null && mc.player != null && RotationManager.getRotationManager().isRotating() && mc.player.age - this.lastRotationTick <= 1) {
         float yaw = mc.player.getYaw();
         float pitch = mc.player.getPitch();
         Vec3d cameraPosVec = mc.player.getCameraPosVec(tickDelta);
         boolean raycastObstructed = this.requiresRaycast
            && !livingEntity.getBoundingBox()
               .offset(livingEntity.getLerpedPos(tickDelta).subtract(livingEntity.getEntityPos()))
               .expand(livingEntity.getTargetingMargin())
               .raycast(cameraPosVec, cameraPosVec.add(EntityUtils.getLookVector(yaw, pitch).multiply(8.0)))
               .isPresent();
         mc.player
            .setYaw(
               mc.player.headYaw = yaw
                  + this.yawQuantizer.quantize(this.auraYawTracker.calculateYawOffset(livingEntity, tickDelta, yaw, pitch, raycastObstructed))
            );
         mc.player
            .setPitch(
               MathHelper.clamp(
                  pitch + this.pitchQuantizer.quantize(this.auraPitchTracker.calculatePitchOffset(livingEntity, tickDelta, pitch, raycastObstructed)), -90.0F, 90.0F
               )
            );
      }
   }
}
