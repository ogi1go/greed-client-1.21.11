package greed.client.module.misc;

import greed.client.setting.ModeSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.setting.NumericTextSetting;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@ModuleInfo(name = "InfoReplace", description = "Подменяет ранг, монеты и токены в сайдбаре.", category = ModuleCategory.MISC)
public final class InfoReplaceModule extends Module {
   private static InfoReplaceModule infoReplaceModule;
   public NumericTextSetting numericTextSetting = this.addSetting(new NumericTextSetting("Кол-во монет", "", "", 16));
   public NumericTextSetting numericTextSetting2 = this.addSetting(new NumericTextSetting("Кол-во токенов", "", "", 16));
   public ModeSetting modeSetting = this.addSetting(
      new ModeSetting(
         "Привилегия", "", "Игрок", "Игрок", "Барон", "Страж", "Герой", "Аспид", "Сквид", "Глава", "Элита", "Титан", "Принц", "Князь", "Герцог"
      )
   );

   public MutableText getMutableText(MutableText mutableText) {
      String string = mutableText.getString();
      String[] stringArray = new String[]{"Ранг: ", "Монет: ", "Токенов: "};
      Formatting[] formattingArray = new Formatting[]{Formatting.WHITE, Formatting.YELLOW, Formatting.GREEN};

      for (int index = 0; index < stringArray.length; index++) {
         int local5589_6 = string.indexOf(stringArray[index]);
         if (local5589_6 >= 3) {
            String local5589_7 = index == 0 ? this.modeSetting.getSelectedOption() : (index == 1 ? this.numericTextSetting : this.numericTextSetting2).getText();
            if (index > 0) {
               String local5589_8 = local5589_7.replaceAll("\\D", "");
               local5589_7 = local5589_8.isEmpty() ? "0" : String.valueOf(Long.parseLong(local5589_8));
            }

            return Text.empty()
               .append(Text.literal(string.substring(0, local5589_6 - 3)).fillStyle(Style.EMPTY.withColor(16720896)))
               .append(
                  Text.literal(string.substring(local5589_6 - 3, local5589_6))
                     .fillStyle(Style.EMPTY.withColor(index == 0 ? Formatting.AQUA : formattingArray[index]))
               )
               .append(Text.literal(stringArray[index]).fillStyle(Style.EMPTY.withColor(Formatting.WHITE)))
               .append(Text.literal(local5589_7).fillStyle(index == 0 ? this.getStyle(local5589_7) : Style.EMPTY.withColor(formattingArray[index])));
         }
      }

      return mutableText;
   }

   public static InfoReplaceModule getInstance() {
      if (infoReplaceModule == null) {
         infoReplaceModule = new InfoReplaceModule();
      }

      return infoReplaceModule;
   }

   private InfoReplaceModule() {
   }

   Style getStyle(String local5592_1) {
      return switch (local5592_1) {
         case "Страж" -> Style.EMPTY.withColor(Formatting.YELLOW);
         case "Барон" -> Style.EMPTY.withColor(Formatting.AQUA);
         case "Герой" -> Style.EMPTY.withColor(Formatting.GREEN);
         case "Аспид" -> Style.EMPTY.withColor(Formatting.DARK_AQUA);
         case "Сквид" -> Style.EMPTY.withColor(Formatting.AQUA).withItalic(true);
         case "Глава", "Титан" -> Style.EMPTY.withColor(Formatting.GOLD);
         case "Элита" -> Style.EMPTY.withColor(Formatting.DARK_PURPLE);
         case "Принц", "Князь" -> Style.EMPTY.withColor(16711680);
         case "Герцог" -> Style.EMPTY.withColor(Formatting.DARK_RED);
         default -> Style.EMPTY.withColor(Formatting.GREEN);
      };
   }
}
