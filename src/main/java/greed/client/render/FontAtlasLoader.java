package greed.client.render;

import greed.client.GreedClient;
import greed.client.render.MsdfGlyph;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

final class FontAtlasLoader {
   private FontAtlasLoader() {
   }

   static FontAtlas loadAtlas(String local3097_0) {
      Identifier identifier = Identifier.of("greed", local3097_0 + ".json");
      Identifier identifier2 = Identifier.of("greed", local3097_0 + ".png");
      MinecraftClient minecraftClient = MinecraftClient.getInstance();

      try {
         minecraftClient.getTextureManager().registerTexture(identifier2, new ResourceTexture(identifier2));
      } catch (Throwable throwable) {
         GreedClient.logger().warn("[MSDF] Failed to register atlas texture {}", identifier2, throwable);
         return null;
      }

      Optional<Resource> resource = minecraftClient.getResourceManager().getResource(identifier);
      if (resource.isEmpty()) {
         GreedClient.logger().warn("[MSDF] Layout JSON not found: {}", identifier);
         return null;
      }

      try (Reader reader = resource.get().getReader()) {
         JsonObject asJsonObject = JsonParser.parseReader(reader).getAsJsonObject();
         JsonObject asJsonObject2 = asJsonObject.getAsJsonObject("atlas");
         int asInt = asJsonObject2.get("width").getAsInt();
         int asInt2 = asJsonObject2.get("height").getAsInt();
         float local3097_10 = asJsonObject2.has("distanceRange") ? asJsonObject2.get("distanceRange").getAsFloat() : 4.0F;
         JsonObject asJsonObject3 = asJsonObject.getAsJsonObject("metrics");
         float asFloat = asJsonObject3.get("lineHeight").getAsFloat();
         float asFloat2 = asJsonObject3.get("ascender").getAsFloat();
         float asFloat3 = asJsonObject3.get("descender").getAsFloat();
         Map<Integer, MsdfGlyph> local3097_15 = new HashMap<>(512);

         for (JsonElement jsonElement : asJsonObject.getAsJsonArray("glyphs")) {
            JsonObject asJsonObject4 = jsonElement.getAsJsonObject();
            int asInt3 = asJsonObject4.get("unicode").getAsInt();
            float local3097_20 = asJsonObject4.has("advance") ? asJsonObject4.get("advance").getAsFloat() : 0.0F;
            if (asJsonObject4.has("planeBounds") && asJsonObject4.has("atlasBounds")) {
               JsonObject asJsonObject5 = asJsonObject4.getAsJsonObject("planeBounds");
               JsonObject asJsonObject6 = asJsonObject4.getAsJsonObject("atlasBounds");
               float asFloat4 = asJsonObject5.get("left").getAsFloat();
               float asFloat5 = asJsonObject5.get("bottom").getAsFloat();
               float asFloat6 = asJsonObject5.get("right").getAsFloat();
               float asFloat7 = asJsonObject5.get("top").getAsFloat();
               float asFloat8 = asJsonObject6.get("left").getAsFloat();
               float asFloat9 = asJsonObject6.get("bottom").getAsFloat();
               float asFloat10 = asJsonObject6.get("right").getAsFloat();
               float asFloat11 = asJsonObject6.get("top").getAsFloat();
               if (asFloat10 - asFloat8 > 1.0F && asFloat11 - asFloat9 > 1.0F) {
                  float local3097_31 = (asFloat6 - asFloat4) * 0.5F / (asFloat10 - asFloat8);
                  float local3097_32 = (asFloat7 - asFloat5) * 0.5F / (asFloat11 - asFloat9);
                  asFloat4 += local3097_31;
                  asFloat6 -= local3097_31;
                  asFloat5 += local3097_32;
                  asFloat7 -= local3097_32;
                  asFloat8 += 0.5F;
                  asFloat10 -= 0.5F;
                  asFloat9 += 0.5F;
                  asFloat11 -= 0.5F;
               }

               float local3097_31 = asFloat8 / asInt;
               float local3097_32 = asFloat10 / asInt;
               float local3097_33 = (asInt2 - asFloat11) / asInt2;
               float local3097_34 = (asInt2 - asFloat9) / asInt2;
               local3097_15.put(
                  asInt3,
                  new MsdfGlyph(
                     true, local3097_20, asFloat4, asFloat7, asFloat6, asFloat5, local3097_31, local3097_33, local3097_32, local3097_34
                  )
               );
            } else {
               local3097_15.put(asInt3, MsdfGlyph.nonDrawable(local3097_20));
            }
         }

         Map<Long, Float> local3097_16 = new HashMap<>();
         if (asJsonObject.has("kerning")) {
            for (JsonElement jsonElement2 : asJsonObject.getAsJsonArray("kerning")) {
               JsonObject asJsonObject7 = jsonElement2.getAsJsonObject();
               int asInt4 = asJsonObject7.get("unicode1").getAsInt();
               int asInt5 = asJsonObject7.get("unicode2").getAsInt();
               float asFloat12 = asJsonObject7.get("advance").getAsFloat();
               if (asFloat12 != 0.0F) {
                  local3097_16.put((long)asInt4 << 32 | asInt5 & 4294967295L, asFloat12);
               }
            }
         }

         GreedClient.logger()
            .info(
               "[MSDF] Loaded font {} ({} glyphs, {}x{} atlas, pxRange {})",
               new Object[]{local3097_0, local3097_15.size(), asInt, asInt2, local3097_10}
            );
         return new FontAtlas(
            identifier2, asInt, asInt2, local3097_10, asFloat, asFloat2, asFloat3, local3097_15, local3097_16
         );
      } catch (Throwable throwable2) {
         GreedClient.logger().warn("[MSDF] Failed to parse layout {}", identifier, throwable2);
         return null;
      }
   }
}
