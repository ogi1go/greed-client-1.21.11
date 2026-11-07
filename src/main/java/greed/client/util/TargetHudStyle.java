package greed.client.util;

import greed.client.render.FontText;
import greed.client.ui.hud.HudIcons;
import greed.client.ui.hud.HudManager;
import greed.client.ui.hud.HudRenderContext;
import greed.client.ui.hud.HudStyle;
import greed.client.render.ItemRenderCache;
import greed.client.ui.UiTheme;

import greed.client.util.Animation;

import greed.client.module.combat.AttackAuraModule;
import greed.mixin.BossBarHudMixin;
import greed.client.util.Easings;
import greed.client.render.FontFace;
import greed.client.storage.FriendStore;
import greed.client.util.ItemDrawOptions;
import greed.client.MinecraftAccess;
import greed.client.render.Render2D;
import greed.client.RotationAngles;
import greed.client.module.player.ServerHelperModule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class TargetHudStyle implements MinecraftAccess {
   private static final EquipmentSlot[] equipmentSlotArray = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
   private static final HashMap<String, Item> map = new HashMap<>();
   private static final HashMap<String, Item> map2 = new HashMap<>();
   private static final HashMap<Item, Integer> map3 = new HashMap<>();
   private static final HashMap<Item, Long> map4 = new HashMap<>();
   private static final HashMap<UUID, TargetHudStyle.SubContext2> map5 = new HashMap<>();
   private static final HashSet<Integer> set = new HashSet<>();
   private static final Animation animator = new Animation();
   private static final Animation animator2 = new Animation();
   private static final Animation animator3 = new Animation();
   private static final Animation animator4 = new Animation();
   private static final Animation animator5 = new Animation();
   private static final Animation animator6 = new Animation();
   private static final Animation animator7 = new Animation();
   private static UUID uUID2;
   private static long timeMillis;
   private static boolean flag;
   private static long timeMillis2;
   private static ClientWorld clientWorld;
   private static TargetHudStyle.SubContext2 nested0509;

   private static void processData(MinecraftClient minecraftClient) {
      if (minecraftClient.player != null && minecraftClient.world != null) {
         if (minecraftClient.world != clientWorld) {
            clientWorld = minecraftClient.world;
            uUID2 = null;
            flag = false;
            map5.clear();
         }

         long local6320_1 = System.currentTimeMillis();
         if (uUID2 != null && minecraftClient.inGameHud != null) {
            if (!((BossBarHudMixin)minecraftClient.inGameHud.getBossBarHud()).getEvents().isEmpty()) {
               flag = true;
               timeMillis2 = 0L;
            } else if (flag && timeMillis2 == 0L) {
               timeMillis2 = local6320_1;
            } else if (flag && local6320_1 - timeMillis2 > 2000L) {
               uUID2 = null;
               flag = false;
               timeMillis2 = 0L;
               map5.clear();
            }
         }

         PlayerEntity playerEntity = AttackAuraModule.getInstance().isEnabled()
               && AttackAuraModule.getInstance().getTarget() instanceof PlayerEntity playerEntity2
            ? playerEntity2
            : null;
         if (playerEntity != null && !FriendStore.isFriend(playerEntity.getGameProfile().name())) {
            timeMillis = local6320_1;
            if (!playerEntity.getUuid().equals(uUID2)) {
               uUID2 = playerEntity.getUuid();
               flag = false;
               timeMillis2 = 0L;
            }
         }

         boolean local6320_4 = uUID2 != null && uUID2.equals(minecraftClient.player.getUuid());
         if (playerEntity != null || !HudRenderContext.checkState() || uUID2 != null && !local6320_4) {
            if (uUID2 != null
               && (!local6320_4 || HudRenderContext.checkState())
               && (playerEntity != null || local6320_1 - timeMillis <= 3000L || flag || HudRenderContext.checkState())) {
               PlayerEntity playerEntity3 = null;

               for (PlayerEntity playerEntity4 : minecraftClient.world.getPlayers()) {
                  if (playerEntity4.getUuid().equals(uUID2)) {
                     playerEntity3 = playerEntity4;
                  }
               }

               if (playerEntity3 != null && EntityUtils.getTotalHealth(playerEntity3) <= 0.0F) {
                  map5.clear();
                  uUID2 = null;
               } else if (playerEntity3 != null || map5.containsKey(uUID2)) {
                  if (playerEntity3 != null) {
                     map5.computeIfAbsent(uUID2, TargetHudStyle.SubContext2::new).processData10(minecraftClient, playerEntity3, local6320_1);
                  }

                  map5.keySet().removeIf(uUID -> !uUID.equals(uUID2));
                  processData3(minecraftClient, local6320_1);
               }
            } else {
               map5.clear();
               uUID2 = null;
            }
         } else {
            uUID2 = minecraftClient.player.getUuid();
            map5.computeIfAbsent(uUID2, TargetHudStyle.SubContext2::new).processData10(minecraftClient, minecraftClient.player, local6320_1);
            map5.get(uUID2).strVal = "Example Player";
         }
      }
   }

   private static boolean matchesCriteria(ItemStack itemStack) {
      return matchesCriteria2(itemStack) && computeInt(itemStack) <= 0
         || itemStack.isOf(Items.ENCHANTED_GOLDEN_APPLE)
         || itemStack.isOf(Items.GOLDEN_APPLE)
         || itemStack.isOf(Items.GOLDEN_CARROT);
   }

   public static void processData2(Item item) {
      map4.put(item, System.currentTimeMillis());
   }

   private static void processData3(MinecraftClient minecraftClient, long local6323_1) {
      TargetHudStyle.SubContext2 local6323_3 = uUID2 == null ? null : map5.get(uUID2);
      if (local6323_3 != null) {
         for (Entity entity : minecraftClient.world.getEntities()) {
            if (entity instanceof ProjectileEntity projectileEntity && !set.contains(entity.getId()) && projectileEntity.getOwner() != null) {
               set.add(entity.getId());
               if (projectileEntity.getOwner().getUuid().equals(uUID2)) {
                  if (entity instanceof ThrownItemEntity thrownItemEntity && !thrownItemEntity.getStack().isEmpty()) {
                     local6323_3.processData12(thrownItemEntity.getStack().getItem(), local6323_1);
                  } else if (entity.getType() == EntityType.WIND_CHARGE) {
                     local6323_3.processData12(Items.WIND_CHARGE, local6323_1);
                  } else if (entity.getType() == EntityType.SMALL_FIREBALL) {
                     local6323_3.processData12(Items.FIRE_CHARGE, local6323_1);
                  }
               }
            }
         }

         if (set.size() > 256) {
            set.clear();
         }
      }
   }

   static {
      map3.put(Items.DRIED_KELP, 10);
      map3.put(Items.NETHERITE_SCRAP, 10);
      map3.put(Items.SNOWBALL, 20);
      map3.put(Items.ENDER_EYE, 60);
      map3.put(Items.FIRE_CHARGE, 60);
      map3.put(Items.SUGAR, 60);
      map3.put(Items.ENCHANTED_GOLDEN_APPLE, 150);
      map3.put(Items.GOLDEN_APPLE, 30);
      map3.put(Items.GOLDEN_CARROT, 30);
      map2.put("minecraft:entity.illusioner.mirror_move", Items.ENDER_EYE);
      map.put("minecraft:entity.blaze.shoot", Items.FIRE_CHARGE);
      map.put("minecraft:entity.wind_charge.throw", Items.WIND_CHARGE);
   }

   public static void processData4(boolean local6325_0, boolean local6325_1) {
      TargetHudStyle.SubContext2 local6325_2 = uUID2 == null ? null : map5.get(uUID2);
      if (local6325_2 != null) {
         Item item = local6325_0 ? Items.NETHERITE_SCRAP : Items.DRIED_KELP;
         if (System.currentTimeMillis() - map4.getOrDefault(item, 0L) >= 2500L) {
            local6325_2.processData12(item, System.currentTimeMillis());
            TargetHudStyle.SubContext local6325_4 = local6325_2.map6.get(item);
            if (local6325_4 != null) {
               local6325_4.intVal2 = local6325_1 ? 15 : 10;
            }
         }
      }
   }

   public static void processData5(PlayerEntity playerEntity) {
      uUID2 = playerEntity.getUuid();
   }

   private static float computeFloat(MinecraftClient minecraftClient, TargetHudStyle.SubContext2 local6327_1) {
      float local6327_2 = 0.0F;
      if (minecraftClient.player != null && local6327_1.vec3d != null) {
         double local6327_3 = local6327_1.vec3d.x - minecraftClient.player.getX();
         double local6327_5 = local6327_1.vec3d.z - minecraftClient.player.getZ();
         if (local6327_3 * local6327_3 + local6327_5 * local6327_5 >= 0.01) {
            local6327_2 = (float)Math.toDegrees(Math.atan2(local6327_5, local6327_3)) - 90.0F - RotationAngles.getCameraYaw();
         }
      }

      animator7.update();
      animator7.animateTo(animator7.floatValue() + MathHelper.wrapDegrees(local6327_2 - animator7.floatValue()), 0.25, Easings.EASE_OUT_QUART);
      return animator7.floatValue();
   }

   private static void processData6(TargetHudStyle.SubContext2 local6328_0, float local6328_1, float local6328_2, float local6328_3) {
      if (!(local6328_3 <= 0.001F) && !local6328_0.map6.isEmpty()) {
         float local6328_4 = HudStyle.computeFloat2(local6328_3);
         ArrayList<TargetHudStyle.SubContext> local6328_5 = new ArrayList<>(local6328_0.map6.values());

         for (int index = 0; index < local6328_5.size(); index++) {
            TargetHudStyle.SubContext local6328_7 = local6328_5.get(local6328_5.size() - 1 - index);
            float local6328_8 = local6328_1 + index * 15.0F - local6328_4;
            HudStyle.processData5(local6328_8, local6328_2, 13.0F, 13.0F, local6328_3, 3.0F);
            ItemRenderCache.processData(
               local6328_7.itemStack2,
               Math.round((local6328_8 + 2.1F) * 2.0F) / 2.0F,
               Math.round((local6328_2 + 2.1F) * 2.0F) / 2.0F,
               8.8F,
               ItemDrawOptions.noDecorations(local6328_3).glint(GlintMode.OFF)
            );
            if (local6328_7.itemStack2.getCount() > 1) {
               FontText.processData3(
                  FontFace.SEMI_BOLD,
                  String.valueOf(local6328_7.itemStack2.getCount()),
                  local6328_8
                     + 13.0F
                     - 1.5F
                     - FontText.computeFloat(FontFace.SEMI_BOLD, String.valueOf(local6328_7.itemStack2.getCount()), 5.0F),
                  local6328_2 + 7.0F,
                  5.0F,
                  UiTheme.computeInt(HudStyle.computeInt4(1.0F), local6328_3)
               );
            }

            int local6328_9 = local6328_7.computeInt4(System.currentTimeMillis());
            if (local6328_9 > 0) {
               String local6328_10 = local6328_9 > 99 ? "99" : String.valueOf(local6328_9);
               float local6328_11 = FontText.computeFloat(FontFace.SEMI_BOLD, local6328_10, 6.5F);
               FontText.processData3(
                  FontFace.SEMI_BOLD,
                  local6328_10,
                  local6328_8 + (13.0F - local6328_11) / 2.0F,
                  local6328_2 + 11.0F,
                  6.5F,
                  UiTheme.computeInt(-50384, local6328_3 * 0.9F)
               );
            }
         }
      }
   }

   public static void processData7(PlayerEntity playerEntity, double local6329_1, double local6329_3, double local6329_5, String local6329_7) {
      if (uUID2 != null && map5.get(uUID2) != null) {
         PlayerEntity playerEntity2 = null;

         for (PlayerEntity playerEntity3 : playerEntity.getEntityWorld().getPlayers()) {
            if (playerEntity3.getUuid().equals(uUID2)) {
               playerEntity2 = playerEntity3;
            }
         }

         if (playerEntity2 != null) {
            Item item = map2.get(local6329_7);
            if (item == null) {
               Item item2 = map.get(local6329_7);
               if (item2 != null) {
                  if (System.currentTimeMillis() - map4.getOrDefault(item2, 0L) >= 2500L) {
                     if (!(playerEntity2.squaredDistanceTo(local6329_1, local6329_3, local6329_5) > 36.0)
                        && !(
                           playerEntity2.squaredDistanceTo(local6329_1, local6329_3, local6329_5)
                              > playerEntity.squaredDistanceTo(local6329_1, local6329_3, local6329_5)
                        )) {
                        map5.get(uUID2).processData12(item2, System.currentTimeMillis());
                     }
                  }
               }
            } else if (System.currentTimeMillis() - map4.getOrDefault(item, 0L) >= 2500L) {
               if (!(playerEntity.squaredDistanceTo(local6329_1, local6329_3, local6329_5) > 36.0)
                  && !(
                     playerEntity.squaredDistanceTo(local6329_1, local6329_3, local6329_5)
                        > playerEntity2.squaredDistanceTo(local6329_1, local6329_3, local6329_5)
                  )) {
                  map5.get(uUID2).processData12(item, System.currentTimeMillis());
               }
            }
         }
      }
   }

   private static int computeInt(ItemStack itemStack) {
      for (ServerHelperModule.SubContext local6330_2 : ServerHelperModule.getInstance().getArrayList()) {
         if (local6330_2.getKeybindSetting().isVisible() && local6330_2.matchesCriteria2(itemStack)) {
            return 3;
         }
      }

      return 0;
   }

   public static void processData8(MinecraftClient minecraftClient, float local6331_1, float local6331_2) {
      processData(minecraftClient);
      TargetHudStyle.SubContext2 local6331_3 = uUID2 == null ? null : map5.get(uUID2);
      if (local6331_3 != null) {
         nested0509 = local6331_3;
      }

      animator2.update();
      animator2.animateTo(local6331_3 != null ? 1.0 : 0.0, 0.6, Easings.EASE_OUT_QUART);
      animator6.update();
      animator6.animateTo(local6331_3 != null && !local6331_3.map6.isEmpty() ? 1.0 : 0.0, 0.6, Easings.EASE_OUT_QUART);
      animator.update();
      animator.animateTo(
              local6331_3 == null && !(animator2.floatValue() > 0.001F) && !(animator6.floatValue() > 0.001F) ? 0.0 : 1.0, 0.4, Easings.EASE_OUT_QUART
      );
      if (nested0509 != null) {
         TargetHudStyle.SubContext2 local6331_4 = nested0509;
         float local6331_5 = computeFloat3(local6331_4);
         float targetValue = computeFloat2(local6331_4);
         float local6331_7 = Math.max(local6331_5 + 2.0F + targetValue, local6331_4.map6.size() * 15.0F - 2.0F);
         float headerWidth = 16.0F + FontText.computeFloat(FontFace.SEMI, "Target", 6.0F) + 4.0F;
         float targetValue2 = Math.max(Math.max(headerWidth, local6331_7), 1.0F);
         animator3.update();
         animator3.animateTo(targetValue2, 0.6, Easings.EASE_OUT_QUART);
         animator4.update();
         animator4.animateTo(Math.max(headerWidth, 1.0F), 0.6, Easings.EASE_OUT_QUART);
         animator5.update();
         animator5.animateTo(targetValue, 0.6, Easings.EASE_OUT_QUART);
         float local6331_9 = animator3.floatValue();
         float local6331_10 = Math.max(13.0F + 14.0F * animator2.floatValue() + 17.0F * animator6.floatValue(), 1.0F);
         float local6331_11 = animator.floatValue();
         if (!(local6331_11 <= 0.001F)) {
            float[] floatArray = HudManager.computeFloat5("Target", local6331_1, local6331_2);
            HudManager.processData9("Target", floatArray[0], floatArray[1], local6331_9, local6331_10);
            HudRenderContext.processData4(floatArray[0] + local6331_9 / 2.0F, floatArray[1] + local6331_10 / 2.0F, local6331_11);
            HudStyle.processData7(
                    floatArray[0], floatArray[1], headerWidth, local6331_11, HudIcons.ENVIRONMENT, "Target"
            );
            processData9(minecraftClient, local6331_4, floatArray[0], floatArray[1] + 15.0F, local6331_5, targetValue, animator2.floatValue() * local6331_11);
            processData6(local6331_4, floatArray[0], floatArray[1] + 15.0F + 13.0F * animator2.floatValue() - 1.0F, animator6.floatValue() * local6331_11);
            HudRenderContext.updateState();
         }
      }
   }

   private static int computeInt2(TargetHudStyle.SubContext2 local6333_0) {
      int local6333_1 = 0;

      for (int index = 0; index < 4; index++) {
         if (!local6333_0.itemStackArray[index].isEmpty()) {
            local6333_1++;
         }
      }

      return local6333_1;
   }

   private static int computeInt3(ItemStack itemStack) {
      return matchesCriteria2(itemStack) ? 15 : map3.getOrDefault(itemStack.getItem(), 0);
   }

   private static boolean matchesCriteria2(ItemStack itemStack) {
      for (StatusEffectInstance statusEffectInstance : itemStack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT).getEffects()) {
         if (statusEffectInstance.getEffectType().getIdAsString().equals("minecraft:instant_health")) {
            return true;
         }
      }

      return false;
   }

   private static float computeFloat2(TargetHudStyle.SubContext2 local6336_0) {
      return HudStyle.computeFloat4(FontText.computeFloat(FontFace.SEMI, (int)local6336_0.floatVal + "HP", 6.0F));
   }

   private static void processData9(
      MinecraftClient minecraftClient,
      TargetHudStyle.SubContext2 local6337_1,
      float local6337_2,
      float local6337_3,
      float local6337_4,
      float local6337_5,
      float local6337_6
   ) {
      if (!(local6337_6 <= 0.001F)) {
         float local6337_7 = HudStyle.computeFloat2(local6337_6);
         float local6337_8 = local6337_2 - local6337_7;
         float local6337_9 = local6337_2 + animator5.floatValue() + 2.0F - local6337_7;
         local6337_4 = Math.max(local6337_4, animator4.floatValue() - animator5.floatValue() - 2.0F);
         float local6337_10 = HudStyle.computeFloat3(local6337_3);
         float local6337_11 = Math.round((local6337_3 + 1.5F) * 2.0F) / 2.0F;
         HudStyle.processData6(local6337_9, local6337_3, local6337_4, 11.0F, local6337_6);
         if (local6337_1.identifier != null) {
            Render2D.drawImageUvNearest(
               local6337_1.identifier.toString(),
               Math.round((local6337_9 + 2.0F) * 2.0F) / 2.0F,
               local6337_11,
               8.0F,
               8.0F,
               3.0F,
               0.25F,
               0.125F,
               0.125F,
               0.25F,
               0.25F,
               UiTheme.computeInt(-1, local6337_6)
            );
         }

         FontText.processData3(
            FontFace.SEMI,
            local6337_1.strVal,
            local6337_9 + 12.5F,
            local6337_10,
            6.0F,
            UiTheme.computeInt(HudStyle.computeInt4(1.0F), local6337_6)
         );
         float local6337_12 = local6337_9 + 12.5F + FontText.computeFloat(FontFace.SEMI, local6337_1.strVal, 6.0F) + 3.0F;

         for (int index = 0; index < 4; index++) {
            if (!local6337_1.itemStackArray[index].isEmpty()) {
               ItemRenderCache.processData(
                  local6337_1.itemStackArray[index],
                  Math.round((local6337_12 - 0.5F) * 2.0F) / 2.0F,
                  local6337_11,
                  8.0F,
                  ItemDrawOptions.noDecorations(local6337_6).glint(GlintMode.OFF)
               );
               local6337_12 += 7.0F;
            }
         }

         HudStyle.processData6(local6337_8, local6337_3, local6337_5, 11.0F, local6337_6);
         FontText.processData3(
            FontFace.SEMI,
            (int)local6337_1.floatVal + "HP",
            local6337_8 + 4.0F,
            local6337_10,
            6.0F,
            UiTheme.computeInt(HudStyle.computeInt4(1.0F), local6337_6)
         );
      }
   }

   private TargetHudStyle() {
   }

   private static float computeFloat3(TargetHudStyle.SubContext2 local6339_0) {
      int local6339_1 = computeInt2(local6339_0);
      return 12.5F
         + FontText.computeFloat(FontFace.SEMI, local6339_0.strVal, 6.0F)
         + (local6339_1 > 0 ? 3.0F + local6339_1 * 7.0F - 1.0F : 0.0F)
         + 4.0F;
   }

   private static final class SubContext {
      final ItemStack itemStack2;
      long timeMillis3;
      long timeMillis4;
      int intVal;
      int intVal2;

      int computeInt4(long local6340_1) {
         int local6340_3 = this.intVal2 > 0 ? this.intVal2 : TargetHudStyle.computeInt3(this.itemStack2);
         return local6340_3 != 0 && this.timeMillis3 != 0L ? Math.max((int)Math.ceil((local6340_3 * 1000L - (local6340_1 - this.timeMillis3)) / 1000.0), 0) : 0;
      }

      SubContext(ItemStack itemStack, int local6341_2) {
         this.itemStack2 = itemStack.copy();
         this.itemStack2.setDamage(0);
         this.intVal = local6341_2;
      }
   }

   private static final class SubContext2 {
      final LinkedHashMap<Item, TargetHudStyle.SubContext> map6 = new LinkedHashMap<>();
      final ItemStack[] itemStackArray = new ItemStack[6];
      String strVal = "";
      Identifier identifier;
      float floatVal;
      Vec3d vec3d;
      boolean flag2;
      int intVal3;
      ItemStack itemStack3 = ItemStack.EMPTY;

      void processData10(MinecraftClient minecraftClient, PlayerEntity playerEntity, long local6342_3) {
         this.strVal = playerEntity.getName().getString();
         if (playerEntity instanceof AbstractClientPlayerEntity abstractClientPlayerEntity) {
            this.identifier = abstractClientPlayerEntity.getSkin().body().texturePath();
         }

         this.floatVal = EntityUtils.getTotalHealth(playerEntity);
         this.vec3d = playerEntity.getEntityPos();

         for (int index = 0; index < 4; index++) {
            this.itemStackArray[index] = playerEntity.getEquippedStack(TargetHudStyle.equipmentSlotArray[index]);
         }

         for (TargetHudStyle.SubContext local6342_6 : this.map6.values()) {
            if (local6342_6.timeMillis4 == 0L
               && local6342_6.itemStack2.getCount() == 1
               && this.itemStackArray[local6342_6.intVal].isOf(local6342_6.itemStack2.getItem())
               && (local6342_6.intVal == 4 ? playerEntity.getMainHandStack() : playerEntity.getOffHandStack()).isEmpty()) {
               local6342_6.timeMillis4 = local6342_3;
            }
         }

         this.itemStackArray[4] = playerEntity.getMainHandStack();
         this.itemStackArray[5] = playerEntity.getOffHandStack();
         if (playerEntity.isUsingItem() && !playerEntity.getActiveItem().isEmpty()) {
            if (playerEntity.getItemUseTimeLeft() <= 1 && !this.flag2) {
               this.flag2 = true;
               this.processData13(playerEntity.getActiveItem().copyWithCount(1), local6342_3);
            }
         } else {
            this.flag2 = false;
         }

         for (int index2 = 4; index2 < 6; index2++) {
            if (TargetHudStyle.computeInt(this.itemStackArray[index2]) > 0 || TargetHudStyle.matchesCriteria(this.itemStackArray[index2])) {
               this.processData11(minecraftClient, playerEntity, this.itemStackArray[index2], index2, local6342_3);
            }
         }

         this.map6
            .values()
            .removeIf(local6348_2 -> local6348_2.timeMillis4 != 0L && local6342_3 - local6348_2.timeMillis4 > 10000L && local6348_2.computeInt4(local6342_3) <= 0);
      }

      SubContext2(UUID uUID) {
         Arrays.fill(this.itemStackArray, ItemStack.EMPTY);
      }

      private void processData11(MinecraftClient minecraftClient, PlayerEntity playerEntity, ItemStack itemStack, int local6344_4, long local6344_5) {
         TargetHudStyle.SubContext local6344_7 = this.map6.remove(itemStack.getItem());
         if (local6344_7 != null) {
            if (itemStack.getCount() < local6344_7.itemStack2.getCount()
               && TargetHudStyle.computeInt3(itemStack) > 0
               && minecraftClient.world
                  .getEntitiesByClass(
                     ItemEntity.class, playerEntity.getBoundingBox().expand(2.0), itemEntity -> itemEntity.getStack().isOf(itemStack.getItem())
                  )
                  .isEmpty()) {
               local6344_7.timeMillis3 = local6344_5;
            }

            local6344_7.itemStack2.setCount(itemStack.getCount());
            local6344_7.intVal = local6344_4;
            local6344_7.timeMillis4 = 0L;
            this.map6.put(itemStack.getItem(), local6344_7);
         } else {
            if (this.map6.size() >= 9) {
               for (Item item : new ArrayList<>(this.map6.keySet())) {
                  if (TargetHudStyle.computeInt(this.map6.get(item).itemStack2) < TargetHudStyle.computeInt(itemStack)) {
                     this.map6.remove(item);
                     break;
                  }
               }

               if (this.map6.size() >= 9) {
                  return;
               }
            }

            this.map6.put(itemStack.getItem(), new TargetHudStyle.SubContext(itemStack, local6344_4));
         }
      }

      void processData12(Item item, long local6345_2) {
         this.processData13(new ItemStack(item), local6345_2);
      }

      void processData13(ItemStack itemStack, long local6347_2) {
         if (TargetHudStyle.matchesCriteria2(itemStack) && TargetHudStyle.computeInt(itemStack) <= 0
            || itemStack.isOf(Items.ENCHANTED_GOLDEN_APPLE)
            || itemStack.isOf(Items.GOLDEN_APPLE)
            || itemStack.isOf(Items.GOLDEN_CARROT)
            || TargetHudStyle.computeInt(itemStack) > 0
               && !itemStack.isOf(Items.POTION)
               && !itemStack.isOf(Items.SPLASH_POTION)
               && !itemStack.isOf(Items.LINGERING_POTION)) {
            TargetHudStyle.SubContext local6347_4 = this.map6.remove(itemStack.getItem());
            if (local6347_4 == null) {
               local6347_4 = new TargetHudStyle.SubContext(itemStack, 4);
            } else if (TargetHudStyle.matchesCriteria(local6347_4.itemStack2) && local6347_4.itemStack2.getCount() > 1) {
               local6347_4.itemStack2.setCount(local6347_4.itemStack2.getCount() - 1);
            }

            local6347_4.timeMillis3 = local6347_2;
            local6347_4.timeMillis4 = local6347_2;
            if (this.map6.size() >= 9) {
               this.map6.remove(this.map6.keySet().iterator().next());
            }

            this.map6.put(itemStack.getItem(), local6347_4);
         }
      }
   }
}
