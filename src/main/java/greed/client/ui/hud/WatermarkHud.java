package greed.client.ui.hud;

import greed.client.render.FontText;
import greed.client.util.GuiScale;
import greed.client.render.ThemeColors;
import greed.client.ui.UiTheme;

import greed.client.util.Animation;

import greed.mixin.BossBarHudMixin;
import greed.client.util.Easings;
import greed.client.render.FontFace;
import greed.client.module.render.InterfaceModule;
import ru.dreamix.protection.api.impl.profile.ProfileAPI;
import greed.client.render.Render2D;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

public final class WatermarkHud {
   private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");

   private static final float HEIGHT = 15.0F;
   private static final float LOGO_SIZE = 15.0F;
   private static final float BLOCK_GAP = 3.0F;
   private static final float PAD = 5.0F;
   private static final float ITEM_GAP = 9.0F;
   private static final float SUFFIX_GAP = 0.5F;
   private static final int ROWS = 2;

   private static final List<WatermarkHud.SubContext> subContexts = new ArrayList<>(
           List.of(
                   new WatermarkHud.SubContext("logo", null, -1),
                   new WatermarkHud.SubContext("avatar", "Аватарка", 0),
                   new WatermarkHud.SubContext("user", "Логин", 0),
                   new WatermarkHud.SubContext("fps", "ФПС", 0),
                   new WatermarkHud.SubContext("ping", "Пинг", 0),
                   new WatermarkHud.SubContext("time", "Время", 0),
                   new WatermarkHud.SubContext("coords", "Координаты", 1),
                   new WatermarkHud.SubContext("server", "Координаты", 1),
                   new WatermarkHud.SubContext("tps", "ТПС", 1)
           )
   );
   private static final Animation animator = new Animation();
   private static final Animation animator2 = new Animation();
   private static final Animation animator3 = new Animation();
   private static final float[] rowPanelWidth = new float[ROWS];
   private static final float[] rowOffsetY = new float[ROWS];
   private static float normalizedBottom;
   private static float watermarkX;
   private static float watermarkY;
   private static float watermarkWidth;
   private static float watermarkHeight = HEIGHT;
   private static float panelsX;
   private static WatermarkHud.SubContext draggedContext;
   private static float draggedOffsetX;

   public static boolean matchesCriteria(double mouseX, double mouseY, int mouseButton) {
      if (mouseButton == 0 && HudRenderContext.checkState2()) {
         float[] floatArray = HudManager.computeFloat11(mouseX, mouseY);
         WatermarkHud.SubContext hoveredContext = findContextAt(floatArray[0], floatArray[1]);
         if (hoveredContext == null) {
            return false;
         }

         if (hoveredContext.row < 0) {
            hoveredContext.pulseStartMillis = System.currentTimeMillis();
            return true;
         }

         draggedContext = hoveredContext;
         draggedOffsetX = floatArray[0] - hoveredContext.animator5.floatValue();
         return true;
      } else {
         return false;
      }
   }

   public static void processData(MinecraftClient minecraftClient, float x, float y) {
      animator.update();
      animator.animateTo(1.0, 0.3, Easings.EASE_OUT_QUART);
      animator2.update();
      animator2.animateTo(minecraftClient.getCurrentFps(), 0.15, Easings.EASE_OUT_QUART);
      String name = ProfileAPI.get().getModel().getName();
      boolean centeredMode = InterfaceModule.getInstance().checkState() && HudManager.computeFloat2() > 0.0F;
      if (draggedContext == null) {
         String configuredComponents = "," + InterfaceModule.getInstance().textSetting.getValue() + ",";
         subContexts.sort(
                 Comparator.comparingInt(
                         context -> configuredComponents.contains("," + context.id + ",")
                                 ? configuredComponents.indexOf("," + context.id + ",")
                                 : Integer.MAX_VALUE
                 )
         );
      }

      int latency = minecraftClient.player != null
              && minecraftClient.getNetworkHandler() != null
              && minecraftClient.getNetworkHandler().getPlayerListEntry(minecraftClient.player.getUuid()) != null
              ? minecraftClient.getNetworkHandler().getPlayerListEntry(minecraftClient.player.getUuid()).getLatency()
              : 0;

      ServerInfo serverInfo = minecraftClient.getCurrentServerEntry();
      String serverText = serverInfo != null ? serverInfo.address : "Singleplayer";

      float tps = Math.min(20.0F, currentTps());
      String tpsText = Math.abs(tps - Math.round(tps)) < 0.05F
              ? String.valueOf(Math.round(tps))
              : String.format(Locale.US, "%.1f", tps);

      for (WatermarkHud.SubContext context : subContexts) {
         context.enabled = context.label == null || InterfaceModule.getInstance().components.matchesOption(context.label);
         switch (context.id) {
            case "logo":
               context.setTextValues(null, null, 10.0F);
               break;
            case "avatar":
               context.setTextValues(null, null, 12.0F);
               break;
            case "user":
               context.setTextValues(name, null, 7.0F);
               break;
            case "fps":
               context.setTextValues(String.valueOf((int)animator2.floatValue()), "fps", 7.0F);
               break;
            case "ping":
               context.setTextValues(String.valueOf(latency), "ms", 7.0F);
               break;
            case "time":
               context.setTextValues(LocalTime.now().format(dateTimeFormatter), null, 7.0F);
               break;
            case "server":
               context.setTextValues(serverText, null, 7.0F);
               break;
            case "tps":
               context.setTextValues(tpsText, "tps", 7.0F);
               break;
            case "coords":
               context.setTextValues(
                       minecraftClient.player == null
                               ? "0 0 0"
                               : minecraftClient.player.getBlockX() + " " + minecraftClient.player.getBlockY() + " " + minecraftClient.player.getBlockZ(),
                       null,
                       7.0F
               );
               break;
         }

         context.animator6.update();
         context.animator6
                 .animateTo(
                         context.id.equals("fps")
                                 ? colorProgress(animator2.floatValue(), 60.0F, 25.0F)
                                 : (context.id.equals("ping") ? colorProgress(latency, 120.0F, 300.0F) : 0.0),
                         0.5,
                         Easings.EASE_OUT_QUART
                 );
         context.animator4.update();
         context.animator4.animateTo(context.contentWidth(), 0.05, Easings.EASE_OUT_EXPO);
      }

      for (int row = 0; row < ROWS; row++) {
         float content = rowContentWidth(row);
         rowPanelWidth[row] = content > 0.0F ? content + PAD * 2.0F : 0.0F;
      }

      rowOffsetY[0] = 0.0F;
      rowOffsetY[1] = rowPanelWidth[0] > 0.0F ? HEIGHT + BLOCK_GAP : 0.0F;

      float infoWidth = Math.max(rowPanelWidth[0], rowPanelWidth[1]);
      watermarkWidth = Math.max(LOGO_SIZE + (infoWidth > 0.0F ? BLOCK_GAP + infoWidth : 0.0F), 1.0F);
      watermarkHeight = rowPanelWidth[1] > 0.0F ? Math.max(HEIGHT, rowOffsetY[1] + HEIGHT) : HEIGHT;

      float uiScale = GuiScale.computeInt() / GuiScale.computeFloat();
      if (centeredMode) {
         x = HudManager.computeFloat2() / 2.0F - watermarkWidth / 2.0F;
         y = 5.0F;
      } else {
         float[] floatArray = HudManager.computeFloat5("watermark", x, y);
         x = floatArray[0];
         y = floatArray[1];
      }

      int bossBarCount = minecraftClient.inGameHud != null ? ((BossBarHudMixin)minecraftClient.inGameHud.getBossBarHud()).getEvents().size() : 0;
      float targetValue = 0.0F;
      if (bossBarCount > 0) {
         float bossBarHeight = (12.0F + (bossBarCount - 1) * 19.0F + 8.0F + 1.5F) * uiScale;
         float centerX = HudManager.computeFloat2() / 2.0F;
         if (x < centerX + 91.0F * uiScale && x + watermarkWidth > centerX - 91.0F * uiScale && y < bossBarHeight) {
            targetValue = bossBarHeight - y;
         }
      }

      animator3.update();
      animator3.animateTo(targetValue, 0.2, Easings.EASE_OUT_QUAD);
      y += animator3.floatValue();
      watermarkX = x;
      watermarkY = y;
      panelsX = x + LOGO_SIZE + BLOCK_GAP;
      HudManager.processData9("watermark", x, y, watermarkWidth, watermarkHeight);
      normalizedBottom = HudManager.computeFloat4() > 0.0F ? (y + watermarkHeight) / HudManager.computeFloat4() : 0.0F;
      float openProgress = animator.floatValue();
      if (!(openProgress <= 0.001F)) {
         boolean mouseActive = HudRenderContext.checkState2();
         float mouseX = (float)(minecraftClient.mouse.getScaledX(minecraftClient.getWindow()) * uiScale);
         float mouseY = (float)(minecraftClient.mouse.getScaledY(minecraftClient.getWindow()) * uiScale);
         if (draggedContext != null) {
            reorderAfterDrag(mouseX);
         }

         float[] rowLeft = new float[ROWS];
         for (int row = 0; row < ROWS; row++) {
            rowLeft[row] = panelsX + (infoWidth - rowPanelWidth[row]) / 2.0F;
         }

         float[] cursorX = {rowLeft[0] + PAD, rowLeft[1] + PAD};

         for (WatermarkHud.SubContext context : subContexts) {
            context.animator5.update();
            if (context.row < 0) {
               context.animator5.resetTo(x + (LOGO_SIZE - context.iconSize) / 2.0F);
               context.positionInitialized = true;
               continue;
            }

            int row = context.row;
            float value = context == draggedContext
                    ? Math.max(rowLeft[row] + PAD, Math.min(mouseX - draggedOffsetX, rowLeft[row] + rowPanelWidth[row] - PAD - context.animatedWidth()))
                    : cursorX[row];
            if (!context.positionInitialized) {
               context.positionInitialized = true;
               context.animator5.resetTo(value);
            } else if (context == draggedContext) {
               context.animator5.resetTo(value);
            } else {
               context.animator5.animateTo(value, 0.15, Easings.EASE_OUT_EXPO);
            }

            if (context.animatedWidth() >= 1.0F) {
               cursorX[row] += context.animatedWidth() + ITEM_GAP;
            }
         }

         float[] minX = {Float.MAX_VALUE, Float.MAX_VALUE};
         float[] maxX = {-Float.MAX_VALUE, -Float.MAX_VALUE};

         for (WatermarkHud.SubContext context : subContexts) {
            if (context.row >= 0 && !(context.animatedWidth() < 1.0F)) {
               minX[context.row] = Math.min(minX[context.row], context.animator5.floatValue());
               maxX[context.row] = Math.max(maxX[context.row], context.animator5.floatValue() + context.animatedWidth());
            }
         }

         HudStyle.processData4(x, y, LOGO_SIZE, HEIGHT, openProgress, 6.0F);
         for (int row = 0; row < ROWS; row++) {
            if (minX[row] != Float.MAX_VALUE) {
               HudStyle.processData4(
                       minX[row] - PAD,
                       y + rowOffsetY[row],
                       maxX[row] - minX[row] + PAD * 2.0F,
                       HEIGHT,
                       openProgress,
                       6.75F
               );
            }
         }

         int dividerColor = UiTheme.computeInt(-1, 0.1F * openProgress);
         for (int row = 0; row < ROWS; row++) {
            WatermarkHud.SubContext previous = null;
            for (WatermarkHud.SubContext context : subContexts) {
               if (context.row != row || !context.visible()) {
                  continue;
               }

               if (previous != null) {
                  float dividerX = (previous.animator5.floatValue() + previous.animatedWidth() + context.animator5.floatValue()) / 2.0F;
                  Render2D.drawRect(dividerX - 0.25F, y + rowOffsetY[row] + 4.0F, 0.5F, 7.0F, 0.25F, dividerColor);
               }

               previous = context;
            }
         }

         WatermarkHud.SubContext hoveredContext = mouseActive ? (draggedContext != null ? draggedContext : findContextAt(mouseX, mouseY)) : null;

         for (WatermarkHud.SubContext context : subContexts) {
            context.animator7.update();
            context.animator7.animateTo(context == hoveredContext ? 1.0 : 0.0, 0.2, Easings.EASE_OUT_QUART);
            if (context != draggedContext && context.visible()) {
               float rowY = context.row < 0 ? y : y + rowOffsetY[context.row];
               context.renderAt(rowY, rowY + 7.5F, openProgress);
            }
         }

         if (draggedContext != null && draggedContext.visible()) {
            float rowY = y + rowOffsetY[draggedContext.row];
            draggedContext.renderAt(rowY, rowY + 7.5F, openProgress);
         }
      }
   }

   private static float currentTps() {
      return 20.0F;
   }

   private static float rowContentWidth(int row) {
      float total = 0.0F;
      int count = 0;

      for (WatermarkHud.SubContext context : subContexts) {
         if (context.row == row && !(context.animatedWidth() < 1.0F)) {
            total += context.animatedWidth();
            count++;
         }
      }

      return count == 0 ? 0.0F : total + ITEM_GAP * (count - 1);
   }

   private static int interpolateColor(int color, float progress) {
      int blended = UiTheme.computeInt2(color, -42406, progress);
      return UiTheme.computeInt2(blended, -55256, ThemeColors.computeFloat((progress - 0.7F) / 0.3F));
   }

   public static float computeFloat2() {
      return normalizedBottom;
   }

   private static WatermarkHud.SubContext findContextAt(float mouseX, float mouseY) {
      for (WatermarkHud.SubContext context : subContexts) {
         if (context.row < 0) {
            if (mouseX >= watermarkX && mouseX <= watermarkX + LOGO_SIZE
                    && mouseY >= watermarkY && mouseY <= watermarkY + HEIGHT) {
               return context;
            }

            continue;
         }

         float top = watermarkY + rowOffsetY[context.row];
         if (mouseY < top + 3.5F || mouseY > top + HEIGHT - 3.5F) {
            continue;
         }

         float width = context.animatedWidth();
         float edgeMargin = Math.min(width * 0.25F, 3.0F);
         if (width >= 1.0F
                 && mouseX >= context.animator5.floatValue() + edgeMargin
                 && mouseX <= context.animator5.floatValue() + width - edgeMargin) {
            return context;
         }
      }

      return null;
   }

   public static void updateState() {
      if (draggedContext != null) {
         draggedContext = null;
         StringBuilder stringBuilder = new StringBuilder();

         for (WatermarkHud.SubContext context : subContexts) {
            stringBuilder.append(stringBuilder.isEmpty() ? "" : ",").append(context.id);
         }

         InterfaceModule.getInstance().textSetting.setValue(stringBuilder.toString());
      }
   }

   private static float colorProgress(float value, float low, float high) {
      return high > low
              ? ThemeColors.computeFloat((value - low) / (high - low))
              : ThemeColors.computeFloat((low - value) / (low - high));
   }

   private static void reorderAfterDrag(float mouseX) {
      float infoWidth = Math.max(rowPanelWidth[0], rowPanelWidth[1]);
      float[] rowLeft = new float[ROWS];
      for (int row = 0; row < ROWS; row++) {
         rowLeft[row] = panelsX + (infoWidth - rowPanelWidth[row]) / 2.0F;
      }

      float cursor = mouseX - draggedOffsetX + draggedContext.animatedWidth() / 2.0F;
      subContexts.remove(draggedContext);
      float currentX = rowLeft[draggedContext.row] + PAD;
      int insertIndex = 0;

      for (int index = 0; index < subContexts.size(); index++) {
         WatermarkHud.SubContext other = subContexts.get(index);
         if (other.row != draggedContext.row) {
            continue;
         }

         float width = other.animatedWidth();
         if (!(width < 1.0F)) {
            if (cursor > currentX + width / 2.0F) {
               insertIndex = index + 1;
            }

            currentX += width + ITEM_GAP;
         }
      }

      subContexts.add(insertIndex, draggedContext);
   }

   private static final class SubContext {
      final String id;
      final String label;
      final int row;
      boolean enabled = true;
      final Animation animator4 = new Animation();
      final Animation animator5 = new Animation();
      final Animation animator6 = new Animation();
      final Animation animator7 = new Animation();
      boolean positionInitialized;
      long pulseStartMillis;
      String valueText;
      String suffixText;
      float iconSize;

      void setTextValues(String valueText, String suffixText, float iconSize) {
         this.valueText = valueText;
         this.suffixText = suffixText;
         this.iconSize = iconSize;
      }

      SubContext(String id, String label, int row) {
         this.id = id;
         this.label = label;
         this.row = row;
      }

      float animatedWidth() {
         return Math.round(this.animator4.floatValue());
      }

      boolean visible() {
         return this.enabled && (this.row < 0 || this.animatedWidth() >= 1.0F);
      }

      void renderAt(float y, float textCenterY, float openProgress) {
         float elementX = Math.round((this.animator5.floatValue() + this.pulseOffset()) * 2.0F) / 2.0F;
         float highlight = this.animator7.floatValue();
         if (highlight > 0.001F) {
            boolean logo = this.row < 0;
            float baseX = this.animator5.floatValue();
            Render2D.drawRect(
                    logo ? baseX - (LOGO_SIZE - this.iconSize) / 2.0F : baseX - 3.0F,
                    logo ? y : y + 1.0F,
                    logo ? LOGO_SIZE : this.animatedWidth() + 6.0F,
                    logo ? HEIGHT : 13.0F,
                    logo ? 6.0F : 5.75F,
                    UiTheme.computeInt(-1, highlight * 0.12F * openProgress)
            );
         }

         float accentT = this.animator6.floatValue();
         int primaryColor = UiTheme.computeInt(WatermarkHud.interpolateColor(ThemeColors.intVal9, accentT), openProgress);
         int secondaryColor = UiTheme.computeInt(WatermarkHud.interpolateColor(HudStyle.computeInt4(1.0F), accentT), openProgress);
         int suffixColor = UiTheme.computeInt(secondaryColor, 0.5F);
         float iconY = textCenterY - this.iconSize / 2.0F;
         switch (this.id) {
            case "logo":
               float logoHeight = this.iconSize * 0.75F;
               float logoY = iconY + (this.iconSize - logoHeight) / 2.0F;
               Render2D.drawImageSized(
                       "assets/greed/textures/gui/mainmenu/icons/logo.png",
                       elementX,
                       logoY,
                       this.iconSize,
                       logoHeight,
                       0.0F,
                       primaryColor
               );
               break;
            case "avatar":
               Render2D.drawImageSized(
                       "assets/greed/textures/images/avatar.png",
                       elementX,
                       iconY,
                       this.iconSize,
                       this.iconSize,
                       this.iconSize / 2.0F,
                       UiTheme.computeInt(-1, openProgress)
               );
               break;
            case "user":
               HudIcons.USER.processData(elementX, iconY, this.iconSize, primaryColor);
               break;
            case "fps":
               HudIcons.FPS.processData(elementX, iconY, this.iconSize, primaryColor);
               break;
            case "ping":
               HudIcons.PING.processData(elementX, iconY, this.iconSize, primaryColor);
               break;
            case "coords":
               HudIcons.COORDS.processData(elementX, iconY, this.iconSize, primaryColor);
               break;
            case "server":
               HudIcons.COORDS.processData(elementX, iconY, this.iconSize, primaryColor);
               break;
            case "tps":
               HudIcons.FPS.processData(elementX, iconY, this.iconSize, primaryColor);
               break;
            default:
               HudIcons.TIME.processData(elementX, iconY, this.iconSize, primaryColor);
         }

         if (this.valueText != null && !this.valueText.isEmpty()) {
            float textX = Math.round((elementX + this.iconSize + 2.5F) * 2.0F) / 2.0F;
            FontText.processData3(FontFace.SEMI, this.valueText, textX, textCenterY - 3.5F, 6.0F, secondaryColor);
            if (this.suffixText != null && !this.suffixText.isEmpty()) {
               float suffixX = Math.round((textX + FontText.computeFloat(FontFace.SEMI, this.valueText, 6.0F) + SUFFIX_GAP) * 2.0F)
                       / 2.0F;
               FontText.processData3(FontFace.SEMI, this.suffixText, suffixX, textCenterY - 3.5F, 6.0F, suffixColor);
            }
         }
      }

      float pulseOffset() {
         if (this.pulseStartMillis == 0L) {
            return 0.0F;
         } else {
            long elapsedMillis = System.currentTimeMillis() - this.pulseStartMillis;
            if (elapsedMillis >= 360L) {
               this.pulseStartMillis = 0L;
               return 0.0F;
            } else {
               return (float)Math.sin((float)elapsedMillis / 1000.0F * 70.0F) * 2.5F * (1.0F - (float)elapsedMillis / 360.0F);
            }
         }
      }

      float contentWidth() {
         if (!this.enabled) {
            return 0.0F;
         }

         if (this.valueText != null && !this.valueText.isEmpty()) {
            float width = this.iconSize + 2.5F + FontText.computeFloat(FontFace.SEMI, this.valueText, 6.0F);
            if (this.suffixText != null && !this.suffixText.isEmpty()) {
               width += SUFFIX_GAP + FontText.computeFloat(FontFace.SEMI, this.suffixText, 6.0F);
            }

            return width;
         } else {
            return this.iconSize;
         }
      }
   }
}