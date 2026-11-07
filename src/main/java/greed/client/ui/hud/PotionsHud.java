package greed.client.ui.hud;

import greed.client.render.FontText;
import greed.client.util.ServerItemType;
import greed.client.render.ThemeColors;
import greed.client.ui.UiTheme;

import greed.client.util.Animation;

import greed.client.util.Easings;
import greed.client.render.FontFace;
import greed.client.module.render.InterfaceModule;
import greed.client.render.Render2D;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class PotionsHud {
   private static final Set<RegistryEntry<StatusEffect>> priorityEffects = Set.of(
      StatusEffects.STRENGTH,
      StatusEffects.SPEED,
      StatusEffects.FIRE_RESISTANCE,
      StatusEffects.RESISTANCE,
      StatusEffects.ABSORPTION,
      StatusEffects.REGENERATION
   );
   private static final List<RegistryEntry<StatusEffect>> exampleEffectRotation = List.of(
      StatusEffects.SPEED,
      StatusEffects.STRENGTH,
      StatusEffects.REGENERATION,
      StatusEffects.FIRE_RESISTANCE,
      StatusEffects.WATER_BREATHING,
      StatusEffects.POISON
   );
   private static final PotionsHud.SubContext examplePotionContext = new PotionsHud.SubContext("Example Potion", 0);
   private static long lastExampleRefreshMillis;
   private static int exampleIndex;
   private static final Map<ServerItemType, PotionsHud.SubContext> donationContexts = new EnumMap<>(ServerItemType.class);
   private static final Map<ServerItemType, Long> lastDonationSeenMillis = new EnumMap<>(ServerItemType.class);
   private static final Map<RegistryEntry<StatusEffect>, PotionsHud.SubContext> effectContexts = new IdentityHashMap<>();
   private static final List<PotionsHud.SubContext> activeContexts = new ArrayList<>();
   private static final Animation panelAlphaAnimation = new Animation();
   private static final Animation panelWidthAnimation = new Animation();
   private static final Animation titleAnimation = new Animation();
   private static final Animation contentWidthAnimation = new Animation();
   private static float panelWidth;
   private static float panelHeight;

   private static float measureHeaderWidth(PotionsHud.SubContext context) {
      return FontText.computeFloat(FontFace.SEMI_BOLD, context.name, 6.0F)
         + FontText.computeFloat(FontFace.SEMI_BOLD, context.amplifierText, 6.0F);
   }

   private static void refreshPotionList(MinecraftClient minecraftClient) {
      activeContexts.clear();
      effectContexts.values().forEach(context -> context.visible = false);
      if (minecraftClient.player != null) {
         for (StatusEffectInstance statusEffectInstance : minecraftClient.player.getStatusEffects()) {
            PotionsHud.SubContext context = effectContexts.computeIfAbsent(statusEffectInstance.getEffectType(), PotionsHud.SubContext::new);
            int currentAmplifier = statusEffectInstance.getAmplifier();
            if (currentAmplifier != context.lastAmplifier) {
               context.lastAmplifier = currentAmplifier;
               context.amplifierText = currentAmplifier > 0 ? " " + (currentAmplifier + 1) : "";
            }

            context.statusEffectInstance3 = statusEffectInstance;
            context.durationText = formatDurationText(statusEffectInstance);
            context.visible = true;
         }
      }

      refreshDonationPotions(minecraftClient);

      for (PotionsHud.SubContext effectContext : effectContexts.values()) {
         if (effectContext.visible || effectContext.visibilityAnimation.floatValue() > 0.001) {
            activeContexts.add(effectContext);
         }
      }

      for (PotionsHud.SubContext donationContext : donationContexts.values()) {
         if (donationContext.visible || donationContext.visibilityAnimation.floatValue() > 0.001) {
            activeContexts.add(donationContext);
         }
      }

      effectContexts.entrySet()
         .removeIf(
            entry -> !entry.getValue().visible
               && entry.getValue().visibilityAnimation.floatValue() <= 0.001
               && entry.getValue().visibilityAnimation.isFinished()
         );
      donationContexts.entrySet()
         .removeIf(
            entry -> !entry.getValue().visible
               && entry.getValue().visibilityAnimation.floatValue() <= 0.001
               && entry.getValue().visibilityAnimation.isFinished()
         );
      examplePotionContext.visible = activeContexts.isEmpty() && HudRenderContext.checkState();
      if (examplePotionContext.visible) {
         long nowMillis = System.currentTimeMillis();
         float remainingSeconds = (float)(10000L - (nowMillis - lastExampleRefreshMillis)) / 1000.0F;
         if (remainingSeconds <= 0.0F) {
            lastExampleRefreshMillis = nowMillis;
            exampleIndex++;
            remainingSeconds = 10.0F;
         }

         examplePotionContext.statusEffectInstance3 = new StatusEffectInstance(exampleEffectRotation.get(exampleIndex % exampleEffectRotation.size()), (int)Math.ceil(Math.max(1.0F, remainingSeconds)) * 20, 0);
         examplePotionContext.durationText = formatDurationText(examplePotionContext.statusEffectInstance3);
      } else {
         lastExampleRefreshMillis = 0L;
      }

      if (examplePotionContext.visible || examplePotionContext.visibilityAnimation.floatValue() > 0.001) {
         activeContexts.add(examplePotionContext);
      }

      activeContexts.sort(
         Comparator.<PotionsHud.SubContext, Integer>comparing(context -> context.sortPriority)
            .thenComparing(Comparator.<PotionsHud.SubContext>comparingDouble(context -> measureHeaderWidth(context)).reversed())
      );

      for (PotionsHud.SubContext context : activeContexts) {
         context.visibilityAnimation.update();
         context.visibilityAnimation.animateTo(context.visible ? 1.0 : 0.0, 0.6, Easings.EASE_OUT_QUART);
      }
   }

   static {
      panelWidthAnimation.resetTo(1.0);
   }

   private static String formatDurationText(StatusEffectInstance statusEffectInstance) {
      if (statusEffectInstance.isInfinite()) {
         return "INF";
      }

      int totalSeconds = statusEffectInstance.getDuration() / 20;
      int minutes = totalSeconds / 60;
      int seconds = totalSeconds % 60;
      return minutes > 0 ? minutes + "м " + seconds + "с" : seconds + "с";
   }

   public static float computeFloat2(MinecraftClient minecraftClient, float centerX, float centerY) {
      refreshPotionList(minecraftClient);
      float titleWidth = 16.0F + FontText.computeFloat(FontFace.SEMI_BOLD, "Potions", 6.0F) + 4.0F;
      float durationColumnWidth = 0.0F;
      float nameColumnWidth = 0.0F;
      float contentHeight = 13.0F;

      for (PotionsHud.SubContext context : activeContexts) {
         durationColumnWidth = Math.max(durationColumnWidth, HudStyle.computeFloat(8.0F, measureDurationTextWidth(context)));
         nameColumnWidth = Math.max(nameColumnWidth, HudStyle.computeFloat4(measureHeaderWidth(context)));
         contentHeight += 14.0F * context.visibilityAnimation.floatValue();
      }

      float contentWidth = durationColumnWidth + (durationColumnWidth > 0.0F && nameColumnWidth > 0.0F ? 2.0F : 0.0F) + nameColumnWidth;
      float targetPanelWidth = Math.max(titleWidth, Math.max(contentWidth, 1.0F));
      boolean isVisible = !activeContexts.isEmpty() || HudRenderContext.checkState();
      panelAlphaAnimation.update();
      panelAlphaAnimation.animateTo(isVisible ? 1.0 : 0.0, 0.4, Easings.EASE_OUT_QUART);
      panelWidthAnimation.update();
      if (!activeContexts.isEmpty()) {
         panelWidthAnimation.animateTo(Math.max(targetPanelWidth, 1.0F), 0.6, Easings.EASE_OUT_QUART);
      }

      titleAnimation.update();
      if (!activeContexts.isEmpty()) {
         titleAnimation.animateTo(Math.max(titleWidth, 1.0F), 0.6, Easings.EASE_OUT_QUART);
      }

      contentWidthAnimation.update();
      if (!activeContexts.isEmpty()) {
         contentWidthAnimation.animateTo(Math.max(durationColumnWidth, 0.0F), 0.6, Easings.EASE_OUT_QUART);
      }

      panelWidth = panelWidthAnimation.floatValue();
      panelHeight = Math.max(contentHeight, 1.0F);
      float alpha = panelAlphaAnimation.floatValue();
      if (alpha <= 0.001F) {
         return 0.0F;
      }

      float[] floatArray = HudManager.computeFloat5("potions", centerX - panelWidth, centerY);
      float panelX = floatArray[0];
      float panelY = floatArray[1];
      HudManager.processData9("potions", panelX, panelY, panelWidth, panelHeight);
      HudRenderContext.processData4(panelX + panelWidth / 2.0F, panelY + panelHeight / 2.0F, alpha);
      HudStyle.processData7(panelX, panelY, titleWidth, alpha, HudIcons.POTIONS, "Potions");
      float rowY = panelY + 15.0F;

      for (PotionsHud.SubContext context : activeContexts) {
         renderPotionEntry(panelX, rowY, alpha, context);
         rowY += 13.0F * context.visibilityAnimation.floatValue();
      }

      HudRenderContext.updateState();
      return panelHeight * alpha + 8.0F * alpha;
   }

   public static void processData2(ServerItemType serverItemType, BlockPos blockPos) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient.player != null && !(minecraftClient.player.getBoundingBox().squaredMagnitude(blockPos.toCenterPos()) > 16.0)) {
         lastDonationSeenMillis.put(serverItemType, System.currentTimeMillis());
      }
   }

   static int effectSortPriority(RegistryEntry<StatusEffect> effectEntry) {
      if (priorityEffects.contains(effectEntry)) {
         return 0;
      } else {
         StatusEffectCategory category = effectEntry.value().getCategory();
         if (category == StatusEffectCategory.BENEFICIAL) {
            return 1;
         } else {
            return category == StatusEffectCategory.NEUTRAL ? 2 : 3;
         }
      }
   }

   private PotionsHud() {
   }

   private static float measureDurationTextWidth(PotionsHud.SubContext context) {
      return FontText.computeFloat(FontFace.SEMI_BOLD, context.durationText, 6.0F);
   }

   private static void refreshDonationPotions(MinecraftClient minecraftClient) {
      Map<ServerItemType, Boolean> previousVisibility = new EnumMap<>(ServerItemType.class);
      donationContexts.forEach((serverItemType, context) -> previousVisibility.put(serverItemType, context.visible));
      donationContexts.values().forEach(context -> context.visible = false);
      if (minecraftClient.player != null && InterfaceModule.getInstance().showDonationPotions.isChecked() && ServerItemType.checkState()) {
         for (ServerItemType serverItemType2 : ServerItemType.values()) {
            StatusEffectInstance statusEffectInstance = null;

            for (ServerItemType.DataRecord dataRecord : serverItemType2.getList()) {
               StatusEffectInstance statusEffectInstance2 = minecraftClient.player.getStatusEffect(dataRecord.effect());
               if (statusEffectInstance2 == null || !ServerItemType.matchesCriteria(statusEffectInstance2.getAmplifier(), dataRecord.amplifier())) {
                  statusEffectInstance = null;
                  break;
               }

               if (statusEffectInstance == null || statusEffectInstance2.getDuration() > statusEffectInstance.getDuration()) {
                  statusEffectInstance = statusEffectInstance2;
               }
            }

            boolean shouldShow = previousVisibility.getOrDefault(serverItemType2, false) || System.currentTimeMillis() - lastDonationSeenMillis.getOrDefault(serverItemType2, 0L) < 3000L;
            if (statusEffectInstance != null && shouldShow) {
               PotionsHud.SubContext context = donationContexts.computeIfAbsent(serverItemType2, serverItemType3 -> {
                  PotionsHud.SubContext newContext = new PotionsHud.SubContext(serverItemType3.formatText(), 0);
                  newContext.text2 = serverItemType3.getText2();
                  return newContext;
               });

               for (ServerItemType.DataRecord entryRecord : serverItemType2.getList()) {
                  PotionsHud.SubContext effectContext = effectContexts.get(entryRecord.effect());
                  if (effectContext != null) {
                     effectContext.visible = false;
                  }
               }

               context.statusEffectInstance3 = statusEffectInstance;
               context.durationText = formatDurationText(statusEffectInstance);
               context.visible = true;
            }
         }
      }
   }

   private static void renderPotionEntry(float x, float y, float containerAlpha, PotionsHud.SubContext context) {
      float entryAlpha = context.visibilityAnimation.floatValue() * containerAlpha;
      if (!(entryAlpha <= 0.001F)) {
         float iconWidth = HudStyle.computeFloat2(entryAlpha);
         float durationTextWidth = measureDurationTextWidth(context);
         float leftBlockWidth = HudStyle.computeFloat(8.0F, durationTextWidth);
         float rightBlockWidth = HudStyle.computeFloat4(measureHeaderWidth(context));
         float iconX = x - iconWidth;
         float rightBlockX = x + contentWidthAnimation.floatValue() + 2.0F - iconWidth;
         float textY = HudStyle.computeFloat3(y);
         HudStyle.processData6(iconX, y, leftBlockWidth, 11.0F, entryAlpha);
         float durationX = iconX + 2.0F + 8.0F + 1.0F;
         float durationTextX = durationX + 0.25F;
         HudStyle.processData(durationX, y + 5.5F, entryAlpha, 3.0F);
         if (context.statusEffectInstance3 != null) {
            Render2D.pushAlpha(entryAlpha);
            Render2D.drawStatusEffectIconWhite(
               context.statusEffectInstance3.getEffectType(), iconX + (durationTextX - iconX - 8.0F) / 2.0F, y + 5.5F - 4.0F, 8.0F
            );
            Render2D.popAlpha();
         }

         FontText.processData3(
            FontFace.SEMI_BOLD,
            context.durationText,
            durationTextX + (iconX + leftBlockWidth - durationTextX - durationTextWidth) / 2.0F,
            textY,
            6.0F,
            UiTheme.computeInt(HudStyle.computeInt4(1.0F), entryAlpha)
         );
         HudStyle.processData6(rightBlockX, y, rightBlockWidth, 11.0F, entryAlpha);
         boolean isHarmful = context.statusEffectInstance3 != null && context.statusEffectInstance3.getEffectType().value().getCategory() == StatusEffectCategory.HARMFUL;
         int nameColor = UiTheme.computeInt(isHarmful ? -44976 : -1, entryAlpha);
         if (context.text2 != null) {
            FontText.processData5(
               FontFace.SEMI_BOLD, context.text2, rightBlockX + 4.0F, textY, 6.0F, UiTheme.computeInt(-1, entryAlpha)
            );
         } else {
            FontText.processData3(FontFace.SEMI_BOLD, context.name, rightBlockX + 4.0F, textY, 6.0F, nameColor);
         }

         if (!context.amplifierText.isEmpty()) {
            FontText.processData3(
               FontFace.SEMI_BOLD,
               context.amplifierText,
               rightBlockX + 4.0F + FontText.computeFloat(FontFace.SEMI_BOLD, context.name, 6.0F),
               textY,
               6.0F,
               UiTheme.computeInt(ThemeColors.intVal9, entryAlpha)
            );
         }
      }
   }

   private static final class SubContext extends DraggableHudElement {
      final String name;
      String amplifierText = "";
      String durationText = "";
      StatusEffectInstance statusEffectInstance3;
      final int sortPriority;
      int lastAmplifier = -1;
      Text text2;

      SubContext(String name, int sortPriority) {
         this.name = name;
         this.sortPriority = sortPriority;
      }

      SubContext(RegistryEntry<StatusEffect> effectEntry) {
         this.name = effectEntry.value().getName().getString();
         this.sortPriority = PotionsHud.effectSortPriority(effectEntry);
      }
   }
}
