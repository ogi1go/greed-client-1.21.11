package greed.client.setting;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MultiChoiceSetting extends Setting<MultiChoiceSetting> {
   private final List<String> options = new ArrayList<>();
   private final Set<String> selectedOptions = new LinkedHashSet<>();
   private int minimumSelection;
   public boolean expanded;

   protected MultiChoiceSetting(String name, String description, String[] optionsArray, String... defaultSelections) {
      super(name, description);

      for (String option : optionsArray) {
         this.options.add(option);
      }

      for (String selection : defaultSelections) {
         String found = this.findOption(selection);
         if (found != null) {
            this.selectedOptions.add(found);
         }
      }
   }

   public MultiChoiceSetting(String name, String description) {
      super(name, description);
   }

   private String findOption(String option) {
      for (String opt : this.options) {
         if (opt.equalsIgnoreCase(option)) {
            return opt;
         }
      }
      return null;
   }

   public MultiChoiceSetting withOptions(String... optionsArray) {
      this.options.clear();
      for (String option : optionsArray) {
         this.options.add(option);
      }
      return this;
   }

   public MultiChoiceSetting withSelection(String... selections) {
      this.selectedOptions.clear();
      for (String selection : selections) {
         String found = this.findOption(selection);
         if (found != null) {
            this.selectedOptions.add(found);
         }
      }
      return this;
   }

   public MultiChoiceSetting withMinimumSelection(int minimumSelection) {
      this.minimumSelection = Math.max(0, minimumSelection);
      return this;
   }

   public void toggleOption(String option) {
      String found = this.findOption(option);
      if (found != null) {
         if (this.selectedOptions.contains(found)) {
            if (this.getSelectedCount() > this.minimumSelection) {
               this.selectedOptions.remove(found);
               this.notifyChanged();
            }
         } else {
            this.selectedOptions.add(found);
            this.notifyChanged();
         }
      }
   }

   public void toggleIndex(int index) {
      if (index >= 0 && index < this.options.size()) {
         this.toggleOption(this.options.get(index));
      }
   }

   public boolean isSelected(String option) {
      String found = this.findOption(option);
      return found != null && this.selectedOptions.contains(found);
   }

   public boolean isIndexSelected(int index) {
      return index >= 0 && index < this.options.size() && this.selectedOptions.contains(this.options.get(index));
   }

   public boolean matchesOption(String option) {
      return this.isSelected(option);
   }

   public int getSelectedCount() {
      return this.selectedOptions.size();
   }

   public List<String> getSelectedOptions() {
      return new ArrayList<>(this.selectedOptions);
   }

   public Set<String> getSelectedSet() {
      return new LinkedHashSet<>(this.selectedOptions);
   }

   public List<String> getOptions() {
      return this.options;
   }

   public String getSelectionSummary() {
      return this.getSelectedCount() + "/" + this.options.size();
   }
}