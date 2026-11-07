package greed.client.module.player;

import greed.client.setting.BooleanSetting;
import greed.client.event.EventListener;
import greed.client.setting.KeybindSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.MovementInputEvent;
import greed.client.event.PacketEvent;
import greed.client.event.PlayerTickEvent;
import greed.client.util.InventoryUtils;
import greed.client.util.InventoryActionQueue;
import greed.client.ui.hud.NotificationHud;
import greed.client.util.InventorySlotManager;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.AttributeModifiersComponent.Entry;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(name = "ElytraHelper", description = "Выполняет действия с элитрой по нажатию назначенной клавиши.", category = ModuleCategory.PLAYER)
public final class ElytraHelperModule extends Module {
   private static ElytraHelperModule elytraHelperModule;
   public KeybindSetting toggleKey;
   public KeybindSetting fireworkKey;
   public BooleanSetting swapOnUse;
   public BooleanSetting autoStartAfterSwap;
   public BooleanSetting useFlyOnSwap;
   public BooleanSetting autoFirework;
   private boolean flag;
   private boolean flag2;
   private int intVal;
   private boolean flag3;
   private boolean flag4;
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (mc.player != null && mc.interactionManager != null) {
         if (mc.currentScreen != null) {
            this.flag3 = this.toggleKey.getBinding().isPressed(mc.getWindow().getHandle());
            this.flag4 = this.fireworkKey.getBinding().isPressed(mc.getWindow().getHandle());
         } else {
            if (this.toggleKey.getBinding().isPressed(mc.getWindow().getHandle()) && !this.flag3) {
               this.updateState();
            }

            this.flag3 = this.toggleKey.getBinding().isPressed(mc.getWindow().getHandle());
            if (this.fireworkKey.getBinding().isPressed(mc.getWindow().getHandle()) && !this.flag4) {
               this.updateState2();
            }

            this.flag4 = this.fireworkKey.getBinding().isPressed(mc.getWindow().getHandle());
         }
      }
   };
   private final EventListener<MovementInputEvent> movementInputListener = event -> {
      if (mc.player != null) {
         if (mc.player.age < 5) {
            this.flag = false;
         } else if (this.flag && this.checkState()) {
            this.onMovementInput(event);
         }
      }
   };
   public final EventListener<PacketEvent> packetListener = packetEvent -> {
      if (packetEvent.isIncoming()
         && packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket
         && this.flag
         && gameMessageS2CPacket.content().getString().contains("Эту команду могут писать только донатеры выше рангом")) {
         this.flag2 = true;
      }
   };

   private boolean matchesCriteria(int local1336_1) {
      return this.intVal + local1336_1 < mc.player.age;
   }

   private void onMovementInput(MovementInputEvent event) {
      boolean local1337_2 = mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA;
      if (!mc.player.isGliding() && local1337_2) {
         event.setJumping(mc.player.age % 2 == 0);
         this.intVal = mc.player.age;
         if (!this.autoFirework.isChecked()) {
            this.flag = false;
         }
      }

      if (mc.player.isGliding()
         && this.autoFirework.isChecked()
         && !mc.player.isTouchingWater()
         && !mc.player.isOnGround()
         && this.matchesCriteria(2)) {
         this.updateState2();
         this.flag = false;
      }

      if (this.matchesCriteria(10)) {
         this.flag = false;
      }
   }

   private boolean checkState() {
      if (this.useFlyOnSwap.isChecked() && !this.flag2) {
         if (this.intVal + 1 == mc.player.age) {
            mc.player.networkHandler.sendChatCommand("fly");
         }

         if (mc.player.getAbilities().allowFlying && this.intVal <= mc.player.age && mc.player.isOnGround()) {
            double x = mc.player.getX();
            double local1338_3 = mc.player.getY() + 0.19999997F;
            double z = mc.player.getZ();
            mc.player
               .networkHandler
               .sendPacket(new PositionAndOnGround(x, local1338_3, z, false, mc.player.horizontalCollision));
            mc.player.setPosition(x, local1338_3, z);
            this.intVal = mc.player.age + 9;
         }

         if (mc.player.getAbilities().allowFlying && !mc.player.isOnGround()) {
            mc.player.getAbilities().flying = true;
            mc.player.setVelocity(Vec3d.ZERO);
            mc.player.sendAbilitiesUpdate();
            this.flag = false;
         }

         if (!this.matchesCriteria(15) && (!this.matchesCriteria(3) || mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA)) {
            return false;
         }

         this.flag = false;
         return false;
      } else {
         return true;
      }
   }

   private void updateState() {
      if (!InventoryActionQueue.getInstance().hasPendingActions() && !InventorySlotManager.getInstance().isLocked()) {
         boolean local1340_1 = mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA;
         int local1340_2 = local1340_1 ? this.computeInt() : InventoryUtils.findInventorySlot(Items.ELYTRA);
         if (local1340_2 == -1) {
            NotificationHud.notifyWithText(
               this.category.getIcon(),
               (local1340_1 ? Items.NETHERITE_CHESTPLATE : Items.ELYTRA).getDefaultStack().getName().getString(),
               Text.literal("нет в инвентаре").setStyle(Style.EMPTY.withColor(Formatting.RED)),
               true
            );
         } else {
            if (this.swapOnUse.isChecked()) {
               InventorySlotManager.getInstance().processData3(mc.player.getInventory().getStack(local1340_2).copy(), true);
            } else {
               InventoryActionQueue.getInstance().processData8(local1340_2, 6, 1);
            }

            if (this.autoStartAfterSwap.isChecked() && !local1340_1) {
               this.intVal = mc.player.age;
               this.flag = true;
            }
         }
      }
   }

   private void updateState2() {
      if (mc.player.isGliding()) {
         if (!InventorySlotManager.getInstance().matchesCriteria2(Items.FIREWORK_ROCKET.getDefaultStack())) {
            NotificationHud.notifyWithText(
               this.category.getIcon(),
               Items.FIREWORK_ROCKET.getDefaultStack().getName().getString(),
               Text.literal("нет в инвентаре").setStyle(Style.EMPTY.withColor(Formatting.RED)),
               true
            );
         } else {
            InventorySlotManager.getInstance().processData3(Items.FIREWORK_ROCKET.getDefaultStack(), true);
         }
      }
   }

   public static ElytraHelperModule getInstance() {
      if (elytraHelperModule == null) {
         elytraHelperModule = new ElytraHelperModule();
      }

      return elytraHelperModule;
   }

   @Override
   protected void onDisable() {
      this.flag = false;
   }

   private int computeInt() {
      RegistryEntry<Enchantment> orThrow = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION);
      int local1347_2 = -1;
      double local1347_3 = 0.0;

      for (int index = 0; index < 36; index++) {
         ItemStack stack = mc.player.getInventory().getStack(index);
         if (!stack.isEmpty()
            && stack.getItem() != Items.ELYTRA
            && stack.get(DataComponentTypes.EQUIPPABLE) != null
            && stack.get(DataComponentTypes.EQUIPPABLE).slot() == EquipmentSlot.CHEST) {
            double level = EnchantmentHelper.getLevel(orThrow, stack);
            AttributeModifiersComponent attributeModifiersComponent = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
            if (attributeModifiersComponent != null) {
               for (Entry entry : attributeModifiersComponent.modifiers()) {
                  if (entry.attribute() == EntityAttributes.ARMOR || entry.attribute() == EntityAttributes.ARMOR_TOUGHNESS) {
                     level += entry.modifier().value();
                  }
               }
            }

            if (level > local1347_3) {
               local1347_3 = level;
               local1347_2 = index;
            }
         }
      }

      return local1347_2;
   }

   private ElytraHelperModule() {
      this.toggleKey = this.addSetting(new KeybindSetting("Кнопка переключения", "Надевает элитры вместо нагрудника и обратно."));
      this.fireworkKey = this.addSetting(new KeybindSetting("Кнопка фейерверка", "Использует фейерверк в полёте по нажатию."));
      this.swapOnUse = this.addSetting(
         new BooleanSetting(
            "Свап использованием", "Меняет нагрудник правым кликом по надеваемому предмету (1.21.2+) вместо трёх кликов по слотам брони.", true
         )
      );
      this.autoStartAfterSwap = this.addSetting(new BooleanSetting("Автостарт после свапа", "Сам взлетает после свапа на элитры.", false));
      this.useFlyOnSwap = this.addSetting(
         new BooleanSetting("Использовать /fly при свапе", "Взлетает через /fly, пока сервер разрешает.", false)
            .withVisibility(() -> this.autoStartAfterSwap.isChecked())
      );
      this.autoFirework = this.addSetting(
         new BooleanSetting("Автофейерверк", "Запускает фейерверк, как только полёт начался.", false).withVisibility(() -> this.autoStartAfterSwap.isChecked())
      );
   }
}
