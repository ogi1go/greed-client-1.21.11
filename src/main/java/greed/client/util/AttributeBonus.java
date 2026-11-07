package greed.client.util;

import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.registry.entry.RegistryEntry;

public record AttributeBonus(RegistryEntry<EntityAttribute> attribute, Operation operation, double amount) {
   public static AttributeBonus of(RegistryEntry<EntityAttribute> local944_0, double local944_1, Operation newOperation) {
      return new AttributeBonus(local944_0, newOperation, local944_1);
   }
}
