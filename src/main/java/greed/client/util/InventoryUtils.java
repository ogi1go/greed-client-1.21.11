package greed.client.util;

import greed.client.MinecraftAccess;

import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import java.util.function.Predicate;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.sync.ItemStackHash;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.GameMode;

public class InventoryUtils implements MinecraftAccess {
   public static void swapSlotWithHotbar(int local684_0, int local684_1) {
      if (mc.player != null && mc.interactionManager != null && local684_0 != -1 && local684_1 >= 0 && local684_1 <= 8) {
         mc.interactionManager
            .clickSlot(mc.player.currentScreenHandler.syncId, getScreenSlotId(local684_0), local684_1, SlotActionType.SWAP, mc.player);
      }
   }

   public static int getScreenSlotId(int local685_0) {
      if (mc.player == null) {
         return local685_0;
      }

      for (Slot slot : mc.player.currentScreenHandler.slots) {
         if (slot.inventory == mc.player.getInventory() && slot.getIndex() == local685_0) {
            return slot.id;
         }
      }

      return -1;
   }

   public static boolean extractItemFromBundle(Item item) {
      if (mc.player != null && mc.currentScreen == null) {
         for (int index = 0; index < 36; index++) {
            BundleContentsComponent bundleContentsComponent = mc.player.getInventory().getStack(index).get(DataComponentTypes.BUNDLE_CONTENTS);
            if (bundleContentsComponent != null && !bundleContentsComponent.isEmpty()) {
               int local686_3 = -1;
               int local686_4 = 0;

               for (ItemStack itemStack : bundleContentsComponent.iterate()) {
                  if (itemStack.getItem() == item) {
                     local686_3 = local686_4;
                     break;
                  }

                  local686_4++;
               }

               if (local686_3 != -1) {
                  for (int index2 = 0; index2 <= local686_3 && findEmptySlot(0, 35) != -1; index2++) {
                     sendSlotClickPacket(getScreenSlotId(index), 1, SlotActionType.PICKUP, false);
                     sendSlotClickPacket(getScreenSlotId(findEmptySlot(0, 35)), 0, SlotActionType.PICKUP, false);
                  }

                  return true;
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static int getItemCount(Item item) {
      if (mc.player == null) {
         return 0;
      }

      int local687_1 = 0;

      for (int index = 0; index < mc.player.getInventory().size(); index++) {
         if (mc.player.getInventory().getStack(index).isOf(item)) {
            local687_1 += mc.player.getInventory().getStack(index).getCount();
         }
      }

      return local687_1;
   }

   public static void swapSlotWithHand(int local688_0, Hand hand) {
      if (mc.player != null && mc.interactionManager != null && local688_0 != -1) {
         mc.interactionManager
            .clickSlot(
               mc.player.currentScreenHandler.syncId,
               getScreenSlotId(local688_0),
               hand == Hand.OFF_HAND ? 40 : getSelectedSlot(),
               SlotActionType.SWAP,
               mc.player
            );
      }
   }

   public static void quickSwapSlots(int local689_0, int local689_1, boolean local689_2) {
      if (local689_0 != local689_1 && local689_0 != -1 && local689_1 != -1) {
         sendSlotClickPacket(getScreenSlotId(local689_0), 0, SlotActionType.PICKUP, local689_2);
         sendSlotClickPacket(getScreenSlotId(local689_1), 0, SlotActionType.PICKUP, local689_2);
         sendSlotClickPacket(getScreenSlotId(local689_0), 0, SlotActionType.PICKUP, local689_2);
      }
   }

   public static void useItemFromSlot(int local690_0) {
      useItemInSlotWithHand(local690_0, Hand.MAIN_HAND);
   }

   public static int findHotbarSlot(Item item) {
      return findSlotMatching(itemStack -> itemStack.getItem() == item, 0, 8);
   }

   public static void pickupAll() {
      sendSlotClickPacket(0, 0, SlotActionType.PICKUP_ALL, true);
   }

   public static ActionResult useItem(Hand hand) {
      if (mc.player != null && mc.interactionManager != null) {
         return mc.interactionManager.getCurrentGameMode() == GameMode.SPECTATOR
            ? ActionResult.PASS
            : mc.interactionManager.interactItem(mc.player, hand);
      } else {
         return ActionResult.PASS;
      }
   }

   public static void useItemInSlotWithHand(int local695_0, Hand hand) {
      if (mc.player != null && mc.interactionManager != null && local695_0 != -1) {
         swapSlotWithHand(local695_0, hand);
         useItem(hand);
         swapSlotWithHand(local695_0, hand);
      }
   }

   public static void clickSlot(Slot slot, int local696_1, SlotActionType slotActionType, boolean local696_3) {
      if (slot != null) {
         sendSlotClickPacket(slot.id, local696_1, slotActionType, local696_3);
      }
   }

   public static void swapSlots(int local697_0, int local697_1, boolean local697_2) {
      if (local697_0 != -1 && local697_1 >= 0 && local697_1 <= 8) {
         sendSlotClickPacket(local697_0, local697_1, SlotActionType.SWAP, local697_2);
      }
   }

   public static int findEmptySlot(int local698_0, int local698_1) {
      return findSlotMatching(itemStack -> itemStack.isEmpty(), local698_0, local698_1);
   }

   public static void sendSlotClickPacket(int local699_0, int local699_1, SlotActionType slotActionType, boolean local699_3) {
      if (mc.player != null) {
         executeSlotClick(mc.player.currentScreenHandler.syncId, local699_0, local699_1, slotActionType, local699_3);
      }
   }

   public static void closeHandledScreen(boolean local701_0) {
      if (mc.player != null) {
         if (local701_0) {
            if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
               return;
            }

            mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
         } else {
            mc.player.closeHandledScreen();
         }
      }
   }

   public static void executeSlotClick(int local702_0, int local702_1, int local702_2, SlotActionType slotActionType, boolean local702_4) {
      if (mc.player != null && mc.interactionManager != null && local702_1 != -1) {
         if (local702_4) {
            mc.player
               .networkHandler
               .sendPacket(
                  new ClickSlotC2SPacket(
                     local702_0,
                     mc.player.currentScreenHandler.getRevision(),
                     (short)local702_1,
                     (byte)local702_2,
                     slotActionType,
                     Int2ObjectMaps.emptyMap(),
                     ItemStackHash.EMPTY
                  )
               );
         } else {
            mc.interactionManager.clickSlot(local702_0, local702_1, local702_2, slotActionType, mc.player);
         }
      }
   }

   public static int findMainInventorySlot(Item item) {
      return findSlotMatching(itemStack -> itemStack.getItem() == item, 9, 35);
   }

   public static void setSelectedSlot(int local706_0, boolean local706_1) {
      if (mc.player != null && local706_0 >= 0 && local706_0 <= 8) {
         if (local706_1) {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(local706_0));
         } else {
            mc.player.getInventory().setSelectedSlot(local706_0);
         }
      }
   }

   public static int getSelectedSlot() {
      return mc.player == null ? -1 : mc.player.getInventory().getSelectedSlot();
   }

   public static int findOffhandSlot(Predicate<ItemStack> local708_0) {
      return findSlotMatching(local708_0, 0, 35);
   }

   public static int findSlotMatching(Predicate<ItemStack> local709_0, int local709_1, int local709_2) {
      if (mc.player == null) {
         return -1;
      }

      for (int index = local709_2; index >= local709_1; index--) {
         if (local709_0.test(mc.player.getInventory().getStack(index))) {
            return index;
         }
      }

      return -1;
   }

   public static int findInventorySlot(Item item) {
      return findSlotMatching(itemStack -> itemStack.getItem() == item, 0, 35);
   }
}
