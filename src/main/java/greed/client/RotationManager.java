package greed.client;

import greed.client.event.*;
import greed.client.util.MathUtils;
import greed.client.util.EntityUtils;
import greed.client.util.MouseSensitivity;
import greed.client.util.Stopwatch;

import net.minecraft.util.math.MathHelper;

public final class RotationManager implements MinecraftAccess {
   private static RotationManager rotationManager;
   private float floatVal;
   private float floatVal2;
   private float clientYaw;
   private float clientPitch;
   private float floatVal3;
   private float floatVal4;
   private float floatVal5;
   private float floatVal6;
   private RotationAngles rotationAngles;
   private RotationSpeed rotationSpeed;
   private boolean isRotatingActive;
   private long currentTick;
   private long lastRotationTick;
   private long rotationTimeoutMillis;
   private long rotationEndMillis = -1L;
   private final Stopwatch stopwatch = new Stopwatch();
   private RotationManager.ReturnMode returnMode = RotationManager.ReturnMode.Smooth;
   private final EventListener<PlayerTickEvent> rotationTickListener = playerTickEvent -> {
      if (mc.player != null) {
         this.currentTick++;
         this.floatVal3 = this.floatVal3 + (this.floatVal5 - this.floatVal3) * 0.4F;
         this.floatVal4 = this.floatVal4 + (this.floatVal6 - this.floatVal4) * 0.4F;
         this.floatVal5 = this.floatVal6 = 0.0F;
         if (this.currentTick - this.lastRotationTick <= 1L && this.rotationAngles != null && this.rotationSpeed != null) {
            if (!this.isRotatingActive) {
               this.clientYaw = mc.player.getYaw();
               this.clientPitch = mc.player.getPitch();
               this.isRotatingActive = true;
            }

            this.applyRotationStep(this.rotationAngles, this.rotationSpeed.getYawSpeed(), this.rotationSpeed.getPitchSpeed());
            this.syncCameraForKeepMode();
         } else if (this.isRotatingActive && this.rotationAngles != null && !this.stopwatch.hasExceededMillis(this.rotationTimeoutMillis)) {
            this.applyRotationStep(this.rotationAngles, this.rotationSpeed.getYawSpeed(), this.rotationSpeed.getPitchSpeed());
            this.syncCameraForKeepMode();
         } else if (this.isRotatingActive) {
            if (this.returnMode == RotationManager.ReturnMode.SmoothFast) {
               float local854_2 = MathHelper.wrapDegrees(this.clientYaw - mc.player.getYaw());
               float local854_3 = this.clientPitch - mc.player.getPitch();
               if (Math.abs(local854_2) <= this.rotationSpeed.getMaxYawSpeed() && Math.abs(local854_3) <= this.rotationSpeed.getMaxPitchSpeed()) {
                  mc.player.setYaw(mc.player.headYaw = this.clientYaw);
                  mc.player.setPitch(this.clientPitch);
                  this.rotationEndMillis = -1L;
                  this.isRotatingActive = false;
                  return;
               }

               float local854_4 = (float)Math.hypot(local854_2, local854_3);
               float local854_5 = mc.player.getAttackCooldownProgress(1.0F) >= 1.0F ? MathUtils.randomFloat(35.0F, 45.0F) : 0.0F;
               float local854_6 = Math.abs(local854_2 / local854_4) * local854_5;
               float local854_7 = Math.abs(local854_3 / local854_4) * local854_5;
               float local854_8 = MathUtils.randomFloat(-0.4F, 0.4F);
               float local854_9 = MathUtils.randomFloat(-0.2F, 0.2F);
               float local854_10 = MathHelper.lerp(
                  0.85F, mc.player.getYaw(), mc.player.getYaw() + MathHelper.clamp(local854_2, -local854_6, local854_6) + local854_8
               );
               float local854_11 = MathHelper.lerp(
                  0.85F, mc.player.getPitch(), mc.player.getPitch() + MathHelper.clamp(local854_3, -local854_7, local854_7) + local854_9
               );
               mc.player
                  .setYaw(
                     mc.player.headYaw = mc.player.headYaw
                        + MouseSensitivity.quantizeSensitivity(MathHelper.wrapDegrees(local854_10 - mc.player.getYaw()))
                  );
               mc.player
                  .setPitch(
                     MathHelper.clamp(mc.player.getPitch() + MouseSensitivity.quantizeSensitivity(local854_11 - mc.player.getPitch()), -90.0F, 90.0F)
                  );
               return;
            }

            if (this.returnMode == RotationManager.ReturnMode.Empty || this.returnMode == RotationManager.ReturnMode.Keep) {
               this.isRotatingActive = false;
               return;
            }

            float local854_2 = MathHelper.wrapDegrees(this.clientYaw - mc.player.getYaw());
            float local854_3 = this.clientPitch - mc.player.getPitch();
            float local854_4 = Math.min(Math.abs(local854_2), this.rotationSpeed.getMaxYawSpeed());
            float local854_5 = Math.min(Math.abs(local854_3), this.rotationSpeed.getMaxPitchSpeed());
            mc.player
               .setYaw(
                  mc.player.headYaw = mc.player.headYaw + MouseSensitivity.quantizeSensitivity(MathHelper.clamp(local854_2, -local854_4, local854_4))
               );
            mc.player
               .setPitch(
                  MathHelper.clamp(
                     mc.player.getPitch() + MouseSensitivity.quantizeSensitivity(MathHelper.clamp(local854_3, -local854_5, local854_5)), -90.0F, 90.0F
                  )
               );
            if (Math.abs(local854_2) <= this.rotationSpeed.getMaxYawSpeed() && Math.abs(local854_3) <= this.rotationSpeed.getMaxPitchSpeed()) {
               mc.player.setYaw(mc.player.headYaw = this.clientYaw);
               mc.player.setPitch(this.clientPitch);
               this.isRotatingActive = false;
            }
         }
      }
   };

   public void setRotationWithTimeout(RotationAngles newRotationAngles, RotationSpeed newRotationSpeed, long local837_3) {
      this.setRotationWithMode(newRotationAngles, newRotationSpeed, local837_3, RotationManager.ReturnMode.Smooth);
   }

   public void syncCameraAngles() {
      if (mc.player != null && this.isRotatingActive) {
         this.clientYaw = mc.player.getYaw();
         this.clientPitch = mc.player.getPitch();
      }
   }

   /**
    * В режиме {@link ReturnMode#Keep} камера каждый тик приклеивается к текущей наводке,
    * чтобы ротация была видимой и по завершении осталась на цели, а не откатилась
    * к положению, в котором игрок смотрел до начала ротации.
    */
   private void syncCameraForKeepMode() {
      if (this.returnMode == RotationManager.ReturnMode.Keep && mc.player != null) {
         this.clientYaw = mc.player.getYaw();
         this.clientPitch = mc.player.getPitch();
      }
   }

   public static void correctMovementStraight(MovementInputEvent movementInputEvent, float local839_1) {
      float local839_2 = movementInputEvent.getForward();
      float local839_3 = movementInputEvent.getSideways();
      double local839_4 = MathHelper.wrapDegrees(Math.toDegrees(calculateMovementAngle(local839_1, local839_2, local839_3)));
      if (local839_2 != 0.0F || local839_3 != 0.0F) {
         float local839_6 = 0.0F;
         float local839_7 = 0.0F;
         float local839_8 = Float.MAX_VALUE;

         for (float local839_9 = -1.0F; local839_9 <= 1.0F; local839_9++) {
            for (float local839_10 = -1.0F; local839_10 <= 1.0F; local839_10++) {
               if (local839_10 != 0.0F || local839_9 != 0.0F) {
                  double local839_11 = MathHelper.wrapDegrees(Math.toDegrees(calculateMovementAngle(mc.player.getYaw(), local839_9, local839_10)));
                  double local839_13 = Math.abs(local839_4 - local839_11);
                  if (local839_13 < local839_8) {
                     local839_8 = (float)local839_13;
                     local839_6 = local839_9;
                     local839_7 = local839_10;
                  }
               }
            }
         }

         movementInputEvent.setForward(local839_6);
         movementInputEvent.setSideways(local839_7);
      }
   }

   public float getYawVelocity() {
      return this.floatVal3;
   }

   public void setRotationSimple(RotationAngles newRotationAngles, RotationSpeed newRotationSpeed) {
      this.setRotationWithTimeout(newRotationAngles, newRotationSpeed, 0L);
   }

   public void resetRotation() {
      this.isRotatingActive = false;
      this.rotationAngles = null;
      this.rotationEndMillis = -1L;
   }

   public boolean handleMouseInput(double local843_1, double local843_3) {
      if (!this.isRotatingActive) {
         return false;
      }

      this.clientYaw += (float)local843_1 * 0.15F;
      this.clientPitch = MathHelper.clamp(this.clientPitch + (float)local843_3 * 0.15F, -90.0F, 90.0F);
      this.floatVal5 = this.floatVal5 + Math.abs((float)local843_1 * 0.15F);
      this.floatVal6 = this.floatVal6 + Math.abs((float)local843_3 * 0.15F);
      return true;
   }

   public float getTargetPitch() {
      return this.clientPitch;
   }

   private RotationManager() {
   }

   private static double calculateMovementAngle(float local846_0, float local846_1, float local846_2) {
      return Math.atan2(
         local846_1 * Math.cos(Math.toRadians(local846_0)) + local846_2 * Math.sin(Math.toRadians(local846_0)),
         local846_2 * Math.cos(Math.toRadians(local846_0)) - local846_1 * Math.sin(Math.toRadians(local846_0))
      );
   }

   public float getPitchVelocity() {
      return this.floatVal;
   }

   public static RotationManager getRotationManager() {
      if (rotationManager == null) {
         rotationManager = new RotationManager();
         EventBus.getInstance().subscribe(rotationManager);
      }

      return rotationManager;
   }

   public static void correctMovement(MovementInputEvent movementInputEvent, float local849_1, float local849_2) {
      if (movementInputEvent.getForward() != 0.0F || movementInputEvent.getSideways() != 0.0F) {
         double local849_3 = MathHelper.wrapDegrees(
            (float)Math.toDegrees(calculateMovementAngle(mc.player.isGliding() ? local849_1 : local849_2, movementInputEvent.getForward(), movementInputEvent.getSideways()))
         );
         float local849_5 = 0.0F;
         float local849_6 = 0.0F;
         float local849_7 = Float.MAX_VALUE;

         for (float local849_8 = -1.0F; local849_8 <= 1.0F; local849_8++) {
            for (float local849_9 = -1.0F; local849_9 <= 1.0F; local849_9++) {
               if ((local849_9 != 0.0F || local849_8 != 0.0F)
                  && (float)Math.abs(local849_3 - MathHelper.wrapDegrees((float)Math.toDegrees(calculateMovementAngle(local849_1, local849_8, local849_9)))) < local849_7) {
                  local849_7 = (float)Math.abs(local849_3 - MathHelper.wrapDegrees((float)Math.toDegrees(calculateMovementAngle(local849_1, local849_8, local849_9))));
                  local849_5 = local849_8;
                  local849_6 = local849_9;
               }
            }
         }

         movementInputEvent.setForward(local849_5);
         movementInputEvent.setSideways(local849_6);
      }
   }

   public void setRotation(RotationAngles newRotationAngles, RotationSpeed newRotationSpeed, RotationManager.ReturnMode newReturnMode) {
      this.setRotationWithMode(newRotationAngles, newRotationSpeed, 0L, newReturnMode);
   }

   public float getSmoothedYawVelocity() {
      return this.floatVal4;
   }

   public float getSmoothedPitchVelocity() {
      return this.floatVal2;
   }

   private boolean applyRotationStep(RotationAngles newRotationAngles, float local853_2, float local853_3) {
      float local853_4 = MathHelper.wrapDegrees(newRotationAngles.getYaw() - mc.player.getYaw());
      float local853_5 = newRotationAngles.getPitch() - mc.player.getPitch();
      float local853_6 = Math.min(Math.abs(local853_4), local853_2);
      float local853_7 = Math.min(Math.abs(local853_5), local853_3);
      mc.player
         .setYaw(mc.player.headYaw = mc.player.headYaw + MouseSensitivity.quantizeSensitivity(MathHelper.clamp(local853_4, -local853_6, local853_6)));
      mc.player
         .setPitch(
            MathHelper.clamp(mc.player.getPitch() + MouseSensitivity.quantizeSensitivity(MathHelper.clamp(local853_5, -local853_7, local853_7)), -90.0F, 90.0F)
         );
      EventBus.getInstance().post(new RotationUpdateEvent());
      return Math.abs(local853_4) <= local853_2 && Math.abs(local853_5) <= local853_3;
   }

   public void setRotationWithMode(RotationAngles newRotationAngles, RotationSpeed newRotationSpeed, long local855_3, RotationManager.ReturnMode newReturnMode) {
      RotationAngles currentRotationAngles = EntityUtils.adjustRotationForWall(newRotationAngles);
      if (currentRotationAngles != newRotationAngles) {
         newRotationAngles = currentRotationAngles;
         newRotationSpeed = new RotationSpeed(
            Math.max(newRotationSpeed.getYawSpeed(), 15.0F), Math.max(newRotationSpeed.getPitchSpeed(), 15.0F), newRotationSpeed.getMaxYawSpeed(), newRotationSpeed.getMaxPitchSpeed()
         );
      }

      this.returnMode = newReturnMode;
      this.rotationAngles = newRotationAngles;
      this.rotationSpeed = newRotationSpeed;
      this.lastRotationTick = this.currentTick;
      this.rotationTimeoutMillis = local855_3;
      this.rotationEndMillis = -1L;
      this.stopwatch.reset();
   }

   public float getTargetYaw() {
      return this.clientYaw;
   }

   public boolean isRotating() {
      return this.isRotatingActive;
   }

   public enum ReturnMode {
      Empty,
      Smooth,
      SmoothFast,

      Keep;
   }
}
