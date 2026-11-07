package greed.client.cosmetic;

import greed.client.cosmetic.CosmeticWarmup;
import greed.client.cosmetic.CosmeticAccess;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import net.fabricmc.loader.api.FabricLoader;

public final class CosmeticStore {
   private static final Gson gson2 = new GsonBuilder().setPrettyPrinting().create();
   private static final Path path = FabricLoader.getInstance().getConfigDir().resolve("greed").resolve("cosmetics.json");
   private static final Map<CosmeticCategory, Integer> map = new EnumMap<>(CosmeticCategory.class);

   public static boolean matchesCriteria(CosmeticEntry cosmeticEntry) {
      return cosmeticEntry != null && computeInt(cosmeticEntry.category()) == cosmeticEntry.id();
   }

   private CosmeticStore() {
   }

   public static void processData(CosmeticEntry cosmeticEntry) {
      if (cosmeticEntry != null && CosmeticAccess.matchesCriteria(cosmeticEntry)) {
         if (matchesCriteria(cosmeticEntry)) {
            map.remove(cosmeticEntry.category());
         } else {
            map.put(cosmeticEntry.category(), cosmeticEntry.id());
            CosmeticWarmup.processData(cosmeticEntry);
         }

         updateState2();
      }
   }

   public static int computeInt(CosmeticCategory cosmeticCategory) {
      return map.getOrDefault(cosmeticCategory, -1);
   }

   private static void updateState() {
      map.clear();
      if (Files.isRegularFile(path)) {
         try {
            JsonObject asJsonObject = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();

            for (CosmeticCategory cosmeticCategory : CosmeticCategory.values()) {
               String local5749_5 = cosmeticCategory.name().toLowerCase();
               if (asJsonObject.has(local5749_5) && asJsonObject.get(local5749_5).isJsonPrimitive()) {
                  CosmeticEntry cosmeticEntry = CosmeticCatalog.getCosmeticEntry(asJsonObject.get(local5749_5).getAsInt());
                  if (cosmeticEntry == null || cosmeticEntry.category() == cosmeticCategory) {
                     map.put(cosmeticCategory, asJsonObject.get(local5749_5).getAsInt());
                  }
               }
            }
         } catch (Exception exception) {
         }
      }
   }

   private static void updateState2() {
      try {
         Files.createDirectories(path.getParent());
         JsonObject jsonObject = new JsonObject();

         for (Entry<CosmeticCategory, Integer> local5750_2 : map.entrySet()) {
            jsonObject.addProperty(local5750_2.getKey().name().toLowerCase(), local5750_2.getValue());
         }

         Files.writeString(path, gson2.toJson(jsonObject), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
      } catch (Exception exception) {
      }
   }

   public static void processData2(CosmeticCategory cosmeticCategory) {
      map.remove(cosmeticCategory);
      updateState2();
   }

   static {
      updateState();
   }

   public static int computeInt2() {
      int local5753_0 = 0;

      for (CosmeticCategory cosmeticCategory : CosmeticCategory.values()) {
         if (getCosmeticEntry(cosmeticCategory) != null) {
            local5753_0++;
         }
      }

      return local5753_0;
   }

   public static CosmeticEntry getCosmeticEntry(CosmeticCategory cosmeticCategory) {
      if (greed.client.module.misc.UnHookModule.isUnhooked()) {
         return null;
      }

      CosmeticEntry cosmeticEntry = CosmeticCatalog.getCosmeticEntry(computeInt(cosmeticCategory));
      return cosmeticEntry != null && cosmeticEntry.category() == cosmeticCategory && CosmeticAccess.matchesCriteria(cosmeticEntry) ? cosmeticEntry : null;
   }

   public static void updateState3() {
      map.clear();
      updateState2();
   }
}
