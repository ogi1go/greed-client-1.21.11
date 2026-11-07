package greed.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MinecraftClient.class)
public interface MinecraftClientMixin6 {
   @Invoker("doAttack")
   boolean invokeStartAttack();

   @Invoker("doItemUse")
   void invokeStartUseItem();

   @Invoker("handleBlockBreaking")
   void invokeContinueAttack(boolean var1);
}
