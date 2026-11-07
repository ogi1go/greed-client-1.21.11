package greed.client.ui;

import greed.client.cosmetic.CosmeticCatalog;
import greed.client.cosmetic.CosmeticCategory;
import greed.client.cosmetic.CosmeticEntry;
import greed.client.cosmetic.CosmeticPreviewAtlas;
import greed.client.cosmetic.CosmeticStore;
import greed.client.render.FontFace;
import greed.client.render.FontText;
import greed.client.ui.UiTheme;
import greed.client.cosmetic.CosmeticAccess;
import greed.client.ui.AnimatedValue;
import greed.client.render.Render2D;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

public final class CosmeticsSelectionScreen {
   private final HashMap<String, AnimatedValue> keyedAnimations = new HashMap<>();
   private final HashMap<Integer, AnimatedValue> hoverAnimations = new HashMap<>();
   private final HashMap<Integer, AnimatedValue> equippedAnimations = new HashMap<>();
   private final EnumMap<CosmeticCategory, AnimatedValue> tabHoverAnimations = new EnumMap<>(CosmeticCategory.class);
   private final EnumMap<CosmeticCategory, AnimatedValue> tabSelectedAnimations = new EnumMap<>(CosmeticCategory.class);
   private final EnumMap<CosmeticCategory, List<CosmeticEntry>> entriesByCategory = new EnumMap<>(CosmeticCategory.class);
   private final EnumMap<CosmeticCategory, String> categoryCountTexts = new EnumMap<>(CosmeticCategory.class);
   private final HashMap<Integer, CosmeticsSelectionScreen.DataRecord> titleCache = new HashMap<>();
   private final ArrayList<CosmeticsSelectionScreen.DataRecord2> clickableRegions = new ArrayList<>();
   private final CosmeticsSelectionScreen.SubContext3 titleTransition = new CosmeticsSelectionScreen.SubContext3();
   private final CosmeticsSelectionScreen.SubContext3 subtitleTransition = new CosmeticsSelectionScreen.SubContext3();
   private final CosmeticsSelectionScreen.SubContext3 equippedTransition = new CosmeticsSelectionScreen.SubContext3();
   private String lastSearchQuery;
   private int lastCatalogRevision = -1;
   private float contentHeight;
   private boolean searchFocused;
   private String searchQuery = "";
   private float targetScroll;
   private float animatedScroll;
   private float maxScroll;
   private long lastFrameTimeMillis = System.nanoTime() / 1000000L;
   private long panelOpenTimeMillis;
   private Runnable backAction = () -> {};
   private float listTop;
   private float listBottom;
   private float detailPanelX;
   private float detailPanelY;
   private float scrollbarX;
   private float uiScale;
   private float lastUiScale = -1.0F;
   private float equipLabelWidth;
   private float unequipLabelWidth;
   private CosmeticCategory selectedCategory = CosmeticCategory.HAT;
   private CosmeticCategory previousCategory = CosmeticCategory.HAT;
   private long categoryChangeTimeMillis;
   private CosmeticEntry hoveredEntry;
   private CosmeticEntry previousHoveredEntry;
   private boolean draggingScrollbar;
   private float dragGrabOffset;

   private float animateByKey(String animationKey, boolean active) {
      AnimatedValue animatedValue = this.keyedAnimations.computeIfAbsent(animationKey, key -> new AnimatedValue(140L));
      animatedValue.processData(active ? 1.0 : 0.0);
      return (float)animatedValue.computeDouble();
   }

   public boolean matchesCriteria(int keyCode) {
      if (!this.searchFocused) {
         return false;
      }

      if (GLFW.glfwGetKey(MinecraftClient.getInstance().getWindow().getHandle(), 341) == 1 && keyCode == 86) {
         this.searchQuery = this.searchQuery + MinecraftClient.getInstance().keyboard.getClipboard().replaceAll("[\\r\\n\\t]", "");
         if (this.searchQuery.length() > 24) {
            this.searchQuery = this.searchQuery.substring(0, 24);
         }

         return true;
      } else if (keyCode == 259) {
         if (!this.searchQuery.isEmpty()) {
            this.searchQuery = this.searchQuery.substring(0, this.searchQuery.length() - 1);
         }

         this.targetScroll = 0.0F;
         return true;
      } else {
         if (keyCode != 257 && keyCode != 335 && keyCode != 256) {
            return false;
         }

         this.searchFocused = false;
         return true;
      }
   }

   public void processData(Runnable runnable) {
      this.panelOpenTimeMillis = System.currentTimeMillis();
      this.categoryChangeTimeMillis = this.panelOpenTimeMillis;
      this.backAction = runnable;
      this.searchFocused = false;
      this.draggingScrollbar = false;
   }

   private void refreshCatalog() {
      if (!this.searchQuery.equals(this.lastSearchQuery) || this.lastCatalogRevision != CosmeticCatalog.computeInt()) {
         boolean queryChanged = !this.searchQuery.equals(this.lastSearchQuery);
         this.lastSearchQuery = this.searchQuery;
         this.lastCatalogRevision = CosmeticCatalog.computeInt();
         this.entriesByCategory.clear();

         for (CosmeticCategory cosmeticCategory : CosmeticCategory.values()) {
            List<CosmeticEntry> categoryEntries = this.getList(cosmeticCategory);
            this.entriesByCategory.put(cosmeticCategory, categoryEntries);
            this.categoryCountTexts.put(cosmeticCategory, String.valueOf(categoryEntries.size()));
         }

         if (queryChanged && !this.searchQuery.isBlank() && this.entriesByCategory.get(this.selectedCategory).isEmpty()) {
            for (CosmeticCategory cosmeticCategory2 : CosmeticCategory.values()) {
               if (!this.entriesByCategory.get(cosmeticCategory2).isEmpty()) {
                  this.selectCategory(cosmeticCategory2);
                  break;
               }
            }
         }
      }
   }

   private void renderEquippedBadge(CosmeticEntry cosmeticEntry, float panelX, float panelY, float mouseX, float mouseY) {
      float panelWidth = 158.0F * this.uiScale;
      float panelHeight = 29.0F * this.uiScale;
      float radius = 14.5F * this.uiScale;

      Render2D.drawRect(panelX, panelY, panelWidth, panelHeight, radius, UiTheme.computeInt(UiTheme.intVal, 0.12F));
      Render2D.drawOutline(panelX, panelY, panelWidth, panelHeight, radius, 0.5F, UiTheme.computeInt(UiTheme.intVal, 0.18F));

      float titleAlpha = this.equippedTransition
              .renderTransitionText(
                      FontFace.SEMI_BOLD,
                      cosmeticEntry == null ? "Ничего не надето" : this.formatText(cosmeticEntry),
                      panelX + 30.0F * this.uiScale,
                      panelY + 14.5F * this.uiScale,
                      6.5F * this.uiScale,
                      cosmeticEntry == null ? UiTheme.intVal30 : UiTheme.intVal,
                      4.0F * this.uiScale,
                      240L
              );
      FontText.processData3(
              FontFace.SEMI,
              cosmeticEntry == null ? "Выберите предмет" : "Надето",
              panelX + 30.0F * this.uiScale,
              panelY + 5.5F * this.uiScale,
              5.5F * this.uiScale,
              UiTheme.intVal30
      );
      float previewAlpha = cosmeticEntry == null ? 0.0F : UiTheme.computeFloat(titleAlpha);
      if (cosmeticEntry == null) {
         UiTheme.processData3(
                 this.selectedCategory.formatText4(),
                 panelX + 14.5F * this.uiScale,
                 panelY + 14.5F * this.uiScale,
                 9.0F * this.uiScale,
                 9.0F * this.uiScale,
                 UiTheme.computeInt(UiTheme.intVal, 0.2F)
         );
      } else {
         CosmeticPreviewAtlas.AtlasEntry atlasEntry = CosmeticPreviewAtlas.getNested0499(cosmeticEntry);
         float previewSize = 18.0F * this.uiScale * Math.max(0.0F, previewAlpha);
         if (atlasEntry != null && previewSize > 0.5F) {
            Render2D.drawImageUv(
                    atlasEntry.atlasTexture(),
                    panelX + 14.5F * this.uiScale - previewSize / 2.0F,
                    panelY + 14.5F * this.uiScale - previewSize / 2.0F,
                    previewSize,
                    previewSize,
                    0.0F,
                    atlasEntry.u0(),
                    atlasEntry.v0(),
                    atlasEntry.u1(),
                    atlasEntry.v1(),
                    UiTheme.intVal
            );
         }

         float takeOffHover = this.animateByKey(
                 "cosTakeOff",
                 UiTheme.matchesCriteria(
                         mouseX,
                         mouseY,
                         panelX + panelWidth - 22.0F * this.uiScale,
                         panelY + 4.5F * this.uiScale,
                         18.0F * this.uiScale,
                         20.0F * this.uiScale
                 )
         );
         UiTheme.processData7(
                 "c",
                 panelX + panelWidth - 13.0F * this.uiScale,
                 panelY + 14.5F * this.uiScale,
                 5.5F * this.uiScale,
                 UiTheme.computeInt2(UiTheme.intVal29, UiTheme.intVal23, takeOffHover)
         );
         this.clickableRegions
                 .add(
                         new CosmeticsSelectionScreen.DataRecord2(
                                 panelX + panelWidth - 22.0F * this.uiScale,
                                 panelY + 4.5F * this.uiScale,
                                 18.0F * this.uiScale,
                                 20.0F * this.uiScale,
                                 false,
                                 () -> CosmeticStore.processData2(this.selectedCategory)
                         )
                 );
      }
   }

   private void renderSidebar(float sidebarX, float sidebarY, float mouseX, float mouseY) {
      float sidebarProgress = this.computePanelSlideProgress(0);
      Render2D.pushAlpha(sidebarProgress);
      float sidebarContentX = sidebarX - (1.0F - sidebarProgress) * 14.0F * this.uiScale;
      float backHover = this.animateByKey(
              "cosBack",
              UiTheme.matchesCriteria(
                      mouseX,
                      mouseY,
                      sidebarContentX - 4.0F * this.uiScale,
                      sidebarY + 105.0F * this.uiScale,
                      18.0F * this.uiScale,
                      18.0F * this.uiScale
              )
      );
      UiTheme.processData3(
              "assets/greed/textures/gui/mainmenu/icons/back.png",
              sidebarContentX + 4.75F * this.uiScale - backHover * 1.5F * this.uiScale,
              sidebarY + 113.75F * this.uiScale,
              9.5F * this.uiScale,
              9.5F * this.uiScale,
              UiTheme.computeInt2(UiTheme.intVal29, UiTheme.intVal, backHover)
      );
      this.clickableRegions
              .add(
                      new CosmeticsSelectionScreen.DataRecord2(
                              sidebarContentX - 4.0F * this.uiScale,
                              sidebarY + 105.0F * this.uiScale,
                              18.0F * this.uiScale,
                              18.0F * this.uiScale,
                              false,
                              this.backAction
                      )
              );
      FontText.processData3(
              FontFace.SEMI,
              "Greed client  /  Главная  /  Косметика",
              sidebarContentX + 15.5F * this.uiScale,
              sidebarY + 110.5F * this.uiScale,
              6.5F * this.uiScale,
              UiTheme.intVal29
      );
      FontText.processData3(
              FontFace.SEMI, "Косметика.", sidebarContentX, sidebarY + 124.5F * this.uiScale, 20.0F * this.uiScale, UiTheme.intVal
      );
      FontText.processData3(
              FontFace.SEMI,
              "Собирайте образ - предметы применяются сразу",
              sidebarContentX,
              sidebarY + 159.5F * this.uiScale,
              7.5F * this.uiScale,
              UiTheme.intVal29
      );
      Render2D.popAlpha();
   }

   private float computePanelSlideProgress(int panelIndex) {
      return UiTheme.computeFloat2(UiTheme.computeFloat3(this.panelOpenTimeMillis, 40L + panelIndex * 55L, 420L));
   }

   private void renderSectionHeader(String title, float x, float y, float mouseX, float mouseY, boolean showClearButton) {
      FontText.processData3(FontFace.SEMI, title, x, y, 6.0F * this.uiScale, UiTheme.intVal30);
      float rightEdge = x + 226.5F * this.uiScale;
      if (showClearButton) {
         String clearLabel = "Снять всё";
         float clearLabelWidth = FontText.computeFloat(FontFace.SEMI, clearLabel, 6.0F * this.uiScale);
         float clearHover = this.animateByKey(
                 "cosClear",
                 UiTheme.matchesCriteria(
                         mouseX,
                         mouseY,
                         x + 226.5F * this.uiScale - clearLabelWidth - 9.0F * this.uiScale,
                         y - 4.0F * this.uiScale,
                         clearLabelWidth + 9.0F * this.uiScale,
                         14.0F * this.uiScale
                 )
         );
         UiTheme.processData7(
                 "c",
                 x + 226.5F * this.uiScale - clearLabelWidth - 5.5F * this.uiScale,
                 y + 3.0F * this.uiScale,
                 5.0F * this.uiScale,
                 UiTheme.computeInt2(UiTheme.intVal29, UiTheme.intVal, clearHover)
         );
         FontText.processData3(
                 FontFace.SEMI,
                 clearLabel,
                 x + 226.5F * this.uiScale - clearLabelWidth,
                 y,
                 6.0F * this.uiScale,
                 UiTheme.computeInt2(UiTheme.intVal29, UiTheme.intVal, clearHover)
         );
         this.clickableRegions
                 .add(
                         new CosmeticsSelectionScreen.DataRecord2(
                                 x + 226.5F * this.uiScale - clearLabelWidth - 9.0F * this.uiScale,
                                 y - 4.0F * this.uiScale,
                                 clearLabelWidth + 9.0F * this.uiScale,
                                 14.0F * this.uiScale,
                                 false,
                                 CosmeticStore::updateState3
                         )
                 );
         rightEdge -= clearLabelWidth + 11.0F * this.uiScale;
      }

      float dividerStartX = x + FontText.computeFloat(FontFace.SEMI, title, 6.0F * this.uiScale) + 6.0F * this.uiScale;
      if (rightEdge - dividerStartX > 2.0F * this.uiScale) {
         Render2D.drawRect(
                 dividerStartX,
                 y + 3.0F * this.uiScale,
                 rightEdge - dividerStartX,
                 0.5F * this.uiScale,
                 0.25F * this.uiScale,
                 UiTheme.computeInt(UiTheme.intVal, 0.12F)
         );
      }
   }

   public boolean matchesCriteria2(float mouseX, float mouseY) {
      if (!this.draggingScrollbar) {
         return false;
      }

      float trackHeight = this.listBottom - this.listTop;
      float trackHeightLogical = trackHeight / this.uiScale;
      float thumbHeight = Math.max(24.0F * this.uiScale, trackHeight * (trackHeightLogical / Math.max(trackHeightLogical, this.contentHeight)));
      this.targetScroll = this.animatedScroll = clamp((mouseY - this.dragGrabOffset - this.listTop) / Math.max(1.0F, trackHeight - thumbHeight), 0.0F, 1.0F)
              * this.maxScroll;
      return true;
   }

   private void selectCategory(CosmeticCategory cosmeticCategory) {
      if (cosmeticCategory != this.selectedCategory) {
         this.previousCategory = this.selectedCategory;
         this.selectedCategory = cosmeticCategory;
         this.targetScroll = this.animatedScroll = 0.0F;
         this.categoryChangeTimeMillis = System.currentTimeMillis();
      }
   }

   private static float dampTowards(float current, float target, float delta) {
      current += (target - current) * (1.0F - (float)Math.exp(-delta * 22.0F));
      return Math.abs(target - current) < 0.1F ? target : current;
   }

   private void renderCosmeticCard(
           CosmeticEntry cosmeticEntry, float cardX, float cardY, float mouseX, float mouseY, boolean hasAccess, int itemIndex
   ) {
      float visibilityFactor = Math.min(
              clamp((cardY + 80.0F * this.uiScale - this.listTop) / (14.0F * this.uiScale), 0.0F, 1.0F),
              clamp((this.listBottom - cardY) / (14.0F * this.uiScale), 0.0F, 1.0F)
      );
      if (!(visibilityFactor <= 0.01F)) {
         boolean isLocked = cosmeticEntry.beta() && !hasAccess;
         boolean isEquipped = CosmeticStore.matchesCriteria(cosmeticEntry);
         boolean isHovered = !this.draggingScrollbar
                 && UiTheme.matchesCriteria(mouseX, mouseY, cardX, cardY, 56.0F * this.uiScale, 80.0F * this.uiScale)
                 && mouseY >= this.listTop
                 && mouseY <= this.listBottom;
         if (isHovered) {
            this.hoveredEntry = cosmeticEntry;
         }

         float hoverAnimation = this.animateEntryValue(this.hoverAnimations, cosmeticEntry.id(), isHovered && !isLocked);
         float equippedAnimation = this.animateEntryValue(this.equippedAnimations, cosmeticEntry.id(), isEquipped);
         float revealAnimation = UiTheme.computeFloat2(UiTheme.computeFloat3(this.categoryChangeTimeMillis, Math.min(Math.max(itemIndex, 0), 27) * 9L, 240L))
                 * visibilityFactor;
         boolean isRevealing = revealAnimation < 0.999F;
         if (isRevealing) {
            Render2D.pushAlpha(revealAnimation);
         }

         float cardWidth = 56.0F * this.uiScale;
         float cardHeight = 80.0F * this.uiScale;
         float cardRadius = 8.0F * this.uiScale;

         Render2D.drawRect(
                 cardX, cardY, cardWidth, cardHeight, cardRadius,
                 UiTheme.computeInt(UiTheme.intVal, 0.035F + 0.02F * hoverAnimation)
         );
         Render2D.drawOutline(
                 cardX, cardY, cardWidth, cardHeight, cardRadius, 0.5F,
                 UiTheme.computeInt(UiTheme.intVal, 0.08F + 0.04F * hoverAnimation)
         );

         float cardInnerSize = 38.0F * this.uiScale;
         float previewX = cardX + (56.0F * this.uiScale - cardInnerSize) / 2.0F;
         float previewY = cardY + 5.0F * this.uiScale;
         CosmeticPreviewAtlas.AtlasEntry atlasEntry = CosmeticPreviewAtlas.getNested0499(cosmeticEntry);
         if (atlasEntry != null) {
            Render2D.drawImageUv(
                    atlasEntry.atlasTexture(),
                    previewX,
                    previewY,
                    cardInnerSize,
                    cardInnerSize,
                    0.0F,
                    atlasEntry.u0(),
                    atlasEntry.v0(),
                    atlasEntry.u1(),
                    atlasEntry.v1(),
                    UiTheme.computeInt(UiTheme.intVal, isLocked ? 0.5F : 1.0F)
            );
         } else {
            Render2D.drawRect(
                    previewX + 8.0F * this.uiScale,
                    previewY + 8.0F * this.uiScale,
                    cardInnerSize - 16.0F * this.uiScale,
                    cardInnerSize - 16.0F * this.uiScale,
                    5.0F * this.uiScale,
                    UiTheme.computeInt(
                            UiTheme.intVal, 0.03F + 0.025F * (float)Math.sin((float)(System.currentTimeMillis() % 1400L) / 1400.0F * Math.PI * 2.0)
                    )
            );
         }

         CosmeticsSelectionScreen.DataRecord titleRecord = this.getCardTitle(cosmeticEntry);
         FontText.processData3(
                 FontFace.SEMI,
                 titleRecord.text(),
                 cardX + (56.0F * this.uiScale - titleRecord.width()) / 2.0F,
                 cardY + 47.0F * this.uiScale,
                 5.5F * this.uiScale,
                 isLocked ? UiTheme.computeInt(UiTheme.intVal, 0.42F) : UiTheme.intVal
         );
         float buttonX = cardX + 4.0F * this.uiScale;
         float buttonY = cardY + 62.0F * this.uiScale;
         float buttonWidth = 48.0F * this.uiScale;
         float buttonHeight = 14.0F * this.uiScale;
         if (isLocked) {
            UiTheme.processData(
                    buttonX,
                    buttonY,
                    buttonWidth,
                    buttonHeight,
                    5.0F * this.uiScale,
                    UiTheme.computeInt(UiTheme.intVal22, 0.07F),
                    UiTheme.computeInt(UiTheme.intVal22, 0.2F)
            );
            float betaLabelWidth = FontText.computeFloat(FontFace.SEMI_BOLD, "Бета", 5.5F * this.uiScale);
            float betaLabelX = cardX + (56.0F * this.uiScale - betaLabelWidth - 8.0F * this.uiScale) / 2.0F;
            UiTheme.processData7(
                    "Z", betaLabelX + 2.5F * this.uiScale, buttonY + 7.0F * this.uiScale, 5.0F * this.uiScale, UiTheme.intVal22
            );
            FontText.processData3(
                    FontFace.SEMI_BOLD,
                    "Бета",
                    betaLabelX + 8.0F * this.uiScale,
                    buttonY + 4.5F * this.uiScale,
                    5.5F * this.uiScale,
                    UiTheme.intVal22
            );
         } else {
            UiTheme.processData(
                    buttonX,
                    buttonY,
                    buttonWidth,
                    buttonHeight,
                    5.0F * this.uiScale,
                    UiTheme.computeInt2(
                            UiTheme.computeInt(UiTheme.intVal, 0.031F + 0.05F * hoverAnimation), UiTheme.intVal7, equippedAnimation
                    ),
                    UiTheme.computeInt2(
                            UiTheme.computeInt(UiTheme.intVal, 0.078F + 0.09F * hoverAnimation), UiTheme.intVal34, equippedAnimation
                    )
            );
            FontText.processData3(
                    FontFace.SEMI_BOLD,
                    isEquipped ? "Снять" : "Надеть",
                    cardX + (56.0F * this.uiScale - (isEquipped ? this.unequipLabelWidth : this.equipLabelWidth)) / 2.0F,
                    buttonY + 4.5F * this.uiScale,
                    5.5F * this.uiScale,
                    UiTheme.computeInt2(
                            UiTheme.computeInt2(UiTheme.intVal28, UiTheme.intVal, hoverAnimation),
                            UiTheme.intVal10,
                            equippedAnimation
                    )
            );
            this.clickableRegions
                    .add(
                            new CosmeticsSelectionScreen.DataRecord2(
                                    cardX, cardY, 56.0F * this.uiScale, 80.0F * this.uiScale, true, () -> CosmeticStore.processData(cosmeticEntry)
                            )
                    );
         }

         if (isRevealing) {
            Render2D.popAlpha();
         }
      }
   }

   private void renderCategoryTab(CosmeticCategory cosmeticCategory, float tabX, float tabY, float mouseX, float mouseY) {
      float hoverAnimation = this.animateCategoryValue(
              this.tabHoverAnimations,
              cosmeticCategory,
              UiTheme.matchesCriteria(mouseX, mouseY, tabX, tabY, 226.5F * this.uiScale, 19.5F * this.uiScale)
      );
      float selectedAnimation = this.animateCategoryValue(this.tabSelectedAnimations, cosmeticCategory, cosmeticCategory == this.selectedCategory);
      UiTheme.processData(
              tabX,
              tabY,
              226.5F * this.uiScale,
              19.5F * this.uiScale,
              6.0F * this.uiScale,
              UiTheme.computeInt2(
                      UiTheme.computeInt(UiTheme.intVal, 0.031F + 0.04F * hoverAnimation),
                      UiTheme.computeInt(UiTheme.intVal7, 0.16F),
                      selectedAnimation
              ),
              UiTheme.computeInt2(
                      UiTheme.computeInt(UiTheme.intVal, 0.078F + 0.08F * hoverAnimation),
                      UiTheme.computeInt(UiTheme.intVal34, 0.6F),
                      selectedAnimation
              )
      );
      UiTheme.processData3(
              cosmeticCategory.formatText4(),
              tabX + 11.0F * this.uiScale,
              tabY + 9.75F * this.uiScale,
              7.0F * this.uiScale,
              7.0F * this.uiScale,
              UiTheme.computeInt2(
                      UiTheme.computeInt2(UiTheme.intVal29, UiTheme.intVal, hoverAnimation),
                      UiTheme.intVal8,
                      selectedAnimation
              )
      );
      FontText.processData3(
              FontFace.SEMI,
              cosmeticCategory.formatText(),
              tabX + 20.0F * this.uiScale,
              tabY + 6.25F * this.uiScale,
              7.0F * this.uiScale,
              UiTheme.computeInt2(UiTheme.intVal28, UiTheme.intVal, Math.max(hoverAnimation, selectedAnimation))
      );
      String orDefault = this.categoryCountTexts.getOrDefault(cosmeticCategory, "0");
      float countLabelWidth = FontText.computeFloat(FontFace.SEMI, orDefault, 6.0F * this.uiScale);
      FontText.processData3(
              FontFace.SEMI,
              orDefault,
              tabX + 218.5F * this.uiScale - countLabelWidth,
              tabY + 6.75F * this.uiScale,
              6.0F * this.uiScale,
              UiTheme.computeInt(UiTheme.intVal, 0.34F + 0.3F * selectedAnimation)
      );
      CosmeticEntry cosmeticEntry = CosmeticStore.getCosmeticEntry(cosmeticCategory);
      if (cosmeticEntry != null) {
         float iconX = tabX + 218.5F * this.uiScale - countLabelWidth - 19.0F * this.uiScale;
         CosmeticPreviewAtlas.AtlasEntry atlasEntry = CosmeticPreviewAtlas.getNested0499(cosmeticEntry);
         if (atlasEntry != null) {
            Render2D.drawImageUv(
                    atlasEntry.atlasTexture(),
                    iconX + 1.0F * this.uiScale,
                    tabY + 4.0F * this.uiScale,
                    11.5F * this.uiScale,
                    11.5F * this.uiScale,
                    0.0F,
                    atlasEntry.u0(),
                    atlasEntry.v0(),
                    atlasEntry.u1(),
                    atlasEntry.v1(),
                    UiTheme.intVal
            );
         }
      }

      this.clickableRegions
              .add(
                      new CosmeticsSelectionScreen.DataRecord2(
                              tabX, tabY, 226.5F * this.uiScale, 19.5F * this.uiScale, false, () -> this.selectCategory(cosmeticCategory)
                      )
              );
   }

   private float animateCategoryValue(EnumMap<CosmeticCategory, AnimatedValue> animationMap, CosmeticCategory cosmeticCategory, boolean active) {
      AnimatedValue animatedValue = animationMap.computeIfAbsent(cosmeticCategory, cosmeticCategory2 -> new AnimatedValue(160L));
      animatedValue.processData(active ? 1.0 : 0.0);
      return (float)animatedValue.computeDouble();
   }

   private String formatText(CosmeticEntry cosmeticEntry) {
      CosmeticsSelectionScreen.DataRecord cachedRecord = this.titleCache.get(-cosmeticEntry.id() - 1);
      if (cachedRecord != null) {
         return cachedRecord.text();
      }

      String displayName = cosmeticEntry.name();
      float maxWidth = 100.0F * this.uiScale;
      float fontSize = 6.5F * this.uiScale;

      while (FontText.computeFloat(FontFace.SEMI_BOLD, displayName, fontSize) > maxWidth && displayName.length() > 3) {
         displayName = displayName.substring(0, displayName.length() - 2) + "…";
      }

      this.titleCache.put(-cosmeticEntry.id() - 1, new CosmeticsSelectionScreen.DataRecord(displayName, 0.0F));
      return displayName;
   }

   private CosmeticsSelectionScreen.DataRecord getCardTitle(CosmeticEntry cosmeticEntry) {
      CosmeticsSelectionScreen.DataRecord cachedRecord = this.titleCache.get(cosmeticEntry.id());
      if (cachedRecord != null) {
         return cachedRecord;
      }

      String displayName = cosmeticEntry.name().replaceFirst("^[^—]+ — ", "");
      float maxWidth = 51.0F * this.uiScale;
      float fontSize = 5.5F * this.uiScale;

      while (FontText.computeFloat(FontFace.SEMI, displayName, fontSize) > maxWidth && displayName.length() > 3) {
         displayName = displayName.substring(0, displayName.length() - 2) + "…";
      }

      CosmeticsSelectionScreen.DataRecord titleRecord = new CosmeticsSelectionScreen.DataRecord(
              displayName, FontText.computeFloat(FontFace.SEMI, displayName, fontSize)
      );
      this.titleCache.put(cosmeticEntry.id(), titleRecord);
      return titleRecord;
   }

   public boolean matchesCriteria3(float mouseX, float mouseY, float scrollDelta) {
      if (!UiTheme.matchesCriteria(mouseX, mouseY, this.detailPanelX, this.detailPanelY, 592.0F * this.uiScale, 396.0F * this.uiScale)) {
         return false;
      }

      this.targetScroll = clamp(this.targetScroll - scrollDelta * 87.0F, 0.0F, this.maxScroll);
      return true;
   }

   private void renderDetailPanel(float panelX, float panelY, float mouseX, float mouseY) {
      float panelProgress = this.computePanelSlideProgress(2);
      Render2D.pushAlpha(panelProgress);
      float contentX = panelX - (1.0F - panelProgress) * 14.0F * this.uiScale;
      float rowY = panelY + 214.0F * this.uiScale;
      this.renderSectionHeader("Образ", contentX, rowY, mouseX, mouseY, true);
      rowY += 13.0F * this.uiScale;
      boolean weaponsHeaderDrawn = false;

      for (CosmeticCategory cosmeticCategory : CosmeticCategory.values()) {
         if (cosmeticCategory.checkState() && !weaponsHeaderDrawn) {
            weaponsHeaderDrawn = true;
            rowY += 5.0F * this.uiScale;
            this.renderSectionHeader("Оружие", contentX, rowY, mouseX, mouseY, false);
            rowY += 13.0F * this.uiScale;
         }

         this.renderCategoryTab(cosmeticCategory, contentX, rowY, mouseX, mouseY);
         rowY += 22.5F * this.uiScale;
      }

      Render2D.popAlpha();
   }

   public boolean matchesCriteria4(int charCode) {
      if (this.searchFocused && charCode >= 32 && charCode <= 65535) {
         if (this.searchQuery.length() < 24) {
            this.searchQuery = this.searchQuery + (char)charCode;
         }

         this.targetScroll = 0.0F;
         return true;
      } else {
         return false;
      }
   }

   public void renderElement(
           DrawContext drawContext,
           float originX,
           float scale,
           float originY,
           float viewportWidth,
           float viewportHeight,
           float mouseX,
           float mouseY
   ) {
      this.uiScale = scale;
      float deltaTime = Math.min(0.05F, (float)(System.nanoTime() / 1000000L - this.lastFrameTimeMillis) / 1000.0F);
      this.lastFrameTimeMillis = System.nanoTime() / 1000000L;
      this.clickableRegions.clear();
      this.previousHoveredEntry = this.hoveredEntry;
      this.hoveredEntry = null;
      if (this.lastUiScale != scale) {
         this.titleCache.clear();
         this.lastUiScale = scale;
         this.equipLabelWidth = FontText.computeFloat(FontFace.SEMI_BOLD, "Надеть", 5.5F * scale);
         this.unequipLabelWidth = FontText.computeFloat(FontFace.SEMI_BOLD, "Снять", 5.5F * scale);
      }

      float contentX = originX + 51.5F * scale;
      this.refreshCatalog();
      this.renderSidebar(contentX, originY, mouseX, mouseY);
      this.renderSearchField(contentX, originY, mouseX, mouseY);
      this.renderDetailPanel(contentX, originY, mouseX, mouseY);
      this.renderDetailContent(drawContext, originX, originY, viewportWidth, viewportHeight, mouseX, mouseY, deltaTime);
   }

   public void updateState2() {
      this.searchFocused = false;
      this.draggingScrollbar = false;
   }

   private static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   private float animateEntryValue(HashMap<Integer, AnimatedValue> animationMap, int entryId, boolean active) {
      AnimatedValue animatedValue = animationMap.computeIfAbsent(entryId, id -> new AnimatedValue(140L));
      animatedValue.processData(active ? 1.0 : 0.0);
      return (float)animatedValue.computeDouble();
   }

   public boolean matchesCriteria5(float mouseX, float mouseY) {
      boolean wasFocused = this.searchFocused;
      this.searchFocused = false;
      if (this.maxScroll > 0.5F
              && UiTheme.matchesCriteria(
              mouseX, mouseY, this.scrollbarX - 4.0F * this.uiScale, this.listTop, 10.0F * this.uiScale, this.listBottom - this.listTop
      )) {
         float trackHeight = this.listBottom - this.listTop;
         float trackHeightLogical = trackHeight / this.uiScale;
         float thumbHeight = Math.max(24.0F * this.uiScale, trackHeight * (trackHeightLogical / Math.max(trackHeightLogical, this.contentHeight)));
         float thumbY = this.listTop + this.animatedScroll / this.maxScroll * (trackHeight - thumbHeight);
         this.dragGrabOffset = mouseY >= thumbY && mouseY <= thumbY + thumbHeight ? mouseY - thumbY : thumbHeight / 2.0F;
         this.draggingScrollbar = true;
         this.matchesCriteria2(mouseX, mouseY);
         return true;
      }

      for (int index = this.clickableRegions.size() - 1; index >= 0; index--) {
         CosmeticsSelectionScreen.DataRecord2 region = this.clickableRegions.get(index);
         if ((!region.clipped() || !(mouseY < this.listTop) && !(mouseY > this.listBottom))
                 && UiTheme.matchesCriteria(mouseX, mouseY, region.x(), region.y(), region.w(), region.h())) {
            region.action().run();
            return true;
         }
      }

      return wasFocused;
   }

   private static String pluralizeItems(int count) {
      int mod100 = count % 100;
      int mod10 = count % 10;
      return mod100 >= 11 && mod100 <= 14
              ? "предметов"
              : (mod10 == 1 ? "предмет" : (mod10 >= 2 && mod10 <= 4 ? "предмета" : "предметов"));
   }

   private void renderDetailContent(
           DrawContext drawContext,
           float originX,
           float originY,
           float viewportWidth,
           float viewportHeight,
           float mouseX,
           float mouseY,
           float deltaSeconds
   ) {
      float panelProgress = this.computePanelSlideProgress(3);
      Render2D.pushAlpha(panelProgress);
      this.detailPanelX = originX + 344.0F * this.uiScale + (1.0F - panelProgress) * 14.0F * this.uiScale;
      this.detailPanelY = originY + 92.0F * this.uiScale;
      float panelWidth = 592.0F * this.uiScale;
      float panelHeight = 396.0F * this.uiScale;
      float panelRadius = 16.0F * this.uiScale;
//
//      Render2D.drawRect(this.detailPanelX, this.detailPanelY, panelWidth, panelHeight, panelRadius, UiTheme.computeInt2(UiTheme.intVal7, UiTheme.intVal8, 0.15F));
//      Render2D.drawOutline(this.detailPanelX, this.detailPanelY, panelWidth, panelHeight, panelRadius, 1.0F, UiTheme.intVal34);

      float listTopBase = this.detailPanelY + 54.0F * this.uiScale;
      float panelContentX = this.detailPanelX + 16.0F * this.uiScale;
      float panelContentY = this.detailPanelY + 15.0F * this.uiScale;
      List<CosmeticEntry> categoryEntries = this.entriesByCategory.getOrDefault(this.selectedCategory, List.of());
      CosmeticEntry cosmeticEntry = CosmeticStore.getCosmeticEntry(this.selectedCategory);
      float categorySwitchProgress = UiTheme.computeFloat2(UiTheme.computeFloat3(this.categoryChangeTimeMillis, 0L, 280L));
      if (categorySwitchProgress < 0.999F && this.previousCategory != this.selectedCategory) {
         UiTheme.processData3(
                 this.previousCategory.formatText4(),
                 panelContentX + 13.5F * this.uiScale,
                 panelContentY + 13.5F * this.uiScale,
                 15.0F * this.uiScale * (1.0F - categorySwitchProgress * 0.4F),
                 15.0F * this.uiScale * (1.0F - categorySwitchProgress * 0.4F),
                 UiTheme.computeInt(UiTheme.intVal8, 1.0F - categorySwitchProgress)
         );
      }

      UiTheme.processData3(
              this.selectedCategory.formatText4(),
              panelContentX + 13.5F * this.uiScale,
              panelContentY + 13.5F * this.uiScale,
              15.0F * this.uiScale * (0.6F + 0.4F * categorySwitchProgress),
              15.0F * this.uiScale * (0.6F + 0.4F * categorySwitchProgress),
              UiTheme.computeInt(UiTheme.intVal8, categorySwitchProgress)
      );
      this.titleTransition
              .renderTransitionText(
                      FontFace.SEMI,
                      this.selectedCategory.formatText(),
                      panelContentX + 36.0F * this.uiScale,
                      panelContentY + 0.5F * this.uiScale,
                      13.0F * this.uiScale,
                      UiTheme.intVal,
                      7.0F * this.uiScale,
                      280L
              );
      String headerSubtitle = this.previousHoveredEntry != null
              ? this.previousHoveredEntry.name()
              : this.categoryCountTexts.getOrDefault(this.selectedCategory, "0") + " " + pluralizeItems(categoryEntries.size()) + (this.searchQuery.isBlank() ? "" : " по запросу");
      this.subtitleTransition
              .renderTransitionText(
                      FontFace.SEMI,
                      headerSubtitle,
                      panelContentX + 36.0F * this.uiScale,
                      panelContentY + 17.0F * this.uiScale,
                      6.5F * this.uiScale,
                      this.previousHoveredEntry != null ? UiTheme.computeInt(UiTheme.intVal, 0.85F) : UiTheme.intVal29,
                      3.0F * this.uiScale,
                      170L
              );
      this.renderEquippedBadge(
              cosmeticEntry,
              this.detailPanelX + panelWidth - 16.0F * this.uiScale - 158.0F * this.uiScale,
              panelContentY - 1.0F * this.uiScale,
              mouseX,
              mouseY
      );
      this.listTop = listTopBase + 8.0F * this.uiScale;
      this.listBottom = this.detailPanelY + panelHeight - 10.0F * this.uiScale;
      float listHeightLogical = (this.listBottom - this.listTop) / this.uiScale;
      float rowHeight = 87.0F;
      int rowCount = (categoryEntries.size() + 9 - 1) / 9;
      this.contentHeight = Math.max(0.0F, rowCount * rowHeight - 7.0F + 8.0F);
      this.maxScroll = Math.max(0.0F, this.contentHeight - listHeightLogical);
      this.targetScroll = clamp(this.targetScroll, 0.0F, this.maxScroll);
      this.animatedScroll = this.draggingScrollbar ? this.targetScroll : dampTowards(this.animatedScroll, this.targetScroll, deltaSeconds);
      Render2D.beginScissor(
              drawContext, panelContentX - 4.0F * this.uiScale, this.listTop, 568.0F * this.uiScale, this.listBottom - this.listTop
      );
      int firstVisibleRow = Math.max(0, (int)Math.floor((this.animatedScroll - 8.0F) / rowHeight));
      int lastVisibleRow = Math.min(rowCount - 1, (int)Math.ceil((this.animatedScroll + listHeightLogical) / rowHeight));
      boolean hasAccess = CosmeticAccess.checkState();

      for (int index = firstVisibleRow; index <= lastVisibleRow; index++) {
         for (int index2 = 0; index2 < 9; index2++) {
            int entryIndex = index * 9 + index2;
            if (entryIndex >= categoryEntries.size()) {
               break;
            }

            this.renderCosmeticCard(
                    categoryEntries.get(entryIndex),
                    panelContentX + index2 * 63.0F * this.uiScale,
                    this.listTop + (4.0F + index * rowHeight - this.animatedScroll) * this.uiScale,
                    mouseX,
                    mouseY,
                    hasAccess,
                    entryIndex - firstVisibleRow * 9
            );
         }
      }

      if (categoryEntries.isEmpty()) {
         String emptyTitle = this.searchQuery.isBlank() ? "Здесь пока пусто" : "Ничего не найдено";
         float emptyCenterX = this.detailPanelX + panelWidth / 2.0F;
         UiTheme.processData3(
                 "assets/greed/textures/gui/mainmenu/icons/search.png",
                 emptyCenterX,
                 this.listTop + 120.0F * this.uiScale,
                 14.0F * this.uiScale,
                 14.0F * this.uiScale,
                 UiTheme.computeInt(UiTheme.intVal, 0.22F)
         );
         FontText.processData3(
                 FontFace.SEMI,
                 emptyTitle,
                 emptyCenterX - FontText.computeFloat(FontFace.SEMI, emptyTitle, 7.5F * this.uiScale) / 2.0F,
                 this.listTop + 137.0F * this.uiScale,
                 7.5F * this.uiScale,
                 UiTheme.intVal30
         );
      }

      Render2D.endScissor(drawContext);
      this.scrollbarX = this.detailPanelX + panelWidth - 9.0F * this.uiScale;
      if (this.maxScroll > 0.5F) {
         float scrollTrackHeight = this.listBottom - this.listTop;
         float scrollThumbHeight = Math.max(24.0F * this.uiScale, scrollTrackHeight * (listHeightLogical / Math.max(listHeightLogical, this.contentHeight)));
         float scrollThumbY = this.listTop + this.animatedScroll / this.maxScroll * (scrollTrackHeight - scrollThumbHeight);
         float thumbHover = this.animateByKey(
                 "cosThumb",
                 this.draggingScrollbar
                         || UiTheme.matchesCriteria(
                         mouseX, mouseY, this.scrollbarX - 3.0F * this.uiScale, this.listTop, 8.0F * this.uiScale, scrollTrackHeight
                 )
         );
         Render2D.drawRect(
                 this.scrollbarX,
                 this.listTop,
                 2.0F * this.uiScale,
                 scrollTrackHeight,
                 1.0F * this.uiScale,
                 UiTheme.computeInt(UiTheme.intVal, 0.05F)
         );
         Render2D.drawRect(
                 this.scrollbarX - thumbHover * 0.5F * this.uiScale,
                 scrollThumbY,
                 (2.0F + thumbHover) * this.uiScale,
                 scrollThumbHeight,
                 1.5F * this.uiScale,
                 UiTheme.computeInt2(UiTheme.intVal31, UiTheme.computeInt(UiTheme.intVal34, 0.9F), thumbHover)
         );
      }

      Render2D.popAlpha();
   }

   private List<CosmeticEntry> getList(CosmeticCategory cosmeticCategory) {
      List<CosmeticEntry> allEntries = CosmeticCatalog.getList(cosmeticCategory);
      if (this.searchQuery.isBlank()) {
         return allEntries;
      }

      String queryLower = this.searchQuery.toLowerCase(Locale.ROOT);
      List<CosmeticEntry> filtered = new ArrayList<>();

      for (CosmeticEntry cosmeticEntry : allEntries) {
         if (cosmeticEntry.name().toLowerCase(Locale.ROOT).contains(queryLower)) {
            filtered.add(cosmeticEntry);
         }
      }

      return filtered;
   }

   public boolean checkState() {
      boolean wasDragging = this.draggingScrollbar;
      this.draggingScrollbar = false;
      return wasDragging;
   }

   private void renderSearchField(float fieldX, float fieldY, float mouseX, float mouseY) {
      float fieldProgress = this.computePanelSlideProgress(1);
      Render2D.pushAlpha(fieldProgress);
      float contentX = fieldX - (1.0F - fieldProgress) * 14.0F * this.uiScale;
      float contentY = fieldY + 184.5F * this.uiScale;
      float focusAnimation = this.animateByKey("cosFieldFocus", this.searchFocused);
      Render2D.drawRect(
              contentX,
              contentY,
              226.5F * this.uiScale,
              19.5F * this.uiScale,
              6.0F * this.uiScale,
              UiTheme.computeInt(UiTheme.intVal, 0.031F)
      );
      Render2D.drawOutline(
              contentX,
              contentY,
              226.5F * this.uiScale,
              19.5F * this.uiScale,
              6.0F * this.uiScale,
              0.5F,
              UiTheme.computeInt2(UiTheme.intVal33, UiTheme.computeInt(UiTheme.intVal34, 0.8F), focusAnimation)
      );
      UiTheme.processData3(
              "assets/greed/textures/gui/mainmenu/icons/search.png",
              contentX + 8.5F * this.uiScale,
              contentY + 9.75F * this.uiScale,
              6.5F * this.uiScale,
              6.5F * this.uiScale,
              UiTheme.computeInt2(UiTheme.intVal30, UiTheme.computeInt(UiTheme.intVal34, 0.95F), focusAnimation)
      );
      String query = this.searchQuery;
      boolean showPlaceholder = query.isEmpty() && !this.searchFocused;
      FontText.processData3(
              FontFace.SEMI,
              showPlaceholder ? "Найти предмет во всех категориях..." : query + (this.searchFocused && System.currentTimeMillis() % 1000L < 500L ? "|" : ""),
              contentX + 19.0F * this.uiScale,
              contentY + 6.25F * this.uiScale,
              7.0F * this.uiScale,
              query.isEmpty() ? UiTheme.intVal30 : UiTheme.intVal
      );
      this.clickableRegions
              .add(
                      new CosmeticsSelectionScreen.DataRecord2(contentX, contentY, 226.5F * this.uiScale, 19.5F * this.uiScale, false, () -> this.searchFocused = true)
              );
      float clearShownAnimation = this.animateByKey("cosSearchClearShown", !query.isEmpty());
      if (clearShownAnimation > 0.01F) {
         float clearHover = this.animateByKey(
                 "cosSearchClear",
                 UiTheme.matchesCriteria(
                         mouseX, mouseY, contentX + 207.0F * this.uiScale, contentY, 19.5F * this.uiScale, 19.5F * this.uiScale
                 )
         );
         UiTheme.processData7(
                 "c",
                 contentX + 216.75F * this.uiScale,
                 contentY + 9.75F * this.uiScale,
                 5.5F * this.uiScale * clearShownAnimation,
                 UiTheme.computeInt(
                         UiTheme.computeInt2(UiTheme.intVal29, UiTheme.intVal, clearHover), clearShownAnimation
                 )
         );
         if (!query.isEmpty()) {
            this.clickableRegions
                    .add(
                            new CosmeticsSelectionScreen.DataRecord2(
                                    contentX + 207.0F * this.uiScale, contentY, 19.5F * this.uiScale, 19.5F * this.uiScale, false, () -> {
                               this.searchQuery = "";
                               this.targetScroll = 0.0F;
                            }
                            )
                    );
         }
      }

      Render2D.popAlpha();
   }

   private record DataRecord(String text, float width) {
   }

   private record DataRecord2(float x, float y, float w, float h, boolean clipped, Runnable action) {
   }

   private static final class SubContext3 {
      private String currentText;
      private String previousText;
      private long transitionStartMillis;

      private float renderTransitionText(
              FontFace fontFace,
              String text,
              float x,
              float y,
              float fontSize,
              int color,
              float offset,
              long durationMillis
      ) {
         if (!text.equals(this.currentText)) {
            this.previousText = this.currentText;
            this.currentText = text;
            this.transitionStartMillis = System.currentTimeMillis();
         }

         float progress = UiTheme.computeFloat2(UiTheme.computeFloat3(this.transitionStartMillis, 0L, durationMillis));
         if (progress < 0.999F && this.previousText != null) {
            Render2D.pushAlpha(1.0F - progress);
            FontText.processData3(fontFace, this.previousText, x, y - offset * progress, fontSize, color);
            Render2D.popAlpha();
         }

         Render2D.pushAlpha(progress);
         FontText.processData3(fontFace, this.currentText, x, y + offset * (1.0F - progress), fontSize, color);
         Render2D.popAlpha();
         return progress;
      }
   }
}