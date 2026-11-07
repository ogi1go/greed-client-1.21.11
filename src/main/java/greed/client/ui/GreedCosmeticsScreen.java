package greed.client.ui;

import greed.client.cosmetic.CosmeticCatalog;
import greed.client.cosmetic.CosmeticCategory;
import greed.client.cosmetic.CosmeticEntry;
import greed.client.cosmetic.CosmeticPreviewAtlas;
import greed.client.cosmetic.CosmeticStore;
import greed.client.util.Easings;
import greed.client.render.FontFace;
import greed.client.module.misc.GlobalsModule;
import greed.client.module.render.InterfaceModule;
import greed.client.util.GuiScale;
import greed.client.render.FontText;
import greed.client.util.Animation;
import greed.client.ui.UiTheme;
import greed.client.cosmetic.CosmeticAccess;
import greed.client.render.ShapeDrawing;
import greed.client.ui.hud.HudRenderContext;
import greed.client.ui.hud.HudStyle;
import greed.client.render.Render2D;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

public final class GreedCosmeticsScreen extends Screen {
   private static final CosmeticCategory[] recoveredClass0304Array = new CosmeticCategory[]{
      CosmeticCategory.HAT,
      CosmeticCategory.ACCESSORY,
      CosmeticCategory.WINGS,
      CosmeticCategory.SHIELD,
      CosmeticCategory.SWORD,
      CosmeticCategory.MACE,
      CosmeticCategory.PET,
      CosmeticCategory.BODYWEAR
   };
   private final Animation animator = new Animation();
   private final Animation animator2 = new Animation();
   private final Animation[] animatorArray = new Animation[recoveredClass0304Array.length];
   private final Animation[] animatorArray2 = new Animation[7];
   private final int[] intValArray = new int[recoveredClass0304Array.length];
   private int intVal = 0;
   private int intVal2 = -1;
   private boolean flag;
   private float floatVal;
   private float floatVal2;
   private float floatVal3;

   @Override
   protected void init() {
      this.flag = false;
      this.animator.animateTo(1.0, 0.22, Easings.EASE_OUT_QUART);
      this.animator2.resetTo(0.0);
      this.animator2.animateTo(1.0, 0.28, Easings.EASE_OUT_QUART);
   }

   @Override
   public boolean mouseScrolled(double local2848_1, double local2848_3, double local2848_5, double local2848_7) {
      int local2848_9 = this.options(recoveredClass0304Array[this.intVal]).size();
      if (local2848_9 <= 7) {
         return true;
      }

      this.intValArray[this.intVal] = Math.max(0, Math.min(local2848_9 - 7, this.intValArray[this.intVal] - (int)Math.signum(local2848_7)));
      return true;
   }

   private void drawCaption(float local2849_1, float local2849_2, float local2849_3) {
      CosmeticCategory cosmeticCategory = recoveredClass0304Array[this.intVal];
      List<CosmeticEntry> local2849_5 = this.options(cosmeticCategory);
      CosmeticEntry cosmeticEntry = CosmeticStore.getCosmeticEntry(cosmeticCategory);
      String local2849_7;
      if (this.intVal2 >= 0) {
         CosmeticEntry cosmeticEntry2 = local2849_5.get(this.intValArray[this.intVal] + this.intVal2);
         if (cosmeticEntry2 == null) {
            local2849_7 = cosmeticEntry == null ? "Ничего не надето" : "Снять " + cosmeticEntry.name();
         } else {
            local2849_7 = cosmeticEntry2.name()
               + (!CosmeticAccess.matchesCriteria(cosmeticEntry2) ? " · только бета" : (CosmeticStore.matchesCriteria(cosmeticEntry2) ? " · надето" : ""));
         }
      } else if (local2849_5.size() <= 1) {
         local2849_7 = "Здесь пока пусто";
      } else {
         local2849_7 = cosmeticEntry == null ? "Ничего не надето" : "Надето: " + cosmeticEntry.name();
      }

      float local2849_8 = this.floatVal2 + 220.0F * this.floatVal3;
      FontText.processData9(
         FontFace.SEMI_BOLD,
         cosmeticCategory.formatText(),
         this.floatVal,
         local2849_8,
         13.0F * this.floatVal3,
         UiTheme.computeInt(UiTheme.intVal, local2849_3)
      );
      FontText.processData9(
         FontFace.SEMI,
         local2849_7,
         this.floatVal,
         local2849_8 + 17.0F * this.floatVal3,
         8.5F * this.floatVal3,
         UiTheme.computeInt(UiTheme.intVal, local2849_3 * 0.62F)
      );
      FontText.processData9(
         FontFace.SEMI,
         "ЛКМ — надеть     ПКМ — снять     Колёсико — листать     Esc — закрыть",
         this.floatVal,
         local2849_8 + 32.0F * this.floatVal3,
         7.5F * this.floatVal3,
         UiTheme.computeInt(UiTheme.intVal, local2849_3 * 0.4F)
      );
   }

   private List<CosmeticEntry> options(CosmeticCategory cosmeticCategory) {
      ArrayList<CosmeticEntry> local2850_2 = new ArrayList<>();
      local2850_2.add(null);
      local2850_2.addAll(CosmeticCatalog.getList(cosmeticCategory));
      return local2850_2;
   }

   private void beginClosing() {
      if (!this.flag) {
         this.flag = true;
         this.animator.animateTo(0.0, 0.16, Easings.EASE_OUT_QUART);
      }
   }

   private int optionCount(int local2852_1) {
      return Math.min(7, this.options(recoveredClass0304Array[local2852_1]).size() - this.intValArray[local2852_1]);
   }

   @Override
   public void renderBackground(DrawContext drawContext, int local2853_2, int local2853_3, float local2853_4) {
   }

   private void drawOptions(float local2854_1) {
      List<CosmeticEntry> local2854_2 = this.options(recoveredClass0304Array[this.intVal]);
      int local2854_3 = this.intValArray[this.intVal];
      int local2854_4 = this.optionCount(this.intVal);
      float local2854_5 = this.animator2.floatValue();
      CosmeticEntry cosmeticEntry = CosmeticStore.getCosmeticEntry(recoveredClass0304Array[this.intVal]);

      for (int index = 0; index < 7; index++) {
         Animation animation = this.animatorArray2[index];
         animation.update();
         animation.animateTo(index == this.intVal2 ? 1.0 : 0.0, 0.14, Easings.EASE_OUT_QUART);
         if (index < local2854_4) {
            CosmeticEntry cosmeticEntry2 = local2854_2.get(local2854_3 + index);
            float[] floatArray = this.optionCenter(index, local2854_4);
            float local2854_11 = Math.max(0.0F, Math.min(1.0F, local2854_5 * 1.6F - index * 0.08F));
            float local2854_12 = animation.floatValue();
            boolean local2854_13 = cosmeticEntry2 == null ? cosmeticEntry == null : CosmeticStore.matchesCriteria(cosmeticEntry2);
            boolean local2854_14 = cosmeticEntry2 != null && !CosmeticAccess.matchesCriteria(cosmeticEntry2);
            float local2854_15 = local2854_1 * local2854_11 * (local2854_14 ? 0.28F : 0.55F + 0.45F * Math.max(local2854_12, local2854_13 ? 1.0F : 0.0F));
            float local2854_16 = 35.0F * this.floatVal3 * (1.0F + 0.14F * local2854_12 + (local2854_13 ? 0.04F : 0.0F));
            if (cosmeticEntry2 == null) {
               float local2854_17 = 7.0F * this.floatVal3;
               Render2D.drawCircleOutline(
                  floatArray[0],
                  floatArray[1],
                  local2854_17,
                  1.2F * this.floatVal3,
                  UiTheme.computeInt(UiTheme.intVal, local2854_15)
               );
               Render2D.drawLine(
                  floatArray[0] - local2854_17 * 0.62F,
                  floatArray[1] + local2854_17 * 0.62F,
                  floatArray[0] + local2854_17 * 0.62F,
                  floatArray[1] - local2854_17 * 0.62F,
                  1.2F * this.floatVal3,
                  UiTheme.computeInt(UiTheme.intVal, local2854_15)
               );
            } else {
               CosmeticPreviewAtlas.AtlasEntry local2854_17 = CosmeticPreviewAtlas.getNested0499(cosmeticEntry2);
               if (local2854_17 != null) {
                  Render2D.drawImageUv(
                     local2854_17.atlasTexture(),
                     floatArray[0] - local2854_16 / 2.0F,
                     floatArray[1] - local2854_16 / 2.0F,
                     local2854_16,
                     local2854_16,
                     0.0F,
                     local2854_17.u0(),
                     local2854_17.v0(),
                     local2854_17.u1(),
                     local2854_17.v1(),
                     UiTheme.computeInt(UiTheme.intVal, local2854_15)
                  );
               } else {
                  Render2D.drawCircleOutline(
                     floatArray[0],
                     floatArray[1],
                     8.0F * this.floatVal3,
                     1.0F * this.floatVal3,
                     UiTheme.computeInt(UiTheme.intVal, local2854_15 * 0.6F)
                  );
               }
            }

            if (local2854_13) {
               float local2854_17 = 185.0F * this.floatVal3 + 23.0F * this.floatVal3;
               Render2D.drawCircle(
                  this.floatVal + (float)Math.cos(floatArray[2]) * local2854_17,
                  this.floatVal2 + (float)Math.sin(floatArray[2]) * local2854_17,
                  1.8F * this.floatVal3,
                  UiTheme.computeInt(UiTheme.intVal7, local2854_1 * local2854_11)
               );
            }
         }
      }

      if (local2854_2.size() > 7) {
         float local2854_7 = 185.0F * this.floatVal3;
         double local2854_8 = this.optionCenter(0, local2854_4)[2] - Math.toRadians(8.0);
         double local2854_10 = this.optionCenter(local2854_4 - 1, local2854_4)[2] + Math.toRadians(8.0);
         if (local2854_3 > 0) {
            Render2D.drawCircle(
               this.floatVal + (float)Math.cos(local2854_8) * local2854_7,
               this.floatVal2 + (float)Math.sin(local2854_8) * local2854_7,
               1.3F * this.floatVal3,
               UiTheme.computeInt(UiTheme.intVal, local2854_1 * 0.4F)
            );
         }

         if (local2854_3 + local2854_4 < local2854_2.size()) {
            Render2D.drawCircle(
               this.floatVal + (float)Math.cos(local2854_10) * local2854_7,
               this.floatVal2 + (float)Math.sin(local2854_10) * local2854_7,
               1.3F * this.floatVal3,
               UiTheme.computeInt(UiTheme.intVal, local2854_1 * 0.4F)
            );
         }
      }
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   @Override
   public boolean keyPressed(KeyInput keyInput) {
      if (keyInput.key() != 256
         && (!GlobalsModule.getInstance().keybindSetting.isBound() || keyInput.key() != GlobalsModule.getInstance().keybindSetting.getKeyCode())) {
         return super.keyPressed(keyInput);
      }

      this.beginClosing();
      return true;
   }

   private void updateHover(float local2857_1, float local2857_2) {
      this.intVal2 = -1;
      if (!this.flag) {
         float local2857_3 = local2857_1 - this.floatVal;
         float local2857_4 = local2857_2 - this.floatVal2;
         float local2857_5 = (float)Math.hypot(local2857_3, local2857_4);
         if (local2857_5 > 60.0F * this.floatVal3 && local2857_5 < 162.0F * this.floatVal3) {
            double local2857_6 = Math.toDegrees(Math.atan2(local2857_4, local2857_3)) + 90.0 + 22.5;

            while (local2857_6 < 0.0) {
               local2857_6 += 360.0;
            }

            int local2857_8 = (int)(local2857_6 % 360.0 / 45.0);
            if (local2857_8 != this.intVal) {
               this.intVal = local2857_8;
               this.animator2.resetTo(0.0);
               this.animator2.animateTo(1.0, 0.28, Easings.EASE_OUT_QUART);
            }
         }

         int local2857_6 = this.optionCount(this.intVal);

         for (int index = 0; index < local2857_6; index++) {
            float[] floatArray = this.optionCenter(index, local2857_6);
            if (Math.hypot(local2857_1 - floatArray[0], local2857_2 - floatArray[1]) < 19.0F * this.floatVal3) {
               this.intVal2 = index;
            }
         }
      }
   }

   public static float modelAlpha() {
      return MinecraftClient.getInstance().currentScreen instanceof GreedCosmeticsScreen greedCosmeticsScreen ? greedCosmeticsScreen.animator.floatValue() : 1.0F;
   }

   private float[] optionCenter(int local2859_1, int local2859_2) {
      double local2859_3 = Math.toRadians(-90.0 + this.intVal * 45.0 + (local2859_1 - (local2859_2 - 1) / 2.0) * 12.5);
      float local2859_5 = 185.0F * this.floatVal3 * (0.94F + 0.06F * this.animator2.floatValue());
      return new float[]{
         this.floatVal + (float)Math.cos(local2859_3) * local2859_5, this.floatVal2 + (float)Math.sin(local2859_3) * local2859_5, (float)local2859_3
      };
   }

   @Override
   public void close() {
      this.beginClosing();
   }

   @Override
   public void render(DrawContext drawContext, int local2861_2, int local2861_3, float local2861_4) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient.player != null) {
         InterfaceModule.getInstance().updateState();
         float local2861_6 = space();
         float local2861_7 = this.width * local2861_6;
         float local2861_8 = this.height * local2861_6;
         this.floatVal3 = Math.min(local2861_7 / 960.0F, local2861_8 / 540.0F);
         this.floatVal = local2861_7 / 2.0F;
         this.floatVal2 = local2861_8 / 2.0F;
         float local2861_9 = local2861_2 * local2861_6;
         float local2861_10 = local2861_3 * local2861_6;
         this.animator.update();
         this.animator2.update();
         float local2861_11 = this.animator.floatValue();
         if (this.flag && local2861_11 <= 0.01F) {
            minecraftClient.execute(() -> {
               if (minecraftClient.currentScreen == this) {
                  minecraftClient.setScreen(null);
               }
            });
         } else {
            this.updateHover(local2861_9, local2861_10);
            Render2D.flush(drawContext);
            HudRenderContext.renderElement(drawContext);
            float local2861_12 = 0.92F + 0.08F * local2861_11;
            float local2861_13 = 107.0F * this.floatVal3 * local2861_12;
            float local2861_14 = 144.0F * this.floatVal3 * local2861_12;

            for (int index = 0; index < recoveredClass0304Array.length; index++) {
               Animation animation = this.animatorArray[index];
               animation.update();
               animation.animateTo(index == this.intVal ? 1.0 : 0.0, 0.16, Easings.EASE_OUT_QUART);
               float local2861_17 = animation.floatValue();
               float local2861_18 = -90.0F + index * 45.0F - 22.5F;
               ShapeDrawing.processData(
                  ShapeDrawing.TypeOption.MIDDLE,
                  this.floatVal,
                  this.floatVal2,
                  local2861_13,
                  local2861_14,
                  local2861_18,
                  45.0F,
                  3.5F * this.floatVal3,
                  5.0F * this.floatVal3,
                  UiTheme.computeInt2(HudStyle.computeInt3(), UiTheme.intVal, 0.16F * local2861_17),
                  0.8F * (1.0F - 0.28F * local2861_17),
                  local2861_11
               );
               double local2861_19 = Math.toRadians(-90.0 + index * 45.0);
               float local2861_21 = this.floatVal + (float)Math.cos(local2861_19) * (local2861_13 + local2861_14) / 2.0F;
               float local2861_22 = this.floatVal2 + (float)Math.sin(local2861_19) * (local2861_13 + local2861_14) / 2.0F;
               float local2861_23 = 16.0F * this.floatVal3;
               Render2D.drawImageSized(
                  recoveredClass0304Array[index].formatText4(),
                  local2861_21 - local2861_23 / 2.0F,
                  local2861_22 - local2861_23 / 2.0F,
                  local2861_23,
                  local2861_23,
                  0.0F,
                  UiTheme.computeInt(UiTheme.intVal, local2861_11 * (0.5F + 0.5F * local2861_17))
               );
               if (CosmeticStore.getCosmeticEntry(recoveredClass0304Array[index]) != null && local2861_17 < 0.99F) {
                  Render2D.drawCircle(
                     this.floatVal + (float)Math.cos(local2861_19) * (local2861_14 + 8.0F * this.floatVal3),
                     this.floatVal2 + (float)Math.sin(local2861_19) * (local2861_14 + 8.0F * this.floatVal3),
                     1.8F * this.floatVal3,
                     UiTheme.computeInt(UiTheme.intVal7, local2861_11 * (1.0F - local2861_17))
                  );
               }
            }

            this.drawOptions(local2861_11);
            this.drawCaption(local2861_7, local2861_8, local2861_11);
            Render2D.endFrame();
            if (local2861_11 > 0.01F) {
               int local2861_15 = (int)(60.0F * this.floatVal3 / local2861_6);
               int local2861_16 = (int)(95.0F * this.floatVal3 / local2861_6);
               int local2861_17 = (int)(this.floatVal / local2861_6);
               int local2861_18 = (int)(this.floatVal2 / local2861_6);
               InventoryScreen.drawEntity(
                  drawContext,
                  local2861_17 - local2861_15,
                  local2861_18 - local2861_16,
                  local2861_17 + local2861_15,
                  local2861_18 + local2861_16,
                  (int)(82.0F * this.floatVal3 / local2861_6),
                  0.0625F,
                  local2861_2,
                  local2861_3,
                  minecraftClient.player
               );
            }
         }
      }
   }

   @Override
   public boolean mouseClicked(Click click, boolean local2864_2) {
      if (this.flag) {
         return true;
      }

      CosmeticCategory cosmeticCategory = recoveredClass0304Array[this.intVal];
      if (click.button() == 1) {
         CosmeticStore.processData2(cosmeticCategory);
         return true;
      }

      if (click.button() == 0 && this.intVal2 >= 0) {
         CosmeticEntry cosmeticEntry = this.options(cosmeticCategory).get(this.intValArray[this.intVal] + this.intVal2);
         if (cosmeticEntry == null) {
            CosmeticStore.processData2(cosmeticCategory);
         } else {
            CosmeticStore.processData(cosmeticEntry);
         }

         return true;
      } else {
         return super.mouseClicked(click, local2864_2);
      }
   }

   public GreedCosmeticsScreen() {
      super(Text.literal("Greed Cosmetics"));

      for (int index = 0; index < recoveredClass0304Array.length; index++) {
         this.animatorArray[index] = new Animation();
      }

      for (int index2 = 0; index2 < 7; index2++) {
         this.animatorArray2[index2] = new Animation();
      }
   }

   private static float space() {
      return GuiScale.computeInt() / GuiScale.computeFloat();
   }
}
