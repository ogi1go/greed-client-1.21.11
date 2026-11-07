package greed.client.ui.hud;

import greed.client.render.FontText;
import greed.client.util.GuiScale;
import greed.client.util.ItemCooldownAccess;
import greed.client.render.ItemRenderCache;
import greed.client.ui.UiTheme;

import greed.client.util.Animation;

import greed.client.util.Easings;
import greed.client.render.FontFace;
import greed.client.module.render.InterfaceModule;
import greed.client.util.ItemDrawOptions;
import greed.client.render.Render2D;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

public final class CooldownsHud {
   private static final Item[] exampleItems = new Item[]{
      Items.ENDER_EYE,
      Items.ENDER_PEARL,
      Items.SUGAR,
      Items.MACE,
      Items.ENCHANTED_GOLDEN_APPLE,
      Items.TRIDENT,
      Items.CROSSBOW,
      Items.DRIED_KELP,
      Items.NETHERITE_SCRAP
   };
   private static final CooldownsHud.SubContext exampleContext = new CooldownsHud.SubContext(Items.ENDER_EYE);
   private static long exampleStartMillis;
   private static int exampleIndex;
   private static final Map<Item, CooldownsHud.SubContext> contextsByItem = new LinkedHashMap<>();
   private static final Map<Item, CooldownsHud.SubContext2> externalCooldownsByItem = new LinkedHashMap<>();
   private static final List<CooldownsHud.SubContext> activeContexts = new ArrayList<>();
   private static final Animation hudAlphaAnimation = new Animation();
   private static final Animation panelWidthAnimation = new Animation();
   private static final Animation panelScaleAnimation = new Animation();
   private static final Animation nameWidthAnimation = new Animation();
   private static float animatedPanelWidth;
   private static float animatedHeight;

   static float getRemainingSeconds(Item item) {
      CooldownsHud.SubContext2 cooldown = externalCooldownsByItem.get(item);
      return cooldown == null ? 0.0F : Math.max(0.0F, cooldown.durationSeconds - (float)(System.currentTimeMillis() - cooldown.startMillis) / 1000.0F);
   }

   private static TextColor getTextColor(ItemStack itemStack) {
      TextColor[] textColorArray = new TextColor[]{null};
      itemStack.getName().visit((style, textContent) -> {
         if (textColorArray[0] == null && style.getColor() != null && !textContent.isBlank()) {
            textColorArray[0] = style.getColor();
         }

         return Optional.empty();
      }, Style.EMPTY);
      return textColorArray[0] != null ? textColorArray[0] : TextColor.fromFormatting(itemStack.getRarity().getFormatting());
   }

   private static void updateTimerText(CooldownsHud.SubContext context, int remainingTenths) {
      if (remainingTenths != context.cachedTenths) {
         context.cachedTenths = remainingTenths;
         int totalSeconds = (remainingTenths + 9) / 10;
         int minutes = totalSeconds / 60;
         int seconds = totalSeconds % 60;
         context.remainingTimeText = minutes > 0 ? minutes + "м " + seconds + "с" : seconds + "с";
         context.timeTextWidth = FontText.computeFloat(FontFace.SEMI_BOLD, context.remainingTimeText, 6.0F);
      }
   }

   private static ItemStack getItemStack(MinecraftClient minecraftClient, Item item, CooldownsHud.SubContext2 cacheEntry) {
      int changeCount = minecraftClient.player.getInventory().getChangeCount();
      long nowMillis = System.currentTimeMillis();
      if (cacheEntry.cachedStack == null
         || cacheEntry.player != minecraftClient.player
         || cacheEntry.inventoryChangeCount != changeCount
         || nowMillis - cacheEntry.lastCacheMillis >= 1000L) {
         cacheEntry.cachedStack = findMatchingStack(minecraftClient, item, cacheEntry.label);
         cacheEntry.player = minecraftClient.player;
         cacheEntry.inventoryChangeCount = changeCount;
         cacheEntry.lastCacheMillis = nowMillis;
      }

      return cacheEntry.cachedStack;
   }

   private CooldownsHud() {
   }

   public static void processData2(String label, Item item, float durationSeconds) {
      CooldownsHud.SubContext2 existing = externalCooldownsByItem.get(item);
      if (existing == null || !((float)(System.currentTimeMillis() - existing.startMillis) < existing.durationSeconds * 1000.0F)) {
         CooldownsHud.SubContext2 entry = new CooldownsHud.SubContext2();
         entry.label = label;
         entry.startMillis = System.currentTimeMillis();
         entry.durationSeconds = durationSeconds;
         externalCooldownsByItem.put(item, entry);
      }
   }

   private static void refreshContexts(MinecraftClient minecraftClient) {
      activeContexts.clear();
      contextsByItem.values().forEach(context -> context.visible = false);
      if (minecraftClient.player != null) {
         for (int index = 0; index < minecraftClient.player.getInventory().size(); index++) {
            trackInventoryCooldown(minecraftClient, minecraftClient.player.getInventory().getStack(index));
         }

         trackInventoryCooldown(minecraftClient, minecraftClient.player.getOffHandStack());
         externalCooldownsByItem.values().removeIf(cooldown -> (float)(System.currentTimeMillis() - cooldown.startMillis) >= cooldown.durationSeconds * 1000.0F);

         for (Entry<Item, CooldownsHud.SubContext2> entry : externalCooldownsByItem.entrySet()) {
            CooldownsHud.SubContext2 value = entry.getValue();
            float remainingSeconds = Math.max(0.0F, value.durationSeconds - (float)(System.currentTimeMillis() - value.startMillis) / 1000.0F);
            CooldownsHud.SubContext context = contextsByItem.computeIfAbsent(entry.getKey(), CooldownsHud.SubContext::new);
            context.displayStack = getItemStack(minecraftClient, entry.getKey(), value);
            context.displayName = value.label;
            context.customLabel = true;
            updateTimerText(context, Math.round(remainingSeconds * 10.0F));
            context.visible = true;
         }
      }

      for (CooldownsHud.SubContext context : contextsByItem.values()) {
         if (context.visible || context.visibilityAnimation.floatValue() > 0.001) {
            activeContexts.add(context);
         }
      }

      contextsByItem.entrySet()
         .removeIf(
            entry -> !entry.getValue().visible
               && entry.getValue().visibilityAnimation.floatValue() <= 0.001
               && entry.getValue().visibilityAnimation.isFinished()
         );
      exampleContext.visible = activeContexts.isEmpty() && HudRenderContext.checkState();
      if (exampleContext.visible) {
         long nowMillis = System.currentTimeMillis();
         float remainingSeconds = (float)(10000L - (nowMillis - exampleStartMillis)) / 1000.0F;
         if (remainingSeconds <= 0.0F) {
            exampleStartMillis = nowMillis;
            exampleIndex++;
            remainingSeconds = 10.0F;
         }

         Item item = exampleItems[exampleIndex % exampleItems.length];
         if (exampleContext.displayStack == null || !exampleContext.displayStack.isOf(item)) {
            exampleContext.displayStack = new ItemStack(item);
         }

         exampleContext.displayName = "Example Cooldown";
         exampleContext.customLabel = true;
         updateTimerText(exampleContext, Math.round(Math.max(1.0F, remainingSeconds) * 10.0F));
      } else {
         exampleStartMillis = 0L;
      }

      if (exampleContext.visible || exampleContext.visibilityAnimation.floatValue() > 0.001) {
         activeContexts.add(exampleContext);
      }

      for (CooldownsHud.SubContext context : activeContexts) {
         context.labelWidth = computeLabelWidth(context);
      }

      activeContexts.sort(Comparator.<CooldownsHud.SubContext>comparingDouble(context -> context.labelWidth).reversed());

      for (CooldownsHud.SubContext context : activeContexts) {
         context.visibilityAnimation.update();
         context.visibilityAnimation.animateTo(context.visible ? 1.0 : 0.0, 0.6, Easings.EASE_OUT_QUART);
      }
   }

   static {
      panelWidthAnimation.resetTo(1.0);
   }

   private static ItemStack findMatchingStack(MinecraftClient minecraftClient, Item item, String matchText) {
      ItemStack itemStack = ItemStack.EMPTY;
      String[] stringArray = matchText.toLowerCase(Locale.ROOT).split("/");

      for (int index = 0; index < minecraftClient.player.getInventory().size() + 1; index++) {
         ItemStack itemStack2 = index < minecraftClient.player.getInventory().size()
            ? minecraftClient.player.getInventory().getStack(index)
            : minecraftClient.player.getOffHandStack();
         if (!itemStack2.isEmpty() && itemStack2.isOf(item)) {
            if (itemStack.isEmpty()) {
               itemStack = itemStack2;
            }

            String stackName = Formatting.strip(itemStack2.getName().getString());
            if (stackName != null) {
               stackName = stackName.toLowerCase(Locale.ROOT);

               for (String searchPart : stringArray) {
                  if (!searchPart.isBlank() && stackName.contains(searchPart.trim())) {
                     return itemStack2.copy();
                  }
               }
            }
         }
      }

      return itemStack.isEmpty() ? new ItemStack(item) : itemStack.copy();
   }

   private static float computeLabelWidth(CooldownsHud.SubContext context) {
      float[] floatArray = new float[]{0.0F};
      getText(context).visit((style, textPart) -> {
         floatArray[0] += FontText.computeFloat(FontFace.SEMI_BOLD, textPart, 6.0F);
         return Optional.empty();
      }, Style.EMPTY);
      return floatArray[0];
   }

   public static float computeFloat3(MinecraftClient minecraftClient, float anchorX, float anchorY) {
      refreshContexts(minecraftClient);
      float headerWidth = 16.0F + FontText.computeFloat(FontFace.SEMI_BOLD, "Cooldowns", 6.0F) + 4.0F;
      float nameColumnWidth = 0.0F;
      float timeColumnWidth = 0.0F;
      float contentHeight = 13.0F;

      for (CooldownsHud.SubContext context : activeContexts) {
         nameColumnWidth = Math.max(nameColumnWidth, HudStyle.computeFloat(8.0F, context.timeTextWidth));
         timeColumnWidth = Math.max(timeColumnWidth, HudStyle.computeFloat4(context.labelWidth));
         contentHeight += 14.0F * context.visibilityAnimation.floatValue();
      }

      float columnsWidth = nameColumnWidth + (nameColumnWidth > 0.0F && timeColumnWidth > 0.0F ? 2.0F : 0.0F) + timeColumnWidth;
      float panelWidth = Math.max(headerWidth, Math.max(columnsWidth, 1.0F));
      boolean visible = !activeContexts.isEmpty() || HudRenderContext.checkState();
      hudAlphaAnimation.update();
      hudAlphaAnimation.animateTo(visible ? 1.0 : 0.0, 0.4, Easings.EASE_OUT_QUART);
      panelWidthAnimation.update();
      if (!activeContexts.isEmpty()) {
         panelWidthAnimation.animateTo(Math.max(panelWidth, 1.0F), 0.6, Easings.EASE_OUT_QUART);
      }

      panelScaleAnimation.update();
      if (!activeContexts.isEmpty()) {
         panelScaleAnimation.animateTo(Math.max(headerWidth, 1.0F), 0.6, Easings.EASE_OUT_QUART);
      }

      nameWidthAnimation.update();
      if (!activeContexts.isEmpty()) {
         nameWidthAnimation.animateTo(Math.max(nameColumnWidth, 0.0F), 0.6, Easings.EASE_OUT_QUART);
      }

      animatedPanelWidth = panelWidthAnimation.floatValue();
      animatedHeight = Math.max(contentHeight, 1.0F);
      float fade = hudAlphaAnimation.floatValue();
      if (fade <= 0.001F) {
         return 0.0F;
      }

      float[] floatArray = HudManager.computeFloat5("cooldowns", anchorX - animatedPanelWidth, anchorY);
      float panelX = floatArray[0];
      float panelY = floatArray[1];
      HudManager.processData9("cooldowns", panelX, panelY, animatedPanelWidth, animatedHeight);
      HudRenderContext.processData4(panelX + animatedPanelWidth / 2.0F, panelY + animatedHeight / 2.0F, fade);
      HudStyle.processData7(
              panelX, panelY, headerWidth, fade, HudIcons.COOLDOWNS, "Cooldowns"
      );
      float rowY = panelY + 15.0F;

      for (CooldownsHud.SubContext context : activeContexts) {
         renderContextRow(panelX, rowY, fade, context);
         rowY += 13.0F * context.visibilityAnimation.floatValue();
      }

      HudRenderContext.updateState();
      return animatedHeight * fade + 8.0F * fade;
   }

   private static void trackInventoryCooldown(MinecraftClient minecraftClient, ItemStack itemStack) {
      if (!itemStack.isEmpty() && !itemStack.isOf(Items.BOW) && !itemStack.isOf(Items.CROSSBOW) && !itemStack.isOf(Items.TRIDENT)) {
         if (!(minecraftClient.player.getItemCooldownManager().getCooldownProgress(itemStack, 0.0F) <= 0.001F)) {
            CooldownsHud.SubContext context = contextsByItem.computeIfAbsent(itemStack.getItem(), CooldownsHud.SubContext::new);
            if (!context.visible) {
               int remainingTicks = ((ItemCooldownAccess)minecraftClient.player.getItemCooldownManager()).remainingCooldownTicks(itemStack);
               updateTimerText(context, Math.max(0, Math.round(remainingTicks / 2.0F)));
               if (context.displayStack == null || context.displayStack.getItem() != itemStack.getItem()) {
                  context.displayStack = itemStack.copy();
                  context.displayName = itemStack.getName().getString();
               }

               context.customLabel = false;
               context.visible = true;
            }
         }
      }
   }

   public static void processData5(MinecraftClient minecraftClient, float screenCenterX, float hotbarY) {
      if (minecraftClient.player != null) {
         float guiScale = GuiScale.computeInt() / GuiScale.computeFloat();

         for (int index = 0; index < 9; index++) {
            ItemStack stack = minecraftClient.player.getInventory().getStack(index);
            if (!stack.isEmpty() && !stack.isOf(Items.BOW) && !stack.isOf(Items.CROSSBOW) && !stack.isOf(Items.TRIDENT)) {
               float cooldownSeconds = Math.max(
                  ((ItemCooldownAccess)minecraftClient.player.getItemCooldownManager()).remainingCooldownTicks(stack) / 20.0F,
                  getRemainingSeconds(stack.getItem())
               );
               if (!(cooldownSeconds <= 0.05F)) {
                  float slotX = screenCenterX / 2.0F - 91.0F + 3.0F + index * 20.0F;
                  Render2D.drawRect(
                     slotX * guiScale,
                     (hotbarY - 19.0F) * guiScale,
                     16.0F * guiScale,
                     16.0F * guiScale,
                     2.0F * guiScale,
                     Math.round(InterfaceModule.getInstance().cooldownSlotDarkening.floatValue() * 2.55F) << 24
                  );
                  String secondsText = String.valueOf((int)Math.ceil(cooldownSeconds));
                  float textSize = 5.0F * guiScale;
                  float textOffset = 0.7F * guiScale;
                  float textX = (slotX + 1.0F) * guiScale;
                  float textY = (hotbarY - 19.0F + 2.5F) * guiScale - textSize / 2.0F;
                  int textColor = cooldownSeconds > 30.0F ? -50384 : (cooldownSeconds <= 5.0F ? -12517568 : (cooldownSeconds < 15.0F ? -20440 : -34264));
                  FontText.processData3(FontFace.BOLD, secondsText, textX - textOffset, textY, textSize, -16777216);
                  FontText.processData3(FontFace.BOLD, secondsText, textX + textOffset, textY, textSize, -16777216);
                  FontText.processData3(FontFace.BOLD, secondsText, textX, textY - textOffset, textSize, -16777216);
                  FontText.processData3(FontFace.BOLD, secondsText, textX, textY + textOffset, textSize, -16777216);
                  FontText.processData3(FontFace.BOLD, secondsText, textX, textY, textSize, textColor);
               }
            }
         }
      }
   }

   private static Text getText(CooldownsHud.SubContext context) {
      if (context.displayStack == null || context.displayStack.isEmpty()) {
         return Text.literal(context.displayName);
      } else {
         return context.customLabel
            ? Text.literal(context.displayName).styled(style -> style.withColor(getTextColor(context.displayStack)))
            : context.displayStack
               .getName()
               .copy()
               .styled(style2 -> style2.getColor() == null ? style2.withColor(context.displayStack.getRarity().getFormatting()) : style2);
      }
   }

   private static void renderContextRow(float panelX, float rowY, float fade, CooldownsHud.SubContext context) {
      float rowAlpha = context.visibilityAnimation.floatValue() * fade;
      if (!(rowAlpha <= 0.001F)) {
         float boxInsetWidth = HudStyle.computeFloat2(rowAlpha);
         float timeTextWidth = context.timeTextWidth;
         float timeBoxWidth = HudStyle.computeFloat(8.0F, timeTextWidth);
         float nameBoxWidth = HudStyle.computeFloat4(context.labelWidth);
         float nameBoxX = panelX - boxInsetWidth;
         float timeBoxX = panelX + nameWidthAnimation.floatValue() + 2.0F - boxInsetWidth;
         float rowTextY = HudStyle.computeFloat3(rowY);
         HudStyle.processData6(nameBoxX, rowY, timeBoxWidth, 11.0F, rowAlpha);
         float iconX = nameBoxX + 2.0F + 8.0F + 1.0F;
         float innerX = iconX + 0.25F;
         HudStyle.processData(iconX, rowY + 5.5F, rowAlpha, 3.0F);
         if (context.displayStack != null && !context.displayStack.isEmpty()) {
            float iconCenterX = Math.round((nameBoxX + (innerX - nameBoxX - 8.0F) / 2.0F) * 2.0F) / 2.0F;
            float iconCenterY = Math.round((rowY + 5.5F - 4.0F) * 2.0F) / 2.0F;
            ItemRenderCache.processData(context.displayStack, iconCenterX, iconCenterY, 8.0F, ItemDrawOptions.noDecorations(rowAlpha));
         }

         FontText.processData3(
            FontFace.SEMI_BOLD,
            context.remainingTimeText,
            innerX + (nameBoxX + timeBoxWidth - innerX - timeTextWidth) / 2.0F,
            rowTextY,
            6.0F,
            UiTheme.computeInt(HudStyle.computeInt4(1.0F), rowAlpha)
         );
         HudStyle.processData6(timeBoxX, rowY, nameBoxWidth, 11.0F, rowAlpha);
         int textColor = UiTheme.computeInt(HudStyle.computeInt4(1.0F), rowAlpha);
         float[] floatArray = new float[]{timeBoxX + 4.0F};
         getText(context).visit((style, textPart) -> {
            if (!textPart.isEmpty()) {
               int segmentColor = style.getColor() != null ? style.getColor().getRgb() & 16777215 | textColor & 0xFF000000 : textColor;
               FontText.processData3(FontFace.SEMI_BOLD, textPart, floatArray[0], rowTextY, 6.0F, segmentColor);
               floatArray[0] += FontText.computeFloat(FontFace.SEMI_BOLD, textPart, 6.0F);
            }

            return Optional.empty();
         }, Style.EMPTY);
      }
   }

   private static final class SubContext extends DraggableHudElement {
      String displayName = "";
      String remainingTimeText = "";
      float timeTextWidth;
      float labelWidth;
      int cachedTenths = -1;
      boolean customLabel;
      ItemStack displayStack;

      SubContext(Item item) {
      }
   }

   private static final class SubContext2 {
      String label = "";
      long startMillis;
      float durationSeconds;
      ItemStack cachedStack;
      Object player;
      int inventoryChangeCount;
      long lastCacheMillis;
   }
}
