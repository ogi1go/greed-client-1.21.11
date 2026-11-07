package greed.client.ui.panel;

import greed.client.util.AuctionItemCatalog;
import greed.client.storage.AutoBuyItem;
import greed.client.ui.ClickGuiStyle;
import greed.client.render.FontText;
import greed.client.render.ItemRenderCache;
import greed.client.render.RoundedClipStack;
import greed.client.ui.setting.SettingRenderContext;
import greed.client.ui.UiTheme;

import greed.client.ui.AutoBuySettingsScreen;
import greed.client.render.FontFace;
import greed.client.util.ItemDrawOptions;
import greed.client.render.Render2D;
import greed.client.setting.TextSetting;

import java.util.ArrayList;

final class AutoBuyItemPanel {
   private static final String[] priceLabels = new String[]{"500K", "250K", "100K", "50K", "0"};
   private static String selectedItemName;

   static void processData(
      SettingRenderContext settingRenderContext,
      AutoBuySettingsScreen autoBuySettingsScreen,
      AutoBuyPanel autoBuyPanel,
      float x,
      float y,
      float width,
      float height,
      boolean inputEnabled,
      long timeMillis
   ) {
      settingRenderContext.processData12(
         x, y, width, height, 4.0F * settingRenderContext.scale, ClickGuiStyle.intVal47, UiTheme.intVal17
      );
      boolean hasSelection = autoBuySettingsScreen.strVal != null && autoBuySettingsScreen.map2.get(autoBuySettingsScreen.strVal) != null;
      if (hasSelection) {
         selectedItemName = autoBuySettingsScreen.strVal;
      }

      FontText.processData3(
         FontFace.SEMI,
         "Цены",
         x + 7.0F * settingRenderContext.scale,
         y + 7.0F * settingRenderContext.scale,
         5.5F * settingRenderContext.scale,
         UiTheme.intVal
      );
      Render2D.pushAlpha(settingRenderContext.computeFloat2("ab:pricehint", !hasSelection));
      FontText.processData3(
         FontFace.SEMI,
         "для всех предметов",
         x
            + 7.0F * settingRenderContext.scale
            + FontText.computeFloat(FontFace.SEMI, "Цены", 5.5F * settingRenderContext.scale)
            + 4.0F * settingRenderContext.scale,
         y + 7.5F * settingRenderContext.scale,
         5.0F * settingRenderContext.scale,
         UiTheme.intVal3
      );
      Render2D.popAlpha();
      renderSelectionChip(
         settingRenderContext,
         autoBuySettingsScreen,
         autoBuyPanel,
         x,
         y - 1.5F * settingRenderContext.scale,
         width + 1.0F * settingRenderContext.scale,
         inputEnabled,
         hasSelection
      );
      float openProgress = AutoBuyPanel.computeFloat(timeMillis - autoBuySettingsScreen.timeMillis2, 0.0F, 0.16F);
      float valueFieldWidth = 92.0F * settingRenderContext.scale;
      float valueFieldHeight = 13.0F * settingRenderContext.scale;
      float valueFieldX = x + width - 7.0F * settingRenderContext.scale - valueFieldWidth;
      Render2D.pushAlpha(openProgress);
      FontText.processData3(
         FontFace.SEMI,
         "Покупка до",
         x + 7.0F * settingRenderContext.scale,
         y + 24.0F * settingRenderContext.scale + 3.75F * settingRenderContext.scale,
         5.5F * settingRenderContext.scale,
         UiTheme.intVal2
      );
      processData7(
         settingRenderContext,
         autoBuySettingsScreen,
         hasSelection ? autoBuySettingsScreen.map2.get(autoBuySettingsScreen.strVal).nested03744 : autoBuySettingsScreen.nested0374,
         hasSelection ? "item:buy:" + autoBuySettingsScreen.strVal : "buyMax",
         "$",
         null,
         valueFieldX,
         y + 24.0F * settingRenderContext.scale + (1.0F - openProgress) * 2.0F * settingRenderContext.scale,
         valueFieldWidth,
         valueFieldHeight,
         inputEnabled,
         timeMillis
      );
      FontText.processData3(
         FontFace.SEMI,
         "Продажа от",
         x + 7.0F * settingRenderContext.scale,
         y + 42.0F * settingRenderContext.scale + 3.75F * settingRenderContext.scale,
         5.5F * settingRenderContext.scale,
         UiTheme.intVal2
      );
      processData7(
         settingRenderContext,
         autoBuySettingsScreen,
         hasSelection ? autoBuySettingsScreen.map2.get(autoBuySettingsScreen.strVal).nested03745 : autoBuySettingsScreen.nested03742,
         hasSelection ? "item:sell:" + autoBuySettingsScreen.strVal : "sellMin",
         "$",
         null,
         valueFieldX,
         y + 42.0F * settingRenderContext.scale + (1.0F - openProgress) * 2.0F * settingRenderContext.scale,
         valueFieldWidth,
         valueFieldHeight,
         inputEnabled,
         timeMillis
      );
      Render2D.popAlpha();
      FontText.processData3(
         FontFace.SEMI,
         "Прочность от",
         x + 7.0F * settingRenderContext.scale,
         y + 60.0F * settingRenderContext.scale + 3.75F * settingRenderContext.scale,
         5.5F * settingRenderContext.scale,
         UiTheme.intVal2
      );
      processData7(
         settingRenderContext,
         autoBuySettingsScreen,
         autoBuySettingsScreen.nested03743,
         "durability",
         null,
         "%",
         valueFieldX,
         y + 60.0F * settingRenderContext.scale,
         valueFieldWidth,
         valueFieldHeight,
         inputEnabled,
         timeMillis
      );
   }

   static void processData2(
      SettingRenderContext settingRenderContext,
      AutoBuySettingsScreen autoBuySettingsScreen,
      AutoBuyPanel autoBuyPanel,
      float x,
      float y,
      float width,
      float height,
      boolean inputEnabled,
      long timeMillis
   ) {
      settingRenderContext.processData9(
         x, y, width, height, 4.0F * settingRenderContext.scale, ClickGuiStyle.intVal47, UiTheme.intVal17
      );
      FontText.processData3(
         FontFace.SEMI,
         "Pricing Configuration",
         x + 8.0F * settingRenderContext.scale,
         y + 8.0F * settingRenderContext.scale,
         5.0F * settingRenderContext.scale,
         UiTheme.intVal3
      );
      if (autoBuySettingsScreen.strVal != null && autoBuySettingsScreen.map2.get(autoBuySettingsScreen.strVal) != null) {
         selectedItemName = autoBuySettingsScreen.strVal;
      }

      renderSelectionChip(
         settingRenderContext,
         autoBuySettingsScreen,
         autoBuyPanel,
         x,
         y,
         width,
         inputEnabled,
         autoBuySettingsScreen.strVal != null && autoBuySettingsScreen.map2.get(autoBuySettingsScreen.strVal) != null
      );
      processData10(
         settingRenderContext,
         "Auto Buy Speed",
         "Select scan frequency and execution speed",
         x + 8.0F * settingRenderContext.scale,
         y + 22.0F * settingRenderContext.scale,
         86.0F * settingRenderContext.scale
      );
      renderSpeedSlider(
         settingRenderContext,
         autoBuySettingsScreen,
         x + 98.0F * settingRenderContext.scale,
         y + 25.0F * settingRenderContext.scale,
         width - 106.0F * settingRenderContext.scale,
         inputEnabled
      );
      Render2D.pushAlpha(AutoBuyPanel.computeFloat(timeMillis - autoBuySettingsScreen.timeMillis2, 0.0F, 0.16F));
      processData10(
         settingRenderContext,
         "Buy Price (Max)",
         "Maximum price limit for automatic purchases",
         x + 8.0F * settingRenderContext.scale,
         y
            + 48.0F * settingRenderContext.scale
            + (1.0F - AutoBuyPanel.computeFloat(timeMillis - autoBuySettingsScreen.timeMillis2, 0.0F, 0.16F)) * 2.0F * settingRenderContext.scale,
         86.0F * settingRenderContext.scale
      );
      processData7(
         settingRenderContext,
         autoBuySettingsScreen,
         autoBuySettingsScreen.strVal != null && autoBuySettingsScreen.map2.get(autoBuySettingsScreen.strVal) != null
            ? autoBuySettingsScreen.map2.get(autoBuySettingsScreen.strVal).nested03744
            : autoBuySettingsScreen.nested0374,
         autoBuySettingsScreen.strVal != null && autoBuySettingsScreen.map2.get(autoBuySettingsScreen.strVal) != null ? "item:buy:" + autoBuySettingsScreen.strVal : "buyMax",
         "$",
         null,
         x + 98.0F * settingRenderContext.scale,
         y
            + 51.0F * settingRenderContext.scale
            + (1.0F - AutoBuyPanel.computeFloat(timeMillis - autoBuySettingsScreen.timeMillis2, 0.0F, 0.16F)) * 2.0F * settingRenderContext.scale,
         width - 106.0F * settingRenderContext.scale,
         14.0F * settingRenderContext.scale,
         inputEnabled,
         timeMillis
      );
      Render2D.popAlpha();
      Render2D.pushAlpha(AutoBuyPanel.computeFloat(timeMillis - autoBuySettingsScreen.timeMillis2, 0.04F, 0.16F));
      processData10(
         settingRenderContext,
         "Sell Price (Min)",
         "Minimum listing price for automatic reselling",
         x + 8.0F * settingRenderContext.scale,
         y
            + 74.0F * settingRenderContext.scale
            + (1.0F - AutoBuyPanel.computeFloat(timeMillis - autoBuySettingsScreen.timeMillis2, 0.04F, 0.16F)) * 2.0F * settingRenderContext.scale,
         86.0F * settingRenderContext.scale
      );
      processData7(
         settingRenderContext,
         autoBuySettingsScreen,
         autoBuySettingsScreen.strVal != null && autoBuySettingsScreen.map2.get(autoBuySettingsScreen.strVal) != null
            ? autoBuySettingsScreen.map2.get(autoBuySettingsScreen.strVal).nested03745
            : autoBuySettingsScreen.nested03742,
         autoBuySettingsScreen.strVal != null && autoBuySettingsScreen.map2.get(autoBuySettingsScreen.strVal) != null ? "item:sell:" + autoBuySettingsScreen.strVal : "sellMin",
         "$",
         null,
         x + 98.0F * settingRenderContext.scale,
         y
            + 77.0F * settingRenderContext.scale
            + (1.0F - AutoBuyPanel.computeFloat(timeMillis - autoBuySettingsScreen.timeMillis2, 0.04F, 0.16F)) * 2.0F * settingRenderContext.scale,
         width - 106.0F * settingRenderContext.scale,
         14.0F * settingRenderContext.scale,
         inputEnabled,
         timeMillis
      );
      Render2D.popAlpha();
      processData10(
         settingRenderContext,
         "Minimum Durability",
         "Items below this durability will be ignored",
         x + 8.0F * settingRenderContext.scale,
         y + 100.0F * settingRenderContext.scale,
         86.0F * settingRenderContext.scale
      );
      processData7(
         settingRenderContext,
         autoBuySettingsScreen,
         autoBuySettingsScreen.nested03743,
         "durability",
         null,
         "%",
         x + 98.0F * settingRenderContext.scale,
         y + 103.0F * settingRenderContext.scale,
         width - 106.0F * settingRenderContext.scale,
         14.0F * settingRenderContext.scale,
         inputEnabled,
         timeMillis
      );
   }

   static void processData3(
      SettingRenderContext settingRenderContext,
      AutoBuySettingsScreen autoBuySettingsScreen,
      float x,
      float y,
      float width,
      float height,
      boolean inputEnabled
   ) {
      settingRenderContext.processData12(
         x,
         y,
         width,
         height,
         4.0F * settingRenderContext.scale,
         ClickGuiStyle.intVal47,
         UiTheme.computeInt2(
            UiTheme.intVal17,
            UiTheme.intVal18,
            settingRenderContext.computeFloat2("ab:searchf", settingRenderContext.textSetting == autoBuySettingsScreen.textSetting)
         )
      );
      settingRenderContext.processData4(
         "R",
         x + 8.0F * settingRenderContext.scale + 2.75F * settingRenderContext.scale,
         y + height / 2.0F,
         5.5F * settingRenderContext.scale,
         UiTheme.computeInt2(
            UiTheme.intVal3,
            UiTheme.intVal2,
            settingRenderContext.computeFloat2("ab:searchf", settingRenderContext.textSetting == autoBuySettingsScreen.textSetting)
         )
      );
      FontText.processData3(
         FontFace.SEMI,
         (
               autoBuySettingsScreen.textSetting.getValue().isEmpty() && settingRenderContext.textSetting != autoBuySettingsScreen.textSetting
                  ? autoBuySettingsScreen.textSetting.getPlaceholder()
                  : autoBuySettingsScreen.textSetting.getValue()
            )
            + (settingRenderContext.textSetting == autoBuySettingsScreen.textSetting ? SettingRenderContext.formatText() : ""),
         x + 8.0F * settingRenderContext.scale + 5.5F * settingRenderContext.scale + 4.0F * settingRenderContext.scale,
         y + height / 2.0F - 2.75F * settingRenderContext.scale,
         5.5F * settingRenderContext.scale,
         autoBuySettingsScreen.textSetting.getValue().isEmpty() ? UiTheme.intVal2 : UiTheme.intVal
      );
      if (inputEnabled) {
         settingRenderContext.processData7(x, y, width, height, null, () -> {
            settingRenderContext.textSetting = autoBuySettingsScreen.textSetting;
            autoBuySettingsScreen.textSetting.moveToEnd(false);
         });
      }
   }

   static void processData4(
      SettingRenderContext settingRenderContext,
      AutoBuySettingsScreen autoBuySettingsScreen,
      float x,
      float y,
      float width,
      float height,
      boolean inputEnabled,
      long timeMillis
   ) {
      settingRenderContext.processData12(
         x, y, width, height, 4.0F * settingRenderContext.scale, ClickGuiStyle.intVal47, UiTheme.intVal17
      );
      FontText.processData3(
         FontFace.SEMI,
         "Telegram",
         x + 7.0F * settingRenderContext.scale,
         y + 7.0F * settingRenderContext.scale,
         5.5F * settingRenderContext.scale,
         UiTheme.intVal
      );
      FontText.processData3(
         FontFace.SEMI,
         "уведомления о покупках",
         x
            + 7.0F * settingRenderContext.scale
            + FontText.computeFloat(FontFace.SEMI, "Telegram", 5.5F * settingRenderContext.scale)
            + 4.0F * settingRenderContext.scale,
         y + 7.5F * settingRenderContext.scale,
         5.0F * settingRenderContext.scale,
         UiTheme.intVal3
      );
      float valueFieldWidth = 92.0F * settingRenderContext.scale;
      float valueFieldHeight = 13.0F * settingRenderContext.scale;
      float valueFieldX = x + width - 7.0F * settingRenderContext.scale - valueFieldWidth;
      FontText.processData3(
         FontFace.SEMI,
         "Токен бота",
         x + 7.0F * settingRenderContext.scale,
         y + 20.0F * settingRenderContext.scale + 3.75F * settingRenderContext.scale,
         5.5F * settingRenderContext.scale,
         UiTheme.intVal2
      );
      processData7(
         settingRenderContext,
         autoBuySettingsScreen,
         autoBuySettingsScreen.textSetting2,
         "tgToken",
         null,
         null,
         valueFieldX,
         y + 20.0F * settingRenderContext.scale,
         valueFieldWidth,
         valueFieldHeight,
         inputEnabled,
         timeMillis
      );
      FontText.processData3(
         FontFace.SEMI,
         "ID чата",
         x + 7.0F * settingRenderContext.scale,
         y + 36.0F * settingRenderContext.scale + 3.75F * settingRenderContext.scale,
         5.5F * settingRenderContext.scale,
         UiTheme.intVal2
      );
      processData7(
         settingRenderContext,
         autoBuySettingsScreen,
         autoBuySettingsScreen.textSetting3,
         "tgChat",
         null,
         null,
         valueFieldX,
         y + 36.0F * settingRenderContext.scale,
         valueFieldWidth,
         valueFieldHeight,
         inputEnabled,
         timeMillis
      );
   }

   static void processData5(
      SettingRenderContext settingRenderContext,
      AutoBuySettingsScreen autoBuySettingsScreen,
      float x,
      float y,
      float width,
      float height,
      boolean inputEnabled,
      long timeMillis
   ) {
      settingRenderContext.processData9(
         x, y, width, height, 4.0F * settingRenderContext.scale, ClickGuiStyle.intVal47, UiTheme.intVal17
      );
      FontText.processData3(
         FontFace.SEMI,
         "Bot API Configuration",
         x + 8.0F * settingRenderContext.scale,
         y + 8.0F * settingRenderContext.scale,
         5.0F * settingRenderContext.scale,
         UiTheme.intVal3
      );
      processData10(
         settingRenderContext,
         "Telegram Bot Token",
         "API token obtained from @BotFather",
         x + 8.0F * settingRenderContext.scale,
         y + 22.0F * settingRenderContext.scale,
         86.0F * settingRenderContext.scale
      );
      processData7(
         settingRenderContext,
         autoBuySettingsScreen,
         autoBuySettingsScreen.textSetting2,
         "tgToken",
         null,
         null,
         x + 98.0F * settingRenderContext.scale,
         y + 22.0F * settingRenderContext.scale,
         width - 106.0F * settingRenderContext.scale,
         14.0F * settingRenderContext.scale,
         inputEnabled,
         timeMillis
      );
      processData10(
         settingRenderContext,
         "Telegram Chat ID",
         "Target chat or channel ID for alerts",
         x + 8.0F * settingRenderContext.scale,
         y + 42.0F * settingRenderContext.scale,
         86.0F * settingRenderContext.scale
      );
      processData7(
         settingRenderContext,
         autoBuySettingsScreen,
         autoBuySettingsScreen.textSetting3,
         "tgChat",
         null,
         null,
         x + 98.0F * settingRenderContext.scale,
         y + 42.0F * settingRenderContext.scale,
         width - 106.0F * settingRenderContext.scale,
         14.0F * settingRenderContext.scale,
         inputEnabled,
         timeMillis
      );
   }

   static void processData6(
      SettingRenderContext settingRenderContext,
      AutoBuySettingsScreen autoBuySettingsScreen,
      float x,
      float y,
      float width,
      float height,
      float scrollDelta,
      long timeMillis,
      long startTimeMillis
   ) {
      settingRenderContext.processData12(
         x, y, width, height, 4.0F * settingRenderContext.scale, ClickGuiStyle.intVal47, UiTheme.intVal17
      );
      FontText.processData3(
         FontFace.SEMI,
         "История покупок",
         x + 7.0F * settingRenderContext.scale,
         y + 7.0F * settingRenderContext.scale,
         5.5F * settingRenderContext.scale,
         UiTheme.intVal
      );
      float listTop = y + 22.0F * settingRenderContext.scale;
      float listHeight = y + height - 8.0F * settingRenderContext.scale - listTop;
      float priceColumnWidth = width - 20.0F * settingRenderContext.scale;
      autoBuySettingsScreen.floatValArray4 = new float[]{x, listTop, width, listHeight};
      autoBuySettingsScreen.smoothScroll2.maximumOffset = Math.max(
         0.0F, autoBuySettingsScreen.list.size() * 13.0F * settingRenderContext.scale - 6.0F * settingRenderContext.scale - listHeight
      );
      autoBuySettingsScreen.smoothScroll2.update(scrollDelta);
      if (autoBuySettingsScreen.list.isEmpty()) {
         FontText.processData3(
            FontFace.SEMI,
            "Покупок пока нет",
            x + (width - FontText.computeFloat(FontFace.SEMI, "Покупок пока нет", 5.0F * settingRenderContext.scale)) / 2.0F,
            listTop + listHeight / 2.0F - 2.5F * settingRenderContext.scale,
            5.0F * settingRenderContext.scale,
            UiTheme.intVal4
         );
      }

      Render2D.beginScissor(
         settingRenderContext.drawContext,
         x + 6.0F * settingRenderContext.scale,
         listTop - 1.0F * settingRenderContext.scale,
         width - 12.0F * settingRenderContext.scale,
         listHeight + 2.0F * settingRenderContext.scale
      );

      for (int index = 0; index < autoBuySettingsScreen.list.size(); index++) {
         float rowY = listTop + index * 13.0F * settingRenderContext.scale - autoBuySettingsScreen.smoothScroll2.getPixelAlignedOffset();
         if (!(rowY + 7.0F * settingRenderContext.scale < listTop - 2.0F) && !(rowY > listTop + listHeight + 2.0F)) {
            float rowProgress = AutoBuyPanel.computeFloat(timeMillis - startTimeMillis, 0.22F + Math.min(4, index) * 0.06F, 0.25F);
            if (!(rowProgress < 0.005F)) {
               Render2D.pushAlpha(rowProgress);
               ItemRenderCache.processData(
                  autoBuySettingsScreen.list.get(index).item().displayStack(),
                  x + 8.0F * settingRenderContext.scale + (1.0F - rowProgress) * 4.0F * settingRenderContext.scale,
                  rowY + 0.75F * settingRenderContext.scale,
                  5.5F * settingRenderContext.scale,
                  ItemDrawOptions.noDecorations(AutoBuyPanel.computeFloat2())
               );
               Render2D.beginScissor(
                  settingRenderContext.drawContext,
                  x + 17.5F * settingRenderContext.scale,
                  rowY - 1.0F * settingRenderContext.scale,
                  75.0F * settingRenderContext.scale,
                  9.0F * settingRenderContext.scale
               );
               FontText.processData3(
                  FontFace.SEMI,
                  autoBuySettingsScreen.list.get(index).item().name(),
                  x + 17.5F * settingRenderContext.scale + (1.0F - rowProgress) * 4.0F * settingRenderContext.scale,
                  rowY,
                  5.5F * settingRenderContext.scale,
                  UiTheme.intVal
               );
               FontText.processData3(
                  FontFace.SEMI,
                  "x" + autoBuySettingsScreen.list.get(index).count(),
                  x
                     + 19.5F * settingRenderContext.scale
                     + (1.0F - rowProgress) * 4.0F * settingRenderContext.scale
                     + FontText.computeFloat(
                        FontFace.SEMI, autoBuySettingsScreen.list.get(index).item().name(), 5.5F * settingRenderContext.scale
                     ),
                  rowY + 0.5F * settingRenderContext.scale,
                  5.0F * settingRenderContext.scale,
                  UiTheme.intVal2
               );
               Render2D.endScissor(settingRenderContext.drawContext);
               FontText.processData3(
                  FontFace.SEMI,
                  autoBuySettingsScreen.list.get(index).time(),
                  x + 95.0F * settingRenderContext.scale,
                  rowY,
                  5.5F * settingRenderContext.scale,
                  UiTheme.intVal3
               );
               FontText.processData3(
                  FontFace.SEMI,
                  AutoBuySettingsScreen.formatText(autoBuySettingsScreen.list.get(index).priceCoins()),
                  x
                     + 8.0F * settingRenderContext.scale
                     + priceColumnWidth
                     - FontText.computeFloat(
                        FontFace.SEMI,
                        AutoBuySettingsScreen.formatText(autoBuySettingsScreen.list.get(index).priceCoins()),
                        5.5F * settingRenderContext.scale
                     ),
                  rowY,
                  5.5F * settingRenderContext.scale,
                  UiTheme.intVal
               );
               FontText.processData3(
                  FontFace.SEMI,
                  "$",
                  x
                     + 8.0F * settingRenderContext.scale
                     + priceColumnWidth
                     - FontText.computeFloat(
                        FontFace.SEMI,
                        AutoBuySettingsScreen.formatText(autoBuySettingsScreen.list.get(index).priceCoins()),
                        5.5F * settingRenderContext.scale
                     )
                     - FontText.computeFloat(FontFace.SEMI, "$", 5.5F * settingRenderContext.scale),
                  rowY,
                  5.5F * settingRenderContext.scale,
                  UiTheme.intVal7
               );
               Render2D.popAlpha();
            }
         }
      }

      Render2D.endScissor(settingRenderContext.drawContext);
      Render2D.drawRect(
         x + width - 5.0F * settingRenderContext.scale,
         listTop,
         1.0F * settingRenderContext.scale,
         listHeight,
         0.5F * settingRenderContext.scale,
         UiTheme.intVal17
      );
      if (autoBuySettingsScreen.smoothScroll2.maximumOffset > 0.0F) {
         Render2D.drawRect(
            x + width - 5.0F * settingRenderContext.scale,
            listTop
               + (listHeight - Math.max(6.0F * settingRenderContext.scale, listHeight * (listHeight / (listHeight + autoBuySettingsScreen.smoothScroll2.maximumOffset))))
                  * (autoBuySettingsScreen.smoothScroll2.getPixelAlignedOffset() / autoBuySettingsScreen.smoothScroll2.maximumOffset),
            1.0F * settingRenderContext.scale,
            Math.max(6.0F * settingRenderContext.scale, listHeight * (listHeight / (listHeight + autoBuySettingsScreen.smoothScroll2.maximumOffset))),
            0.5F * settingRenderContext.scale,
            UiTheme.intVal5
         );
      }
   }

   private AutoBuyItemPanel() {
   }

   static void processData7(
      SettingRenderContext settingRenderContext,
      AutoBuySettingsScreen autoBuySettingsScreen,
      TextSetting newTextSetting,
      String settingKey,
      String prefix,
      String suffix,
      float x,
      float y,
      float width,
      float height,
      boolean inputEnabled,
      long timeMillis
   ) {
      Render2D.drawRect(x, y, width, height, 2.0F * settingRenderContext.scale, ClickGuiStyle.intVal47);
      Render2D.drawOutline(
         x,
         y,
         width,
         height,
         2.0F * settingRenderContext.scale,
         0.5F,
         UiTheme.computeInt2(
            UiTheme.intVal17,
            UiTheme.intVal18,
            settingRenderContext.computeFloat2("ab:inf:" + settingKey, settingRenderContext.textSetting == newTextSetting)
         )
      );
      Render2D.drawRect(
         x + width - 14.5F * settingRenderContext.scale,
         y + 1.0F * settingRenderContext.scale,
         0.5F * settingRenderContext.scale,
         height - 2.0F * settingRenderContext.scale,
         0.0F,
         UiTheme.intVal17
      );
      String displayText = settingRenderContext.textSetting == newTextSetting
         ? (prefix == null ? "" : prefix)
            + (
               newTextSetting.getValue().isEmpty()
                  ? ""
                  : (
                     prefix == null && suffix == null
                        ? newTextSetting.getValue()
                        : AutoBuySettingsScreen.formatText(AutoBuySettingsScreen.computeLong(newTextSetting.getValue()))
                  )
            )
            + SettingRenderContext.formatText()
            + (suffix == null ? "" : suffix)
         : (
            newTextSetting.getValue().isEmpty()
               ? newTextSetting.getPlaceholder()
               : (
                  prefix == null && suffix == null
                     ? newTextSetting.getValue()
                     : (prefix == null ? "" : prefix)
                        + AutoBuySettingsScreen.formatText(AutoBuySettingsScreen.computeLong(newTextSetting.getValue()))
                        + (suffix == null ? "" : suffix)
               )
         );
      Render2D.beginScissor(
         settingRenderContext.drawContext,
         x + 5.0F * settingRenderContext.scale,
         y,
         width - 14.5F * settingRenderContext.scale - 8.0F * settingRenderContext.scale,
         height
      );
      FontText.processData3(
         FontFace.SEMI,
         displayText,
         settingRenderContext.textSetting == newTextSetting
               && FontText.computeFloat(FontFace.SEMI, displayText, 5.0F * settingRenderContext.scale)
                  > width - 14.5F * settingRenderContext.scale - 8.0F * settingRenderContext.scale
            ? x
               + width
               - 17.5F * settingRenderContext.scale
               - FontText.computeFloat(FontFace.SEMI, displayText, 5.0F * settingRenderContext.scale)
            : x + 5.0F * settingRenderContext.scale,
         y + height / 2.0F - 2.5F * settingRenderContext.scale,
         5.0F * settingRenderContext.scale,
         newTextSetting.getValue().isEmpty() && settingRenderContext.textSetting != newTextSetting ? UiTheme.intVal3 : UiTheme.intVal
      );
      Render2D.endScissor(settingRenderContext.drawContext);
      float clearProgress = autoBuySettingsScreen.map4.get(settingKey) != null && (float)(timeMillis - autoBuySettingsScreen.map4.get(settingKey)) / 400.0F < 1.0F
         ? 1.0F - (float)(timeMillis - autoBuySettingsScreen.map4.get(settingKey)) / 400.0F
         : 0.0F;
      if (autoBuySettingsScreen.map4.get(settingKey) != null && clearProgress <= 0.0F) {
         autoBuySettingsScreen.map4.remove(settingKey);
      }

      Render2D.drawRect(
         x + width - 9.5F * settingRenderContext.scale,
         y + height / 2.0F - 2.5F * settingRenderContext.scale,
         5.0F * settingRenderContext.scale,
         5.0F * settingRenderContext.scale,
         1.5F * settingRenderContext.scale,
         UiTheme.computeInt2(
            UiTheme.computeInt2(
               UiTheme.intVal7,
               UiTheme.intVal9,
               settingRenderContext.computeFloat2(
                  "ab:int:" + settingKey,
                  inputEnabled
                     && UiTheme.matchesCriteria(
                        settingRenderContext.mouseX,
                        settingRenderContext.mouseY,
                        x + width - 14.0F * settingRenderContext.scale,
                        y,
                        14.0F * settingRenderContext.scale,
                        height
                     )
               )
            ),
            UiTheme.intVal,
            clearProgress
         )
      );
      Render2D.drawRect(
         x + width - 9.5F * settingRenderContext.scale + 1.1F * settingRenderContext.scale,
         y + height / 2.0F - 2.5F * settingRenderContext.scale + 1.1F * settingRenderContext.scale,
         5.0F * settingRenderContext.scale - 2.2F * settingRenderContext.scale,
         5.0F * settingRenderContext.scale - 2.2F * settingRenderContext.scale,
         0.9F * settingRenderContext.scale,
         -1
      );
      Render2D.drawCircle(
         x + width - 7.0F * settingRenderContext.scale, y + height / 2.0F, 0.7F * settingRenderContext.scale, -3158318
      );
      if (inputEnabled) {
         settingRenderContext.processData7(x, y, width - 14.0F * settingRenderContext.scale, height, null, () -> {
            settingRenderContext.textSetting = newTextSetting;
            newTextSetting.moveToEnd(false);
         });
         settingRenderContext.processData7(
            x + width - 14.0F * settingRenderContext.scale, y, 14.0F * settingRenderContext.scale, height, null, () -> {
               if (settingRenderContext.textSetting == newTextSetting) {
                  settingRenderContext.textSetting = null;
               }

               autoBuySettingsScreen.map4.put(settingKey, System.currentTimeMillis());
            }
         );
      }
   }

   private static void renderSpeedSlider(
      SettingRenderContext settingRenderContext, AutoBuySettingsScreen autoBuySettingsScreen, float x, float y, float width, boolean inputEnabled
   ) {
      if (Math.max(0, Math.min(2, autoBuySettingsScreen.numberSetting.intValue())) != 2) {
         FontText.processData3(
            FontFace.SEMI,
            AutoBuySettingsScreen.strValArray[Math.max(0, Math.min(2, autoBuySettingsScreen.numberSetting.intValue()))],
            x,
            y,
            4.5F * settingRenderContext.scale,
            UiTheme.intVal2
         );
      }

      FontText.processData3(
         FontFace.SEMI,
         AutoBuySettingsScreen.strValArray[2],
         x + width - FontText.computeFloat(FontFace.SEMI, AutoBuySettingsScreen.strValArray[2], 4.5F * settingRenderContext.scale),
         y,
         4.5F * settingRenderContext.scale,
         UiTheme.intVal
      );
      Render2D.drawRect(
         x,
         y + 7.0F * settingRenderContext.scale,
         width,
         7.0F * settingRenderContext.scale,
         2.0F * settingRenderContext.scale,
         ClickGuiStyle.intVal48
      );
      Render2D.drawRect(
         x,
         y + 7.0F * settingRenderContext.scale + 0.15F,
         x
            + 1.0F * settingRenderContext.scale
            + settingRenderContext.computeFloat7("ab:speed", autoBuySettingsScreen.numberSetting.getFraction())
               * (width - 2.0F * settingRenderContext.scale - 5.0F * settingRenderContext.scale)
            + 6.0F * settingRenderContext.scale
            - x,
         7.0F * settingRenderContext.scale,
         2.0F * settingRenderContext.scale,
         UiTheme.computeInt2(
            UiTheme.intVal7,
            UiTheme.computeInt2(UiTheme.intVal13, UiTheme.intVal7, 0.3F),
            settingRenderContext.computeFloat10(autoBuySettingsScreen.numberSetting, autoBuySettingsScreen.numberSetting.intValue() == 2)
         )
      );
      if (settingRenderContext.computeFloat10(autoBuySettingsScreen.numberSetting, autoBuySettingsScreen.numberSetting.intValue() == 2) > 0.005F) {
         Render2D.drawDither(
            SettingRenderContext.computeFloat8(x),
            SettingRenderContext.computeFloat8((y + 7.0F * settingRenderContext.scale) * settingRenderContext.scale),
            width,
            7.0F * settingRenderContext.scale,
            2.0F * settingRenderContext.scale,
            Math.max(2.0F, Math.round(3.0F * settingRenderContext.scale)) / 2.0F,
            settingRenderContext.computeFloat10(autoBuySettingsScreen.numberSetting, autoBuySettingsScreen.numberSetting.intValue() == 2),
            UiTheme.intVal7
         );
      }

      if (settingRenderContext.computeFloat10(autoBuySettingsScreen.numberSetting, autoBuySettingsScreen.numberSetting.intValue() == 2) > 0.005F) {
         Render2D.beginScissor(
            settingRenderContext.drawContext, x, y + 7.0F * settingRenderContext.scale, width, 7.0F * settingRenderContext.scale
         );
         RoundedClipStack.processData(
            x,
            y + 7.0F * settingRenderContext.scale,
            width,
            7.0F * settingRenderContext.scale,
            2.0F * settingRenderContext.scale,
            2.0F * settingRenderContext.scale,
            2.0F * settingRenderContext.scale,
            2.0F * settingRenderContext.scale
         );

         for (int index = 0; index * Math.max(2.0F, Math.round(3.0F * settingRenderContext.scale)) < width; index++) {
            for (int index2 = 0; index2 * Math.max(2.0F, Math.round(3.0F * settingRenderContext.scale)) < 7.0F * settingRenderContext.scale; index2++) {
               if ((index + index2 + (int)(System.currentTimeMillis() / 90L)) % 2 != 1) {
                  Render2D.drawRect(
                     x + index * Math.max(2.0F, Math.round(3.0F * settingRenderContext.scale)),
                     y + 7.0F * settingRenderContext.scale + index2 * Math.max(2.0F, Math.round(3.0F * settingRenderContext.scale)),
                     Math.max(2.0F, Math.round(3.0F * settingRenderContext.scale)),
                     Math.max(2.0F, Math.round(3.0F * settingRenderContext.scale)),
                     0.0F,
                     UiTheme.computeInt(
                        UiTheme.intVal7,
                        settingRenderContext.computeFloat10(autoBuySettingsScreen.numberSetting, autoBuySettingsScreen.numberSetting.intValue() == 2)
                           * (0.28F + 0.34F * (float)Math.sin(System.currentTimeMillis() / 260.0 + index * 0.55F))
                     )
                  );
               }
            }
         }

         RoundedClipStack.updateState2();
         Render2D.endScissor(settingRenderContext.drawContext);
      }

      for (int index3 = 0; index3 < 3; index3++) {
         Render2D.drawRect(
            x + 1.5F * settingRenderContext.scale + index3 * (width - 5.0F * settingRenderContext.scale) / 2.0F,
            y + 10.5F * settingRenderContext.scale - 1.0F * settingRenderContext.scale + 0.25F,
            2.0F * settingRenderContext.scale,
            2.0F * settingRenderContext.scale,
            1.0F * settingRenderContext.scale,
            UiTheme.intVal4
         );
      }

      Render2D.drawRect(
         x
            + 1.0F * settingRenderContext.scale
            + settingRenderContext.computeFloat7("ab:speed", autoBuySettingsScreen.numberSetting.getFraction()) * (width - 7.0F * settingRenderContext.scale),
         y + 8.0F * settingRenderContext.scale,
         5.0F * settingRenderContext.scale,
         5.0F * settingRenderContext.scale,
         1.0F * settingRenderContext.scale,
         UiTheme.intVal10
      );
      if (inputEnabled) {
         settingRenderContext.map7.put(autoBuySettingsScreen.numberSetting, new float[]{x, width});
         settingRenderContext.processData7(
            x,
            y + 5.0F * settingRenderContext.scale,
            width,
            11.0F * settingRenderContext.scale,
            null,
            () -> settingRenderContext.numberSetting = autoBuySettingsScreen.numberSetting
         );
      }
   }

   private static void renderSelectionChip(
      SettingRenderContext settingRenderContext,
      AutoBuySettingsScreen autoBuySettingsScreen,
      AutoBuyPanel autoBuyPanel,
      float x,
      float y,
      float width,
      boolean inputEnabled,
      boolean editMode
   ) {
      if (!(settingRenderContext.computeFloat2("ab:editmode", editMode) < 0.005F) && selectedItemName != null && AuctionItemCatalog.getAutoBuyItem(selectedItemName) != null) {
         float chipWidth = 3.0F * settingRenderContext.scale
            + 5.5F * settingRenderContext.scale
            + 3.0F * settingRenderContext.scale
            + FontText.computeFloat(FontFace.SEMI, selectedItemName, 5.0F * settingRenderContext.scale)
            + 4.0F * settingRenderContext.scale
            + 5.0F * settingRenderContext.scale
            + 3.0F * settingRenderContext.scale;
         float chipX = x
            + width
            - 8.0F * settingRenderContext.scale
            - chipWidth
            + (1.0F - settingRenderContext.computeFloat2("ab:editmode", editMode)) * 8.0F * settingRenderContext.scale;
         float chipY = y + 8.0F * settingRenderContext.scale + 2.5F * settingRenderContext.scale - 5.0F * settingRenderContext.scale;
         Render2D.pushAlpha(settingRenderContext.computeFloat2("ab:editmode", editMode));
         Render2D.drawRect(
            chipX, chipY, chipWidth, 10.0F * settingRenderContext.scale, 2.0F * settingRenderContext.scale, ClickGuiStyle.intVal49
         );
         ItemRenderCache.processData(
            AuctionItemCatalog.getAutoBuyItem(selectedItemName).displayStack(),
            chipX + 3.0F * settingRenderContext.scale,
            chipY + 2.25F * settingRenderContext.scale,
            5.5F * settingRenderContext.scale,
            ItemDrawOptions.noDecorations(AutoBuyPanel.computeFloat2())
         );
         FontText.processData3(
            FontFace.SEMI,
            selectedItemName,
            chipX + 11.5F * settingRenderContext.scale,
            chipY + 2.0F * settingRenderContext.scale,
            5.0F * settingRenderContext.scale,
            UiTheme.intVal
         );
         settingRenderContext.processData4(
            "ε",
            chipX + chipWidth - 5.5F * settingRenderContext.scale,
            chipY + 5.0F * settingRenderContext.scale,
            5.5F * settingRenderContext.scale,
            UiTheme.computeInt2(
               UiTheme.intVal3,
               UiTheme.intVal,
               settingRenderContext.computeFloat2(
                  "ab:editx",
                  inputEnabled
                     && editMode
                     && UiTheme.matchesCriteria(
                        settingRenderContext.mouseX, settingRenderContext.mouseY, chipX, chipY, chipWidth, 10.0F * settingRenderContext.scale
                     )
               )
            )
         );
         Render2D.popAlpha();
         if (inputEnabled && editMode) {
            settingRenderContext.processData7(chipX, chipY, chipWidth, 10.0F * settingRenderContext.scale, null, () -> autoBuyPanel.updateState3());
         }
      }
   }

   static void processData10(SettingRenderContext settingRenderContext, String title, String description, float x, float y, float maxWidth) {
      FontText.processData3(
         FontFace.SEMI, title, x, y, 5.5F * settingRenderContext.scale, UiTheme.intVal
      );
      float lineY = y + 8.0F * settingRenderContext.scale;

      for (String textLine : UiTheme.getList(FontFace.SEMI, description, 5.0F * settingRenderContext.scale, maxWidth)) {
         FontText.processData3(
            FontFace.SEMI, textLine, x, lineY, 5.0F * settingRenderContext.scale, UiTheme.intVal2
         );
         lineY += 6.0F * settingRenderContext.scale;
      }
   }

   static void processData11(
      SettingRenderContext settingRenderContext,
      AutoBuySettingsScreen autoBuySettingsScreen,
      AutoBuyPanel autoBuyPanel,
      float x,
      float y,
      float width,
      float height,
      boolean inputEnabled,
      float scrollDelta,
      long timeMillis
   ) {
      float tileSize = 23.0F * settingRenderContext.scale;
      float contentWidth = width - 5.0F * settingRenderContext.scale;
      float tileStep = tileSize + (contentWidth - 11.0F * tileSize) / 10.0F;
      autoBuySettingsScreen.floatValArray3 = new float[]{x, y, width, height};
      ArrayList<AutoBuyItem> items = autoBuySettingsScreen.getArrayList();
      autoBuySettingsScreen.smoothScroll.maximumOffset = Math.max(
         0.0F, (items.size() + 10) / 11 * tileStep - (contentWidth - 11.0F * tileSize) / 10.0F - height
      );
      autoBuySettingsScreen.smoothScroll.update(scrollDelta);
      if (items.isEmpty()) {
         FontText.processData3(
            FontFace.SEMI,
            "Ничего не найдено",
            x + (contentWidth - FontText.computeFloat(FontFace.SEMI, "Ничего не найдено", 5.0F * settingRenderContext.scale)) / 2.0F,
            y + height / 2.0F - 2.5F * settingRenderContext.scale,
            5.0F * settingRenderContext.scale,
            UiTheme.intVal3
         );
      }

      float[] floatArray = new float[]{x, y, contentWidth, height};
      String tooltipName = "";
      float tooltipAnchorX = 0.0F;
      float tooltipAnchorY = 0.0F;
      Render2D.beginScissor(
         settingRenderContext.drawContext,
         x - 3.0F * settingRenderContext.scale,
         y - 2.0F * settingRenderContext.scale,
         contentWidth + 6.0F * settingRenderContext.scale,
         height + 4.0F * settingRenderContext.scale
      );
      RoundedClipStack.processData(
         x - 3.0F * settingRenderContext.scale,
         y - 2.0F * settingRenderContext.scale,
         contentWidth + 6.0F * settingRenderContext.scale,
         height + 4.0F * settingRenderContext.scale,
         4.0F * settingRenderContext.scale,
         4.0F * settingRenderContext.scale,
         4.0F * settingRenderContext.scale,
         4.0F * settingRenderContext.scale
      );

      for (int index = 0; index < items.size(); index++) {
         float tileX = x + index % 11 * tileStep;
         float tileY = y + index / 11 * tileStep - autoBuySettingsScreen.smoothScroll.getPixelAlignedOffset();
         if (!(tileY + tileSize < y - 2.0F) && !(tileY > y + height + 2.0F)) {
            String itemName = items.get(index).name();
            float tileProgress = AutoBuyPanel.computeFloat(
               timeMillis - autoBuySettingsScreen.timeMillis3, Math.min(0.5F, 0.05F + index / 11 * 0.045F + index % 11 * 0.012F), 0.25F
            );
            if (!(tileProgress < 0.005F)) {
               float tileAnimY = tileY + (1.0F - tileProgress) * 3.0F * settingRenderContext.scale;
               float selectedProgress = settingRenderContext.computeFloat2("ab:sel:" + itemName, autoBuySettingsScreen.matchesCriteria(itemName));
               float hoverProgress = settingRenderContext.computeFloat2(
                  "ab:hov:" + itemName,
                  inputEnabled
                     && UiTheme.matchesCriteria(settingRenderContext.mouseX, settingRenderContext.mouseY, tileX, tileAnimY, tileSize, tileSize)
                     && UiTheme.matchesCriteria(settingRenderContext.mouseX, settingRenderContext.mouseY, x, y, contentWidth, height)
               );
               Render2D.pushAlpha(tileProgress);
               if (hoverProgress > 0.005F) {
                  Render2D.drawRect(
                     tileX,
                     tileAnimY,
                     tileSize,
                     tileSize,
                     4.0F * settingRenderContext.scale,
                     UiTheme.computeInt(ClickGuiStyle.intVal48, hoverProgress)
                  );
               }

               if (selectedProgress > 0.005F) {
                  Render2D.drawRect(
                     tileX,
                     tileAnimY,
                     tileSize,
                     tileSize,
                     4.0F * settingRenderContext.scale,
                     UiTheme.computeInt(UiTheme.intVal7, 0.14F * selectedProgress)
                  );
               }

               if (settingRenderContext.computeFloat2("ab:editc:" + itemName, itemName.equals(autoBuySettingsScreen.strVal)) > 0.005F) {
                  Render2D.drawOutline(
                     tileX,
                     tileAnimY,
                     tileSize,
                     tileSize,
                     4.0F * settingRenderContext.scale,
                     0.5F,
                     UiTheme.computeInt(
                        UiTheme.intVal7, settingRenderContext.computeFloat2("ab:editc:" + itemName, itemName.equals(autoBuySettingsScreen.strVal))
                     )
                  );
               }

               if (autoBuySettingsScreen.map3.get(itemName) != null && (float)(timeMillis - autoBuySettingsScreen.map3.get(itemName)) / 350.0F < 1.0F) {
                  Render2D.drawOutline(
                     tileX
                        - 2.5F
                           * settingRenderContext.scale
                           * (
                              (float)(timeMillis - autoBuySettingsScreen.map3.get(itemName))
                                 / 350.0F
                                 * (2.0F - (float)(timeMillis - autoBuySettingsScreen.map3.get(itemName)) / 350.0F)
                           ),
                     tileAnimY
                        - 2.5F
                           * settingRenderContext.scale
                           * (
                              (float)(timeMillis - autoBuySettingsScreen.map3.get(itemName))
                                 / 350.0F
                                 * (2.0F - (float)(timeMillis - autoBuySettingsScreen.map3.get(itemName)) / 350.0F)
                           ),
                     tileSize
                        + 5.0F
                           * settingRenderContext.scale
                           * (
                              (float)(timeMillis - autoBuySettingsScreen.map3.get(itemName))
                                 / 350.0F
                                 * (2.0F - (float)(timeMillis - autoBuySettingsScreen.map3.get(itemName)) / 350.0F)
                           ),
                     tileSize
                        + 5.0F
                           * settingRenderContext.scale
                           * (
                              (float)(timeMillis - autoBuySettingsScreen.map3.get(itemName))
                                 / 350.0F
                                 * (2.0F - (float)(timeMillis - autoBuySettingsScreen.map3.get(itemName)) / 350.0F)
                           ),
                     4.0F * settingRenderContext.scale
                        + 2.5F
                           * settingRenderContext.scale
                           * (
                              (float)(timeMillis - autoBuySettingsScreen.map3.get(itemName))
                                 / 350.0F
                                 * (2.0F - (float)(timeMillis - autoBuySettingsScreen.map3.get(itemName)) / 350.0F)
                           ),
                     0.75F,
                     UiTheme.computeInt(
                        UiTheme.intVal7,
                        (
                              1.0F
                                 - (float)(timeMillis - autoBuySettingsScreen.map3.get(itemName))
                                    / 350.0F
                                    * (2.0F - (float)(timeMillis - autoBuySettingsScreen.map3.get(itemName)) / 350.0F)
                           )
                           * 0.9F
                     )
                  );
               } else if (autoBuySettingsScreen.map3.get(itemName) != null) {
                  autoBuySettingsScreen.map3.remove(itemName);
               }

               ItemRenderCache.processData(
                  items.get(index).displayStack(),
                  tileX + tileSize / 4.0F,
                  tileAnimY + tileSize / 4.0F,
                  tileSize * 0.5F,
                  ItemDrawOptions.noDecorations(AutoBuyPanel.computeFloat2() * (0.48F + 0.52F * Math.max(selectedProgress, hoverProgress * 0.55F)))
               );
               Render2D.popAlpha();
               if (inputEnabled
                  && UiTheme.matchesCriteria(settingRenderContext.mouseX, settingRenderContext.mouseY, tileX, tileAnimY, tileSize, tileSize)
                  && UiTheme.matchesCriteria(settingRenderContext.mouseX, settingRenderContext.mouseY, x, y, contentWidth, height)) {
                  tooltipName = itemName;
                  tooltipAnchorX = tileX + tileSize / 2.0F;
                  tooltipAnchorY = tileAnimY;
               }

               if (inputEnabled) {
                  settingRenderContext.processData6(
                     tileX,
                     tileAnimY,
                     tileSize,
                     tileSize,
                     floatArray,
                     () -> autoBuySettingsScreen.processData2(itemName),
                     () -> autoBuyPanel.processData3(itemName)
                  );
               }
            }
         }
      }

      RoundedClipStack.updateState2();
      Render2D.endScissor(settingRenderContext.drawContext);
      if (!tooltipName.isEmpty()) {
         Render2D.drawRect(
            Math.max(
               x,
               Math.min(
                  x
                     + contentWidth
                     - FontText.computeFloat(FontFace.SEMI, tooltipName, 5.0F * settingRenderContext.scale)
                     - 6.0F * settingRenderContext.scale,
                  tooltipAnchorX
                     - FontText.computeFloat(FontFace.SEMI, tooltipName, 5.0F * settingRenderContext.scale) / 2.0F
                     - 3.0F * settingRenderContext.scale
               )
            ),
            tooltipAnchorY - 12.0F * settingRenderContext.scale < y
               ? tooltipAnchorY + tileSize + 3.0F * settingRenderContext.scale
               : tooltipAnchorY - 12.0F * settingRenderContext.scale,
            FontText.computeFloat(FontFace.SEMI, tooltipName, 5.0F * settingRenderContext.scale) + 6.0F * settingRenderContext.scale,
            10.0F * settingRenderContext.scale,
            3.0F * settingRenderContext.scale,
            ClickGuiStyle.intVal48
         );
         Render2D.drawOutline(
            Math.max(
               x,
               Math.min(
                  x
                     + contentWidth
                     - FontText.computeFloat(FontFace.SEMI, tooltipName, 5.0F * settingRenderContext.scale)
                     - 6.0F * settingRenderContext.scale,
                  tooltipAnchorX
                     - FontText.computeFloat(FontFace.SEMI, tooltipName, 5.0F * settingRenderContext.scale) / 2.0F
                     - 3.0F * settingRenderContext.scale
               )
            ),
            tooltipAnchorY - 12.0F * settingRenderContext.scale < y
               ? tooltipAnchorY + tileSize + 3.0F * settingRenderContext.scale
               : tooltipAnchorY - 12.0F * settingRenderContext.scale,
            FontText.computeFloat(FontFace.SEMI, tooltipName, 5.0F * settingRenderContext.scale) + 6.0F * settingRenderContext.scale,
            10.0F * settingRenderContext.scale,
            3.0F * settingRenderContext.scale,
            0.5F,
            UiTheme.intVal17
         );
         FontText.processData3(
            FontFace.SEMI,
            tooltipName,
            Math.max(
                  x,
                  Math.min(
                     x
                        + contentWidth
                        - FontText.computeFloat(FontFace.SEMI, tooltipName, 5.0F * settingRenderContext.scale)
                        - 6.0F * settingRenderContext.scale,
                     tooltipAnchorX
                        - FontText.computeFloat(FontFace.SEMI, tooltipName, 5.0F * settingRenderContext.scale) / 2.0F
                        - 3.0F * settingRenderContext.scale
                  )
               )
               + 3.0F * settingRenderContext.scale,
            (
                  tooltipAnchorY - 12.0F * settingRenderContext.scale < y
                     ? tooltipAnchorY + tileSize + 3.0F * settingRenderContext.scale
                     : tooltipAnchorY - 12.0F * settingRenderContext.scale
               )
               + 2.5F * settingRenderContext.scale,
            5.0F * settingRenderContext.scale,
            UiTheme.intVal
         );
      }

      if (autoBuySettingsScreen.smoothScroll.maximumOffset > 0.0F) {
         Render2D.drawRect(
            x + width - 1.0F * settingRenderContext.scale,
            y
               + (height - Math.max(8.0F * settingRenderContext.scale, height * (height / (height + autoBuySettingsScreen.smoothScroll.maximumOffset))))
                  * (autoBuySettingsScreen.smoothScroll.getPixelAlignedOffset() / autoBuySettingsScreen.smoothScroll.maximumOffset),
            1.0F * settingRenderContext.scale,
            Math.max(8.0F * settingRenderContext.scale, height * (height / (height + autoBuySettingsScreen.smoothScroll.maximumOffset))),
            0.5F * settingRenderContext.scale,
            UiTheme.intVal5
         );
      }
   }

   static void processData12(
      SettingRenderContext settingRenderContext,
      AutoBuySettingsScreen autoBuySettingsScreen,
      float x,
      float y,
      float width,
      float height,
      long timeMillis,
      long startTimeMillis,
      float deltaTime
   ) {
      settingRenderContext.processData9(
         x, y, width, height, 4.0F * settingRenderContext.scale, ClickGuiStyle.intVal47, UiTheme.intVal17
      );
      FontText.processData3(
         FontFace.SEMI,
         "Price Chart",
         x + 8.0F * settingRenderContext.scale,
         y + 8.0F * settingRenderContext.scale,
         5.0F * settingRenderContext.scale,
         UiTheme.intVal3
      );
      autoBuySettingsScreen.doubleVal = autoBuySettingsScreen.doubleVal + (autoBuySettingsScreen.timeMillis - autoBuySettingsScreen.doubleVal) * (1.0 - Math.exp(-deltaTime * 8.0F));
      if (Math.abs(autoBuySettingsScreen.timeMillis - autoBuySettingsScreen.doubleVal) < 1.0) {
         autoBuySettingsScreen.doubleVal = autoBuySettingsScreen.timeMillis;
      }

      FontText.processData3(
         FontFace.SEMI,
         "Total sales: $" + AutoBuySettingsScreen.formatText(Math.round(autoBuySettingsScreen.doubleVal)),
         x
            + width
            - 8.0F * settingRenderContext.scale
            - FontText.computeFloat(
               FontFace.SEMI, "Total sales: $" + AutoBuySettingsScreen.formatText(Math.round(autoBuySettingsScreen.doubleVal)), 4.5F * settingRenderContext.scale
            ),
         y + 8.5F * settingRenderContext.scale,
         4.5F * settingRenderContext.scale,
         UiTheme.intVal4
      );
      float chartTop = y + 22.0F * settingRenderContext.scale;
      float chartHeight = y + height - 8.0F * settingRenderContext.scale - chartTop;
      float chartLeft = x + 28.0F * settingRenderContext.scale;
      float chartWidth = x + width - 8.0F * settingRenderContext.scale - chartLeft;

      for (int index = 0; index < priceLabels.length; index++) {
         FontText.processData3(
            FontFace.SEMI,
            priceLabels[index],
            x
               + 20.0F * settingRenderContext.scale
               - FontText.computeFloat(FontFace.SEMI, priceLabels[index], 4.5F * settingRenderContext.scale),
            chartTop + index * (chartHeight - 5.0F * settingRenderContext.scale) / (priceLabels.length - 1),
            4.5F * settingRenderContext.scale,
            UiTheme.intVal4
         );
         Render2D.drawRect(
            chartLeft,
            chartTop + index * (chartHeight - 5.0F * settingRenderContext.scale) / (priceLabels.length - 1) + 2.25F * settingRenderContext.scale,
            chartWidth,
            0.5F * settingRenderContext.scale,
            0.25F * settingRenderContext.scale,
            UiTheme.intVal6
         );
      }

      for (int index2 = 0; index2 < autoBuySettingsScreen.floatValArray2.length; index2++) {
         float barHeight = Math.max(
            0.0F,
            settingRenderContext.computeFloat7("ab:bar:" + index2, autoBuySettingsScreen.floatValArray2[index2])
               * chartHeight
               * AutoBuyPanel.computeFloat(timeMillis - startTimeMillis, 0.18F + index2 * 0.025F, 0.3F)
         );
         if (!(barHeight < 0.5F)) {
            Render2D.drawRectCorners(
               chartLeft + index2 * (chartWidth - 13.0F * settingRenderContext.scale) / (autoBuySettingsScreen.floatValArray2.length - 1),
               chartTop + chartHeight - barHeight,
               13.0F * settingRenderContext.scale,
               barHeight,
               Math.min(2.0F * settingRenderContext.scale, barHeight / 2.0F),
               Math.min(2.0F * settingRenderContext.scale, barHeight / 2.0F),
               0.0F,
               0.0F,
               UiTheme.computeInt2(
                  UiTheme.intVal7,
                  UiTheme.intVal9,
                  settingRenderContext.computeFloat2(
                     "ab:barh:" + index2,
                     UiTheme.matchesCriteria(
                        settingRenderContext.mouseX,
                        settingRenderContext.mouseY,
                        chartLeft + index2 * (chartWidth - 13.0F * settingRenderContext.scale) / (autoBuySettingsScreen.floatValArray2.length - 1),
                        chartTop,
                        13.0F * settingRenderContext.scale,
                        chartHeight
                     )
                  )
               )
            );
         }
      }
   }
}
