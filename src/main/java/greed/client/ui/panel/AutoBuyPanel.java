package greed.client.ui.panel;

import greed.client.ui.AnimatedValue;
import greed.client.util.AuctionItemCatalog;
import greed.client.ui.ClickGuiStyle;
import greed.client.render.FontText;
import greed.client.ui.hud.HudStyle;
import greed.client.ui.setting.SettingRenderContext;
import greed.client.render.ShapeDrawing;
import greed.client.ui.UiTheme;

import greed.client.ui.AutoBuySettingsScreen;
import greed.client.render.FontFace;
import greed.client.render.Render2D;

public final class AutoBuyPanel {
   static final String strVal = "ε";
   private static AutoBuyPanel autoBuyPanel;
   private AnimatedValue animatedValue = new AnimatedValue(300L).getAnimatedValue(0.0);
   private boolean flag;
   private long timeMillis;
   private SettingRenderContext settingRenderContext;

   public boolean checkState() {
      if (AutoBuySettingsScreen.getInstance().strVal != null) {
         this.updateState3();
         return true;
      }

      if (!this.flag) {
         return false;
      }

      this.flag = false;
      return true;
   }

   public void updateState() {
      if (!this.flag) {
         AuctionItemCatalog.updateState();
         this.flag = true;
         this.timeMillis = System.currentTimeMillis();
         AutoBuySettingsScreen.getInstance().timeMillis3 = this.timeMillis;
         this.updateState3();
      }
   }

   static float computeFloat(long local4909_0, float local4909_2, float local4909_3) {
      float local4909_4 = ((float)local4909_0 / 1000.0F - local4909_2) / local4909_3;
      if (local4909_4 <= 0.0F) {
         return 0.0F;
      } else {
         return local4909_4 >= 1.0F ? 1.0F : local4909_4 * local4909_4 * (3.0F - 2.0F * local4909_4);
      }
   }

   public boolean checkState2() {
      return this.flag;
   }

   private void processData(SettingRenderContext newSettingRenderContext, String local4911_2, String local4911_3, float local4911_4, float local4911_5) {
      Render2D.drawRect(
         local4911_4,
         local4911_5,
         FontText.computeFloat(FontFace.SEMI, local4911_2, 4.5F * newSettingRenderContext.scale) + 6.0F * newSettingRenderContext.scale,
         9.0F * newSettingRenderContext.scale,
         2.0F * newSettingRenderContext.scale,
         ClickGuiStyle.intVal49
      );
      FontText.processData3(
         FontFace.SEMI,
         local4911_2,
         local4911_4 + 3.0F * newSettingRenderContext.scale,
         local4911_5 + 2.25F * newSettingRenderContext.scale,
         4.5F * newSettingRenderContext.scale,
         UiTheme.intVal7
      );
      FontText.processData3(
         FontFace.SEMI,
         local4911_3,
         local4911_4 + FontText.computeFloat(FontFace.SEMI, local4911_2, 4.5F * newSettingRenderContext.scale) + 10.0F * newSettingRenderContext.scale,
         local4911_5 + 2.0F * newSettingRenderContext.scale,
         5.0F * newSettingRenderContext.scale,
         UiTheme.intVal3
      );
   }

   static float computeFloat2() {
      return (Render2D.applyAlpha(-16777216) >>> 24 & 0xFF) / 255.0F;
   }

   public static AutoBuyPanel getInstance() {
      if (autoBuyPanel == null) {
         autoBuyPanel = new AutoBuyPanel();
      }

      return autoBuyPanel;
   }

   private AutoBuyPanel() {
   }

   private void processData2(SettingRenderContext newSettingRenderContext, float local4916_2, float local4916_3, boolean local4916_4, float local4916_5, float local4916_6) {
      float local4916_7 = 512.0F * newSettingRenderContext.scale;
      float local4916_8 = 300.0F * newSettingRenderContext.scale;
      float local4916_9 = (local4916_2 - local4916_7) / 2.0F;
      float local4916_10 = (local4916_3 - local4916_8) / 2.0F + local4916_6;
      long local4916_11 = System.currentTimeMillis();
      HudStyle.processData2(
         local4916_9, local4916_10, local4916_7, local4916_8, computeFloat2(), 7.0F * newSettingRenderContext.scale, ShapeDrawing.TypeOption.LOW
      );
      if (local4916_4) {
         newSettingRenderContext.processData7(local4916_9, local4916_10, local4916_7, local4916_8, null, () -> {});
      }

      newSettingRenderContext.processData4(
         "9",
         local4916_9 + 13.0F * newSettingRenderContext.scale,
         local4916_10 + 12.0F * newSettingRenderContext.scale,
         6.6F * newSettingRenderContext.scale,
         UiTheme.intVal7
      );
      FontText.processData3(
         FontFace.SEMI,
         "AutoBuy",
         local4916_9 + 21.0F * newSettingRenderContext.scale,
         local4916_10 + 9.0F * newSettingRenderContext.scale,
         6.0F * newSettingRenderContext.scale,
         UiTheme.intVal
      );
      FontText.processData3(
         FontFace.SEMI,
         "в списке " + AutoBuySettingsScreen.getInstance().map2.size(),
         local4916_9
            + 21.0F * newSettingRenderContext.scale
            + FontText.computeFloat(FontFace.SEMI, "AutoBuy", 6.0F * newSettingRenderContext.scale)
            + 5.0F * newSettingRenderContext.scale,
         local4916_10 + 9.5F * newSettingRenderContext.scale,
         5.0F * newSettingRenderContext.scale,
         UiTheme.intVal3
      );
      float local4916_13 = newSettingRenderContext.computeFloat2(
         "ab:close",
         local4916_4
            && UiTheme.matchesCriteria(
               newSettingRenderContext.mouseX,
               newSettingRenderContext.mouseY,
               local4916_9 + local4916_7 - 19.0F * newSettingRenderContext.scale,
               local4916_10 + 6.0F * newSettingRenderContext.scale,
               12.0F * newSettingRenderContext.scale,
               12.0F * newSettingRenderContext.scale
            )
      );
      if (local4916_13 > 0.005F) {
         Render2D.drawCircle(
            local4916_9 + local4916_7 - 13.0F * newSettingRenderContext.scale,
            local4916_10 + 12.0F * newSettingRenderContext.scale,
            6.0F * newSettingRenderContext.scale,
            UiTheme.computeInt(UiTheme.intVal5, local4916_13)
         );
      }

      newSettingRenderContext.processData4(
         "ε",
         local4916_9 + local4916_7 - 13.0F * newSettingRenderContext.scale,
         local4916_10 + 12.0F * newSettingRenderContext.scale,
         6.6F * newSettingRenderContext.scale,
         UiTheme.computeInt2(UiTheme.intVal4, UiTheme.intVal, local4916_13)
      );
      if (local4916_4) {
         newSettingRenderContext.processData7(
            local4916_9 + local4916_7 - 19.0F * newSettingRenderContext.scale,
            local4916_10 + 6.0F * newSettingRenderContext.scale,
            12.0F * newSettingRenderContext.scale,
            12.0F * newSettingRenderContext.scale,
            null,
            () -> this.updateState2()
         );
      }

      Render2D.drawRect(
         local4916_9 + 8.0F * newSettingRenderContext.scale,
         local4916_10 + 23.5F * newSettingRenderContext.scale,
         local4916_7 - 16.0F * newSettingRenderContext.scale,
         0.5F * newSettingRenderContext.scale,
         0.0F,
         UiTheme.intVal17
      );
      float local4916_14 = local4916_10 + 30.0F * newSettingRenderContext.scale;
      float local4916_15 = local4916_10 + local4916_8 - 8.0F * newSettingRenderContext.scale;
      AutoBuyItemPanel.processData3(
         newSettingRenderContext,
         AutoBuySettingsScreen.getInstance(),
         local4916_9 + 8.0F * newSettingRenderContext.scale,
         local4916_14,
         300.0F * newSettingRenderContext.scale,
         17.0F * newSettingRenderContext.scale,
         local4916_4
      );
      AutoBuyItemPanel.processData11(
         newSettingRenderContext,
         AutoBuySettingsScreen.getInstance(),
         this,
         local4916_9 + 8.0F * newSettingRenderContext.scale,
         local4916_14 + 21.0F * newSettingRenderContext.scale,
         300.0F * newSettingRenderContext.scale,
         local4916_15 - 14.0F * newSettingRenderContext.scale - local4916_14 - 21.0F * newSettingRenderContext.scale,
         local4916_4,
         local4916_5,
         local4916_11
      );
      this.processData(newSettingRenderContext, "ЛКМ", "добавить или убрать", local4916_9 + 8.0F * newSettingRenderContext.scale, local4916_15 - 9.0F * newSettingRenderContext.scale);
      this.processData(newSettingRenderContext, "ПКМ", "свои цены предмета", local4916_9 + 100.0F * newSettingRenderContext.scale, local4916_15 - 9.0F * newSettingRenderContext.scale);
      float local4916_16 = local4916_9 + 316.0F * newSettingRenderContext.scale;
      float local4916_17 = local4916_7 - 324.0F * newSettingRenderContext.scale;
      Render2D.pushAlpha(computeFloat(local4916_11 - this.timeMillis, 0.06F, 0.3F));
      AutoBuyItemPanel.processData(
         newSettingRenderContext,
         AutoBuySettingsScreen.getInstance(),
         this,
         local4916_16,
         local4916_14,
         local4916_17,
         80.0F * newSettingRenderContext.scale,
         local4916_4,
         local4916_11
      );
      Render2D.popAlpha();
      Render2D.pushAlpha(computeFloat(local4916_11 - this.timeMillis, 0.12F, 0.3F));
      AutoBuyItemPanel.processData4(
         newSettingRenderContext,
         AutoBuySettingsScreen.getInstance(),
         local4916_16,
         local4916_14 + 86.0F * newSettingRenderContext.scale,
         local4916_17,
         52.0F * newSettingRenderContext.scale,
         local4916_4,
         local4916_11
      );
      Render2D.popAlpha();
      Render2D.pushAlpha(computeFloat(local4916_11 - this.timeMillis, 0.18F, 0.3F));
      AutoBuyItemPanel.processData6(
         newSettingRenderContext,
         AutoBuySettingsScreen.getInstance(),
         local4916_16,
         local4916_14 + 144.0F * newSettingRenderContext.scale,
         local4916_17,
         local4916_15 - local4916_14 - 144.0F * newSettingRenderContext.scale,
         local4916_5,
         local4916_11,
         this.timeMillis
      );
      Render2D.popAlpha();
   }

   public void updateState2() {
      this.flag = false;
      this.updateState3();
   }

   public boolean matchesCriteria(float local4918_1, float local4918_2, double local4918_3, float local4918_5) {
      if (AutoBuySettingsScreen.getInstance().floatValArray3 != null
         && UiTheme.matchesCriteria(
            local4918_1,
            local4918_2,
            AutoBuySettingsScreen.getInstance().floatValArray3[0],
            AutoBuySettingsScreen.getInstance().floatValArray3[1],
            AutoBuySettingsScreen.getInstance().floatValArray3[2],
            AutoBuySettingsScreen.getInstance().floatValArray3[3]
         )) {
         AutoBuySettingsScreen.getInstance().smoothScroll.targetOffset = SettingRenderContext.computeFloat(
            AutoBuySettingsScreen.getInstance().smoothScroll.targetOffset - (float)local4918_3 * 24.0F * local4918_5,
            0.0F,
            AutoBuySettingsScreen.getInstance().smoothScroll.maximumOffset
         );
      } else if (AutoBuySettingsScreen.getInstance().floatValArray4 != null
         && UiTheme.matchesCriteria(
            local4918_1,
            local4918_2,
            AutoBuySettingsScreen.getInstance().floatValArray4[0],
            AutoBuySettingsScreen.getInstance().floatValArray4[1],
            AutoBuySettingsScreen.getInstance().floatValArray4[2],
            AutoBuySettingsScreen.getInstance().floatValArray4[3]
         )) {
         AutoBuySettingsScreen.getInstance().smoothScroll2.targetOffset = SettingRenderContext.computeFloat(
            AutoBuySettingsScreen.getInstance().smoothScroll2.targetOffset - (float)local4918_3 * 24.0F * local4918_5,
            0.0F,
            AutoBuySettingsScreen.getInstance().smoothScroll2.maximumOffset
         );
      }

      return true;
   }

   void processData3(String local4919_1) {
      if (this.settingRenderContext != null) {
         this.settingRenderContext.textSetting = null;
      }

      if (!AutoBuySettingsScreen.getInstance().matchesCriteria(local4919_1)) {
         AutoBuySettingsScreen.getInstance().processData2(local4919_1);
      }

      if (!local4919_1.equals(AutoBuySettingsScreen.getInstance().strVal)) {
         AutoBuySettingsScreen.getInstance().strVal = local4919_1;
         AutoBuySettingsScreen.getInstance().timeMillis2 = System.currentTimeMillis();
      }
   }

   public float computeFloat3() {
      this.animatedValue.processData(this.flag ? 1.0 : 0.0);
      return (float)this.animatedValue.computeDouble();
   }

   public void processData4(SettingRenderContext newSettingRenderContext, float local4921_2, float local4921_3, float local4921_4, float local4921_5) {
      if (!(local4921_4 < 0.005F)) {
         this.settingRenderContext = newSettingRenderContext;
         newSettingRenderContext.intVal = newSettingRenderContext.list.size();
         AutoBuySettingsScreen.getInstance().processData(System.currentTimeMillis());
         Render2D.drawRect(0.0F, 0.0F, local4921_2, local4921_3, 0.0F, UiTheme.computeInt(ClickGuiStyle.intVal41, local4921_4));
         if (this.flag && local4921_4 > 0.6F) {
            newSettingRenderContext.processData7(0.0F, 0.0F, local4921_2, local4921_3, null, () -> this.updateState2());
         }

         Render2D.pushAlpha(local4921_4);
         this.processData2(
            newSettingRenderContext, local4921_2, local4921_3, this.flag && local4921_4 > 0.6F, local4921_5, (1.0F - local4921_4) * 12.0F * newSettingRenderContext.scale
         );
         Render2D.popAlpha();
      }
   }

   void updateState3() {
      if (AutoBuySettingsScreen.getInstance().strVal != null) {
         if (this.settingRenderContext != null
            && AutoBuySettingsScreen.getInstance().map2.get(AutoBuySettingsScreen.getInstance().strVal) != null
            && (
               this.settingRenderContext.textSetting == AutoBuySettingsScreen.getInstance().map2.get(AutoBuySettingsScreen.getInstance().strVal).nested03744
                  || this.settingRenderContext.textSetting == AutoBuySettingsScreen.getInstance().map2.get(AutoBuySettingsScreen.getInstance().strVal).nested03745
            )) {
            this.settingRenderContext.textSetting = null;
         }

         AutoBuySettingsScreen.getInstance().strVal = null;
         AutoBuySettingsScreen.getInstance().timeMillis2 = System.currentTimeMillis();
      }
   }
}
