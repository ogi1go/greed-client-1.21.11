package greed.client.render;

public record LineDrawData(float x1, float y1, float x2, float y2, float thickness, int color, float fadeStart, float fadeEnd) {
   public float[] corners() {
      float local3058_1 = this.x2 - this.x1;
      float local3058_2 = this.y2 - this.y1;
      float local3058_3 = (float)Math.sqrt(local3058_1 * local3058_1 + local3058_2 * local3058_2);
      if (local3058_3 < 1.0E-4F) {
         local3058_3 = 1.0E-4F;
      }

      float local3058_4 = -local3058_2 / local3058_3 * this.thickness * 0.5F;
      float local3058_5 = local3058_1 / local3058_3 * this.thickness * 0.5F;
      return new float[]{
         this.x1 + local3058_4,
         this.y1 + local3058_5,
         this.x1 - local3058_4,
         this.y1 - local3058_5,
         this.x2 - local3058_4,
         this.y2 - local3058_5,
         this.x2 + local3058_4,
         this.y2 + local3058_5
      };
   }

   public float length() {
      float local3065_1 = this.x2 - this.x1;
      float local3065_2 = this.y2 - this.y1;
      return (float)Math.sqrt(local3065_1 * local3065_1 + local3065_2 * local3065_2);
   }

   public LineDrawData(float local3069_1, float local3069_2, float local3069_3, float local3069_4, float local3069_5, int local3069_6) {
      this(local3069_1, local3069_2, local3069_3, local3069_4, local3069_5, local3069_6, 1.0F, 1.0F);
   }

   public boolean visible() {
      if (!(this.thickness <= 0.0F) && (this.color >>> 24 & 0xFF) != 0) {
         float local3070_1 = this.x2 - this.x1;
         float local3070_2 = this.y2 - this.y1;
         return local3070_1 * local3070_1 + local3070_2 * local3070_2 > 1.0E-4F;
      } else {
         return false;
      }
   }
}
