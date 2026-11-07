package greed.client.cosmetic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BedrockModel {
   public List<BedrockBone> list = new ArrayList<>();
   public int intVal = 64;
   public int intVal2 = 64;

   private BedrockBone getBedrockBone(String local1816_1, BedrockBone bedrockBone) {
      if (bedrockBone.strVal.equals(local1816_1)) {
         return bedrockBone;
      }

      for (BedrockBone bedrockBone2 : bedrockBone.list) {
         BedrockBone bedrockBone3 = this.getBedrockBone(local1816_1, bedrockBone2);
         if (bedrockBone3 != null) {
            return bedrockBone3;
         }
      }

      return null;
   }

   public Optional<BedrockBone> getOptional(String local1817_1) {
      for (BedrockBone bedrockBone : this.list) {
         BedrockBone bedrockBone2 = this.getBedrockBone(local1817_1, bedrockBone);
         if (bedrockBone2 != null) {
            return Optional.of(bedrockBone2);
         }
      }

      return Optional.empty();
   }
}
