package greed.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftClient.class)
public interface MinecraftClientMixin3 {
   @Accessor("itemUseCooldown")
   void setRightClickDelay(int var1);

   @Accessor("attackCooldown")
   void setMissTime(int var1);
}
