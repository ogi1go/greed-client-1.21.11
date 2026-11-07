package greed.client.ui.hud;

import greed.client.render.Render2D;

public enum KeybindAnimationSprite {
   MAIN("main", 62, 8, 8, 30.0F),
   TOGGLE("toggle", 22, 5, 5, 25.0F);

   public final String strVal;
   public final int intVal;
   public final int intVal2;
   public final int intVal3;
   public final float floatVal;

   private KeybindAnimationSprite(String local2597_3, int local2597_4, int local2597_5, int local2597_6, float local2597_7) {
      this.strVal = "assets/greed/textures/gui/hud/" + local2597_3 + ".png";
      this.intVal = local2597_4;
      this.intVal2 = local2597_5;
      this.intVal3 = local2597_6;
      this.floatVal = local2597_7;
   }

   public int computeInt(float local2598_1) {
      int local2598_2 = this.intVal - 1;
      int local2598_3 = Math.round(local2598_1 * local2598_2);
      return Math.max(0, Math.min(local2598_2, local2598_3));
   }

   public void processData(float local2599_1, float local2599_2, float local2599_3, float local2599_4, int local2599_5, float local2599_6) {
      int local2599_7 = this.computeInt(local2599_6);
      int local2599_8 = local2599_7 % this.intVal2;
      int local2599_9 = local2599_7 / this.intVal2;
      float local2599_10 = 1.0F / this.intVal2;
      float local2599_11 = 1.0F / this.intVal3;
      Render2D.drawImageUv(
         this.strVal,
         local2599_1,
         local2599_2,
         local2599_3,
         local2599_4,
         0.0F,
         local2599_8 * local2599_10,
         local2599_9 * local2599_11,
         local2599_8 * local2599_10 + local2599_10,
         local2599_9 * local2599_11 + local2599_11,
         local2599_5
      );
   }
}
