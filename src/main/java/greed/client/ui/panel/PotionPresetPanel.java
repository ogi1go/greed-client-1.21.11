package greed.client.ui.panel;

import greed.client.ui.AnimatedValue;
import greed.client.ui.ClickGuiStyle;
import greed.client.render.CornerRadii;
import greed.client.render.FontText;
import greed.client.render.ItemRenderCache;
import greed.client.util.PotionPreset;
import greed.client.render.RoundedClipStack;
import greed.client.ui.setting.SettingRenderContext;
import greed.client.render.ShapeDrawing;
import greed.client.ui.UiTheme;

import greed.client.render.FontFace;
import greed.client.util.ItemDrawOptions;
import greed.client.setting.KeybindSetting;
import greed.client.render.Render2D;
import greed.client.module.player.ServerHelperModule;
import greed.client.setting.TextSetting;

import java.util.ArrayList;
import net.minecraft.item.ItemStack;

public final class PotionPresetPanel {
   static final String logoTexture = "assets/greed/textures/gui/presets/logo.png";
   static final String chipTexture = "assets/greed/textures/gui/presets/chip.png";
   static final String searchTexture = "assets/greed/textures/gui/presets/search.png";
   static final String keyboardDimTexture = "assets/greed/textures/gui/presets/keyboard_dim.png";
   static final String keyboardOnTexture = "assets/greed/textures/gui/presets/keyboard_on.png";
   static final String toggleTexture = "assets/greed/textures/gui/presets/toggle.png";
   static final String trashTexture = "assets/greed/textures/gui/presets/trash.png";
   static final String chipSmallTexture = "assets/greed/textures/gui/presets/chip_small.png";
   static final String potionAddTexture = "assets/greed/textures/gui/presets/potion_add.png";
   static final String searchSmallTexture = "assets/greed/textures/gui/presets/search_small.png";
   private static PotionPresetPanel potionPresetPanel;
   private AnimatedValue animatedValue = new AnimatedValue(300L).getAnimatedValue(0.0);
   private boolean visible;
   private long openTimeMillis;

   private void renderPanel(SettingRenderContext settingRenderContext, float panelWidth, float panelHeight, boolean interactive, float panelScrollDelta, float verticalOffset) {
      PotionPreset potionPreset = PotionPreset.getInstance();
      float panelX = (panelWidth - 263.0F * settingRenderContext.scale) / 2.0F;
      float panelY = (panelHeight - 133.33F * settingRenderContext.scale) / 2.0F + verticalOffset;
      long nowMillis = System.currentTimeMillis();
      processData2(
         panelX, panelY, 166.0F * settingRenderContext.scale, 133.33F * settingRenderContext.scale, 5.7F * settingRenderContext.scale, settingRenderContext.scale
      );
      processData2(
         panelX + 172.0F * settingRenderContext.scale,
         panelY,
         91.0F * settingRenderContext.scale,
         133.33F * settingRenderContext.scale,
         5.7F * settingRenderContext.scale,
         settingRenderContext.scale
      );
      if (interactive) {
         settingRenderContext.processData7(panelX, panelY, 166.0F * settingRenderContext.scale, 133.33F * settingRenderContext.scale, null, () -> {});
      }

      if (interactive) {
         settingRenderContext.processData7(
            panelX + 172.0F * settingRenderContext.scale, panelY, 91.0F * settingRenderContext.scale, 133.33F * settingRenderContext.scale, null, () -> {}
         );
      }

      processData3(
         settingRenderContext,
         panelX,
         panelY,
         166.0F * settingRenderContext.scale,
         "Создание пресета",
         false,
         "Potion Combiner",
         "Выбрасывает указанные зелья из инвентаря по нажатию указанной вами клавиши",
         136.0F,
         151.33F
      );
      processData3(
         settingRenderContext,
         panelX + 172.0F * settingRenderContext.scale,
         panelY,
         91.0F * settingRenderContext.scale,
         "Настройки пресетов",
         true,
         "Ваши пресеты",
         "Созданные вами ранее пресеты системы Potion Combiner",
         80.0F,
         75.0F
      );
      FontText.processData3(
         FontFace.SEMI,
         "Имя пресета",
         panelX + 6.67F * settingRenderContext.scale,
         panelY + 46.67F * settingRenderContext.scale,
         4.2F * settingRenderContext.scale,
         UiTheme.intVal
      );
      FontText.processData3(
         FontFace.SEMI,
         "Уникальное имя данного пресета",
         panelX + 6.67F * settingRenderContext.scale,
         panelY + 52.33F * settingRenderContext.scale,
         4.2F * settingRenderContext.scale,
         UiTheme.computeInt(UiTheme.intVal, 0.35F)
      );
      processData5(
         settingRenderContext,
         potionPreset.textSetting,
         panelX + 6.67F * settingRenderContext.scale,
         panelY + 60.0F * settingRenderContext.scale,
         151.33F * settingRenderContext.scale,
         9.0F * settingRenderContext.scale,
         interactive,
         "pc:name"
      );
      FontText.processData3(
         FontFace.SEMI,
         "Горячая клавиша",
         panelX + 6.67F * settingRenderContext.scale,
         panelY + 74.33F * settingRenderContext.scale,
         4.2F * settingRenderContext.scale,
         UiTheme.intVal
      );
      FontText.processData3(
         FontFace.SEMI,
         "Зелье выбрасываются при нажатии бинда",
         panelX + 6.67F * settingRenderContext.scale,
         panelY + 80.0F * settingRenderContext.scale,
         4.2F * settingRenderContext.scale,
         UiTheme.computeInt(UiTheme.intVal, 0.35F)
      );
      this.renderKeybind(
         settingRenderContext,
         potionPreset.keybindSetting,
         panelX + 134.0F * settingRenderContext.scale,
         panelY + 76.0F * settingRenderContext.scale,
         30.0F,
         interactive,
         "pc:draftbind"
      );

      for (int index = 0; index < 7; index++) {
         float slotX = panelX + 6.67F * settingRenderContext.scale + index * 22.0F * settingRenderContext.scale;
         float slotY = panelY + 90.0F * settingRenderContext.scale;
         Render2D.drawRect(
            slotX,
            slotY,
            19.67F * settingRenderContext.scale,
            19.67F * settingRenderContext.scale,
            3.33F * settingRenderContext.scale,
            UiTheme.computeInt(UiTheme.intVal7, 0.05F)
         );
         Render2D.drawOutline(
            slotX,
            slotY,
            19.67F * settingRenderContext.scale,
            19.67F * settingRenderContext.scale,
            3.33F * settingRenderContext.scale,
            0.35F * settingRenderContext.scale,
            UiTheme.computeInt(
               UiTheme.intVal7,
               index == 6
                  ? 0.19F
                  : 0.06F
                     + 0.13F
                        * settingRenderContext.computeFloat2(
                           "pc:slot:" + index,
                           interactive
                              && UiTheme.matchesCriteria(
                                 settingRenderContext.mouseX,
                                 settingRenderContext.mouseY,
                                 slotX,
                                 slotY,
                                 19.67F * settingRenderContext.scale,
                                 19.67F * settingRenderContext.scale
                              )
                        )
            )
         );
         if (index < 6 && !potionPreset.itemStackArray[index].isEmpty()) {
            ItemRenderCache.processData(
               potionPreset.itemStackArray[index],
               slotX + 4.33F * settingRenderContext.scale,
               slotY + 4.17F * settingRenderContext.scale,
               11.0F * settingRenderContext.scale,
               ItemDrawOptions.noDecorations(computeFloat2())
            );
         }

         if (index < 6 && potionPreset.strValArray[index] != null) {
            settingRenderContext.processData17(
               potionPreset.strValArray[index], slotX, slotY, 19.67F * settingRenderContext.scale, 19.67F * settingRenderContext.scale
            );
         }

         int index2 = index;
         if (interactive) {
            settingRenderContext.processData7(slotX, slotY, 19.67F * settingRenderContext.scale, 19.67F * settingRenderContext.scale, null, () -> {
               if (index2 < 6 && !potionPreset.itemStackArray[index2].isEmpty()) {
                  potionPreset.itemStackArray[index2] = ItemStack.EMPTY;
                  potionPreset.strValArray[index2] = null;
               } else {
                  potionPreset.intVal = index2 < 6 ? index2 : potionPreset.computeInt();
               }
            });
         }
      }

      float saveHover = settingRenderContext.computeFloat2(
         "pc:save",
         interactive
            && UiTheme.matchesCriteria(
               settingRenderContext.mouseX,
               settingRenderContext.mouseY,
               panelX + 6.67F * settingRenderContext.scale,
               panelY + 116.33F * settingRenderContext.scale,
               151.33F * settingRenderContext.scale,
               11.0F * settingRenderContext.scale
            )
      );
      Render2D.drawRect(
         panelX + 6.67F * settingRenderContext.scale,
         panelY + 116.33F * settingRenderContext.scale,
         151.33F * settingRenderContext.scale,
         11.0F * settingRenderContext.scale,
         2.0F * settingRenderContext.scale,
         UiTheme.computeInt(UiTheme.intVal, 0.02F + 0.03F * saveHover)
      );
      Render2D.drawOutline(
         panelX + 6.67F * settingRenderContext.scale,
         panelY + 116.33F * settingRenderContext.scale,
         151.33F * settingRenderContext.scale,
         11.0F * settingRenderContext.scale,
         2.0F * settingRenderContext.scale,
         0.35F * settingRenderContext.scale,
         UiTheme.computeInt(UiTheme.intVal7, 0.06F + 0.14F * saveHover)
      );
      FontText.processData3(
         FontFace.SEMI_BOLD,
         "Сохранить пресет",
         panelX
            + 6.67F * settingRenderContext.scale
            + (
                  151.33F * settingRenderContext.scale
                     - FontText.computeFloat(FontFace.SEMI_BOLD, "Сохранить пресет", 4.2F * settingRenderContext.scale)
               )
               / 2.0F,
         panelY + 119.33F * settingRenderContext.scale,
         4.2F * settingRenderContext.scale,
         UiTheme.computeInt(UiTheme.intVal, 1.0F - 0.45F * saveHover)
      );
      if (interactive) {
         settingRenderContext.processData7(
            panelX + 6.67F * settingRenderContext.scale,
            panelY + 116.33F * settingRenderContext.scale,
            151.33F * settingRenderContext.scale,
            11.0F * settingRenderContext.scale,
            null,
            () -> potionPreset.updateState2()
         );
      }

      this.renderPresetList(settingRenderContext, potionPreset, panelX + 172.0F * settingRenderContext.scale, panelY, interactive, panelScrollDelta);
      if (potionPreset.intVal < 0 && !(settingRenderContext.computeFloat10("pc:dropdown", potionPreset.intVal >= 0) > 0.005F)) {
         potionPreset.floatValArray = null;
      } else {
         this.renderPotionList(
            settingRenderContext,
            potionPreset,
            panelX - 80.0F * settingRenderContext.scale,
            panelY + 27.33F * settingRenderContext.scale,
            interactive && potionPreset.intVal >= 0,
            panelScrollDelta,
            settingRenderContext.computeFloat10("pc:dropdown", potionPreset.intVal >= 0)
         );
      }
   }

   public static PotionPresetPanel getInstance() {
      if (potionPresetPanel == null) {
         potionPresetPanel = new PotionPresetPanel();
      }

      return potionPresetPanel;
   }

   public static void processData2(float boxX, float boxY, float boxWidth, float boxHeight, float boxCornerRadius, float boxProgress) {
      ShapeDrawing.processData4(
         boxX, boxY, boxWidth, boxHeight, computeFloat2(), CornerRadii.uniform(boxCornerRadius), ShapeDrawing.TypeOption.LOW, true
      );
      Render2D.drawRect(boxX, boxY, boxWidth, boxHeight, boxCornerRadius, ClickGuiStyle.intVal2);
      Render2D.drawOutline(
         boxX,
         boxY,
         boxWidth,
         boxHeight,
         boxCornerRadius,
         0.35F * boxProgress,
         UiTheme.computeInt(UiTheme.intVal7, 0.06F)
      );
   }

   public boolean matchesCriteria(float mouseX, float mouseY, double wheelDelta, float wheelScale) {
      if (PotionPreset.getInstance().floatValArray != null
         && PotionPreset.getInstance().intVal >= 0
         && UiTheme.matchesCriteria(
            mouseX,
            mouseY,
            PotionPreset.getInstance().floatValArray[0],
            PotionPreset.getInstance().floatValArray[1],
            PotionPreset.getInstance().floatValArray[2],
            PotionPreset.getInstance().floatValArray[3]
         )) {
         PotionPreset.getInstance().smoothScroll.targetOffset = SettingRenderContext.computeFloat(
            PotionPreset.getInstance().smoothScroll.targetOffset - (float)wheelDelta * 14.0F * wheelScale,
            0.0F,
            PotionPreset.getInstance().smoothScroll.maximumOffset
         );
      } else if (PotionPreset.getInstance().floatValArray2 != null
         && UiTheme.matchesCriteria(
            mouseX,
            mouseY,
            PotionPreset.getInstance().floatValArray2[0],
            PotionPreset.getInstance().floatValArray2[1],
            PotionPreset.getInstance().floatValArray2[2],
            PotionPreset.getInstance().floatValArray2[3]
         )) {
         PotionPreset.getInstance().smoothScroll2.targetOffset = SettingRenderContext.computeFloat(
            PotionPreset.getInstance().smoothScroll2.targetOffset - (float)wheelDelta * 14.0F * wheelScale,
            0.0F,
            PotionPreset.getInstance().smoothScroll2.maximumOffset
         );
      }

      return true;
   }

   public static void processData3(
      SettingRenderContext settingRenderContext,
      float cardX,
      float cardY,
      float cardWidth,
      String cardTitle,
      boolean shiftedLayout,
      String cardHeading,
      String cardDescription,
      float wrapWidth,
      float dividerWidth
   ) {
      FontText.processData3(
         FontFace.SEMI_BOLD,
         cardTitle,
         shiftedLayout ? cardX + 8.0F * settingRenderContext.scale : cardX + 6.67F * settingRenderContext.scale,
         cardY + 6.33F * settingRenderContext.scale,
         4.2F * settingRenderContext.scale,
         UiTheme.computeInt(UiTheme.intVal, 0.1F)
      );
      FontText.processData3(
         FontFace.SEMI,
         cardHeading,
         cardX + (shiftedLayout ? 8.0F : 6.67F) * settingRenderContext.scale,
         cardY + 15.33F * settingRenderContext.scale,
         5.33F * settingRenderContext.scale,
         UiTheme.intVal
      );
      float lineY = cardY + 23.33F * settingRenderContext.scale;

      for (String wrappedLine : UiTheme.getList(
         FontFace.SEMI, cardDescription, 4.2F * settingRenderContext.scale, wrapWidth * settingRenderContext.scale
      )) {
         FontText.processData3(
            FontFace.SEMI,
            wrappedLine,
            cardX + (shiftedLayout ? 8.0F : 6.67F) * settingRenderContext.scale,
            lineY,
            4.2F * settingRenderContext.scale,
            UiTheme.computeInt(UiTheme.intVal, 0.35F)
         );
         lineY += 5.0F * settingRenderContext.scale;
      }

      Render2D.drawRect(
         cardX + (shiftedLayout ? 8.0F : 6.67F) * settingRenderContext.scale,
         cardY + 40.0F * settingRenderContext.scale,
         dividerWidth * settingRenderContext.scale,
         0.33F * settingRenderContext.scale,
         0.0F,
         UiTheme.computeInt(UiTheme.intVal, 0.03F)
      );
   }

   public void updateState() {
      this.visible = false;
      PotionPreset.getInstance().intVal = -1;
      PotionPreset.getInstance().updateState();
   }

   private void renderKeybind(
      SettingRenderContext settingRenderContext,
      KeybindSetting newKeybindSetting,
      float bindX,
      float bindY,
      float bindWidth,
      boolean bindInteractive,
      String bindHoverId
   ) {
      boolean isEditingBind = settingRenderContext.keybindSetting == newKeybindSetting;
      Render2D.drawImageSized(
         !newKeybindSetting.isBound() && !isEditingBind
            ? "assets/greed/textures/gui/presets/keyboard_dim.png"
            : "assets/greed/textures/gui/presets/keyboard_on.png",
         bindX,
         bindY,
         3.33F * settingRenderContext.scale,
         3.33F * settingRenderContext.scale,
         0.0F,
         UiTheme.computeInt(UiTheme.intVal, 1.0F)
      );
      Render2D.beginScissor(
         settingRenderContext.drawContext, bindX, bindY - 2.0F * settingRenderContext.scale, bindWidth * settingRenderContext.scale, 8.0F * settingRenderContext.scale
      );
      FontText.processData3(
         FontFace.SEMI_BOLD,
         isEditingBind ? "..." : (newKeybindSetting.isBound() ? newKeybindSetting.getKeyName().toUpperCase() : "Press key"),
         bindX + 5.67F * settingRenderContext.scale,
         bindY - 0.33F * settingRenderContext.scale,
         4.0F * settingRenderContext.scale,
         !isEditingBind && !newKeybindSetting.isBound()
            ? UiTheme.computeInt(
               UiTheme.intVal,
               0.2F
                  + 0.3F
                     * settingRenderContext.computeFloat2(
                        bindHoverId,
                        bindInteractive
                           && UiTheme.matchesCriteria(
                              settingRenderContext.mouseX,
                              settingRenderContext.mouseY,
                              bindX - 1.0F * settingRenderContext.scale,
                              bindY - 2.0F * settingRenderContext.scale,
                              bindWidth * settingRenderContext.scale,
                              8.0F * settingRenderContext.scale
                           )
                     )
            )
            : UiTheme.intVal7
      );
      Render2D.endScissor(settingRenderContext.drawContext);
      if (bindInteractive) {
         settingRenderContext.processData6(
            bindX - 1.0F * settingRenderContext.scale,
            bindY - 2.0F * settingRenderContext.scale,
            bindWidth * settingRenderContext.scale,
            8.0F * settingRenderContext.scale,
            null,
            () -> settingRenderContext.keybindSetting = newKeybindSetting,
            () -> newKeybindSetting.withKeyCode(-1)
         );
      }
   }

   public static void processData5(
      SettingRenderContext settingRenderContext,
      TextSetting newTextSetting,
      float inputX,
      float inputY,
      float inputWidth,
      float inputHeight,
      boolean inputInteractive,
      String inputHoverId
   ) {
      Render2D.drawRect(
         inputX, inputY, inputWidth, inputHeight, 2.0F * settingRenderContext.scale, UiTheme.computeInt(UiTheme.intVal7, 0.05F)
      );
      Render2D.drawOutline(
         inputX,
         inputY,
         inputWidth,
         inputHeight,
         2.0F * settingRenderContext.scale,
         0.35F * settingRenderContext.scale,
         UiTheme.computeInt(
            UiTheme.intVal7, 0.06F + 0.13F * settingRenderContext.computeFloat2(inputHoverId, settingRenderContext.textSetting == newTextSetting)
         )
      );
      Render2D.beginScissor(
         settingRenderContext.drawContext, inputX + 2.5F * settingRenderContext.scale, inputY, inputWidth - 5.5F * settingRenderContext.scale, inputHeight
      );
      FontText.processData3(
         FontFace.SEMI,
         (newTextSetting.getValue().isEmpty() && settingRenderContext.textSetting != newTextSetting ? newTextSetting.getPlaceholder() : newTextSetting.getValue())
            + (settingRenderContext.textSetting == newTextSetting ? SettingRenderContext.formatText() : ""),
         inputX + 3.33F * settingRenderContext.scale,
         inputY + inputHeight / 2.0F - 1.67F * settingRenderContext.scale,
         3.33F * settingRenderContext.scale,
         newTextSetting.getValue().isEmpty() && settingRenderContext.textSetting != newTextSetting
            ? UiTheme.computeInt(UiTheme.intVal, 0.24F)
            : UiTheme.intVal
      );
      Render2D.endScissor(settingRenderContext.drawContext);
      if (inputInteractive) {
         settingRenderContext.processData7(inputX, inputY, inputWidth, inputHeight, null, () -> {
            settingRenderContext.textSetting = newTextSetting;
            newTextSetting.moveToEnd(false);
         });
      }
   }

   public static void processData6(SettingRenderContext settingRenderContext, float headerWidth, float headerY) {
      Render2D.drawImageSized(
         "assets/greed/textures/gui/presets/logo.png",
         headerWidth / 2.0F
            - (8.0F + FontText.computeFloat(FontFace.BOLD, "Greed Client", 3.43F * settingRenderContext.scale) / settingRenderContext.scale)
               * settingRenderContext.scale
               / 2.0F,
         headerY - 23.0F * settingRenderContext.scale,
         5.33F * settingRenderContext.scale,
         4.33F * settingRenderContext.scale,
         0.0F,
         UiTheme.computeInt(UiTheme.intVal, 1.0F)
      );
      FontText.processData3(
         FontFace.BOLD,
         "Greed Client",
         headerWidth / 2.0F
            - (8.0F + FontText.computeFloat(FontFace.BOLD, "Greed Client", 3.43F * settingRenderContext.scale) / settingRenderContext.scale)
               * settingRenderContext.scale
               / 2.0F
            + 8.0F * settingRenderContext.scale,
         headerY - 22.7F * settingRenderContext.scale,
         3.43F * settingRenderContext.scale,
         UiTheme.intVal7
      );
      FontText.processData3(
         FontFace.SEMI,
         "Beta 1.21.11",
         headerWidth / 2.0F
            - (8.0F + FontText.computeFloat(FontFace.BOLD, "Greed Client", 3.43F * settingRenderContext.scale) / settingRenderContext.scale)
               * settingRenderContext.scale
               / 2.0F
            + 9.0F * settingRenderContext.scale,
         headerY - 17.3F * settingRenderContext.scale,
         3.43F * settingRenderContext.scale,
         UiTheme.computeInt(UiTheme.intVal7, 0.4F)
      );
   }

   private PotionPresetPanel() {
   }

   public void updateState2() {
      if (!this.visible) {
         this.visible = true;
         this.openTimeMillis = System.currentTimeMillis();
      }
   }

   public float computeFloat() {
      this.animatedValue.processData(this.visible ? 1.0 : 0.0);
      return (float)this.animatedValue.computeDouble();
   }

   public void processData7(SettingRenderContext settingRenderContext, float layoutWidth, float layoutHeight, float openProgress, float frameScrollDelta) {
      if (!(openProgress < 0.005F)) {
         settingRenderContext.intVal = settingRenderContext.list.size();
         Render2D.drawRect(0.0F, 0.0F, layoutWidth, layoutHeight, 0.0F, UiTheme.computeInt(ClickGuiStyle.intVal41, openProgress));
         if (this.visible && openProgress > 0.6F) {
            settingRenderContext.processData7(0.0F, 0.0F, layoutWidth, layoutHeight, null, () -> this.updateState());
         }

         Render2D.pushAlpha(openProgress);
         this.renderPanel(
            settingRenderContext, layoutWidth, layoutHeight, this.visible && openProgress > 0.6F, frameScrollDelta, (1.0F - openProgress) * 12.0F * settingRenderContext.scale
         );
         Render2D.popAlpha();
      }
   }

   public static float computeFloat2() {
      return (Render2D.applyAlpha(-16777216) >>> 24 & 0xFF) / 255.0F;
   }

   public boolean checkState() {
      return this.visible;
   }

   private void renderPotionList(
      SettingRenderContext settingRenderContext,
      PotionPreset potionPreset,
      float dropdownX,
      float dropdownY,
      boolean dropdownInteractive,
      float dropdownScrollDelta,
      float dropdownProgress
   ) {
      dropdownInteractive = dropdownInteractive && dropdownProgress > 0.6F;
      dropdownX -= (1.0F - dropdownProgress) * 8.0F * settingRenderContext.scale;
      Render2D.pushAlpha(dropdownProgress);
      if (dropdownInteractive) {
         settingRenderContext.processData7(0.0F, 0.0F, 99999.0F, 99999.0F, null, () -> potionPreset.intVal = -1);
      }

      float dropdownWidth = 74.0F * settingRenderContext.scale;
      float dropdownHeight = 78.67F * settingRenderContext.scale;
      processData2(dropdownX, dropdownY, dropdownWidth, dropdownHeight, 3.0F * settingRenderContext.scale, settingRenderContext.scale);
      Render2D.drawRect(
         dropdownX, dropdownY, dropdownWidth, dropdownHeight, 3.0F * settingRenderContext.scale, UiTheme.computeInt(UiTheme.intVal7, 0.05F)
      );
      if (dropdownInteractive) {
         settingRenderContext.processData7(dropdownX, dropdownY, dropdownWidth, dropdownHeight, null, () -> {});
      }

      FontText.processData3(
         FontFace.SEMI_BOLD,
         "Ваши зелья",
         dropdownX + 5.5F * settingRenderContext.scale,
         dropdownY + 5.0F * settingRenderContext.scale,
         2.8F * settingRenderContext.scale,
         UiTheme.computeInt(UiTheme.intVal, 0.1F)
      );
      FontText.processData3(
         FontFace.SEMI,
         "Текущие зелья",
         dropdownX + 5.5F * settingRenderContext.scale,
         dropdownY + 10.0F * settingRenderContext.scale,
         4.2F * settingRenderContext.scale,
         UiTheme.intVal
      );
      processData5(
         settingRenderContext,
         potionPreset.textSetting2,
         dropdownX + 5.5F * settingRenderContext.scale,
         dropdownY + 18.33F * settingRenderContext.scale,
         63.0F * settingRenderContext.scale,
         9.0F * settingRenderContext.scale,
         dropdownInteractive,
         "pc:psearch"
      );
      Render2D.drawRect(
         dropdownX + 5.5F * settingRenderContext.scale,
         dropdownY + 32.33F * settingRenderContext.scale,
         63.0F * settingRenderContext.scale,
         0.33F * settingRenderContext.scale,
         0.0F,
         UiTheme.computeInt(UiTheme.intVal, 0.03F)
      );
      ArrayList<ServerHelperModule.SubContext> potionEntries = potionPreset.getArrayList();
      float potionListTop = dropdownY + 36.33F * settingRenderContext.scale;
      float potionListHeight = dropdownHeight - 40.0F * settingRenderContext.scale;
      potionPreset.floatValArray = new float[]{dropdownX, dropdownY, dropdownWidth, dropdownHeight};
      potionPreset.smoothScroll.maximumOffset = Math.max(0.0F, potionEntries.size() * 7.33F * settingRenderContext.scale - potionListHeight);
      potionPreset.smoothScroll.update(dropdownScrollDelta);
      if (potionEntries.isEmpty()) {
         FontText.processData3(
            FontFace.SEMI,
            "Зелий нет",
            dropdownX + 12.0F * settingRenderContext.scale,
            potionListTop + 2.0F * settingRenderContext.scale,
            3.67F * settingRenderContext.scale,
            UiTheme.computeInt(UiTheme.intVal, 0.2F)
         );
      }

      Render2D.beginScissor(
         settingRenderContext.drawContext, dropdownX + 2.0F * settingRenderContext.scale, potionListTop, dropdownWidth - 4.0F * settingRenderContext.scale, potionListHeight
      );
      float[] floatArray = new float[]{dropdownX + 2.0F * settingRenderContext.scale, potionListTop, dropdownWidth - 4.0F * settingRenderContext.scale, potionListHeight};

      for (int index = 0; index < potionEntries.size(); index++) {
         float entryY = potionListTop + index * 7.33F * settingRenderContext.scale - potionPreset.smoothScroll.getPixelAlignedOffset();
         if (!(entryY + 6.0F * settingRenderContext.scale < potionListTop - 2.0F) && !(entryY > potionListTop + potionListHeight + 2.0F)) {
            ServerHelperModule.SubContext potionEntry = potionEntries.get(index);
            float entryHover = settingRenderContext.computeFloat2(
               "pc:drow:" + index,
               dropdownInteractive
                  && UiTheme.matchesCriteria(
                     settingRenderContext.mouseX,
                     settingRenderContext.mouseY,
                     dropdownX + 3.5F * settingRenderContext.scale,
                     entryY - 1.0F * settingRenderContext.scale,
                     dropdownWidth - 9.5F * settingRenderContext.scale,
                     7.33F * settingRenderContext.scale
                  )
                  && UiTheme.matchesCriteria(settingRenderContext.mouseX, settingRenderContext.mouseY, dropdownX, potionListTop, dropdownWidth, potionListHeight)
            );
            if (entryHover > 0.005F) {
               Render2D.drawRect(
                  dropdownX + 3.5F * settingRenderContext.scale,
                  entryY - 1.0F * settingRenderContext.scale,
                  dropdownWidth - 9.5F * settingRenderContext.scale,
                  7.0F * settingRenderContext.scale,
                  2.0F * settingRenderContext.scale,
                  UiTheme.computeInt(UiTheme.intVal7, 0.08F * entryHover)
               );
            }

            ItemRenderCache.processData(
               PotionPreset.getItemStack(potionEntry.getKeybindSetting().name),
               dropdownX + 5.5F * settingRenderContext.scale,
               entryY,
               4.67F * settingRenderContext.scale,
               ItemDrawOptions.noDecorations(computeFloat2())
            );
            Render2D.beginScissor(
               settingRenderContext.drawContext,
               dropdownX + 12.5F * settingRenderContext.scale,
               entryY - 1.0F * settingRenderContext.scale,
               dropdownWidth - 19.5F * settingRenderContext.scale,
               7.33F * settingRenderContext.scale
            );
            float var10002 = dropdownX + 12.5F * settingRenderContext.scale;
            float var10003 = entryY + 0.33F * settingRenderContext.scale;
            float var10004 = 3.67F * settingRenderContext.scale;
            FontText.processData3(
               FontFace.SEMI, potionEntry.getKeybindSetting().name, var10002, var10003, var10004, UiTheme.intVal
            );
            Render2D.endScissor(settingRenderContext.drawContext);
            if (dropdownInteractive) {
               settingRenderContext.processData7(
                  dropdownX + 3.5F * settingRenderContext.scale,
                  entryY - 1.0F * settingRenderContext.scale,
                  dropdownWidth - 9.5F * settingRenderContext.scale,
                  7.33F * settingRenderContext.scale,
                  floatArray,
                  () -> potionPreset.processData(
                     potionPreset.intVal < 0 ? potionPreset.computeInt() : potionPreset.intVal, potionEntry.getKeybindSetting().name
                  )
               );
            }
         }
      }

      Render2D.endScissor(settingRenderContext.drawContext);
      if (potionPreset.smoothScroll.maximumOffset > 0.0F) {
         Render2D.drawRect(
            dropdownX + 67.67F * settingRenderContext.scale,
            potionListTop + (potionListHeight - 11.0F * settingRenderContext.scale) * (potionPreset.smoothScroll.getPixelAlignedOffset() / potionPreset.smoothScroll.maximumOffset),
            0.67F * settingRenderContext.scale,
            11.0F * settingRenderContext.scale,
            3.0F * settingRenderContext.scale,
            UiTheme.computeInt(UiTheme.intVal7, 0.35F)
         );
      }

      Render2D.popAlpha();
   }

   public boolean checkState2() {
      if (PotionPreset.getInstance().intVal >= 0) {
         PotionPreset.getInstance().intVal = -1;
         return true;
      }

      if (!this.visible) {
         return false;
      }

      this.visible = false;
      PotionPreset.getInstance().updateState();
      return true;
   }

   private void renderPresetList(
      SettingRenderContext settingRenderContext, PotionPreset potionPreset, float presetListX, float presetListY, boolean presetInteractive, float presetScrollDelta
   ) {
      float presetListTop = presetListY + 45.0F * settingRenderContext.scale;
      float presetListHeight = 133.33F * settingRenderContext.scale - 47.0F * settingRenderContext.scale;
      potionPreset.floatValArray2 = new float[]{presetListX, presetListTop, 91.0F * settingRenderContext.scale, presetListHeight};
      potionPreset.smoothScroll2.maximumOffset = Math.max(0.0F, potionPreset.list.size() * 16.0F * settingRenderContext.scale - presetListHeight);
      potionPreset.smoothScroll2.update(presetScrollDelta);
      if (potionPreset.list.isEmpty()) {
         FontText.processData3(
            FontFace.SEMI,
            "Пресетов пока нет",
            presetListX
               + (91.0F * settingRenderContext.scale - FontText.computeFloat(FontFace.SEMI, "Пресетов пока нет", 4.0F * settingRenderContext.scale))
                  / 2.0F,
            presetListTop + 8.0F * settingRenderContext.scale,
            4.0F * settingRenderContext.scale,
            UiTheme.computeInt(UiTheme.intVal, 0.2F)
         );
      }

      Render2D.beginScissor(
         settingRenderContext.drawContext, presetListX + 2.0F * settingRenderContext.scale, presetListTop, 89.0F * settingRenderContext.scale, presetListHeight
      );
      RoundedClipStack.processData(
         presetListX + 2.0F * settingRenderContext.scale,
         presetListTop,
         89.0F * settingRenderContext.scale,
         presetListHeight,
         2.0F * settingRenderContext.scale,
         2.0F * settingRenderContext.scale,
         2.0F * settingRenderContext.scale,
         2.0F * settingRenderContext.scale
      );
      float[] floatArray = new float[]{presetListX + 2.0F * settingRenderContext.scale, presetListTop, 89.0F * settingRenderContext.scale, presetListHeight};

      for (int index = 0; index < potionPreset.list.size(); index++) {
         PotionPreset.SubContext presetEntry = potionPreset.list.get(index);
         float presetEntryY = presetListTop + index * 16.0F * settingRenderContext.scale - potionPreset.smoothScroll2.getPixelAlignedOffset();
         if (!(presetEntryY + 15.5F * settingRenderContext.scale < presetListTop - 2.0F) && !(presetEntryY > presetListTop + presetListHeight + 2.0F)) {
            FontText.processData3(
               FontFace.SEMI,
               presetEntry.strVal,
               presetListX + 8.0F * settingRenderContext.scale,
               presetEntryY,
               3.8F * settingRenderContext.scale,
               UiTheme.computeInt(UiTheme.intVal, presetEntry.flag ? 1.0F : 0.5F)
            );
            float presetRowY = presetEntryY + 5.5F * settingRenderContext.scale;
            float iconSpacing = presetEntry.list2.size() <= 1 ? 8.5F : Math.min(8.5F, 29.0F / (Math.min(5, presetEntry.list2.size()) - 1));

            for (int index2 = 0; index2 < Math.min(5, presetEntry.list2.size()); index2++) {
               ItemRenderCache.processData(
                  presetEntry.list2.get(index2).getItemStack2(),
                  presetListX + 8.0F * settingRenderContext.scale + index2 * iconSpacing * settingRenderContext.scale,
                  presetRowY + 0.83F * settingRenderContext.scale,
                  7.0F * settingRenderContext.scale,
                  ItemDrawOptions.noDecorations(computeFloat2() * (presetEntry.flag ? 1.0F : 0.4F))
               );
            }

            this.renderKeybind(
               settingRenderContext,
               presetEntry.keybindSetting,
               presetListX + (18.0F + (Math.min(5, presetEntry.list2.size()) - 1) * iconSpacing) * settingRenderContext.scale,
               presetRowY + 2.0F * settingRenderContext.scale,
               46.0F - (Math.min(5, presetEntry.list2.size()) - 1) * iconSpacing,
               presetInteractive,
               "pc:prb:" + index
            );
            float toggleX = presetListX + 64.67F * settingRenderContext.scale;
            float deleteX = presetListX + 74.67F * settingRenderContext.scale;
            Render2D.drawRect(
               toggleX,
               presetRowY,
               8.67F * settingRenderContext.scale,
               8.67F * settingRenderContext.scale,
               2.0F * settingRenderContext.scale,
               UiTheme.computeInt(UiTheme.intVal7, 0.05F)
            );
            Render2D.drawOutline(
               toggleX,
               presetRowY,
               8.67F * settingRenderContext.scale,
               8.67F * settingRenderContext.scale,
               2.0F * settingRenderContext.scale,
               0.35F * settingRenderContext.scale,
               UiTheme.computeInt(
                  UiTheme.intVal7,
                  0.06F
                     + 0.13F
                        * settingRenderContext.computeFloat2(
                           "pc:tgh:" + index,
                           presetInteractive
                              && UiTheme.matchesCriteria(
                                 settingRenderContext.mouseX,
                                 settingRenderContext.mouseY,
                                 toggleX,
                                 presetRowY,
                                 8.67F * settingRenderContext.scale,
                                 8.67F * settingRenderContext.scale
                              )
                        )
               )
            );
            settingRenderContext.processData4(
               presetEntry.flag ? "O" : "N",
               toggleX + 4.33F * settingRenderContext.scale,
               presetRowY + 4.33F * settingRenderContext.scale,
               4.5F * settingRenderContext.scale,
               presetEntry.flag
                  ? UiTheme.intVal7
                  : UiTheme.computeInt2(
                     UiTheme.intVal3,
                     UiTheme.intVal,
                     settingRenderContext.computeFloat2(
                        "pc:tgh:" + index,
                        presetInteractive
                           && UiTheme.matchesCriteria(
                              settingRenderContext.mouseX,
                              settingRenderContext.mouseY,
                              toggleX,
                              presetRowY,
                              8.67F * settingRenderContext.scale,
                              8.67F * settingRenderContext.scale
                           )
                     )
                  )
            );
            Render2D.drawRect(
               deleteX,
               presetRowY,
               8.67F * settingRenderContext.scale,
               8.67F * settingRenderContext.scale,
               2.0F * settingRenderContext.scale,
               UiTheme.computeInt(UiTheme.intVal7, 0.05F)
            );
            Render2D.drawOutline(
               deleteX,
               presetRowY,
               8.67F * settingRenderContext.scale,
               8.67F * settingRenderContext.scale,
               2.0F * settingRenderContext.scale,
               0.35F * settingRenderContext.scale,
               UiTheme.computeInt(
                  UiTheme.intVal7,
                  0.06F
                     + 0.13F
                        * settingRenderContext.computeFloat2(
                           "pc:trh:" + index,
                           presetInteractive
                              && UiTheme.matchesCriteria(
                                 settingRenderContext.mouseX,
                                 settingRenderContext.mouseY,
                                 deleteX,
                                 presetRowY,
                                 8.67F * settingRenderContext.scale,
                                 8.67F * settingRenderContext.scale
                              )
                        )
               )
            );
            settingRenderContext.processData4(
               "P",
               deleteX + 4.33F * settingRenderContext.scale,
               presetRowY + 4.33F * settingRenderContext.scale,
               4.5F * settingRenderContext.scale,
               UiTheme.computeInt2(
                  UiTheme.intVal3,
                  UiTheme.intVal,
                  settingRenderContext.computeFloat2(
                     "pc:trh:" + index,
                     presetInteractive
                        && UiTheme.matchesCriteria(
                           settingRenderContext.mouseX,
                           settingRenderContext.mouseY,
                           deleteX,
                           presetRowY,
                           8.67F * settingRenderContext.scale,
                           8.67F * settingRenderContext.scale
                        )
                  )
               )
            );
            int index3 = index;
            if (presetInteractive) {
               settingRenderContext.processData7(toggleX, presetRowY, 8.67F * settingRenderContext.scale, 8.67F * settingRenderContext.scale, floatArray, () -> {
                  presetEntry.flag = !presetEntry.flag;
                  potionPreset.updateState();
               });
            }

            if (presetInteractive) {
               settingRenderContext.processData7(deleteX, presetRowY, 8.67F * settingRenderContext.scale, 8.67F * settingRenderContext.scale, floatArray, () -> {
                  potionPreset.list.remove(index3);
                  potionPreset.updateState();
               });
            }
         }
      }

      RoundedClipStack.updateState2();
      Render2D.endScissor(settingRenderContext.drawContext);
   }
}
