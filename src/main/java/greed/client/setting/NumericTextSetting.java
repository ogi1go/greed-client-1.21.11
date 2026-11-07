package greed.client.setting;

public final class NumericTextSetting extends TextSetting {
   private boolean numericOnly;

   public NumericTextSetting(String name, String description, String defaultValue, int maxLength) {
      super(name, description, defaultValue, maxLength);
   }

   public NumericTextSetting numericOnly() {
      this.numericOnly = true;
      return this;
   }

   @Override
   public void insertText(String text) {
      super.insertText(text != null && this.numericOnly ? text.replaceAll("\\D", "") : text);
   }
}