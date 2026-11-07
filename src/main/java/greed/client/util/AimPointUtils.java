package greed.client.util;

import greed.client.util.MathUtils;
import greed.client.MinecraftAccess;
import greed.client.RotationAngles;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class AimPointUtils implements MinecraftAccess {
   private static Vec3d vec3d4 = Vec3d.ZERO;
   private static Vec3d vec3d5 = Vec3d.ZERO;
   private static float floatVal;
   private static float floatVal2;
   private static long timeMillis;

   public static RotationAngles getRotationAngles(Vec3d vec3d) {
      double local7498_1 = Math.sqrt(vec3d.x * vec3d.x + vec3d.z * vec3d.z);
      float local7498_3 = (float)Math.toDegrees(Math.atan2(vec3d.z, vec3d.x)) - 90.0F;
      float local7498_4 = (float)(-Math.toDegrees(Math.atan2(vec3d.y, local7498_1)));
      return new RotationAngles(MathHelper.wrapDegrees(local7498_3), MathHelper.clamp(local7498_4, -90.0F, 90.0F));
   }

   private AimPointUtils() {
   }

   private static Vec3d getVec3d(LivingEntity livingEntity, Vec3d vec3d) {
      Vec3d vec3d2 = livingEntity.getBoundingBox()
         .raycast(
            vec3d,
            vec3d.add(
               new RotationAngles(mc.player.getYaw(), mc.player.getPitch())
                  .getDirectionVector()
                  .multiply(mc.player.distanceTo(livingEntity) + livingEntity.getWidth() + 1.0F)
            )
         )
         .orElse(
            new Vec3d(
               MathHelper.clamp(vec3d.x, livingEntity.getBoundingBox().minX, livingEntity.getBoundingBox().maxX),
               MathHelper.clamp(vec3d.y, livingEntity.getBoundingBox().minY, livingEntity.getBoundingBox().maxY),
               MathHelper.clamp(vec3d.z, livingEntity.getBoundingBox().minZ, livingEntity.getBoundingBox().maxZ)
            )
         );
      float local7500_3 = computeFloat(vec3d, vec3d2);

      for (float local7500_7 : new float[]{0.35F, 0.55F, 0.75F, 0.9F}) {
         Vec3d vec3d3 = new Vec3d(
            MathHelper.clamp(vec3d.x, livingEntity.getBoundingBox().minX, livingEntity.getBoundingBox().maxX),
            livingEntity.getBoundingBox().minY + (livingEntity.getBoundingBox().maxY - livingEntity.getBoundingBox().minY) * local7500_7,
            MathHelper.clamp(vec3d.z, livingEntity.getBoundingBox().minZ, livingEntity.getBoundingBox().maxZ)
         );
         float local7500_9 = computeFloat(vec3d, vec3d3);
         if (local7500_9 < local7500_3) {
            local7500_3 = local7500_9;
            vec3d2 = vec3d3;
         }
      }

      return vec3d2;
   }

   public static Vec3d getVec3d2(LivingEntity livingEntity, boolean local7501_1, boolean local7501_2) {
      floatVal = MathHelper.lerp(MathUtils.computeFloat10(0.65F, 0.85F), floatVal, MathUtils.computeFloat10(0.65F, 0.85F));
      floatVal2 = MathHelper.lerp(MathUtils.computeFloat10(0.65F, 0.85F), floatVal2, MathUtils.computeFloat10(0.65F, 0.85F));
      if (vec3d5 == Vec3d.ZERO) {
         vec3d5 = livingEntity.getLerpedPos(1.0F);
      }

      if (vec3d4 == Vec3d.ZERO) {
         vec3d4 = mc.player.getCameraPosVec(1.0F);
      }

      if (!local7501_2
         && System.currentTimeMillis() - timeMillis > (local7501_1 ? MathUtils.computeLong(25L, 67L) : MathUtils.computeLong(87L, 185L))) {
         timeMillis = System.currentTimeMillis();
         vec3d5 = new Vec3d(
            vec3d5.x,
            MathHelper.lerp(
               MathUtils.computeFloat10(0.29F, Math.abs(livingEntity.getY() - mc.player.getY()) > 1.0 ? 0.45F : 0.69F),
               vec3d5.y,
               livingEntity.getY() + livingEntity.getHeight() / 2.0F
            ),
            vec3d5.z
         );
      }

      if (System.currentTimeMillis() - timeMillis > (local7501_1 ? MathUtils.computeLong(25L, 67L) : MathUtils.computeLong(55L, 115L))) {
         timeMillis = System.currentTimeMillis();
         vec3d4 = new Vec3d(
            vec3d4.x, MathHelper.lerp(MathUtils.computeFloat10(0.15F, 0.35F), vec3d4.y, mc.player.getCameraPosVec(1.0F).y), vec3d4.z
         );
      }

      vec3d4 = new Vec3d(
         MathHelper.lerp(floatVal, vec3d4.x, mc.player.getCameraPosVec(1.0F).x),
         vec3d4.y,
         MathHelper.lerp(floatVal2, vec3d4.z, mc.player.getCameraPosVec(1.0F).z)
      );
      if (local7501_2) {
         Vec3d vec3d = getVec3d(livingEntity, vec3d4);
         vec3d5 = new Vec3d(
            MathHelper.lerp(floatVal, vec3d5.x, vec3d.x),
            MathHelper.lerp(local7501_1 ? 0.45F : 0.65F, vec3d5.y, vec3d.y),
            MathHelper.lerp(floatVal2, vec3d5.z, vec3d.z)
         );
         return vec3d5.subtract(vec3d4);
      } else {
         vec3d5 = new Vec3d(
            MathHelper.lerp(floatVal, vec3d5.x, livingEntity.getX()), vec3d5.y, MathHelper.lerp(floatVal2, vec3d5.z, livingEntity.getZ())
         );
         return vec3d5.subtract(vec3d4);
      }
   }

   public static void updateState() {
      vec3d4 = Vec3d.ZERO;
      vec3d5 = Vec3d.ZERO;
      floatVal = MathUtils.computeFloat10(0.38F, 0.55F);
      floatVal2 = MathUtils.computeFloat10(0.38F, 0.55F);
      timeMillis = 0L;
   }

   private static float computeFloat(Vec3d vec3d, Vec3d vec3d2) {
      return (float)Math.hypot(
         MathHelper.wrapDegrees(
            Math.toDegrees(Math.atan2(vec3d2.subtract(vec3d).z, vec3d2.subtract(vec3d).x)) - 90.0 - mc.player.getYaw()
         ),
         -Math.toDegrees(Math.atan2(vec3d2.subtract(vec3d).y, Math.hypot(vec3d2.subtract(vec3d).x, vec3d2.subtract(vec3d).z)))
            - mc.player.getPitch()
      );
   }

   public static Vec3d getVec3d3(LivingEntity livingEntity, boolean local7505_1) {
      return getVec3d2(livingEntity, local7505_1, false);
   }
}
