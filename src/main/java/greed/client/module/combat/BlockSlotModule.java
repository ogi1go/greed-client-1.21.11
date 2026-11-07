package greed.client.module.combat;

import greed.client.event.EventListener;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.setting.MultiSelectSetting;
import greed.client.event.SlotClickEvent;

import net.minecraft.screen.slot.SlotActionType;

@ModuleInfo(name = "BlockSlot", description = "Не даёт выкинуть предмет из выбранных слотов хотбара.", category = ModuleCategory.COMBAT)
public final class BlockSlotModule extends Module {
   private static BlockSlotModule blockSlotModule;
   public MultiSelectSetting multiSelectSetting;
   private final EventListener<SlotClickEvent> slotClickListener = slotClickEvent -> {
      if (mc.player != null
         && slotClickEvent.getActionType() == SlotActionType.THROW
         && mc.player.currentScreenHandler == mc.player.playerScreenHandler) {
         if (slotClickEvent.getSlotId() >= 36 && slotClickEvent.getSlotId() <= 44 && this.isSlotProtected(slotClickEvent.getSlotId() - 36)) {
            slotClickEvent.cancel();
         }
      }
   };

   public boolean isSlotProtected(int local2017_1) {
      return this.isEnabled() && this.multiSelectSetting.isIndexSelected(local2017_1);
   }

   public static BlockSlotModule getInstance() {
      if (blockSlotModule == null) {
         blockSlotModule = new BlockSlotModule();
      }

      return blockSlotModule;
   }

   private BlockSlotModule() {
      this.multiSelectSetting = this.addSetting(
         new MultiSelectSetting("Слоты", "Какие слоты хотбара защищать от выброса.", new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9"})
      );
   }
}
