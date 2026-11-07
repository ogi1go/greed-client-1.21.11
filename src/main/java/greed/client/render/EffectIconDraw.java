package greed.client.render;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;

public record EffectIconDraw(RegistryEntry<StatusEffect> effect, float x, float y, float size, int color) {
   public static final int DEFAULT_COLOR = -1;

   public EffectIconDraw(StatusEffectInstance statusEffectInstance, float local528_2, float local528_3, float local528_4, int local528_5) {
      this(statusEffectInstance == null ? null : statusEffectInstance.getEffectType(), local528_2, local528_3, local528_4, local528_5);
   }

   public EffectIconDraw(RegistryEntry<StatusEffect> local530_1, float local530_2, float local530_3, float local530_4) {
      this(local530_1, local530_2, local530_3, local530_4, -1);
   }

   public EffectIconDraw(StatusEffectInstance statusEffectInstance, float local534_2, float local534_3, float local534_4) {
      this(statusEffectInstance == null ? null : statusEffectInstance.getEffectType(), local534_2, local534_3, local534_4, -1);
   }

   public boolean visible() {
      return this.effect != null && this.effect.hasKeyAndValue() && this.size > 0.0F && (this.color >>> 24 & 0xFF) > 0;
   }
}
