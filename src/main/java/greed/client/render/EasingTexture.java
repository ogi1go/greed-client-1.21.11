package greed.client.render;

import greed.client.util.EasingFunction;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.math.MathHelper;

public final class EasingTexture {
   private static final Map<EasingFunction, NativeImageBackedTexture> textures = new IdentityHashMap<>();

   public static TextureSetup getTexture(EasingFunction easingFunction) {
      if (easingFunction == null) {
         return null;
      }

      NativeImageBackedTexture nativeImageBackedTexture = textures.computeIfAbsent(easingFunction, EasingTexture::bakeTexture);
      GpuTextureView glTextureView = nativeImageBackedTexture.getGlTextureView();
      return glTextureView == null ? null : TextureSetup.of(glTextureView, RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
   }

   private EasingTexture() {
   }

   private static NativeImageBackedTexture bakeTexture(EasingFunction easingFunction) {
      NativeImageBackedTexture nativeImageBackedTexture = new NativeImageBackedTexture(() -> "greed_easing_lut", 256, 1, false);
      NativeImage image = nativeImageBackedTexture.getImage();

      for (int index = 0; index < 256; index++) {
         float local4856_4 = index / 255.0F;
         int local4856_5 = MathHelper.clamp(Math.round((float)easingFunction.apply(local4856_4) * 255.0F), 0, 255);
         image.setColorArgb(index, 0, 0xFF000000 | local4856_5 << 16 | local4856_5 << 8 | local4856_5);
      }

      nativeImageBackedTexture.upload();
      return nativeImageBackedTexture;
   }
}
