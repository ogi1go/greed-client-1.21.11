package greed.client.ui;

import greed.client.util.Easings;
import greed.client.render.FontFace;
import greed.client.util.FormattedText;
import greed.client.util.ItemDrawOptions;
import greed.client.util.GuiScale;
import greed.client.render.FontText;
import greed.client.ui.ScreenClosingTransition;
import greed.client.render.ItemRenderCache;
import greed.client.util.Animation;
import greed.client.ui.UiTheme;
import greed.client.render.ShapeDrawing;
import greed.client.ui.hud.HudRenderContext;
import greed.client.ui.hud.HudStyle;
import greed.client.render.Render2D;

import java.util.function.IntConsumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class SwapWheelScreen extends Screen implements ScreenClosingTransition.SubInterface {
   private final ItemStack[] itemStackArray2;
   private final IntConsumer intConsumer2;
   private final Animation[] recoveredClass0165Array;
   private final Animation animation = new Animation();
   private int intVal = -1;
   private float floatVal = 92.0F;
   private boolean flag;

   private int sliceColor(float local3463_1) {
      int local3463_2 = HudStyle.computeInt3();
      return local3463_1 > 0.004F ? UiTheme.computeInt2(local3463_2, UiTheme.intVal, 0.16F * local3463_1) : local3463_2;
   }

   private void clampCursor(MinecraftClient minecraftClient, float local3464_2, float local3464_3) {
      if (minecraftClient.isWindowFocused()) {
         Window window = minecraftClient.getWindow();
         double local3464_5 = (double)window.getFramebufferWidth() / local3464_2;
         double local3464_7 = (double)window.getFramebufferHeight() / local3464_3;
         double local3464_9 = local3464_2 / 2.0 * local3464_5;
         double local3464_11 = local3464_3 / 2.0 * local3464_7;
         double local3464_13 = minecraftClient.mouse.getX() - local3464_9;
         double local3464_15 = minecraftClient.mouse.getY() - local3464_11;
         double local3464_17 = (this.floatVal - 1.0) * local3464_5;
         double local3464_19 = Math.hypot(local3464_13, local3464_15);
         if (local3464_19 > local3464_17) {
            double local3464_21 = local3464_17 / local3464_19;
            GLFW.glfwSetCursorPos(window.getHandle(), local3464_9 + local3464_13 * local3464_21, local3464_11 + local3464_15 * local3464_21);
         }
      }
   }

   private int hoveredAt(float local3465_1, float local3465_2, float local3465_3, float local3465_4) {
      float local3465_5 = local3465_1 - local3465_3;
      float local3465_6 = local3465_2 - local3465_4;
      if (Math.hypot(local3465_5, local3465_6) > this.floatVal * 1.25F) {
         return -1;
      }

      double local3465_7 = 360.0 / this.itemStackArray2.length;
      double local3465_9 = Math.toDegrees(Math.atan2(local3465_6, local3465_5)) + 90.0 + local3465_7 / 2.0;

      while (local3465_9 < 0.0) {
         local3465_9 += 360.0;
      }

      local3465_9 %= 360.0;
      return Math.min(this.itemStackArray2.length - 1, (int)(local3465_9 / local3465_7));
   }

   @Override
   public void renderBackground(DrawContext drawContext, int local3466_2, int local3466_3, float local3466_4) {
   }

   private static float space() {
      return GuiScale.computeInt() / GuiScale.computeFloat();
   }

   @Override
   public void close() {
      this.startClosing();
   }

   @Override
   public void render(DrawContext drawContext, int local3469_2, int local3469_3, float local3469_4) {
      if (!this.flag) {
         this.drawWheel(drawContext, true);
      }
   }

   public boolean closing() {
      return this.flag;
   }

   @Override
   public boolean mouseClicked(Click click, boolean local3471_2) {
      if (this.intVal == -1 || click.button() != 1 && (click.button() != 0 || !this.itemStackArray2[this.intVal].isEmpty())) {
         return super.mouseClicked(click, local3471_2);
      }

      this.intConsumer2.accept(this.intVal);
      return true;
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   public int hoveredSlot() {
      return this.intVal;
   }

   private void drawWheel(DrawContext drawContext, boolean local3474_2) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient.player != null) {
         Render2D.flush(drawContext);
         HudRenderContext.renderElement(drawContext);
         float local3474_4 = this.width * space();
         float local3474_5 = this.height * space();
         float local3474_6 = local3474_4 / 2.0F;
         float local3474_7 = local3474_5 / 2.0F;
         this.floatVal = Math.min(140.0F, Math.max(56.0F, Math.min(local3474_4, local3474_5) * 0.17F));
         if (local3474_2) {
            this.clampCursor(minecraftClient, local3474_4, local3474_5);
         }

         this.animation.update();
         float local3474_8 = (float)minecraftClient.mouse.getScaledX(minecraftClient.getWindow()) * space();
         float local3474_9 = (float)minecraftClient.mouse.getScaledY(minecraftClient.getWindow()) * space();
         this.intVal = local3474_2 && !this.flag ? this.hoveredAt(local3474_8, local3474_9, local3474_6, local3474_7) : -1;
         float local3474_10 = this.animation.floatValue();
         if (!(local3474_10 <= 0.004F)) {
            float local3474_11 = this.floatVal * (0.88F + 0.12F * local3474_10);
            float local3474_12 = local3474_11 / 92.0F;
            float local3474_13 = 360.0F / this.itemStackArray2.length;

            for (int index = 0; index < this.itemStackArray2.length; index++) {
               this.recoveredClass0165Array[index].update();
               this.recoveredClass0165Array[index].animateTo(index == this.intVal ? 1.0 : 0.0, 0.18, Easings.EASE_OUT_QUAD);
               float local3474_15 = this.recoveredClass0165Array[index].floatValue();
               float local3474_16 = -90.0F - local3474_13 / 2.0F + local3474_13 * index;
               double local3474_17 = Math.toRadians(local3474_16 + local3474_13 / 2.0F);
               float local3474_19 = 1.0F + 0.03F * local3474_15;
               HudRenderContext.processData3(local3474_6, local3474_7, local3474_19);
               ShapeDrawing.processData3(
                  ShapeDrawing.TypeOption.MIDDLE,
                  local3474_6,
                  local3474_7,
                  local3474_11,
                  local3474_16,
                  local3474_13,
                  6.0F * local3474_12 / local3474_19,
                  10.0F * local3474_12,
                  this.sliceColor(local3474_15),
                  0.8F * (1.0F - 0.28F * local3474_15),
                  local3474_10
               );
               float local3474_20 = local3474_6 + (float)Math.cos(local3474_17) * local3474_11 * 0.62F;
               float local3474_21 = local3474_7 + (float)Math.sin(local3474_17) * local3474_11 * 0.62F;
               this.drawSlot(index, local3474_20, local3474_21, local3474_12, local3474_10, local3474_15);
               HudRenderContext.updateState();
            }

            if (this.intVal != -1 && !this.itemStackArray2[this.intVal].isEmpty()) {
               String local3474_14 = this.trimmedName(this.intVal);
               FontText.processData9(
                  FontFace.SEMI_BOLD,
                  local3474_14,
                  local3474_6,
                  local3474_7 + local3474_11 + 6.0F,
                  6.0F,
                  UiTheme.computeInt(HudStyle.computeInt4(1.0F), local3474_10)
               );
            }

            Render2D.endFrame();
         }
      }
   }

   @Override
   public boolean renderClosing(DrawContext drawContext) {
      this.drawWheel(drawContext, false);
      return this.animation.floatValue() > 0.01F;
   }

   public void startClosing() {
      if (!this.flag) {
         this.flag = true;
         this.intVal = -1;
         this.animation.animateTo(0.0, 0.22, Easings.EASE_OUT_QUAD);
         ScreenClosingTransition.processData(this);
         if (this.client != null && this.client.currentScreen == this) {
            this.client.setScreen(null);
         }
      }
   }

   public SwapWheelScreen(ItemStack[] itemStackArray, IntConsumer intConsumer) {
      super(Text.literal("SwapWheel"));
      this.itemStackArray2 = itemStackArray;
      this.intConsumer2 = intConsumer;
      this.recoveredClass0165Array = new Animation[itemStackArray.length];

      for (int index = 0; index < itemStackArray.length; index++) {
         this.recoveredClass0165Array[index] = new Animation();
      }
   }

   @Override
   protected void init() {
      ScreenClosingTransition.processData2(SwapWheelScreen.class);
      if (!this.flag) {
         this.animation.animateTo(1.0, 0.22, Easings.EASE_OUT_QUAD);
      }
   }

   private String trimmedName(int local3479_1) {
      String local3479_2 = FormattedText.formatText(this.itemStackArray2[local3479_1].getName().getString());
      return local3479_2.length() <= 16 ? local3479_2 : local3479_2.substring(0, 15) + "…";
   }

   public boolean pickerOpen() {
      return false;
   }

   private void drawSlot(int local3481_1, float local3481_2, float local3481_3, float local3481_4, float local3481_5, float local3481_6) {
      ItemStack itemStack = this.itemStackArray2[local3481_1];
      if (itemStack.isEmpty()) {
         float local3481_8 = 20.0F * local3481_4;
         FontText.processData9(
            FontFace.BOLD,
            "+",
            local3481_2,
            local3481_3 - local3481_8 / 2.0F,
            local3481_8,
            UiTheme.computeInt(HudStyle.computeInt2(0.4F + 0.3F * local3481_6), local3481_5)
         );
      } else {
         float local3481_8 = 20.0F * local3481_4;
         float local3481_9 = Math.round((local3481_2 - local3481_8 / 2.0F) * 2.0F) / 2.0F;
         float local3481_10 = Math.round((local3481_3 - local3481_8 / 2.0F) * 2.0F) / 2.0F;
         ItemRenderCache.processData(itemStack, local3481_9, local3481_10, local3481_8, ItemDrawOptions.noDecorations(local3481_5));
      }
   }
}
