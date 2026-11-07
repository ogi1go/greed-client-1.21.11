package greed.client.storage;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

public final class StaffStore {
   private static final Map<String, String> map = new LinkedHashMap<>();
   private static final Map<String, String> map2 = new LinkedHashMap<>();
   private static boolean flag;
   private static Path path;

   public static boolean matchesCriteria(String local7287_0) {
      updateState3();
      if (local7287_0 != null && !local7287_0.trim().isEmpty() && !map.containsKey(local7287_0.toLowerCase(Locale.ROOT))) {
         map.put(local7287_0.toLowerCase(Locale.ROOT), local7287_0);
         updateState();
         return true;
      } else {
         return false;
      }
   }

   private static void updateState() {
      updateState3();

      try {
         Files.createDirectories(path.getParent());
         JsonObject jsonObject = new JsonObject();
         JsonArray jsonArray = new JsonArray();

         for (String local7288_3 : map.values()) {
            jsonArray.add(local7288_3);
         }

         JsonArray jsonArray2 = new JsonArray();

         for (String local7288_4 : map2.values()) {
            jsonArray2.add(local7288_4);
         }

         jsonObject.add("names", jsonArray);
         jsonObject.add("prefixes", jsonArray2);
         Files.writeString(path, jsonObject.toString(), StandardCharsets.UTF_8);
      } catch (Exception exception) {
      }
   }

   public static boolean matchesCriteria2(String local7289_0) {
      updateState3();
      if (local7289_0 != null && !local7289_0.isEmpty()) {
         for (String local7289_2 : map2.keySet()) {
            if (local7289_0.toLowerCase(Locale.ROOT).contains(local7289_2)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static ArrayList<String> getArrayList() {
      updateState3();
      return new ArrayList<>(map2.values());
   }

   public static ArrayList<String> getArrayList2() {
      updateState3();
      return new ArrayList<>(map.values());
   }

   public static boolean matchesCriteria3(String local7292_0) {
      updateState3();
      return local7292_0 != null && map.containsKey(local7292_0.toLowerCase(Locale.ROOT));
   }

   public static void updateState2() {
      updateState3();
      map.clear();
      map2.clear();
      updateState();
   }

   private static void updateState3() {
      if (!flag) {
         flag = true;
         path = FabricLoader.getInstance().getConfigDir().resolve("greed").resolve("staffs.json");

         try {
            if (!Files.exists(path)) {
               return;
            }

            JsonObject asJsonObject = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();

            for (JsonElement jsonElement : asJsonObject.getAsJsonArray("names")) {
               map.put(jsonElement.getAsString().toLowerCase(Locale.ROOT), jsonElement.getAsString());
            }

            for (JsonElement jsonElement2 : asJsonObject.getAsJsonArray("prefixes")) {
               map2.put(jsonElement2.getAsString().toLowerCase(Locale.ROOT), jsonElement2.getAsString());
            }
         } catch (Exception exception) {
         }
      }
   }

   public static boolean matchesCriteria4(String local7295_0) {
      updateState3();
      if (local7295_0 != null
         && (map.remove(local7295_0.toLowerCase(Locale.ROOT)) != null || map2.remove(local7295_0.toLowerCase(Locale.ROOT)) != null)) {
         updateState();
         return true;
      } else {
         return false;
      }
   }

   private StaffStore() {
   }

   public static boolean matchesCriteria5(String local7298_0) {
      updateState3();
      if (local7298_0 != null && !local7298_0.trim().isEmpty() && !map2.containsKey(local7298_0.toLowerCase(Locale.ROOT))) {
         map2.put(local7298_0.toLowerCase(Locale.ROOT), local7298_0);
         updateState();
         return true;
      } else {
         return false;
      }
   }
}
