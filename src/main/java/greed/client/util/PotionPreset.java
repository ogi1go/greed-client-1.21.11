package greed.client.util;

import greed.client.ui.SmoothScroll;

import greed.client.setting.KeybindSetting;
import greed.client.MinecraftAccess;
import greed.client.module.player.ServerHelperModule;
import greed.client.setting.TextSetting;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public final class PotionPreset implements MinecraftAccess {
   private static PotionPreset potionPreset;
   public TextSetting textSetting = (new TextSetting("Имя пресета", "Уникальное имя данного пресета", "", 7) {}).withPlaceholder("Введите текст...");
   public TextSetting textSetting2 = new TextSetting("Поиск", "").withPlaceholder("Поиск");
   public KeybindSetting keybindSetting = new KeybindSetting("Горячая клавиша", "Зелье выбрасываются при нажатии бинда");
   public ItemStack[] itemStackArray = new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
   public String[] strValArray = new String[6];
   public int intVal = -1;
   public ArrayList<PotionPreset.SubContext> list = new ArrayList<>();
   public SmoothScroll smoothScroll = new SmoothScroll();
   public SmoothScroll smoothScroll2 = new SmoothScroll();
   public float[] floatValArray;
   public float[] floatValArray2;

   public ArrayList<ServerHelperModule.SubContext> getArrayList() {
      ArrayList<ServerHelperModule.SubContext> local3393_1 = new ArrayList<>();

      for (ServerHelperModule.SubContext local3393_3 : ServerHelperModule.getInstance().getArrayList()) {
         if (local3393_3.getItem2() == null
            && (
               this.textSetting2.getValue().trim().isEmpty()
                  || local3393_3.getKeybindSetting().name.toLowerCase(Locale.ROOT).contains(this.textSetting2.getValue().toLowerCase(Locale.ROOT).trim())
            )) {
            local3393_1.add(local3393_3);
         }
      }

      return local3393_1;
   }

   public void processData(int local3394_1, String local3394_2) {
      if (local3394_1 >= 0 && local3394_1 < 6) {
         this.itemStackArray[local3394_1] = getItemStack(local3394_2);
         this.strValArray[local3394_1] = local3394_2;
         this.intVal = -1;
         this.textSetting2.setValue("");
      }
   }

   public int computeInt() {
      for (int index = 0; index < 6; index++) {
         if (this.itemStackArray[index].isEmpty()) {
            return index;
         }
      }

      return -1;
   }

   public static PotionPreset getInstance() {
      if (potionPreset == null) {
         potionPreset = new PotionPreset();
      }

      return potionPreset;
   }

   public void updateState() {
      JsonArray jsonArray = new JsonArray();

      for (PotionPreset.SubContext local3397_3 : this.list) {
         JsonObject jsonObject = new JsonObject();
         jsonObject.addProperty("name", local3397_3.strVal);
         jsonObject.addProperty("active", local3397_3.flag);
         jsonObject.addProperty("key", local3397_3.keybindSetting.getKeyCode());
         JsonArray jsonArray2 = new JsonArray();

         for (PotionPreset.SubContext2 local3397_7 : local3397_3.list2) {
            JsonObject jsonObject2 = new JsonObject();
            jsonObject2.addProperty("kind", local3397_7.strVal2);
            jsonObject2.addProperty("name", local3397_7.strVal3);
            jsonArray2.add(jsonObject2);
         }

         jsonObject.add("potions", jsonArray2);
         jsonArray.add(jsonObject);
      }

      try {
         Files.createDirectories(FabricLoader.getInstance().getConfigDir().resolve("greed"));
         Files.writeString(
            FabricLoader.getInstance().getConfigDir().resolve("greed").resolve("potioncombiner.json"),
            new GsonBuilder().setPrettyPrinting().create().toJson(jsonArray)
         );
      } catch (Exception exception) {
      }
   }

   public static ItemStack getItemStack(String local3398_0) {
      ItemStack itemStack = new ItemStack(Items.SPLASH_POTION);
      if (ServerItemType.getServerItemType(local3398_0) != null) {
         itemStack.set(
            DataComponentTypes.POTION_CONTENTS,
            new PotionContentsComponent(
               Optional.empty(), Optional.of(ServerItemType.getServerItemType(local3398_0).computeInt() | 0xFF000000), List.of(), Optional.empty()
            )
         );
      } else if (local3398_0.toLowerCase(Locale.ROOT).contains("прыгуч")) {
         itemStack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.empty(), Optional.of(-131196), List.of(), Optional.empty()));
      }

      return itemStack;
   }

   public void updateState2() {
      PotionPreset.SubContext local3399_1 = new PotionPreset.SubContext();
      local3399_1.strVal = this.textSetting.getValue().trim().isEmpty()
         ? "Прес " + (this.list.size() + 1)
         : this.textSetting.getValue().trim().substring(0, Math.min(7, this.textSetting.getValue().trim().length()));
      local3399_1.keybindSetting.setBinding(this.keybindSetting.getBinding());

      for (String local3399_5 : this.strValArray) {
         if (local3399_5 != null) {
            local3399_1.list2.add(new PotionPreset.SubContext2("splash", local3399_5));
         }
      }

      if (!local3399_1.list2.isEmpty()) {
         this.list.add(local3399_1);
         this.textSetting.setValue("");
         this.keybindSetting.setBinding(null);

         for (int index = 0; index < 6; index++) {
            this.itemStackArray[index] = ItemStack.EMPTY;
         }

         for (int index2 = 0; index2 < 6; index2++) {
            this.strValArray[index2] = null;
         }

         this.updateState();
      }
   }

   private PotionPreset() {
      this.updateState3();
   }

   private void updateState3() {
      try {
         if (!Files.exists(FabricLoader.getInstance().getConfigDir().resolve("greed").resolve("potioncombiner.json"))) {
            return;
         }

         for (JsonElement jsonElement : JsonParser.parseString(
               Files.readString(FabricLoader.getInstance().getConfigDir().resolve("greed").resolve("potioncombiner.json"))
            )
            .getAsJsonArray()) {
            PotionPreset.SubContext local3401_3 = new PotionPreset.SubContext();
            local3401_3.strVal = jsonElement.getAsJsonObject().get("name").getAsString();
            local3401_3.flag = jsonElement.getAsJsonObject().get("active").getAsBoolean();
            local3401_3.keybindSetting.withKeyCode(jsonElement.getAsJsonObject().get("key").getAsInt());

            for (JsonElement jsonElement2 : jsonElement.getAsJsonObject().getAsJsonArray("potions")) {
               local3401_3.list2
                  .add(
                     new PotionPreset.SubContext2(
                        jsonElement2.getAsJsonObject().get("kind").getAsString(), jsonElement2.getAsJsonObject().get("name").getAsString()
                     )
                  );
            }

            this.list.add(local3401_3);
         }
      } catch (Exception exception) {
      }
   }

   public static final class SubContext {
      public String strVal = "";
      public boolean flag = true;
      public KeybindSetting keybindSetting = new KeybindSetting("Горячая клавиша", "Зелье выбрасываются при нажатии бинда");
      public ArrayList<PotionPreset.SubContext2> list2 = new ArrayList<>();
      public boolean flag2;
   }

   public static final class SubContext2 {
      public String strVal2;
      public String strVal3;

      SubContext2(String local3404_1, String local3404_2) {
         this.strVal2 = local3404_1;
         this.strVal3 = local3404_2;
      }

      public ItemStack getItemStack2() {
         return PotionPreset.getItemStack(this.strVal3);
      }

      public static String formatText(ItemStack itemStack) {
         return itemStack.isOf(Items.SPLASH_POTION) ? "splash" : (itemStack.isOf(Items.LINGERING_POTION) ? "lingering" : "potion");
      }
   }
}
