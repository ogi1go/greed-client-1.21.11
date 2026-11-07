package greed.client.render;

public enum FontFace {
   THIN("thin"),
   MINI("mini"),
   SEMI("semi"),
   SEMI_BOLD("semi_bold"),
   BOLD("bold"),
   SF("sf"),
   SF_MEDIUM("sf_medium"),
   ICONS("icons");

   private final String resourceName;

   private FontFace(String initialResourceName) {
      this.resourceName = initialResourceName;
   }

   public String getResourceName() {
      return this.resourceName;
   }
}
