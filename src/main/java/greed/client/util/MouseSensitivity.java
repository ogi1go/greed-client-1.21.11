package greed.client.util;

import greed.client.MinecraftAccess;

public final class MouseSensitivity implements MinecraftAccess {
   public static float getSensitivityDelta() {
      return (float)(getSensitivityMultiplier() * 0.15);
   }

   public static float quantizePitch(float local7424_0) {
      return local7424_0 / getPitchSensitivityDelta() * getPitchSensitivityDelta();
   }

   public static float quantizeSensitivity(float local7425_0) {
      return roundToSteps(local7425_0) * getSensitivityDelta();
   }

   public float getMouseSensitivityFactor() {
      return (float)(Math.pow(mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2, 3.0) * 8.0 * 0.15);
   }

   public static float quantizeSensitivityWithMin(float local7427_0) {
      return roundToSteps(local7427_0) == 0.0F && Math.abs(local7427_0) >= getSensitivityDelta() * 0.2F
         ? Math.signum(local7427_0) * getSensitivityDelta()
         : quantizeSensitivity(local7427_0);
   }

   public static float roundSensitivityPitch(float local7428_0) {
      return Math.round(local7428_0 / getPitchSensitivityDelta()) * getPitchSensitivityDelta();
   }

   public static float roundToSteps(float local7429_0) {
      return Math.round(local7429_0 / getSensitivityDelta());
   }

   public static float getRawMouseFactor() {
      return (float)(Math.pow(mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2, 3.0) * 0.78);
   }

   public static float getPitchSensitivityDelta() {
      return getRawMouseFactor() * 0.15F;
   }

   public float getYawSensitivityDelta(float local7433_1) {
      return Math.round(local7433_1 / this.getMouseSensitivityFactor()) * this.getMouseSensitivityFactor();
   }

   public static float getSensitivityMultiplier() {
      double value = mc.options.getMouseSensitivity().getValue();
      return (float)(Math.pow(value * 0.6F + 0.2F, 3.0) * 8.0);
   }
}
