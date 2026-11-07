package greed.client.module;

public enum ModuleCategory {
   COMBAT("Combat", "k"),
   MOVEMENT("Movement", "l"),
   RENDER("Visuals", "m"),
   PLAYER("Player", "n"),
   MISC("Misc", "o");

   private final String displayName;
   private final String icon;

   private ModuleCategory(String initialDisplayName, String initialIcon) {
      this.displayName = initialDisplayName;
      this.icon = initialIcon;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public String getIcon() {
      return this.icon;
   }
}
