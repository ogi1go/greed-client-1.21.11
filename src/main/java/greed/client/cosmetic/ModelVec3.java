package greed.client.cosmetic;

public class ModelVec3 {
   public static final ModelVec3 modelVec3 = new ModelVec3(0.0F, 0.0F, 0.0F);
   public float floatVal;
   public float floatVal2;
   public float floatVal3;

   public void processData(float local7372_1, float local7372_2, float local7372_3) {
      this.floatVal = local7372_1;
      this.floatVal2 = local7372_2;
      this.floatVal3 = local7372_3;
   }

   public float computeFloat() {
      return this.floatVal;
   }

   public void processData2(float local7374_1) {
      this.floatVal3 = local7374_1;
   }

   public ModelVec3(float local7375_1, float local7375_2, float local7375_3) {
      this.floatVal = local7375_1;
      this.floatVal2 = local7375_2;
      this.floatVal3 = local7375_3;
   }

   public void processData3(float local7377_1) {
      this.floatVal = local7377_1;
   }

   public void processData4(float local7378_1) {
      this.floatVal2 = local7378_1;
   }

   public float computeFloat2() {
      return this.floatVal3;
   }

   @Override
   public String toString() {
      return this.floatVal + "," + this.floatVal2 + "," + this.floatVal3;
   }

   public ModelVec3 getModelVec3(ModelVec3 newModelVec3) {
      return new ModelVec3(
         this.floatVal2 * newModelVec3.floatVal3 - this.floatVal3 * newModelVec3.floatVal2,
         this.floatVal3 * newModelVec3.floatVal - this.floatVal * newModelVec3.floatVal3,
         this.floatVal * newModelVec3.floatVal2 - this.floatVal2 * newModelVec3.floatVal
      );
   }

   public float computeFloat3() {
      return this.floatVal2;
   }
}
