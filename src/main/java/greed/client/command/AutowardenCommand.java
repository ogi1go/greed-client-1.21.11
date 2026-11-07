package greed.client.command;

import greed.client.module.misc.AutoWardenModule;
import greed.client.util.ClientChat;

import java.util.ArrayList;
import java.util.Arrays;

public class AutowardenCommand implements ClientCommand {
   @Override
   public String getName() {
      return "autowarden";
   }

   @Override
   public void execute(String[] stringArray) {
      if (stringArray.length != 0 && !stringArray[0].equalsIgnoreCase("list")) {
         if (stringArray[0].equalsIgnoreCase("clear")) {
            AutoWardenModule.getInstance().list3.clear();
            ClientChat.processData8("Список анархий очищен, бот останется на основной.");
         } else if (!stringArray[0].equalsIgnoreCase("del") && !stringArray[0].equalsIgnoreCase("remove")) {
            if (!stringArray[0].matches("\\d+")) {
               ClientChat.processData8("Использование: &7.autowarden <номер>&r, &7.autowarden del <номер>&r, &7.autowarden list&r, &7.autowarden clear");
            } else if (AutoWardenModule.getInstance().list3.contains(Integer.valueOf(stringArray[0]))) {
               ClientChat.processData8("Анархия &7" + stringArray[0] + "&r уже в списке.");
            } else {
               AutoWardenModule.getInstance().list3.add(Integer.valueOf(stringArray[0]));
               ClientChat.processData8("Анархия &7" + stringArray[0] + "&r добавлена, бот уйдёт туда после двух кругов на основной.");
            }
         } else if (stringArray.length >= 2 && stringArray[1].matches("\\d+")) {
            ClientChat.processData8(
               AutoWardenModule.getInstance().list3.remove(Integer.valueOf(stringArray[1]))
                  ? "Анархия &7" + stringArray[1] + "&r убрана из списка."
                  : "Анархии &7" + stringArray[1] + "&r в списке нет."
            );
         } else {
            ClientChat.processData8("Использование: &7.autowarden del <номер анархии>");
         }
      } else {
         ClientChat.processData8(
            AutoWardenModule.getInstance().list3.isEmpty()
               ? "Список анархий пуст, лут только на основной. Пример: &7.autowarden 302"
               : "Анархии для лута по очереди: &7" + AutoWardenModule.getInstance().list3.toString().replaceAll("[\\[\\]]", "")
         );
      }
   }

   @Override
   public ArrayList<String> suggestArguments(String[] stringArray) {
      if (stringArray.length <= 1) {
         return new ArrayList<>(Arrays.asList("del", "list", "clear"));
      }

      if (stringArray.length == 2 && (stringArray[0].equalsIgnoreCase("del") || stringArray[0].equalsIgnoreCase("remove"))) {
         ArrayList<String> local1281_2 = new ArrayList<>();

         for (Integer local1281_4 : AutoWardenModule.getInstance().list3) {
            local1281_2.add(String.valueOf(local1281_4));
         }

         return local1281_2;
      } else {
         return new ArrayList<>();
      }
   }

   @Override
   public String getDescription() {
      return "Анархии для AutoWarden (.autowarden 302 / del 302 / list / clear)";
   }
}
