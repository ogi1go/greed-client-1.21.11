package greed.client.ui.panel;

import greed.client.ui.ClickGuiStyle;
import greed.client.render.FontFace;
import greed.client.render.FontText;
import greed.client.render.RectDrawData;
import greed.client.ui.setting.SettingRenderContext;
import greed.client.ui.setting.SettingRendererRegistry;
import greed.client.ui.UiTheme;

import greed.client.module.Module;
import greed.client.render.Render2D;
import greed.client.setting.Setting;
import greed.client.setting.SettingGroup;

import java.util.List;

public final class ModulePanel {
   public static final float floatVal = 19.6F;

   private static void renderToggleSwitch(SettingRenderContext settingRenderContext, Module newModule, float x, float y, float[] floatArray) {
      float scale = settingRenderContext.scale;
      float toggleProgress = settingRenderContext.computeFloat2("tgl:" + newModule.name, newModule.isEnabled());
      float trackWidth = 10.74F * scale;
      float trackHeight = 6.64F * scale;
      Render2D.drawRectData(
         new RectDrawData(
               x,
               y,
               trackWidth,
               trackHeight,
               trackHeight / 2.0F,
               trackHeight / 2.0F,
               trackHeight / 2.0F,
               trackHeight / 2.0F,
               Render2D.applyAlpha(UiTheme.computeInt2(ClickGuiStyle.intVal15, ClickGuiStyle.intVal17, toggleProgress))
            )
            .withSmoothness(0.5F)
      );
      float knobDiameter = 4.88F * scale;
      float knobX = x + 0.93F * scale + toggleProgress * (trackWidth - knobDiameter - 1.86F * scale);
      Render2D.drawCircleSmooth(
         knobX + knobDiameter / 2.0F,
         y + trackHeight / 2.0F,
         knobDiameter / 2.0F,
         0.5F,
         Render2D.applyAlpha(UiTheme.computeInt2(ClickGuiStyle.intVal16, ClickGuiStyle.intVal, toggleProgress))
      );
      settingRenderContext.processData7(
         x - 1.0F * scale,
         y - 1.5F * scale,
         trackWidth + 2.0F * scale,
         trackHeight + 3.0F * scale,
         floatArray,
         newModule::toggle
      );
   }

   private static float renderSettingGroup(
      SettingRenderContext settingRenderContext,
      SettingGroup settingGroup,
      float toggleProgress,
      float x,
      float y,
      float width,
      float[] floatArray
   ) {
      float scale = settingRenderContext.scale;
      if (!settingGroup.checkState()) {
         return renderSettingList(settingRenderContext, settingGroup.list, toggleProgress, x, y, width, floatArray);
      }

      float expandProgress = !settingGroup.list.isEmpty()
         ? settingRenderContext.computeFloat10(settingGroup.object, settingRenderContext.matchesCriteria(settingGroup.object, false))
         : 0.0F;
      settingRenderContext.processData15(
         settingGroup.strVal,
         x,
         y,
         5.5F * scale,
         UiTheme.computeInt2(ClickGuiStyle.intVal13, ClickGuiStyle.intVal14, toggleProgress),
         width - 6.0F * scale
      );
      Render2D.drawImageRotated(
         "assets/greed/textures/gui/clickgui/chev.png",
         x + width - 4.0F * scale,
         y + 0.6F * scale,
         4.0F * scale,
         0.0F,
         expandProgress * 180.0F,
         x + width - 2.0F * scale,
         y + 2.6F * scale,
         UiTheme.computeInt2(ClickGuiStyle.intVal8, ClickGuiStyle.intVal9, toggleProgress)
      );
      settingRenderContext.processData7(
         x,
         y - 1.5F * scale,
         width,
         8.0F * scale,
         floatArray,
         () -> settingRenderContext.processData5(settingGroup.object, false)
      );
      float totalHeight = 6.0F * scale;
      if (expandProgress > 0.001F) {
         float[] floatArray2 = SettingRenderContext.computeFloat3(
            floatArray,
            x,
            y + 6.0F * scale,
            width,
            expandProgress * (3.5F * scale + computeSettingListHeight(settingRenderContext, settingGroup, width))
         );
         Render2D.beginScissor(settingRenderContext.drawContext, floatArray2[0], floatArray2[1], floatArray2[2], floatArray2[3]);
         Render2D.pushAlpha(expandProgress);
         renderSettingList(
            settingRenderContext, settingGroup.list, toggleProgress, x, y + 6.0F * scale + 3.5F * scale, width, floatArray2
         );
         Render2D.popAlpha();
         Render2D.endScissor(settingRenderContext.drawContext);
         totalHeight = 6.0F * scale + expandProgress * (3.5F * scale + computeSettingListHeight(settingRenderContext, settingGroup, width));
      }

      return totalHeight;
   }

   private static float renderSettingList(
      SettingRenderContext settingRenderContext,
      List<Setting<?>> settings,
      float toggleProgress,
      float x,
      float y,
      float width,
      float[] floatArray
   ) {
      float scale = settingRenderContext.scale;
      float cursorY = y;
      boolean isFirst = true;

      for (Setting<?> setting : settings) {
         float revealProgress = settingRenderContext.computeFloat10(setting, setting.isVisible());
         if (!(revealProgress < 0.001F)) {
            if (!isFirst) {
               cursorY += 3.5F * scale * revealProgress;
            }

            isFirst = false;
            if (revealProgress > 0.999F) {
               cursorY += SettingRendererRegistry.computeFloat(settingRenderContext, setting, toggleProgress, x, cursorY, width, floatArray);
            } else {
               float settingHeight = SettingRendererRegistry.computeFloat2(settingRenderContext, setting, width);
               float[] floatArray2 = SettingRenderContext.computeFloat3(
                  floatArray, x - 3.0F * scale, cursorY, width + 6.0F * scale, settingHeight * revealProgress
               );
               Render2D.beginScissor(settingRenderContext.drawContext, floatArray2[0], floatArray2[1], floatArray2[2], floatArray2[3]);
               Render2D.pushAlpha(revealProgress);
               SettingRendererRegistry.computeFloat(
                  settingRenderContext, setting, toggleProgress, x, cursorY - (1.0F - revealProgress) * 4.0F * scale, width, floatArray2
               );
               Render2D.popAlpha();
               Render2D.endScissor(settingRenderContext.drawContext);
               cursorY += settingHeight * revealProgress;
            }
         }
      }

      return cursorY - y;
   }

   private static float computeSettingGroupHeight(SettingRenderContext settingRenderContext, SettingGroup settingGroup, float width) {
      float scale = settingRenderContext.scale;
      if (!settingGroup.checkState()) {
         return computeSettingListHeight(settingRenderContext, settingGroup, width);
      }

      float baseHeight = 6.0F * scale;
      if (settingGroup.list.isEmpty()) {
         return baseHeight;
      }

      float expandProgress = settingRenderContext.computeFloat10(settingGroup.object, settingRenderContext.matchesCriteria(settingGroup.object, false));
      return expandProgress < 0.001F ? baseHeight : baseHeight + expandProgress * (3.5F * scale + computeSettingListHeight(settingRenderContext, settingGroup, width));
   }

   public static float computeFloat4(SettingRenderContext settingRenderContext, Module newModule, float width) {
      float scale = settingRenderContext.scale;
      float collapsedHeight = 19.6F * scale;
      if (!SettingGroup.matchesCriteria(SettingGroup.getList(newModule))) {
         return collapsedHeight;
      }

      float expandProgress = settingRenderContext.computeFloat10(newModule, newModule.bindingActive);
      return expandProgress < 0.001F
         ? collapsedHeight
         : collapsedHeight
            + expandProgress * (computeSettingGroupsHeight(settingRenderContext, SettingGroup.getList(newModule), width - 12.5F * scale) + 5.1F * scale);
   }

   private ModulePanel() {
   }

   public static void processData2(
      SettingRenderContext settingRenderContext,
      Module newModule,
      float x,
      float y,
      float width,
      float[] floatArray,
      List<String> descriptionLines
   ) {
      float scale = settingRenderContext.scale;
      float toggleProgress = settingRenderContext.computeFloat2("tgl:" + newModule.name, newModule.isEnabled());
      List<SettingGroup> settingGroups = SettingGroup.getList(newModule);
      boolean hasGroups = SettingGroup.matchesCriteria(settingGroups);
      float expandProgress = hasGroups ? settingRenderContext.computeFloat10(newModule, newModule.bindingActive) : 0.0F;

      for (SettingGroup settingGroup : settingGroups) {
         for (Setting<?> setting : settingGroup.list) {
            if (settingRenderContext.object == setting
               && (
                  !newModule.bindingActive || !setting.isVisible() || settingGroup.checkState() && !settingRenderContext.matchesCriteria(settingGroup.object, false)
               )) {
               settingRenderContext.object = null;
            }
         }
      }

      float panelHeight = computeFloat4(settingRenderContext, newModule, width);
      float descriptionHeight = descriptionLines.isEmpty() ? 0.0F : descriptionLines.size() * 6.0F * scale + 4.0F * scale;
      float cardHeight = panelHeight + descriptionHeight;
      float highlightProgress = Math.max(toggleProgress, expandProgress);
      highlightProgress = Math.max(
         highlightProgress,
         0.35F
            * settingRenderContext.computeFloat2(
               "row:" + newModule.name,
               UiTheme.matchesCriteria(settingRenderContext.mouseX, settingRenderContext.mouseY, x, y, width, cardHeight)
                  && UiTheme.matchesCriteria(
                     settingRenderContext.mouseX, settingRenderContext.mouseY, floatArray[0], floatArray[1], floatArray[2], floatArray[3]
                  )
            )
      );
      if (highlightProgress > 0.005F) {
         Render2D.drawRect(
            x,
            y,
            width,
            cardHeight,
            4.5F * scale,
            UiTheme.computeInt(
               UiTheme.computeInt2(ClickGuiStyle.intVal6, ClickGuiStyle.intVal5, toggleProgress), highlightProgress
            )
         );
         Render2D.drawOutline(
            x,
            y,
            width,
            cardHeight,
            4.5F * scale,
            0.5F,
            UiTheme.computeInt(ClickGuiStyle.intVal3, highlightProgress)
         );
      }

      settingRenderContext.processData11(
         x,
         y,
         width,
         19.6F * scale,
         floatArray,
         newModule::toggle,
         hasGroups ? () -> newModule.bindingActive = !newModule.bindingActive : null,
         () -> settingRenderContext.module = settingRenderContext.module == newModule ? null : newModule
      );
      float toggleX = x + width - 16.1F * scale;
      float toggleY = y + 9.8F * scale;
      if (newModule.description != null
         && !newModule.description.isEmpty()
         && UiTheme.matchesCriteria(settingRenderContext.mouseX, settingRenderContext.mouseY, floatArray[0], floatArray[1], floatArray[2], floatArray[3])) {
         settingRenderContext.processData17(
            newModule.description, x + 3.0F * scale, y + 2.0F * scale, width * 0.6F, 15.6F * scale
         );
      }

      float labelMaxWidth = (hasGroups ? toggleX - 10.5F * scale : toggleX - 2.0F * scale) - (x + 6.3F * scale);
      settingRenderContext.processData15(
         settingRenderContext.module == newModule ? "Нажми клавишу" : newModule.name,
         x + 6.3F * scale,
         y + 5.8F * scale,
         7.0F * scale,
         settingRenderContext.module == newModule
            ? ClickGuiStyle.intVal
            : UiTheme.computeInt2(ClickGuiStyle.intVal7, ClickGuiStyle.intVal, toggleProgress),
         labelMaxWidth
      );
      if (hasGroups) {
         float chevronX = toggleX - 6.7F * scale;
         Render2D.drawImageRotated(
            "assets/greed/textures/gui/clickgui/chev.png",
            chevronX - 2.25F * scale,
            toggleY - 2.25F * scale,
            4.5F * scale,
            0.0F,
            expandProgress * 180.0F,
            chevronX,
            toggleY,
            UiTheme.computeInt2(ClickGuiStyle.intVal8, ClickGuiStyle.intVal9, Math.max(toggleProgress, expandProgress))
         );
         settingRenderContext.processData7(
            chevronX - 3.5F * scale,
            toggleY - 3.5F * scale,
            7.0F * scale,
            7.0F * scale,
            floatArray,
            () -> newModule.bindingActive = !newModule.bindingActive
         );
      }

      renderToggleSwitch(settingRenderContext, newModule, toggleX, toggleY - 3.32F * scale, floatArray);
      if (expandProgress > 0.001F) {
         float contentTopY = y + 19.0F * scale;
         float[] floatArray2 = SettingRenderContext.computeFloat3(floatArray, x, contentTopY, width, panelHeight - 19.0F * scale);
         Render2D.beginScissor(settingRenderContext.drawContext, floatArray2[0], floatArray2[1], floatArray2[2], floatArray2[3]);
         Render2D.pushAlpha(expandProgress);
         float cursorY = contentTopY;

         for (int index = 0; index < settingGroups.size(); index++) {
            cursorY += renderSettingGroup(
               settingRenderContext,
               settingGroups.get(index),
               toggleProgress,
               x + 6.3F * scale,
               cursorY,
               width - 12.5F * scale,
               floatArray2
            );
            if (index < settingGroups.size() - 1) {
               cursorY += 3.5F * scale;
            }
         }

         Render2D.popAlpha();
         Render2D.endScissor(settingRenderContext.drawContext);
      }

      if (!descriptionLines.isEmpty()) {
         float descriptionY = y + panelHeight - 4.0F * scale;
         int descriptionColor = UiTheme.computeInt2(ClickGuiStyle.intVal7, ClickGuiStyle.intVal, highlightProgress);

         for (String line : descriptionLines) {
            FontText.processData3(FontFace.SF, line, x + 6.0F * scale, descriptionY, 5.0F * scale, descriptionColor);
            descriptionY += 6.0F * scale;
         }
      }
   }

   private static float computeSettingListHeight(SettingRenderContext settingRenderContext, SettingGroup settingGroup, float width) {
      float scale = settingRenderContext.scale;
      float totalHeight = 0.0F;
      boolean isFirst = true;

      for (Setting<?> setting : settingGroup.list) {
         float revealProgress = settingRenderContext.computeFloat10(setting, setting.isVisible());
         if (!(revealProgress < 0.001F)) {
            if (!isFirst) {
               totalHeight += 3.5F * scale * revealProgress;
            }

            totalHeight += SettingRendererRegistry.computeFloat2(settingRenderContext, setting, width) * revealProgress;
            isFirst = false;
         }
      }

      return totalHeight;
   }

   private static float computeSettingGroupsHeight(SettingRenderContext settingRenderContext, List<SettingGroup> settingGroups, float width) {
      float scale = settingRenderContext.scale;
      float totalHeight = 0.0F;

      for (int index = 0; index < settingGroups.size(); index++) {
         totalHeight += computeSettingGroupHeight(settingRenderContext, settingGroups.get(index), width);
         if (index < settingGroups.size() - 1) {
            totalHeight += 3.5F * scale;
         }
      }

      return totalHeight;
   }
}
