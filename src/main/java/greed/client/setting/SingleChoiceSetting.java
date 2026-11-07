package greed.client.setting;

import java.util.ArrayList;
import java.util.List;

public class SingleChoiceSetting extends Setting<SingleChoiceSetting> {
   private final List<String> options = new ArrayList<>();
   private int selectedIndex;
   public boolean expanded;

   public void cycle() {
      if (!this.options.isEmpty()) {
         this.selectIndex((this.selectedIndex + 1) % this.options.size());
      }
   }

   public SingleChoiceSetting withIndex(int local998_1) {
      this.selectedIndex = this.clampIndex(local998_1);
      return this;
   }

   public String getSelectedOption() {
      return this.options.isEmpty() ? "" : this.options.get(this.selectedIndex);
   }

   public void selectIndex(int local1000_1) {
      int currentSelectedIndex = this.clampIndex(local1000_1);
      if (currentSelectedIndex != this.selectedIndex) {
         this.selectedIndex = currentSelectedIndex;
         this.notifyChanged();
      }
   }

   public SingleChoiceSetting withSelectedOption(String local1001_1) {
      for (int currentSelectedIndex = 0; currentSelectedIndex < this.options.size(); currentSelectedIndex++) {
         if (this.options.get(currentSelectedIndex).equalsIgnoreCase(local1001_1)) {
            this.selectedIndex = currentSelectedIndex;
            break;
         }
      }

      return this;
   }

   public List<String> getOptions() {
      return this.options;
   }

   public SingleChoiceSetting(String name, String local1003_2) {
      super(name, local1003_2);
   }

   public boolean isSelected(String local1004_1) {
      return this.getSelectedOption().equalsIgnoreCase(local1004_1);
   }

   public void selectOption(String local1005_1) {
      for (int index = 0; index < this.options.size(); index++) {
         if (this.options.get(index).equalsIgnoreCase(local1005_1)) {
            this.selectIndex(index);
            return;
         }
      }
   }

   public int getSelectedIndex() {
      return this.selectedIndex;
   }

   public boolean matchesOption(String local1007_1) {
      return this.isSelected(local1007_1);
   }

   public String getValue() {
      return this.getSelectedOption();
   }

   private int clampIndex(int local1009_1) {
      return this.options.isEmpty() ? 0 : Math.max(0, Math.min(this.options.size() - 1, local1009_1));
   }

   public SingleChoiceSetting withOptions(String... stringArray) {
      this.options.clear();

      for (String local1010_5 : stringArray) {
         this.options.add(local1010_5);
      }

      this.selectedIndex = this.clampIndex(this.selectedIndex);
      return this;
   }

   protected SingleChoiceSetting(String name, String local1011_2, String local1011_3, String... stringArray) {
      super(name, local1011_2);

      for (String local1011_8 : stringArray) {
         this.options.add(local1011_8);
      }

      this.withSelectedOption(local1011_3);
   }
}
