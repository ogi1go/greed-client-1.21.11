package greed.client.module.combat;

import greed.client.util.EntityUtils;

import greed.client.MinecraftAccess;
import greed.client.RotationAngles;
import greed.client.RotationManager;
import greed.client.RotationSpeed;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class ShieldBreaker implements MinecraftAccess {
   public void update(LivingEntity livingEntity, boolean local6074_2, float local6074_3) {
      if (livingEntity != null && mc.player != null && mc.world != null) {
         float yaw = mc.player.getYaw();
         float pitch = mc.player.getPitch();
         Vec3d eyePos = mc.player.getEyePos();
         Box boundingBox = livingEntity.getBoundingBox();
         Vec3d vec3d = new Vec3d(livingEntity.getX() - livingEntity.lastRenderX, 0.0, livingEntity.getZ() - livingEntity.lastRenderZ)
            .subtract(mc.player.getVelocity().x, 0.0, mc.player.getVelocity().z)
            .multiply(1.5);
         Vec3d vec3d2 = new Vec3d(boundingBox.getCenter().x, boundingBox.minY + boundingBox.getLengthY() * 0.82, boundingBox.getCenter().z)
            .add(vec3d)
            .subtract(eyePos);
         boolean local6074_10 = vec3d2.x * vec3d2.x + vec3d2.z * vec3d2.z < 0.03F;
         double local6074_11 = Math.max(0.05, Math.hypot(vec3d2.x, vec3d2.z));
         ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
         float local6074_14 = (float)MathHelper.clamp(local6074_11 / 2.0, 0.7, 1.0);
         Vec3d vec3d3 = new Vec3d(boundingBox.getCenter().x, boundingBox.minY + boundingBox.getLengthY() * 0.87, boundingBox.getCenter().z)
            .subtract(eyePos);
         float local6074_16 = local6074_10 ? 0.0F : MathHelper.wrapDegrees((float)Math.toDegrees(Math.atan2(-vec3d2.x, vec3d2.z)) - yaw);
         float local6074_17 = (float)(-Math.toDegrees(Math.atan2(vec3d3.y, Math.max(0.7, Math.hypot(vec3d3.x, vec3d3.z))))) - pitch;
         float local6074_18 = (float)Math.toDegrees(Math.atan2(boundingBox.getLengthX() * 0.5, local6074_11));
         boolean local6074_19 = local6074_2 || AttackController.hasAttackCooldown(-30L);
         boolean local6074_20 = local6074_2 || AttackController.hasAttackCooldown(-30L);
         float attackCooldownProgress = mc.player.getAttackCooldownProgress(0.5F);
         float local6074_22 = local6074_19 ? 0.0F : 0.55F + 0.45F * attackCooldownProgress + 0.2F;
         double local6074_23 = System.currentTimeMillis() / 60.0;
         float local6074_25 = (
               (float)(Math.cos(local6074_23 * 0.7) + Math.sin(local6074_23 * 1.7) * 0.5) * (local6074_18 * 1.8F + 7.0F) + threadLocalRandom.nextFloat(-4.0F, 4.0F)
            )
            * local6074_22;
         float local6074_26 = ((float)(Math.sin(local6074_23 * 0.55) + Math.cos(local6074_23 * 1.3) * 0.4) * 6.0F + threadLocalRandom.nextFloat(-2.0F, 2.0F))
            * local6074_22;
         float local6074_27 = MathHelper.clamp(
            (local6074_19 ? threadLocalRandom.nextFloat(0.85F, 1.0F) : threadLocalRandom.nextFloat(0.55F, 0.85F) * (1.0F - attackCooldownProgress * 0.35F))
               * (1.0F + Math.min(Math.abs(local6074_16), 90.0F) / 300.0F)
               * local6074_14,
            0.1F,
            1.0F
         );
         float local6074_28 = threadLocalRandom.nextFloat(55.0F, 80.0F) * local6074_14;
         float local6074_29 = MathHelper.clamp((local6074_16 + local6074_25) * local6074_27, -local6074_28, local6074_28);
         if (local6074_2
            && !boundingBox.contains(eyePos)
            && !boundingBox.raycast(
                  eyePos, eyePos.add(EntityUtils.getLookVector(yaw + local6074_29, pitch).multiply(local6074_3 + 1.0))
               )
               .isPresent()) {
            local6074_29 = local6074_16;
         }

         float local6074_30 = local6074_20
            ? local6074_17 * threadLocalRandom.nextFloat(0.8F, 0.95F)
            : local6074_26 * 0.35F + local6074_17 * (Math.abs(local6074_17) > 18.0F ? 0.3F : 0.15F);
         if (!local6074_2 && AttackController.hasAttackCooldown(-30L)) {
            local6074_29 *= 0.35F;
            local6074_30 *= 0.35F;
         }

         RotationManager.getRotationManager()
            .setRotation(
               new RotationAngles(yaw + local6074_29, MathHelper.clamp(pitch + local6074_30, -89.0F, 89.0F)),
               new RotationSpeed(local6074_10 ? 8.0F : 35.0F, 22.0F, 30.0F, 30.0F),
               RotationManager.ReturnMode.Empty
            );
      }
   }

   public void reset() {
   }
}
