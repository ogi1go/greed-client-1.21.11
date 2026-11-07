package greed.client.module.player;

import greed.client.module.combat.AttackAuraModule;
import greed.client.setting.BooleanSetting;
import greed.client.util.ClientChat;
import greed.client.util.Colors;
import greed.client.event.EventListener;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.PacketEvent;
import greed.client.event.PlayerTickEvent;
import greed.client.ui.hud.NotificationHud;
import greed.client.ui.UiTheme;
import greed.client.setting.SectionSetting;
import greed.client.event.WorldSoundEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

@ModuleInfo(
   name = "UseTracker",
   description = "Уведомляет о сломанных тотемах, использованных предметах, кулдаунах и специальных зельях.",
   category = ModuleCategory.PLAYER
)
public final class UseTrackerModule extends Module {
   private static UseTrackerModule useTrackerModule;
   public BooleanSetting brokenTotems;
   public BooleanSetting usedItems;
   public BooleanSetting itemOutOfCooldown;
   public BooleanSetting specialPotions;
   public BooleanSetting fadingEffects;
   public BooleanSetting arrowCounter;
   public BooleanSetting showInChat;
   public BooleanSetting showInNotification;
   private final HashSet<Integer> set = new HashSet<>();
   private final HashMap<String, ItemStack> map = new HashMap<>();
   private final HashMap<String, Long> map2 = new HashMap<>();
   private final HashMap<RegistryEntry<StatusEffect>, StatusEffectInstance> map3 = new HashMap<>();
   private final String strVal = "${rgba(65,67,69,0)}[${rgba(68,63,64,0)}★${rgba(72,59,60,0)}]${rgba(76,55,56,0)} ${rgba(79,51,52,0)}З${rgba(83,47,48,0)}е${rgba(87,43,44,0)}л${rgba(90,39,40,0)}ь${rgba(94,35,36,0)}е${rgba(98,31,32,0)} ${rgba(102,27,28,0)}А${rgba(105,23,24,0)}с${rgba(109,19,20,0)}с${rgba(113,15,16,0)}а${rgba(116,11,12,0)}с${rgba(120,7,8,0)}и${rgba(124,3,4,0)}н${rgba(128,0,0,0)}а";
   private final LinkedHashMap<String, int[]> map4 = new LinkedHashMap<>();
   private final LinkedHashMap<String, List<Entry<RegistryEntry<StatusEffect>, int[]>>> map5 = new LinkedHashMap<>();
   private final HashSet<Integer> set2 = new HashSet<>();
   private UUID uUID;
   private LivingEntity livingEntity2;
   private World world2;
   private int intVal;
   private ItemStack itemStack2 = Items.ARROW.getDefaultStack();
   private final String[] strValArray = new String[]{"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (mc.player != null && mc.world != null) {
         this.updateState();
         if (this.usedItems.isChecked()) {
            for (AbstractClientPlayerEntity abstractClientPlayerEntity : mc.world.getPlayers()) {
               if (!abstractClientPlayerEntity.getUuid().equals(mc.player.getUuid())) {
                  if (abstractClientPlayerEntity.isUsingItem() && !abstractClientPlayerEntity.getActiveItem().isEmpty()) {
                     if (abstractClientPlayerEntity.getItemUseTimeLeft() <= 1 && this.set.add(abstractClientPlayerEntity.getId())) {
                        this.processData(
                           "Предмет",
                           abstractClientPlayerEntity.getDisplayName()
                              .copy()
                              .append(
                                 Text.literal(
                                       (
                                             Formatting.strip(abstractClientPlayerEntity.getDisplayName().getString()).stripTrailing().length()
                                                   == Formatting.strip(abstractClientPlayerEntity.getDisplayName().getString()).length()
                                                ? " "
                                                : ""
                                          )
                                          + "использовал "
                                    )
                                    .setStyle(Style.EMPTY.withColor(Formatting.GRAY))
                              )
                              .append(this.getMutableText(abstractClientPlayerEntity.getActiveItem().copyWithCount(1)))
                        );
                     }
                  } else {
                     this.set.remove(abstractClientPlayerEntity.getId());
                  }
               }
            }
         }

         if (this.itemOutOfCooldown.isChecked()) {
            HashMap<String, ItemStack> local2775_2 = new HashMap<>();

            for (int index = 0; index < 36; index++) {
               if (!mc.player.getInventory().getStack(index).isEmpty()) {
                  local2775_2.putIfAbsent(
                     Formatting.strip(mc.player.getInventory().getStack(index).getName().getString()),
                     mc.player.getInventory().getStack(index)
                  );
               }

               if (mc.player.getInventory().getStack(index).get(DataComponentTypes.BUNDLE_CONTENTS) != null) {
                  for (ItemStack itemStack : mc.player.getInventory().getStack(index).get(DataComponentTypes.BUNDLE_CONTENTS).iterate()) {
                     if (!itemStack.isEmpty()) {
                        local2775_2.putIfAbsent(Formatting.strip(itemStack.getName().getString()), itemStack);
                     }
                  }
               }
            }

            for (Entry<String, ItemStack> local2775_4 : this.map.entrySet()) {
               if (local2775_2.containsKey(local2775_4.getKey())
                  && !mc.player.getItemCooldownManager().isCoolingDown(local2775_4.getValue())
                  && System.currentTimeMillis() - this.map2.getOrDefault(local2775_4.getKey(), System.currentTimeMillis()) > 7000L) {
                  this.processData(
                     "Кулдаун",
                     this.getMutableText(local2775_2.get(local2775_4.getKey()).copyWithCount(1))
                        .append(Text.literal(" вышел из кд").setStyle(Style.EMPTY.withColor(Formatting.GREEN)))
                  );
               }
            }

            this.map.clear();

            for (Entry<String, ItemStack> local2775_4 : local2775_2.entrySet()) {
               if (mc.player.getItemCooldownManager().isCoolingDown(local2775_4.getValue())) {
                  this.map.put(local2775_4.getKey(), local2775_4.getValue());
               }
            }

            this.map2.keySet().retainAll(this.map.keySet());

            for (String local2775_4 : this.map.keySet()) {
               this.map2.putIfAbsent(local2775_4, System.currentTimeMillis());
            }
         }

         if (this.fadingEffects.isChecked()) {
            for (Entry<RegistryEntry<StatusEffect>, StatusEffectInstance> local2775_3 : this.map3.entrySet()) {
               if (mc.player.getStatusEffect(local2775_3.getKey()) == null) {
                  this.processData(
                     "Эффект",
                     Text.literal(local2775_3.getValue().getEffectType().value().getName().getString())
                        .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(local2775_3.getValue().getEffectType().value().getColor() & 16777215)))
                        .append(Text.literal(" пропал").setStyle(Style.EMPTY.withColor(Formatting.RED)))
                  );
               }
            }

            this.map3.clear();

            for (StatusEffectInstance statusEffectInstance : mc.player.getStatusEffects()) {
               if (statusEffectInstance.getEffectType() == StatusEffects.STRENGTH
                  || statusEffectInstance.getEffectType() == StatusEffects.SPEED
                  || statusEffectInstance.getEffectType() == StatusEffects.RESISTANCE
                  || statusEffectInstance.getEffectType() == StatusEffects.WEAKNESS) {
                  this.map3.put(statusEffectInstance.getEffectType(), statusEffectInstance);
               }
            }
         }
      }
   };
   private final EventListener<PacketEvent> packetListener = packetEvent -> {
      if (packetEvent.isIncoming()
         && this.brokenTotems.isChecked()
         && packetEvent.getPacket() instanceof EntityStatusS2CPacket entityStatusS2CPacket
         && entityStatusS2CPacket.getStatus() == 35) {
         mc.execute(
            () -> {
               if (mc.world != null && entityStatusS2CPacket.getEntity(mc.world) instanceof PlayerEntity playerEntity) {
                  this.processData(
                     "Тотем",
                     playerEntity.getDisplayName()
                        .copy()
                        .append(
                           Text.literal(
                                 (
                                       Formatting.strip(playerEntity.getDisplayName().getString()).stripTrailing().length()
                                             == Formatting.strip(playerEntity.getDisplayName().getString()).length()
                                          ? " "
                                          : ""
                                    )
                                    + "лишился "
                              )
                              .setStyle(Style.EMPTY.withColor(Formatting.RED))
                        )
                        .append(
                           this.getMutableText(
                              (playerEntity.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING
                                    ? playerEntity.getOffHandStack()
                                    : (
                                       playerEntity.getMainHandStack().getItem() == Items.TOTEM_OF_UNDYING
                                          ? playerEntity.getMainHandStack()
                                          : Items.TOTEM_OF_UNDYING.getDefaultStack()
                                    ))
                                 .copyWithCount(1)
                           )
                        )
                  );
               }
            }
         );
      }
   };
   private final EventListener<WorldSoundEvent> worldSoundListener = worldSoundEvent -> {
      String local2776_2 = this.formatText(worldSoundEvent.computeInt());
      if (this.specialPotions.isChecked() && mc.world != null && local2776_2 != null) {
         for (AbstractClientPlayerEntity abstractClientPlayerEntity2 : mc.world.getPlayers()) {
            if (abstractClientPlayerEntity2.isAlive() && !(abstractClientPlayerEntity2.squaredDistanceTo(worldSoundEvent.getBlockPos().toCenterPos()) > 64.0)) {
               double local2776_5 = 1.0
                  - Math.sqrt(
                        Math.pow(
                              worldSoundEvent.getBlockPos().toCenterPos().x
                                 - MathHelper.clamp(
                                    worldSoundEvent.getBlockPos().toCenterPos().x, abstractClientPlayerEntity2.getBoundingBox().minX, abstractClientPlayerEntity2.getBoundingBox().maxX
                                 ),
                              2.0
                           )
                           + Math.pow(
                              worldSoundEvent.getBlockPos().toCenterPos().y
                                 - MathHelper.clamp(
                                    worldSoundEvent.getBlockPos().toCenterPos().y, abstractClientPlayerEntity2.getBoundingBox().minY, abstractClientPlayerEntity2.getBoundingBox().maxY
                                 ),
                              2.0
                           )
                           + Math.pow(
                              worldSoundEvent.getBlockPos().toCenterPos().z
                                 - MathHelper.clamp(
                                    worldSoundEvent.getBlockPos().toCenterPos().z, abstractClientPlayerEntity2.getBoundingBox().minZ, abstractClientPlayerEntity2.getBoundingBox().maxZ
                                 ),
                              2.0
                           )
                     )
                     / 4.0;
               if (!(local2776_5 <= 0.0)) {
                  int local2776_7 = MathHelper.clamp((int)Math.round(local2776_5 * 100.0), 0, 100);
                  MutableText mutableText = abstractClientPlayerEntity2.getDisplayName()
                     .copy()
                     .append(
                        Text.literal(
                              (
                                    Formatting.strip(abstractClientPlayerEntity2.getDisplayName().getString()).stripTrailing().length()
                                          == Formatting.strip(abstractClientPlayerEntity2.getDisplayName().getString()).length()
                                       ? " "
                                       : ""
                                 )
                                 + "получил "
                           )
                           .setStyle(Style.EMPTY.withColor(Formatting.GRAY))
                     )
                     .append(this.getMutableText2(local2776_2))
                     .append(
                        Text.literal(" (" + local2776_7 + "%)")
                           .setStyle(
                              Style.EMPTY
                                 .withColor(
                                    TextColor.fromRgb(
                                       (
                                             local2776_7 <= 50
                                                ? Colors.computeInt2(16729156, 16768324, local2776_7 / 50.0F)
                                                : Colors.computeInt2(16768324, 4521813, (local2776_7 - 50) / 50.0F)
                                          )
                                          & 16777215
                                    )
                                 )
                           )
                     );
                  MutableText mutableText2 = Text.empty().append(mutableText);

                  for (Entry<RegistryEntry<StatusEffect>, int[]> local2776_11 : this.map5
                     .getOrDefault(this.getMutableText2(local2776_2).getString(), List.of())) {
                     int local2776_12 = ((int[])local2776_11.getValue())[0] == 0
                        ? 0
                        : Math.max(0, MathHelper.floor(local2776_11.getValue()[0] * local2776_5 + 0.5));
                     if (local2776_11.getValue()[0] <= 0 || local2776_12 > 20) {
                        mutableText2.append(Text.literal("\n  • ").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY)))
                           .append(
                              Text.literal(
                                    local2776_11.getKey().value().getName().getString()
                                       + (((int[])local2776_11.getValue())[1] > 0 ? " " + this.strValArray[Math.min(local2776_11.getValue()[1], 9)] : "")
                                 )
                                 .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(local2776_11.getKey().value().getColor() & 16777215)))
                           )
                           .append(
                              Text.literal(
                                    ((int[])local2776_11.getValue())[0] == 0
                                       ? " — сразу"
                                       : " — " + local2776_12 / 1200 + ":" + String.format("%02d", local2776_12 / 20 % 60)
                                 )
                                 .setStyle(Style.EMPTY.withColor(Formatting.GRAY))
                           );
                     }
                  }

                  this.processData2("Зелье", mutableText, mutableText2);
               }
            }
         }
      }
   };

   private boolean matchesCriteria(PlayerEntity playerEntity) {
      return playerEntity.getMainHandStack().getItem() instanceof BowItem
         || playerEntity.getMainHandStack().getItem() instanceof CrossbowItem
         || playerEntity.getOffHandStack().getItem() instanceof BowItem
         || playerEntity.getOffHandStack().getItem() instanceof CrossbowItem;
   }

   private MutableText getMutableText(ItemStack itemStack) {
      MutableText mutableText = Text.empty();
      mutableText.append(
         itemStack.getName()
            .copy()
            .styled(style -> style.getColor() == null ? style.withColor(itemStack.getRarity().getFormatting()) : style)
      );
      if (itemStack.getCount() > 1) {
         mutableText.append(Text.literal(" [").setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
         mutableText.append(Text.literal("x" + itemStack.getCount()).setStyle(Style.EMPTY.withColor(Formatting.WHITE)));
         mutableText.append(Text.literal("]").setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
      }

      return mutableText;
   }

   public static UseTrackerModule getInstance() {
      if (useTrackerModule == null) {
         useTrackerModule = new UseTrackerModule();
      }

      return useTrackerModule;
   }

   private MutableText getMutableText2(String local2769_1) {
      MutableText mutableText = Text.empty();
      Matcher matcher = Pattern.compile("\\$\\{rgba\\((\\d+),(\\d+),(\\d+),(\\d+)\\)\\}([^$]*)").matcher(local2769_1);

      while (matcher.find()) {
         mutableText.append(
            Text.literal(matcher.group(5))
               .setStyle(
                  Style.EMPTY
                     .withColor(
                        TextColor.fromRgb(
                           Integer.parseInt(matcher.group(1)) << 16 | Integer.parseInt(matcher.group(2)) << 8 | Integer.parseInt(matcher.group(3))
                        )
                     )
               )
         );
      }

      return mutableText;
   }

   public int computeInt() {
      return this.intVal;
   }

   private String formatText(int local2772_1) {
      int local2772_2 = local2772_1 & 16777215;

      for (Entry<String, int[]> local2772_4 : this.map4.entrySet()) {
         for (int local2772_8 : local2772_4.getValue()) {
            if ((local2772_8 & 16777215) == local2772_2) {
               return local2772_4.getKey();
            }
         }
      }

      int local2772_3 = local2772_2 >> 16 & 0xFF;
      int local2772_4 = local2772_2 >> 8 & 0xFF;
      int local2772_5 = local2772_2 & 0xFF;
      return Math.abs(local2772_3 - local2772_4) <= 10
            && Math.abs(local2772_4 - local2772_5) <= 10
            && Math.abs(local2772_3 - local2772_5) <= 10
            && local2772_3 >= 14
            && local2772_3 <= 88
         ? "${rgba(65,67,69,0)}[${rgba(68,63,64,0)}★${rgba(72,59,60,0)}]${rgba(76,55,56,0)} ${rgba(79,51,52,0)}З${rgba(83,47,48,0)}е${rgba(87,43,44,0)}л${rgba(90,39,40,0)}ь${rgba(94,35,36,0)}е${rgba(98,31,32,0)} ${rgba(102,27,28,0)}А${rgba(105,23,24,0)}с${rgba(109,19,20,0)}с${rgba(113,15,16,0)}а${rgba(116,11,12,0)}с${rgba(120,7,8,0)}и${rgba(124,3,4,0)}н${rgba(128,0,0,0)}а"
         : null;
   }

   private UseTrackerModule() {
      this.map4
         .put(
            "${rgba(255,20,147,0)}[${rgba(231,41,152,0)}★${rgba(208,62,157,0)}]${rgba(185,84,162,0)} ${rgba(162,105,167,0)}Х${rgba(139,126,172,0)}л${rgba(115,148,178,0)}о${rgba(92,169,183,0)}п${rgba(69,190,188,0)}у${rgba(46,212,193,0)}ш${rgba(23,233,198,0)}к${rgba(0,255,204,0)}а",
            new int[]{16738740}
         );
      this.map4
         .put(
            "${rgba(255,247,58,0)}[${rgba(248,247,58,0)}★${rgba(242,247,58,0)}]${rgba(235,248,58,0)} ${rgba(229,248,58,0)}З${rgba(223,249,59,0)}е${rgba(216,249,59,0)}л${rgba(210,250,59,0)}ь${rgba(204,250,59,0)}е${rgba(197,251,60,0)} ${rgba(191,251,60,0)}Р${rgba(185,252,60,0)}а${rgba(178,252,60,0)}д${rgba(172,253,61,0)}и${rgba(166,253,61,0)}а${rgba(159,254,61,0)}ц${rgba(153,254,61,0)}и${rgba(147,255,62,0)}и",
            new int[]{3329330}
         );
      this.map4
         .put(
            "${rgba(255,255,203,0)}[${rgba(255,254,192,0)}★${rgba(255,254,181,0)}]${rgba(255,254,170,0)} ${rgba(255,254,159,0)}С${rgba(255,254,148,0)}в${rgba(255,254,137,0)}я${rgba(255,254,126,0)}т${rgba(255,254,115,0)}а${rgba(255,254,104,0)}я${rgba(255,254,93,0)} ${rgba(255,254,82,0)}в${rgba(255,254,71,0)}о${rgba(255,254,60,0)}д${rgba(255,254,50,0)}а",
            new int[]{16777215}
         );
      this.map4
         .put(
            "${rgba(139,0,0,0)}[${rgba(147,10,0,0)}★${rgba(155,20,0,0)}]${rgba(163,30,0,0)} ${rgba(172,40,0,0)}З${rgba(180,50,0,0)}е${rgba(188,60,0,0)}л${rgba(197,70,0,0)}ь${rgba(205,80,0,0)}е${rgba(213,89,0,0)} ${rgba(221,100,0,0)}Г${rgba(230,109,0,0)}н${rgba(238,120,0,0)}е${rgba(246,129,0,0)}в${rgba(255,140,0,0)}а",
            new int[]{10040115}
         );
      this.map4
         .put(
            "${rgba(209,255,91,0)}[${rgba(209,255,91,0)}★${rgba(209,255,91,0)}]${rgba(209,255,91,0)} ${rgba(209,255,91,0)}З${rgba(209,255,91,0)}е${rgba(209,255,91,0)}л${rgba(209,255,91,0)}ь${rgba(209,255,91,0)}е${rgba(209,255,91,0)} ${rgba(209,255,91,0)}П${rgba(209,255,91,0)}а${rgba(209,255,91,0)}л${rgba(209,255,91,0)}л${rgba(209,255,91,0)}а${rgba(209,255,91,0)}д${rgba(209,255,91,0)}и${rgba(209,255,91,0)}н${rgba(209,255,91,0)}а",
            new int[]{65535}
         );
      this.map4
         .put(
            "${rgba(65,67,69,0)}[${rgba(68,63,64,0)}★${rgba(72,59,60,0)}]${rgba(76,55,56,0)} ${rgba(79,51,52,0)}З${rgba(83,47,48,0)}е${rgba(87,43,44,0)}л${rgba(90,39,40,0)}ь${rgba(94,35,36,0)}е${rgba(98,31,32,0)} ${rgba(102,27,28,0)}А${rgba(105,23,24,0)}с${rgba(109,19,20,0)}с${rgba(113,15,16,0)}а${rgba(116,11,12,0)}с${rgba(120,7,8,0)}и${rgba(124,3,4,0)}н${rgba(128,0,0,0)}а",
            new int[]{3355443}
         );
      this.map4
         .put(
            "${rgba(63,13,154,0)}[${rgba(66,23,160,0)}★${rgba(69,33,166,0)}]${rgba(72,43,172,0)} ${rgba(75,53,178,0)}С${rgba(79,64,184,0)}н${rgba(82,74,190,0)}о${rgba(85,84,196,0)}т${rgba(88,94,202,0)}в${rgba(92,105,208,0)}о${rgba(95,115,214,0)}р${rgba(98,125,220,0)}н${rgba(101,135,226,0)}о${rgba(105,146,233,0)}е",
            new int[]{254}
         );
      this.map5
         .put(
            "[★] Хлопушка",
            List.of(
               Map.entry(StatusEffects.SLOWNESS, new int[]{200, 9}),
               Map.entry(StatusEffects.SPEED, new int[]{300, 4}),
               Map.entry(StatusEffects.BLINDNESS, new int[]{100, 9}),
               Map.entry(StatusEffects.GLOWING, new int[]{3600, 0})
            )
         );
      this.map5
         .put(
            "[★] Зелье Радиации",
            List.of(
               Map.entry(StatusEffects.POISON, new int[]{1200, 1}),
               Map.entry(StatusEffects.WITHER, new int[]{1200, 1}),
               Map.entry(StatusEffects.SLOWNESS, new int[]{1800, 2}),
               Map.entry(StatusEffects.HUNGER, new int[]{1200, 4}),
               Map.entry(StatusEffects.GLOWING, new int[]{2400, 0})
            )
         );
      this.map5
         .put(
            "[★] Святая вода",
            List.of(
               Map.entry(StatusEffects.REGENERATION, new int[]{900, 1}),
               Map.entry(StatusEffects.INVISIBILITY, new int[]{12000, 1}),
               Map.entry(StatusEffects.INSTANT_HEALTH, new int[]{0, 2})
            )
         );
      this.map5
         .put("[★] Зелье Гнева", List.of(Map.entry(StatusEffects.STRENGTH, new int[]{600, 4}), Map.entry(StatusEffects.SLOWNESS, new int[]{600, 3})));
      this.map5
         .put(
            "[★] Зелье Палладина",
            List.of(
               Map.entry(StatusEffects.RESISTANCE, new int[]{12000, 0}),
               Map.entry(StatusEffects.FIRE_RESISTANCE, new int[]{12000, 0}),
               Map.entry(StatusEffects.HEALTH_BOOST, new int[]{1200, 2}),
               Map.entry(StatusEffects.INVISIBILITY, new int[]{18000, 2})
            )
         );
      this.map5
         .put(
            "[★] Зелье Ассасина",
            List.of(
               Map.entry(StatusEffects.STRENGTH, new int[]{1200, 3}),
               Map.entry(StatusEffects.SPEED, new int[]{6000, 2}),
               Map.entry(StatusEffects.HASTE, new int[]{1200, 0}),
               Map.entry(StatusEffects.INSTANT_DAMAGE, new int[]{0, 1})
            )
         );
      this.map5
         .put(
            "[★] Снотворное",
            List.of(
               Map.entry(StatusEffects.WEAKNESS, new int[]{1800, 1}),
               Map.entry(StatusEffects.MINING_FATIGUE, new int[]{200, 1}),
               Map.entry(StatusEffects.WITHER, new int[]{1800, 2}),
               Map.entry(StatusEffects.BLINDNESS, new int[]{200, 0})
            )
         );
      this.brokenTotems = this.addSetting(new BooleanSetting("Сломанные тотемы", "Пишет, когда игрок лишается тотема.", true));
      this.usedItems = this.addSetting(new BooleanSetting("Использованные предметы", "Пишет, когда игрок использует предмет.", true));
      this.itemOutOfCooldown = this.addSetting(
         new BooleanSetting("Предмет вышел из кд", "Пишет, когда твой предмет из инвентаря, хотбара или мешка выходит из кулдауна длиннее 7 секунд.", true)
      );
      this.specialPotions = this.addSetting(new BooleanSetting("Специальные зелья", "Пишет, кто и на сколько процентов получил специальное зелье.", true));
      this.fadingEffects = this.addSetting(
         new BooleanSetting("Пропадающие эффекты", "Пишет, когда у тебя пропадает сила, скорость, сопротивление или слабость.", true)
      );
      this.arrowCounter = this.addSetting(new BooleanSetting("Счётчик стрел", "Показывает на экране, сколько стрел выпустила цель ауры.", true));
      this.addSetting(new SectionSetting("Уведомления"));
      this.showInChat = this.addSetting(new BooleanSetting("Показывать в чате", "Дублирует уведомления в чат.", true));
      this.showInNotification = this.addSetting(new BooleanSetting("Показывать в уведомлении", "Показывает всплывающее уведомление.", true));
   }

   private void processData(String local2774_1, MutableText mutableText) {
      this.processData2(local2774_1, mutableText, mutableText);
   }

   private void processData2(String local2778_1, MutableText text, MutableText mutableText) {
      if (this.showInNotification.isChecked()) {
         NotificationHud.notifyWithText(this.category.getIcon(), local2778_1, text, true);
      }

      if (this.showInChat.isChecked()) {
         ClientChat.processData4(
            ClientChat.getMutableText("UseTracker", UiTheme.intVal7, UiTheme.intVal9, true)
               .append(Text.literal(Formatting.DARK_GRAY + " » " + Formatting.RESET))
               .append(mutableText)
         );
      }
   }

   private void updateState() {
      LivingEntity livingEntity = AttackAuraModule.getInstance().isEnabled() ? AttackAuraModule.getInstance().getTarget() : null;
      if (mc.world != this.world2 || livingEntity != null && livingEntity != this.livingEntity2) {
         this.world2 = mc.world;
         if (livingEntity != null) {
            this.livingEntity2 = livingEntity;
         }

         this.uUID = livingEntity instanceof PlayerEntity playerEntity ? playerEntity.getUuid() : null;
         this.intVal = 0;
         this.itemStack2 = Items.ARROW.getDefaultStack();
         this.set2.clear();
      }

      if (this.uUID != null) {
         PlayerEntity playerByUuid = mc.world.getPlayerByUuid(this.uUID);
         if (playerByUuid != null && !playerByUuid.isAlive()) {
            this.uUID = null;
            this.intVal = 0;
         } else {
            for (Entity entity : mc.world.getEntities()) {
               if (entity instanceof PersistentProjectileEntity persistentProjectileEntity
                  && !(entity instanceof TridentEntity)
                  && !this.set2.contains(entity.getId())) {
                  if (persistentProjectileEntity.getOwner() == null) {
                     if (persistentProjectileEntity.age > 5) {
                        this.set2.add(entity.getId());
                     }
                  } else {
                     this.set2.add(entity.getId());
                     if (persistentProjectileEntity.getOwner() instanceof PlayerEntity playerEntity2
                        && playerEntity2.getUuid().equals(this.uUID)
                        && this.matchesCriteria(playerEntity2)) {
                        this.intVal++;
                        ItemStack itemStack = persistentProjectileEntity.getItemStack();
                        this.itemStack2 = itemStack.isEmpty() ? Items.ARROW.getDefaultStack() : itemStack.copyWithCount(1);
                     }
                  }
               }
            }

            if (this.set2.size() > 512) {
               this.set2.clear();
            }
         }
      }
   }

   public ItemStack getItemStack2() {
      return this.itemStack2;
   }
}
