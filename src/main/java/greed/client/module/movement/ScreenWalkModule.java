package greed.client.module.movement;

import greed.client.ui.ClickGuiScreen;
import greed.mixin.ClientCommonNetworkHandlerMixin;
import greed.client.event.EventListener;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.PacketEvent;
import greed.client.event.PlayerTickEvent;
import greed.client.module.movement.MovementPauseController;
import greed.client.module.player.ServerHelperModule;

import java.util.ArrayList;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.BlockItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.slot.SlotActionType;

@ModuleInfo(name = "ScreenWalk", description = "Позволяет двигаться с открытым контейнером, задерживая пакеты инвентаря.", category = ModuleCategory.MOVEMENT)
public final class ScreenWalkModule extends Module {
   private static ScreenWalkModule screenWalkModule;
   private final ArrayList<ScreenWalkModule.SubContext> list = new ArrayList<>();
   private final EventListener<PacketEvent> packetListener = packetEvent -> {
      if (packetEvent.isOutgoing()) {
         if (mc.currentScreen instanceof InventoryScreen && packetEvent.getPacket() instanceof ClickSlotC2SPacket clickSlotC2SPacket && this.checkState2()) {
            boolean local1352_3 = clickSlotC2SPacket.button() == 1
               && mc.player.currentScreenHandler.getCursorStack().getItem() instanceof BlockItem blockItem
               && blockItem.getBlock() instanceof ShulkerBoxBlock;
            if (local1352_3) {
               MovementPauseController.getInstance().pauseForTicks(2);
            }

            this.list.add(new ScreenWalkModule.SubContext(packetEvent.getPacket(), 2, local1352_3));
            packetEvent.cancel();
         }

         if (packetEvent.getPacket() instanceof CloseHandledScreenC2SPacket && this.checkState2() && mc.currentScreen instanceof InventoryScreen) {
            packetEvent.cancel();

            for (ScreenWalkModule.SubContext local1352_3 : this.list) {
               MovementPauseController.getInstance().pauseForTicks(local1352_3.intVal);
            }
         }
      }
   };
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (mc.player != null) {
         if (mc.currentScreen != null
            && !(mc.currentScreen instanceof ChatScreen)
            && !(mc.currentScreen instanceof SignEditScreen)
            && !(mc.currentScreen instanceof AnvilScreen)
            && !(mc.currentScreen instanceof CreativeInventoryScreen)) {
            for (KeyBinding keyBinding : new KeyBinding[]{
               mc.options.forwardKey, mc.options.backKey, mc.options.leftKey, mc.options.rightKey, mc.options.jumpKey
            }) {
               keyBinding.setPressed(
                  !(
                        mc.currentScreen instanceof ClickGuiScreen clickGuiScreen
                           && (clickGuiScreen.getCtx().textSetting != null || clickGuiScreen.getCtx().nested0492 != null)
                     )
                     && InputUtil.isKeyPressed(mc.getWindow(), KeyBindingHelper.getBoundKeyOf(keyBinding).getCode())
               );
            }
         }

         if (!this.checkState2() && !this.list.isEmpty()) {
            for (ScreenWalkModule.SubContext local1349_3 : this.list) {
               ((ClientCommonNetworkHandlerMixin)mc.getNetworkHandler()).connection().send(local1349_3.packet2, null);
            }

            this.list.clear();
            if (mc.currentScreen == null) {
               ((ClientCommonNetworkHandlerMixin)mc.getNetworkHandler())
                  .connection()
                  .send(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId), null);
            }
         }
      }
   };

   @Override
   protected void onDisable() {
      this.list.clear();
   }

   public boolean checkState() {
      return ServerHelperModule.getInstance().modeSetting.matchesOption("SpookyTime");
   }

   private ScreenWalkModule() {
   }

   public void updateState() {
      if (mc.player != null && mc.interactionManager != null) {
         int local1354_1 = mc.player.playerScreenHandler.syncId;
         if (!mc.player.playerScreenHandler.getCursorStack().isEmpty()) {
            mc.interactionManager.clickSlot(local1354_1, -999, 0, SlotActionType.PICKUP, mc.player);
         }

         for (int index = 1; index < mc.player.playerScreenHandler.slots.size(); index++) {
            if (mc.player.playerScreenHandler.getSlot(index).hasStack()) {
               mc.interactionManager.clickSlot(local1354_1, index, 1, SlotActionType.THROW, mc.player);
            }
         }
      }
   }

   public static ScreenWalkModule getInstance() {
      if (screenWalkModule == null) {
         screenWalkModule = new ScreenWalkModule();
      }

      return screenWalkModule;
   }

   public boolean checkState2() {
      return mc.player != null && (mc.player.input.getMovementInput().lengthSquared() > 0.0F || mc.player.input.playerInput.jump());
   }

   static final class SubContext {
      private final Packet<?> packet2;
      private final int intVal;
      private final boolean flag;

      private SubContext(Packet<?> local1357_1, int local1357_2, boolean local1357_3) {
         this.packet2 = local1357_1;
         this.intVal = local1357_2;
         this.flag = local1357_3;
      }
   }
}
