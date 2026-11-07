package greed.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityMixin {
   @Accessor("jumpingCooldown")
   void setNoJumpDelay(int var1);

   @Accessor("jumpingCooldown")
   int getNoJumpDelay();

   @Accessor("jumping")
   boolean getJumping();

   @Invoker("getJumpVelocity")
   float invokeGetJumpPower();
}
