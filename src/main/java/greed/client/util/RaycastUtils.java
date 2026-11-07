package greed.client.util;

import greed.client.MinecraftAccess;
import greed.client.RotationAngles;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public final class RaycastUtils implements MinecraftAccess {
   public static Entity getEntity2(LivingEntity livingEntity, double yaw, double pitch, double distance) {
      if (livingEntity != null && mc.player != null) {
         Vec3d eyePos = mc.player.getEyePos();
         Vec3d vec3d = eyePos.add(getVec3d(pitch, yaw).multiply(distance));
         EntityHitResult entityHitResult = getEntityHitResult2(
            mc.player,
            eyePos,
            vec3d,
            livingEntity.getBoundingBox().expand(distance),
            entity -> !entity.isSpectator() && entity.canHit(),
            distance
         );
         if (entityHitResult != null && entityHitResult.getEntity() == livingEntity) {
            return livingEntity;
         } else {
            return livingEntity.getBoundingBox().raycast(eyePos, vec3d).isPresent() ? livingEntity : null;
         }
      } else {
         return null;
      }
   }

   public static boolean matchesCriteria(Entity entity, Entity entity2) {
      return entity.getEntityWorld()
            .raycast(
               new RaycastContext(
                  new Vec3d(entity.getX(), entity.getEyeY(), entity.getZ()),
                  new Vec3d(entity2.getX(), entity2.getEyeY(), entity2.getZ()),
                  ShapeType.COLLIDER,
                  FluidHandling.NONE,
                  entity
               )
            )
            .getType()
         == Type.MISS;
   }

   private RaycastUtils() {
   }

   public static Vec3d getVec3d(double pitch, double yaw) {
      double yawRadians = -yaw * (Math.PI / 180.0) - Math.PI;
      double pitchRadians = -pitch * (Math.PI / 180.0);
      return new Vec3d(Math.sin(yawRadians) * -Math.cos(pitchRadians), Math.sin(pitchRadians), Math.cos(yawRadians) * -Math.cos(pitchRadians));
   }

   public static EntityHitResult getEntityHitResult(double distance, RotationAngles rotationAngles, Predicate<Entity> predicate) {
      return mc.getCameraEntity() == null
         ? null
         : getEntityHitResult2(
            mc.getCameraEntity(),
            mc.getCameraEntity().getCameraPosVec(1.0F),
            mc.getCameraEntity().getCameraPosVec(1.0F).add(rotationAngles.getDirectionVector().multiply(distance)),
            mc.getCameraEntity().getBoundingBox().stretch(rotationAngles.getDirectionVector().multiply(distance)).expand(1.0, 1.0, 1.0),
            entity -> !entity.isSpectator() && predicate.test(entity),
            distance
         );
   }

   public static EntityHitResult getEntityHitResult2(
      Entity entity, Vec3d vec3d, Vec3d vec3d2, Box box, Predicate<Entity> predicate, double distance
   ) {
      double closestDistance = distance;
      Entity entity2 = null;
      Vec3d vec3d3 = null;

      for (Entity entity3 : entity.getEntityWorld().getOtherEntities(entity, box, predicate)) {
         Box boundingBox = entity3.getBoundingBox();
         Optional<Vec3d> raycastResult = boundingBox.raycast(vec3d, vec3d2);
         if (boundingBox.contains(vec3d)) {
            if (closestDistance >= 0.0) {
               entity2 = entity3;
               vec3d3 = vec3d;
               closestDistance = 0.0;
            }
         } else if (raycastResult.isPresent()) {
            double hitDistance = vec3d.distanceTo(raycastResult.get());
            if (hitDistance < closestDistance || closestDistance == 0.0) {
               if (entity3.getRootVehicle() == entity.getRootVehicle()) {
                  if (closestDistance == 0.0) {
                     entity2 = entity3;
                     vec3d3 = raycastResult.get();
                  }
               } else {
                  entity2 = entity3;
                  vec3d3 = raycastResult.get();
                  closestDistance = hitDistance;
               }
            }
         }
      }

      return entity2 == null ? null : new EntityHitResult(entity2, vec3d3);
   }

   public static Entity getEntity3(LivingEntity livingEntity, double yaw, double pitch, double distance) {
      if (livingEntity != null && mc.player != null) {
         Vec3d eyePos = mc.player.getEyePos();
         Vec3d vec3d = eyePos.add(getVec3d(pitch, yaw).multiply(distance));
         EntityHitResult entityHitResult = getEntityHitResult2(
            mc.player,
            eyePos,
            vec3d,
            livingEntity.getBoundingBox().expand(distance),
            entity -> !entity.isSpectator(),
            distance
         );
         return entityHitResult == null ? null : entityHitResult.getEntity();
      } else {
         return null;
      }
   }
}
