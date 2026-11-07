package greed.client.setting;

public final class BooleanSetting extends Setting<BooleanSetting> {
   private boolean value;
   private final boolean defaultValue;

   public BooleanSetting setValue(boolean newValue) {
      if (newValue != this.value) {
         this.value = newValue;
         this.notifyChanged();
      }

      return this;
   }

   public boolean getDefaultValue() {
      return this.defaultValue;
   }

   public boolean getValue() {
      return this.value;
   }

   public BooleanSetting(String name, String local3806_2) {
      this(name, local3806_2, false);
   }

   public boolean isChecked() {
      return this.value;
   }

   public BooleanSetting(String name, String local3808_2, boolean initialValue) {
      super(name, local3808_2);
      this.value = initialValue;
      this.defaultValue = initialValue;
   }

   public BooleanSetting toggleValue() {
      return this.setValue(!this.value);
   }
}
