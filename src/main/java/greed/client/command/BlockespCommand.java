package greed.client.command;

import greed.client.module.render.BlockEspModule;
import greed.client.util.ClientChat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class BlockespCommand implements ClientCommand {
   @Override
   public String getDescription() {
      return "Свои блоки в BlockEsp (.blockesp add <блок> / del <блок> / list / clear)";
   }

   @Override
   public ArrayList<String> suggestArguments(String[] stringArray) {
      if (stringArray.length <= 1) {
         return new ArrayList<>(Arrays.asList("add", "del", "list", "clear"));
      }

      if (stringArray.length != 2) {
         return new ArrayList<>();
      }

      ArrayList<String> local4286_2 = new ArrayList<>();
      String local4286_3 = stringArray[1].toLowerCase(Locale.ROOT);
      if (!stringArray[0].equalsIgnoreCase("del") && !stringArray[0].equalsIgnoreCase("remove")) {
         for (Identifier identifier : Registries.BLOCK.getIds()) {
            if (identifier.getPath().startsWith(local4286_3)) {
               local4286_2.add(identifier.getPath());
            }

            if (local4286_2.size() >= 40) {
               break;
            }
         }

         return local4286_2;
      } else {
         for (Block block : BlockEspModule.getInstance().getArrayList()) {
            local4286_2.add(Registries.BLOCK.getId(block).getPath());
         }

         return local4286_2;
      }
   }

   private Block getBlock(String local4287_1) {
      Identifier identifier = Identifier.tryParse(
         local4287_1.toLowerCase(Locale.ROOT).contains(":") ? local4287_1.toLowerCase(Locale.ROOT) : "minecraft:" + local4287_1.toLowerCase(Locale.ROOT)
      );
      if (identifier == null) {
         return null;
      }

      Block block = Registries.BLOCK.get(identifier);
      return block == Blocks.AIR && !identifier.getPath().equals("air") ? null : block;
   }

   @Override
   public String getName() {
      return "blockesp";
   }

   @Override
   public void execute(String[] stringArray) {
      BlockEspModule blockEspModule = BlockEspModule.getInstance();
      if (stringArray.length == 0 || stringArray[0].equalsIgnoreCase("list")) {
         ArrayList<String> local4290_3 = new ArrayList<>();

         for (Block block : blockEspModule.getArrayList()) {
            local4290_3.add(Registries.BLOCK.getId(block).getPath());
         }

         ClientChat.processData8(
            local4290_3.isEmpty() ? "Своих блоков пока нет. Пример: &7.blockesp add diamond_ore" : "Свои блоки: &7" + String.join(", ", local4290_3)
         );
      } else if (stringArray[0].equalsIgnoreCase("clear")) {
         blockEspModule.updateState2();
         ClientChat.processData8("Список своих блоков очищен.");
      } else if (stringArray.length < 2) {
         ClientChat.processData8("Использование: &7.blockesp add <блок>&r, &7.blockesp del <блок>&r, &7.blockesp list&r, &7.blockesp clear");
      } else {
         Block block2 = this.getBlock(stringArray[1]);
         if (block2 == null) {
            ClientChat.processData8("Блок &7" + stringArray[1] + "&r не найден.");
         } else {
            String path = Registries.BLOCK.getId(block2).getPath();
            switch (stringArray[0].toLowerCase(Locale.ROOT)) {
               case "add":
                  ClientChat.processData8(
                     blockEspModule.matchesCriteria2(block2) ? "Блок &7" + path + "&r теперь подсвечивается." : "Блок &7" + path + "&r уже добавлен."
                  );
                  break;
               case "del":
               case "remove":
               case "delete":
                  ClientChat.processData8(
                     blockEspModule.matchesCriteria(block2) ? "Блок &7" + path + "&r убран." : "Блока &7" + path + "&r нет в списке."
                  );
                  break;
               default:
                  ClientChat.processData8("Использование: &7.blockesp add <блок>&r, &7.blockesp del <блок>&r, &7.blockesp list&r, &7.blockesp clear");
            }
         }
      }
   }
}
