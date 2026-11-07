package greed.client.ui.hud;

import greed.client.render.CornerRadii;
import greed.client.util.EntityUtils;
import greed.client.render.FontText;
import greed.client.util.GlintMode;
import greed.client.render.ItemRenderCache;
import greed.client.render.ShapeDrawing;
import greed.client.render.ThemeColors;
import greed.client.ui.UiTheme;

import greed.client.util.Animation;

import greed.client.module.combat.AttackAuraModule;
import greed.client.util.Colors;
import greed.client.util.Easings;
import greed.client.render.FontFace;
import greed.client.module.render.InterfaceModule;
import greed.client.util.ItemDrawOptions;
import greed.client.render.Render2D;
import greed.client.module.player.ServerHelperModule;
import greed.client.module.misc.YTMODEModule;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public final class TargetHud {
   private static final float PANEL_WIDTH = 112.0F;
   private static final float PANEL_HEIGHT = 30.0F;
   private static final float PANEL_PAD = 4.5F;
   private static final float PANEL_RADIUS = 7.0F;
   private static final float HEAD_SIZE = 21.0F;
   private static final float HEAD_RADIUS = 3.0F;
   private static final float TEXT_X = 32.0F;
   private static final float FONT_SIZE = 7.0F;
   private static final float NAME_Y = 8.0F;
   private static final float BAR_Y = 21.0F;
   private static final float BAR_HEIGHT = 2.0F;
   private static final float ROW_HEIGHT = 14.0F;
   private static final float ROW_GAP = 2.0F;
   private static final float PILL_GAP = 3.0F;
   private static final float ARMOR_PILL_WIDTH = 49.5F;
   private static final float SLOT_PILL_WIDTH = 15.0F;
   private static final float SLOT_PAD = 7.5F;
   private static final float SLOT_SPACING = 11.5F;
   private static final float ITEM_SIZE = 8.0F;
   private static final float GHOST_SIZE = 7.0F;
   private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
   private static final String[] ARMOR_GHOSTS = {"helmet", "chestplate", "leggings", "boots"};

   private static final Animation animator = new Animation();
   private static final Animation animator2 = new Animation();
   private static final Animation animator3 = new Animation();
   private static final Animation animator4 = new Animation();
   private static final Animation animator5 = new Animation();
   private static final Map<EntityType<?>, Identifier> entityTypeTextures = new HashMap<>();
   private static final Map<String, Identifier> ghostTextures = new HashMap<>();
   private static final Map<Identifier, float[]> textureUvCache = new HashMap<>();
   private static final ArrayList<TargetHud.DataRecord> particles = new ArrayList<>();
   private static LivingEntity currentTarget;
   private static LivingEntity previousTarget;
   private static LivingEntity outgoingTarget;
   private static long lastSeenTimeMillis;
   private static int lastHurtTime;

   private static int readBigEndianInt(byte[] byteArray, int byteOffset) {
      return (byteArray[byteOffset] & 0xFF) << 24
              | (byteArray[byteOffset + 1] & 0xFF) << 16
              | (byteArray[byteOffset + 2] & 0xFF) << 8
              | byteArray[byteOffset + 3] & 0xFF;
   }

   private static boolean isPngSignature(byte[] byteArray) {
      return byteArray.length == 24
              && (byteArray[0] & 255) == 137
              && byteArray[1] == 80
              && byteArray[2] == 78
              && byteArray[3] == 71
              && byteArray[12] == 73
              && byteArray[13] == 72
              && byteArray[14] == 68
              && byteArray[15] == 82;
   }

   private static float snapToHalf(float value) {
      return Math.round(value * 2.0F) / 2.0F;
   }

   public static void processData(MinecraftClient minecraftClient, float screenWidth, float screenHeight, float deltaSeconds) {
      long nowMillis = System.currentTimeMillis();
      LivingEntity livingEntity = getLivingEntity(minecraftClient);
      if (livingEntity != null) {
         currentTarget = livingEntity;
         lastSeenTimeMillis = nowMillis;
      }

      boolean hidden = currentTarget == null || !currentTarget.isAlive() || currentTarget.isRemoved() || nowMillis - lastSeenTimeMillis > 800L;
      animator.update();
      animator.animateTo(hidden ? 0.0 : 1.0, 0.18, Easings.EASE_OUT_QUART);
      float visibility = animator.floatValue();
      if (!(visibility <= 0.01F) && currentTarget != null) {
         if (currentTarget != previousTarget) {
            outgoingTarget = previousTarget;
            previousTarget = currentTarget;
            animator5.resetTo(0.0);
            animator5.animateTo(1.0, 0.32, Easings.EASE_OUT_EXPO);
         }

         animator5.update();
         float health = YTMODEModule.getInstance().isEnabled() && currentTarget.isInvisible()
                 ? 1000.0F
                 : (currentTarget instanceof PlayerEntity ? EntityUtils.getTotalHealth(currentTarget) : currentTarget.getHealth());
         float maxHealth = Math.max(1.0F, Math.max(currentTarget.getMaxHealth(), health));
         float clampedHealth = MathHelper.clamp(health, 0.0F, maxHealth);
         float absorption = MathHelper.clamp(currentTarget.getAbsorptionAmount(), 0.0F, maxHealth);
         boolean hasAbsorption = absorption > 0.01F;
         float targetValue = clampedHealth / maxHealth;
         float targetValue2 = hasAbsorption ? absorption / maxHealth : 0.0F;
         animator2.update();
         animator2.animateTo(targetValue, 0.5, Easings.EASE_OUT_EXPO);
         if (hasAbsorption) {
            animator3.update();
            animator3.animateTo(targetValue2, 0.5, Easings.EASE_OUT_EXPO);
         } else {
            animator3.resetTo(0.0);
         }

         float totalWidth = PANEL_WIDTH + 8.0F + HEAD_SIZE + PANEL_PAD * 2.0F;
         float[] floatArray = HudManager.computeFloat5("target", screenWidth / 2.0F - totalWidth / 2.0F, screenHeight / 2.0F + 24.0F);
         float startX = floatArray[0];
         float panelY = floatArray[1];
         float slideOffset = 1.0F - (float)Easings.EASE_OUT_EXPO.apply(visibility);

         float headBoxWidth = HEAD_SIZE + PANEL_PAD * 2.0F;
         float headBoxX = startX - slideOffset * 16.0F;
         HudStyle.processData4(headBoxX, panelY, headBoxWidth, PANEL_HEIGHT, visibility, PANEL_RADIUS);

         float mainPanelX = headBoxX + headBoxWidth + 6.0F;
         HudManager.processData9("target", mainPanelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);
         HudRenderContext.processData4(mainPanelX + PANEL_WIDTH / 2.0F, panelY + PANEL_HEIGHT / 2.0F, visibility);
         HudStyle.processData4(mainPanelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, visibility, PANEL_RADIUS);

         float switchProgress = animator5.floatValue();
         float iconX = headBoxX + PANEL_PAD;
         float iconY = panelY + PANEL_PAD;
         if (outgoingTarget != null && switchProgress < 0.999F) {
            renderTargetAvatar(minecraftClient, outgoingTarget, iconX - 5.0F * switchProgress, iconY, HEAD_SIZE, visibility * (1.0F - switchProgress));
         }

         renderTargetAvatar(
                 minecraftClient,
                 currentTarget,
                 iconX + 5.0F * (1.0F - switchProgress),
                 iconY,
                 HEAD_SIZE,
                 visibility * (outgoingTarget != null ? switchProgress : 1.0F)
         );
         if (InterfaceModule.getInstance().headParticlesOnHit.isChecked() && currentTarget.hurtTime > lastHurtTime) {
            spawnHitParticles(iconX + HEAD_SIZE / 2.0F, iconY + HEAD_SIZE / 2.0F, nowMillis);
         }

         lastHurtTime = currentTarget.hurtTime;
         animator4.update();
         animator4.animateTo(hasVisibleEquipment(currentTarget) ? 1.0 : 0.0, 0.35, Easings.EASE_OUT_QUAD);
         float textX = mainPanelX + TEXT_X - 25.0F;
         float rightX = mainPanelX + PANEL_WIDTH - PANEL_PAD;
         float textY = panelY + NAME_Y - slideOffset * 6.0F;
         float barY = panelY + BAR_Y + slideOffset * 6.0F;
         String healthText = formatHealthText(clampedHealth);
         float valueWidth = FontText.computeFloat(FontFace.SEMI_BOLD, healthText, FONT_SIZE);
         float suffixWidth = FontText.computeFloat(FontFace.SEMI_BOLD, "hp", FONT_SIZE);
         float healthX = rightX - valueWidth - suffixWidth;
         renderTargetName(currentTarget, textX, textY, healthX - 4.0F - textX, visibility);
         float equipmentVisibility = animator4.floatValue();
         if (equipmentVisibility > 0.01F) {
            renderEquipmentRow(
                    minecraftClient,
                    currentTarget,
                    mainPanelX,
                    panelY - ROW_GAP - ROW_HEIGHT + (1.0F - equipmentVisibility) * 4.0F - slideOffset * 6.0F,
                    visibility * equipmentVisibility
            );
         }

         renderHealthBar(textX, barY, rightX - textX, visibility);
         FontText.processData3(
                 FontFace.SEMI_BOLD,
                 healthText,
                 healthX,
                 textY,
                 FONT_SIZE,
                 UiTheme.computeInt(HudStyle.computeInt4(1.0F), visibility)
         );
         FontText.processData3(
                 FontFace.SEMI_BOLD,
                 "hp",
                 healthX + valueWidth,
                 textY,
                 FONT_SIZE,
                 UiTheme.computeInt(ThemeColors.intVal9, visibility)
         );
         renderHitParticles(nowMillis, visibility);
         HudRenderContext.updateState();
      } else {
         if (hidden && visibility <= 0.01F) {
            currentTarget = null;
         }

         previousTarget = null;
         outgoingTarget = null;
         animator5.resetTo(1.0);
      }
   }

   private static void renderHealthBar(float x, float y, float width, float alpha) {
      CornerRadii cornerRadii = CornerRadii.uniform(BAR_HEIGHT / 2.0F);
      int trackColor = Colors.computeInt6(-1, alpha * 0.1F);
      ShapeDrawing.processData6(x, y, width, BAR_HEIGHT, trackColor, trackColor, trackColor, trackColor, 1.0F, cornerRadii);
      int healthColor = Colors.computeInt6(ThemeColors.intVal9, alpha);
      float healthWidth = width * MathHelper.clamp(animator2.floatValue(), 0.0F, 1.0F);
      if (healthWidth > 0.5F) {
         ShapeDrawing.processData6(x, y, healthWidth, BAR_HEIGHT, healthColor, healthColor, healthColor, healthColor, 1.0F, cornerRadii);
      }

      if (!ServerHelperModule.getInstance().modeSetting.isSelected("FunTime")) {
         float absorptionAlpha = alpha * 0.9F;
         int absorptionColorLeft = Colors.computeInt6(-7717102, absorptionAlpha);
         int absorptionColorRight = Colors.computeInt6(-1013958, absorptionAlpha);
         float absorptionWidth = Math.min(width, width * MathHelper.clamp(animator3.floatValue(), 0.0F, 1.0F));
         if (absorptionWidth > 0.5F) {
            ShapeDrawing.processData6(x, y, absorptionWidth, BAR_HEIGHT, absorptionColorLeft, absorptionColorLeft, absorptionColorRight, absorptionColorRight, 1.0F, cornerRadii);
         }
      }
   }

   private static void renderEquipmentRow(MinecraftClient minecraftClient, LivingEntity livingEntity, float x, float y, float alpha) {
      float snappedX = snapToHalf(x);
      float snappedY = snapToHalf(y);
      float centerY = snappedY + ROW_HEIGHT / 2.0F;
      float pillRadius = ROW_HEIGHT / 2.0F;
      HudStyle.processData4(snappedX, snappedY, ARMOR_PILL_WIDTH, ROW_HEIGHT, alpha, pillRadius);

      for (int index = 0; index < ARMOR_SLOTS.length; index++) {
         renderSlotIcon(
                 minecraftClient,
                 livingEntity.getEquippedStack(ARMOR_SLOTS[index]),
                 ARMOR_GHOSTS[index],
                 snappedX + SLOT_PAD + index * SLOT_SPACING,
                 centerY,
                 alpha
         );
      }

      float mainHandX = snappedX + ARMOR_PILL_WIDTH + PILL_GAP;
      HudStyle.processData4(mainHandX, snappedY, SLOT_PILL_WIDTH, ROW_HEIGHT, alpha, pillRadius);
      renderSlotIcon(minecraftClient, livingEntity.getMainHandStack(), "sword", mainHandX + SLOT_PAD, centerY, alpha);
      float offHandX = mainHandX + SLOT_PILL_WIDTH + PILL_GAP;
      HudStyle.processData4(offHandX, snappedY, SLOT_PILL_WIDTH, ROW_HEIGHT, alpha, pillRadius);
      renderSlotIcon(minecraftClient, livingEntity.getOffHandStack(), "shield", offHandX + SLOT_PAD, centerY, alpha);
   }

   private static void renderSlotIcon(MinecraftClient minecraftClient, ItemStack itemStack, String ghostName, float centerX, float centerY, float alpha) {
      if (!itemStack.isEmpty()) {
         ItemRenderCache.processData(
                 itemStack,
                 snapToHalf(centerX - ITEM_SIZE / 2.0F),
                 snapToHalf(centerY - ITEM_SIZE / 2.0F),
                 ITEM_SIZE,
                 ItemDrawOptions.decorated(alpha).glint(GlintMode.OFF)
         );
         return;
      }

      Identifier ghost = getGhostTexture(minecraftClient, ghostName);
      if (ghost != null) {
         Render2D.drawImageUvNearest(
                 ghost.toString(),
                 snapToHalf(centerX - GHOST_SIZE / 2.0F),
                 snapToHalf(centerY - GHOST_SIZE / 2.0F),
                 GHOST_SIZE,
                 GHOST_SIZE,
                 0.0F,
                 0.25F,
                 0.0F,
                 0.0F,
                 1.0F,
                 1.0F,
                 UiTheme.computeInt(-1, alpha * 0.3F)
         );
      }
   }

   private static Identifier getGhostTexture(MinecraftClient minecraftClient, String name) {
      if (ghostTextures.containsKey(name)) {
         return ghostTextures.get(name);
      }

      Identifier identifier = Identifier.ofVanilla("textures/gui/sprites/container/slot/" + name + ".png");
      if (minecraftClient.getResourceManager().getResource(identifier).isEmpty()) {
         identifier = null;
      }

      ghostTextures.put(name, identifier);
      return identifier;
   }

   private static Identifier resolveEntityTexture(MinecraftClient minecraftClient, LivingEntity livingEntity) {
      try {
         EntityRenderManager entityRenderDispatcher = minecraftClient.getEntityRenderDispatcher();
         if (entityRenderDispatcher.getRenderer(livingEntity) instanceof LivingEntityRenderer livingEntityRenderer
                 && entityRenderDispatcher.getAndUpdateRenderState(livingEntity, 1.0F) instanceof LivingEntityRenderState livingEntityRenderState) {
            Identifier texture = livingEntityRenderer.getTexture(livingEntityRenderState);
            return texture != null && minecraftClient.getResourceManager().getResource(texture).isPresent() ? texture : null;
         }
      } catch (Throwable throwable) {
      }

      return null;
   }

   private static Identifier getEntityTexture(MinecraftClient minecraftClient, LivingEntity livingEntity) {
      if (livingEntity instanceof AbstractClientPlayerEntity abstractClientPlayerEntity) {
         return abstractClientPlayerEntity.getSkin().body().texturePath();
      } else {
         EntityType<?> type = livingEntity.getType();
         if (entityTypeTextures.containsKey(type)) {
            return entityTypeTextures.get(type);
         }

         ResourceManager resourceManager = minecraftClient.getResourceManager();
         String path = EntityType.getId(type).getPath();
         Identifier identifier = Identifier.ofVanilla("textures/entity/" + path + ".png");
         if (resourceManager.getResource(identifier).isEmpty()) {
            identifier = Identifier.ofVanilla("textures/entity/" + path + "/" + path + ".png");
         }

         if (resourceManager.getResource(identifier).isEmpty()) {
            identifier = resolveEntityTexture(minecraftClient, livingEntity);
         }

         entityTypeTextures.put(type, identifier);
         return identifier;
      }
   }

   private static String formatHealthText(float value) {
      int wholePart = (int)value;
      int tenthPart = Math.round((value - wholePart) * 10.0F);
      if (tenthPart >= 10) {
         return Integer.toString(wholePart + 1);
      } else {
         return tenthPart <= 0 ? Integer.toString(wholePart) : wholePart + "." + tenthPart;
      }
   }

   private static void renderTargetAvatar(MinecraftClient minecraftClient, LivingEntity livingEntity, float x, float y, float size, float alpha) {
      if (!(alpha <= 0.004F)) {
         float snappedX = snapToHalf(x);
         float snappedY = snapToHalf(y);
         CornerRadii cornerRadii = CornerRadii.uniform(HEAD_RADIUS);
         Identifier identifier = getEntityTexture(minecraftClient, livingEntity);
         if (identifier == null) {
            float iconInset = size * 0.25F;
            ShapeDrawing.processData9(
                    snappedX, snappedY, size, size, Colors.computeInt2(-15790575, -1, 0.15F), alpha * 0.6F, cornerRadii
            );
            HudIcons.USER
                    .processData(
                            snappedX + iconInset,
                            snappedY + iconInset,
                            size * 0.5F,
                            UiTheme.computeInt(HudStyle.computeInt2(1.0F), alpha)
                    );
         } else {
            float[] floatArray = getTextureUv(minecraftClient, identifier);
            String textureId = identifier.toString();
            int tint = UiTheme.computeInt(-1, alpha);
            Render2D.drawImageUvNearest(
                    textureId,
                    snappedX,
                    snappedY,
                    size,
                    size,
                    HEAD_RADIUS,
                    0.25F,
                    floatArray[0],
                    floatArray[1],
                    floatArray[2],
                    floatArray[3],
                    tint
            );
            if (floatArray[4] > 0.5F) {
               Render2D.drawImageUvNearest(
                       textureId,
                       snappedX,
                       snappedY,
                       size,
                       size,
                       HEAD_RADIUS,
                       0.25F,
                       floatArray[5],
                       floatArray[6],
                       floatArray[7],
                       floatArray[8],
                       tint
               );
            }

            float hurtTimeFraction = livingEntity.maxHurtTime > 0 ? (float)livingEntity.hurtTime / livingEntity.maxHurtTime : 0.0F;
            if (hurtTimeFraction > 0.0F) {
               ShapeDrawing.processData9(snappedX, snappedY, size, size, -65536, alpha * hurtTimeFraction * 0.55F, cornerRadii);
            }
         }
      }
   }

   private static void renderTargetName(LivingEntity livingEntity, float x, float y, float width, float alpha) {
      String string = livingEntity.getName().getString();
      float clipMaxX = x + Math.max(0.0F, width);
      FontText.processData7(
              FontFace.SEMI_BOLD,
              string,
              x,
              y,
              FONT_SIZE,
              UiTheme.computeInt(HudStyle.computeInt4(1.0F), alpha),
              x,
              clipMaxX,
              4.0F,
              false,
              true
      );
   }

   private static float[] getTextureUv(MinecraftClient minecraftClient, Identifier identifier) {
      float[] floatArray = textureUvCache.get(identifier);
      if (floatArray != null) {
         return floatArray;
      }

      int textureWidth = 64;
      int textureHeight = 64;
      Optional<Resource> resource = minecraftClient.getResourceManager().getResource(identifier);
      if (resource.isPresent()) {
         boolean headerParsed = false;

         try (InputStream inputStream = resource.get().getInputStream()) {
            byte[] byteArray = inputStream.readNBytes(24);
            if (isPngSignature(byteArray)) {
               textureWidth = Math.max(1, readBigEndianInt(byteArray, 16));
               textureHeight = Math.max(1, readBigEndianInt(byteArray, 20));
               headerParsed = true;
            }
         } catch (Exception exception) {
         }

         if (!headerParsed) {
            try (
                    InputStream inputStream2 = resource.get().getInputStream();
                    NativeImage nativeImage = NativeImage.read(inputStream2);
            ) {
               textureWidth = Math.max(1, nativeImage.getWidth());
               textureHeight = Math.max(1, nativeImage.getHeight());
            } catch (Exception exception2) {
            }
         }
      }

      float isDefaultAtlas = textureWidth == 64 && textureHeight == 64 ? 1.0F : 0.0F;
      float[] floatArray2 = new float[]{
              8.0F / textureWidth,
              8.0F / textureHeight,
              16.0F / textureWidth,
              16.0F / textureHeight,
              isDefaultAtlas,
              40.0F / textureWidth,
              8.0F / textureHeight,
              48.0F / textureWidth,
              16.0F / textureHeight
      };
      textureUvCache.put(identifier, floatArray2);
      return floatArray2;
   }

   private static void spawnHitParticles(float x, float y, long nowMillis) {
      particles.replaceAll(
              dataRecord -> dataRecord.fadeFromMs() > 0L
                      ? dataRecord
                      : new TargetHud.DataRecord(
                      dataRecord.startX(), dataRecord.startY(), dataRecord.offsetX(), dataRecord.offsetY(), dataRecord.size(), dataRecord.bornMs(), nowMillis
              )
      );
      ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
      int particleCount = 10 + threadLocalRandom.nextInt(4);

      for (int index = 0; index < particleCount; index++) {
         double angle = (Math.PI * 2) * (index + threadLocalRandom.nextDouble(0.7)) / particleCount;
         float distance = 20.0F + threadLocalRandom.nextFloat() * 14.0F;
         float particleSize = 5.0F + threadLocalRandom.nextFloat() * 4.0F;
         particles.add(
                 new TargetHud.DataRecord(
                         x, y, (float)Math.cos(angle) * distance, (float)Math.sin(angle) * distance, particleSize, nowMillis, 0L
                 )
         );
      }
   }

   private TargetHud() {
   }

   private static void renderHitParticles(long nowMillis, float alpha) {
      particles.removeIf(
              dataRecord -> nowMillis - dataRecord.bornMs() >= 1000L || dataRecord.fadeFromMs() > 0L && nowMillis - dataRecord.fadeFromMs() >= 200L
      );
      if (!particles.isEmpty()) {
         if (HudRenderContext.getDrawContext() != null) {
            HudRenderContext.getDrawContext().createNewRootLayer();
         }

         for (TargetHud.DataRecord particle : particles) {
            float progress = (float)(nowMillis - particle.bornMs()) / 1000.0F;
            float remaining = 1.0F - progress;
            float easedProgress = 1.0F - remaining * remaining * remaining;
            float particleAlpha = Math.min(1.0F, progress / 0.15F) * remaining * alpha;
            float particleX = particle.startX() + particle.offsetX() * easedProgress;
            float particleY = particle.startY() + particle.offsetY() * easedProgress;
            if (particle.fadeFromMs() > 0L) {
               particleAlpha *= Math.max(0.0F, 1.0F - (float)(nowMillis - particle.fadeFromMs()) / 200.0F);
            }

            float particleSize = particle.size() * (1.0F - progress * 0.4F);
            float halfSize = particleSize / 2.0F;
            Render2D.drawImageRotatedGlow(
                    "assets/greed/textures/particles/glow.png",
                    particleX - halfSize,
                    particleY - halfSize,
                    particleSize,
                    0.0F,
                    0.0F,
                    particleX,
                    particleY,
                    UiTheme.computeInt(ThemeColors.intVal9, particleAlpha)
            );
         }
      }
   }

   private static boolean hasVisibleEquipment(LivingEntity livingEntity) {
      if (livingEntity.getMainHandStack().isEmpty() && livingEntity.getOffHandStack().isEmpty()) {
         for (EquipmentSlot equipmentSlot : ARMOR_SLOTS) {
            if (!livingEntity.getEquippedStack(equipmentSlot).isEmpty()) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private static LivingEntity getLivingEntity(MinecraftClient minecraftClient) {
      if (minecraftClient.player == null) {
         return null;
      } else if (HudRenderContext.checkState2()) {
         return minecraftClient.player;
      } else {
         AttackAuraModule attackAuraModule = AttackAuraModule.getInstance();
         if (attackAuraModule.isEnabled() && attackAuraModule.getTarget() instanceof LivingEntity livingEntity && livingEntity.isAlive()) {
            return livingEntity;
         } else {
            return minecraftClient.targetedEntity instanceof LivingEntity livingEntity2 && livingEntity2 != minecraftClient.player && livingEntity2.isAlive() ? livingEntity2 : null;
         }
      }
   }

   public static float computeFloat3() {
      return animator.floatValue();
   }

   private record DataRecord(float startX, float startY, float offsetX, float offsetY, float size, long bornMs, long fadeFromMs) {
   }
}