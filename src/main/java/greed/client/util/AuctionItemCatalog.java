package greed.client.util;

import greed.client.storage.AutoBuyItem;

import greed.client.MinecraftAccess;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;

public final class AuctionItemCatalog implements MinecraftAccess {
   private static final ArrayList<AutoBuyItem> list = new ArrayList<>();
   private static boolean flag;
   public static final BiPredicate<ItemStack, AutoBuyItem> biPredicate = (itemStack, autoBuyItem) -> {
      if (itemStack.getItem() != Items.PLAYER_HEAD || autoBuyItem.item() != Items.PLAYER_HEAD) {
         return false;
      } else if (autoBuyItem.attributes().isEmpty()) {
         return false;
      } else {
         return !AutoBuyItem.CHECKER.test(itemStack, autoBuyItem) ? false : matchesCriteria(itemStack, autoBuyItem.displayStack());
      }
   };

   public static boolean matchesCriteria(ItemStack itemStack, ItemStack itemStack2) {
      ProfileComponent profileComponent = itemStack.get(DataComponentTypes.PROFILE);
      ProfileComponent profileComponent2 = itemStack2.get(DataComponentTypes.PROFILE);
      if (profileComponent != null && profileComponent2 != null) {
         String local1436_4 = formatText(profileComponent);
         String local1436_5 = formatText(profileComponent2);
         return local1436_4 != null && local1436_4.equals(local1436_5);
      } else {
         return false;
      }
   }

   public static ArrayList<AuctionItemCatalog.DataRecord> getArrayList() {
      ArrayList<AuctionItemCatalog.DataRecord> local1437_0 = new ArrayList<>();
      processData(local1437_0, "Броня");
      processData(local1437_0, "Оружие");
      processData(local1437_0, "Сферы");
      processData(local1437_0, "Талисманы");
      processData(local1437_0, "Зелья");
      processData(local1437_0, "Прочее");
      return local1437_0;
   }

   private static AutoBuyItem.SubContext getNested0523(String local1438_0, int local1438_1, int local1438_2, int local1438_3, int local1438_4) {
      return new AutoBuyItem.SubContext()
         .with__("Сферы")
         .getNested05235(getItemStack(local1438_0, local1438_1, local1438_2, local1438_3, local1438_4))
         .getNested05234(Items.PLAYER_HEAD);
   }

   public static AutoBuyItem getAutoBuyItem(String local1439_0) {
      return list.stream().filter(autoBuyItem -> autoBuyItem.name().equals(local1439_0)).findFirst().orElse(null);
   }

   public static void updateState() {
      if (!flag) {
         list.clear();
         getAutoBuyItem2(
            getNested05234(Items.NETHERITE_HELMET, "Броня")
               .getNested05238("Шлем Крушителя")
               .getNested05237(EnchantmentRequirement.of(Enchantments.RESPIRATION, 3), EnchantmentRequirement.of(Enchantments.AQUA_AFFINITY, 1))
               .getAutoBuyItem()
         );
         getAutoBuyItem2(getNested05234(Items.NETHERITE_CHESTPLATE, "Броня").getNested05238("Нагрудник Крушителя").getAutoBuyItem());
         getAutoBuyItem2(getNested05234(Items.NETHERITE_LEGGINGS, "Броня").getNested05238("Поножи Крушителя").getAutoBuyItem());
         getAutoBuyItem2(
            getNested05234(Items.NETHERITE_BOOTS, "Броня")
               .getNested05238("Ботинки Крушителя")
               .getNested05237(
                  EnchantmentRequirement.of(Enchantments.FEATHER_FALLING, 4),
                  EnchantmentRequirement.of(Enchantments.SOUL_SPEED, 3),
                  EnchantmentRequirement.of(Enchantments.DEPTH_STRIDER, 3)
               )
               .getAutoBuyItem()
         );
         getAutoBuyItem2(
            new AutoBuyItem.SubContext()
               .with__("Оружие")
               .getNested05238("Меч Крушителя")
               .getNested05234(Items.NETHERITE_SWORD)
               .getNested05235(getItemStack2(Items.NETHERITE_SWORD))
               .getNested05237(
                  EnchantmentRequirement.of(Enchantments.SHARPNESS, 7),
                  EnchantmentRequirement.of(Enchantments.SMITE, 7),
                  EnchantmentRequirement.of(Enchantments.BANE_OF_ARTHROPODS, 7),
                  EnchantmentRequirement.of(Enchantments.SWEEPING_EDGE, 3),
                  EnchantmentRequirement.of(Enchantments.FIRE_ASPECT, 2),
                  EnchantmentRequirement.of(Enchantments.LOOTING, 5),
                  EnchantmentRequirement.of(Enchantments.UNBREAKING, 5),
                  EnchantmentRequirement.of(Enchantments.MENDING, 1)
               )
               .getNested0523("Детекция III", "Окисление II", "Вампиризм II", "Опытный III", "Яд III")
               .getAutoBuyItem()
         );
         getAutoBuyItem2(
            new AutoBuyItem.SubContext()
               .with__("Оружие")
               .getNested05238("Булава Крушителя")
               .getNested05234(Items.MACE)
               .getNested05235(getItemStack2(Items.MACE))
               .getNested05237(
                  EnchantmentRequirement.of(Enchantments.SHARPNESS, 7),
                  EnchantmentRequirement.of(Enchantments.SMITE, 7),
                  EnchantmentRequirement.of(Enchantments.BANE_OF_ARTHROPODS, 7),
                  EnchantmentRequirement.of(Enchantments.DENSITY, 5),
                  EnchantmentRequirement.of(Enchantments.BREACH, 3),
                  EnchantmentRequirement.of(Enchantments.SWEEPING_EDGE, 3),
                  EnchantmentRequirement.of(Enchantments.FIRE_ASPECT, 2),
                  EnchantmentRequirement.of(Enchantments.LOOTING, 5),
                  EnchantmentRequirement.of(Enchantments.UNBREAKING, 5),
                  EnchantmentRequirement.of(Enchantments.MENDING, 1)
               )
               .getNested0523("Опытный III", "Вампиризм II", "Окисление II", "Яд III", "Детекция III")
               .getAutoBuyItem()
         );
         getAutoBuyItem2(
            new AutoBuyItem.SubContext()
               .with__("Оружие")
               .getNested05238("Кирка Крушителя")
               .getNested05234(Items.NETHERITE_PICKAXE)
               .getNested05235(getItemStack2(Items.NETHERITE_PICKAXE))
               .getNested05237(
                  EnchantmentRequirement.of(Enchantments.FORTUNE, 5),
                  EnchantmentRequirement.of(Enchantments.EFFICIENCY, 10),
                  EnchantmentRequirement.of(Enchantments.UNBREAKING, 5),
                  EnchantmentRequirement.of(Enchantments.MENDING, 1)
               )
               .getNested0523("Паутина", "Магнит", "Пингер", "Опытный III", "Авто-Плавка", "Бульдозер II")
               .getAutoBuyItem()
         );
         getAutoBuyItem2(
            new AutoBuyItem.SubContext()
               .with__("Оружие")
               .getNested05238("Трезубец Крушителя")
               .getNested05234(Items.TRIDENT)
               .getNested05235(getItemStack2(Items.TRIDENT))
               .getNested05237(
                  EnchantmentRequirement.of(Enchantments.SHARPNESS, 7),
                  EnchantmentRequirement.of(Enchantments.IMPALING, 5),
                  EnchantmentRequirement.of(Enchantments.LOYALTY, 3),
                  EnchantmentRequirement.of(Enchantments.CHANNELING, 1),
                  EnchantmentRequirement.of(Enchantments.FIRE_ASPECT, 2),
                  EnchantmentRequirement.of(Enchantments.UNBREAKING, 5),
                  EnchantmentRequirement.of(Enchantments.MENDING, 1)
               )
               .getNested0523(
                  "Детекция III",
                  "Яд III",
                  "Возвращение",
                  "Вампиризм II",
                  "Опытный III",
                  "Окисление II",
                  "Ступор III",
                  "Притяжение II",
                  "Подрывник",
                  "Скаут III"
               )
               .getAutoBuyItem()
         );
         getAutoBuyItem2(
            new AutoBuyItem.SubContext()
               .with__("Оружие")
               .getNested05238("Арбалет Крушителя")
               .getNested05234(Items.CROSSBOW)
               .getNested05235(getItemStack2(Items.CROSSBOW))
               .getNested05237(
                  EnchantmentRequirement.of(Enchantments.MULTISHOT, 1),
                  EnchantmentRequirement.of(Enchantments.PIERCING, 5),
                  EnchantmentRequirement.of(Enchantments.QUICK_CHARGE, 3),
                  EnchantmentRequirement.of(Enchantments.UNBREAKING, 3),
                  EnchantmentRequirement.of(Enchantments.MENDING, 1)
               )
               .getAutoBuyItem()
         );
         getAutoBuyItem2(
            new AutoBuyItem.SubContext()
               .with__("Оружие")
               .getNested05238("Заострённый меч")
               .getNested05234(Items.NETHERITE_SWORD)
               .getNested05235(getItemStack2(Items.NETHERITE_SWORD))
               .getNested05237(EnchantmentRequirement.of(Enchantments.SHARPNESS, 8))
               .getAutoBuyItem()
         );
         getAutoBuyItem2(
            new AutoBuyItem.SubContext()
               .with__("Сферы")
               .getNested05238("Отмычка к сферам")
               .getNested05234(Items.TRIPWIRE_HOOK)
               .getNested05235(getItemStack2(Items.TRIPWIRE_HOOK))
               .getNested05233(Pair.of("spookystash:stash_id", "spheres"))
               .getAutoBuyItem()
         );
         getAutoBuyItem2(
            getNested0523(
                  "ewogICJ0aW1lc3RhbXAiIDogMTc1MDM0Mzg2MTE4NywKICAicHJvZmlsZUlkIiA6ICJlZGUyYzdhMGFjNjM0MTNiYjA5ZDNmMGJlZTllYzhlYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJTcGhlcmVBdGhlbmEiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTNmOWVlZGEzYmEyM2ZlMTQyM2M0MDM2ZTdkZDBhNzQ0NjFkZmY5NmJhZGM1YjJmMmI5ZmFhN2NjMTZmMzgyZiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9",
                  694031243,
                  1593913619,
                  -1794784623,
                  596248838
               )
               .getNested05238("Сфера Афины")
               .getNested05232(
                  AttributeBonus.of(EntityAttributes.ATTACK_SPEED, 0.15, Operation.ADD_MULTIPLIED_BASE),
                  AttributeBonus.of(EntityAttributes.MOVEMENT_SPEED, 0.15, Operation.ADD_MULTIPLIED_BASE),
                  AttributeBonus.of(EntityAttributes.ATTACK_DAMAGE, 3.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.MAX_HEALTH, -2.0, Operation.ADD_VALUE)
               )
               .getAutoBuyItem()
         );
         getAutoBuyItem2(
            getNested0523(
                  "ewogICJ0aW1lc3RhbXAiIDogMTc1MDI3ODY0MTkwMCwKICAicHJvZmlsZUlkIiA6ICIxNzRjZmRiNGEzY2I0M2I1YmZjZGU0MjRjM2JiMmM2ZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJtYXJhZWwxOCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9lN2E3YWU3Y2RjZjYxNmU4YjdhNDIyMWE2MjFiMjQzNTc1M2M2MGVkNmEyNThlYTA2MGRhZTMwMDJmZmU5ZTI4IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=",
                  -1582985800,
                  1375286202,
                  -1125478243,
                  595287099
               )
               .getNested05238("Сфера Хаоса")
               .getNested05232(
                  AttributeBonus.of(EntityAttributes.ATTACK_DAMAGE, 2.5, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.MOVEMENT_SPEED, 0.07, Operation.ADD_MULTIPLIED_BASE),
                  AttributeBonus.of(EntityAttributes.ATTACK_SPEED, 0.13, Operation.ADD_MULTIPLIED_BASE),
                  AttributeBonus.of(EntityAttributes.ARMOR, 1.5, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.MAX_HEALTH, -4.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.GRAVITY, 0.09, Operation.ADD_MULTIPLIED_BASE)
               )
               .getAutoBuyItem()
         );
         getAutoBuyItem2(
            getNested0523(
                  "ewogICJ0aW1lc3RhbXAiIDogMTc1MDI3ODYwODUyOCwKICAicHJvZmlsZUlkIiA6ICJkMTQ4NjFiM2UwZmM0Njk5OTFlMTcyNTllMzdiZjZhZCIsCiAgInByb2ZpbGVOYW1lIiA6ICJyYXhpdG9jbCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83NzFhOWE0OThiNGZhNWVjNDkzNjJmOWJjODhlZGE0ZjUyYjA0ZGU0OWQ3NWFhM2NhMzMyYTFmZWExYWEwZTU3IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=",
                  -2101547208,
                  2058105556,
                  -1495491604,
                  1400184240
               )
               .getNested05238("Сфера Сатира")
               .getNested05232(
                  AttributeBonus.of(EntityAttributes.ATTACK_DAMAGE, 2.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.ATTACK_SPEED, 0.15, Operation.ADD_MULTIPLIED_BASE),
                  AttributeBonus.of(EntityAttributes.JUMP_STRENGTH, -0.1, Operation.ADD_MULTIPLIED_BASE)
               )
               .getAutoBuyItem()
         );
         getAutoBuyItem2(
            getNested0523(
                  "ewogICJ0aW1lc3RhbXAiIDogMTc1MDM0MzgzNDkzMCwKICAicHJvZmlsZUlkIiA6ICI1MzUzNWIxN2M0ZDY0NWQ0YWUwY2U2ZjM4Zjk0NTFjYSIsCiAgInByb2ZpbGVOYW1lIiA6ICJVYml2aXMiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTQxMWFjMTczODFiOWZjZTliYWIzYzcyYWZkYjdmMTk4NTcwZGFmNDczMmJkODExZDMxYzIyN2Q4MGZhMzliMSIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9",
                  -1896649230,
                  -398640094,
                  -1320063783,
                  945063190
               )
               .getNested05238("Сфера Бестии")
               .getNested05232(
                  AttributeBonus.of(EntityAttributes.ARMOR, 1.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.MOVEMENT_SPEED, 0.1, Operation.ADD_MULTIPLIED_BASE),
                  AttributeBonus.of(EntityAttributes.ATTACK_SPEED, 0.1, Operation.ADD_MULTIPLIED_BASE),
                  AttributeBonus.of(EntityAttributes.MAX_HEALTH, 4.0, Operation.ADD_VALUE)
               )
               .getAutoBuyItem()
         );
         getAutoBuyItem2(
            getNested0523(
                  "ewogICJ0aW1lc3RhbXAiIDogMTc1MDM0Mzc3NDI1NSwKICAicHJvZmlsZUlkIiA6ICJhYWMxYjA2OWNkMjE0NWE2ODNlNzQxNzE4MDcxMGU4MiIsCiAgInByb2ZpbGVOYW1lIiA6ICJqdXNhbXUiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzE2YWRjNmJhZmNiNTdmZDcwN2RlZTdkZDZhNzM2ZmUxMjY3MTFkNTNhMWZkNmNlNzg5ZGE0MWIzYmUxM2YyYSIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9",
                  2124864246,
                  465122563,
                  -1511078600,
                  -555424585
               )
               .getNested05238("Сфера Ареса")
               .getNested05232(
                  AttributeBonus.of(EntityAttributes.MAX_HEALTH, -2.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.ARMOR, -2.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.ATTACK_DAMAGE, 6.0, Operation.ADD_VALUE)
               )
               .getAutoBuyItem()
         );
         getAutoBuyItem2(
            getNested0523(
                  "ewogICJ0aW1lc3RhbXAiIDogMTc1MDI3ODUzMjE4MywKICAicHJvZmlsZUlkIiA6ICI1OGZmZWI5NTMxNGQ0ODcwYTQwYjVjYjQyZDRlYTU5OCIsCiAgInByb2ZpbGVOYW1lIiA6ICJTa2luREJuZXQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2UzYzExOGQ2OTZkOTEwZTU0ZGUwMmNhNGQ4MDc1NDNmOWIxOGMwMDhjOTgzOGQyZmY2OTM3NzYyMmZiMWQzMiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9",
                  -1308798306,
                  1128020291,
                  -1307054059,
                  -1317369961
               )
               .getNested05238("Сфера Гидры")
               .getNested05232(
                  AttributeBonus.of(EntityAttributes.ARMOR, 2.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.MAX_HEALTH, 4.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.SUBMERGED_MINING_SPEED, 0.5, Operation.ADD_MULTIPLIED_BASE),
                  AttributeBonus.of(EntityAttributes.OXYGEN_BONUS, 0.5, Operation.ADD_MULTIPLIED_BASE)
               )
               .getAutoBuyItem()
         );
         getAutoBuyItem2(
            getNested0523(
                  "ewogICJ0aW1lc3RhbXAiIDogMTc1MDI3ODU4MjQ5MSwKICAicHJvZmlsZUlkIiA6ICJhZWNkODIxZTQyYzE0ZDJlOThmNTA1OTg1MWI5OWMzNyIsCiAgInByb2ZpbGVOYW1lIiA6ICJSb2RyaVgyMDc1IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2M2ODAzZTZkNTY2N2EyZDYxMDYyOGJjM2IzMmY4NjNjZGE0OTVjNDY1NjE2ZGU2NTVjYjMyOTkzM2I2MWFmNzciLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==",
                  1858435695,
                  -517000716,
                  -1346102858,
                  1389869685
               )
               .getNested05238("Сфера Икара")
               .getNested05232(
                  AttributeBonus.of(EntityAttributes.ATTACK_DAMAGE, 2.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.MAX_HEALTH, 2.0, Operation.ADD_VALUE)
               )
               .getAutoBuyItem()
         );
         getAutoBuyItem2(
            getNested0523(
                  "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODFlOTY5ODQ1OGI3ODQxYzk2YWU0ZjI0ZWM4NGFlMDE3MjQxMDA2NDFjNTY0ZTJhN2IxODVmNDA2ZThlZDIzIn19fQ==",
                  654012711,
                  1596536861,
                  -2049048826,
                  -850597571
               )
               .getNested05238("Сфера Титана")
               .getNested05232(
                  AttributeBonus.of(EntityAttributes.ARMOR, 2.5, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.ARMOR_TOUGHNESS, 2.5, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.MOVEMENT_SPEED, -0.15, Operation.ADD_MULTIPLIED_BASE)
               )
               .getAutoBuyItem()
         );
         getAutoBuyItem2(
            getNested0523(
                  "ewogICJ0aW1lc3RhbXAiIDogMTc1MDM0Mzg2MTE4NywKICAicHJvZmlsZUlkIiA6ICJlZGUyYzdhMGFjNjM0MTNiYjA5ZDNmMGJlZTllYzhlYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJ0aGVEZXZKYWRlIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzZlNGUyZjEwNDdmM2VjNmU5ZTQ1OTE4NDczOWUzM2I3YzFmYzYzYWQ4MjAyYmRhYjlmMDI0NTA4YWRkMjNlNWIiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==",
                  -122635583,
                  2062758360,
                  -2035264015,
                  874042263
               )
               .getNested05238("Сфера Эрида")
               .getNested05232(
                  AttributeBonus.of(EntityAttributes.LUCK, 1.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.MAX_HEALTH, 2.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.BLOCK_INTERACTION_RANGE, 1.0, Operation.ADD_VALUE)
               )
               .getAutoBuyItem()
         );
         getAutoBuyItem2(
            getNested05235(
                  "Талисман Крушителя",
                  AttributeBonus.of(EntityAttributes.MAX_HEALTH, 4.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.ATTACK_DAMAGE, 3.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.ARMOR, 2.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.ARMOR_TOUGHNESS, 2.0, Operation.ADD_VALUE)
               )
               .getAutoBuyItem()
         );
         getAutoBuyItem2(
            getNested05235(
                  "Талисман Карателя",
                  AttributeBonus.of(EntityAttributes.MAX_HEALTH, -4.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.MOVEMENT_SPEED, 0.1, Operation.ADD_MULTIPLIED_BASE),
                  AttributeBonus.of(EntityAttributes.ATTACK_DAMAGE, 7.0, Operation.ADD_VALUE)
               )
               .getAutoBuyItem()
         );
         getAutoBuyItem2(
            getNested05235(
                  "Талисман Раздора",
                  AttributeBonus.of(EntityAttributes.MAX_HEALTH, 2.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.MOVEMENT_SPEED, 0.1, Operation.ADD_MULTIPLIED_BASE),
                  AttributeBonus.of(EntityAttributes.ATTACK_SPEED, 0.1, Operation.ADD_MULTIPLIED_BASE),
                  AttributeBonus.of(EntityAttributes.ATTACK_DAMAGE, 4.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.ARMOR, -3.0, Operation.ADD_VALUE)
               )
               .getAutoBuyItem()
         );
         getAutoBuyItem2(
            getNested05235(
                  "Талисман Тирана",
                  AttributeBonus.of(EntityAttributes.MAX_HEALTH, -4.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.ATTACK_DAMAGE, 2.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.ARMOR, 2.0, Operation.ADD_VALUE)
               )
               .getAutoBuyItem()
         );
         getAutoBuyItem2(
            getNested05235(
                  "Талисман Ярости",
                  AttributeBonus.of(EntityAttributes.MAX_HEALTH, -4.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.ATTACK_DAMAGE, 5.0, Operation.ADD_VALUE)
               )
               .getAutoBuyItem()
         );
         getAutoBuyItem2(
            getNested05235(
                  "Талисман Вихря",
                  AttributeBonus.of(EntityAttributes.MAX_HEALTH, 2.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.ATTACK_SPEED, 0.15, Operation.ADD_MULTIPLIED_BASE),
                  AttributeBonus.of(EntityAttributes.MOVEMENT_SPEED, 0.15, Operation.ADD_MULTIPLIED_BASE)
               )
               .getAutoBuyItem()
         );
         getAutoBuyItem2(
            getNested05235(
                  "Талисман Мрака",
                  AttributeBonus.of(EntityAttributes.MAX_HEALTH, 1.5, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.ARMOR, 1.5, Operation.ADD_VALUE)
               )
               .getAutoBuyItem()
         );
         getAutoBuyItem2(
            getNested05235(
                  "Талисман Демона",
                  AttributeBonus.of(EntityAttributes.ATTACK_SPEED, 0.1, Operation.ADD_MULTIPLIED_BASE),
                  AttributeBonus.of(EntityAttributes.ATTACK_DAMAGE, 2.5, Operation.ADD_VALUE)
               )
               .getAutoBuyItem()
         );
         getAutoBuyItem2(
            getNested05232(
                  "Хлопушка",
                  StatusEffectRequirement.of(StatusEffects.SLOWNESS, 10, 8),
                  StatusEffectRequirement.of(StatusEffects.SPEED, 20, 4),
                  StatusEffectRequirement.of(StatusEffects.BLINDNESS, 5, 8),
                  StatusEffectRequirement.of(StatusEffects.GLOWING, 180, 0)
               )
               .getAutoBuyItem()
         );
         getAutoBuyItem2(
            getNested05232(
                  "Святая вода",
                  StatusEffectRequirement.of(StatusEffects.REGENERATION, 45, 0),
                  StatusEffectRequirement.of(StatusEffects.INVISIBILITY, 600, 1),
                  StatusEffectRequirement.instant(StatusEffects.INSTANT_HEALTH, 1)
               )
               .getAutoBuyItem()
         );
         getAutoBuyItem2(
            getNested05232("Зелье гнева", StatusEffectRequirement.of(StatusEffects.STRENGTH, 30, 4), StatusEffectRequirement.of(StatusEffects.SLOWNESS, 30, 3)).getAutoBuyItem()
         );
         getAutoBuyItem2(
            getNested05232(
                  "Зелье палладина",
                  StatusEffectRequirement.of(StatusEffects.RESISTANCE, 600, 0),
                  StatusEffectRequirement.of(StatusEffects.FIRE_RESISTANCE, 600, 0),
                  StatusEffectRequirement.of(StatusEffects.HEALTH_BOOST, 60, 2),
                  StatusEffectRequirement.of(StatusEffects.INVISIBILITY, 900, 0)
               )
               .getAutoBuyItem()
         );
         getAutoBuyItem2(
            getNested05232(
                  "Зелье ассасина",
                  StatusEffectRequirement.of(StatusEffects.STRENGTH, 60, 3),
                  StatusEffectRequirement.of(StatusEffects.SPEED, 300, 2),
                  StatusEffectRequirement.of(StatusEffects.HASTE, 60, 0),
                  StatusEffectRequirement.instant(StatusEffects.INSTANT_DAMAGE, 1)
               )
               .getAutoBuyItem()
         );
         getAutoBuyItem2(
            getNested05232(
                  "Зелье радиации",
                  StatusEffectRequirement.of(StatusEffects.POISON, 20, 0),
                  StatusEffectRequirement.of(StatusEffects.WITHER, 20, 0),
                  StatusEffectRequirement.of(StatusEffects.SLOWNESS, 20, 2),
                  StatusEffectRequirement.of(StatusEffects.HUNGER, 20, 4),
                  StatusEffectRequirement.of(StatusEffects.GLOWING, 20, 0)
               )
               .getAutoBuyItem()
         );
         getAutoBuyItem2(
            getNested05232(
                  "Снотворное",
                  StatusEffectRequirement.of(StatusEffects.WEAKNESS, 90, 0),
                  StatusEffectRequirement.of(StatusEffects.MINING_FATIGUE, 10, 0),
                  StatusEffectRequirement.of(StatusEffects.WITHER, 90, 2),
                  StatusEffectRequirement.of(StatusEffects.BLINDNESS, 10, 0)
               )
               .getAutoBuyItem()
         );
         getAutoBuyItem2(new AutoBuyItem.SubContext().with__("Прочее").getNested05238("Чарка").getNested05234(Items.ENCHANTED_GOLDEN_APPLE).getAutoBuyItem());
         getAutoBuyItem2(new AutoBuyItem.SubContext().with__("Прочее").getNested05238("Золотое яблоко").getNested05234(Items.GOLDEN_APPLE).getAutoBuyItem());
         getAutoBuyItem2(new AutoBuyItem.SubContext().with__("Прочее").getNested05238("Золотая морковь").getNested05234(Items.GOLDEN_CARROT).getAutoBuyItem());
         getAutoBuyItem2(new AutoBuyItem.SubContext().with__("Прочее").getNested05238("Дезориентация").getNested05234(Items.ENDER_EYE).getAutoBuyItem());
         getAutoBuyItem2(getNested05233("Прочее", Items.PHANTOM_MEMBRANE, "Божья Аура", "effect-item-god").getAutoBuyItem());
         getAutoBuyItem2(getNested05233("Прочее", Items.PAPER, "Драконий скин", "trap-skin-item-dragon").getNested05235(getItemStack2(Items.PAPER)).getAutoBuyItem());
         getAutoBuyItem2(getNested05233("Прочее", Items.PAPER, "Неизбежный скин", "trap-skin-item-inevitable").getNested05235(getItemStack2(Items.PAPER)).getAutoBuyItem());
         getAutoBuyItem2(
            new AutoBuyItem.SubContext()
               .with__("Прочее")
               .getNested05238("Серебро")
               .getNested05234(Items.IRON_NUGGET)
               .getNested05233(Pair.of("spookystash:currency", "silver"))
               .getAutoBuyItem()
         );
         getAutoBuyItem2(new AutoBuyItem.SubContext().with__("Прочее").getNested05238("Алмаз").getNested05234(Items.DIAMOND).getAutoBuyItem());
         getAutoBuyItem2(
            new AutoBuyItem.SubContext()
               .with__("Прочее")
               .getNested05238("Книга починка")
               .getNested05234(Items.ENCHANTED_BOOK)
               .getNested05237(EnchantmentRequirement.of(Enchantments.MENDING, 1))
               .getAutoBuyItem()
         );
         getAutoBuyItem2(
            new AutoBuyItem.SubContext()
               .with__("Прочее")
               .getNested05238("Элитры Крушителя")
               .getNested05234(Items.ELYTRA)
               .getNested05237(EnchantmentRequirement.of(Enchantments.UNBREAKING, 5), EnchantmentRequirement.of(Enchantments.MENDING, 1))
               .getAutoBuyItem()
         );
         getAutoBuyItem2(
            getNested05233("Прочее", Items.GOLDEN_PICKAXE, "Божье касание", "spawner-item-spawner-break").getNested05235(getItemStack2(Items.GOLDEN_PICKAXE)).getAutoBuyItem()
         );
         getAutoBuyItem2(
            getNested05233("Прочее", Items.GOLDEN_PICKAXE, "Мощный удар", "bedrock-item-bedrock-break").getNested05235(getItemStack2(Items.GOLDEN_PICKAXE)).getAutoBuyItem()
         );
         getAutoBuyItem2(
            getNested05233("Прочее", Items.NETHERITE_PICKAXE, "Молот Тора", "radius-item-mega-buldozer")
               .getNested05235(getItemStack2(Items.NETHERITE_PICKAXE))
               .getAutoBuyItem()
         );
         getAutoBuyItem2(getNested05233("Прочее", Items.NETHERITE_SCRAP, "Трапка", "schematic-item-trap").getAutoBuyItem());
         getAutoBuyItem2(new AutoBuyItem.SubContext().with__("Прочее").getNested05238("Спавнер").getNested05234(Items.SPAWNER).getAutoBuyItem());
         getAutoBuyItem2(new AutoBuyItem.SubContext().with__("Прочее").getNested05238("Маяк").getNested05234(Items.BEACON).getAutoBuyItem());
         getAutoBuyItem2(
            new AutoBuyItem.SubContext().with__("Прочее").getNested05238("Череп визер-скелета").getNested05234(Items.WITHER_SKELETON_SKULL).getAutoBuyItem()
         );
         getAutoBuyItem2(new AutoBuyItem.SubContext().with__("Прочее").getNested05238("Голова дракона").getNested05234(Items.DRAGON_HEAD).getAutoBuyItem());
         getAutoBuyItem2(getNested05233("Прочее", Items.TNT, "Таер блэк", "tnt-item-black").getAutoBuyItem());
         getAutoBuyItem2(getNested05233("Прочее", Items.TNT, "Таер вайт", "tnt-item-white").getAutoBuyItem());
         getAutoBuyItem2(new AutoBuyItem.SubContext().with__("Прочее").getNested05238("Незеритовый слиток").getNested05234(Items.NETHERITE_INGOT).getAutoBuyItem());
         getAutoBuyItem2(new AutoBuyItem.SubContext().with__("Прочее").getNested05238("Изумрудная руда").getNested05234(Items.EMERALD_ORE).getAutoBuyItem());
         getAutoBuyItem2(getNested05233("Прочее", Items.SNOWBALL, "Снежок заморозка", "effect-item-snowball").getAutoBuyItem());
         getAutoBuyItem2(getNested05233("Прочее", Items.FIRE_CHARGE, "Огненный смерч", "effect-item-smerch").getAutoBuyItem());
         getAutoBuyItem2(getNested05233("Прочее", Items.SUGAR, "Явная пыль", "effect-item-dust").getAutoBuyItem());
         getAutoBuyItem2(getNested05233("Прочее", Items.DRIED_KELP, "Пласт", "schematic-item-plast").getAutoBuyItem());
         getAutoBuyItem2(
            new AutoBuyItem.SubContext()
               .with__("Прочее")
               .getNested05238("Кровавая стрела")
               .getNested05234(Items.TIPPED_ARROW)
               .getNested05236(
                  StatusEffectRequirement.of(StatusEffects.WEAKNESS, 3, 2),
                  StatusEffectRequirement.of(StatusEffects.BLINDNESS, 2, 0),
                  StatusEffectRequirement.of(StatusEffects.MINING_FATIGUE, 2, 0),
                  StatusEffectRequirement.of(StatusEffects.NAUSEA, 5, 0)
               )
               .getAutoBuyItem()
         );
         getAutoBuyItem2(
            new AutoBuyItem.SubContext()
               .with__("Прочее")
               .getNested05238("Мучительная стрела")
               .getNested05234(Items.TIPPED_ARROW)
               .getNested05236(
                  StatusEffectRequirement.of(StatusEffects.SLOWNESS, 5, 2),
                  StatusEffectRequirement.of(StatusEffects.WITHER, 5, 2),
                  StatusEffectRequirement.of(StatusEffects.POISON, 5, 2)
               )
               .getAutoBuyItem()
         );
         getAutoBuyItem2(getNested05233("Прочее", Items.STRUCTURE_BLOCK, "Чанкер 1x1", "executable-block-chunker-1").getAutoBuyItem());
         getAutoBuyItem2(getNested05233("Прочее", Items.JIGSAW, "Дамагер", "executable-block-damager").getAutoBuyItem());
         getAutoBuyItem2(getNested05233("Прочее", Items.POPPED_CHORUS_FRUIT, "Модификатор варпов", "modifier-item-setwarp-day").getAutoBuyItem());
         getAutoBuyItem2(
            new AutoBuyItem.SubContext()
               .with__("Прочее")
               .getNested05234(Items.FEATHER)
               .getNested05238("Модификатор полёта")
               .getNested05233(Pair.of("spookyitems:spooky-item", "modifier-item-fly-day"))
               .getAutoBuyItem()
         );
         getAutoBuyItem2(getNested05233("Прочее", Items.ENDER_EYE, "Модификатор эндер-сундука", "modifier-item-ec-day").getAutoBuyItem());
         getAutoBuyItem2(getNested05233("Прочее", Items.BUBBLE_CORAL, "Модификатор починки", "modifier-item-fix-day").getAutoBuyItem());
         flag = mc.world != null;
      }
   }

   public static boolean matchesCriteria2(ItemStack itemStack, AutoBuyItem autoBuyItem) {
      return matchesCriteria4(autoBuyItem) ? biPredicate.test(itemStack, autoBuyItem) : AutoBuyItem.CHECKER.test(itemStack, autoBuyItem);
   }

   private static String formatText(ProfileComponent profileComponent) {
      for (Property property : profileComponent.getGameProfile().properties().get("textures")) {
         if ("textures".equals(property.name())) {
            return property.value();
         }
      }

      return null;
   }

   private AuctionItemCatalog() {
   }

   private static AutoBuyItem.SubContext getNested05232(String local1446_0, StatusEffectRequirement... recoveredClass0114Array) {
      return new AutoBuyItem.SubContext()
         .with__("Зелья")
         .getNested05238(local1446_0)
         .getNested05234(Items.SPLASH_POTION)
         .getNested05235(getItemStack3(recoveredClass0114Array))
         .getNested05236(recoveredClass0114Array);
   }

   private static ItemStack getItemStack(String local1447_0, int local1447_1, int local1447_2, int local1447_3, int local1447_4) {
      ItemStack itemStack = new ItemStack(Items.PLAYER_HEAD);
      itemStack.set(
         DataComponentTypes.PROFILE,
         ProfileComponent.ofStatic(
            new GameProfile(
               new UUID((long)local1447_1 << 32 | local1447_2 & 4294967295L, (long)local1447_3 << 32 | local1447_4 & 4294967295L),
               "",
               new PropertyMap(ImmutableMultimap.of("textures", new Property("textures", local1447_0)))
            )
         )
      );
      return itemStack;
   }

   public static float computeFloat() {
      float local1448_0 = 0.0F;

      for (AuctionItemCatalog.DataRecord local1448_2 : getArrayList()) {
         local1448_0 += 8.5F + local1448_2.items().size() * 17.5F + 4.0F;
      }

      return local1448_0;
   }

   private static ItemStack getItemStack2(Item newItem) {
      ItemStack defaultStack = newItem.getDefaultStack();
      if (mc.world == null) {
         return defaultStack;
      }

      defaultStack.addEnchantment(mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING), 1);
      return defaultStack;
   }

   private static AutoBuyItem getAutoBuyItem2(AutoBuyItem autoBuyItem) {
      list.add(autoBuyItem);
      return autoBuyItem;
   }

   private static AutoBuyItem.SubContext getNested05233(String __, Item newItem, String local1451_2, String local1451_3) {
      return new AutoBuyItem.SubContext()
         .with__(__)
         .getNested05234(newItem)
         .getNested05238(local1451_2)
         .getNested05233(Pair.of("spookyitems:spooky-item", local1451_3));
   }

   private static AutoBuyItem.SubContext getNested05234(Item newItem, String __) {
      return new AutoBuyItem.SubContext()
         .with__(__)
         .getNested05234(newItem)
         .getNested05235(getItemStack2(newItem))
         .getNested05237(
            EnchantmentRequirement.of(Enchantments.PROTECTION, 5),
            EnchantmentRequirement.of(Enchantments.BLAST_PROTECTION, 5),
            EnchantmentRequirement.of(Enchantments.FIRE_PROTECTION, 5),
            EnchantmentRequirement.of(Enchantments.PROJECTILE_PROTECTION, 5),
            EnchantmentRequirement.of(Enchantments.MENDING, 1),
            EnchantmentRequirement.of(Enchantments.UNBREAKING, 5)
         );
   }

   private static void processData(ArrayList<AuctionItemCatalog.DataRecord> local1454_0, String local1454_1) {
      ArrayList<AutoBuyItem> local1454_2 = list.stream()
         .filter(autoBuyItem -> autoBuyItem.category().equals(local1454_1))
         .collect(Collectors.toCollection(ArrayList::new));
      if (!local1454_2.isEmpty()) {
         local1454_0.add(new AuctionItemCatalog.DataRecord(local1454_1, local1454_2));
      }
   }

   private static ItemStack getItemStack3(StatusEffectRequirement... recoveredClass0114Array) {
      ItemStack defaultStack = Items.SPLASH_POTION.getDefaultStack();
      ArrayList<StatusEffectInstance> local1455_2 = new ArrayList<>();

      for (StatusEffectRequirement statusEffectRequirement : recoveredClass0114Array) {
         local1455_2.add(statusEffectRequirement.effectInstance());
      }

      defaultStack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.empty(), Optional.empty(), local1455_2, Optional.empty()));
      return defaultStack;
   }

   private static AutoBuyItem.SubContext getNested05235(String local1456_0, AttributeBonus... recoveredClass0021Array) {
      return new AutoBuyItem.SubContext().with__("Талисманы").getNested05238(local1456_0).getNested05234(Items.TOTEM_OF_UNDYING).getNested05232(recoveredClass0021Array);
   }

   public static boolean matchesCriteria3(Item newItem) {
      return newItem instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
   }

   public static float computeFloat2(int local1459_0) {
      if (local1459_0 <= 0) {
         return 0.0F;
      }

      int local1459_1 = (local1459_0 + 1) / 2;
      return local1459_1 * 115.0F;
   }

   public static AutoBuyItem getAutoBuyItem3(ItemStack itemStack) {
      if (itemStack.getItem() == Items.PLAYER_HEAD) {
         for (AutoBuyItem autoBuyItem : list) {
            if (matchesCriteria5(autoBuyItem) && biPredicate.test(itemStack, autoBuyItem)) {
               return autoBuyItem;
            }
         }
      }

      return list.stream().filter(autoBuyItem2 -> {
         if (matchesCriteria3(autoBuyItem2.item()) && matchesCriteria3(itemStack.getItem())) {
            return true;
         } else {
            return matchesCriteria5(autoBuyItem2) ? false : AutoBuyItem.CHECKER.test(itemStack, autoBuyItem2);
         }
      }).findFirst().orElse(null);
   }

   public static boolean matchesCriteria4(AutoBuyItem autoBuyItem) {
      return matchesCriteria5(autoBuyItem);
   }

   public static ArrayList<AutoBuyItem> getArrayList2() {
      return list;
   }

   private static boolean matchesCriteria5(AutoBuyItem autoBuyItem) {
      return autoBuyItem != null
         && autoBuyItem.item() == Items.PLAYER_HEAD
         && !autoBuyItem.attributes().isEmpty()
         && autoBuyItem.displayStack().get(DataComponentTypes.PROFILE) != null;
   }

   public record DataRecord(String title, ArrayList<AutoBuyItem> items) {
   }
}
