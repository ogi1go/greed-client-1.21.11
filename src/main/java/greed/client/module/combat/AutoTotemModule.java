package greed.client.module.combat;

import greed.client.util.AuctionItemCatalog;
import greed.client.setting.BooleanSetting;
import greed.mixin.BossBarHudMixin;
import greed.client.event.EventListener;
import greed.client.storage.FriendStore;
import greed.client.setting.LegacyNumberSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.setting.MultiSelectSetting;
import greed.client.event.PlayerTickEvent;
import greed.client.util.InventoryUtils;
import greed.client.util.AttributeBonus;
import greed.client.setting.NumericTextSetting;
import greed.client.util.InventoryActionQueue;
import greed.client.util.EntityUtils;
import greed.client.storage.AutoBuyItem;
import greed.client.module.player.ServerHelperModule;

import java.util.HashMap;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.AttributeModifiersComponent.Entry;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.MathHelper;

@ModuleInfo(name = "AutoTotem", description = "Автоматически берёт тотем в левую руку по триггерам.", category = ModuleCategory.COMBAT)
public final class AutoTotemModule extends Module {
   private static AutoTotemModule autoTotemModule;
   public MultiSelectSetting multiSelectSetting;
   public LegacyNumberSetting healthThreshold;
   public LegacyNumberSetting fallDistance;
   public LegacyNumberSetting tntRadius;
   public LegacyNumberSetting tridentRadius;
   public LegacyNumberSetting maceRadius;
   public LegacyNumberSetting crystalRadius;
   public BooleanSetting saveEnchantedTotem;
   public BooleanSetting takeEnchantedWithSave;
   public NumericTextSetting numericTextSetting;
   public BooleanSetting displayTotemPop;
   private ItemStack savedOffhandStack = ItemStack.EMPTY;
   private boolean isTotemEquippedByModule;
   private long lastSwapTimeMillis;
   private int originalOffhandSlot = -1;
   private long lastActionMillis;
   private final HashMap<Integer, Double> playerPeakFallY = new HashMap<>();
   private final EventListener<PlayerTickEvent> totemTickListener = playerTickEvent -> {
      if (mc.player != null && mc.world != null) {
         if (mc.world != this.currentWorld) {
            this.resetTotemState();
            this.lastSwapTimeMillis = 0L;
            this.playerPeakFallY.clear();
            this.currentWorld = mc.world;
         }

         this.updatePredictedDamage();
         if (mc.player.currentScreenHandler == mc.player.playerScreenHandler) {
            if (System.currentTimeMillis() - this.lastActionMillis >= 250L) {
               if (this.isTotemEquippedByModule && mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING && !this.hasPendingTotemSwap()) {
                  this.resetTotemState();
               }

               if (this.shouldEquipTotem()) {
                  if (!this.hasValidOffhandTotem() && this.findTotemSlot() != -1) {
                     this.equipTotem();
                  }
               } else if (this.isTotemEquippedByModule && System.currentTimeMillis() >= this.lastSwapTimeMillis) {
                  if (this.findReturnSlot() != -1) {
                     this.restorePreviousOffhandItem();
                  }
               }
            }
         }
      }
   };
   private ClientWorld currentWorld;

   private boolean isTntNearby() {
      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof TntEntity && entity.squaredDistanceTo(mc.player) < this.tntRadius.getValue() * this.tntRadius.getValue()
            )
          {
            return true;
         }
      }

      return false;
   }

   public static AutoTotemModule getInstance() {
      if (autoTotemModule == null) {
         autoTotemModule = new AutoTotemModule();
      }

      return autoTotemModule;
   }

   private boolean isTridentNearby() {
      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof TridentEntity
            && entity.squaredDistanceTo(mc.player) < this.tridentRadius.getValue() * this.tridentRadius.getValue()) {
            return true;
         }
      }

      return false;
   }

   public boolean isCustomTotem(ItemStack itemStack) {
      AuctionItemCatalog.updateState();
      boolean local6104_2 = false;

      for (AutoBuyItem autoBuyItem : AuctionItemCatalog.getArrayList2()) {
         if (autoBuyItem.item() == Items.TOTEM_OF_UNDYING && autoBuyItem.name().toLowerCase().contains(this.numericTextSetting.getText().toLowerCase())) {
            local6104_2 = true;
            if (AutoBuyItem.CHECKER.test(itemStack, autoBuyItem)) {
               return true;
            }
         }
      }

      return !local6104_2 && itemStack.getName().getString().toLowerCase().contains(this.numericTextSetting.getText().toLowerCase());
   }

   public boolean isTotemActive() {
      return this.isEnabled() && this.isTotemEquippedByModule;
   }

   private boolean hasValidOffhandTotem() {
      if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
         return false;
      } else if (this.isCustomTotemEnabled()
         && !this.isCustomTotem(mc.player.getOffHandStack())
         && this.findTotemSlot() != -1
         && this.isCustomTotem(mc.player.getInventory().getStack(this.findTotemSlot()))) {
         return false;
      } else {
         return (this.saveEnchantedTotem.isChecked() || this.takeEnchantedWithSave.isChecked()) && this.shouldSaveTotem(mc.player.getOffHandStack())
            ? this.findTotemSlot() == -1 || this.shouldSaveTotem(mc.player.getInventory().getStack(this.findTotemSlot()))
            : true;
      }
   }

   private AutoTotemModule() {
      this.multiSelectSetting = this.addSetting(
         new MultiSelectSetting(
            "Триггеры",
            "Когда автоматически брать тотем в левую руку.",
            new String[]{"Здоровье", "Падение", "Динамит", "Трезубец", "Булава", "Кристалл"},
            "Здоровье"
         )
      );
      this.healthThreshold = this.addSetting(new LegacyNumberSetting("Здоровье", "При каком здоровье брать тотем.", 8.0, 1.0, 19.0, 0.5));
      this.fallDistance = this.addSetting(new LegacyNumberSetting("Дистанция падения", "С какой высоты падения брать тотем.", 6.0, 3.0, 40.0, 0.5));
      this.tntRadius = this.addSetting(new LegacyNumberSetting("Радиус динамита", "На каком расстоянии поджённый динамит берёт тотем.", 8.0, 1.0, 16.0, 0.5));
      this.tridentRadius = this.addSetting(new LegacyNumberSetting("Радиус трезубца", "На каком расстоянии летящий трезубец берёт тотем.", 8.0, 1.0, 16.0, 0.5));
      this.crystalRadius = this.addSetting(new LegacyNumberSetting("Радиус кристалла", "На каком расстоянии кристалл края берёт тотем.", 6.0, 1.0, 16.0, 0.5));
      this.maceRadius = this.addSetting(
         new LegacyNumberSetting("Радиус булавы", "На каком расстоянии по горизонтали игрок с булавой берёт тотем.", 7.0, 1.0, 16.0, 0.5)
      );
      this.saveEnchantedTotem = this.addSetting(
         new BooleanSetting("Сохранять зачарованный тотем", "Берёт обычные тотемы, зачарованный только если обычных нет.", true)
      );
      this.takeEnchantedWithSave = this.addSetting(
         new BooleanSetting(
            "Брать зачарованный с сохранением", "Дон-тотем в руке сохраняется, на замену берётся зачарованный тотем с выбранным названием для перебива.", false
         )
      );
      this.numericTextSetting = this.addSetting(new NumericTextSetting("Название тотема", "Часть названия зачарованного тотема на замену.", "Тирана", 32));
      this.displayTotemPop = this.addSetting(new BooleanSetting("Отображать снесение тотема", "Рисует призрака игрока на месте лопнувшего тотема.", false));
      this.numericTextSetting.withVisibility(() -> this.takeEnchantedWithSave.isChecked());
      this.healthThreshold.withVisibility(() -> this.multiSelectSetting.matchesOption("Здоровье"));
      this.fallDistance.withVisibility(() -> this.multiSelectSetting.matchesOption("Падение"));
      this.tntRadius.withVisibility(() -> this.multiSelectSetting.matchesOption("Динамит"));
      this.tridentRadius.withVisibility(() -> this.multiSelectSetting.matchesOption("Трезубец"));
      this.maceRadius.withVisibility(() -> this.multiSelectSetting.matchesOption("Булава"));
      this.crystalRadius.withVisibility(() -> this.multiSelectSetting.matchesOption("Кристалл"));
   }

   private boolean shouldSaveTotem(ItemStack itemStack) {
      return this.takeEnchantedWithSave.isChecked()
            && !this.numericTextSetting.getText().isEmpty()
            && !EntityUtils.isDonationTotem(itemStack)
            && this.isCustomTotem(itemStack)
         ? false
         : EntityUtils.isDonationTotem(itemStack) || itemStack.hasEnchantments();
   }

   private boolean isFallingDangerously() {
      return mc.player.fallDistance >= this.fallDistance.getValue();
   }

   private float getOffhandAttackBonus(ItemStack itemStack) {
      if (itemStack.isEmpty()) {
         return 0.0F;
      }

      float local6111_2 = 0.0F;
      AuctionItemCatalog.updateState();

      for (AutoBuyItem autoBuyItem : AuctionItemCatalog.getArrayList2()) {
         if (!autoBuyItem.attributes().isEmpty() && AutoBuyItem.CHECKER.test(itemStack, autoBuyItem)) {
            for (AttributeBonus attributeBonus : autoBuyItem.attributes()) {
               if (attributeBonus.attribute().matches(EntityAttributes.ATTACK_DAMAGE) && attributeBonus.operation() == Operation.ADD_VALUE) {
                  local6111_2 += (float)attributeBonus.amount();
               }
            }

            return local6111_2;
         }
      }

      for (Entry entry : itemStack.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT).modifiers()) {
         if (entry.attribute().matches(EntityAttributes.ATTACK_DAMAGE) && entry.modifier().operation() == Operation.ADD_VALUE) {
            local6111_2 += (float)entry.modifier().value();
         }
      }

      return local6111_2;
   }

   private boolean isCrystalNearby() {
      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof EndCrystalEntity
            && entity.squaredDistanceTo(mc.player) < this.crystalRadius.getValue() * this.crystalRadius.getValue()) {
            return true;
         }
      }

      return false;
   }

   private float getBaseAttackDamage(AbstractClientPlayerEntity abstractClientPlayerEntity) {
      float local6113_2 = 1.0F;

      for (Entry entry : abstractClientPlayerEntity.getMainHandStack()
         .getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT)
         .modifiers()) {
         if (entry.attribute().matches(EntityAttributes.ATTACK_DAMAGE)
            && entry.slot().matches(EquipmentSlot.MAINHAND)
            && entry.modifier().operation() == Operation.ADD_VALUE) {
            local6113_2 += (float)entry.modifier().value();
         }
      }

      return Math.max(local6113_2, 6.0F) + this.getOffhandAttackBonus(abstractClientPlayerEntity.getOffHandStack());
   }

   private int findSavedItemSlot() {
      if (this.savedOffhandStack.isEmpty()) {
         return -1;
      }

      if (this.originalOffhandSlot != -1 && ItemStack.areItemsAndComponentsEqual(mc.player.getInventory().getStack(this.originalOffhandSlot), this.savedOffhandStack)) {
         return this.originalOffhandSlot;
      }

      int local6114_1 = InventoryUtils.findSlotMatching(itemStack -> ItemStack.areItemsAndComponentsEqual(itemStack, this.savedOffhandStack), 0, 35);
      return local6114_1 != -1 ? local6114_1 : InventoryUtils.findSlotMatching(itemStack2 -> itemStack2.getItem() == this.savedOffhandStack.getItem(), 0, 35);
   }

   private float calculatePredictedMaceDamage(AbstractClientPlayerEntity abstractClientPlayerEntity, float local6115_2) {
      ItemStack mainHandStack = abstractClientPlayerEntity.getMainHandStack();
      int local6115_4 = EnchantmentHelper.getLevel(
         mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.DENSITY), mainHandStack
      );
      int local6115_5 = EnchantmentHelper.getLevel(
         mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.BREACH), mainHandStack
      );
      float local6115_6 = local6115_2 <= 3.0F ? 4.0F * local6115_2 : (local6115_2 <= 8.0F ? 12.0F + 2.0F * (local6115_2 - 3.0F) : 22.0F + local6115_2 - 8.0F);
      float local6115_7 = (this.getBaseAttackDamage(abstractClientPlayerEntity) + local6115_6 + 0.5F * local6115_4 * local6115_2) * 1.5F;
      float armor = mc.player.getArmor();
      float local6115_9 = (float)mc.player.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS);
      float local6115_10 = MathHelper.clamp(
         MathHelper.clamp(armor - local6115_7 / (2.0F + local6115_9 / 4.0F), armor * 0.2F, 20.0F) / 25.0F - 0.15F * local6115_5, 0.0F, 1.0F
      );
      float local6115_11 = local6115_7 * (1.0F - local6115_10);
      StatusEffectInstance statusEffect = mc.player.getStatusEffect(StatusEffects.RESISTANCE);
      if (statusEffect != null) {
         local6115_11 *= Math.max(0.0F, 1.0F - (statusEffect.getAmplifier() + 1) * 0.2F);
      }

      int local6115_13 = 0;

      for (EquipmentSlot equipmentSlot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
         local6115_13 += EnchantmentHelper.getLevel(
            mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION),
            mc.player.getEquippedStack(equipmentSlot)
         );
      }

      return local6115_11 * (1.0F - Math.min(local6115_13, 20) / 25.0F);
   }

   private void resetTotemState() {
      this.isTotemEquippedByModule = false;
      this.savedOffhandStack = ItemStack.EMPTY;
      this.originalOffhandSlot = -1;
   }

   private boolean isCustomTotemEnabled() {
      return this.takeEnchantedWithSave.isChecked()
         && !this.numericTextSetting.getText().isEmpty()
         && this.multiSelectSetting.matchesOption("Здоровье")
         && mc.player.getHealth() + mc.player.getAbsorptionAmount() <= this.healthThreshold.getValue();
   }

   private void equipTotem() {
      if (mc.player != null && mc.interactionManager != null && mc.player.currentScreenHandler == mc.player.playerScreenHandler
         )
       {
         if ((this.shouldEquipTotem() || System.currentTimeMillis() < this.lastSwapTimeMillis) && !this.hasValidOffhandTotem()) {
            int local6121_1 = this.findTotemSlot();
            if (local6121_1 != -1) {
               if (!this.isTotemEquippedByModule) {
                  this.savedOffhandStack = mc.player.getOffHandStack().copy();
                  this.originalOffhandSlot = local6121_1;
               }

               this.isTotemEquippedByModule = true;
               this.lastActionMillis = System.currentTimeMillis();
               InventoryActionQueue.getInstance().processData5(local6121_1, 40, 1);
            }
         }
      }
   }

   private int findTotemSlot() {
      if (this.isCustomTotemEnabled()) {
         int local6122_1 = InventoryUtils.findSlotMatching(
            itemStack -> itemStack.getItem() == Items.TOTEM_OF_UNDYING
               && itemStack.hasEnchantments()
               && !EntityUtils.isDonationTotem(itemStack)
               && this.isCustomTotem(itemStack)
               && !mc.player.getItemCooldownManager().isCoolingDown(itemStack),
            0,
            35
         );
         if (local6122_1 != -1) {
            return local6122_1;
         }
      }

      if (!this.saveEnchantedTotem.isChecked() && !this.takeEnchantedWithSave.isChecked()) {
         return InventoryUtils.findSlotMatching(
            itemStack2 -> itemStack2.getItem() == Items.TOTEM_OF_UNDYING
               && !AutoSwapModule.getInstance().isConfiguredSwapItem(itemStack2)
               && !mc.player.getItemCooldownManager().isCoolingDown(itemStack2),
            0,
            35
         );
      }

      int local6122_1 = InventoryUtils.findSlotMatching(
         itemStack3 -> itemStack3.getItem() == Items.TOTEM_OF_UNDYING
            && !this.shouldSaveTotem(itemStack3)
            && !AutoSwapModule.getInstance().isConfiguredSwapItem(itemStack3)
            && !mc.player.getItemCooldownManager().isCoolingDown(itemStack3),
         0,
         35
      );
      return local6122_1 != -1
         ? local6122_1
         : InventoryUtils.findSlotMatching(
            itemStack4 -> itemStack4.getItem() == Items.TOTEM_OF_UNDYING
               && !AutoSwapModule.getInstance().isConfiguredSwapItem(itemStack4)
               && !mc.player.getItemCooldownManager().isCoolingDown(itemStack4),
            0,
            35
         );
   }

   private boolean shouldEquipTotem() {
      if ((
            !this.multiSelectSetting.matchesOption("Здоровье")
               || !(mc.player.getHealth() + mc.player.getAbsorptionAmount() <= this.healthThreshold.getValue())
         )
         && (!this.multiSelectSetting.matchesOption("Падение") || !this.isFallingDangerously())
         && (!this.multiSelectSetting.matchesOption("Динамит") || !this.isTntNearby())
         && (!this.multiSelectSetting.matchesOption("Трезубец") || !this.isTridentNearby())
         && (!this.multiSelectSetting.matchesOption("Булава") || !this.isMaceThreatNearby())
         && (!this.multiSelectSetting.matchesOption("Кристалл") || !this.isCrystalNearby())) {
         return false;
      }

      this.lastSwapTimeMillis = System.currentTimeMillis() + 100L;
      return true;
   }

   private boolean hasPendingTotemSwap() {
      return mc.player.getOffHandStack().isEmpty() && !this.savedOffhandStack.isEmpty() && this.findSavedItemSlot() != -1;
   }

   private void updatePredictedDamage() {
      this.playerPeakFallY.keySet().removeIf(local6131_0 -> mc.world.getEntityById(local6131_0) == null);

      for (AbstractClientPlayerEntity abstractClientPlayerEntity : mc.world.getPlayers()) {
         if (abstractClientPlayerEntity != mc.player) {
            if (!abstractClientPlayerEntity.isOnGround()
               && !(abstractClientPlayerEntity.getY() >= abstractClientPlayerEntity.lastY)
               && !abstractClientPlayerEntity.isGliding()
               && !abstractClientPlayerEntity.isTouchingWater()
               && !abstractClientPlayerEntity.isClimbing()) {
               this.playerPeakFallY.merge(abstractClientPlayerEntity.getId(), abstractClientPlayerEntity.getY(), Math::max);
            } else {
               this.playerPeakFallY.put(abstractClientPlayerEntity.getId(), abstractClientPlayerEntity.getY());
            }
         }
      }
   }

   private int findReturnSlot() {
      if (!this.savedOffhandStack.isEmpty()) {
         int local6130_1 = this.findSavedItemSlot();
         return local6130_1 != -1 ? local6130_1 : InventoryUtils.findEmptySlot(0, 35);
      } else {
         return this.originalOffhandSlot != -1 && mc.player.getInventory().getStack(this.originalOffhandSlot).isEmpty()
            ? this.originalOffhandSlot
            : InventoryUtils.findEmptySlot(0, 35);
      }
   }

   private void restorePreviousOffhandItem() {
      if (mc.player != null && mc.interactionManager != null && mc.player.currentScreenHandler == mc.player.playerScreenHandler
         )
       {
         if (this.isTotemEquippedByModule && !this.shouldEquipTotem() && System.currentTimeMillis() >= this.lastSwapTimeMillis) {
            if (mc.player.getOffHandStack().isEmpty()) {
               int local6133_1 = this.findSavedItemSlot();
               if (local6133_1 != -1) {
                  this.lastActionMillis = System.currentTimeMillis();
                  this.resetTotemState();
                  InventoryActionQueue.getInstance().processData5(local6133_1, 40, 1);
               }
            } else if (mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) {
               int local6133_1 = this.findReturnSlot();
               if (local6133_1 != -1) {
                  this.lastActionMillis = System.currentTimeMillis();
                  this.resetTotemState();
                  InventoryActionQueue.getInstance().processData5(local6133_1, 40, 1);
               }
            }
         }
      }
   }

   private boolean isMaceThreatNearby() {
      if (((BossBarHudMixin)mc.inGameHud.getBossBarHud()).getEvents().isEmpty()) {
         return false;
      }

      for (AbstractClientPlayerEntity abstractClientPlayerEntity : mc.world.getPlayers()) {
         if (!ServerHelperModule.getInstance().modeSetting.isSelected("SpookyTime")) {
            if (abstractClientPlayerEntity != mc.player
               && !FriendStore.isFriend(abstractClientPlayerEntity.getGameProfile().name())
               && abstractClientPlayerEntity.getMainHandStack().getItem() == Items.MACE
               && abstractClientPlayerEntity.getY() - mc.player.getY() >= 2.0
               && abstractClientPlayerEntity.getY() - abstractClientPlayerEntity.lastY < -0.1
               && mc.player.squaredDistanceTo(abstractClientPlayerEntity.getX(), mc.player.getY(), abstractClientPlayerEntity.getZ())
                  <= this.maceRadius.getValue() * this.maceRadius.getValue()
               && (abstractClientPlayerEntity.getX() - abstractClientPlayerEntity.lastX) * (mc.player.getX() - abstractClientPlayerEntity.getX())
                     + (abstractClientPlayerEntity.getZ() - abstractClientPlayerEntity.lastZ) * (mc.player.getZ() - abstractClientPlayerEntity.getZ())
                  >= 0.0) {
               return true;
            }
         } else if (abstractClientPlayerEntity != mc.player
            && !FriendStore.isFriend(abstractClientPlayerEntity.getGameProfile().name())
            && abstractClientPlayerEntity.getMainHandStack().getItem() == Items.MACE
            && !(abstractClientPlayerEntity.getY() - mc.player.getY() < 2.0)
            && !(abstractClientPlayerEntity.getY() - abstractClientPlayerEntity.lastY >= -0.1)
            && !(
               mc.player.squaredDistanceTo(abstractClientPlayerEntity.getX(), mc.player.getY(), abstractClientPlayerEntity.getZ())
                  > this.maceRadius.getValue() * this.maceRadius.getValue()
            )
            && !(
               (abstractClientPlayerEntity.getX() - abstractClientPlayerEntity.lastX) * (mc.player.getX() - abstractClientPlayerEntity.getX())
                     + (abstractClientPlayerEntity.getZ() - abstractClientPlayerEntity.lastZ) * (mc.player.getZ() - abstractClientPlayerEntity.getZ())
                  < 0.0
            )) {
            double local6136_3 = this.playerPeakFallY.getOrDefault(abstractClientPlayerEntity.getId(), abstractClientPlayerEntity.getY())
               - (mc.player.getY() + mc.player.getHeight());
            if (!(local6136_3 <= 1.5) && this.calculatePredictedMaceDamage(abstractClientPlayerEntity, (float)local6136_3) > 2.5F) {
               return true;
            }
         }
      }

      return false;
   }
}
