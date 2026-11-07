package greed.client.module.combat;

import greed.client.util.EntityUtils;

import greed.client.module.combat.AttackAuraModule;
import greed.client.util.Easings;
import greed.client.util.MathUtils;
import greed.client.MinecraftAccess;
import greed.client.RotationAngles;
import greed.client.RotationManager;
import greed.client.RotationSpeed;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class PredictiveAuraRotation implements MinecraftAccess {
   private float floatVal;
   private float floatVal2;
   private float floatVal3;
   private float floatVal4;
   private float floatVal5;
   private float floatVal6;
   private float floatVal7;
   private long timeMillis;
   private long timeMillis2;
   private long timeMillis3;

   public void update(LivingEntity livingEntity, boolean local6308_2, float local6308_3) {
      if (livingEntity != null && mc.player != null && mc.world != null) {
         AttackAuraModule attackAuraModule = AttackAuraModule.getInstance();
         long local6308_5 = System.currentTimeMillis();
         if (this.timeMillis3 == 0L) {
            this.timeMillis3 = local6308_5;
         }

         if (local6308_5 - this.timeMillis > this.timeMillis2) {
            this.timeMillis = local6308_5;
            this.timeMillis2 = (long)MathUtils.randomFloat(25.0F, 50.0F);
            this.floatVal4 = MathUtils.randomFloat(248.0F, 360.0F);
            this.floatVal5 = MathUtils.randomFloat(34.0F, 54.0F);
            this.floatVal6 = MathUtils.randomFloat(8.0F, 16.0F);
            this.floatVal7 = MathUtils.randomFloat(0.04F, 0.12F) * (ThreadLocalRandom.current().nextBoolean() ? 1.0F : -1.0F);
         }

         Vec3d vec3d = mc.player.getCameraPosVec(mc.getRenderTickCounter().getTickProgress(false));
         Vec3d vec3d2 = livingEntity.getEntityPos().add(0.0, Math.max(0.1, livingEntity.getHeight() - 0.1), 0.0).subtract(vec3d).normalize();
         float local6308_9 = (float)Math.toDegrees(Math.atan2(-vec3d2.x, vec3d2.z));
         float local6308_10 = (float)MathHelper.clamp(-Math.toDegrees(Math.atan2(vec3d2.y, Math.hypot(vec3d2.x, vec3d2.z))), -90.0, 90.0);
         boolean local6308_11 = false;
         if (local6308_2 && EntityUtils.getDistanceToBox(livingEntity) < local6308_3) {
            this.floatVal = attackAuraModule.randomRange(1.9F, 2.3F);
         }

         if (this.floatVal > attackAuraModule.randomRange(0.43F, 0.75F)) {
            local6308_11 = true;
            this.floatVal = attackAuraModule.randomRange(0.13F, 0.25F);
         }

         float local6308_12 = MathHelper.wrapDegrees(local6308_9 - mc.player.getYaw());
         float local6308_13 = local6308_10 - mc.player.getPitch();
         float local6308_14 = (float)Math.hypot(local6308_12, local6308_13);
         float local6308_15 = this.floatVal4
            * (float)Easings.LINEAR.apply(MathUtils.clampUnit(local6308_14 / this.floatVal5))
            * MathHelper.lerp(MathHelper.clamp((float)(local6308_5 - this.timeMillis3) / 160.0F, 0.0F, 1.0F), 0.78F, 1.0F)
            * (local6308_11 ? 1.25F : 1.0F)
            * 3.6F;
         float local6308_16 = this.floatVal6 * (local6308_11 ? 4.0F : 1.0F) * 3.0F;
         this.floatVal2 = this.floatVal2
            + MathHelper.clamp(
               local6308_15 * (local6308_14 < 0.001F ? 0.0F : local6308_12 / local6308_14) * 1.25F / (EntityUtils.isIntersecting(livingEntity) ? 2.0F : 1.0F)
                  - this.floatVal2,
               -local6308_16 * 1.2F,
               local6308_16 * 1.2F
            );
         this.floatVal3 = this.floatVal3
            + MathHelper.clamp(
               local6308_15 * (local6308_14 < 0.001F ? 0.0F : local6308_13 / local6308_14) + this.floatVal2 * this.floatVal7 - this.floatVal3,
               -local6308_16 * 0.68F,
               local6308_16 * 0.68F
            );
         this.floatVal2 = MathHelper.clamp(this.floatVal2, -Math.abs(local6308_12), Math.abs(local6308_12));
         this.floatVal3 = MathHelper.clamp(
            this.floatVal3,
            -Math.abs(local6308_13) - Math.abs(this.floatVal2 * this.floatVal7),
            Math.abs(local6308_13) + Math.abs(this.floatVal2 * this.floatVal7)
         );
         RotationManager.getRotationManager()
            .setRotation(
               new RotationAngles(local6308_9, local6308_10),
               new RotationSpeed(
                  Math.min(Math.abs(this.floatVal2), EntityUtils.calculateYawSpeed(new RotationAngles(local6308_9, local6308_10), 0.5F, 0.9F)) * 3.9F,
                  Math.min(Math.abs(this.floatVal3), EntityUtils.calculatePitchSpeed(new RotationAngles(local6308_9, local6308_10), 0.9F, 1.6F)) * 3.9F,
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
      this.timeMillis3 = 0L;
   }
}
