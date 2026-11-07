package greed.client.setting;

public class TextSetting extends Setting<TextSetting> {
   private String value = "";
   private String placeholder = "";
   private int minimumLength;
   private int maximumLength = 256;
   private int caret;
   private int selectionAnchor;
   public boolean flag;

   public String getSelectedText() {
      return this.value.substring(this.getSelectionStart(), this.getSelectionEnd());
   }

   public void backspace() {
      if (this.hasSelection()) {
         this.replaceRange(this.getSelectionStart(), this.getSelectionEnd(), "");
      } else if (this.caret > 0) {
         this.replaceRange(this.caret - 1, this.caret, "");
      }
   }

   public void moveToStart(boolean local3836_1) {
      this.caret = 0;
      if (!local3836_1) {
         this.selectionAnchor = 0;
      }
   }

   private void replaceRange(int local3837_1, int local3837_2, String local3837_3) {
      this.caret = this.selectionAnchor = local3837_1 + local3837_3.length();
      this.setValue(this.value.substring(0, local3837_1) + local3837_3 + this.value.substring(local3837_2));
   }

   public String getPlaceholder() {
      return this.placeholder;
   }

   public TextSetting(String name, String local3839_2) {
      super(name, local3839_2);
   }

   public int getCaret() {
      return this.caret;
   }

   public TextSetting withPlaceholder(String local3841_1) {
      this.placeholder = local3841_1 == null ? "" : local3841_1;
      return this;
   }

   public int getMaximumLength() {
      return this.maximumLength;
   }

   public int getMinimumLength() {
      return this.minimumLength;
   }

   public boolean hasSelection() {
      return this.caret != this.selectionAnchor;
   }

   public void insertText(String local3845_1) {
      this.replaceRange(this.getSelectionStart(), this.getSelectionEnd(), local3845_1 == null ? "" : local3845_1.replaceAll("[\\r\\n\\t]", ""));
   }

   public void appendCharacter(char local3846_1) {
      if (this.value.length() < this.maximumLength) {
         this.setValue(this.value + local3846_1);
      }
   }

   public void removeLastCharacter() {
      if (!this.value.isEmpty()) {
         this.setValue(this.value.substring(0, this.value.length() - 1));
      }
   }

   public void deleteForward() {
      if (this.hasSelection()) {
         this.replaceRange(this.getSelectionStart(), this.getSelectionEnd(), "");
      } else if (this.caret < this.value.length()) {
         this.replaceRange(this.caret, this.caret + 1, "");
      }
   }

   public String getValue() {
      return this.value;
   }

   public void selectAll() {
      this.selectionAnchor = 0;
      this.caret = this.value.length();
   }

   public int getSelectionEnd() {
      return Math.max(this.caret, this.selectionAnchor);
   }

   public TextSetting withLengthRange(int local3852_1, int local3852_2) {
      this.minimumLength = Math.max(0, local3852_1);
      this.maximumLength = Math.max(this.minimumLength, local3852_2);
      if (this.value.length() > this.maximumLength) {
         this.setValue(this.value.substring(0, this.maximumLength));
      }

      return this;
   }

   public int getSelectionStart() {
      return Math.min(this.caret, this.selectionAnchor);
   }

   protected TextSetting(String name, String local3854_2, String local3854_3, int local3854_4) {
      super(name, local3854_2);
      this.maximumLength = Math.max(0, local3854_4);
      this.setValue(local3854_3);
   }

   public TextSetting setValue(String local3855_1) {
      String local3855_2 = local3855_1 == null ? "" : local3855_1;
      if (local3855_2.length() > this.maximumLength) {
         local3855_2 = local3855_2.substring(0, this.maximumLength);
      }

      if (!local3855_2.equals(this.value)) {
         this.value = local3855_2;
         this.notifyChanged();
      }

      this.caret = Math.min(this.caret, this.value.length());
      this.selectionAnchor = Math.min(this.selectionAnchor, this.value.length());
      return this;
   }

   public String getText() {
      return this.value;
   }

   public void moveToEnd(boolean local3857_1) {
      this.caret = this.value.length();
      if (!local3857_1) {
         this.selectionAnchor = this.caret;
      }
   }

   public void moveCaret(int local3858_1, boolean local3858_2) {
      this.caret = Math.max(0, Math.min(this.value.length(), this.caret + local3858_1));
      if (!local3858_2) {
         this.selectionAnchor = this.caret;
      }
   }
}
