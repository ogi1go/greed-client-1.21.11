package greed.client.cosmetic;

import greed.client.cosmetic.CosmeticEntry;

public final class CosmeticAccess {
   public static boolean matchesCriteria(CosmeticEntry cosmeticEntry) {
      return cosmeticEntry != null;
   }

   private CosmeticAccess() {
   }

   public static boolean checkState() {
      return true;
   }
}
