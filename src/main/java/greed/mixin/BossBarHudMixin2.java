package greed.mixin;

import greed.client.module.misc.StreamerModModule;
import greed.client.module.misc.YTMODEModule;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.entity.boss.BossBar.Color;
import net.minecraft.entity.boss.BossBar.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BossBarHud.class)
public abstract class BossBarHudMixin2 {
   @ModifyExpressionValue(
      method = "render(Lnet/minecraft/client/gui/DrawContext;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ClientBossBar;getName()Lnet/minecraft/text/Text;")
   )
   private Text maskDuelBossBarTitle(Text text) {
      if (YTMODEModule.getInstance().checkState()) {
         return YTMODEModule.getInstance().getText();
      } else {
         return !StreamerModModule.getInstance().isEnabled() ? text : StreamerModModule.getInstance().getText2();
      }
   }

   @ModifyExpressionValue(
      method = "renderBossBar(Lnet/minecraft/client/gui/DrawContext;IILnet/minecraft/entity/boss/BossBar;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/boss/BossBar;getPercent()F")
   )
   private float freezeDuelBossBarProgress(float local8364_1) {
      return !StreamerModModule.getInstance().isEnabled() && !YTMODEModule.getInstance().checkState() ? local8364_1 : 1.0F;
   }

   @ModifyExpressionValue(
      method = "renderBossBar(Lnet/minecraft/client/gui/DrawContext;IILnet/minecraft/entity/boss/BossBar;I[Lnet/minecraft/util/Identifier;[Lnet/minecraft/util/Identifier;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/boss/BossBar;getColor()Lnet/minecraft/entity/boss/BossBar$Color;")
   )
   private Color recolorDuelBossBar(Color color) {
      if (YTMODEModule.getInstance().checkState()) {
         return YTMODEModule.getInstance().getColor();
      } else {
         return !StreamerModModule.getInstance().isEnabled() ? color : Color.BLUE;
      }
   }

   @ModifyExpressionValue(
      method = "renderBossBar(Lnet/minecraft/client/gui/DrawContext;IILnet/minecraft/entity/boss/BossBar;I[Lnet/minecraft/util/Identifier;[Lnet/minecraft/util/Identifier;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/boss/BossBar;getStyle()Lnet/minecraft/entity/boss/BossBar$Style;")
   )
   private Style notchDuelBossBar(Style style) {
      return !StreamerModModule.getInstance().isEnabled() && !YTMODEModule.getInstance().checkState() ? style : Style.NOTCHED_20;
   }
}
