package greed.client.setting;

public final class ActionSetting extends Setting<ActionSetting> {
   private String buttonLabel = "";
   private Runnable action;

   public ActionSetting withButtonLabel(String btnLabel) {
      this.buttonLabel = btnLabel == null ? "" : btnLabel;
      return this;
   }

   public void runAction() {
      if (this.action != null) {
         this.action.run();
      }

      this.notifyChanged();
   }

   public String getButtonLabel() {
      return this.buttonLabel.isEmpty() ? this.name : this.buttonLabel;
   }

   public ActionSetting(String initialButtonLabel, String local7457_2) {
      super(initialButtonLabel, local7457_2);
      this.buttonLabel = initialButtonLabel;
   }

   public ActionSetting onClick(Runnable newAction) {
      this.action = newAction;
      return this;
   }
}
