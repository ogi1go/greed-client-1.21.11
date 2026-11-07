package greed.mixin;

import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerInteractEntityC2SPacket.class)
public interface PlayerInteractEntityC2SPacketMixin {
   @Accessor("playerSneaking")
   @Mutable
   void setUsingSecondaryAction(boolean var1);

   @Accessor("entityId")
   int entityId();
}
