package greed.client.ui.hud;

import greed.client.render.FontText;
import greed.client.render.ThemeColors;
import greed.client.ui.UiTheme;

import greed.client.util.Animation;

import greed.client.util.Easings;
import greed.client.render.FontFace;
import greed.client.render.Render2D;
import greed.client.storage.StaffStore;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

public final class StaffHud {
   public static final Pattern pattern = Pattern.compile("^\\w{3,16}$");
   public static final Pattern pattern2 = Pattern.compile(
      ".*(mod|der|adm|help|wne|мод|хелп|помо|адм|владе|отри|таф|taf|curat|курато|dev|раз|supp|сапп|yt|ютуб).*"
   );
   private static final String[][] exampleStaffRows = new String[][]{
      {"karandash", "Helper", "26:08", "ι"}, {"smdxyxz", "Tester", "08:56", "κ"}, {"Кирейко", "Moder", "10:34", "ι"}
   };
   private static final Map<String, StaffHud.SubContext2> contextsByName = new LinkedHashMap<>();
   private static final HashMap<String, Long> firstSeenMillisByName = new HashMap<>();
   private static final List<StaffHud.SubContext2> activeContexts = new ArrayList<>();
   private static final Animation panelAlphaAnimation = new Animation();
   private static final Animation panelWidthAnimation = new Animation();
   private static final Animation titleAnimation = new Animation();
   private static final Animation contentWidthAnimation = new Animation();
   private static float panelWidth;
   private static float panelHeight;
   private static long lastUpdateMillis;
   private static final IdentityHashMap<PlayerListEntry, StaffHud.DataRecord> recordCache = new IdentityHashMap<>();
   private static final HashSet<String> onlineNames = new HashSet<>();
   private static Object cachedWorld;
   private static long lastWorldCheckMillis;

   private static float badgeWidth(StaffHud.SubContext2 context) {
      return context.rank.isEmpty() ? 0.0F : FontText.computeFloat(FontFace.SEMI, context.rank, 6.0F) + 3.0F;
   }

   private StaffHud() {
   }

   private static String resolveRankText(PlayerListEntry playerListEntry, String playerName, Team newTeam) {
      if (!StaffStore.matchesCriteria3(playerName) && !pattern.matcher(playerName).matches()) {
         return null;
      }

      String teamPrefix = newTeam == null ? "" : newTeam.getPrefix().getString().trim();
      String displayPrefix = !teamPrefix.isEmpty()
         ? teamPrefix
         : (playerListEntry.getDisplayName() == null ? "" : playerListEntry.getDisplayName().getString().replace(playerName, "").trim());
      return StaffStore.matchesCriteria3(playerName)
            || StaffStore.matchesCriteria2(displayPrefix)
            || !displayPrefix.isEmpty() && pattern2.matcher(displayPrefix.toLowerCase(Locale.ROOT)).matches()
         ? displayPrefix
         : null;
   }

   static {
      panelWidthAnimation.resetTo(1.0);
   }

   private static void renderStaffEntry(
      MinecraftClient minecraftClient, float x, float y, float containerAlpha, StaffHud.SubContext2 context
   ) {
      float entryAlpha = context.visibilityAnimation.floatValue() * containerAlpha;
      if (!(entryAlpha <= 0.001F)) {
         float avatarWidth = HudStyle.computeFloat2(entryAlpha);
         float timeTextWidth = measureTimeTextWidth(context);
         float nameBlockWidth = HudStyle.computeFloat(6.0F, timeTextWidth);
         float entryWidth = computeEntryWidth(context);
         float iconX = x - avatarWidth;
         float rightBlockX = x + contentWidthAnimation.floatValue() + 2.0F - avatarWidth;
         entryWidth = Math.max(entryWidth, titleAnimation.floatValue() - contentWidthAnimation.floatValue() - 2.0F);
         float textY = HudStyle.computeFloat3(y);
         HudStyle.processData6(rightBlockX, y, entryWidth, 11.0F, entryAlpha);
         String skinTexture = resolveSkinTexture(minecraftClient, context.name);
         float avatarY = y + 5.5F - 4.0F;
         int avatarColor = UiTheme.computeInt(-1, entryAlpha);
         Render2D.drawImageUvNearest(skinTexture, rightBlockX + 4.0F, avatarY, 8.0F, 8.0F, 3.5F, 0.25F, 0.125F, 0.125F, 0.25F, 0.25F, avatarColor);
         Render2D.drawImageUvNearest(skinTexture, rightBlockX + 4.0F, avatarY, 8.0F, 8.0F, 3.5F, 0.25F, 0.625F, 0.125F, 0.75F, 0.25F, avatarColor);
         float rightNameX = rightBlockX + 4.0F + 8.0F + 3.0F;
         if (context.text2 != null) {
            FontText.processData5(
               FontFace.SEMI,
               context.text2,
               rightNameX,
               textY,
               6.0F,
               UiTheme.computeInt(HudStyle.computeInt4(1.0F), entryAlpha)
            );
         } else if (!context.rank.isEmpty()) {
            FontText.processData3(
               FontFace.SEMI,
               context.rank,
               rightNameX,
               textY,
               6.0F,
               UiTheme.computeInt(HudStyle.computeInt2(1.0F), entryAlpha)
            );
         }

         FontText.processData3(
            FontFace.SEMI,
            context.name,
            rightNameX + badgeWidth(context),
            textY,
            6.0F,
            UiTheme.computeInt(HudStyle.computeInt4(1.0F), entryAlpha)
         );
         HudStyle.processData6(iconX, y, nameBlockWidth, 11.0F, entryAlpha);
         float leftNameX = iconX + 2.0F + 6.0F + 1.0F;
         float leftNameTextX = leftNameX + 0.25F;
         HudStyle.processData(leftNameX, y + 5.5F, entryAlpha, 3.0F);
         ThemeColors.processData(
            context.statusGlyph,
            iconX + (leftNameTextX - iconX - 6.0F) / 2.0F + 3.0F,
            y + 5.5F,
            6.0F,
            UiTheme.computeInt(HudStyle.computeInt2(1.0F), entryAlpha)
         );
         FontText.processData3(
            FontFace.SEMI,
            context.timeText,
            leftNameTextX + (iconX + nameBlockWidth - leftNameTextX - timeTextWidth) / 2.0F,
            textY,
            6.0F,
            UiTheme.computeInt(HudStyle.computeInt4(1.0F), entryAlpha)
         );
      }
   }

   private static float computeEntryWidth(StaffHud.SubContext2 context) {
      return 15.0F + badgeWidth(context) + FontText.computeFloat(FontFace.SEMI, context.name, 6.0F) + 4.0F;
   }

   private static void updateStaffList(MinecraftClient minecraftClient) {
      activeContexts.clear();
      long nowMillis = System.currentTimeMillis();
      if (nowMillis - lastUpdateMillis >= 250L) {
         refreshStaffEntries(minecraftClient);
         lastUpdateMillis = nowMillis;
      }

      boolean hasRealEntries = contextsByName.values().stream().anyMatch(context -> context.visible && !context.exampleEntry);
      if (!hasRealEntries && HudRenderContext.checkState()) {
         for (String[] stringArray : exampleStaffRows) {
            StaffHud.SubContext2 context = contextsByName.computeIfAbsent(stringArray[0], StaffHud.SubContext2::new);
            context.rank = stringArray[1];
            context.text2 = null;
            context.timeText = stringArray[2];
            context.statusGlyph = stringArray[3];
            context.exampleEntry = true;
            context.visible = true;
         }
      }

      for (StaffHud.SubContext2 context : contextsByName.values()) {
         if (context.visible || context.visibilityAnimation.floatValue() > 0.001) {
            activeContexts.add(context);
         }
      }

      contextsByName.entrySet()
         .removeIf(
            entry -> !entry.getValue().visible
               && entry.getValue().visibilityAnimation.floatValue() <= 0.001
               && entry.getValue().visibilityAnimation.isFinished()
         );

      for (StaffHud.SubContext2 context : activeContexts) {
         context.visibilityAnimation.update();
         context.visibilityAnimation.animateTo(context.visible ? 1.0 : 0.0, 0.6, Easings.EASE_OUT_QUART);
      }
   }

   public static float computeFloat3(float centerX, float centerY) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      updateStaffList(minecraftClient);
      float titleWidth = 16.0F + FontText.computeFloat(FontFace.SEMI, "Staffs", 6.0F) + 4.0F;
      float durationColumnWidth = 0.0F;
      float entryColumnWidth = 0.0F;
      float contentHeight = 13.0F;

      for (StaffHud.SubContext2 context : activeContexts) {
         durationColumnWidth = Math.max(durationColumnWidth, HudStyle.computeFloat(6.0F, measureTimeTextWidth(context)));
         entryColumnWidth = Math.max(entryColumnWidth, computeEntryWidth(context));
         contentHeight += 14.0F * context.visibilityAnimation.floatValue();
      }

      float contentWidth = durationColumnWidth + (durationColumnWidth > 0.0F && entryColumnWidth > 0.0F ? 2.0F : 0.0F) + entryColumnWidth;
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

      float[] floatArray = HudManager.computeFloat5("staffs", centerX - panelWidth, centerY);
      float panelX = floatArray[0];
      float panelY = floatArray[1];
      HudManager.processData9("staffs", panelX, panelY, panelWidth, panelHeight);
      HudRenderContext.processData4(panelX + panelWidth / 2.0F, panelY + panelHeight / 2.0F, alpha);
      HudStyle.processData7(panelX, panelY, titleWidth, alpha, HudIcons.STAFF, "Staffs");
      float rowY = panelY + 15.0F;

      for (StaffHud.SubContext2 context : activeContexts) {
         renderStaffEntry(minecraftClient, panelX, rowY, alpha, context);
         rowY += 13.0F * context.visibilityAnimation.floatValue();
      }

      HudRenderContext.updateState();
      return panelHeight * alpha;
   }

   private static float measureTimeTextWidth(StaffHud.SubContext2 context) {
      return FontText.computeFloat(FontFace.SEMI, context.timeText, 6.0F);
   }

   private static void refreshStaffEntries(MinecraftClient minecraftClient) {
      for (StaffHud.SubContext2 context : contextsByName.values()) {
         context.visible = false;
      }

      if (minecraftClient.getNetworkHandler() != null && minecraftClient.world != null) {
         long nowMillis = System.currentTimeMillis();
         if (minecraftClient.world != cachedWorld || nowMillis - lastWorldCheckMillis >= 5000L) {
            recordCache.clear();
            cachedWorld = minecraftClient.world;
            lastWorldCheckMillis = nowMillis;
         }

         onlineNames.clear();
         Collection<PlayerListEntry> playerList = minecraftClient.getNetworkHandler().getPlayerList();
         if (recordCache.size() > playerList.size() * 2 + 16) {
            recordCache.clear();
         }

         for (PlayerListEntry playerListEntry : playerList) {
            String profileName = playerListEntry.getProfile().name();
            if (profileName != null && !profileName.isEmpty()) {
               Team scoreHolderTeam = minecraftClient.world.getScoreboard().getScoreHolderTeam(profileName);
               Object prefix = scoreHolderTeam == null ? null : scoreHolderTeam.getPrefix();
               Object displayName = playerListEntry.getDisplayName();
               StaffHud.DataRecord dataRecord = recordCache.get(playerListEntry);
               if (dataRecord == null
                  || dataRecord.team != scoreHolderTeam
                  || dataRecord.prefix != prefix
                  || dataRecord.display != displayName
                  || !profileName.equals(dataRecord.name)) {
                  dataRecord = new StaffHud.DataRecord(
                     profileName, scoreHolderTeam, prefix, displayName, resolveRankText(playerListEntry, profileName, scoreHolderTeam)
                  );
                  recordCache.put(playerListEntry, dataRecord);
               }

               String rankText = dataRecord.rankText;
               if (rankText != null) {
                  onlineNames.add(profileName);
                  long firstSeenMillis = firstSeenMillisByName.computeIfAbsent(profileName, name -> System.currentTimeMillis());
                  StaffHud.SubContext2 context = contextsByName.computeIfAbsent(profileName, StaffHud.SubContext2::new);
                  context.rank = rankText.isEmpty() ? "Staff" : rankText;
                  context.text2 = scoreHolderTeam != null && !scoreHolderTeam.getPrefix().getString().trim().isEmpty() ? scoreHolderTeam.getPrefix() : null;
                  context.timeText = String.format(
                     "%02d:%02d", (System.currentTimeMillis() - firstSeenMillis) / 60000L, (System.currentTimeMillis() - firstSeenMillis) / 1000L % 60L
                  );
                  context.statusGlyph = playerListEntry.getGameMode() == GameMode.SPECTATOR ? "ι" : "κ";
                  context.exampleEntry = false;
                  context.visible = true;
               }
            }
         }

         firstSeenMillisByName.keySet().retainAll(onlineNames);
      } else {
         firstSeenMillisByName.clear();
      }
   }

   private static String resolveSkinTexture(MinecraftClient minecraftClient, String playerName) {
      if (minecraftClient.getNetworkHandler() != null) {
         PlayerListEntry playerListEntry = minecraftClient.getNetworkHandler().getPlayerListEntry(playerName);
         if (playerListEntry != null) {
            return playerListEntry.getSkinTextures().body().texturePath().toString();
         }
      }

      UUID uUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes(StandardCharsets.UTF_8));
      return DefaultSkinHelper.getSkinTextures(uUID).body().texturePath().toString();
   }

   private record DataRecord(String name, Object team, Object prefix, Object display, String rankText) {
   }

   private static final class SubContext2 extends DraggableHudElement {
      final String name;
      String rank = "";
      Text text2;
      String timeText = "";
      String statusGlyph = "κ";
      boolean exampleEntry;

      SubContext2(String name) {
         this.name = name;
      }
   }
}
