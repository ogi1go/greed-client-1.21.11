package greed.client.module.combat;

import greed.client.util.EntityUtils;

import greed.client.module.combat.AttackAuraModule;
import greed.client.util.Easings;
import greed.client.module.combat.MaceHelperModule;
import greed.client.util.MathUtils;
import greed.client.MinecraftAccess;
import greed.client.RotationAngles;
import greed.client.RotationManager;
import greed.client.RotationSpeed;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class TimedAuraRotation implements MinecraftAccess {
   private float floatVal;
   private float floatVal2;
   private float floatVal3;
   private long timeMillis;
   private long timeMillis2;
   private float floatVal4;
   private float floatVal5;
   private float floatVal6;
   private float floatVal7;
   private float floatVal8;
   private float floatVal9;
   private float floatVal10;
   private long timeMillis3;
   private long timeMillis4;
   private long timeMillis5;
   private long timeMillis6;
   private float floatVal11;
   private float floatVal12;
   private float floatVal13;
   private float floatVal14;

   public void update(LivingEntity livingEntity, boolean local1812_2, float local1812_3) {
      if (livingEntity != null && mc.player != null && mc.world != null) {
         AttackAuraModule attackAuraModule = AttackAuraModule.getInstance();
         long local1812_5 = System.currentTimeMillis();
         boolean local1812_7 = attackAuraModule.canPerformMaceSmash() || MaceHelperModule.getInstance().isWindChargeJumping() && !mc.player.isOnGround();
         if (this.timeMillis5 == 0L) {
            this.timeMillis5 = local1812_5;
         }

         if (this.floatVal12 == 0.0F) {
            this.floatVal12 = MathUtils.randomFloat(0.24F, 0.72F);
         }

         if (local1812_5 - this.timeMillis3 > this.timeMillis4) {
            this.timeMillis3 = local1812_5;
            this.timeMillis4 = (long)MathUtils.randomFloat(25.0F, 50.0F);
            this.floatVal6 = MathUtils.randomFloat(142.0F, 200.0F);
            this.floatVal7 = MathUtils.randomFloat(34.0F, 54.0F);
            this.floatVal8 = MathUtils.randomFloat(7.0F, 14.0F);
            this.floatVal9 = MathUtils.randomFloat(0.04F, 0.12F) * (ThreadLocalRandom.current().nextBoolean() ? 1.0F : -1.0F);
            this.floatVal10 = MathUtils.randomFloat(0.4F, 0.9F);
         }

         Vec3d vec3d = mc.player.getCameraPosVec(mc.getRenderTickCounter().getTickProgress(false));
         boolean local1812_9 = AttackAuraModule.getInstance().wallAttackMode.isSelected("Неполноценные")
            && !EntityUtils.hasLineOfSight(vec3d, livingEntity.getBoundingBox().getCenter())
            && EntityUtils.canSeeVector(vec3d, livingEntity.getBoundingBox().getCenter());
         Vec3d vec3d2 = this.getVec3d(
               livingEntity,
               local1812_7 ? 1.0 : this.floatVal12,
               !local1812_7 && !local1812_9 ? this.floatVal13 : 0.0,
               0.0,
               !local1812_7 && !local1812_9 ? this.floatVal14 : 0.0
            )
            .subtract(vec3d)
            .normalize();
         float local1812_11 = (float)Math.toDegrees(Math.atan2(-vec3d2.x, vec3d2.z));
         float local1812_12 = (float)MathHelper.clamp(-Math.toDegrees(Math.atan2(vec3d2.y, Math.hypot(vec3d2.x, vec3d2.z))), -90.0, 90.0);
         float yaw = mc.player.getYaw();
         float pitch = mc.player.getPitch();
         boolean local1812_15 = false;
         if (local1812_2 && EntityUtils.getDistanceToBox(livingEntity) < local1812_3) {
            this.floatVal = attackAuraModule.randomRange(1.9F, 2.3F);
            this.timeMillis = local1812_5;
            this.timeMillis2 = (long)ThreadLocalRandom.current().nextDouble(200.0, 250.0);
            this.floatVal12 = MathUtils.randomFloat(0.54F, 0.72F);
            this.floatVal13 = MathUtils.randomFloat(-0.12F, 0.12F);
            this.floatVal14 = MathUtils.randomFloat(-0.12F, 0.12F);
         }

         if (this.floatVal > attackAuraModule.randomRange(0.43F, 0.75F)) {
            local1812_15 = true;
            this.floatVal = attackAuraModule.randomRange(0.13F, 0.25F);
         }

         if (local1812_15) {
            this.floatVal2 = local1812_11;
            this.floatVal3 = local1812_12;
         } else if (local1812_5 - this.timeMillis < this.timeMillis2) {
            local1812_11 = this.floatVal2;
            local1812_12 = this.floatVal3;
         }

         if (EntityUtils.matchesCriteria(
            mc.player.getEyePos(), new RotationAngles(yaw, pitch).getDirectionVector(), livingEntity.getBoundingBox(), local1812_3 + 1.0
         )) {
            if (this.timeMillis6 == 0L) {
               this.timeMillis6 = local1812_5;
            }
         } else {
            this.timeMillis6 = 0L;
         }

         float local1812_16 = MathHelper.clamp(((float)(this.timeMillis6 == 0L ? 0L : local1812_5 - this.timeMillis6) - 250.0F) / 300.0F, 0.0F, 1.0F);
         if (local1812_16 >= this.floatVal11) {
            this.floatVal11 = local1812_16;
         } else {
            this.floatVal11 = this.floatVal11 + (local1812_16 - this.floatVal11) * 4.0F;
         }

         float local1812_17 = MathHelper.wrapDegrees(local1812_11 - yaw);
         float local1812_18 = local1812_12 - pitch;
         float local1812_19 = (float)Math.hypot(local1812_17, local1812_18);
         float local1812_20 = this.floatVal6
            * (float)Easings.LINEAR.apply(MathUtils.clampUnit(local1812_19 / this.floatVal7))
            * MathHelper.lerp(MathHelper.clamp((float)(local1812_5 - this.timeMillis5) / 220.0F, 0.0F, 1.0F), 0.78F, 1.0F)
            * (1.0F - MathUtils.clampUnit(this.floatVal11) * (local1812_7 ? 0.35F : 1.0F))
            * (local1812_15 ? 1.25F : 1.0F)
            * (local1812_7 ? 3.2F : 1.0F)
            * (mc.player.handSwingTicks > attackAuraModule.randomRange(0.2F, 0.4F) && !local1812_7 ? 0.5F : 1.0F);
         float local1812_21 = this.floatVal8 * (local1812_15 ? 4.0F : 1.0F) * (local1812_7 ? 2.6F : 1.0F);
         this.floatVal4 = this.floatVal4
            + MathHelper.clamp(
               local1812_20 * (local1812_19 < 0.001F ? 0.0F : local1812_17 / local1812_19) * 1.25F / (EntityUtils.isIntersecting(livingEntity) ? 2.0F : 1.0F)
                  - this.floatVal4,
               -local1812_21 * (local1812_7 ? 0.6F : 1.2F),
               local1812_21 * (local1812_7 ? 0.6F : 1.2F)
            );
         this.floatVal5 = this.floatVal5
            + MathHelper.clamp(
               local1812_20 * (local1812_19 < 0.001F ? 0.0F : local1812_18 / local1812_19) + this.floatVal4 * this.floatVal9 - this.floatVal5,
               -local1812_21 * 0.68F,
               local1812_21 * 0.68F
            );
         this.floatVal4 = MathHelper.clamp(this.floatVal4, -Math.abs(local1812_17), Math.abs(local1812_17));
         this.floatVal5 = MathHelper.clamp(
            this.floatVal5,
            -Math.abs(local1812_18) - Math.abs(this.floatVal4 * this.floatVal9),
            Math.abs(local1812_18) + Math.abs(this.floatVal4 * this.floatVal9)
         );
         if (local1812_19 < this.floatVal10) {
            this.floatVal4 = this.floatVal5 = 0.0F;
         }

         RotationManager.getRotationManager()
            .setRotation(
               new RotationAngles(local1812_11, local1812_12),
               new RotationSpeed(
                  Math.min(
                        Math.abs(this.floatVal4),
                        EntityUtils.calculateYawSpeed(new RotationAngles(local1812_11, local1812_12), local1812_7 ? 0.6F : 1.0F, local1812_7 ? 1.0F : 1.5F)
                     )
                     * (local1812_7 ? 3.4F : 1.0F),
                  Math.min(
                        Math.abs(this.floatVal5),
                        EntityUtils.calculatePitchSpeed(new RotationAngles(local1812_11, local1812_12), local1812_7 ? 1.0F : 2.5F, local1812_7 ? 1.8F : 4.0F)
                     )
                     * (local1812_7 ? 3.4F : 1.0F),
                  25.0F,
                  25.0F
               ),
               RotationManager.ReturnMode.Empty
            );
      }
   }

   public void reset() {
      this.floatVal = 0.0F;
      this.floatVal2 = this.floatVal3 = 0.0F;
      this.timeMillis = this.timeMillis2 = 0L;
      this.floatVal4 = this.floatVal5 = 0.0F;
      this.timeMillis3 = this.timeMillis4 = 0L;
      this.timeMillis5 = 0L;
      this.timeMillis6 = 0L;
      this.floatVal11 = 0.0F;
      this.floatVal12 = 0.0F;
      this.floatVal13 = this.floatVal14 = 0.0F;
   }

   private Vec3d getVec3d(LivingEntity livingEntity, double local1815_2, double local1815_4, double local1815_6, double local1815_8) {
      return livingEntity.getEntityPos()
         .add(
            local1815_4, MathHelper.clamp(livingEntity.getHeight() * local1815_2 + local1815_6, 0.1, Math.max(0.1, livingEntity.getHeight() - 0.1)), local1815_8
         );
   }
}
