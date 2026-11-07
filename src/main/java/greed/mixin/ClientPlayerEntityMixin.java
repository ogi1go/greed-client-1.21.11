package greed.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPlayerEntity.class)
public interface ClientPlayerEntityMixin {
   @Accessor("lastSprinting")
   boolean serverSprintState();

   @Accessor("lastSprinting")
   @Mutable
   void setServerSprintState(boolean var1);
}
