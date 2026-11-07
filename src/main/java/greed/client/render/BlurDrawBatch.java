package greed.client.render;

import net.minecraft.client.texture.TextureSetup;

public final class BlurDrawBatch {
   public float floatVal;
   public float floatVal2;
   public float floatVal3 = 1.0F;
   public float floatVal4 = 1.0F;
   public TextureSetup textureSetup = TextureSetup.empty();

   public void updateState() {
      this.floatVal = 0.0F;
      this.floatVal2 = 0.0F;
      this.floatVal3 = 1.0F;
      this.floatVal4 = 1.0F;
      this.textureSetup = TextureSetup.empty();
   }
}
