package greed.client;


public final class RotationSpeed {
   private final float minYawSpeed;
   private final float minPitchSpeed;
   private final float maxYawSpeed;
   private final float maxPitchSpeed;

   public float getMaxYawSpeed() {
      return this.maxYawSpeed;
   }

   public float getYawSpeed() {
      return this.minYawSpeed;
   }

   public RotationSpeed(float local3565_1, float local3565_2, float local3565_3, float local3565_4) {
      this.minYawSpeed = local3565_1;
      this.minPitchSpeed = local3565_2;
      this.maxYawSpeed = local3565_3;
      this.maxPitchSpeed = local3565_4;
   }

   public float getMaxPitchSpeed() {
      return this.maxPitchSpeed;
   }

   public float getPitchSpeed() {
      return this.minPitchSpeed;
   }

   public RotationSpeed(float local3568_1, float local3568_2) {
      this(local3568_1, local3568_2, local3568_1, local3568_2);
   }
}
