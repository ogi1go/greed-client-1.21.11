package greed.client.render;

import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;

public record CachedTexture(Identifier id, TextureSetup linearSetup, TextureSetup nearestSetup, int width, int height) {
   public TextureSetup setup(boolean local1950_1) {
      return local1950_1 ? this.nearestSetup : this.linearSetup;
   }

   public float drawWidth(float local1953_1) {
      return this.width > 0 && this.height > 0 && this.width < this.height ? local1953_1 * ((float)this.width / this.height) : local1953_1;
   }

   public float drawHeight(float local1955_1) {
      return this.width > 0 && this.height > 0 && this.height < this.width ? local1955_1 * ((float)this.height / this.width) : local1955_1;
   }
}
