package greed.client.storage;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

public final class FriendStore {
   private static final Map<String, String> friends = new LinkedHashMap<>();
   private static boolean loaded;
   private static Path file;

   private FriendStore() {
   }

   public static boolean isFriend(String local5594_0) {
      ensureLoaded();
      return local5594_0 != null && friends.containsKey(local5594_0.toLowerCase(Locale.ROOT));
   }

   public static ArrayList<String> getFriendList() {
      ensureLoaded();
      return new ArrayList<>(friends.values());
   }

   private static void saveFriends() {
      ensureLoaded();

      try {
         Files.createDirectories(file.getParent());
         JsonArray jsonArray = new JsonArray();

         for (String local5596_2 : friends.values()) {
            jsonArray.add(local5596_2);
         }

         Files.writeString(file, jsonArray.toString(), StandardCharsets.UTF_8);
      } catch (Exception exception) {
      }
   }

   public static boolean addFriend(String local5598_0) {
      ensureLoaded();
      if (local5598_0 != null && !local5598_0.trim().isEmpty() && !friends.containsKey(local5598_0.toLowerCase(Locale.ROOT))) {
         friends.put(local5598_0.toLowerCase(Locale.ROOT), local5598_0);
         saveFriends();
         return true;
      } else {
         return false;
      }
   }

   public static void clearFriends() {
      ensureLoaded();
      friends.clear();
      saveFriends();
   }

   public static boolean removeFriend(String local5600_0) {
      ensureLoaded();
      if (local5600_0 != null && friends.remove(local5600_0.toLowerCase(Locale.ROOT)) != null) {
         saveFriends();
         return true;
      } else {
         return false;
      }
   }

   private static void ensureLoaded() {
      if (!loaded) {
         loaded = true;
         file = FabricLoader.getInstance().getConfigDir().resolve("greed").resolve("friends.json");

         try {
            if (!Files.exists(file)) {
               return;
            }

            for (JsonElement jsonElement : JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonArray()) {
               friends.put(jsonElement.getAsString().toLowerCase(Locale.ROOT), jsonElement.getAsString());
            }
         } catch (Exception exception) {
         }
      }
   }
}
