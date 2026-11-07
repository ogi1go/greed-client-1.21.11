package greed.client.ui;

import greed.client.util.Easings;
import greed.client.render.FontFace;
import greed.client.util.ItemDrawOptions;
import greed.client.module.player.PotionThrowerModule;
import greed.client.util.GuiScale;
import greed.client.render.FontText;
import greed.client.ui.ScreenClosingTransition;
import greed.client.render.ItemRenderCache;
import greed.client.util.ServerItemType;
import greed.client.util.Animation;
import greed.client.ui.UiTheme;
import greed.client.render.ShapeDrawing;
import greed.client.storage.AutoBuyItem;
import greed.client.ui.hud.HudRenderContext;
import greed.client.ui.hud.HudStyle;
import greed.client.render.Render2D;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class PotionWheelScreen extends Screen implements ScreenClosingTransition.SubInterface {
   private final ArrayList<Animation> slotAnimations = new ArrayList<>();
   private final Animation wheelAnimator = new Animation();
   private final Animation pickerAnimator = new Animation();
   private final ArrayList<Animation> pickerAnimations = new ArrayList<>();
   private final HashMap<String, ItemStack> potionStackCache = new HashMap<>();
   private int hoveredSlotIndex = -1;
   private int draggedSlotIndex = -1;
   private int pickerSlotIndex = -1;
   private int hoveredPickerRow = -1;
   private float wheelRadius = 92.0F;
   private float cursorX;
   private float cursorY;
   private float pickerX;
   private float pickerY;
   private float pickerWidth;
   private float pickerHeight;
   private boolean closingFlag;

   private static PotionThrowerModule thrower() {
      return PotionThrowerModule.getInstance();
   }

   @Override
   protected void init() {
      ScreenClosingTransition.processData3(this);
      this.closingFlag = false;
      this.hoveredSlotIndex = -1;
      this.draggedSlotIndex = -1;
      this.pickerSlotIndex = -1;
      this.wheelAnimator.animateTo(1.0, 0.22, Easings.EASE_OUT_QUAD);
   }

   @Override
   public void renderBackground(DrawContext drawContext, int mouseX, int mouseY, float delta) {
   }

   private void drawSlot(int slotIndex, float centerX, float centerY, float scale, float alpha, float hoverProgress) {
      AutoBuyItem autoBuyItem = thrower().getAutoBuyItem(slotIndex);
      if (autoBuyItem == null) {
         float iconSize = 20.0F * scale;
         FontText.processData9(
            FontFace.BOLD,
            "+",
            centerX,
            centerY - iconSize / 2.0F,
            iconSize,
            UiTheme.computeInt(HudStyle.computeInt2(0.4F + 0.3F * hoverProgress), alpha)
         );
      } else {
         float iconSize = 20.0F * scale;
         float iconX = Math.round((centerX - iconSize / 2.0F) * 2.0F) / 2.0F;
         float iconY = Math.round((centerY - iconSize / 2.0F) * 2.0F) / 2.0F;
         ItemRenderCache.processData(this.potionStack(autoBuyItem), iconX, iconY, iconSize, ItemDrawOptions.noDecorations(alpha));
      }
   }

   private void drawPicker(float x, float anchorY, float viewportHeight, float alpha) {
      ArrayList<AutoBuyItem> items = thrower().getArrayList2();
      ArrayList<String> rows = this.pickerRows(items);
      float maxRowWidth = 0.0F;

      for (String row : rows) {
         maxRowWidth = Math.max(maxRowWidth, FontText.computeFloat(FontFace.SEMI_BOLD, row, 6.0F));
      }

      this.pickerWidth = 16.0F + maxRowWidth + 6.0F;
      this.pickerHeight = rows.size() * 13.0F + 4.0F;
      this.pickerX = x;
      this.pickerY = Math.max(4.0F, Math.min(viewportHeight - this.pickerHeight - 4.0F, anchorY - this.pickerHeight / 2.0F));
      float fadeInset = 6.0F * (1.0F - alpha);
      float panelX = this.pickerX + fadeInset;
      HudStyle.processData4(panelX, this.pickerY, this.pickerWidth, this.pickerHeight, alpha, 6.0F);
      this.hoveredPickerRow = -1;

      while (this.pickerAnimations.size() < rows.size()) {
         this.pickerAnimations.add(new Animation());
      }

      float pickerProgress = this.pickerAnimator.floatValue();

      for (int index = 0; index < rows.size(); index++) {
         float rowY = this.pickerY + 2.0F + index * 13.0F;
         boolean rowHovered = this.pickerOpen()
            && this.cursorX >= panelX
            && this.cursorX <= panelX + this.pickerWidth
            && this.cursorY >= rowY
            && this.cursorY < rowY + 13.0F;
         if (rowHovered) {
            this.hoveredPickerRow = index;
         }

         Animation animation = this.pickerAnimations.get(index);
         animation.update();
         animation.animateTo(rowHovered ? 1.0 : 0.0, 0.16, Easings.EASE_OUT_QUAD);
         float rowHoverAnim = animation.floatValue();
         float reveal = (float)Easings.EASE_OUT_QUART
            .apply(Math.max(0.0F, Math.min(1.0F, pickerProgress * (1.0F + rows.size() * 0.12F) - index * 0.12F)));
         float rowAlpha = alpha * reveal * (0.72F + 0.28F * rowHoverAnim);
         float rowIconX = panelX + 4.0F + 6.0F * (1.0F - reveal) + 2.0F * rowHoverAnim;
         boolean isExtraRow = index >= items.size();
         if (!isExtraRow) {
            ItemRenderCache.processData(
               this.potionStack(items.get(index)),
               rowIconX,
               rowY + 2.5F,
               8.0F,
               ItemDrawOptions.noDecorations(alpha * reveal)
            );
         }

         if (isExtraRow) {
            FontText.processData3(
               FontFace.SEMI_BOLD,
               rows.get(index),
               rowIconX + 8.0F + 4.0F,
               rowY + 6.5F - 3.0F,
               6.0F,
               UiTheme.computeInt(UiTheme.computeInt2(HudStyle.computeInt2(1.0F), -42386, rowHoverAnim), rowAlpha)
            );
         } else {
            FontText.processData5(
               FontFace.SEMI_BOLD,
               this.potionName(items.get(index)),
               rowIconX + 8.0F + 4.0F,
               rowY + 6.5F - 3.0F,
               6.0F,
               UiTheme.computeInt(HudStyle.computeInt4(1.0F), rowAlpha)
            );
         }
      }
   }

   private ArrayList<String> pickerRows(ArrayList<AutoBuyItem> items) {
      ArrayList<String> rows = new ArrayList<>();

      for (AutoBuyItem autoBuyItem : items) {
         rows.add(autoBuyItem.name());
      }

      if (this.pickerSlotIndex != -1 && this.pickerSlotIndex < slots().size() && !slots().get(this.pickerSlotIndex).isEmpty()) {
         rows.add("Очистить");
      }

      return rows;
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   @Override
   public boolean renderClosing(DrawContext drawContext) {
      this.drawWheel(drawContext, false);
      return this.wheelAnimator.floatValue() > 0.01F;
   }

   private void closePicker() {
      this.pickerSlotIndex = -1;
      this.hoveredPickerRow = -1;
      if (!thrower().checkState()) {
         this.startClosing();
      }
   }

   private ItemStack potionStack(AutoBuyItem autoBuyItem) {
      return this.potionStackCache
         .computeIfAbsent(
            autoBuyItem.name(),
            name -> {
               ServerItemType serverItemType = ServerItemType.getServerItemType(name);
               ItemStack itemStack = autoBuyItem.displayStack().copy();
               if (serverItemType != null) {
                  itemStack.set(
                     DataComponentTypes.POTION_CONTENTS,
                     new PotionContentsComponent(Optional.empty(), Optional.of(serverItemType.computeInt() | 0xFF000000), List.of(), Optional.empty())
                  );
               }

               return itemStack;
            }
         );
   }

   private void clampCursor(MinecraftClient minecraftClient, float viewportWidth, float viewportHeight) {
      if (minecraftClient.isWindowFocused()) {
         Window window = minecraftClient.getWindow();
         double scaleX = (double)window.getFramebufferWidth() / viewportWidth;
         double scaleY = (double)window.getFramebufferHeight() / viewportHeight;
         double centerX = viewportWidth / 2.0 * scaleX;
         double centerY = viewportHeight / 2.0 * scaleY;
         double deltaX = minecraftClient.mouse.getX() - centerX;
         double deltaY = minecraftClient.mouse.getY() - centerY;
         double maxDistance = (this.wheelRadius - 1.0) * scaleX;
         double distance = Math.hypot(deltaX, deltaY);
         if (distance > maxDistance) {
            double correction = maxDistance / distance;
            GLFW.glfwSetCursorPos(window.getHandle(), centerX + deltaX * correction, centerY + deltaY * correction);
         }
      }
   }

   public boolean closing() {
      return this.closingFlag;
   }

   private void openPicker(int slotIndex) {
      this.pickerSlotIndex = slotIndex;
      this.draggedSlotIndex = -1;
   }

   private static float space() {
      return GuiScale.computeInt() / GuiScale.computeFloat();
   }

   public PotionWheelScreen() {
      super(Text.literal("PotionWheel"));
   }

   @Override
   public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
      if (!this.closingFlag) {
         this.drawWheel(drawContext, true);
      }
   }

   public void releaseHeldSlot(int targetSlot) {
      if (this.draggedSlotIndex != -1) {
         if (targetSlot != -1 && targetSlot != this.draggedSlotIndex && targetSlot < slots().size() && this.draggedSlotIndex < slots().size()) {
            Collections.swap(slots(), this.draggedSlotIndex, targetSlot);
            thrower().updateState();
         }

         this.draggedSlotIndex = -1;
      }
   }

   public int draggedSlot() {
      return this.draggedSlotIndex;
   }

   private Text potionName(AutoBuyItem autoBuyItem) {
      ServerItemType serverItemType = ServerItemType.getServerItemType(autoBuyItem.name());
      return serverItemType != null ? serverItemType.getText() : Text.literal(autoBuyItem.name());
   }

   public boolean pickerOpen() {
      return this.pickerSlotIndex != -1;
   }

   private int hoveredAt(float x, float y, float centerX, float centerY, int slotCount) {
      float deltaX = x - centerX;
      float deltaY = y - centerY;
      if (Math.hypot(deltaX, deltaY) > this.wheelRadius * 1.25F) {
         return -1;
      }

      double anglePerSlot = 360.0 / slotCount;
      double angle = Math.toDegrees(Math.atan2(deltaY, deltaX)) + 90.0 + anglePerSlot / 2.0;

      while (angle < 0.0) {
         angle += 360.0;
      }

      angle %= 360.0;
      return Math.min(slotCount - 1, (int)(angle / anglePerSlot));
   }

   public void startClosing() {
      if (!this.closingFlag) {
         this.closingFlag = true;
         this.hoveredSlotIndex = -1;
         this.draggedSlotIndex = -1;
         this.pickerSlotIndex = -1;
         this.wheelAnimator.animateTo(0.0, 0.22, Easings.EASE_OUT_QUAD);
         ScreenClosingTransition.processData(this);
         if (this.client != null && this.client.currentScreen == this) {
            this.client.setScreen(null);
         }
      }
   }

   private static ArrayList<String> slots() {
      return thrower().getArrayList();
   }

   private void drawWheel(DrawContext drawContext, boolean interactive) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient.player != null) {
         Render2D.flush(drawContext);
         HudRenderContext.renderElement(drawContext);
         float viewportWidth = this.width * space();
         float viewportHeight = this.height * space();
         float centerX = viewportWidth / 2.0F;
         float centerY = viewportHeight / 2.0F;
         this.wheelRadius = Math.min(140.0F, Math.max(56.0F, Math.min(viewportWidth, viewportHeight) * 0.17F));
         if (interactive && !this.pickerOpen()) {
            this.clampCursor(minecraftClient, viewportWidth, viewportHeight);
         }

         this.wheelAnimator.update();
         this.pickerAnimator.update();
         this.pickerAnimator.animateTo(this.pickerOpen() ? 1.0 : 0.0, this.pickerOpen() ? 0.32 : 0.16, Easings.EASE_OUT_QUAD);
         this.cursorX = (float)minecraftClient.mouse.getScaledX(minecraftClient.getWindow()) * space();
         this.cursorY = (float)minecraftClient.mouse.getScaledY(minecraftClient.getWindow()) * space();
         int slotCount = slots().size();

         while (this.slotAnimations.size() < slotCount) {
            this.slotAnimations.add(new Animation());
         }

         this.hoveredSlotIndex = interactive && !this.closingFlag && !this.pickerOpen()
            ? this.hoveredAt(this.cursorX, this.cursorY, centerX, centerY, slotCount)
            : -1;
         float wheelProgress = this.wheelAnimator.floatValue();
         if (!(wheelProgress <= 0.004F)) {
            float animatedRadius = this.wheelRadius * (0.88F + 0.12F * wheelProgress);
            float contentScale = animatedRadius / 92.0F;
            float anglePerSlot = 360.0F / slotCount;

            for (int index = 0; index < slotCount; index++) {
               Animation animation = this.slotAnimations.get(index);
               animation.update();
               animation.animateTo(
                  index != this.hoveredSlotIndex && index != this.draggedSlotIndex && index != this.pickerSlotIndex ? 0.0 : 1.0,
                  0.18,
                  Easings.EASE_OUT_QUAD
               );
               float slotHoverAnim = animation.floatValue();
               float slotAngle = -90.0F - anglePerSlot / 2.0F + anglePerSlot * index;
               double slotAngleRad = Math.toRadians(slotAngle + anglePerSlot / 2.0F);
               float slotScale = 1.0F + 0.03F * slotHoverAnim;
               HudRenderContext.processData3(centerX, centerY, slotScale);
               ShapeDrawing.processData3(
                  ShapeDrawing.TypeOption.MIDDLE,
                  centerX,
                  centerY,
                  animatedRadius,
                  slotAngle,
                  anglePerSlot,
                  6.0F * contentScale / slotScale,
                  10.0F * contentScale,
                  index == this.draggedSlotIndex
                     ? UiTheme.computeInt2(HudStyle.computeInt3(), HudStyle.computeInt(1.0F), 0.35F)
                     : HudStyle.computeInt3(),
                  0.8F,
                  wheelProgress
               );
               float slotIconX = centerX + (float)Math.cos(slotAngleRad) * animatedRadius * 0.62F;
               float slotIconY = centerY + (float)Math.sin(slotAngleRad) * animatedRadius * 0.62F;
               if (index != this.draggedSlotIndex) {
                  this.drawSlot(index, slotIconX, slotIconY, contentScale, wheelProgress, slotHoverAnim);
               }

               HudRenderContext.updateState();
            }

            if (this.draggedSlotIndex != -1) {
               AutoBuyItem autoBuyItem = thrower().getAutoBuyItem(this.draggedSlotIndex);
               if (autoBuyItem != null) {
                  float dragIconSize = 20.0F * contentScale;
                  ItemRenderCache.processData(
                     this.potionStack(autoBuyItem),
                     this.cursorX - dragIconSize / 2.0F,
                     this.cursorY - dragIconSize / 2.0F,
                     dragIconSize,
                     ItemDrawOptions.noDecorations(wheelProgress)
                  );
               }
            }

            int activeSlot = this.draggedSlotIndex != -1 ? this.draggedSlotIndex : this.hoveredSlotIndex;
            AutoBuyItem autoBuyItem2 = thrower().getAutoBuyItem(activeSlot);
            if (autoBuyItem2 != null) {
               Text text = this.potionName(autoBuyItem2);
               FontText.processData5(
                  FontFace.SEMI_BOLD,
                  text,
                  centerX - FontText.computeFloat(FontFace.SEMI_BOLD, text.getString(), 6.0F) / 2.0F,
                  centerY + animatedRadius + 6.0F,
                  6.0F,
                  UiTheme.computeInt(HudStyle.computeInt4(1.0F), wheelProgress)
               );
            }

            float pickerProgress = this.pickerAnimator.floatValue();
            if (pickerProgress > 0.004F) {
               this.drawPicker(centerX + animatedRadius + 12.0F, centerY, viewportHeight, wheelProgress * pickerProgress);
            } else {
               this.hoveredPickerRow = -1;
            }

            Render2D.endFrame();
         }
      }
   }

   @Override
   public void close() {
      if (this.pickerOpen()) {
         this.closePicker();
      } else {
         this.startClosing();
      }
   }

   @Override
   public boolean mouseReleased(Click click) {
      if (click.button() == 0 && this.draggedSlotIndex != -1) {
         this.releaseHeldSlot(this.hoveredSlotIndex);
         return true;
      } else {
         return super.mouseReleased(click);
      }
   }

   public int hoveredSlot() {
      return this.hoveredSlotIndex;
   }

   @Override
   public boolean mouseClicked(Click click, boolean doubleClick) {
      if (this.closingFlag) {
         return true;
      }

      if (this.pickerOpen()) {
         if (click.button() == 0 && this.hoveredPickerRow != -1) {
            ArrayList<AutoBuyItem> items = thrower().getArrayList2();
            ArrayList<String> rows = this.pickerRows(items);
            if (this.hoveredPickerRow < items.size()) {
               slots().set(this.pickerSlotIndex, items.get(this.hoveredPickerRow).name());
            } else if (rows.get(this.hoveredPickerRow).equals("Очистить")) {
               slots().set(this.pickerSlotIndex, "");
            }

            thrower().updateState();
         }

         this.closePicker();
         return true;
      } else {
         if (this.hoveredSlotIndex != -1) {
            if (click.button() == 1) {
               this.openPicker(this.hoveredSlotIndex);
               return true;
            }

            if (click.button() == 0) {
               if (thrower().getAutoBuyItem(this.hoveredSlotIndex) != null) {
                  this.draggedSlotIndex = this.hoveredSlotIndex;
               } else {
                  this.openPicker(this.hoveredSlotIndex);
               }

               return true;
            }
         }

         return super.mouseClicked(click, doubleClick);
      }
   }
}
