package greed.client.module.misc;

import greed.client.util.Colors;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.setting.NumericTextSetting;
import greed.client.ui.UiTheme;

import net.minecraft.text.CharacterVisitor;

@ModuleInfo(name = "NameProtect", description = "Заменяет твой ник во всём интерфейсе.", category = ModuleCategory.MISC)
public final class NameProtectModule extends Module {
   private static NameProtectModule nameProtectModule;
   public NumericTextSetting numericTextSetting = this.addSetting(new NumericTextSetting("Ник", "", "Greed", 16));

   public String formatText(String local4493_1) {
      return this.isEnabled()
            && local4493_1 != null
            && mc.player != null
            && !this.numericTextSetting.getText().isEmpty()
            && local4493_1.contains(mc.player.getNameForScoreboard())
         ? local4493_1.replace(mc.player.getNameForScoreboard(), this.numericTextSetting.getText())
         : local4493_1;
   }

   private NameProtectModule() {
   }

   public static NameProtectModule getInstance() {
      if (nameProtectModule == null) {
         nameProtectModule = new NameProtectModule();
      }

      return nameProtectModule;
   }

   public CharacterVisitor getCharacterVisitor(String local4497_1, CharacterVisitor characterVisitor) {
      return !this.numericTextSetting.getText().isEmpty() && local4497_1.contains(this.numericTextSetting.getText())
         ? (local4494_3, style, local4494_5) -> {
            int local4494_6 = local4497_1.lastIndexOf(this.numericTextSetting.getText(), local4494_3);
            return local4494_6 >= 0 && local4494_3 < local4494_6 + this.numericTextSetting.getText().length()
               ? characterVisitor.accept(
                  local4494_3,
                  style.withColor(
                        Colors.computeInt2(
                              UiTheme.intVal7,
                              UiTheme.intVal9,
                              Math.min(1.0F, (local4494_3 - local4494_6) * 1.1F / this.numericTextSetting.getText().length())
                           )
                           & 16777215
                     )
                     .withBold(true),
                  local4494_5
               )
               : characterVisitor.accept(local4494_3, style, local4494_5);
         }
         : characterVisitor;
   }

   public int computeInt(int local4498_1, int local4498_2) {
      return Colors.computeInt2(
               UiTheme.intVal7, UiTheme.intVal9, Math.min(1.0F, local4498_1 * 1.1F / this.numericTextSetting.getText().length())
            )
            & 16777215
         | local4498_2 & 0xFF000000;
   }
}
