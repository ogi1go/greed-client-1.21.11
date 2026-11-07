package greed.client.storage;

import greed.client.setting.ColorSetting;
import greed.client.ui.hud.HudManager;

import greed.client.module.misc.AutoBuyModule;
import greed.client.ui.AutoBuySettingsScreen;
import greed.client.module.misc.AutoDeliverModule;
import greed.client.module.misc.AutoRegisterModule;
import greed.client.module.misc.UnHookModule;
import greed.client.setting.BooleanSetting;
import greed.client.util.InputBinding;
import greed.client.setting.KeybindSetting;
import greed.client.module.Module;
import greed.client.module.ModuleManager;
import greed.client.setting.MultiChoiceSetting;
import greed.client.setting.NumberSetting;
import greed.client.setting.Setting;
import greed.client.setting.SingleChoiceSetting;
import greed.client.setting.TextSetting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.fabricmc.loader.api.FabricLoader;

public final class ConfigStore {
   private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yy");
   private static volatile int revision;
   private static volatile String activeConfig = "default";
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final Map<String, String> lastSavedConfigs = new ConcurrentHashMap<>();
   private static final Map<Path, String> pendingWrites = new ConcurrentHashMap<>();
   private static final ExecutorService writer = Executors.newSingleThreadExecutor(runnable -> {
      Thread thread = new Thread(runnable, "greed-config-writer");
      thread.setDaemon(true);
      return thread;
   });

   public static boolean load(String local4345_0) {
      if (!exists(local4345_0)) {
         return false;
      }

      try {
         JsonObject asJsonObject = JsonParser.parseString(Files.readString(getConfigPath(local4345_0), StandardCharsets.UTF_8)).getAsJsonObject();

         for (String local4345_3 : asJsonObject.keySet()) {
            Module module = ModuleManager.findByName(local4345_3);
            if (module != null && module != AutoDeliverModule.getInstance() && asJsonObject.get(local4345_3).isJsonObject()) {
               JsonObject asJsonObject2 = asJsonObject.getAsJsonObject(local4345_3);
               if (asJsonObject2.has("bind")) {
                  module.withKeyCode(asJsonObject2.get("bind").getAsInt());
               }

               if (asJsonObject2.has("settings") && asJsonObject2.get("settings").isJsonObject()) {
                  HashMap<String, Integer> local4345_6 = new HashMap<>();

                  for (Setting<?> local4345_8 : module.getSettings()) {
                     String local4345_9 = getUniqueSettingKey(local4345_6, local4345_8);
                     if (local4345_8 != AutoRegisterModule.getInstance().password) {
                        deserializeSetting(asJsonObject2.getAsJsonObject("settings"), local4345_9, local4345_8);
                     }
                  }
               }

               if (asJsonObject2.has("enabled") && asJsonObject2.get("enabled").getAsBoolean() != module.isEnabled()) {
                  module.setEnabled(asJsonObject2.get("enabled").getAsBoolean());
               }
            }
         }

         if (asJsonObject.has("autobuy") && asJsonObject.get("autobuy").isJsonObject()) {
            deserializeAutoBuy(asJsonObject.getAsJsonObject("autobuy"));
         }

         if (asJsonObject.has("draggables") && asJsonObject.get("draggables").isJsonObject()) {
            HudManager.processData10(asJsonObject.getAsJsonObject("draggables"));
         }

         selectActiveConfig(local4345_0);
         return true;
      } catch (Exception exception) {
         return false;
      }
   }

   public static int getRevision() {
      return revision;
   }

   public static ArrayList<String> listConfigs() {
      ArrayList<String> local4347_0 = new ArrayList<>();
      if (!Files.isDirectory(getConfigDirectory())) {
         return local4347_0;
      }

      try (DirectoryStream<Path> local4347_1 = Files.newDirectoryStream(getConfigDirectory(), "*.json")) {
         for (Path path : local4347_1) {
            local4347_0.add(path.getFileName().toString().substring(0, path.getFileName().toString().length() - ".json".length()));
         }
      } catch (Exception exception) {
      }

      local4347_0.sort(String.CASE_INSENSITIVE_ORDER);
      return local4347_0;
   }

   private static JsonObject serializeAutoBuy() {
      JsonObject jsonObject = new JsonObject();
      jsonObject.addProperty("speed", AutoBuySettingsScreen.getInstance().numberSetting.floatValue());
      jsonObject.addProperty("buyPercent", AutoBuyModule.getInstance().buyPercent.floatValue());
      jsonObject.addProperty("sellPercent", AutoBuyModule.getInstance().sellPercent.floatValue());
      jsonObject.addProperty("buyMax", AutoBuySettingsScreen.getInstance().nested0374.getValue());
      jsonObject.addProperty("sellMin", AutoBuySettingsScreen.getInstance().nested03742.getValue());
      jsonObject.addProperty("durability", AutoBuySettingsScreen.getInstance().nested03743.getValue());
      jsonObject.addProperty("tgToken", AutoBuySettingsScreen.getInstance().textSetting2.getValue());
      jsonObject.addProperty("tgChat", AutoBuySettingsScreen.getInstance().textSetting3.getValue());
      JsonArray jsonArray = new JsonArray();

      for (String local4349_3 : AutoBuySettingsScreen.getInstance().map2.keySet()) {
         JsonObject jsonObject2 = new JsonObject();
         jsonObject2.addProperty("name", local4349_3);
         jsonObject2.addProperty("buy", AutoBuySettingsScreen.getInstance().map2.get(local4349_3).nested03744.getValue());
         jsonObject2.addProperty("sell", AutoBuySettingsScreen.getInstance().map2.get(local4349_3).nested03745.getValue());
         jsonArray.add(jsonObject2);
      }

      jsonObject.add("items", jsonArray);
      return jsonObject;
   }

   public static InputBinding parseBinding(String local4350_0) {
      if (!local4350_0.contains(":")) {
         return InputBinding.UNBOUND;
      } else if (local4350_0.startsWith("MOUSE:")) {
         return InputBinding.mouseButton(Integer.parseInt(local4350_0.substring("MOUSE:".length())));
      } else {
         return local4350_0.startsWith("KEYBOARD:")
            ? InputBinding.keyboardKey(Integer.parseInt(local4350_0.substring("KEYBOARD:".length())))
            : InputBinding.UNBOUND;
      }
   }

   private static void queueWrite(Path path, String local4351_1, Runnable runnable, Runnable runnable2) {
      pendingWrites.put(path, local4351_1);
      writer.execute(() -> {
         String local4371_3 = pendingWrites.remove(path);
         if (local4371_3 != null) {
            try {
               Files.createDirectories(path.getParent());
               Files.writeString(path, local4371_3, StandardCharsets.UTF_8);
               if (runnable != null) {
                  runnable.run();
               }
            } catch (Exception exception) {
               if (runnable2 != null) {
                  runnable2.run();
               }
            }
         }
      });
   }

   public static Path getConfigDirectory() {
      return FabricLoader.getInstance().getConfigDir().resolve("greed").resolve("configs");
   }

   public static String normalizeName(String local4353_0) {
      return sanitizeName(local4353_0);
   }

   public static Path getConfigPath(String local4354_0) {
      return getConfigDirectory().resolve(sanitizeName(local4354_0) + ".json");
   }

   private static String serializeModules() {
      JsonObject jsonObject = new JsonObject();

      for (Module module : ModuleManager.getModules()) {
         if (module != AutoDeliverModule.getInstance() && !(module instanceof UnHookModule)) {
            JsonObject jsonObject2 = new JsonObject();
            jsonObject2.addProperty("enabled", module.isEnabled());
            jsonObject2.addProperty("bind", module.getKeyCode());
            JsonObject jsonObject3 = new JsonObject();
            HashMap<String, Integer> local4355_5 = new HashMap<>();

            for (Setting<?> local4355_7 : module.getSettings()) {
               String local4355_8 = getUniqueSettingKey(local4355_5, local4355_7);
               if (local4355_7 != AutoRegisterModule.getInstance().password) {
                  serializeSetting(jsonObject3, local4355_8, local4355_7);
               }
            }

            jsonObject2.add("settings", jsonObject3);
            jsonObject.add(module.name, jsonObject2);
         }
      }

      jsonObject.add("autobuy", serializeAutoBuy());
      jsonObject.add("draggables", HudManager.getJsonObject());
      return GSON.toJson(jsonObject);
   }

   private static void writeLastConfigName(String local4356_0) {
      try {
         Files.writeString(getLastConfigPath(), local4356_0, StandardCharsets.UTF_8);
      } catch (Exception exception) {
      }
   }

   public static void writeAsync(Path path, String local4357_1) {
      queueWrite(path, local4357_1, null, null);
   }

   public static void saveActiveConfig() {
      save(activeConfig);
   }

   private ConfigStore() {
   }

   public static String getModifiedDate(String local4362_0) {
      try {
         return DATE_FORMAT.format(
            Instant.ofEpochMilli(Files.getLastModifiedTime(getConfigPath(local4362_0)).toMillis()).atZone(ZoneId.systemDefault()).toLocalDate()
         );
      } catch (Exception exception) {
         return "";
      }
   }

   private static void selectActiveConfig(String local4363_0) {
      activeConfig = sanitizeName(local4363_0);

      try {
         Files.writeString(getLastConfigPath(), activeConfig, StandardCharsets.UTF_8);
      } catch (Exception exception) {
      }
   }

   public static boolean exists(String local4364_0) {
      return !sanitizeName(local4364_0).isEmpty() && Files.exists(getConfigPath(local4364_0));
   }

   public static long getFileSize(String local4365_0) {
      try {
         return Files.size(getConfigPath(local4365_0));
      } catch (Exception exception) {
         return 0L;
      }
   }

   private static void serializeSetting(JsonObject jsonObject, String local4366_1, Setting<?> local4366_2) {
      if (local4366_2 instanceof BooleanSetting booleanSetting) {
         jsonObject.addProperty(local4366_1, booleanSetting.isChecked());
      } else if (local4366_2 instanceof ColorSetting colorSetting) {
         jsonObject.addProperty(local4366_1, colorSetting.getValue());
      } else if (local4366_2 instanceof NumberSetting currentNumberSetting) {
         jsonObject.addProperty(local4366_1, currentNumberSetting.floatValue());
      } else if (local4366_2 instanceof SingleChoiceSetting singleChoiceSetting) {
         jsonObject.addProperty(local4366_1, singleChoiceSetting.getSelectedOption());
      } else if (local4366_2 instanceof MultiChoiceSetting multiChoiceSetting) {
         JsonArray jsonArray = new JsonArray();

         for (String local4366_12 : multiChoiceSetting.getSelectedOptions()) {
            jsonArray.add(local4366_12);
         }

         jsonObject.add(local4366_1, jsonArray);
      } else if (local4366_2 instanceof KeybindSetting keybindSetting) {
         jsonObject.addProperty(local4366_1, keybindSetting.getBinding().getDevice().name() + ":" + keybindSetting.getBinding().getCode());
      } else if (local4366_2 instanceof TextSetting textSetting) {
         jsonObject.addProperty(local4366_1, textSetting.getValue());
      }
   }

   private static String sanitizeName(String local4367_0) {
      return local4367_0 == null ? "" : local4367_0.replaceAll("[^a-zA-Z0-9_ -]", "").trim();
   }

   public static Path getLastConfigPath() {
      return getConfigDirectory().resolve(".last");
   }

   private static void flushWrites() {
      try {
         writer.submit(() -> {}).get(5L, TimeUnit.SECONDS);
      } catch (Exception exception) {
      }
   }

   private static String getUniqueSettingKey(HashMap<String, Integer> local4370_0, Setting<?> local4370_1) {
      return local4370_0.merge(local4370_1.name, 1, Integer::sum) == 1
         ? local4370_1.name
         : local4370_1.name + "#" + local4370_0.get(local4370_1.name);
   }

   public static void loadLastConfig() {
      try {
         if (Files.exists(getLastConfigPath())) {
            activeConfig = sanitizeName(Files.readString(getLastConfigPath(), StandardCharsets.UTF_8).trim());
         }
      } catch (Exception exception) {
      }

      if (activeConfig.isEmpty()) {
         activeConfig = "default";
      }

      if (!exists(activeConfig)) {
         long local4372_0 = 0L;

         for (String local4372_3 : listConfigs()) {
            try {
               if (Files.getLastModifiedTime(getConfigPath(local4372_3)).toMillis() > local4372_0) {
                  local4372_0 = Files.getLastModifiedTime(getConfigPath(local4372_3)).toMillis();
                  activeConfig = local4372_3;
               }
            } catch (Exception exception2) {
            }
         }
      }

      load(activeConfig);
   }

   public static void autosave() {
      if (UnHookModule.isUnhooked()) {
         return;
      }

      String local4373_0 = sanitizeName(activeConfig);
      if (!local4373_0.isEmpty()) {
         String local4373_1;
         try {
            local4373_1 = serializeModules();
         } catch (Exception exception) {
            return;
         }

         if (!local4373_1.equals(lastSavedConfigs.get(local4373_0))) {
            lastSavedConfigs.put(local4373_0, local4373_1);
            queueWrite(getConfigPath(local4373_0), local4373_1, () -> {
               writeLastConfigName(local4373_0);
               revision++;
            }, () -> lastSavedConfigs.remove(local4373_0, local4373_1));
         }
      }
   }

   private static void deserializeSetting(JsonObject jsonObject, String local4375_1, Setting<?> local4375_2) {
      if (jsonObject.has(local4375_1)) {
         JsonElement jsonElement = jsonObject.get(local4375_1);
         if (local4375_2 instanceof BooleanSetting booleanSetting) {
            booleanSetting.setValue(jsonElement.getAsBoolean());
         } else if (local4375_2 instanceof ColorSetting colorSetting) {
            colorSetting.setValue(jsonElement.getAsInt());
         } else if (local4375_2 instanceof NumberSetting currentNumberSetting) {
            currentNumberSetting.setValue(jsonElement.getAsFloat());
         } else if (local4375_2 instanceof SingleChoiceSetting singleChoiceSetting) {
            singleChoiceSetting.selectOption(jsonElement.getAsString());
         } else if (local4375_2 instanceof MultiChoiceSetting multiChoiceSetting) {
            ArrayList<String> local4375_11 = new ArrayList<>();

            for (JsonElement jsonElement2 : jsonElement.getAsJsonArray()) {
               local4375_11.add(jsonElement2.getAsString());
            }

            multiChoiceSetting.withSelection(local4375_11.toArray(new String[0]));
         } else if (local4375_2 instanceof KeybindSetting keybindSetting) {
            keybindSetting.setBinding(parseBinding(jsonElement.getAsString()));
         } else if (local4375_2 instanceof TextSetting textSetting) {
            textSetting.setValue(jsonElement.getAsString());
         }
      }
   }

   public static boolean delete(String local4376_0) {
      try {
         if (Files.deleteIfExists(getConfigPath(local4376_0))) {
            revision++;
            return true;
         }
      } catch (Exception exception) {
      }

      return false;
   }

   public static boolean save(String local4377_0) {
      if (UnHookModule.isUnhooked() || sanitizeName(local4377_0).isEmpty()) {
         return false;
      }

      flushWrites();

      try {
         Files.createDirectories(getConfigDirectory());
         String local4377_1 = serializeModules();
         Files.writeString(getConfigPath(local4377_0), local4377_1, StandardCharsets.UTF_8);
         lastSavedConfigs.put(sanitizeName(local4377_0), local4377_1);
         selectActiveConfig(local4377_0);
         revision++;
         return true;
      } catch (Exception exception) {
         return false;
      }
   }

   private static void deserializeAutoBuy(JsonObject jsonObject) {
      if (jsonObject.has("speed")) {
         AutoBuySettingsScreen.getInstance().numberSetting.setValue(jsonObject.get("speed").getAsFloat());
      }

      if (jsonObject.has("buyPercent")) {
         AutoBuyModule.getInstance().buyPercent.setValue(jsonObject.get("buyPercent").getAsFloat());
      }

      if (jsonObject.has("sellPercent")) {
         AutoBuyModule.getInstance().sellPercent.setValue(jsonObject.get("sellPercent").getAsFloat());
      }

      if (jsonObject.has("buyMax")) {
         AutoBuySettingsScreen.getInstance().nested0374.setValue(jsonObject.get("buyMax").getAsString());
      }

      if (jsonObject.has("sellMin")) {
         AutoBuySettingsScreen.getInstance().nested03742.setValue(jsonObject.get("sellMin").getAsString());
      }

      if (jsonObject.has("durability")) {
         AutoBuySettingsScreen.getInstance().nested03743.setValue(jsonObject.get("durability").getAsString());
      }

      if (jsonObject.has("tgToken")) {
         AutoBuySettingsScreen.getInstance().textSetting2.setValue(jsonObject.get("tgToken").getAsString());
      }

      if (jsonObject.has("tgChat")) {
         AutoBuySettingsScreen.getInstance().textSetting3.setValue(jsonObject.get("tgChat").getAsString());
      }

      if (jsonObject.has("items") && jsonObject.get("items").isJsonArray()) {
         AutoBuySettingsScreen.getInstance().map2.clear();

         for (JsonElement jsonElement : jsonObject.getAsJsonArray("items")) {
            AutoBuySettingsScreen.getInstance().processData2(jsonElement.getAsJsonObject().get("name").getAsString());
            if (AutoBuySettingsScreen.getInstance().map2.get(jsonElement.getAsJsonObject().get("name").getAsString()) != null) {
               AutoBuySettingsScreen.getInstance().map2.get(jsonElement.getAsJsonObject().get("name").getAsString())
                  .nested03744
                  .setValue(jsonElement.getAsJsonObject().get("buy").getAsString());
               AutoBuySettingsScreen.getInstance().map2.get(jsonElement.getAsJsonObject().get("name").getAsString())
                  .nested03745
                  .setValue(jsonElement.getAsJsonObject().get("sell").getAsString());
            }
         }
      }
   }
}
