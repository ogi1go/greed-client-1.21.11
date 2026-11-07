package greed.client.util;

import java.util.ArrayList;
import java.util.Optional;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

public final class FormattedText {
   private static Style getStyle2(char local7007_0, Style style) {
      Formatting formatting = Formatting.byCode(local7007_0);
      if (formatting == null) {
         return style;
      } else if (formatting == Formatting.RESET) {
         return Style.EMPTY;
      } else if (formatting == Formatting.BOLD) {
         return style.withBold(true);
      } else if (formatting == Formatting.ITALIC) {
         return style.withItalic(true);
      } else if (formatting == Formatting.UNDERLINE) {
         return style.withUnderline(true);
      } else if (formatting == Formatting.STRIKETHROUGH) {
         return style.withStrikethrough(true);
      } else if (formatting == Formatting.OBFUSCATED) {
         return style.withObfuscated(true);
      } else {
         return formatting.getColorValue() != null ? style.withColor(TextColor.fromFormatting(formatting)) : style;
      }
   }

   private static int computeInt(String local7008_0, int local7008_1) {
      int local7008_2 = 0;

      for (int index = 0; index < 6; index++) {
         if (local7008_0.charAt(local7008_1 + index * 2) != 167 || Character.digit(local7008_0.charAt(local7008_1 + index * 2 + 1), 16) < 0) {
            return -1;
         }

         local7008_2 = local7008_2 << 4 | Character.digit(local7008_0.charAt(local7008_1 + index * 2 + 1), 16);
      }

      return local7008_2;
   }

   private static int computeInt2(Style style, int local7009_1) {
      TextColor currentColor = style.getColor();
      return currentColor != null ? currentColor.getRgb() | 0xFF000000 : local7009_1;
   }

   public static ArrayList<FormattedText.DataRecord> getArrayList(String local7010_0, int local7010_1) {
      ArrayList<FormattedText.DataRecord> local7010_2 = new ArrayList<>();
      processData3(local7010_2, local7010_0, local7010_1);
      return local7010_2;
   }

   private static void processData(ArrayList<FormattedText.DataRecord> local7011_0, StringBuilder stringBuilder, Style style, int local7011_3) {
      if (!stringBuilder.isEmpty()) {
         local7011_0.add(getNested0521(style, stringBuilder.toString(), local7011_3));
         stringBuilder.setLength(0);
      }
   }

   public static String formatText(String local7013_0) {
      StringBuilder stringBuilder = new StringBuilder(local7013_0.length());

      for (int index = 0; index < local7013_0.length(); index++) {
         if (local7013_0.charAt(index) != 167) {
            stringBuilder.append(local7013_0.charAt(index));
         } else {
            if (index + 1 >= local7013_0.length()) {
               break;
            }

            if (local7013_0.charAt(index + 1) == 'x' && index + 13 < local7013_0.length() && computeInt(local7013_0, index + 2) >= 0) {
               index += 13;
            } else {
               index++;
            }
         }
      }

      return stringBuilder.toString();
   }

   public static ArrayList<FormattedText.DataRecord> getArrayList2(Text newText, int local7014_1) {
      ArrayList<FormattedText.DataRecord> local7014_2 = new ArrayList<>();
      processData2(local7014_2, newText, Style.EMPTY, local7014_1);
      return local7014_2;
   }

   private static void processData2(ArrayList<FormattedText.DataRecord> local7015_0, Text newText, Style style, int local7015_3) {
      Style style2 = newText.getStyle().withParent(style);
      newText.getContent().visit((style3, local7012_3) -> {
         if (local7012_3.isEmpty()) {
            return Optional.empty();
         }

         if (local7012_3.indexOf(167) >= 0) {
            processData3(local7015_0, local7012_3, local7015_3);
         } else {
            local7015_0.add(getNested0521(style3, local7012_3, local7015_3));
         }

         return Optional.empty();
      }, style2);

      for (Text currentText : newText.getSiblings()) {
         processData2(local7015_0, currentText, style2, local7015_3);
      }
   }

   private static FormattedText.DataRecord getNested0521(Style style, String local7016_1, int local7016_2) {
      return new FormattedText.DataRecord(
         local7016_1,
         computeInt2(style, local7016_2),
         style.isBold(),
         style.isItalic(),
         style.isUnderlined(),
         style.isStrikethrough(),
         style.isObfuscated()
      );
   }

   private FormattedText() {
   }

   private static void processData3(ArrayList<FormattedText.DataRecord> local7018_0, String local7018_1, int local7018_2) {
      Style style = Style.EMPTY;
      StringBuilder stringBuilder = new StringBuilder();

      for (int index = 0; index < local7018_1.length(); index++) {
         if (local7018_1.charAt(index) != 167) {
            stringBuilder.append(local7018_1.charAt(index));
         } else {
            if (index + 1 >= local7018_1.length()) {
               break;
            }

            processData(local7018_0, stringBuilder, style, local7018_2);
            char local7018_6 = local7018_1.charAt(index + 1);
            if (local7018_6 == 'x' && index + 13 < local7018_1.length()) {
               int local7018_7 = computeInt(local7018_1, index + 2);
               if (local7018_7 >= 0) {
                  style = Style.EMPTY.withColor(TextColor.fromRgb(local7018_7));
                  index += 13;
                  continue;
               }
            }

            style = getStyle2(local7018_6, style);
            index++;
         }
      }

      processData(local7018_0, stringBuilder, style, local7018_2);
   }

   public record DataRecord(String text, int color, boolean bold, boolean italic, boolean underlined, boolean strikethrough, boolean obfuscated) {
   }
}
