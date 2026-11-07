package greed.client.module.render;

import greed.client.module.movement.AirStuckModule;
import greed.client.setting.BooleanSetting;
import greed.client.util.Colors;
import greed.client.event.EventListener;
import greed.client.render.FontFace;
import greed.client.storage.FriendStore;
import greed.client.event.HudRenderEvent;
import greed.client.util.ItemDrawOptions;
import greed.client.setting.LegacyNumberSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.setting.MultiChoiceSetting;
import greed.client.render.PlayerOutlineRenderer;
import greed.client.event.PlayerTickEvent;
import greed.client.util.GuiScale;
import greed.client.render.WorldProjection;
import greed.client.render.FontText;
import greed.client.render.ItemRenderCache;
import greed.client.util.ServerItemType;
import greed.client.util.PlayerItemCooldowns;
import greed.client.util.EntityUtils;
import greed.client.ui.UiTheme;
import greed.client.setting.ColorSetting;
import greed.client.util.GlintMode;
import greed.client.render.ThemeColors;
import greed.client.ui.hud.HudRenderContext;
import greed.client.ui.hud.HudStyle;
import greed.client.render.Render2D;
import greed.client.setting.SectionSetting;
import greed.client.module.player.ServerHelperModule;
import greed.client.event.WorldRenderEvent;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.AbstractWindChargeEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.joml.Vector4f;

@ModuleInfo(name = "Tags", description = "Ники, предметы, броня и руки игроков поверх мира.", category = ModuleCategory.RENDER)
public final class TagsModule extends Module {
   private static TagsModule tagsModule;
   private static int backgroundColor = new Color(15, 15, 15, 155).getRGB();
   private static int friendBackgroundColor = new Color(0, 255, 0, 40).getRGB();
   private static int redTextColor = new Color(255, 70, 70, 255).getRGB();
   private static int airStuckBackgroundColor = new Color(190, 30, 30, 150).getRGB();
   private static int grayTextColor = new Color(160, 160, 160, 255).getRGB();
   private static int greenTextColor = new Color(112, 255, 112, 255).getRGB();
   public MultiChoiceSetting display;
   public MultiChoiceSetting elements;
   public BooleanSetting showPlayerDonationPotions;
   public BooleanSetting applyShader;
   public LegacyNumberSetting outline;
   public LegacyNumberSetting highlight;
   public LegacyNumberSetting fill;
   public BooleanSetting customColor;
   public ColorSetting color;
   public BooleanSetting box;
   public BooleanSetting customColor2;
   public ColorSetting color2;
   private final HashMap<Integer, Vec3d> lastPositionById = new HashMap<>();
   private final HashMap<Integer, Integer> airStuckTicksById = new HashMap<>();
   private final HashMap<Integer, Integer> stationaryTicksById = new HashMap<>();
   private final HashMap<Integer, Integer> cooldownTicksById = new HashMap<>();
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (mc.world != null) {
         if (this.lastPositionById.size() > 256) {
            this.lastPositionById.clear();
            this.airStuckTicksById.clear();
            this.stationaryTicksById.clear();
            this.cooldownTicksById.clear();
         }

         for (AbstractClientPlayerEntity abstractClientPlayerEntity : mc.world.getPlayers()) {
            Vec3d lerpedPos = abstractClientPlayerEntity.getInterpolator().getLerpedPos();
            Vec3d vec3d = this.lastPositionById.put(abstractClientPlayerEntity.getId(), lerpedPos);
            boolean isStationary = !abstractClientPlayerEntity.hasVehicle()
               && !abstractClientPlayerEntity.isGliding()
               && abstractClientPlayerEntity.getGameMode() != GameMode.CREATIVE
               && abstractClientPlayerEntity.getGameMode() != GameMode.SPECTATOR
               && !abstractClientPlayerEntity.isTouchingWater()
               && !abstractClientPlayerEntity.isInLava()
               && !abstractClientPlayerEntity.isClimbing()
               && mc.world.isSpaceEmpty(abstractClientPlayerEntity, abstractClientPlayerEntity.getBoundingBox().offset(0.0, -0.0625, 0.0));
            boolean isPositionUnchanged = vec3d != null && vec3d.squaredDistanceTo(lerpedPos) < 1.0E-4;
            int stationaryTicks = isStationary && vec3d != null && !isPositionUnchanged ? this.stationaryTicksById.getOrDefault(abstractClientPlayerEntity.getId(), 0) + 1 : 0;
            this.stationaryTicksById.put(abstractClientPlayerEntity.getId(), stationaryTicks);
            int cooldownTicks = stationaryTicks >= 25 ? 40 : Math.max(0, this.cooldownTicksById.getOrDefault(abstractClientPlayerEntity.getId(), 0) - 1);
            this.cooldownTicksById.put(abstractClientPlayerEntity.getId(), cooldownTicks);
            this.airStuckTicksById
               .put(abstractClientPlayerEntity.getId(), isStationary && isPositionUnchanged && cooldownTicks == 0 ? this.airStuckTicksById.getOrDefault(abstractClientPlayerEntity.getId(), 0) + 1 : 0);
         }
      }
   };
   private final EventListener<HudRenderEvent> hudRenderListener = hudRenderEvent -> {
      if (mc.player != null && mc.world != null && mc.gameRenderer != null) {
         HudRenderContext.renderElement(hudRenderEvent.getDrawContext());
         int scaledWidth = mc.getWindow().getScaledWidth();
         int scaledHeight = mc.getWindow().getScaledHeight();
         float guiScale = GuiScale.computeInt() / GuiScale.computeFloat();
         if (this.clientWorld != mc.world || this.itemRecordsById.size() > 512) {
            this.itemRecordsById.clear();
            this.clientWorld = mc.world;
         }

         if (this.shouldShowDonationItems()) {
            PlayerItemCooldowns.processData2(mc.world);
         }

         for (Entity entity : mc.world.getEntities()) {
            boolean wasRendered = false;
            if (entity instanceof AbstractClientPlayerEntity abstractClientPlayerEntity2 && this.box.isChecked()) {
               this.drawPlayerBox(abstractClientPlayerEntity2, hudRenderEvent.computeFloat(), scaledWidth, scaledHeight, guiScale);
            }

            if (entity instanceof AbstractClientPlayerEntity abstractClientPlayerEntity3) {
               wasRendered = this.renderPlayerLabels(abstractClientPlayerEntity3, hudRenderEvent.computeFloat(), scaledWidth, scaledHeight, guiScale);
            } else if (entity instanceof ItemEntity itemEntity && this.elements.isIndexSelected(1)) {
               wasRendered = this.renderItemEntityLabel(itemEntity, hudRenderEvent.computeFloat(), scaledWidth, scaledHeight, guiScale);
            } else if (entity instanceof ProjectileEntity projectileEntity && this.elements.isIndexSelected(1)) {
               wasRendered = this.renderProjectileLabel(projectileEntity, hudRenderEvent.computeFloat(), scaledWidth, scaledHeight, guiScale);
            }

            if (wasRendered) {
               Render2D.bumpGuiLayer(hudRenderEvent.getDrawContext());
            }
         }
      }
   };
   private final HashMap<Integer, TagsModule.DataRecord> itemRecordsById = new HashMap<>();
   private ClientWorld clientWorld;
   private final EventListener<WorldRenderEvent> worldRenderListener = worldRenderEvent -> {};
   private static final int[][] boxFaceIndices = new int[][]{{0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}};

   private ItemStack getProjectileItemStack(ProjectileEntity projectileEntity) {
      ItemStack itemStack = projectileEntity instanceof PersistentProjectileEntity persistentProjectileEntity
         ? persistentProjectileEntity.getItemStack()
         : (projectileEntity instanceof FlyingItemEntity flyingItemEntity ? flyingItemEntity.getStack() : ItemStack.EMPTY);
      return itemStack.isEmpty() && projectileEntity instanceof AbstractWindChargeEntity ? Items.WIND_CHARGE.getDefaultStack() : itemStack;
   }

   private int getPlayerColor(AbstractClientPlayerEntity abstractClientPlayerEntity, boolean isFriend) {
      return this.matchesCriteria8(abstractClientPlayerEntity) ? airStuckBackgroundColor : (isFriend ? friendBackgroundColor : Colors.computeInt7(ThemeColors.intVal, 133));
   }

   private void drawEnchantLabel(ItemStack itemStack, float x, float y) {
      int enchantLevel = 0;
      String prefixText;
      if (!this.hasMaceEnchant(itemStack) && !this.hasSharpness8(itemStack)) {
         if (itemStack.isIn(ItemTags.SWORDS) && (enchantLevel = this.getEnchantmentLevel(itemStack, "minecraft:knockback")) > 0) {
            prefixText = "От";
         } else {
            if (!this.hasProtectionEnchant(itemStack)) {
               return;
            }

            prefixText = "По";
            enchantLevel = this.getEnchantmentLevel(itemStack, "minecraft:mending");
         }
      } else {
         prefixText = "Кр";
      }

      String levelText = enchantLevel > 0 ? String.valueOf(enchantLevel) : "";
      float prefixWidth = FontText.computeFloat(FontFace.SEMI_BOLD, prefixText, 4.5F);
      float textX = x + 4.0F - (prefixWidth + FontText.computeFloat(FontFace.SEMI_BOLD, levelText, 4.5F)) / 2.0F;
      FontText.processData3(FontFace.SEMI_BOLD, prefixText, textX, y - 5.5F, 4.5F, ThemeColors.intVal5);
      if (!levelText.isEmpty()) {
         FontText.processData3(FontFace.SEMI_BOLD, levelText, textX + prefixWidth, y - 5.5F, 4.5F, redTextColor);
      }
   }

   private boolean hasSharpness8(ItemStack itemStack) {
      if (!itemStack.isIn(ItemTags.SWORDS)) {
         return false;
      }

      HashSet<String> enchantmentKeys = new HashSet<>();

      for (Entry<RegistryEntry<Enchantment>> enchantmentEntry : itemStack.getEnchantments().getEnchantmentEntries()) {
         enchantmentKeys.add(((RegistryEntry)enchantmentEntry.getKey()).getIdAsString() + enchantmentEntry.getIntValue());
      }

      return enchantmentKeys.contains("minecraft:sharpness8");
   }

   private boolean hasCombatEnchant(ItemStack itemStack) {
      if (!itemStack.isIn(ItemTags.SWORDS)) {
         return false;
      }

      HashSet<String> enchantmentKeys = new HashSet<>();

      for (Entry<RegistryEntry<Enchantment>> enchantmentEntry : itemStack.getEnchantments().getEnchantmentEntries()) {
         enchantmentKeys.add(((RegistryEntry)enchantmentEntry.getKey()).getIdAsString() + enchantmentEntry.getIntValue());
      }

      return enchantmentKeys.contains("minecraft:knockback1") || enchantmentKeys.contains("minecraft:knockback2") || enchantmentKeys.contains("minecraft:sharpness8");
   }

   private boolean hasProtectionEnchant(ItemStack itemStack) {
      HashSet<String> enchantmentKeys = new HashSet<>();

      for (Entry<RegistryEntry<Enchantment>> enchantmentEntry : itemStack.getEnchantments().getEnchantmentEntries()) {
         enchantmentKeys.add(((RegistryEntry)enchantmentEntry.getKey()).getIdAsString() + enchantmentEntry.getIntValue());
      }

      return enchantmentKeys.contains("minecraft:protection5") && (enchantmentKeys.contains("minecraft:mending1") || enchantmentKeys.contains("minecraft:mending2"));
   }

   @Override
   protected void onDisable() {
      PlayerOutlineRenderer.getInstance().updateState();
      this.lastPositionById.clear();
      this.airStuckTicksById.clear();
      this.stationaryTicksById.clear();
      this.cooldownTicksById.clear();
      this.itemRecordsById.clear();
      this.clientWorld = null;
   }

   public boolean checkState() {
      return this.isEnabled() && this.applyShader.isChecked();
   }

   private TagsModule() {
      this.display = this.addSetting(
         new MultiChoiceSetting("Отображать", "На ком рисовать элементы и шейдер.")
            .withOptions("Игроков", "Друзей", "Себя")
            .withSelection("Игроков", "Друзей", "Себя")
      );
      this.elements = this.addSetting(
         new MultiChoiceSetting("Элементы", "Что рисовать поверх игроков и предметов.")
            .withOptions("Ники", "Предметы", "Броню", "Руки")
            .withSelection("Ники", "Предметы", "Руки")
      );
      this.showPlayerDonationPotions = this.addSetting(
         new BooleanSetting("Показывать донатные зелья игроков", "Донатные зелья, которыми задело игроков и тебя, и сколько осталось.", true)
      );
      this.addSetting(new SectionSetting("Применить шейдер"));
      this.applyShader = this.addSetting(new BooleanSetting("Применить шейдер", "Обводка и тень вокруг игроков.", false));
      this.outline = this.addSetting(new LegacyNumberSetting("Оутлайн", "Сила обводки.", 0.8, 0.0, 1.0, 0.05));
      this.highlight = this.addSetting(new LegacyNumberSetting("Подсветка", "Сила свечения вокруг игроков.", 0.55, 0.0, 3.0, 0.05));
      this.fill = this.addSetting(new LegacyNumberSetting("Заливка", "Заливка игроков цветом.", 0.0, 0.0, 1.0, 0.05));
      this.customColor = this.addSetting(new BooleanSetting("Кастомный цвет", "Свой цвет вместо акцента темы.", false));
      this.color = this.addSetting(new ColorSetting("Цвет", "Цвет обводки и тени."));
      this.outline.withVisibility(() -> this.applyShader.isChecked());
      this.highlight.withVisibility(() -> this.applyShader.isChecked());
      this.fill.withVisibility(() -> this.applyShader.isChecked());
      this.customColor.withVisibility(() -> this.applyShader.isChecked());
      this.color.withVisibility(() -> this.applyShader.isChecked() && this.customColor.isChecked());
      this.addSetting(new SectionSetting("Бокс"));
      this.box = this.addSetting(new BooleanSetting("Бокс", "Показывать хитбоксы игроков в мире.", false));
      this.customColor2 = this.addSetting(new BooleanSetting("Кастомный цвет", "Свой цвет вместо акцента темы.", false));
      this.color2 = this.addSetting(new ColorSetting("Цвет", "Цвет обводки и заливки хитбокса."));
      this.customColor2.withVisibility(() -> this.box.isChecked());
      this.color2.withVisibility(() -> this.box.isChecked() && this.customColor2.isChecked());
   }

   public float computeFloat(float distance) {
      return Math.max(0.65F, 1.1F - distance * 0.004F);
   }

   public float computeFloat2(float distance) {
      return this.isEnabled() && this.elements.isIndexSelected(1) ? GuiScale.computeFloat4(11.0F * this.computeFloat(distance)) : 0.0F;
   }

   private void drawBoxEdge(float x, float y, float width, float height) {
      Render2D.drawRect(
         Math.min(x, x + width) - (width == 0.0F ? 0.875F : 0.5F),
         Math.min(y, y + height) - (height == 0.0F ? 0.875F : 0.5F),
         (width == 0.0F ? 0.75F : Math.abs(width)) + 1.0F,
         (height == 0.0F ? 0.75F : Math.abs(height)) + 1.0F,
         0.0F,
         -16777216
      );
      Render2D.drawRect(
         Math.min(x, x + width) - (width == 0.0F ? 0.375F : 0.0F),
         Math.min(y, y + height) - (height == 0.0F ? 0.375F : 0.0F),
         width == 0.0F ? 0.75F : Math.abs(width),
         height == 0.0F ? 0.75F : Math.abs(height),
         0.0F,
         0xFF000000 | this.computeInt3() & 16777215
      );
   }

   private boolean renderPlayerLabels(AbstractClientPlayerEntity abstractClientPlayerEntity, float tickDelta, int screenWidth, int screenHeight, float scale) {
      if (abstractClientPlayerEntity == mc.player && mc.options.getPerspective().isFirstPerson()) {
         return false;
      }

      if (!this.matchesCriteria5(abstractClientPlayerEntity)) {
         return false;
      }

      boolean isFriend = FriendStore.isFriend(abstractClientPlayerEntity.getGameProfile().name());
      float distance = (float)mc.gameRenderer.getCamera().getCameraPos().distanceTo(abstractClientPlayerEntity.getLerpedPos(tickDelta));
      float alpha = Math.max(0.3F, 1.0F - distance * 0.008F);
      Vec3d vec3d = WorldProjection.getVec3d2(abstractClientPlayerEntity.getLerpedPos(tickDelta), screenWidth, screenHeight);
      if (vec3d != null) {
         float labelY = (float)vec3d.y * scale + 2.0F;
         if (this.elements.isIndexSelected(3)) {
            labelY = this.drawHandItems(
               abstractClientPlayerEntity, isFriend, (float)vec3d.x * scale, labelY, alpha * Math.max(0.7F, 1.0F - distance * 0.005F)
            );
         }

         if (this.shouldShowDonationItems()) {
            this.drawItemCooldowns(abstractClientPlayerEntity, (float)vec3d.x * scale, labelY, alpha * Math.max(0.7F, 1.0F - distance * 0.005F));
         }
      }

      if (this.elements.isIndexSelected(0)) {
         Vec3d vec3d2 = WorldProjection.getVec3d2(
            abstractClientPlayerEntity.getLerpedPos(tickDelta).add(0.0, abstractClientPlayerEntity.getBoundingBox().maxY - abstractClientPlayerEntity.getBoundingBox().minY + 0.3, 0.0),
            screenWidth,
            screenHeight
         );
         if (vec3d2 != null) {
            this.drawNametag(abstractClientPlayerEntity, isFriend, (float)vec3d2.x * scale, (float)vec3d2.y * scale, distance, alpha);
         }
      }

      return true;
   }

   public int computeInt2() {
      return this.customColor.isChecked() ? this.color.getValue() : UiTheme.intVal7;
   }

   private float drawHandItems(AbstractClientPlayerEntity abstractClientPlayerEntity, boolean isFriend, float x, float y, float alpha) {
      ItemStack mainHandStack = abstractClientPlayerEntity.getMainHandStack();
      ItemStack offHandStack = abstractClientPlayerEntity.getOffHandStack();
      if (mainHandStack.isEmpty() && offHandStack.isEmpty()) {
         return y;
      }

      float rowHeight = 12.0F;
      HudRenderContext.processData3(x, y, alpha);
      float currentY = y + 1.0F;

      for (ItemStack itemStack : new ItemStack[]{mainHandStack, offHandStack}) {
         if (!itemStack.isEmpty()) {
            String countText = itemStack.getCount() > 1 ? " " + itemStack.getCount() + "x" : "";
            float totalWidth = FontText.computeFloat(FontFace.SEMI_BOLD, itemStack.getName().getString(), 6.0F)
               + FontText.computeFloat(FontFace.SEMI_BOLD, countText, 6.0F);
            Render2D.drawRect(
               x - totalWidth / 2.0F - 2.0F, currentY, totalWidth + 4.0F, rowHeight, 0.0F, this.getPlayerColor(abstractClientPlayerEntity, isFriend)
            );
            FontText.processData5(
               FontFace.SEMI_BOLD,
               itemStack.getName()
                  .copy()
                  .styled(
                     style -> style.getColor() != null && (style.getColor().getRgb() & 16777215) != 16777215
                        ? style
                        : style.withColor(TextColor.fromRgb(ThemeColors.intVal5 & 16777215))
                  ),
               x - totalWidth / 2.0F,
               currentY + rowHeight / 2.0F - 3.65F,
               6.0F,
               ThemeColors.intVal5
            );
            if (!countText.isEmpty()) {
               FontText.processData3(
                  FontFace.SEMI_BOLD,
                  countText,
                  x - totalWidth / 2.0F + FontText.computeFloat(FontFace.SEMI_BOLD, itemStack.getName().getString(), 6.0F),
                  currentY + rowHeight / 2.0F - 3.65F,
                  6.0F,
                  ThemeColors.intVal5
               );
            }

            currentY += rowHeight + 1.0F;
         }
      }

      HudRenderContext.updateState();
      return y + (currentY - y) * alpha;
   }

   private void drawPlayerBox(AbstractClientPlayerEntity abstractClientPlayerEntity, float tickDelta, int screenWidth, int screenHeight, float scale) {
      if ((abstractClientPlayerEntity != mc.player || !mc.options.getPerspective().isFirstPerson()) && this.matchesCriteria5(abstractClientPlayerEntity)) {
         Vector4f vector4f = WorldProjection.getVector4f(
            abstractClientPlayerEntity.getBoundingBox().offset(abstractClientPlayerEntity.getLerpedPos(tickDelta).subtract(abstractClientPlayerEntity.getEntityPos())), screenWidth, screenHeight
         );
         if (vector4f != null && !(vector4f.z <= 0.0F) && !(vector4f.w <= 0.0F)) {
            this.drawBoxEdge(vector4f.x * scale, vector4f.y * scale, vector4f.z * scale, 0.0F);
            this.drawBoxEdge(vector4f.x * scale, (vector4f.y + vector4f.w) * scale, vector4f.z * scale, 0.0F);
            this.drawBoxEdge(vector4f.x * scale, vector4f.y * scale, 0.0F, vector4f.w * scale);
            this.drawBoxEdge((vector4f.x + vector4f.z) * scale, vector4f.y * scale, 0.0F, vector4f.w * scale);
         }
      }
   }

   public boolean matchesCriteria5(AbstractClientPlayerEntity abstractClientPlayerEntity) {
      if (abstractClientPlayerEntity == mc.player) {
         return this.display.matchesOption("Себя");
      } else {
         return FriendStore.isFriend(abstractClientPlayerEntity.getGameProfile().name())
            ? this.display.matchesOption("Друзей")
            : this.display.matchesOption("Игроков");
      }
   }

   public static TagsModule getInstance() {
      if (tagsModule == null) {
         tagsModule = new TagsModule();
      }

      return tagsModule;
   }

   private boolean renderItemEntityLabel(ItemEntity itemEntity, float tickDelta, int screenWidth, int screenHeight, float scale) {
      return this.renderItemLabel(itemEntity.getStack(), itemEntity.getLerpedPos(tickDelta), itemEntity.getId(), screenWidth, screenHeight, scale);
   }

   private void drawItemCooldowns(AbstractClientPlayerEntity abstractClientPlayerEntity, float x, float y, float alpha) {
      Map<ServerItemType, Long> cooldowns = PlayerItemCooldowns.getMap(abstractClientPlayerEntity);
      if (!cooldowns.isEmpty()) {
         long nowMillis = System.currentTimeMillis();
         float currentY = y + 2.0F;
         HudRenderContext.processData3(x, y, alpha);

         for (java.util.Map.Entry<ServerItemType, Long> cooldownEntry : cooldowns.entrySet()) {
            String secondsText = " " + Math.max(1L, (cooldownEntry.getValue() - nowMillis + 999L) / 1000L) + "с";
            float nameWidth = FontText.computeFloat(FontFace.SEMI_BOLD, cooldownEntry.getKey().formatText(), 5.0F);
            float totalWidth = nameWidth + FontText.computeFloat(FontFace.SEMI_BOLD, secondsText, 5.0F);
            FontText.processData5(
               FontFace.SEMI_BOLD,
               cooldownEntry.getKey().getText2(),
               x - totalWidth / 2.0F,
               currentY,
               5.0F,
               ThemeColors.intVal5
            );
            FontText.processData3(
               FontFace.SEMI_BOLD,
               secondsText,
               x - totalWidth / 2.0F + nameWidth,
               currentY,
               5.0F,
               HudStyle.computeInt2(1.0F)
            );
            currentY += 6.5F;
         }

         HudRenderContext.updateState();
      }
   }

   public int computeInt3() {
      return this.customColor2.isChecked() ? this.color2.getValue() : UiTheme.intVal7;
   }

   private boolean hasMaceEnchant(ItemStack itemStack) {
      if (!itemStack.isOf(Items.MACE)) {
         return false;
      }

      HashSet<String> enchantmentKeys = new HashSet<>();

      for (Entry<RegistryEntry<Enchantment>> enchantmentEntry : itemStack.getEnchantments().getEnchantmentEntries()) {
         enchantmentKeys.add(((RegistryEntry)enchantmentEntry.getKey()).getIdAsString() + enchantmentEntry.getIntValue());
      }

      return enchantmentKeys.contains("minecraft:breach3") && enchantmentKeys.contains("minecraft:density5");
   }

   private int getEnchantmentLevel(ItemStack itemStack, String enchantmentId) {
      for (Entry<RegistryEntry<Enchantment>> enchantmentEntry : itemStack.getEnchantments().getEnchantmentEntries()) {
         if (((RegistryEntry)enchantmentEntry.getKey()).getIdAsString().equals(enchantmentId)) {
            return enchantmentEntry.getIntValue();
         }
      }

      return 0;
   }

   private boolean shouldShowArmor() {
      return this.elements.isIndexSelected(2)
         || ServerHelperModule.getInstance().isEnabled() && ServerHelperModule.getInstance().modeSetting.isSelected("SpookyTime");
   }

   private void drawNametag(
      AbstractClientPlayerEntity abstractClientPlayerEntity, boolean isFriend, float x, float y, float distance, float alpha
   ) {
      String name = abstractClientPlayerEntity.getGameProfile().name();
      String healthText = EntityUtils.formatHealth(abstractClientPlayerEntity);
      Team team = mc.world.getScoreboard().getScoreHolderTeam(abstractClientPlayerEntity.getNameForScoreboard());
      Text text = team == null ? null : team.getPrefix();
      String teamPrefix = text == null ? "" : text.getString().trim();
      String maxHealthText = String.valueOf(Math.round(distance));
      float nameWidth = FontText.computeFloat(FontFace.SEMI_BOLD, name, 6.0F);
      float gap = 3.0F;
      float prefixWidth = teamPrefix.isEmpty() ? 0.0F : FontText.computeFloat(FontFace.SEMI_BOLD, teamPrefix, 6.0F) + gap;
      boolean isAirStuck = this.matchesCriteria8(abstractClientPlayerEntity);
      float airStuckWidth = isAirStuck ? FontText.computeFloat(FontFace.SEMI_BOLD, "AIRSTUCK", 6.0F) + gap : 0.0F;
      float totalWidth = 2.0F
         + prefixWidth
         + airStuckWidth
         + nameWidth
         + gap
         + FontText.computeFloat(FontFace.SEMI_BOLD, "[", 6.0F)
         + FontText.computeFloat(FontFace.SEMI_BOLD, healthText, 6.0F)
         + FontText.computeFloat(FontFace.SEMI_BOLD, "/", 6.0F)
         + FontText.computeFloat(FontFace.SEMI_BOLD, maxHealthText, 6.0F)
         + FontText.computeFloat(FontFace.SEMI_BOLD, "]", 6.0F)
         + 2.0F;
      float boxHeight = 12.0F;
      float boxX = x - totalWidth / 2.0F;
      float boxY = y - boxHeight - 3.0F + 6.0F - Math.min(6.0F, distance * 0.08F);
      float textY = boxY + boxHeight / 2.0F - 3.65F;
      HudRenderContext.processData3(x, y, alpha * Math.max(0.7F, 1.0F - distance * 0.005F));
      Render2D.drawRect(
         boxX,
         boxY,
         totalWidth,
         boxHeight,
         0.0F,
         isAirStuck ? airStuckBackgroundColor : (isFriend ? friendBackgroundColor : Colors.computeInt7(ThemeColors.intVal, 133))
      );
      float currentX = boxX + 2.0F;
      if (!teamPrefix.isEmpty()) {
         FontText.processData5(FontFace.SEMI_BOLD, text, currentX, textY, 6.0F, ThemeColors.intVal5);
         currentX += FontText.computeFloat(FontFace.SEMI_BOLD, teamPrefix, 6.0F) + gap;
      }

      if (isAirStuck) {
         FontText.processData3(FontFace.SEMI_BOLD, "AIRSTUCK", currentX, textY, 6.0F, redTextColor);
         currentX += FontText.computeFloat(FontFace.SEMI_BOLD, "AIRSTUCK", 6.0F) + gap;
      }

      FontText.processData3(FontFace.SEMI_BOLD, name, currentX, textY, 6.0F, ThemeColors.intVal5);
      currentX += nameWidth + gap;
      FontText.processData3(FontFace.SEMI_BOLD, "[", currentX, textY, 6.0F, grayTextColor);
      currentX += FontText.computeFloat(FontFace.SEMI_BOLD, "[", 6.0F);
      FontText.processData3(FontFace.SEMI_BOLD, healthText, currentX, textY, 6.0F, redTextColor);
      currentX += FontText.computeFloat(FontFace.SEMI_BOLD, healthText, 6.0F);
      FontText.processData3(FontFace.SEMI_BOLD, "/", currentX, textY, 6.0F, grayTextColor);
      currentX += FontText.computeFloat(FontFace.SEMI_BOLD, "/", 6.0F);
      FontText.processData3(FontFace.SEMI_BOLD, maxHealthText, currentX, textY, 6.0F, greenTextColor);
      currentX += FontText.computeFloat(FontFace.SEMI_BOLD, maxHealthText, 6.0F);
      FontText.processData3(FontFace.SEMI_BOLD, "]", currentX, textY, 6.0F, grayTextColor);
      if (!this.shouldShowArmor()) {
         ArrayList<ItemStack> items = new ArrayList<>();
         if (this.hasCombatEnchant(abstractClientPlayerEntity.getMainHandStack()) || this.hasMaceEnchant(abstractClientPlayerEntity.getMainHandStack())) {
            items.add(abstractClientPlayerEntity.getMainHandStack());
         }

         for (ItemStack itemStack : new ItemStack[]{
            abstractClientPlayerEntity.getEquippedStack(EquipmentSlot.HEAD),
            abstractClientPlayerEntity.getEquippedStack(EquipmentSlot.CHEST),
            abstractClientPlayerEntity.getEquippedStack(EquipmentSlot.LEGS),
            abstractClientPlayerEntity.getEquippedStack(EquipmentSlot.FEET)
         }) {
            if (this.hasProtectionEnchant(itemStack)) {
               items.add(itemStack);
            }
         }

         currentX = x - (items.size() * 8.0F + (items.size() - 1) * 2.0F) / 2.0F;

         for (ItemStack itemStack2 : items) {
            this.drawEnchantLabel(itemStack2, currentX, boxY - 10.0F - 1.5F + 1.0F);
            ItemRenderCache.processData(
               itemStack2, currentX, boxY - 10.0F - 1.5F + 1.0F, 8.0F, ItemDrawOptions.noDecorations(1.0F).glint(GlintMode.OFF)
            );
            currentX += 10.0F;
         }
      }

      if (this.shouldShowArmor()) {
         ArrayList<ItemStack> items = new ArrayList<>();

         for (ItemStack itemStack3 : new ItemStack[]{
            abstractClientPlayerEntity.getMainHandStack(),
            abstractClientPlayerEntity.getEquippedStack(EquipmentSlot.HEAD),
            abstractClientPlayerEntity.getEquippedStack(EquipmentSlot.CHEST),
            abstractClientPlayerEntity.getEquippedStack(EquipmentSlot.LEGS),
            abstractClientPlayerEntity.getEquippedStack(EquipmentSlot.FEET),
            abstractClientPlayerEntity.getOffHandStack()
         }) {
            if (!itemStack3.isEmpty()) {
               items.add(itemStack3);
            }
         }

         if (!items.isEmpty()) {
            currentX = boxX + 2.0F;

            for (ItemStack itemStack4 : items) {
               this.drawEnchantLabel(itemStack4, currentX, boxY - 10.0F - 1.5F + 1.0F);
               ItemRenderCache.processData(
                  itemStack4, currentX, boxY - 10.0F - 1.5F + 1.0F, 8.0F, ItemDrawOptions.decorated(1.0F).glint(GlintMode.OFF)
               );
               currentX += 10.0F;
            }
         }
      }

      HudRenderContext.updateState();
   }

   public boolean matchesCriteria8(AbstractClientPlayerEntity abstractClientPlayerEntity) {
      if (abstractClientPlayerEntity == mc.player) {
         return AirStuckModule.getInstance().isFrozenInAir();
      } else {
         return abstractClientPlayerEntity.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA) ? false : this.airStuckTicksById.getOrDefault(abstractClientPlayerEntity.getId(), 0) >= 20;
      }
   }

   private boolean shouldShowDonationItems() {
      return this.showPlayerDonationPotions.isChecked() && ServerItemType.checkState();
   }

   private boolean renderItemLabel(ItemStack itemStack, Vec3d vec3d, int entityId, int screenWidth, int screenHeight, float scale) {
      if (itemStack.isEmpty()) {
         return false;
      }

      Vec3d vec3d2 = WorldProjection.getVec3d2(vec3d.add(0.0, 0.25, 0.0), screenWidth, screenHeight);
      if (vec3d2 == null) {
         return false;
      }

      TagsModule.DataRecord dataRecord = this.itemRecordsById.get(entityId);
      if (dataRecord == null || dataRecord.stack() != itemStack && !ItemStack.areItemsAndComponentsEqual(dataRecord.stack(), itemStack)) {
         dataRecord = new TagsModule.DataRecord(
            itemStack,
            itemStack.getName().getString(),
            itemStack.getName()
               .copy()
               .styled(
                  style -> style.getColor() != null && (style.getColor().getRgb() & 16777215) != 16777215
                     ? style
                     : style.withColor(TextColor.fromRgb(ThemeColors.intVal5 & 16777215))
               )
         );
         this.itemRecordsById.put(entityId, dataRecord);
      }

      String label = dataRecord.label();
      float distance = (float)mc.gameRenderer.getCamera().getCameraPos().distanceTo(vec3d);
      float alpha = this.computeFloat(distance);
      float boxHeight = 9.0F;
      float totalWidth = 2.0F + FontText.computeFloat(FontFace.SEMI_BOLD, label, 4.0F) + 2.0F;
      float centerX = (float)vec3d2.x * scale;
      float topY = (float)vec3d2.y * scale - boxHeight - 2.0F;
      float boxX = centerX - totalWidth / 2.0F;
      float centerY = topY + boxHeight / 2.0F;
      HudRenderContext.processData3(centerX, (float)vec3d2.y * scale, alpha);
      Render2D.drawRect(
         boxX, topY, totalWidth, boxHeight, 0.0F, Colors.computeInt7(ThemeColors.intVal, 133)
      );
      FontText.processData5(
         FontFace.SEMI_BOLD,
         dataRecord.styledName(),
         boxX + 2.0F,
         topY + boxHeight / 2.0F - 2.45F,
         4.0F,
         ThemeColors.intVal5
      );
      HudRenderContext.updateState();
      return true;
   }

   private boolean renderProjectileLabel(ProjectileEntity projectileEntity, float tickDelta, int screenWidth, int screenHeight, float scale) {
      return this.renderItemLabel(this.getProjectileItemStack(projectileEntity), projectileEntity.getLerpedPos(tickDelta), projectileEntity.getId(), screenWidth, screenHeight, scale);
   }

   private record DataRecord(ItemStack stack, String label, Text styledName) {
   }
}
