package greed.client.cosmetic;

import greed.client.cosmetic.BedrockModel;
import greed.client.cosmetic.CosmeticWarmup;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public final class CosmeticAnimationLoader {
   private static final Map<Integer, CosmeticAnimationState> map = new ConcurrentHashMap<>();

   private static float computeFloat(JsonObject jsonObject, String local6553_1, float local6553_2) {
      try {
         return jsonObject.has(local6553_1) ? jsonObject.get(local6553_1).getAsFloat() : local6553_2;
      } catch (Exception exception) {
         return local6553_2;
      }
   }

   private static int computeInt(JsonObject jsonObject, String local6554_1, int local6554_2) {
      try {
         return jsonObject.has(local6554_1) ? jsonObject.get(local6554_1).getAsInt() : local6554_2;
      } catch (Exception exception) {
         return local6554_2;
      }
   }

   private static float[] computeFloat2(JsonElement jsonElement, float[] floatArray) {
      if (jsonElement == null || jsonElement.isJsonNull()) {
         return (float[])floatArray.clone();
      }

      if (jsonElement.isJsonPrimitive()) {
         float local6555_2;
         try {
            local6555_2 = jsonElement.getAsFloat();
         } catch (Exception exception) {
            return (float[])floatArray.clone();
         }

         return new float[]{local6555_2, local6555_2, local6555_2};
      } else {
         if (!jsonElement.isJsonArray()) {
            return (float[])floatArray.clone();
         }

         JsonArray asJsonArray = jsonElement.getAsJsonArray();
         float[] floatArray2 = (float[])floatArray.clone();

         for (int index = 0; index < Math.min(3, asJsonArray.size()); index++) {
            try {
               floatArray2[index] = asJsonArray.get(index).getAsFloat();
            } catch (Exception exception2) {
            }
         }

         return floatArray2;
      }
   }

   private static JsonObject getJsonObject(JsonObject jsonObject, String local6556_1) {
      return jsonObject != null && jsonObject.has(local6556_1) && jsonObject.get(local6556_1).isJsonObject()
         ? jsonObject.getAsJsonObject(local6556_1)
         : null;
   }

   private static Map<String, CosmeticAnimationState.ChannelState> getMap(JsonObject jsonObject) {
      Map<String, CosmeticAnimationState.ChannelState> local6557_1 = new LinkedHashMap<>();
      JsonObject jsonObject2 = getJsonObject(jsonObject, "animation");
      JsonObject jsonObject3 = jsonObject2 == null ? null : getJsonObject(jsonObject2, "animations");
      if (jsonObject3 == null) {
         return local6557_1;
      }

      for (Entry<String, JsonElement> local6557_5 : jsonObject3.entrySet()) {
         if (local6557_5.getValue().isJsonObject()) {
            JsonObject jsonObject4 = local6557_5.getValue().getAsJsonObject();
            boolean local6557_7 = matchesCriteria(jsonObject4, "loop", true);
            float local6557_8 = computeFloat(jsonObject4, "animation_length", 1.0F);
            Map<String, CosmeticAnimationState.KeyframeState> local6557_9 = new LinkedHashMap<>();
            JsonObject jsonObject5 = getJsonObject(jsonObject4, "bones");
            if (jsonObject5 != null) {
               for (Entry<String, JsonElement> local6557_12 : jsonObject5.entrySet()) {
                  if (local6557_12.getValue().isJsonObject()) {
                     JsonObject asJsonObject = local6557_12.getValue().getAsJsonObject();
                     local6557_9.put(
                        local6557_12.getKey(),
                        new CosmeticAnimationState.KeyframeState(
                           getNested0533(asJsonObject.get("rotation"), new float[]{0.0F, 0.0F, 0.0F}),
                           getNested0533(asJsonObject.get("position"), new float[]{0.0F, 0.0F, 0.0F}),
                           getNested0533(asJsonObject.get("scale"), new float[]{1.0F, 1.0F, 1.0F})
                        )
                     );
                  }
               }
            }

            local6557_1.put(local6557_5.getKey(), new CosmeticAnimationState.ChannelState(local6557_5.getKey(), local6557_7, local6557_8, local6557_9));
         }
      }

      return local6557_1;
   }

   private static CosmeticAnimationState.BoneState getNested0533(JsonElement jsonElement, float[] floatArray) {
      CosmeticAnimationState.BoneState local6558_2 = new CosmeticAnimationState.BoneState();
      if (jsonElement == null || jsonElement.isJsonNull()) {
         return local6558_2;
      }

      if (jsonElement.isJsonArray() || jsonElement.isJsonPrimitive()) {
         local6558_2.floatValArray = computeFloat2(jsonElement, floatArray);
         return local6558_2;
      }

      if (!jsonElement.isJsonObject()) {
         return local6558_2;
      }

      JsonObject asJsonObject = jsonElement.getAsJsonObject();

      for (Entry<String, JsonElement> local6558_5 : asJsonObject.entrySet()) {
         try {
            float local6558_6 = Float.parseFloat(local6558_5.getKey());
            local6558_2.map4.put(local6558_6, computeFloat3(local6558_5.getValue(), floatArray));
         } catch (NumberFormatException exception) {
         }
      }

      return local6558_2;
   }

   private static boolean matchesCriteria(JsonObject jsonObject, String local6560_1, boolean local6560_2) {
      try {
         return jsonObject.has(local6560_1) ? jsonObject.get(local6560_1).getAsBoolean() : local6560_2;
      } catch (Exception exception) {
         return local6560_2;
      }
   }

   private CosmeticAnimationLoader() {
   }

   private static CosmeticAnimationState getCosmeticAnimationState(CosmeticEntry cosmeticEntry) {
      try (InputStream inputStream = new ByteArrayInputStream(CosmeticPreviewAtlas.getByte(cosmeticEntry.modelResource()))) {
         if (inputStream == null) {
            return null;
         }

         JsonObject jsonObject = JsonParser.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).getAsJsonObject();
         JsonObject jsonObject2 = getJsonObject(jsonObject, "model");
         if (jsonObject2 == null) {
            return null;
         }

         BedrockModel bedrockModel = BedrockGeometryLoader.getBedrockModel2(jsonObject2.toString());
         return bedrockModel == null
            ? null
            : new CosmeticAnimationState(
               cosmeticEntry.id(),
               formatText(jsonObject, "name", cosmeticEntry.name()),
               computeInt(jsonObject, "pos", 1),
               computeFloat(jsonObject, "scale", 1.0F),
               computeFloat(jsonObject, "x", 0.0F),
               computeFloat(jsonObject, "y", 0.0F),
               computeFloat(jsonObject, "z", 0.0F),
               computeFloat(jsonObject, "yaw", 0.0F),
               computeFloat(jsonObject, "pitch", 0.0F),
               computeFloat(jsonObject, "roll", 0.0F),
               bedrockModel,
               getMap(jsonObject)
            );
      } catch (Exception exception) {
         System.err.println("[Cosmetics] failed to load " + cosmeticEntry.name() + ": " + exception);
         return null;
      }
   }

   public static CosmeticAnimationState getCosmeticAnimationState2(CosmeticEntry cosmeticEntry) {
      if (cosmeticEntry != null && cosmeticEntry.modelResource() != null) {
         CosmeticAnimationState cosmeticAnimationState = map.get(cosmeticEntry.id());
         if (cosmeticAnimationState == null) {
            CosmeticWarmup.processData(cosmeticEntry);
         }

         return cosmeticAnimationState;
      } else {
         return null;
      }
   }

   public static CosmeticAnimationLoader.AnimationVec3 getNested0513(CosmeticEntry cosmeticEntry) {
      CosmeticAnimationState cosmeticAnimationState = getCosmeticAnimationState2(cosmeticEntry);
      return cosmeticAnimationState == null ? null : new CosmeticAnimationLoader.AnimationVec3(cosmeticAnimationState.floatVal2, cosmeticAnimationState.floatVal3, cosmeticAnimationState.floatVal4);
   }

   private static float[] computeFloat3(JsonElement jsonElement, float[] floatArray) {
      if (jsonElement != null && jsonElement.isJsonObject()) {
         JsonObject asJsonObject = jsonElement.getAsJsonObject();
         if (asJsonObject.has("post")) {
            return computeFloat2(asJsonObject.get("post"), floatArray);
         }

         if (asJsonObject.has("pre")) {
            return computeFloat2(asJsonObject.get("pre"), floatArray);
         }

         if (asJsonObject.has("vector")) {
            return computeFloat2(asJsonObject.get("vector"), floatArray);
         }
      }

      return computeFloat2(jsonElement, floatArray);
   }

   private static String formatText(JsonObject jsonObject, String local6567_1, String local6567_2) {
      try {
         return jsonObject.has(local6567_1) ? jsonObject.get(local6567_1).getAsString() : local6567_2;
      } catch (Exception exception) {
         return local6567_2;
      }
   }

   public static boolean matchesCriteria2(CosmeticEntry cosmeticEntry) {
      if (map.containsKey(cosmeticEntry.id())) {
         return true;
      }

      CosmeticAnimationState cosmeticAnimationState = getCosmeticAnimationState(cosmeticEntry);
      return cosmeticAnimationState != null
         && CosmeticPreviewAtlas.matchesCriteria2(cosmeticEntry.textureId(), cosmeticEntry.textureResource(), () -> map.put(cosmeticEntry.id(), cosmeticAnimationState));
   }

   public record AnimationVec3(float x, float y, float z) {
   }
}
