package greed.client.render;

import greed.client.util.ItemDrawOptions;

import net.minecraft.item.ItemStack;

public record ItemDrawData(ItemStack stack, float x, float y, float size, ItemDrawOptions options, int seed) {
   public ItemDrawData(ItemStack itemStack, float local4520_2, float local4520_3, float local4520_4) {
      this(itemStack, local4520_2, local4520_3, local4520_4, ItemDrawOptions.defaults(), 0);
   }

   public ItemDrawData {
      stack = stack == null ? ItemStack.EMPTY : stack;
      options = options == null ? ItemDrawOptions.defaults() : options;
   }

   public boolean visible() {
      return !this.stack.isEmpty() && this.size > 0.0F && this.options.alpha() > 0.0F && this.options.color() >>> 24 != 0;
   }
}
