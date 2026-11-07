package greed.mixin;

import greed.client.module.render.InterfaceModule;
import greed.client.module.misc.UnHookModule;
import greed.client.util.GuiScale;
import greed.client.ui.UiTheme;
import greed.client.render.Render2D;
import ru.dreamix.fabricloader.VMBridge;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SplashOverlay.class)
public abstract class SplashOverlayMixin {
   @Shadow
   @Final
   private MinecraftClient client;
   @Shadow
   @Final
   private boolean reloading;
   @Shadow
   private long reloadCompleteTime;
   @Shadow
   private long reloadStartTime;
   @Unique
   private long greed$shownAtMs;
   @Unique
   private boolean greed$sceneMode;

   @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("HEAD"), cancellable = true)
   private void renderGreedOverlay(DrawContext drawContext, int local8099_2, int local8099_3, float local8099_4, CallbackInfo callbackInfo) {
      if (UnHookModule.isUnhooked()) {
         return;
      }

      long currentReloadStartTime = Util.getMeasuringTimeMs();
      if (this.reloading && this.reloadStartTime == -1L) {
         this.reloadStartTime = currentReloadStartTime;
      }

      if (this.greed$shownAtMs == 0L) {
         this.greed$shownAtMs = System.currentTimeMillis();
         this.greed$sceneMode = UiTheme.flag;
      }

      float local8099_8 = this.reloadCompleteTime > -1L ? (float)(currentReloadStartTime - this.reloadCompleteTime) / 1000.0F : -1.0F;
      float local8099_9 = this.reloadStartTime > -1L ? (float)(currentReloadStartTime - this.reloadStartTime) / 500.0F : -1.0F;
      float local8099_10;
      if (local8099_8 >= 1.0F) {
         if (this.client.currentScreen != null) {
            this.client.currentScreen.renderWithTooltip(drawContext, 0, 0, local8099_4);
         } else {
            this.client.inGameHud.renderDeferredSubtitles();
         }

         local8099_10 = 1.0F - MathHelper.clamp(local8099_8 - 1.0F, 0.0F, 1.0F);
         drawContext.createNewRootLayer();
         drawContext.fill(0, 0, drawContext.getScaledWindowWidth(), drawContext.getScaledWindowHeight(), MathHelper.ceil(local8099_10 * 255.0F) << 24 | 1381139);
      } else if (this.reloading) {
         if (this.client.currentScreen != null && local8099_9 < 1.0F) {
            this.client.currentScreen.renderWithTooltip(drawContext, local8099_2, local8099_3, local8099_4);
         } else {
            this.client.inGameHud.renderDeferredSubtitles();
         }

         local8099_10 = MathHelper.clamp(local8099_9, 0.0F, 1.0F);
         drawContext.createNewRootLayer();
         drawContext.fill(
            0,
            0,
            drawContext.getScaledWindowWidth(),
            drawContext.getScaledWindowHeight(),
            MathHelper.ceil(MathHelper.clamp(local8099_9, 0.15F, 1.0F) * 255.0F) << 24 | 1381139
         );
      } else {
         local8099_10 = 1.0F;
         drawContext.fill(0, 0, drawContext.getScaledWindowWidth(), drawContext.getScaledWindowHeight(), -15396077);
      }

      if (this.greed$sceneMode) {
         this.greed$drawLoaderScene(drawContext, local8099_10);
      }

      if (local8099_8 >= 2.0F) {
         this.client.setOverlay(null);
      }

      callbackInfo.cancel();
   }

   private void greed$drawLoaderScene(DrawContext drawContext, float local8100_2) {
      InterfaceModule.getInstance().updateState();
      float local8100_3 = GuiScale.computeInt() / GuiScale.computeFloat();
      float local8100_4 = drawContext.getScaledWindowWidth() * local8100_3;
      float local8100_5 = drawContext.getScaledWindowHeight() * local8100_3;
      Render2D.flush(drawContext);
      Render2D.pushAlpha(local8100_2);
      UiTheme.processData6("assets/greed/textures/gui/mainmenu/background/menu.png", local8100_4, local8100_5, 1.0F);
      UiTheme.processData4(this.greed$shownAtMs, local8100_4, local8100_5);
      Render2D.popAlpha();
      Render2D.endFrame();
   }

   static {
      VMBridge.identifyClass(SplashOverlayMixin.class, "EXanmR3J");
   }
}
