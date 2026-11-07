package greed.client.setting;

public final class ModeSetting extends SingleChoiceSetting {

   public ModeSetting(String name, String description, String defaultMode, String... modes) {
      super(name, description, defaultMode, modes);
   }
}