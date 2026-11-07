package greed.client.command;

import greed.client.util.ClientChat;
import greed.client.util.InputBinding;
import greed.client.storage.MacroStore;

import java.util.ArrayList;
import java.util.Arrays;

public class MacroCommand implements ClientCommand {
   @Override
   public void execute(String[] stringArray) {
      if (stringArray.length != 0 && !stringArray[0].equalsIgnoreCase("list")) {
         if (stringArray[0].equalsIgnoreCase("clear")) {
            ClientChat.processData8(
               MacroStore.getInstance().getArrayList().isEmpty()
                  ? "Макросов нет."
                  : "Удалено макросов: &7" + MacroStore.getInstance().getArrayList().size()
            );
            MacroStore.getInstance().updateState2();
         } else if (!stringArray[0].equalsIgnoreCase("del") && !stringArray[0].equalsIgnoreCase("remove") && !stringArray[0].equalsIgnoreCase("delete")) {
            if (!stringArray[0].equalsIgnoreCase("add")) {
               ClientChat.processData8(
                  "Использование: &7.macro add <название> <клавиша> <текст>&r, &7.macro del <название>&r, &7.macro list&r, &7.macro clear"
               );
            } else if (stringArray.length < 4) {
               ClientChat.processData8("Использование: &7.macro add <название> <клавиша> <текст>&r, например &7.macro add дом H /home");
            } else if (!MacroStore.getInstance().getInputBinding(stringArray[2]).isBound()) {
               ClientChat.processData8("Клавиша &7" + stringArray[2] + "&r не найдена. Пиши так: &7H&r, &7F5&r, &7NUM1&r, &7SPACE&r, &7M4");
            } else {
               MacroStore.getInstance()
                  .processData(
                     stringArray[1],
                     MacroStore.getInstance().getInputBinding(stringArray[2]),
                     String.join(" ", Arrays.copyOfRange(stringArray, 3, stringArray.length))
                  );
               ClientChat.processData8(
                  "Макрос &7"
                     + stringArray[1]
                     + "&r на &7"
                     + MacroStore.getInstance().getInputBinding(stringArray[2]).getDisplayName()
                     + "&r пишет &7"
                     + String.join(" ", Arrays.copyOfRange(stringArray, 3, stringArray.length))
               );
            }
         } else if (stringArray.length < 2) {
            ClientChat.processData8("Использование: &7.macro del <название>");
         } else {
            ClientChat.processData8(
               MacroStore.getInstance().matchesCriteria(stringArray[1])
                  ? "Макрос &7" + stringArray[1] + "&r удалён."
                  : "Макрос &7" + stringArray[1] + "&r не найден."
            );
         }
      } else if (MacroStore.getInstance().getArrayList().isEmpty()) {
         ClientChat.processData8("Макросов пока нет. Пример: &7.macro add дом H /home");
      } else {
         ArrayList<String> local8039_2 = new ArrayList<>();

         for (MacroStore.DataRecord local8039_4 : MacroStore.getInstance().getArrayList()) {
            local8039_2.add("&7" + local8039_4.name() + " &8[" + local8039_4.key().getDisplayName() + "] &8— &f" + local8039_4.chatLine());
         }

         ClientChat.processData9("&fМакросы:", local8039_2.toArray(new String[0]));
      }
   }

   @Override
   public ArrayList<String> suggestArguments(String[] stringArray) {
      if (stringArray.length <= 1) {
         return new ArrayList<>(Arrays.asList("add", "del", "list", "clear"));
      }

      if (stringArray.length == 3 && stringArray[0].equalsIgnoreCase("add")) {
         ArrayList<String> local8040_2 = new ArrayList<>();

         for (int code = 32; code <= 348; code++) {
            if (!InputBinding.keyboardKey(code).getDisplayName().startsWith("KEY")
               && !local8040_2.contains(InputBinding.keyboardKey(code).getDisplayName())) {
               local8040_2.add(InputBinding.keyboardKey(code).getDisplayName());
            }
         }

         for (int index = 1; index <= 5; index++) {
            local8040_2.add("M" + index);
         }

         return local8040_2;
      } else {
         ArrayList<String> local8040_2 = new ArrayList<>();
         if (stringArray.length == 2
            && (stringArray[0].equalsIgnoreCase("del") || stringArray[0].equalsIgnoreCase("remove") || stringArray[0].equalsIgnoreCase("delete"))) {
            for (MacroStore.DataRecord local8040_4 : MacroStore.getInstance().getArrayList()) {
               local8040_2.add(local8040_4.name());
            }
         }

         return local8040_2;
      }
   }

   @Override
   public String getName() {
      return "macro";
   }

   @Override
   public String getDescription() {
      return "Макросы (.macro add <название> <клавиша> <текст> / del <название> / list / clear)";
   }
}
