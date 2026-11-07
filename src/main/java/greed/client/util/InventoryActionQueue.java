package greed.client.util;

import greed.client.module.movement.MovementPauseController;

import greed.client.event.EventBus;
import greed.client.event.EventListener;
import greed.client.MinecraftAccess;
import greed.client.event.PlayerTickEvent;

import java.util.ArrayList;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.BundleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.BundleItemSelectedC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.slot.SlotActionType;

public class InventoryActionQueue implements MinecraftAccess {
   private static InventoryActionQueue inventoryActionQueue;
   private final ArrayList<InventoryActionQueue.SubContext> list = new ArrayList<>();
   private boolean flag;
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (!this.list.isEmpty() && mc.player != null && mc.interactionManager != null) {
         InventoryActionQueue.SubContext local4148_2 = this.list.get(0);
         if (MovementPauseController.getInstance().getRemainingTicks() < local4148_2.intVal3) {
            this.flag = false;
            int local4148_3 = !local4148_2.itemStack2.isEmpty()
                  && !ItemStack.areItemsAndComponentsEqual(mc.player.getInventory().getStack(local4148_2.intVal), local4148_2.itemStack2)
               ? InventoryUtils.findSlotMatching(itemStack -> ItemStack.areItemsAndComponentsEqual(itemStack, local4148_2.itemStack2), 0, 35)
               : local4148_2.intVal;
            if (local4148_3 != -1 && (local4148_3 != local4148_2.intVal2 || local4148_2.flag2)) {
               int local4148_4 = this.computeInt2(local4148_3);
               int local4148_5 = local4148_2.flag2 ? local4148_2.intVal2 : this.computeInt2(local4148_2.intVal2);
               if (local4148_2.intVal4 != -1) {
                  this.processData3(local4148_2, local4148_4, local4148_5);
               } else if (mc.player.playerScreenHandler.getSlot(local4148_4).getStack().get(DataComponentTypes.BUNDLE_CONTENTS) != null) {
                  this.processData7(local4148_4, 1, SlotActionType.PICKUP);
                  this.processData7(local4148_5, 0, SlotActionType.PICKUP);
                  if (!mc.player.playerScreenHandler.getCursorStack().isEmpty()) {
                     this.processData7(local4148_4, 0, SlotActionType.PICKUP);
                  }
               } else {
                  int local4148_6 = this.computeInt(local4148_2.intVal2, local4148_5);
                  if (local4148_6 != -1) {
                     this.processData7(local4148_4, local4148_6, SlotActionType.SWAP);
                  } else {
                     int local4148_7 = this.computeInt(local4148_3, local4148_4);
                     if (local4148_7 != -1) {
                        this.processData7(local4148_5, local4148_7, SlotActionType.SWAP);
                     } else if (local4148_4 != local4148_5) {
                        this.processData7(local4148_4, 0, SlotActionType.SWAP);
                        this.processData7(local4148_5, 0, SlotActionType.SWAP);
                        this.processData7(local4148_4, 0, SlotActionType.SWAP);
                     }
                  }
               }

               mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
               this.processData(local4148_2);
            } else {
               this.processData(local4148_2);
            }
         }
      }
   };

   private void processData(InventoryActionQueue.SubContext local4136_1) {
      this.list.remove(local4136_1);
      if (!this.list.isEmpty()) {
         MovementPauseController.getInstance().pauseForTicks(this.list.get(0).intVal3);
      }
   }

   public static InventoryActionQueue getInstance() {
      if (inventoryActionQueue == null) {
         inventoryActionQueue = new InventoryActionQueue();
         EventBus.getInstance().subscribe(inventoryActionQueue);
      }

      return inventoryActionQueue;
   }

   private void processData2(InventoryActionQueue.SubContext local4138_1) {
      if (local4138_1.intVal != -1 && local4138_1.intVal2 != -1) {
         if (this.list.isEmpty() && local4138_1.intVal3 > 0) {
            MovementPauseController.getInstance().pauseForTicks(local4138_1.intVal3);
         }

         this.list.add(local4138_1);
      }
   }

   private InventoryActionQueue() {
   }

   private void processData3(InventoryActionQueue.SubContext local4140_1, int local4140_2, int local4140_3) {
      int local4140_4 = this.computeInt2(local4140_1.intVal4);
      int local4140_5 = this.computeInt(local4140_1.intVal2, local4140_3);
      if (local4140_1.intVal5 == -1) {
         this.processData7(local4140_4, local4140_5, SlotActionType.SWAP);
      } else if (mc.player.getInventory().getStack(local4140_1.intVal4).isEmpty()
         && mc.player.playerScreenHandler.getCursorStack().isEmpty()) {
         if (BundleItem.getSelectedStackIndex(mc.player.playerScreenHandler.getSlot(local4140_2).getStack()) != local4140_1.intVal5) {
            mc.player.playerScreenHandler.selectBundleStack(local4140_2, local4140_1.intVal5);
            mc.player.networkHandler.sendPacket(new BundleItemSelectedC2SPacket(local4140_2, local4140_1.intVal5));
         }

         this.processData7(local4140_2, 1, SlotActionType.PICKUP);
         this.processData7(local4140_4, 0, SlotActionType.PICKUP);
         this.processData7(local4140_4, local4140_5, SlotActionType.SWAP);
      }
   }

   public void processData4(int local4141_1, int local4141_2, int local4141_3, int local4141_4, int local4141_5) {
      if (local4141_3 != -1 && local4141_4 != -1) {
         this.processData2(new InventoryActionQueue.SubContext(local4141_1, local4141_2, local4141_3, local4141_4, local4141_5));
      }
   }

   public void processData5(int local4142_1, int local4142_2, int local4142_3) {
      this.processData2(
         new InventoryActionQueue.SubContext(
            local4142_1,
            local4142_2,
            local4142_3,
            false,
            mc.player != null && local4142_1 >= 0 && local4142_1 <= 35
               ? mc.player.getInventory().getStack(local4142_1).copy()
               : ItemStack.EMPTY
         )
      );
   }

   private int computeInt(int local4143_1, int local4143_2) {
      if (local4143_1 == 40 || local4143_1 == 45 || local4143_2 == 45) {
         return 40;
      } else {
         return local4143_2 >= 36 && local4143_2 <= 44 ? local4143_2 - 36 : -1;
      }
   }

   public void queueSlotSwap(int local4144_1, int local4144_2, int local4144_3) {
      this.processData2(new InventoryActionQueue.SubContext(local4144_1, local4144_2, local4144_3, false, ItemStack.EMPTY));
   }

   private int computeInt2(int local4145_1) {
      return local4145_1 >= 0 && local4145_1 <= 8 ? local4145_1 + 36 : local4145_1;
   }

   public void processData6(int local4146_1, int local4146_2, int local4146_3, int local4146_4) {
      if (local4146_3 != -1) {
         this.processData2(new InventoryActionQueue.SubContext(local4146_1, local4146_2, local4146_3, -1, local4146_4));
      }
   }

   public boolean hasPendingActions() {
      return !this.list.isEmpty();
   }

   private void processData7(int local4149_1, int local4149_2, SlotActionType slotActionType) {
      if (local4149_1 != -1 && local4149_2 != -1) {
         if (this.flag) {
            mc.player.playerScreenHandler.nextRevision();
         }

         this.flag = true;
         mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, local4149_1, local4149_2, slotActionType, mc.player);
      }
   }

   public void processData8(int local4150_1, int local4150_2, int local4150_3) {
      if (mc.player != null && mc.player.currentScreenHandler == mc.player.playerScreenHandler) {
         this.processData2(new InventoryActionQueue.SubContext(local4150_1, local4150_2, local4150_3, true, ItemStack.EMPTY));
      }
   }

   static final class SubContext {
      private final int intVal;
      private final int intVal2;
      private final int intVal3;
      private final boolean flag2;
      private final ItemStack itemStack2;
      private final int intVal4;
      private final int intVal5;

      private SubContext(int local4151_1, int local4151_2, int local4151_3, boolean local4151_4, ItemStack itemStack) {
         this.intVal = local4151_1;
         this.intVal2 = local4151_2;
         this.intVal3 = local4151_3;
         this.flag2 = local4151_4;
         this.itemStack2 = itemStack;
         this.intVal4 = -1;
         this.intVal5 = -1;
      }

      private SubContext(int local4152_1, int local4152_2, int local4152_3, int local4152_4, int local4152_5) {
         this.intVal = local4152_1;
         this.intVal2 = local4152_2;
         this.intVal3 = local4152_5;
         this.flag2 = false;
         this.itemStack2 = ItemStack.EMPTY;
         this.intVal4 = local4152_3;
         this.intVal5 = local4152_4;
      }
   }
}
