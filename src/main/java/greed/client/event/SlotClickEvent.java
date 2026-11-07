package greed.client.event;

import net.minecraft.screen.slot.SlotActionType;

public class SlotClickEvent extends CancellableEvent {
   private int syncId;
   private int slotId;
   private int button;
   private SlotActionType actionType;

   public int getButton() {
      return this.button;
   }

   public SlotClickEvent(int local4734_1, int local4734_2, int local4734_3, SlotActionType slotActionType) {
      this.syncId = local4734_1;
      this.slotId = local4734_2;
      this.button = local4734_3;
      this.actionType = slotActionType;
   }

   public void setSyncId(int local4735_1) {
      this.syncId = local4735_1;
   }

   public int getSlotId() {
      return this.slotId;
   }

   public void setSlotId(int local4737_1) {
      this.slotId = local4737_1;
   }

   public int getSyncId() {
      return this.syncId;
   }

   public void setActionType(SlotActionType slotActionType) {
      this.actionType = slotActionType;
   }

   public void setButton(int local4740_1) {
      this.button = local4740_1;
   }

   public SlotActionType getActionType() {
      return this.actionType;
   }
}
