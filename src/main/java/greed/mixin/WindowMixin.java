package greed.mixin;

import greed.client.WindowAppearance;
import greed.client.module.misc.UnHookModule;

import net.minecraft.client.WindowEventHandler;
import net.minecraft.client.WindowSettings;
import net.minecraft.client.util.MonitorTracker;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public abstract class WindowMixin {
   @Shadow
   @Final
   private long handle;

   @ModifyVariable(method = "<init>(Lnet/minecraft/client/WindowEventHandler;Lnet/minecraft/client/util/MonitorTracker;Lnet/minecraft/client/WindowSettings;Ljava/lang/String;Ljava/lang/String;)V", at = @At("HEAD"), argsOnly = true, ordinal = 1)
   private static String kimiko$forceInitialTitle(String local8326_0) {
      return UnHookModule.isUnhooked() ? local8326_0 : WindowAppearance.formatText().concat(" | by ogi");
   }

   @ModifyVariable(method = "setTitle(Ljava/lang/String;)V", at = @At("HEAD"), argsOnly = true)
   private String kimiko$forceTitle(String local8327_1) {
      return UnHookModule.isUnhooked() ? local8327_1 : WindowAppearance.formatText().concat(" | by ogi");
   }

   @Inject(method = "<init>(Lnet/minecraft/client/WindowEventHandler;Lnet/minecraft/client/util/MonitorTracker;Lnet/minecraft/client/WindowSettings;Ljava/lang/String;Ljava/lang/String;)V", at = @At("TAIL"))
   private void kimiko$applyDarkTitleBar(
      WindowEventHandler windowEventHandler, MonitorTracker monitorTracker, WindowSettings windowSettings, String local8328_4, String local8328_5, CallbackInfo callbackInfo
   ) {
      WindowAppearance.processData(this.handle);
   }
}
