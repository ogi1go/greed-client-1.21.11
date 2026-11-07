package greed.client.util;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;

public record StatusEffectRequirement(RegistryEntry<StatusEffect> effect, int durationTicks, int amplifier) {
   public static StatusEffectRequirement of(RegistryEntry<StatusEffect> local3181_0, int local3181_1, int local3181_2) {
      return new StatusEffectRequirement(local3181_0, local3181_1 * 20, local3181_2);
   }

   public static StatusEffectRequirement instant(RegistryEntry<StatusEffect> local3185_0, int local3185_1) {
      return new StatusEffectRequirement(local3185_0, 1, local3185_1);
   }

   public StatusEffectInstance effectInstance() {
      return new StatusEffectInstance(this.effect, this.durationTicks, this.amplifier);
   }
}
