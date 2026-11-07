package greed.client.util;

import greed.mixin.ClientPlayerInteractionManagerMixin2;
import greed.client.event.EventBus;
import greed.client.event.EventListener;
import greed.client.MinecraftAccess;
import greed.client.event.PlayerTickEvent;
import greed.client.module.player.ServerHelperModule;

import java.util.ArrayList;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;

public class InventorySlotManager implements MinecraftAccess {
   private static InventorySlotManager inventorySlotManager;
   private final ArrayList<InventorySlotManager.SubContext> list = new ArrayList<>();
   private int intVal = -1;
   private ItemStack itemStack4 = ItemStack.EMPTY;
   private int intVal2;
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (!this.list.isEmpty() && mc.player != null && mc.interactionManager != null) {
         InventorySlotManager.SubContext local6992_2 = this.list.get(0);
         int local6992_3 = this.computeInt2(local6992_2.itemStack5);
         int local6992_4 = this.computeInt3(local6992_2.itemStack5);
         if (local6992_2.intVal9 != -1
            || (local6992_3 != -1 || local6992_4 != -1) && !mc.player.getItemCooldownManager().isCoolingDown(local6992_2.itemStack5)) {
            if (local6992_2.intVal9 < 0 || !InventoryActionQueue.getInstance().hasPendingActions()) {
               local6992_2.intVal9++;
               if (local6992_2.intVal9 == 0) {
                  local6992_2.intVal3 = InventoryUtils.getSelectedSlot();
                  if (local6992_3 != -1) {
                     local6992_2.intVal4 = local6992_3;
                     if (local6992_3 != local6992_2.intVal3) {
                        InventoryUtils.setSelectedSlot(local6992_3, false);
                     }
                  } else if (local6992_4 != -1) {
                     int local6992_5 = this.computeInt4(
                        mc.player.getInventory().getStack(local6992_4), local6992_2.itemStack5, this.matchesCriteria3(local6992_2.itemStack5)
                     );
                     if (local6992_5 == -1) {
                        local6992_2.intVal4 = local6992_4;
                        if (!local6992_2.__) {
                           InventoryActionQueue.getInstance().queueSlotSwap(local6992_4, local6992_2.intVal3, 1);
                        } else {
                           local6992_2.intVal6 = this.computeInt(local6992_2.intVal3, local6992_4, -1);
                           this.processData2(local6992_2.intVal6);
                           InventoryActionQueue.getInstance().queueSlotSwap(local6992_4, local6992_2.intVal6, 1);
                        }
                     } else {
                        local6992_2.intVal5 = InventoryUtils.findEmptySlot(9, 35);
                        if (local6992_2.intVal5 == -1) {
                           local6992_2.intVal5 = InventoryUtils.findEmptySlot(0, 8);
                        }

                        if (local6992_2.intVal5 == -1) {
                           this.list.remove(local6992_2);
                        } else {
                           local6992_2.intVal7 = local6992_4;
                           if (!local6992_2.__) {
                              if (local6992_4 == local6992_2.intVal3 && InventoryUtils.findEmptySlot(0, 8) != -1) {
                                 local6992_2.intVal8 = local6992_2.intVal3;
                                 local6992_2.intVal3 = InventoryUtils.findEmptySlot(0, 8);
                                 InventoryUtils.setSelectedSlot(local6992_2.intVal3, false);
                              }

                              local6992_2.intVal4 = local6992_2.intVal3;
                              local6992_2.itemStack6 = mc.player.getInventory().getStack(local6992_2.intVal3).copy();
                              InventoryActionQueue.getInstance().processData4(local6992_4, local6992_2.intVal3, local6992_2.intVal5, local6992_5, 1);
                           } else {
                              local6992_2.intVal6 = this.computeInt(local6992_2.intVal3, local6992_4, local6992_2.intVal5);
                              this.processData2(local6992_2.intVal6);
                              local6992_2.intVal4 = local6992_2.intVal6;
                              local6992_2.itemStack6 = mc.player.getInventory().getStack(local6992_2.intVal6).copy();
                              InventoryActionQueue.getInstance().processData4(local6992_4, local6992_2.intVal6, local6992_2.intVal5, local6992_5, 1);
                           }
                        }
                     }
                  }
               } else if (local6992_2.intVal9 == 1) {
                  if (local6992_2.intVal6 != -1) {
                     if (this.matchesCriteria(mc.player.getInventory().getStack(local6992_2.intVal6), local6992_2.itemStack5)
                        && !mc.player.getItemCooldownManager().isCoolingDown(local6992_2.itemStack5)) {
                        InventoryUtils.setSelectedSlot(local6992_2.intVal6, true);
                        ((ClientPlayerInteractionManagerMixin2)mc.interactionManager)
                           .invokeStartPrediction(
                              mc.world,
                              local7000_0 -> new PlayerInteractItemC2SPacket(
                                 Hand.MAIN_HAND, local7000_0, mc.player.getYaw(), mc.player.getPitch()
                              )
                           );
                        InventoryUtils.setSelectedSlot(local6992_2.intVal3, true);
                     }

                     if (local6992_2.intVal7 != -1) {
                        if (!ItemStack.areItemsAndComponentsEqual(mc.player.getInventory().getStack(local6992_2.intVal6), local6992_2.itemStack6)) {
                           InventoryActionQueue.getInstance().processData6(local6992_2.intVal7, local6992_2.intVal6, local6992_2.intVal5, 1);
                        }
                     } else {
                        InventoryActionQueue.getInstance().queueSlotSwap(local6992_2.intVal6, local6992_2.intVal4, 1);
                     }

                     this.list.remove(local6992_2);
                  } else {
                     if (this.matchesCriteria(mc.player.getMainHandStack(), local6992_2.itemStack5)
                        && !mc.player.getItemCooldownManager().isCoolingDown(local6992_2.itemStack5)) {
                        ((ClientPlayerInteractionManagerMixin2)mc.interactionManager)
                           .invokeStartPrediction(
                              mc.world,
                              local6981_0 -> new PlayerInteractItemC2SPacket(
                                 Hand.MAIN_HAND, local6981_0, mc.player.getYaw(), mc.player.getPitch()
                              )
                           );
                     }

                     if (local6992_2.intVal7 != -1) {
                        if (!ItemStack.areItemsAndComponentsEqual(mc.player.getInventory().getStack(local6992_2.intVal3), local6992_2.itemStack6)) {
                           InventoryActionQueue.getInstance().processData6(local6992_2.intVal7, local6992_2.intVal3, local6992_2.intVal5, 1);
                        }

                        if (local6992_2.intVal8 != -1) {
                           InventoryUtils.setSelectedSlot(local6992_2.intVal8, false);
                        }
                     } else if (local6992_2.intVal4 > 8) {
                        InventoryActionQueue.getInstance().queueSlotSwap(local6992_2.intVal3, local6992_2.intVal4, 1);
                     } else if (local6992_2.intVal3 != InventoryUtils.getSelectedSlot()) {
                        InventoryUtils.setSelectedSlot(local6992_2.intVal3, false);
                     }

                     this.list.remove(local6992_2);
                  }
               }
            }
         } else {
            this.list.remove(local6992_2);
         }
      }
   };
   private final EventListener<PlayerTickEvent> playerTickListener2 = playerTickEvent2 -> {
      if (this.intVal != -1) {
         if (this.list.isEmpty() && !InventoryActionQueue.getInstance().hasPendingActions()) {
            if (--this.intVal2 <= 0) {
               this.intVal = -1;
            }
         } else {
            this.intVal2 = 4;
         }
      }
   };

   public void lockSlot(ItemStack itemStack) {
      this.list.add(new InventorySlotManager.SubContext(itemStack, false));
   }

   public static InventorySlotManager getInstance() {
      if (inventorySlotManager == null) {
         inventorySlotManager = new InventorySlotManager();
         EventBus.getInstance().subscribe(inventorySlotManager);
      }

      return inventorySlotManager;
   }

   private int computeInt(int local6985_1, int local6985_2, int local6985_3) {
      if (mc.player == null) {
         return -1;
      }

      for (int index = 8; index >= 0; index--) {
         if (index != local6985_1
            && index != local6985_2
            && index != local6985_3
            && mc.player.getInventory().getStack(index).isEmpty()) {
            return index;
         }
      }

      for (int index2 = 8; index2 >= 0; index2--) {
         if (index2 != local6985_1 && index2 != local6985_2 && index2 != local6985_3) {
            return index2;
         }
      }

      return -1;
   }

   private int computeInt2(ItemStack itemStack) {
      return InventoryUtils.findSlotMatching(itemStack2 -> this.matchesCriteria(itemStack2, itemStack), 0, 8);
   }

   public boolean isLocked() {
      return !this.list.isEmpty();
   }

   public ItemStack getItemStack(int local6989_1, ItemStack itemStack) {
      return this.intVal == local6989_1 ? this.itemStack4 : itemStack;
   }

   private InventorySlotManager() {
   }

   private boolean matchesCriteria(ItemStack itemStack, ItemStack itemStack2) {
      return this.matchesCriteria3(itemStack2) ? ItemStack.areItemsAndComponentsEqual(itemStack, itemStack2) : itemStack.getItem() == itemStack2.getItem();
   }

   public boolean matchesCriteria2(ItemStack itemStack) {
      return this.computeInt2(itemStack) != -1 || this.computeInt3(itemStack) != -1;
   }

   private boolean matchesCriteria3(ItemStack itemStack) {
      return itemStack.getItem() == Items.SPLASH_POTION || itemStack.getItem() == Items.LINGERING_POTION;
   }

   private boolean matchesCriteria4(ItemStack itemStack) {
      if (mc.player != null && mc.interactionManager != null && mc.player.currentScreenHandler == mc.player.playerScreenHandler
         )
       {
         int local6996_2 = this.computeInt2(itemStack);
         if (local6996_2 != -1 && !mc.player.getItemCooldownManager().isCoolingDown(itemStack)) {
            int local6996_3 = InventoryUtils.getSelectedSlot();
            if (local6996_2 != local6996_3) {
               InventoryUtils.setSelectedSlot(local6996_2, true);
            }

            ((ClientPlayerInteractionManagerMixin2)mc.interactionManager)
               .invokeStartPrediction(
                  mc.world,
                  local6991_0 -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, local6991_0, mc.player.getYaw(), mc.player.getPitch())
               );
            if (local6996_2 != local6996_3) {
               InventoryUtils.setSelectedSlot(local6996_3, true);
            }

            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private int computeInt3(ItemStack itemStack) {
      int local6999_2 = InventoryUtils.findSlotMatching(itemStack2 -> ItemStack.areItemsAndComponentsEqual(itemStack2, itemStack), 9, 35);
      return local6999_2 != -1
         ? local6999_2
         : InventoryUtils.findSlotMatching(
            itemStack3 -> this.matchesCriteria(itemStack3, itemStack) || this.computeInt4(itemStack3, itemStack, this.matchesCriteria3(itemStack)) != -1, 0, 35
         );
   }

   public void processData(ItemStack itemStack) {
      this.processData3(itemStack, false);
   }

   public boolean checkState() {
      return ServerHelperModule.getInstance().modeSetting.isSelected("SpookyTime");
   }

   private void processData2(int local7003_1) {
      if (mc.player != null && local7003_1 != -1) {
         this.intVal = local7003_1;
         this.itemStack4 = mc.player.getInventory().getStack(local7003_1).copy();
         this.intVal2 = 4;
      }
   }

   public void processData3(ItemStack itemStack, boolean new__) {
      if (!this.checkState() || !this.matchesCriteria4(itemStack)) {
         this.list.add(new InventorySlotManager.SubContext(itemStack, new__));
      }
   }

   private int computeInt4(ItemStack itemStack, ItemStack itemStack2, boolean local7005_3) {
      if (itemStack.get(DataComponentTypes.BUNDLE_CONTENTS) == null) {
         return -1;
      }

      for (int index = 0; index < itemStack.get(DataComponentTypes.BUNDLE_CONTENTS).size(); index++) {
         if (ItemStack.areItemsAndComponentsEqual(itemStack.get(DataComponentTypes.BUNDLE_CONTENTS).get(index), itemStack2)) {
            return index;
         }
      }

      if (local7005_3) {
         return -1;
      }

      for (int index2 = 0; index2 < itemStack.get(DataComponentTypes.BUNDLE_CONTENTS).size(); index2++) {
         if (ItemStack.areItemsEqual(itemStack.get(DataComponentTypes.BUNDLE_CONTENTS).get(index2), itemStack2)) {
            return index2;
         }
      }

      return -1;
   }

   static final class SubContext {
      private final ItemStack itemStack5;
      private final boolean __;
      private ItemStack itemStack6 = ItemStack.EMPTY;
      private int intVal3;
      private int intVal4;
      private int intVal5 = -1;
      private int intVal6 = -1;
      private int intVal7 = -1;
      private int intVal8 = -1;
      private int intVal9 = -1;

      private SubContext(ItemStack itemStack, boolean initial__) {
         this.itemStack5 = itemStack;
         this.__ = initial__;
      }
   }
}
