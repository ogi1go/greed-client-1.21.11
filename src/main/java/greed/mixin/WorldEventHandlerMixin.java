package greed.mixin;

import greed.client.event.EventBus;
import greed.client.util.ServerItemType;
import greed.client.util.PlayerItemCooldowns;
import greed.client.ui.hud.PotionsHud;
import greed.client.event.WorldSoundEvent;

import net.minecraft.client.world.WorldEventHandler;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldEventHandler.class)
public class WorldEventHandlerMixin {
   @Inject(method = {"processWorldEvent(ILnet/minecraft/util/math/BlockPos;I)V", "processGlobalEvent(ILnet/minecraft/util/math/BlockPos;I)V"}, at = @At("HEAD"))
   private void firePotionSplash(int local8253_1, BlockPos blockPos, int local8253_3, CallbackInfo callbackInfo) {
      if (local8253_1 == 2002 || local8253_1 == 2007) {
         ServerItemType serverItemType = ServerItemType.getServerItemType3(blockPos);
         if (serverItemType == null) {
            serverItemType = ServerItemType.getServerItemType2(local8253_3);
         }

         if (serverItemType != null) {
            PotionsHud.processData2(serverItemType, blockPos);
            PlayerItemCooldowns.processData(serverItemType, blockPos);
         }

         if (EventBus.getInstance().hasListeners(WorldSoundEvent.class)) {
            EventBus.getInstance().post(new WorldSoundEvent(local8253_3, blockPos));
         }
      }
   }
}
