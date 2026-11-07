package greed.client.util;

import java.util.ArrayList;
import java.util.Arrays;

public class ClientBrand {
   private String strVal;
   private String strVal2;
   private ArrayList<String> list;

   public String formatText() {
      return this.strVal;
   }

   public String formatText2() {
      return this.strVal2;
   }

   public String formatText3(String local1106_1) {
      return this.strVal2.formatted(local1106_1);
   }

   public ClientBrand(String local1107_1, String local1107_2, String... stringArray) {
      this.strVal = local1107_1;
      this.strVal2 = local1107_2;
      this.list = new ArrayList<>(Arrays.asList(stringArray));
   }

   public ArrayList<String> getArrayList() {
      return this.list;
   }
}
