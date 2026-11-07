package greed.mixin;

import greed.client.module.render.ArmorBarModule;
import greed.client.module.render.CameraFeatureModule;
import greed.client.module.render.CrosshairModule;
import greed.client.event.EventBus;
import greed.client.event.HudRenderEvent;
import greed.client.module.misc.InfoReplaceModule;
import greed.client.module.misc.UnHookModule;
import greed.client.module.render.InterfaceModule;
import greed.client.module.combat.ItemHelperModule;
import greed.client.module.render.NoRenderModule;
import greed.client.util.InventorySlotManager;
import greed.client.render.Render2D;
import greed.client.module.misc.StreamerModModule;
import ru.dreamix.fabricloader.VMBridge;
import greed.client.module.render.WardenEspModule;
import greed.client.module.misc.YTMODEModule;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Collection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.option.AttackIndicator;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.Window;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
   @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("HEAD"))
   private void renderBelowHud(DrawContext drawContext, RenderTickCounter renderTickCounter, CallbackInfo callbackInfo) {
      WardenEspModule.getInstance().renderElement2(drawContext, renderTickCounter.getTickProgress(false));
   }

   @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("RETURN"))
   private void renderHud(DrawContext drawContext, RenderTickCounter renderTickCounter, CallbackInfo callbackInfo) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient.getWindow() != null) {
         Window window = minecraftClient.getWindow();
         Render2D.flush(drawContext);
         if (!UnHookModule.isUnhooked() && EventBus.getInstance().hasListeners(HudRenderEvent.class)) {
            EventBus.getInstance()
               .post(new HudRenderEvent(drawContext, renderTickCounter.getTickProgress(false), window.getScaledWidth(), window.getScaledHeight()));
         }

         Render2D.endFrame();
      }
   }

   @Inject(method = "renderOverlay(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/util/Identifier;F)V", at = @At("HEAD"), cancellable = true)
   private void removePumpkinOverlay(DrawContext drawContext, Identifier identifier, float local8263_3, CallbackInfo callbackInfo) {
      if (NoRenderModule.getInstance().matchesCriteria2("Тыква на голове") && identifier.getPath().contains("pumpkinblur")) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "renderCrosshair(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("HEAD"), cancellable = true)
   private void hideVanillaCrosshair(DrawContext drawContext, RenderTickCounter renderTickCounter, CallbackInfo callbackInfo) {
      if (CrosshairModule.getInstance().isEnabled() || CameraFeatureModule.getInstance().checkState4()) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "renderMainHud(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("TAIL"))
   private void renderHotbarOverlays(DrawContext drawContext, RenderTickCounter renderTickCounter, CallbackInfo callbackInfo) {
      if (ArmorBarModule.getInstance().isEnabled()) {
         ArmorBarModule.getInstance().renderElement(drawContext, renderTickCounter);
      }

      if (ItemHelperModule.getInstance().isEnabled()) {
         ItemHelperModule.getInstance().renderElement(drawContext);
      }
   }

   @Inject(method = "renderStatusEffectOverlay(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("HEAD"), cancellable = true)
   private void hideVanillaEffects(DrawContext drawContext, RenderTickCounter renderTickCounter, CallbackInfo callbackInfo) {
      if (InterfaceModule.getInstance().matchesCriteria("potions")) {
         callbackInfo.cancel();
      }
   }

   @ModifyExpressionValue(
      method = "renderCrosshair(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;")
   )
   private Object hideCrosshairAttackIndicator(Object local8267_1) {
      return NoRenderModule.getInstance().matchesCriteria2("Индикатор атаки") ? AttackIndicator.OFF : local8267_1;
   }

   @ModifyExpressionValue(
      method = "renderHotbar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;")
   )
   private Object hideHotbarAttackIndicator(Object local8268_1) {
      return NoRenderModule.getInstance().matchesCriteria2("Индикатор атаки") ? AttackIndicator.OFF : local8268_1;
   }

   @WrapOperation(
      method = "renderHotbar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;getStack(I)Lnet/minecraft/item/ItemStack;")
   )
   private ItemStack hideCarriedFromHotbar(PlayerInventory playerInventory, int local8269_2, Operation<ItemStack> local8269_3) {
      return InventorySlotManager.getInstance().getItemStack(local8269_2, (ItemStack)local8269_3.call(new Object[]{playerInventory, local8269_2}));
   }

   @WrapOperation(method = "renderHotbar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;getSelectedSlot()I"))
   private int hideHelperSlotSelection(PlayerInventory playerInventory, Operation<Integer> local8270_2) {
      return ItemHelperModule.getInstance().computeInt2() != -1
         ? ItemHelperModule.getInstance().computeInt2()
         : (Integer)local8270_2.call(new Object[]{playerInventory});
   }

   @Inject(
      method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void hideScoreboardSidebar(DrawContext drawContext, RenderTickCounter renderTickCounter, CallbackInfo callbackInfo) {
      if (NoRenderModule.getInstance().matchesCriteria2("Скорборд")) {
         callbackInfo.cancel();
      }
   }

   @ModifyExpressionValue(
      method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/scoreboard/Scoreboard;getScoreboardEntries(Lnet/minecraft/scoreboard/ScoreboardObjective;)Ljava/util/Collection;"
      )
   )
   private Collection<ScoreboardEntry> hideStreamerSidebarRows(Collection<ScoreboardEntry> local8272_1) {
      return !StreamerModModule.getInstance().isEnabled() && !YTMODEModule.getInstance().isEnabled()
         ? local8272_1
         : local8272_1.stream()
            .filter(
               scoreboardEntry -> (!YTMODEModule.getInstance().isEnabled() || !YTMODEModule.getInstance().matchesCriteria(scoreboardEntry))
                  && (!StreamerModModule.getInstance().isEnabled() || !StreamerModModule.getInstance().matchesCriteria(scoreboardEntry))
            )
            .toList();
   }

   @ModifyExpressionValue(
      method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/scoreboard/ScoreboardObjective;getDisplayName()Lnet/minecraft/text/Text;")
   )
   private Text maskStreamerSidebarTitle(Text text) {
      return !StreamerModModule.getInstance().isEnabled() ? text : StreamerModModule.getInstance().getText(text);
   }

   @ModifyExpressionValue(
      method = "method_55439(Lnet/minecraft/scoreboard/Scoreboard;Lnet/minecraft/scoreboard/number/NumberFormat;Lnet/minecraft/scoreboard/ScoreboardEntry;)Lnet/minecraft/client/gui/hud/InGameHud$SidebarEntry;",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/scoreboard/Team;decorateName(Lnet/minecraft/scoreboard/AbstractTeam;Lnet/minecraft/text/Text;)Lnet/minecraft/text/MutableText;"
      )
   )
   private MutableText maskInfoSidebarRow(MutableText mutableText) {
      return !InfoReplaceModule.getInstance().isEnabled() ? mutableText : InfoReplaceModule.getInstance().getMutableText(mutableText);
   }

   static {
      VMBridge.identifyClass(InGameHudMixin.class, "3SjEjwxM");
   }
}
