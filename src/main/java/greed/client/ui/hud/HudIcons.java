package greed.client.ui.hud;

import greed.client.render.ThemeColors;

import greed.client.render.Render2D;

public enum HudIcons {
   KEYBINDS("z", null),
   POTIONS(null, "hud_potions"),
   COOLDOWNS("ξ", null),
   STAFF("ν", null),
   INVENTORY(null, "hud_inventory"),
   USER("E", null),
   FPS("x", null),
   TIME("B", null),
   PING(null, "hud_ping"),
   COORDS(null, "hud_coords"),
   ENVIRONMENT(null, "hud_environment"),
   ARROW(null, "hud_arrow"),
   COPY(null, "hud_copy"),
   CHECK(null, "hud_check");

   public final String strVal;
   public final String strVal2;

   private HudIcons(String local6687_3, String local6687_4) {
      this.strVal = local6687_3;
      this.strVal2 = local6687_4 == null ? null : "assets/greed/textures/gui/hud/" + local6687_4 + ".png";
   }

   public void processData(float local6688_1, float local6688_2, float local6688_3, int local6688_4) {
      if (this.strVal != null) {
         ThemeColors.processData(this.strVal, local6688_1 + local6688_3 / 2.0F, local6688_2 + local6688_3 / 2.0F, local6688_3, local6688_4);
      } else {
         Render2D.drawImageSized(this.strVal2, local6688_1, local6688_2, local6688_3, local6688_3, 0.0F, local6688_4);
      }
   }
}
