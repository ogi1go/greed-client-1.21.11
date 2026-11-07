package greed.client.setting;

public final class LegacyNumberSetting extends NumberSetting {

   public LegacyNumberSetting(
           String name,
           String description,
           double defaultValue,
           double minimum,
           double maximum,
           double increment
   ) {
      super(
              name,
              description,
              (float) defaultValue,
              (float) minimum,
              (float) maximum,
              (float) increment,
              false
      );
   }
}