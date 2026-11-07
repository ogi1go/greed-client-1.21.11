package greed.client.cosmetic;

public class BedrockQuad {
   public BedrockVertex[] recoveredClass0057Array;
   public ModelVec3 modelVec3;

   public BedrockQuad(BedrockVertex[] initialRecoveredClass0057Array, float local6683_2, float local6683_3, float local6683_4) {
      this.recoveredClass0057Array = initialRecoveredClass0057Array;
      this.modelVec3 = new ModelVec3(local6683_2, local6683_3, local6683_4);
   }

   public BedrockQuad(BedrockVertex[] initialRecoveredClass0057Array, ModelVec3 initialModelVec3) {
      this.recoveredClass0057Array = initialRecoveredClass0057Array;
      this.modelVec3 = initialModelVec3;
   }
}
