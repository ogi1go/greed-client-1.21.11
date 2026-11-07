package greed.client.module.combat;

import greed.client.module.combat.AttackAuraModule;
import greed.client.module.combat.MaceHelperModule;
import greed.client.util.MathUtils;
import greed.client.MinecraftAccess;
import greed.client.RotationAngles;
import greed.client.RotationManager;
import greed.client.RotationSpeed;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class CooldownAuraRotation implements MinecraftAccess {
   private boolean isRotationActive;
   private float yawOffset;
   private float pitchOffset;
   private float prevYawOffset;
   private float prevPitchOffset;
   private long lastUpdateMillis;
   private long targetSwitchMillis;

   private float calculateSpeedFactor() {
      float attackCooldownProgress = mc.player.getAttackCooldownProgress(1.0F);
      double attributeValue = mc.player.getAttributeValue(EntityAttributes.ATTACK_SPEED);
      if (attributeValue > 1.6) {
         attackCooldownProgress *= (float)(1.6 / attributeValue);
      }

      float local1858_4;
      if (attributeValue < 2.0) {
         local1858_4 = 2.70158F * attackCooldownProgress * attackCooldownProgress * attackCooldownProgress - 1.70158F * attackCooldownProgress * attackCooldownProgress;
      } else if (attackCooldownProgress < 0.5F) {
         local1858_4 = 8.0F * attackCooldownProgress * attackCooldownProgress * attackCooldownProgress * attackCooldownProgress;
      } else {
         local1858_4 = 1.0F - 8.0F * (attackCooldownProgress - 1.0F) * (attackCooldownProgress - 1.0F) * (attackCooldownProgress - 1.0F) * (attackCooldownProgress - 1.0F);
      }

      return (1.0F + 29.0F * local1858_4) / 50.0F;
   }

   public void update(LivingEntity livingEntity, boolean local1859_2, float local1859_3) {
      if (livingEntity != null && mc.player != null && mc.world != null) {
         Vec3d eyePos = mc.player.getEyePos();
         Vec3d vec3d = new Vec3d(
            livingEntity.getX() - livingEntity.lastRenderX, livingEntity.getY() - livingEntity.lastRenderY, livingEntity.getZ() - livingEntity.lastRenderZ
         );
         Box box = livingEntity.getBoundingBox()
            .offset(vec3d.lengthSquared() > 0.36 ? vec3d.normalize().multiply(0.6) : vec3d)
            .contract(0.08);
         Vec3d vec3d2 = new Vec3d(
            MathHelper.clamp(eyePos.x, box.minX, box.maxX),
            MathHelper.clamp(
               eyePos.y, box.minY + 0.15, Math.max(box.minY + 0.15, box.minY + (box.maxY - box.minY) * 0.7)
            ),
            MathHelper.clamp(eyePos.z, box.minZ, box.maxZ)
         );
         Vec3d vec3d3 = new Vec3d(
            (box.minX + box.maxX) * 0.5 - vec3d2.x, 0.0, (box.minZ + box.maxZ) * 0.5 - vec3d2.z
         );
         Vec3d vec3d4 = vec3d2.add(
            vec3d3.multiply(MathHelper.clamp((local1859_3 - 0.15 - eyePos.distanceTo(vec3d2)) / Math.max(0.05, vec3d3.length()), 0.0, 0.8))
         );
         Box box2 = livingEntity.getBoundingBox().contract(0.1);
         Vec3d vec3d5 = new Vec3d(
               MathHelper.clamp(vec3d4.x, box2.minX, box2.maxX),
               MathHelper.clamp(vec3d4.y, box2.minY, box2.maxY),
               MathHelper.clamp(vec3d4.z, box2.minZ, box2.maxZ)
            )
            .subtract(eyePos);
         RotationAngles rotationAngles = new RotationAngles(
            (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vec3d5.z, vec3d5.x)) - 90.0),
            (float)MathHelper.clamp(-Math.toDegrees(Math.atan2(vec3d5.y, Math.hypot(vec3d5.x, vec3d5.z))), -90.0, 90.0)
         );
         float yaw = mc.player.getYaw();
         float pitch = mc.player.getPitch();
         boolean local1859_15 = vec3d5.x * vec3d5.x + vec3d5.z * vec3d5.z < 0.0925F;
         if (local1859_2 || AttackAuraModule.getInstance().canPerformMaceSmash() || MaceHelperModule.getInstance().isWindChargeJumping() && !mc.player.isOnGround()) {
            boolean local1859_16 = false;
         } else {
            boolean local1859_16 = true;
         }

         long local1859_17 = System.currentTimeMillis();
         float local1859_19 = rotationAngles.getYaw();
         float local1859_20 = rotationAngles.getPitch();
         if (local1859_2) {
            this.lastUpdateMillis = local1859_17;
            this.targetSwitchMillis = (long)MathUtils.randomFloat(200.0F, 250.0F);
            this.prevYawOffset = local1859_19;
            this.prevPitchOffset = local1859_20;
         } else if (local1859_17 - this.lastUpdateMillis < this.targetSwitchMillis) {
            local1859_19 = this.prevYawOffset;
            local1859_20 = this.prevPitchOffset;
         }

         float local1859_21 = 1.0F - mc.player.getAttackCooldownProgress(1.0F);
         float local1859_22 = local1859_2 ? 0.0F : local1859_21 * local1859_21;
         float local1859_23 = AttackAuraModule.getInstance().hitCounter % 2 == 0 ? 1.0F : -1.0F;
         if (!local1859_2) {
            local1859_19 += (float)Math.ceil(
               local1859_22
                  * (
                     AttackAuraModule.getInstance().randomRange(5.0F, 15.0F) * MathHelper.cos(mc.player.age * 0.5F)
                        + AttackAuraModule.getInstance().randomRange(310.0F, 530.0F) * local1859_23
                  )
            );
            local1859_20 += (float)Math.ceil(
               local1859_22
                  * (
                     AttackAuraModule.getInstance().randomRange(5.0F, 10.0F) * MathHelper.sin(mc.player.age * 0.5F)
                        + AttackAuraModule.getInstance().randomRange(310.0F, 530.0F) * local1859_23
                  )
            );
         }

         float local1859_24 = MathHelper.wrapDegrees(local1859_19 - yaw);
         float local1859_25 = local1859_20 - pitch;
         float local1859_26 = (float)Math.hypot(local1859_24, local1859_25);
         float local1859_27 = MathHelper.clamp(MathHelper.sin((float)(this.calculateSpeedFactor() * Math.PI)), 0.15F, 0.9F);
         float local1859_28 = local1859_26 * local1859_27 * (this.isRotationActive ? 1.25F : 1.0F);
         float local1859_29 = 6.0F + 26.0F * local1859_27;
         this.yawOffset = this.yawOffset
            + MathHelper.clamp(local1859_28 * (local1859_26 < 0.001F ? 0.0F : local1859_24 / local1859_26) - this.yawOffset, -local1859_29, local1859_29);
         this.pitchOffset = this.pitchOffset
            + MathHelper.clamp(
               local1859_28 * (local1859_26 < 0.001F ? 0.0F : local1859_25 / local1859_26) - this.pitchOffset, -local1859_29 * 0.72F, local1859_29 * 0.72F
            );
         this.yawOffset = MathHelper.clamp(this.yawOffset, -Math.abs(local1859_24), Math.abs(local1859_24));
         this.pitchOffset = MathHelper.clamp(this.pitchOffset, -Math.abs(local1859_25), Math.abs(local1859_25));
         RotationAngles rotationAngles2 = new RotationAngles(
            yaw + this.yawOffset + MathUtils.randomFloat(-0.08F, 0.08F),
            MathHelper.clamp(pitch + this.pitchOffset + MathUtils.randomFloat(-0.04F, 0.04F), -90.0F, 90.0F)
         );
         RotationManager.getRotationManager()
            .setRotation(
               rotationAngles2,
               new RotationSpeed(
                  Math.abs(MathHelper.wrapDegrees(rotationAngles2.getYaw() - yaw)), Math.abs(rotationAngles2.getPitch() - pitch), 23.0F, 23.0F
               ),
               RotationManager.ReturnMode.SmoothFast
            );
      }
   }

   public void setActive(boolean local1860_1) {
      this.isRotationActive = local1860_1;
   }

   public void reset() {
      this.isRotationActive = false;
      this.yawOffset = this.pitchOffset = 0.0F;
      this.prevYawOffset = this.prevPitchOffset = 0.0F;
      this.lastUpdateMillis = this.targetSwitchMillis = 0L;
   }
}
