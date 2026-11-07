package greed.mixin;

import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.boss.BossBar.Color;
import net.minecraft.entity.boss.BossBar.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net/minecraft/client/gui/hud/BossBarHud$1")
public abstract class BossBarHud_1Mixin {
   @Inject(method = "remove(Ljava/util/UUID;)V", at = @At("HEAD"), cancellable = true)
   private void skipUnknownRemove(UUID uUID, CallbackInfo callbackInfo) {
      if (unknownBar(uUID)) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "updateProgress(Ljava/util/UUID;F)V", at = @At("HEAD"), cancellable = true)
   private void skipUnknownProgress(UUID uUID, float local8169_2, CallbackInfo callbackInfo) {
      if (unknownBar(uUID)) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "updateName(Ljava/util/UUID;Lnet/minecraft/text/Text;)V", at = @At("HEAD"), cancellable = true)
   private void skipUnknownName(UUID uUID, Text text, CallbackInfo callbackInfo) {
      if (unknownBar(uUID)) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "updateStyle(Ljava/util/UUID;Lnet/minecraft/entity/boss/BossBar$Color;Lnet/minecraft/entity/boss/BossBar$Style;)V", at = @At("HEAD"), cancellable = true)
   private void skipUnknownStyle(UUID uUID, Color color, Style style, CallbackInfo callbackInfo) {
      if (unknownBar(uUID)) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "updateProperties(Ljava/util/UUID;ZZZ)V", at = @At("HEAD"), cancellable = true)
   private void skipUnknownProperties(UUID uUID, boolean local8172_2, boolean local8172_3, boolean local8172_4, CallbackInfo callbackInfo) {
      if (unknownBar(uUID)) {
         callbackInfo.cancel();
      }
   }

   private static boolean unknownBar(UUID uUID) {
      return !((BossBarHudMixin)MinecraftClient.getInstance().inGameHud.getBossBarHud()).getEvents().containsKey(uUID);
   }
}
