package greed.mixin;

import greed.client.module.combat.NoServerPackModule;

import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket.Status;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonNetworkHandler.class)
public abstract class ClientCommonNetworkHandlerMixin2 {
   @Shadow
   @Final
   protected ClientConnection connection;

   @Inject(method = "onResourcePackSend(Lnet/minecraft/network/packet/s2c/common/ResourcePackSendS2CPacket;)V", at = @At("HEAD"), cancellable = true)
   private void skipServerPackDownload(ResourcePackSendS2CPacket resourcePackSendS2CPacket, CallbackInfo callbackInfo) {
      if (NoServerPackModule.getInstance().isEnabled()) {
         this.connection.send(new ResourcePackStatusC2SPacket(resourcePackSendS2CPacket.id(), Status.ACCEPTED));
         this.connection.send(new ResourcePackStatusC2SPacket(resourcePackSendS2CPacket.id(), Status.SUCCESSFULLY_LOADED));
         callbackInfo.cancel();
      }
   }
}
