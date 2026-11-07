package greed.client.cosmetic;

import greed.client.cosmetic.CosmeticAnimationLoader;
import greed.client.cosmetic.CosmeticCatalog;
import greed.client.cosmetic.CosmeticCategory;
import greed.client.cosmetic.CosmeticEntry;
import greed.client.cosmetic.CosmeticModel;
import greed.client.cosmetic.CosmeticPreviewAtlas;
import greed.client.cosmetic.CosmeticStore;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;
import net.minecraft.util.Identifier;

public final class CosmeticWarmup {
   private static final Set<String> set = ConcurrentHashMap.newKeySet();
   private static final Map<String, Long> map = new ConcurrentHashMap<>();
   private static final ExecutorService executorService = Executors.newSingleThreadExecutor(runnable -> {
      Thread thread = new Thread(runnable, "greed-cosmetic-warmup");
      thread.setDaemon(true);
      thread.setPriority(1);
      return thread;
   });

   public static void updateState() {
      for (CosmeticCategory cosmeticCategory : CosmeticCategory.values()) {
         processData(CosmeticStore.getCosmeticEntry(cosmeticCategory));
      }
   }

   public static void processData(CosmeticEntry cosmeticEntry) {
      if (cosmeticEntry != null) {
         processData3(
            cosmeticEntry.modelResource(),
            () -> cosmeticEntry.placement() != null ? CosmeticModel.matchesCriteria(cosmeticEntry) : CosmeticAnimationLoader.matchesCriteria2(cosmeticEntry)
         );
      }
   }

   public static void updateState2() {
      processData3("previews/atlas/index.json", CosmeticPreviewAtlas::checkState);
   }

   private CosmeticWarmup() {
   }

   public static void processData2(Identifier identifier, String local4627_1) {
      processData3(local4627_1, () -> CosmeticPreviewAtlas.matchesCriteria(identifier, local4627_1));
   }

   private static void processData3(String local4629_0, BooleanSupplier booleanSupplier) {
      Long local4629_2 = map.get(local4629_0);
      if (local4629_2 != null && System.currentTimeMillis() - local4629_2 >= 30000L && map.remove(local4629_0, local4629_2)) {
         set.remove(local4629_0);
      }

      if (set.add(local4629_0)) {
         executorService.execute(() -> {
            if (!booleanSupplier.getAsBoolean()) {
               map.put(local4629_0, System.currentTimeMillis());
            }
         });
      }
   }

   public static void updateState3() {
      processData3("catalog.json", CosmeticCatalog::checkState);
   }
}
