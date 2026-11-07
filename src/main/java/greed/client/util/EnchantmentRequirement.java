package greed.client.util;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKey;

public record EnchantmentRequirement(RegistryKey<Enchantment> enchantment, int level) {
   public static EnchantmentRequirement of(RegistryKey<Enchantment> local2594_0, int local2594_1) {
      return new EnchantmentRequirement(local2594_0, local2594_1);
   }
}
