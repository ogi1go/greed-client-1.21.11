package greed.client.ui.panel;

import greed.client.ui.ClickGuiStyle;
import greed.client.render.RoundedClipStack;
import greed.client.ui.setting.SettingRenderContext;
import greed.client.ui.SmoothScroll;
import greed.client.ui.UiTheme;

import greed.client.render.FontFace;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleManager;
import greed.client.render.Render2D;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class ModuleCategoryPanel {
   private static final Map<Module, ModuleCategoryPanel.DataRecord> map = new IdentityHashMap<>();

   public static void processData(
           SettingRenderContext settingRenderContext,
           ModuleCategory moduleCategory,
           SmoothScroll smoothScroll,
           float panelX,
           float panelY,
           float panelWidth,
           float panelHeight,
           float headerHeight,
           float scrollDelta,
           int columns
   ) {
      float scale = settingRenderContext.scale;
      float contentY = panelY + headerHeight;

      Render2D.drawBlurSmooth(
              panelX + 0.5F * scale,
              panelY + 0.5F * scale,
              panelWidth - 1.0F * scale,
              panelHeight - 1.0F * scale,
              8.0F * scale,
              30.0F,
              1.0F,
              -1
      );
      Render2D.drawRect(panelX, panelY, panelWidth, panelHeight, 8.5F * scale, ClickGuiStyle.intVal2);
      Render2D.drawOutline(panelX, panelY, panelWidth, panelHeight, 8.5F * scale, 0.5F, ClickGuiStyle.intVal3);

      smoothScroll.update(scrollDelta);
      float contentHeightAvailable = panelHeight - headerHeight;
      Render2D.beginScissor(settingRenderContext.drawContext, panelX, contentY, panelWidth, contentHeightAvailable);
      RoundedClipStack.processData(
              panelX, contentY, panelWidth, contentHeightAvailable, 0.0F, 0.0F, 8.5F * scale, 8.5F * scale
      );

      List<Module> modules = settingRenderContext.strVal.isEmpty()
              ? ModuleManager.getModulesByCategory(moduleCategory)
              : ModuleManager.getModules();
      float spacing = 4.0F * scale;
      float totalSpacingWidth = spacing * (columns + 1);
      float colWidth = (panelWidth - totalSpacingWidth) / columns;
      float startX = panelX + spacing;
      float startY = contentY + 6.0F * scale - smoothScroll.getPixelAlignedOffset();

      float[] columnY = new float[columns];
      for (int c = 0; c < columns; c++) {
         columnY[c] = startY;
      }

      float maxColumnBottom = startY;

      for (int i = 0; i < modules.size(); i++) {
         Module module = modules.get(i);
         ModuleCategoryPanel.DataRecord record = getNested0484(module);
         float alpha = settingRenderContext.computeFloat7(
                 record.searchKey,
                 !settingRenderContext.strVal.isEmpty()
                         && !record.lowerName.contains(settingRenderContext.strVal)
                         && !record.lowerDescription.contains(settingRenderContext.strVal)
                         ? 0.0F
                         : 1.0F
         );

         if (!(alpha < 0.01F)) {
            if (alpha < 0.999F) {
               Render2D.pushAlpha(alpha * alpha);
            }

            int fixedCol = i % columns;
            float moduleX = startX + fixedCol * (colWidth + spacing);
            float moduleY = columnY[fixedCol];

            float baseModuleHeight = ModulePanel.computeFloat4(settingRenderContext, module, colWidth);

            List<String> descLines = List.of();
            float descHeight = 0.0F;
            if (module.description != null && !module.description.isEmpty()) {
               descLines = UiTheme.getList(FontFace.SF, module.description, 5.0F * scale, colWidth - 12.0F * scale);
               descHeight = descLines.size() * 6.0F * scale + 4.0F * scale;
            }

            float moduleHeight = baseModuleHeight + descHeight;

            boolean visible = (moduleY + moduleHeight >= contentY - 2.0F) && (moduleY <= contentY + contentHeightAvailable + 2.0F);

            if (visible) {
               float[] boundsArray = new float[]{panelX, contentY, panelWidth, contentHeightAvailable};

               // Рисуем подложку ровно на всю вычисленную высоту (настройки + описание)
               Render2D.drawRect(moduleX, moduleY, colWidth, moduleHeight, 6.0F * scale, ClickGuiStyle.intVal2);
               Render2D.drawOutline(moduleX, moduleY, colWidth, moduleHeight, 6.0F * scale, 0.5F, ClickGuiStyle.intVal3);

               // Отрисовываем настройки модуля и описание (описание рисуется в рамках подсветки карточки)
               ModulePanel.processData2(
                       settingRenderContext, module, moduleX, moduleY, colWidth, boundsArray, descLines
               );
            }

            if (alpha < 0.999F) {
               Render2D.popAlpha();
            }

            columnY[fixedCol] += moduleHeight + 4.0F * scale;
            maxColumnBottom = Math.max(maxColumnBottom, columnY[fixedCol]);
         }
      }

      RoundedClipStack.updateState2();
      Render2D.endScissor(settingRenderContext.drawContext);

      float totalContentHeight = maxColumnBottom - startY + smoothScroll.getPixelAlignedOffset();
      smoothScroll.maximumOffset = Math.max(0.0F, totalContentHeight - contentHeightAvailable + 8.0F * scale);
   }

   private ModuleCategoryPanel() {
   }

   private static ModuleCategoryPanel.DataRecord getNested0484(Module module) {
      ModuleCategoryPanel.DataRecord record = map.get(module);
      if (record == null || record.name != module.name || record.description != module.description) {
         String desc = module.description == null ? "" : module.description;
         record = new ModuleCategoryPanel.DataRecord(
                 module.name, module.description, module.name.toLowerCase(), desc.toLowerCase(), "search." + module.name
         );
         map.put(module, record);
      }
      return record;
   }

   private record DataRecord(String name, String description, String lowerName, String lowerDescription, String searchKey) {
   }
}