package greed.client.ui.hud;

import greed.client.render.FontText;
import greed.client.util.ItemCooldownAccess;
import greed.client.render.ItemRenderCache;
import greed.client.util.PotionPreset;
import greed.client.ui.UiTheme;

import greed.client.util.Animation;

import greed.client.util.Colors;
import greed.mixin.ContainerComponentMixin;
import greed.client.util.Easings;
import greed.client.render.FontFace;
import greed.client.util.ItemDrawOptions;
import greed.client.module.misc.PotionCombinerModule;
import greed.client.module.player.ServerHelperModule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public final class ServerKeybindsHud {
   private static final Map<String, ServerKeybindsHud.SubContext> subContextsByKey = new HashMap<>();
   private static final List<ServerKeybindsHud.SubContext> visibleSubContexts = new ArrayList<>();
   private static final Animation animation = new Animation();
   private static float contentWidth;
   private static float contentHeight;

   private static float computeLabelWidth(ServerKeybindsHud.SubContext context) {
      float progress = context.animation.floatValue();
      float primaryWidth = FontText.computeFloat(FontFace.SEMI, context.labelText, 6.0F);
      float cooldownWidth = context.cooldownText.isEmpty() ? 0.0F : FontText.computeFloat(FontFace.SEMI, context.cooldownText, 6.0F);
      return primaryWidth * (1.0F - progress) + cooldownWidth * progress;
   }

   private static void collectSubContextItems(MinecraftClient minecraftClient, ServerKeybindsHud.SubContext context, ServerHelperModule.SubContext keybind) {
      int totalCount = 0;
      ItemStack itemStack = ItemStack.EMPTY;

      for (int index = 0; index < minecraftClient.player.getInventory().size(); index++) {
         ItemStack stack = minecraftClient.player.getInventory().getStack(index);
         if (keybind.matchesCriteria2(stack)) {
            totalCount += stack.getCount();
            if (itemStack.isEmpty()) {
               itemStack = stack;
            }
         }

         if (stack.get(DataComponentTypes.BUNDLE_CONTENTS) != null) {
            for (ItemStack itemStack2 : stack.get(DataComponentTypes.BUNDLE_CONTENTS).iterate()) {
               if (keybind.matchesCriteria2(itemStack2)) {
                  totalCount += itemStack2.getCount();
                  if (itemStack.isEmpty()) {
                     itemStack = itemStack2;
                  }
               }
            }
         }

         if (Block.getBlockFromItem(stack.getItem()) instanceof ShulkerBoxBlock && stack.get(DataComponentTypes.CONTAINER) != null) {
            for (ItemStack itemStack3 : ((ContainerComponentMixin)(Object)stack.get(DataComponentTypes.CONTAINER)).getItems()) {
               if (keybind.matchesCriteria2(itemStack3)) {
                  totalCount += itemStack3.getCount();
                  if (itemStack.isEmpty()) {
                     itemStack = itemStack3;
                  }
               }

               if (itemStack3.get(DataComponentTypes.BUNDLE_CONTENTS) != null) {
                  for (ItemStack itemStack4 : itemStack3.get(DataComponentTypes.BUNDLE_CONTENTS).iterate()) {
                     if (keybind.matchesCriteria2(itemStack4)) {
                        totalCount += itemStack4.getCount();
                        if (itemStack.isEmpty()) {
                           itemStack = itemStack4;
                        }
                     }
                  }
               }
            }
         }
      }

      if (!itemStack.isEmpty()) {
         context.displayStack = itemStack.copy();
      }

      context.itemCount = totalCount;
      context.cooldownActive = false;
      ItemStack itemStack5 = !itemStack.isEmpty()
         ? itemStack
         : (keybind.getItem2() != null ? new ItemStack(keybind.getItem2()) : ItemStack.EMPTY);
      if (!itemStack5.isEmpty() && minecraftClient.player.getItemCooldownManager().getCooldownProgress(itemStack5, 0.0F) > 0.001F) {
         int remainingTicks = ((ItemCooldownAccess)minecraftClient.player.getItemCooldownManager()).remainingCooldownTicks(itemStack5);
         context.cooldownProgress = minecraftClient.player.getItemCooldownManager().getCooldownProgress(itemStack5, 0.0F);
         int tenthsOfSecond = Math.max(0, Math.round(remainingTicks / 2.0F));
         context.cooldownText = tenthsOfSecond / 10 + "." + tenthsOfSecond % 10 + "с";
         context.cooldownActive = true;
         if (itemStack.isEmpty()) {
            context.displayStack = itemStack5.copy();
         }
      }

      context.visible = totalCount > 0 || context.cooldownActive;
   }

   private ServerKeybindsHud() {
   }

   private static float computeRowWidth(ServerKeybindsHud.SubContext context) {
      return HudStyle.computeFloat(8.0F + (context.comboStacks == null ? 0.0F : (context.comboStacks.size() - 1) * 9.5F), computeLabelWidth(context));
   }

   public static float computeFloat3(MinecraftClient minecraftClient, float anchorX, float anchorY) {
      rebuildSubContexts(minecraftClient);
      float maxColumnHeight = 0.0F;
      float currentColumnY = 0.0F;
      int columnCount = 1;
      boolean hasEntries = false;

      for (ServerKeybindsHud.SubContext context : visibleSubContexts) {
         float rowWidth = computeRowWidth(context);
         if (currentColumnY > 0.0F && currentColumnY + rowWidth > 150.0F) {
            columnCount++;
            currentColumnY = 0.0F;
         }

         context.sortIndex = columnCount - 1;
         context.yOffset = currentColumnY;
         currentColumnY += rowWidth + 2.0F;
         maxColumnHeight = Math.max(maxColumnHeight, currentColumnY - 2.0F);
         hasEntries = true;
      }

      float totalHeight = hasEntries ? 2.0F + columnCount * 11.0F + (columnCount - 1) * 2.0F : 0.0F;
      animation.update();
      animation.animateTo(visibleSubContexts.isEmpty() ? 0.0 : 1.0, 0.4, Easings.EASE_OUT_QUART);
      if (hasEntries) {
         contentWidth = Math.max(maxColumnHeight, 1.0F);
         contentHeight = Math.max(totalHeight, 1.0F);
      }

      float visibilityProgress = animation.floatValue();
      if (visibilityProgress <= 0.001F) {
         return 0.0F;
      }

      float[] floatArray = HudManager.computeFloat5("serverbinds", anchorX, anchorY);
      float boxX = floatArray[0] - contentWidth / 2.0F;
      float boxY = floatArray[1] - contentHeight;
      HudManager.processData6("serverbinds", boxX, boxY, contentWidth, contentHeight, floatArray[0], floatArray[1]);
      HudRenderContext.processData4(boxX + contentWidth / 2.0F, boxY + contentHeight / 2.0F, visibilityProgress);

      for (ServerKeybindsHud.SubContext context : visibleSubContexts) {
         float rowAlpha = context.visibilityAnimation.floatValue() * visibilityProgress;
         if (!(rowAlpha <= 0.001F)) {
            renderRow(boxX + context.yOffset, boxY + context.sortIndex * 13.0F, rowAlpha, context);
         }
      }

      HudRenderContext.updateState();
      return contentHeight * visibilityProgress;
   }

   private static void renderRow(float x, float y, float alpha, ServerKeybindsHud.SubContext context) {
      float rowWidth = computeRowWidth(context);
      float labelWidth = computeLabelWidth(context);
      HudStyle.processData6(x, y, rowWidth, 11.0F, alpha);
      float labelX = x + 2.0F + 8.0F + (context.comboStacks == null ? 0.0F : (context.comboStacks.size() - 1) * 9.5F) + 1.0F;
      float labelOffsetX = labelX + 0.25F;
      HudStyle.processData(labelX, y + 5.5F, alpha, 3.0F);
      float iconCenterX = x
         + (labelOffsetX - x - 8.0F - (context.comboStacks == null ? 0.0F : (context.comboStacks.size() - 1) * 9.5F)) / 2.0F;
      if (context.comboStacks != null) {
         for (int index = 0; index < context.comboStacks.size(); index++) {
            ItemRenderCache.processData(
               context.comboStacks.get(index),
               Math.round((iconCenterX + index * 9.5F) * 2.0F) / 2.0F,
               Math.round((y + 5.5F - 4.0F) * 2.0F) / 2.0F,
               8.0F,
               ItemDrawOptions.noDecorations(alpha)
            );
         }
      } else if (context.displayStack != null && !context.displayStack.isEmpty()) {
         float iconX = Math.round(iconCenterX * 2.0F) / 2.0F;
         float iconY = Math.round((y + 5.5F - 4.0F) * 2.0F) / 2.0F;
         ItemRenderCache.processData(context.displayStack, iconX, iconY, 8.0F, ItemDrawOptions.noDecorations(alpha));
      }

      if (context.itemCount > 0) {
         String countText = Integer.toString(context.itemCount);
         float countScale = 5.0F;
         float countWidth = FontText.computeFloat(FontFace.SEMI, countText, countScale);
         float countX = iconCenterX + 8.0F - countWidth;
         float countY = y + 5.5F + 4.0F - countScale;
         int countShadowColor = Colors.computeInt2(HudStyle.computeInt4(1.0F), -16777216, 0.1F);
         FontText.processData3(
            FontFace.SEMI, countText, countX, countY, countScale, UiTheme.computeInt(countShadowColor, alpha)
         );
      }

      float labelCenteredX = labelOffsetX + (x + rowWidth - labelOffsetX - labelWidth) / 2.0F;
      float textY = HudStyle.computeFloat3(y);
      float labelProgress = context.animation.floatValue();
      if (labelProgress < 0.999F) {
         FontText.processData3(
            FontFace.SEMI,
            context.labelText,
            labelCenteredX,
            textY,
            6.0F,
            UiTheme.computeInt(HudStyle.computeInt4(1.0F), alpha * (1.0F - labelProgress))
         );
      }

      if (labelProgress > 0.001F && !context.cooldownText.isEmpty()) {
         int cooldownColor = Colors.computeInt2(-16711936, -65536, context.cooldownProgress);
         FontText.processData3(
            FontFace.SEMI,
            context.cooldownText,
            labelCenteredX,
            textY,
            6.0F,
            UiTheme.computeInt(cooldownColor, alpha * labelProgress)
         );
      }
   }

   private static void rebuildSubContexts(MinecraftClient minecraftClient) {
      visibleSubContexts.clear();
      subContextsByKey.values().forEach(context -> context.visible = false);
      if (minecraftClient.player != null) {
         for (ServerHelperModule.SubContext keybind : ServerHelperModule.getInstance().getArrayList()) {
            if (keybind.getKeybindSetting().isBound() && keybind.getKeybindSetting().isVisible()) {
               ServerKeybindsHud.SubContext context = subContextsByKey.computeIfAbsent(keybind.getKeybindSetting().getKeyName(), ServerKeybindsHud.SubContext::new);
               collectSubContextItems(minecraftClient, context, keybind);
            }
         }

         for (PotionPreset.SubContext potion : PotionPreset.getInstance().list) {
            if (potion.flag && potion.keybindSetting.isBound() && PotionCombinerModule.getInstance().isEnabled()) {
               collectPotionItems(
                  minecraftClient,
                  subContextsByKey.computeIfAbsent(
                     "combo:" + potion.strVal + ":" + potion.keybindSetting.getKeyName(),
                     key -> new ServerKeybindsHud.SubContext(potion.keybindSetting.getKeyName())
                  ),
                  potion
               );
            }
         }
      }

      boolean anyVisible = subContextsByKey.values().stream().anyMatch(context -> context.visible);
      if (!anyVisible && HudRenderContext.checkState()) {
         Object[][] objectArrayArray = new Object[][]{
            {"R", Items.NETHERITE_SCRAP, 4}, {"F", Items.ENDER_EYE, 16}, {"V", Items.PHANTOM_MEMBRANE, 8}, {"X", Items.FIRE_CHARGE, 12}
         };

         for (Object[] objectArray : objectArrayArray) {
            ServerKeybindsHud.SubContext context = subContextsByKey.computeIfAbsent(
               "preview:" + objectArray[0], key -> new ServerKeybindsHud.SubContext((String)objectArray[0])
            );
            context.displayStack = new ItemStack((Item)objectArray[1]);
            context.itemCount = (Integer)objectArray[2];
            context.cooldownActive = false;
            context.visible = true;
         }
      }

      for (ServerKeybindsHud.SubContext context : subContextsByKey.values()) {
         if (context.visible || context.visibilityAnimation.floatValue() > 0.001) {
            visibleSubContexts.add(context);
         }
      }

      subContextsByKey.entrySet()
         .removeIf(
            entry -> !entry.getValue().visible
               && entry.getValue().visibilityAnimation.floatValue() <= 0.001
               && entry.getValue().visibilityAnimation.isFinished()
         );
      visibleSubContexts.sort(
         Comparator.<ServerKeybindsHud.SubContext>comparingInt(context -> context.cooldownActive ? 1 : 0)
            .thenComparingDouble(label -> -(8.0F + FontText.computeFloat(FontFace.SEMI, label.labelText, 6.0F)))
      );

      for (ServerKeybindsHud.SubContext context : visibleSubContexts) {
         context.visibilityAnimation.update();
         context.visibilityAnimation.animateTo(context.visible ? 1.0 : 0.0, 0.6, Easings.EASE_OUT_QUART);
         context.animation.update();
         context.animation.animateTo(context.cooldownActive ? 1.0 : 0.0, 0.35, Easings.EASE_OUT_QUART);
      }
   }

   private static void collectPotionItems(MinecraftClient minecraftClient, ServerKeybindsHud.SubContext context, PotionPreset.SubContext potion) {
      int totalCount = 0;

      for (int index = 0; index < minecraftClient.player.getInventory().size(); index++) {
         ItemStack stack = minecraftClient.player.getInventory().getStack(index);
         if (!stack.isEmpty()) {
            for (ServerHelperModule.SubContext keybind : ServerHelperModule.getInstance().getArrayList()) {
               if (keybind.getItem2() == null
                  && keybind.matchesCriteria2(stack)
                  && !potion.list2.stream().noneMatch(entry -> entry.strVal3.equals(keybind.getKeybindSetting().name))) {
                  totalCount += stack.getCount();
                  break;
               }
            }
         }
      }

      context.comboStacks = new ArrayList<>();

      for (PotionPreset.SubContext2 ingredient : potion.list2) {
         context.comboStacks.add(PotionPreset.getItemStack(ingredient.strVal3));
      }

      context.displayStack = context.comboStacks.isEmpty() ? ItemStack.EMPTY : context.comboStacks.get(0);
      context.itemCount = totalCount;
      context.cooldownActive = false;
      context.visible = totalCount > 0;
   }

   private static final class SubContext extends DraggableHudElement {
      final String labelText;
      ItemStack displayStack;
      ArrayList<ItemStack> comboStacks;
      int itemCount;
      boolean cooldownActive;
      float cooldownProgress;
      String cooldownText = "";
      final Animation animation = new Animation();

      SubContext(String label) {
         this.labelText = label;
      }
   }
}
