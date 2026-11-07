package greed.client.cosmetic;

public class BedrockVertex {
   public ModelVec3 modelVec3;
   public float floatVal;
   public float floatVal2;

   public BedrockVertex(ModelVec3 initialModelVec3, float local1757_2, float local1757_3) {
      this.modelVec3 = initialModelVec3;
      this.floatVal = local1757_2;
      this.floatVal2 = local1757_3;
   }

   public BedrockVertex(float local1758_1, float local1758_2, float local1758_3, float local1758_4, float local1758_5) {
      this.modelVec3 = new ModelVec3(local1758_1, local1758_2, local1758_3);
      this.floatVal = local1758_4;
      this.floatVal2 = local1758_5;
   }
}
