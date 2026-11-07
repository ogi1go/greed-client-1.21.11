package greed.client.command;

import greed.client.util.ClientChat;
import greed.client.storage.ConfigStore;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import net.minecraft.util.Util;

public class ConfigCommand implements ClientCommand {
   @Override
   public ArrayList<String> suggestArguments(String[] stringArray) {
      if (stringArray.length <= 1) {
         return new ArrayList<>(Arrays.asList("list", "create", "save", "load", "delete", "dir"));
      } else {
         return stringArray.length != 2
               || !stringArray[0].equalsIgnoreCase("load") && !stringArray[0].equalsIgnoreCase("delete") && !stringArray[0].equalsIgnoreCase("save")
            ? new ArrayList<>()
            : ConfigStore.listConfigs();
      }
   }

   @Override
   public String getDescription() {
      return "Управление конфигами (.cfg create/save/load/delete/list/dir)";
   }

   @Override
   public String getName() {
      return "cfg";
   }

   @Override
   public void execute(String[] stringArray) {
      if (stringArray.length == 0 || stringArray[0].equalsIgnoreCase("list")) {
         ClientChat.processData8(
            ConfigStore.listConfigs().isEmpty() ? "Конфигов пока нет." : "Конфиги: &7" + String.join(", ", ConfigStore.listConfigs())
         );
      } else if (stringArray[0].equalsIgnoreCase("dir")) {
         try {
            Files.createDirectories(ConfigStore.getConfigDirectory());
         } catch (Exception exception) {
         }

         Util.getOperatingSystem().open(ConfigStore.getConfigDirectory());
         ClientChat.processData8("Папка конфигов открыта.");
      } else if (stringArray.length < 2) {
         ClientChat.processData8("Использование: &7.cfg create/save/load/delete <имя>");
      } else {
         switch (stringArray[0].toLowerCase()) {
            case "create":
            case "save":
               ClientChat.processData8(
                  ConfigStore.save(String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length)))
                     ? "Конфиг &7" + String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length)) + "&r сохранён."
                     : "Не удалось сохранить конфиг."
               );
               break;
            case "load":
               ClientChat.processData8(
                  ConfigStore.load(String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length)))
                     ? "Конфиг &7" + String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length)) + "&r загружен."
                     : "Конфиг &7" + String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length)) + "&r не найден."
               );
               break;
            case "delete":
               ClientChat.processData8(
                  ConfigStore.delete(String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length)))
                     ? "Конфиг &7" + String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length)) + "&r удалён."
                     : "Конфиг &7" + String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length)) + "&r не найден."
               );
               break;
            default:
               ClientChat.processData8("Использование: &7.cfg create/save/load/delete <имя>");
         }
      }
   }
}
