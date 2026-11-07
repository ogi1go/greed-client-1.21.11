package greed.client.render;

public record CornerBracketDrawData(float x, float y, float size, float radius, float stroke, float smoothness, int quadrant, int color) {
   public static final float DEFAULT_SMOOTHNESS = 0.25F;
   public static final int TL = 0;
   public static final int TR = 1;
   public static final int BR = 2;
   public static final int BL = 3;

   public CornerBracketDrawData(float local4561_1, float local4561_2, float local4561_3, float local4561_4, float local4561_5, int local4561_6, int local4561_7) {
      this(local4561_1, local4561_2, local4561_3, local4561_4, local4561_5, 0.25F, local4561_6, local4561_7);
   }

   public boolean visible() {
      return this.size > 0.0F && this.stroke > 0.0F && this.color >>> 24 != 0;
   }
}
