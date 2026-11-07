package greed.client.module.combat;

import net.minecraft.entity.LivingEntity;

public interface AuraRotationStrategy {
   default boolean isRotating(LivingEntity livingEntity, float range) {
      return false;
   }

   void applyRotation(LivingEntity livingEntity, float tickDelta);

   void update(LivingEntity livingEntity, boolean canAttack, float range);

   void reset();
}
