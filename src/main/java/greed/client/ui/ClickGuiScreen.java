package greed.client.ui;

import greed.client.util.Colors;
import greed.client.render.FontFace;
import greed.client.util.InputBinding;
import greed.client.module.render.InterfaceModule;
import greed.client.module.ModuleCategory;
import greed.client.util.GuiScale;
import greed.client.render.FontText;
import greed.client.ui.ScreenClosingTransition;
import greed.client.ui.UiTheme;
import greed.client.ui.ClickGuiStyle;
import greed.client.ui.panel.ModuleCategoryPanel;
import greed.client.ui.setting.SettingRenderContext;
import greed.client.render.GuiEffectsState;
import greed.client.ui.SmoothScroll;
import greed.client.render.Render2D;
import greed.client.setting.TextSetting;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class ClickGuiScreen extends Screen implements ScreenClosingTransition.SubInterface {
   private final SettingRenderContext settingRenderContext = new SettingRenderContext();
   private final ModuleCategory[] moduleCategoryArray = ModuleCategory.values();
   private final SmoothScroll[] scrollStates;
   private final float[][] categoryBounds;
   private final float[][] tabBounds;

   private float openProgress;
   private float openProgressStart;
   private long openTransitionStartMillis;
   private boolean wasVisible;
   private boolean closing;
   private boolean closeScheduled;
   private boolean blurPending;
   private long lastFrameTimeMillis = System.nanoTime() / 1000000L;
   private boolean searchFocused;
   private final TextSetting textSetting = new TextSetting("Поиск", "").withLengthRange(0, 28);
   private long lastSearchInputMillis;
   private ModuleCategory selectedCategory = ModuleCategory.COMBAT;
   private static ClickGuiScreen clickGuiScreen;

   @Override
   public void close() {
      if (!this.closing) {
         this.closing = true;
         this.settingRenderContext.textSetting = null;
         this.settingRenderContext.nested0492 = null;
         this.settingRenderContext.keybindSetting = null;
         this.settingRenderContext.object = null;
         this.settingRenderContext.module = null;
         this.blurPending = true;
         ScreenClosingTransition.processData(this);
         if (MinecraftClient.getInstance().currentScreen == this) {
            MinecraftClient.getInstance().setScreen(null);
         }
      }
   }

   private void drawHeader(float panelX, float panelY, float panelWidth) {
      float scale = this.settingRenderContext.scale;
      float padding = 8.0F * scale;

      float titleY = panelY + 8.0F * scale;
      FontText.processData3(FontFace.SF, selectedCategory.name(), panelX + padding, titleY, 10.0F * scale, ClickGuiStyle.intVal);
      FontText.processData3(
              FontFace.SF,
              "Исследуйте материалы, эффекты и рендер.",
              panelX + padding,
              titleY + 12.0F * scale,
              6.0F * scale,
              ClickGuiStyle.intVal7
      );

      float searchWidth = 150.0F * scale;
      float searchHeight = 20.0F * scale;
      drawSearchBar(panelX + panelWidth - padding - searchWidth, titleY, searchWidth, searchHeight);

      float iconSize = 7.0F * scale;
      float fontSize = 5.0F * scale;
      float iconGap = 3.0F * scale;
      float padX = 6.0F * scale;
      float tabHeight = 16.0F * scale;
      float tabY = panelY + 29.0F * scale;
      float tabsX = panelX + padding;
      float tabsWidth = panelWidth - 2.0F * padding;
      float spacing = 4.0F * scale;

      float[] widths = new float[moduleCategoryArray.length];
      float totalWidth = spacing * (moduleCategoryArray.length - 1);
      for (int i = 0; i < moduleCategoryArray.length; i++) {
         float labelWidth = FontText.computeFloat(FontFace.SF, moduleCategoryArray[i].name(), fontSize);
         widths[i] = iconSize + iconGap + labelWidth + 2.0F * padX;
         totalWidth += widths[i];
      }

      float cursor = tabsX + (tabsWidth - totalWidth) / 2.0F;
      for (int i = 0; i < moduleCategoryArray.length; i++) {
         ModuleCategory category = moduleCategoryArray[i];
         boolean selected = (category == this.selectedCategory);
         float tabX = cursor;
         float tabWidth = widths[i];

         this.tabBounds[i][0] = tabX;
         this.tabBounds[i][1] = tabY;
         this.tabBounds[i][2] = tabWidth;
         this.tabBounds[i][3] = tabHeight;

         int iconColor = selected ? ClickGuiStyle.intVal : ClickGuiStyle.intVal7;
         if (selected) {
            Render2D.drawRect(tabX, tabY, tabWidth, tabHeight, tabHeight / 2.0F, ClickGuiStyle.intVal5);
         }

         Render2D.drawImageSized(
                 ClickGuiStyle.formatText(category),
                 tabX + padX,
                 tabY + (tabHeight - iconSize) / 2.0F,
                 iconSize,
                 iconSize,
                 0.0F,
                 iconColor
         );

         FontText.processData3(
                 FontFace.SF,
                 category.name(),
                 tabX + padX + iconSize + iconGap,
                 tabY + (tabHeight - fontSize) / 2.0F,
                 fontSize,
                 iconColor
         );

         cursor += tabWidth + spacing;
      }
   }

   private void drawSearchBar(float barX, float barY, float barWidth, float barHeight) {
      float scale = this.settingRenderContext.scale;
      Render2D.drawRect(barX, barY, barWidth, barHeight, barHeight / 2.0F, ClickGuiStyle.intVal2);
      Render2D.drawOutline(barX, barY, barWidth, barHeight, barHeight / 2.0F, 0.5F, ClickGuiStyle.intVal3);

      Render2D.drawImageSized(
              "assets/greed/textures/gui/clickgui/search.png",
              barX + 10.0F * scale,
              barY + barHeight / 2.0F - 3.0F * scale,
              6.0F * scale,
              6.0F * scale,
              0.0F,
              ClickGuiStyle.intVal4
      );

      String searchText = this.textSetting.getText();
      String displayText = searchText.isEmpty() ? (this.searchFocused ? "" : "Поиск...") : searchText;
      float textX = barX + 22.0F * scale;
      float textY = barY + (barHeight - 6.0F * scale) / 2.0F;
      float textFieldWidth = barWidth - 30.0F * scale;

      Render2D.beginScissor(this.settingRenderContext.drawContext, textX - 1.0F * scale, barY, textFieldWidth + 2.0F * scale, barHeight);
      float displayTextWidth = FontText.computeFloat(FontFace.SF, displayText, 6.0F * scale);
      float textScrollOffset = Math.max(0.0F, displayTextWidth - textFieldWidth);

      if (!displayText.isEmpty()) {
         FontText.processData3(
                 FontFace.SF,
                 displayText,
                 textX - textScrollOffset,
                 textY,
                 6.0F * scale,
                 searchText.isEmpty() ? ClickGuiStyle.intVal7 : Colors.computeInt7(-1, 210)
         );
      }

      if (this.searchFocused && System.nanoTime() / 1000000L / 530L % 2L == 0L) {
         float caretX = textX
                 - textScrollOffset
                 + FontText.computeFloat(FontFace.SF, searchText.substring(0, this.textSetting.getCaret()), 6.0F * scale);
         Render2D.drawRect(
                 caretX + 0.4F * scale,
                 textY - 0.5F * scale,
                 0.6F * scale,
                 7.0F * scale,
                 0.3F * scale,
                 Colors.computeInt7(-1, 200)
         );
      }

      Render2D.endScissor(this.settingRenderContext.drawContext);
      this.settingRenderContext.processData7(barX, barY, barWidth, barHeight, null, () -> {
         this.searchFocused = true;
         this.textSetting.moveToEnd(false);
         this.lastSearchInputMillis = System.nanoTime() / 1000000L;
      });
   }

   public static ClickGuiScreen instance() {
      if (clickGuiScreen == null) {
         clickGuiScreen = new ClickGuiScreen();
      }
      return clickGuiScreen;
   }

   @Override
   public void renderBackground(DrawContext drawContext, int mouseX, int mouseY, float tickDelta) {
   }

   @Override
   public void render(DrawContext drawContext, int mouseXIn, int mouseYIn, float tickDelta) {
      float scale = getScaleFactor();
      float mouseX = mouseXIn * scale;
      float mouseY = mouseYIn * scale;
      this.settingRenderContext
              .renderElement(
                      drawContext, 1.13F * Math.min(1.0F, Math.min(this.width * scale / 640.0F, this.height * scale / 460.0F)), mouseX, mouseY
              );
      scale = Math.min(0.05F, (float)(System.nanoTime() / 1000000L - this.lastFrameTimeMillis) / 1000.0F);
      this.lastFrameTimeMillis = System.nanoTime() / 1000000L;
      InterfaceModule.getInstance().updateState();
      Render2D.flush(drawContext);
      boolean isVisible = !this.closing;
      if (isVisible != this.wasVisible) {
         this.openProgressStart = this.openProgress;
         this.openTransitionStartMillis = System.nanoTime() / 1000000L;
         this.wasVisible = isVisible;
      }

      mouseX = Math.min(1.0F, (float)(System.nanoTime() / 1000000L - this.openTransitionStartMillis) / 300.0F);
      mouseY = mouseX * mouseX * mouseX * (mouseX * (mouseX * 6.0F - 15.0F) + 10.0F);
      this.openProgress = this.openProgressStart + ((isVisible ? 1.0F : 0.0F) - this.openProgressStart) * mouseY;

      if (this.closing && this.openProgress <= 0.003F) {
         GuiEffectsState.processData(false);
         Render2D.endFrame();
         if (!this.closeScheduled) {
            this.closeScheduled = true;
            MinecraftClient.getInstance().execute(() -> {
               if (MinecraftClient.getInstance().currentScreen == this) {
                  MinecraftClient.getInstance().setScreen(null);
               }
            });
         }
      } else {
         float viewportWidth = this.width * getScaleFactor();
         float viewportHeight = this.height * getScaleFactor();
         GuiEffectsState.processData(true);
         GuiEffectsState.processData2(8.0F);
         GuiEffectsState.processData4(0.0F);
         drawContext.createNewRootLayer();
         if (this.blurPending) {
            try {
               drawContext.applyBlur();
            } catch (IllegalStateException exception) {
            }
         }

         Render2D.pushAlpha(this.openProgress);

         float panelWidth = Math.min(500.0F * this.settingRenderContext.scale, viewportWidth - 40.0F * this.settingRenderContext.scale);
         float panelHeight = Math.min(320.0F * this.settingRenderContext.scale, viewportHeight - 80.0F * this.settingRenderContext.scale);
         float panelX = (viewportWidth - panelWidth) / 2.0F;
         float panelY = (viewportHeight - panelHeight) / 2.0F + 20.0F * this.settingRenderContext.scale;

         float headerHeight = 50.0F * this.settingRenderContext.scale;

         int activeIndex = -1;
         for (int i = 0; i < moduleCategoryArray.length; i++) {
            if (moduleCategoryArray[i] == selectedCategory) {
               activeIndex = i;
               break;
            }
         }

         if (activeIndex != -1) {
            this.categoryBounds[activeIndex][0] = panelX;
            this.categoryBounds[activeIndex][1] = panelY + headerHeight;
            this.categoryBounds[activeIndex][2] = panelWidth;
            this.categoryBounds[activeIndex][3] = panelHeight - headerHeight;

            ModuleCategoryPanel.processData(
                    this.settingRenderContext,
                    selectedCategory,
                    this.scrollStates[activeIndex],
                    panelX,
                    panelY,
                    panelWidth,
                    panelHeight,
                    headerHeight,
                    scale,
                    4
            );
         }

         drawHeader(panelX, panelY, panelWidth);
         this.drawTip(viewportWidth, panelY);
         this.settingRenderContext.updateState();
         Render2D.popAlpha();
         this.updateDrag(mouseXIn * getScaleFactor(), mouseYIn * getScaleFactor());
         Render2D.endFrame();
      }
   }

   private void finishOverlay() {
      GuiEffectsState.processData(false);
      this.blurPending = false;
      this.closing = false;
      this.closeScheduled = false;
      this.openProgress = 0.0F;
      this.openProgressStart = 0.0F;
      this.wasVisible = false;
      this.openTransitionStartMillis = 0L;
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   private int searchWordHop(int direction) {
      String text = this.textSetting.getText();
      int caret = this.textSetting.getCaret();
      if (direction < 0) {
         while (caret > 0 && text.charAt(caret - 1) == ' ') {
            caret--;
         }
         while (caret > 0 && text.charAt(caret - 1) != ' ') {
            caret--;
         }
         return caret - this.textSetting.getCaret();
      } else {
         while (caret < text.length() && text.charAt(caret) != ' ') {
            caret++;
         }
         while (caret < text.length() && text.charAt(caret) == ' ') {
            caret++;
         }
         return caret - this.textSetting.getCaret();
      }
   }

   @Override
   public void removed() {
      GuiEffectsState.processData(false);
      if (!this.blurPending) {
         this.closing = false;
         this.closeScheduled = false;
         this.openProgress = 0.0F;
         this.openProgressStart = 0.0F;
         this.wasVisible = false;
         this.openTransitionStartMillis = 0L;
      }

      this.settingRenderContext.textSetting = null;
      this.settingRenderContext.nested0492 = null;
      this.settingRenderContext.keybindSetting = null;
      this.settingRenderContext.object = null;
      this.settingRenderContext.module = null;
      this.searchFocused = false;
      this.textSetting.setValue("");
      this.settingRenderContext.strVal = "";
      super.removed();
   }

   @Override
   public boolean charTyped(CharInput charInput) {
      if (this.settingRenderContext.nested0492 != null) {
         if (charInput.codepoint() >= 32) {
            this.settingRenderContext.nested0492.processData20((char)charInput.codepoint());
         }
         return true;
      } else if (this.settingRenderContext.textSetting != null) {
         if (charInput.codepoint() >= 32) {
            this.settingRenderContext.textSetting.insertText(String.valueOf((char)charInput.codepoint()));
         }
         return true;
      } else if (this.searchFocused) {
         if (charInput.codepoint() >= 32) {
            this.textSetting.insertText(String.valueOf((char)charInput.codepoint()));
            this.settingRenderContext.strVal = this.textSetting.getText().toLowerCase();
            this.lastSearchInputMillis = System.nanoTime() / 1000000L;
         }
         return true;
      } else {
         return super.charTyped(charInput);
      }
   }

   private static float getScaleFactor() {
      return GuiScale.computeInt() / GuiScale.computeFloat();
   }

   private void updateDrag(float mouseX, float mouseY) {
      if (this.settingRenderContext.numberSetting != null || this.settingRenderContext.colorSetting != null) {
         float mouseDown = GLFW.glfwGetMouseButton(MinecraftClient.getInstance().getWindow().getHandle(), 0) == 1 ? 1.0F : 0.0F;
         if (this.settingRenderContext.numberSetting != null) {
            if (mouseDown < 0.5F) {
               this.settingRenderContext.numberSetting = null;
            } else if (this.settingRenderContext.map7.get(this.settingRenderContext.numberSetting) != null) {
               this.settingRenderContext
                       .numberSetting
                       .setFraction(
                               SettingRenderContext.computeFloat(
                                       (mouseX - this.settingRenderContext.map7.get(this.settingRenderContext.numberSetting)[0])
                                               / this.settingRenderContext.map7.get(this.settingRenderContext.numberSetting)[1],
                                       0.0F,
                                       1.0F
                               )
                       );
            }
         }

         if (this.settingRenderContext.colorSetting != null) {
            if (mouseDown < 0.5F) {
               this.settingRenderContext.colorSetting = null;
            } else if (this.settingRenderContext.map8.get(this.settingRenderContext.colorSetting) != null) {
               float[] floatArray = this.settingRenderContext.computeFloat5(this.settingRenderContext.colorSetting);
               float[] floatArray2 = this.settingRenderContext.map8.get(this.settingRenderContext.colorSetting);
               if (this.settingRenderContext.intVal2 == 0) {
                  floatArray[1] = SettingRenderContext.computeFloat((mouseX - floatArray2[0]) / floatArray2[2], 0.0F, 1.0F);
                  floatArray[2] = SettingRenderContext.computeFloat(1.0F - (mouseY - floatArray2[1]) / floatArray2[3], 0.0F, 1.0F);
               } else {
                  floatArray[0] = SettingRenderContext.computeFloat((mouseX - floatArray2[4]) / floatArray2[6], 0.0F, 1.0F);
               }
               this.settingRenderContext.processData10(this.settingRenderContext.colorSetting, floatArray);
            }
         }
      }
   }

   @Override
   public boolean keyPressed(KeyInput keyInput) {
      if (this.closing) {
         return true;
      }

      int keyCode = keyInput.key();
      if (this.settingRenderContext.module != null) {
         this.settingRenderContext.module.withKeyCode(keyCode != 256 && keyCode != 261 && keyCode != 344 ? keyCode : -1);
         this.settingRenderContext.module = null;
         return true;
      }

      if (this.settingRenderContext.keybindSetting != null) {
         this.settingRenderContext.keybindSetting.withKeyCode(keyCode != 256 && keyCode != 261 ? keyCode : -1);
         this.settingRenderContext.keybindSetting = null;
         return true;
      }

      if (this.settingRenderContext.nested0492 != null) {
         if (keyCode == 256) {
            this.settingRenderContext.nested0492 = null;
         } else {
            this.settingRenderContext.nested0492.processData19(keyCode);
         }
         return true;
      } else if (this.settingRenderContext.textSetting != null) {
         if ((keyInput.modifiers() & 2) != 0 && keyCode == 65) {
            this.settingRenderContext.textSetting.selectAll();
         } else if ((keyInput.modifiers() & 2) != 0 && keyCode == 67 && this.settingRenderContext.textSetting.hasSelection()) {
            MinecraftClient.getInstance().keyboard.setClipboard(this.settingRenderContext.textSetting.getSelectedText());
         } else if ((keyInput.modifiers() & 2) != 0 && keyCode == 88 && this.settingRenderContext.textSetting.hasSelection()) {
            MinecraftClient.getInstance().keyboard.setClipboard(this.settingRenderContext.textSetting.getSelectedText());
            this.settingRenderContext.textSetting.backspace();
         } else if ((keyInput.modifiers() & 2) != 0 && keyCode == 86) {
            this.settingRenderContext.textSetting.insertText(MinecraftClient.getInstance().keyboard.getClipboard());
         } else if (keyCode == 259) {
            this.settingRenderContext.textSetting.backspace();
         } else if (keyCode == 261) {
            this.settingRenderContext.textSetting.deleteForward();
         } else if (keyCode == 263) {
            this.settingRenderContext.textSetting.moveCaret(-1, (keyInput.modifiers() & 1) != 0);
         } else if (keyCode == 262) {
            this.settingRenderContext.textSetting.moveCaret(1, (keyInput.modifiers() & 1) != 0);
         } else if (keyCode == 268) {
            this.settingRenderContext.textSetting.moveToStart((keyInput.modifiers() & 1) != 0);
         } else if (keyCode == 269) {
            this.settingRenderContext.textSetting.moveToEnd((keyInput.modifiers() & 1) != 0);
         } else if (keyCode == 257 || keyCode == 256) {
            this.settingRenderContext.textSetting = null;
         }
         return true;
      } else {
         if (this.settingRenderContext.object != null && keyCode == 256) {
            this.settingRenderContext.object = null;
            return true;
         }

         if ((keyInput.modifiers() & 2) != 0 && keyCode == 70) {
            this.searchFocused = !this.searchFocused;
            if (!this.searchFocused) {
               this.textSetting.setValue("");
               this.settingRenderContext.strVal = "";
            }
            this.lastSearchInputMillis = System.nanoTime() / 1000000L;
            return true;
         } else if (!this.searchFocused) {
            if (keyCode != 344 && keyCode != 256) {
               return super.keyPressed(keyInput);
            }
            this.close();
            return true;
         } else if (keyCode != 256 && keyCode != 257) {
            boolean ctrlDown = (keyInput.modifiers() & 2) != 0;
            boolean shiftDown = (keyInput.modifiers() & 1) != 0;
            String textBefore = this.textSetting.getText();
            if (ctrlDown && keyCode == 65) {
               this.textSetting.selectAll();
            } else if (ctrlDown && keyCode == 67 && this.textSetting.hasSelection()) {
               MinecraftClient.getInstance().keyboard.setClipboard(this.textSetting.getSelectedText());
            } else if (ctrlDown && keyCode == 88 && this.textSetting.hasSelection()) {
               MinecraftClient.getInstance().keyboard.setClipboard(this.textSetting.getSelectedText());
               this.textSetting.backspace();
            } else if (ctrlDown && keyCode == 86) {
               this.textSetting.insertText(MinecraftClient.getInstance().keyboard.getClipboard());
            } else if (keyCode == 259) {
               if (ctrlDown && !this.textSetting.hasSelection()) {
                  this.textSetting.moveCaret(this.searchWordHop(-1), true);
               }
               this.textSetting.backspace();
            } else if (keyCode == 261) {
               if (ctrlDown && !this.textSetting.hasSelection()) {
                  this.textSetting.moveCaret(this.searchWordHop(1), true);
               }
               this.textSetting.deleteForward();
            } else if (keyCode == 263) {
               this.textSetting.moveCaret(ctrlDown ? this.searchWordHop(-1) : -1, shiftDown);
            } else if (keyCode == 262) {
               this.textSetting.moveCaret(ctrlDown ? this.searchWordHop(1) : 1, shiftDown);
            } else if (keyCode == 268) {
               this.textSetting.moveToStart(shiftDown);
            } else if (keyCode == 269) {
               this.textSetting.moveToEnd(shiftDown);
            }

            if (!textBefore.equals(this.textSetting.getText())) {
               this.settingRenderContext.strVal = this.textSetting.getText().toLowerCase();
               this.lastSearchInputMillis = System.nanoTime() / 1000000L;
            }
            return true;
         } else {
            this.searchFocused = false;
            this.textSetting.setValue("");
            this.settingRenderContext.strVal = "";
            return true;
         }
      }
   }

   private void drawTip(float viewportWidth, float anchorY) {
      if (this.settingRenderContext.strVal2 != null) {
         this.settingRenderContext.strVal3 = this.settingRenderContext.strVal2;
      }

      float tipProgress = this.settingRenderContext.computeFloat7("tip.show", this.settingRenderContext.strVal2 != null ? 1.0F : 0.0F);
      if (this.settingRenderContext.strVal2 != null) {
         tipProgress = 1.0F;
      }

      if (!(tipProgress < 0.01F) && !this.settingRenderContext.strVal3.isEmpty()) {
         float scale = this.settingRenderContext.scale;
         List<String> lines = UiTheme.getList(FontFace.SF, this.settingRenderContext.strVal3, 6.0F * scale, 240.0F * scale);
         float blockHeight = lines.size() * 6.95F * scale;
         float lineY = Math.max(5.0F * scale, anchorY - 29.6F * scale + (13.9F * scale - blockHeight) / 2.0F);
         Render2D.pushAlpha(tipProgress);

         for (String line : lines) {
            FontText.processData3(
                    FontFace.SF,
                    line,
                    (viewportWidth - FontText.computeFloat(FontFace.SF, line, 6.0F * scale)) / 2.0F,
                    lineY,
                    6.0F * scale,
                    ClickGuiStyle.intVal40
            );
            lineY += 6.95F * scale;
         }

         Render2D.popAlpha();
      }
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (this.closing) {
         return true;
      }

      float scale = getScaleFactor();
      float scaledX = (float)mouseX * scale;
      float scaledY = (float)mouseY * scale;

      for (int index = 0; index < this.moduleCategoryArray.length; index++) {
         if (this.moduleCategoryArray[index] != this.selectedCategory) {
            continue;
         }

         if (UiTheme.matchesCriteria(
                 scaledX,
                 scaledY,
                 this.categoryBounds[index][0],
                 this.categoryBounds[index][1],
                 this.categoryBounds[index][2],
                 this.categoryBounds[index][3]
         )) {
            this.scrollStates[index].targetOffset = SettingRenderContext.computeFloat(
                    this.scrollStates[index].targetOffset - (float)verticalAmount * 24.0F * this.settingRenderContext.scale,
                    0.0F,
                    this.scrollStates[index].maximumOffset
            );
            return true;
         }
      }

      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   private ClickGuiScreen() {
      super(Text.literal("Greed ClickGUI"));
      this.scrollStates = new SmoothScroll[this.moduleCategoryArray.length];
      this.categoryBounds = new float[this.moduleCategoryArray.length][4];
      this.tabBounds = new float[this.moduleCategoryArray.length][4];

      for (int index = 0; index < this.moduleCategoryArray.length; index++) {
         this.scrollStates[index] = new SmoothScroll();
      }
   }

   public SettingRenderContext getCtx() {
      return this.settingRenderContext;
   }

   @Override
   protected void init() {
      super.init();
      if (this.blurPending) {
         ScreenClosingTransition.processData3(this);
         this.blurPending = false;
         this.closing = false;
         this.closeScheduled = false;
      }
   }

   @Override
   public boolean renderClosing(DrawContext drawContext) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (this.blurPending && minecraftClient.player != null) {
         this.render(drawContext, (int)minecraftClient.mouse.getScaledX(minecraftClient.getWindow()), (int)minecraftClient.mouse.getScaledY(minecraftClient.getWindow()), 0.0F);
         if (this.openProgress <= 0.003F) {
            this.finishOverlay();
            return false;
         } else {
            return true;
         }
      } else {
         this.finishOverlay();
         return false;
      }
   }

   @Override
   public boolean mouseClicked(Click click, boolean doubleClick) {
      if (this.closing) {
         return true;
      }

      float scale = getScaleFactor();
      float mouseX = (float)click.x() * scale;
      float mouseY = (float)click.y() * scale;

      for (int i = 0; i < moduleCategoryArray.length; i++) {
         if (UiTheme.matchesCriteria(mouseX, mouseY, this.tabBounds[i][0], this.tabBounds[i][1], this.tabBounds[i][2], this.tabBounds[i][3])) {
            this.selectedCategory = moduleCategoryArray[i];
            return true;
         }
      }

      if (this.settingRenderContext.keybindSetting != null) {
         if (click.button() != 0) {
            this.settingRenderContext.keybindSetting.setBinding(InputBinding.mouseButton(click.button()));
         }
         this.settingRenderContext.keybindSetting = null;
         return true;
      } else {
         if (click.button() == 0 || click.button() == 1 || click.button() == 2) {
            if (click.button() == 0) {
               this.settingRenderContext.textSetting = null;
               this.settingRenderContext.nested0492 = null;
               this.settingRenderContext.module = null;
            }

            for (int index = this.settingRenderContext.list.size() - 1; index >= this.settingRenderContext.intVal; index--) {
               SettingRenderContext.DataRecord2 actionRecord = this.settingRenderContext.list.get(index);
               if (UiTheme.matchesCriteria(mouseX, mouseY, actionRecord.x(), actionRecord.y(), actionRecord.w(), actionRecord.h())) {
                  if (click.button() == 0) {
                     actionRecord.action().run();
                     return true;
                  }

                  if (click.button() == 1 && actionRecord.rightAction() != null) {
                     actionRecord.rightAction().run();
                     return true;
                  }

                  if (click.button() == 2 && actionRecord.middleAction() != null) {
                     actionRecord.middleAction().run();
                     return true;
                  }
               }
            }

            if (this.settingRenderContext.intVal > 0) {
               return true;
            }
         }

         return super.mouseClicked(click, doubleClick);
      }
   }
}