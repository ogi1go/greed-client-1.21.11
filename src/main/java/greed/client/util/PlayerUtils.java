package greed.client.util;

import greed.client.MinecraftAccess;

import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Vector3f;

public final class PlayerUtils implements MinecraftAccess {
   public static double computeDouble() {
      return 1488.0;
   }

   public static PlayerInput getPlayerInput(PlayerInput playerInput, double local6831_1, float local6831_3) {
      boolean local6831_4 = playerInput.forward();
      boolean local6831_5 = playerInput.backward();
      boolean local6831_6 = playerInput.left();
      boolean local6831_7 = playerInput.right();
      if (local6831_1 >= -90.0F + local6831_3 && local6831_1 <= 90.0F - local6831_3) {
         local6831_4 = true;
      } else if (local6831_1 < -90.0F - local6831_3 || local6831_1 > 90.0F + local6831_3) {
         local6831_5 = true;
      }

      if (local6831_1 >= 0.0F + local6831_3 && local6831_1 <= 180.0F - local6831_3) {
         local6831_7 = true;
      } else if (local6831_1 >= -180.0F + local6831_3 && local6831_1 <= 0.0F - local6831_3) {
         local6831_6 = true;
      }

      return new PlayerInput(local6831_4, local6831_5, local6831_6, local6831_7, playerInput.jump(), playerInput.sneak(), playerInput.sprint());
   }

   private PlayerUtils() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static float computeFloat(
      float local6833_0, float local6833_1, double local6833_2, double local6833_4, double local6833_6, double local6833_8, float local6833_10
   ) {
      double local6833_11 = local6833_6 - local6833_2;
      double local6833_13 = local6833_8 - local6833_4;
      float local6833_15 = (float)(local6833_11 * local6833_11 + local6833_13 * local6833_13);
      float local6833_16 = local6833_1;
      float local6833_17 = mc.player.handSwingProgress;
      if (local6833_15 > 0.0025000002F) {
         float local6833_18 = (float)MathHelper.atan2(local6833_13, local6833_11) * (180.0F / (float)Math.PI) - 90.0F;
         float local6833_19 = MathHelper.abs(MathHelper.wrapDegrees(local6833_0) - local6833_18);
         if (95.0F < local6833_19 && local6833_19 < 265.0F) {
            local6833_16 = local6833_18 - 180.0F;
         } else {
            local6833_16 = local6833_18;
         }
      }

      if (mc.player != null && mc.player.handSwingProgress - 0.3F > 0.0F) {
         local6833_16 = local6833_0;
      }

      float local6833_18 = MathHelper.wrapDegrees(local6833_16 - local6833_1);
      local6833_16 = local6833_1 + local6833_18 * 0.3F;
      float local6833_19 = MathHelper.wrapDegrees(local6833_0 - local6833_16);
      float local6833_20 = 52.0F;
      if (Math.abs(local6833_19) > local6833_20) {
         local6833_16 += local6833_19 - MathHelper.sign(local6833_19) * local6833_20;
      }

      return local6833_16;
   }

   public static String formatText() {
      return "lunarclient:v2.21.5-2540    ";
   }

   public static double computeDouble2(Vec3d vec3d, float local6835_1) {
      float local6835_2 = (float)Math.atan2(-vec3d.x, vec3d.z);
      double local6835_3 = Math.toRadians(MathHelper.wrapDegrees(local6835_1));
      return Math.toDegrees(MathHelper.wrapDegrees(local6835_2 - local6835_3));
   }

   public static final boolean matchesCriteria(int local6836_0) {
      boolean isPressed = mc.options.forwardKey.isPressed();
      boolean isPressed2 = mc.options.leftKey.isPressed();
      boolean isPressed3 = mc.options.backKey.isPressed();
      boolean isPressed4 = mc.options.rightKey.isPressed();
      return local6836_0 == 0 ? isPressed : (local6836_0 == 1 ? isPressed2 : (local6836_0 == 2 ? isPressed3 : local6836_0 == 3 && isPressed4));
   }

   public static final boolean checkState() {
      return matchesCriteria(0);
   }

   public static PlayerInput getPlayerInput2(PlayerInput playerInput, double local6838_1) {
      return getPlayerInput(playerInput, local6838_1, 20.0F);
   }

   public static final boolean checkState2() {
      return matchesCriteria(3);
   }

   public static double computeDouble3(Entity entity) {
      return Math.sqrt(entity.squaredDistanceTo(new Vec3d(entity.lastX, entity.lastY, entity.lastZ)));
   }

   public static boolean checkState3() {
      return mc.player.input.getMovementInput().lengthSquared() > 0.0F;
   }

   public static final boolean checkState4() {
      return matchesCriteria(1);
   }

   public static final boolean checkState5() {
      return matchesCriteria(2);
   }

   public static float computeFloat2(int local6844_0) {
      Vector3f vector3f = new Vector3f(0.0F, (float)mc.player.getVelocity().y, 0.0F);
      if (vector3f.y != 0.0F && !mc.player.isOnGround()) {
         float local6844_2 = 0.0F;
         double local6844_3 = 0.08;
         boolean local6844_5 = vector3f.y <= 0.0;
         if (local6844_5 && mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
            local6844_3 = 0.01;
         }

         for (int index = 0; index < local6844_0 + 1; index++) {
            double local6844_7 = vector3f.y;
            local6844_7 -= local6844_3;
            vector3f.y = (float)(local6844_7 * 0.98F);
            if (vector3f.y > 0.0F) {
               local6844_2 = 0.0F;
            } else {
               local6844_2 -= vector3f.y;
            }
         }

         return local6844_2;
      } else {
         return 0.0F;
      }
   }

   public static PlayerUtils.SubContext getNested0517(PlayerEntity playerEntity, int local6845_1) {
      PlayerUtils.SubContext local6845_2 = new PlayerUtils.SubContext();
      local6845_2.vector3f2 = playerEntity.getEntityPos().toVector3f();
      local6845_2.vector3f3 = playerEntity.getVelocity().toVector3f();
      double local6845_3 = playerEntity.getEntityPos().y;

      for (int index = 0; index < local6845_1; index++) {
         double local6845_6 = 0.08;
         boolean local6845_8 = playerEntity.getVelocity().y <= 0.0;
         if (local6845_8 && playerEntity.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
            local6845_6 = 0.01;
         }

         local6845_2.vector3f3 = local6845_2.vector3f3.add(0.0F, (float)(-local6845_6 / 4.0), 0.0F);
         BlockHitResult blockHitResult = playerEntity.getEntityWorld()
            .raycast(
               new RaycastContext(
                  new Vec3d(local6845_2.vector3f2),
                  new Vec3d(local6845_2.vector3f2.add(local6845_2.vector3f3)),
                  ShapeType.OUTLINE,
                  FluidHandling.WATER,
                  playerEntity
               )
            );
         local6845_2.vector3f2 = local6845_2.vector3f2.add(local6845_2.vector3f3);
      }

      local6845_2.floatVal = computeFloat2(local6845_1);
      return local6845_2;
   }

   public static final float computeFloat3(float local6846_0) {
      return local6846_0
         + (
            !checkState4() || !checkState2() || checkState() && checkState5() || !checkState() && !checkState5()
               ? (
                  !checkState() || !checkState5() || checkState4() && checkState2() || !checkState4() && !checkState2()
                     ? (
                        (!checkState4() || !checkState2() || checkState() && checkState5()) && (!checkState() || !checkState5() || checkState4() && checkState2())
                           ? (
                              !checkState4() && !checkState2() && !checkState5()
                                 ? 0
                                 : (
                                       checkState() && !checkState5()
                                          ? 45
                                          : (
                                             !checkState5() || checkState()
                                                ? (!checkState() && !checkState5() || checkState() && checkState5() ? 90 : 0)
                                                : (!checkState4() && !checkState2() ? 180 : 135)
                                          )
                                    )
                                    * (checkState4() ? -1 : 1)
                           )
                           : 0
                     )
                     : (checkState4() ? -90 : (checkState2() ? 90 : 0))
               )
               : (checkState() ? 0 : (checkState5() ? 180 : 0))
         );
   }

   public static class SubContext {
      public boolean flag = false;
      public float floatVal = 0.0F;
      public Vector3f vector3f2 = new Vector3f();
      public Vector3f vector3f3 = new Vector3f();
   }
}
