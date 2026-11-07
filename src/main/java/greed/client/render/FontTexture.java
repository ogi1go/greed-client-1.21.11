package greed.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import java.awt.image.BufferedImage;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureSetup;

public final class FontTexture implements AutoCloseable {
   private static int intVal;
   private final int intVal2;
   private final NativeImage nativeImage;
   private final NativeImageBackedTexture nativeImageBackedTexture;
   private final TextureSetup textureSetup;
   private int intVal3 = 1;
   private int intVal4 = 1;
   private int intVal5;
   private int intVal6 = Integer.MAX_VALUE;
   private int intVal7 = Integer.MAX_VALUE;
   private int intVal8 = Integer.MIN_VALUE;
   private int intVal9 = Integer.MIN_VALUE;

   TextureSetup getTextureSetup() {
      return this.textureSetup;
   }

   public FontTexture.DataRecord getNested0490(int local5774_1, int local5774_2) {
      int local5774_3 = local5774_1 + 2;
      int local5774_4 = local5774_2 + 2;
      if (local5774_3 <= this.intVal2 && local5774_4 <= this.intVal2) {
         if (this.intVal3 + local5774_3 > this.intVal2) {
            this.intVal3 = 1;
            this.intVal4 = this.intVal4 + this.intVal5;
            this.intVal5 = 0;
         }

         if (this.intVal4 + local5774_4 > this.intVal2) {
            return null;
         }

         int local5774_5 = this.intVal3;
         int local5774_6 = this.intVal4;
         this.intVal3 += local5774_3;
         this.intVal5 = Math.max(this.intVal5, local5774_4);
         return new FontTexture.DataRecord(
            this,
            local5774_5,
            local5774_6,
            (float)local5774_5 / this.intVal2,
            (float)local5774_6 / this.intVal2,
            (float)(local5774_5 + local5774_1) / this.intVal2,
            (float)(local5774_6 + local5774_2) / this.intVal2
         );
      } else {
         return null;
      }
   }

   @Override
   public void close() {
      this.nativeImageBackedTexture.close();
   }

   public void processData(BufferedImage bufferedImage, int local5776_2, int local5776_3) {
      int width = bufferedImage.getWidth();
      int height = bufferedImage.getHeight();

      for (int index = 0; index < height; index++) {
         for (int index2 = 0; index2 < width; index2++) {
            this.nativeImage.setColor(local5776_2 + index2, local5776_3 + index, computeInt(bufferedImage.getRGB(index2, index)));
         }
      }

      this.intVal6 = Math.min(this.intVal6, local5776_2);
      this.intVal7 = Math.min(this.intVal7, local5776_3);
      this.intVal8 = Math.max(this.intVal8, local5776_2 + width);
      this.intVal9 = Math.max(this.intVal9, local5776_3 + height);
   }

   public FontTexture(int local5777_1) {
      this.intVal2 = local5777_1;
      this.nativeImage = new NativeImage(local5777_1, local5777_1, true);
      this.nativeImageBackedTexture = new NativeImageBackedTexture(() -> "greed_font_atlas_" + intVal++, this.nativeImage);
      this.textureSetup = TextureSetup.of(this.nativeImageBackedTexture.getGlTextureView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
   }

   private static int computeInt(int local5778_0) {
      int local5778_1 = local5778_0 >>> 24 & 0xFF;
      int local5778_2 = local5778_0 >>> 16 & 0xFF;
      int local5778_3 = local5778_0 >>> 8 & 0xFF;
      int local5778_4 = local5778_0 & 0xFF;
      return local5778_1 << 24 | local5778_4 << 16 | local5778_3 << 8 | local5778_2;
   }

   public void updateState() {
      if (this.intVal8 > this.intVal6 && this.intVal9 > this.intVal7) {
         if (this.nativeImageBackedTexture.getGlTexture() != null) {
            int local5780_1 = Math.max(0, this.intVal6);
            int local5780_2 = Math.max(0, this.intVal7);
            int local5780_3 = Math.min(this.intVal2, this.intVal8) - local5780_1;
            int local5780_4 = Math.min(this.intVal2, this.intVal9) - local5780_2;
            if (local5780_3 > 0 && local5780_4 > 0) {
               RenderSystem.getDevice()
                  .createCommandEncoder()
                  .writeToTexture(
                     this.nativeImageBackedTexture.getGlTexture(), this.nativeImage, 0, 0, local5780_1, local5780_2, local5780_3, local5780_4, local5780_1, local5780_2
                  );
            }

            this.intVal6 = Integer.MAX_VALUE;
            this.intVal7 = Integer.MAX_VALUE;
            this.intVal8 = Integer.MIN_VALUE;
            this.intVal9 = Integer.MIN_VALUE;
         }
      }
   }

   public record DataRecord(FontTexture page, int x, int y, float u0, float v0, float u1, float v1) {
   }
}
