package greed.client.cosmetic;

import greed.client.GreedClient;
import greed.client.cosmetic.CosmeticWarmup;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.MinecraftClient;

public final class CosmeticCatalog {
   private static final List<CosmeticEntry> list = new ArrayList<>();
   private static final Map<Integer, CosmeticEntry> map = new HashMap<>();
   private static final Map<CosmeticCategory, List<CosmeticEntry>> map2 = new EnumMap<>(CosmeticCategory.class);
   private static int intVal;

   private CosmeticCatalog() {
   }

   public static boolean checkState() {
      try {
         ArrayList<CosmeticEntry> local662_0 = new ArrayList<>();

         for (JsonElement jsonElement : JsonParser.parseString(new String(CosmeticPreviewAtlas.getByte("catalog.json"), StandardCharsets.UTF_8))
            .getAsJsonArray()) {
            JsonObject asJsonObject = jsonElement.getAsJsonObject();
            JsonArray asJsonArray = asJsonObject.getAsJsonArray("transform");
            JsonArray jsonArray = asJsonObject.has("align") ? asJsonObject.getAsJsonArray("align") : null;
            CosmeticPlacement cosmeticPlacement = new CosmeticPlacement(
               asJsonObject.get("kind").getAsString(),
               asJsonObject.get("anchor").getAsString(),
               asJsonArray.get(0).getAsFloat(),
               asJsonArray.get(1).getAsFloat(),
               asJsonArray.get(2).getAsFloat(),
               asJsonArray.get(3).getAsFloat(),
               asJsonArray.get(4).getAsFloat(),
               asJsonArray.get(5).getAsFloat(),
               asJsonArray.get(6).getAsFloat(),
               jsonArray == null ? 0.0F : jsonArray.get(0).getAsFloat(),
               jsonArray == null ? 0.0F : jsonArray.get(1).getAsFloat(),
               jsonArray == null ? 1.0F : jsonArray.get(2).getAsFloat(),
               asJsonObject.has("flying") && asJsonObject.get("flying").getAsBoolean(),
               new ArrayList<>()
            );
            if (asJsonObject.has("targets")) {
               for (JsonElement jsonElement2 : asJsonObject.getAsJsonArray("targets")) {
                  cosmeticPlacement.targets().add(jsonElement2.getAsString());
               }
            }

            local662_0.add(
               new CosmeticEntry(
                  asJsonObject.get("id").getAsInt(),
                  CosmeticCategory.valueOf(asJsonObject.get("category").getAsString().toUpperCase(Locale.ROOT)),
                  asJsonObject.get("name").getAsString(),
                  asJsonObject.has("beta") && asJsonObject.get("beta").getAsBoolean(),
                  asJsonObject.get("model").getAsString(),
                  null,
                  asJsonObject.get("preview").getAsString(),
                  cosmeticPlacement
               )
            );
         }

         MinecraftClient.getInstance().execute(() -> {
            for (CosmeticEntry cosmeticEntry : local662_0) {
               if (!map.containsKey(cosmeticEntry.id())) {
                  list.add(cosmeticEntry);
                  map.put(cosmeticEntry.id(), cosmeticEntry);
                  map2.computeIfAbsent(cosmeticEntry.category(), cosmeticCategory -> new ArrayList<>()).add(cosmeticEntry);
               }
            }

            intVal++;
            CosmeticWarmup.updateState();
         });
         return true;
      } catch (Exception exception) {
         GreedClient.logger().warn("[Cosmetics] catalog.json not loaded: {}", exception.toString());
         return false;
      }
   }

   public static CosmeticEntry getCosmeticEntry(int local664_0) {
      return map.get(local664_0);
   }

   private static void processData(int local665_0, CosmeticCategory cosmeticCategory, String local665_2, boolean local665_3) {
      String local665_4 = cosmeticCategory.formatText3() + "/" + local665_0 + "/";
      CosmeticEntry cosmeticEntry = new CosmeticEntry(
         local665_0, cosmeticCategory, local665_2, local665_3, local665_4 + "model.json", local665_4 + "texture.png", local665_4 + "preview.png", null
      );
      list.add(cosmeticEntry);
      map.put(local665_0, cosmeticEntry);
      map2.computeIfAbsent(cosmeticCategory, cosmeticCategory2 -> new ArrayList<>()).add(cosmeticEntry);
   }

   static {
      processData(36, CosmeticCategory.BODYWEAR, "Louis Vuitton", true);
      processData(37, CosmeticCategory.BODYWEAR, "Gucci", false);
      processData(38, CosmeticCategory.BODYWEAR, "Adidas", false);
      processData(39, CosmeticCategory.BODYWEAR, "Nike", false);
      processData(40, CosmeticCategory.BODYWEAR, "Supreme", false);
      processData(41, CosmeticCategory.BODYWEAR, "Vlone", false);
      processData(42, CosmeticCategory.BODYWEAR, "Bape", false);
      processData(43, CosmeticCategory.BODYWEAR, "Balenciaga", true);
      processData(44, CosmeticCategory.BODYWEAR, "Chrome Hearts", false);
      processData(95, CosmeticCategory.BODYWEAR, "PePe", false);
      processData(137, CosmeticCategory.BODYWEAR, "Катана - ila_studio", true);
      processData(25, CosmeticCategory.WINGS, "Aurum", false);
      processData(61, CosmeticCategory.WINGS, "Angel", false);
      processData(62, CosmeticCategory.WINGS, "Dragon", false);
      processData(63, CosmeticCategory.WINGS, "Diamond", false);
      processData(64, CosmeticCategory.WINGS, "Spider", false);
      processData(65, CosmeticCategory.WINGS, "Easter", true);
      processData(82, CosmeticCategory.WINGS, "Fluger", true);
      processData(86, CosmeticCategory.WINGS, "Трезубец - Броя", true);
      processData(91, CosmeticCategory.WINGS, "Аквыч", false);
      processData(99, CosmeticCategory.WINGS, "Dark", false);
      processData(123, CosmeticCategory.WINGS, "Pulse", false);
      processData(124, CosmeticCategory.WINGS, "Ghoul", false);
      processData(136, CosmeticCategory.WINGS, "Ягуар", false);
      processData(48, CosmeticCategory.HAT, "Armour", false);
      processData(49, CosmeticCategory.HAT, "Capybara", false);
      processData(50, CosmeticCategory.HAT, "Chicken", false);
      processData(53, CosmeticCategory.HAT, "Pepe", false);
      processData(57, CosmeticCategory.HAT, "Sheep", false);
      processData(80, CosmeticCategory.HAT, "Fluger", true);
      processData(88, CosmeticCategory.HAT, "Аквыч", false);
      processData(92, CosmeticCategory.HAT, "Bear", false);
      processData(96, CosmeticCategory.HAT, "ПеПе", false);
      processData(97, CosmeticCategory.HAT, "Happy", false);
      processData(98, CosmeticCategory.HAT, "Нимб", true);
      processData(45, CosmeticCategory.PET, "Angel", false);
      processData(46, CosmeticCategory.PET, "Demon", false);
      processData(47, CosmeticCategory.PET, "Buddy", false);
      processData(81, CosmeticCategory.PET, "Fluger Door", true);
      processData(84, CosmeticCategory.PET, "Diamond", true);
      processData(90, CosmeticCategory.PET, "Аквыч", true);
      processData(93, CosmeticCategory.PET, "Bee", false);
      processData(126, CosmeticCategory.PET, "1 Год", false);
      processData(132, CosmeticCategory.PET, "Ягуар", false);
      processData(133, CosmeticCategory.PET, "Cergifff", false);
      processData(134, CosmeticCategory.PET, "Ila_Studio", false);
      processData(135, CosmeticCategory.PET, "Creeper", false);

      for (CosmeticCategory cosmeticCategory : CosmeticCategory.values()) {
         map2.computeIfAbsent(cosmeticCategory, cosmeticCategory2 -> new ArrayList<>());
      }
   }

   public static int computeInt() {
      return intVal;
   }

   public static List<CosmeticEntry> getList(CosmeticCategory cosmeticCategory) {
      return Collections.unmodifiableList(map2.getOrDefault(cosmeticCategory, List.of()));
   }

   public static List<CosmeticEntry> getList2() {
      return Collections.unmodifiableList(list);
   }
}
