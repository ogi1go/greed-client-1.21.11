package greed.client.util;

import net.minecraft.item.ItemStack;

public interface ItemCooldownAccess {
   int remainingCooldownTicks(ItemStack itemStack);

   void wipeAllCooldowns();
}
