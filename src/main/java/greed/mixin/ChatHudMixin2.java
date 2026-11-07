package greed.mixin;

import greed.client.module.misc.AutoAcceptModule;
import greed.client.module.misc.AutoMineModule;
import greed.client.module.misc.AutoVillagerModule;
import greed.client.module.misc.AutoWardenModule;
import greed.client.module.render.SmoothFeatureModule;
import greed.client.module.misc.StreamerModModule;
import greed.client.module.misc.YTMODEModule;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin2 {
   @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), cancellable = true)
   private void muteBaritoneChat(Text text, CallbackInfo callbackInfo) {
      if (AutoMineModule.getInstance().isEnabled() && AutoMineModule.getInstance().matchesCriteria5(text.getString())) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), cancellable = true)
   private void muteVillagerBaritoneChat(Text text, CallbackInfo callbackInfo) {
      if (AutoVillagerModule.checkState3() && AutoMineModule.getInstance().matchesCriteria5(text.getString())) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), cancellable = true)
   private void muteWardenBaritoneChat(Text text, CallbackInfo callbackInfo) {
      if (AutoWardenModule.checkState() && AutoMineModule.getInstance().matchesCriteria5(text.getString())) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), cancellable = true)
   private void muteServerGreetingChat(Text text, CallbackInfo callbackInfo) {
      if (YTMODEModule.getInstance().isEnabled() && YTMODEModule.getInstance().matchesCriteria2(text.getString())
         || StreamerModModule.getInstance().isEnabled() && StreamerModModule.getInstance().matchesCriteria2(text.getString())) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"))
   private void acceptChatInvites(Text text, CallbackInfo callbackInfo) {
      AutoAcceptModule.getInstance().processData(text.getString());
   }

   @Inject(
      method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void stackDuplicateChatSpam(Text text, MessageSignatureData messageSignatureData, MessageIndicator messageIndicator, CallbackInfo callbackInfo) {
      if (SmoothFeatureModule.getInstance().matchesCriteria((ChatHud)(Object)this, text)) {
         callbackInfo.cancel();
      }
   }

   @Inject(
      method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
      at = @At("TAIL")
   )
   private void slideChatOnNewLine(Text text, MessageSignatureData messageSignatureData, MessageIndicator messageIndicator, CallbackInfo callbackInfo) {
      SmoothFeatureModule.getInstance().updateState();
   }

   @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V", at = @At("HEAD"))
   private void beginChatSlideRender(
      DrawContext drawContext,
      TextRenderer textRenderer,
      int local8323_3,
      int local8323_4,
      int local8323_5,
      boolean local8323_6,
      boolean local8323_7,
      CallbackInfo callbackInfo
   ) {
      SmoothFeatureModule.getInstance().renderElement4((ChatHud)(Object)this, drawContext);
   }

   @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V", at = @At("RETURN"))
   private void endChatSlideRender(
      DrawContext drawContext,
      TextRenderer textRenderer,
      int local8324_3,
      int local8324_4,
      int local8324_5,
      boolean local8324_6,
      boolean local8324_7,
      CallbackInfo callbackInfo
   ) {
      SmoothFeatureModule.getInstance().renderElement3(drawContext);
   }
}
