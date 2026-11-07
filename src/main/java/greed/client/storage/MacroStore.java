package greed.client.storage;

import greed.client.event.EventListener;
import greed.client.util.InputBinding;
import greed.client.MinecraftAccess;
import greed.client.event.PlayerTickEvent;
import greed.client.storage.ConfigStore;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import net.fabricmc.loader.api.FabricLoader;

public final class MacroStore implements MinecraftAccess {
   private static MacroStore macroStore;
   private final LinkedHashMap<String, MacroStore.DataRecord> map = new LinkedHashMap<>();
   private final HashSet<String> set = new HashSet<>();
   private Path path;
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (mc.player != null && !this.map.isEmpty()) {
         for (MacroStore.DataRecord local7038_3 : this.map.values()) {
            if (!local7038_3.key().isPressed(mc.getWindow().getHandle())) {
               this.set.remove(local7038_3.name());
            } else if (this.set.add(local7038_3.name())) {
               if (local7038_3.chatLine().startsWith("/")) {
                  mc.player.networkHandler.sendChatCommand(local7038_3.chatLine().substring(1));
               } else {
                  mc.player.networkHandler.sendChatMessage(local7038_3.chatLine());
               }
            }
         }
      }
   };

   public InputBinding getInputBinding(String local7035_1) {
      if (local7035_1.length() == 2 && (local7035_1.charAt(0) == 'M' || local7035_1.charAt(0) == 'm') && Character.isDigit(local7035_1.charAt(1))) {
         return InputBinding.mouseButton(Character.getNumericValue(local7035_1.charAt(1)) - 1);
      }

      for (int code = 32; code <= 348; code++) {
         if (InputBinding.keyboardKey(code).getDisplayName().equalsIgnoreCase(local7035_1)) {
            return InputBinding.keyboardKey(code);
         }
      }

      return InputBinding.UNBOUND;
   }

   private void updateState() {
      try {
         Files.createDirectories(this.path.getParent());
         JsonArray jsonArray = new JsonArray();

         for (MacroStore.DataRecord local7036_3 : this.map.values()) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("name", local7036_3.name());
            jsonObject.addProperty("key", local7036_3.key().getDevice().name() + ":" + local7036_3.key().getCode());
            jsonObject.addProperty("text", local7036_3.chatLine());
            jsonArray.add(jsonObject);
         }

         Files.writeString(this.path, jsonArray.toString(), StandardCharsets.UTF_8);
      } catch (Exception exception) {
      }
   }

   public void processData(String local7037_1, InputBinding inputBinding, String local7037_3) {
      this.map.put(local7037_1.toLowerCase(Locale.ROOT), new MacroStore.DataRecord(local7037_1, inputBinding, local7037_3));
      this.updateState();
   }

   private MacroStore() {
      this.path = FabricLoader.getInstance().getConfigDir().resolve("greed").resolve("macros.json");

      try {
         if (!Files.exists(this.path)) {
            return;
         }

         for (JsonElement jsonElement : JsonParser.parseString(Files.readString(this.path, StandardCharsets.UTF_8)).getAsJsonArray()) {
            this.map
               .put(
                  jsonElement.getAsJsonObject().get("name").getAsString().toLowerCase(Locale.ROOT),
                  new MacroStore.DataRecord(
                     jsonElement.getAsJsonObject().get("name").getAsString(),
                     ConfigStore.parseBinding(jsonElement.getAsJsonObject().get("key").getAsString()),
                     jsonElement.getAsJsonObject().get("text").getAsString()
                  )
               );
         }
      } catch (Exception exception) {
      }
   }

   public boolean matchesCriteria(String local7040_1) {
      if (this.map.remove(local7040_1.toLowerCase(Locale.ROOT)) == null) {
         return false;
      }

      this.updateState();
      return true;
   }

   public static MacroStore getInstance() {
      if (macroStore == null) {
         macroStore = new MacroStore();
      }

      return macroStore;
   }

   public ArrayList<MacroStore.DataRecord> getArrayList() {
      return new ArrayList<>(this.map.values());
   }

   public void updateState2() {
      this.map.clear();
      this.set.clear();
      this.updateState();
   }

   public record DataRecord(String name, InputBinding key, String chatLine) {
   }
}
