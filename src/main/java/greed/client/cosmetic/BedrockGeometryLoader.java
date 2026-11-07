package greed.client.cosmetic;

import greed.client.cosmetic.BedrockBone;
import greed.client.cosmetic.BedrockVertex;
import greed.client.cosmetic.BedrockModel;
import greed.client.cosmetic.BedrockCube;
import greed.client.cosmetic.BedrockQuad;
import greed.client.cosmetic.ModelVec3;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.HashMap;

public class BedrockGeometryLoader {
   private static BedrockBone getBedrockBone(JsonObject jsonObject, int textureWidth, int textureHeight) {
      String asString = jsonObject.get("name").getAsString();
      BedrockBone currentBedrockBone = new BedrockBone(asString);
      if (jsonObject.has("pivot")) {
         JsonArray asJsonArray = jsonObject.getAsJsonArray("pivot");
         currentBedrockBone.floatVal = -asJsonArray.get(0).getAsFloat();
         currentBedrockBone.floatVal2 = asJsonArray.get(1).getAsFloat();
         currentBedrockBone.floatVal3 = asJsonArray.get(2).getAsFloat();
      }

      if (jsonObject.has("rotation")) {
         JsonArray asJsonArray2 = jsonObject.getAsJsonArray("rotation");
         currentBedrockBone.processData5((float)Math.toRadians(-asJsonArray2.get(0).getAsFloat()));
         currentBedrockBone.processData7((float)Math.toRadians(-asJsonArray2.get(1).getAsFloat()));
         currentBedrockBone.processData8((float)Math.toRadians(asJsonArray2.get(2).getAsFloat()));
      }

      if (jsonObject.has("cubes")) {
         for (JsonElement jsonElement : jsonObject.getAsJsonArray("cubes")) {
            JsonObject asJsonObject = jsonElement.getAsJsonObject();
            BedrockCube bedrockCube = getBedrockCube(asJsonObject, textureWidth, textureHeight);
            currentBedrockBone.list2.add(bedrockCube);
         }
      }

      return currentBedrockBone;
   }

   private static BedrockCube getBedrockCube(JsonObject jsonObject, int textureWidth, int textureHeight) {
      float[] floatArray = readFloatArray(jsonObject, "origin", new float[]{0.0F, 0.0F, 0.0F});
      float[] floatArray2 = readFloatArray(jsonObject, "size", new float[]{1.0F, 1.0F, 1.0F});
      float[] floatArray3 = readFloatArray(jsonObject, "pivot", (float[])floatArray.clone());
      float[] floatArray4 = readFloatArray(jsonObject, "rotation", new float[]{0.0F, 0.0F, 0.0F});
      float inflate = jsonObject.has("inflate") ? jsonObject.get("inflate").getAsFloat() : 0.0F;
      boolean mirror = jsonObject.has("mirror") && jsonObject.get("mirror").getAsBoolean();
      BedrockCube bedrockCube = new BedrockCube(floatArray2[0], floatArray2[1], floatArray2[2]);
      bedrockCube.modelVec32 = new ModelVec3(-floatArray3[0], floatArray3[1], floatArray3[2]);
      bedrockCube.modelVec33 = new ModelVec3(
         (float)Math.toRadians(-floatArray4[0]), (float)Math.toRadians(-floatArray4[1]), (float)Math.toRadians(floatArray4[2])
      );
      bedrockCube.floatVal = inflate;
      bedrockCube.flag = mirror;
      buildCubeQuads(bedrockCube, floatArray, floatArray2, inflate, mirror, jsonObject, textureWidth, textureHeight);
      return bedrockCube;
   }

   private static float[] readFloatArray(JsonObject jsonObject, String key, float[] floatArray) {
      if (!jsonObject.has(key)) {
         return floatArray;
      }

      JsonArray asJsonArray = jsonObject.getAsJsonArray(key);
      float[] floatArray2 = new float[asJsonArray.size()];

      for (int index = 0; index < asJsonArray.size(); index++) {
         floatArray2[index] = asJsonArray.get(index).getAsFloat();
      }

      return floatArray2;
   }

   private static BedrockQuad getBedrockQuad(
      JsonObject jsonObject,
      String faceName,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      float normalX,
      float normalY,
      float normalZ,
      int textureWidth,
      int textureHeight
   ) {
      if (!jsonObject.has(faceName)) {
         return null;
      }

      JsonObject asJsonObject = jsonObject.getAsJsonObject(faceName);
      JsonArray asJsonArray = asJsonObject.getAsJsonArray("uv");
      JsonArray asJsonArray2 = asJsonObject.getAsJsonArray("uv_size");
      float uvX = asJsonArray.get(0).getAsFloat() / textureWidth;
      float uvY = asJsonArray.get(1).getAsFloat() / textureHeight;
      float uvWidth = asJsonArray2.get(0).getAsFloat() / textureWidth;
      float uvHeight = asJsonArray2.get(1).getAsFloat() / textureHeight;
      BedrockVertex[] recoveredClass0057Array = getBedrockVertex(
         faceName, x1, y1, z1, x2, y2, z2, uvX, uvY, uvWidth, uvHeight
      );
      return new BedrockQuad(recoveredClass0057Array, normalX, normalY, normalZ);
   }

   private static BedrockModel getBedrockModel(JsonObject jsonObject) {
      JsonArray asJsonArray = jsonObject.getAsJsonArray("minecraft:geometry");
      if (asJsonArray != null && !asJsonArray.isEmpty()) {
         JsonObject asJsonObject = asJsonArray.get(0).getAsJsonObject();
         JsonObject asJsonObject2 = asJsonObject.getAsJsonObject("description");
         int textureWidth = asJsonObject2.has("texture_width") ? asJsonObject2.get("texture_width").getAsInt() : 64;
         int textureHeight = asJsonObject2.has("texture_height") ? asJsonObject2.get("texture_height").getAsInt() : 64;
         BedrockModel bedrockModel = new BedrockModel();
         bedrockModel.intVal = textureWidth;
         bedrockModel.intVal2 = textureHeight;
         JsonArray asJsonArray2 = asJsonObject.getAsJsonArray("bones");
         if (asJsonArray2 != null) {
            HashMap hashMap = new HashMap();

            for (JsonElement jsonElement : asJsonArray2) {
               JsonObject asJsonObject3 = jsonElement.getAsJsonObject();
               BedrockBone currentBedrockBone = getBedrockBone(asJsonObject3, textureWidth, textureHeight);
               hashMap.put(currentBedrockBone.strVal, currentBedrockBone);
            }

            for (JsonElement jsonElement2 : asJsonArray2) {
               JsonObject asJsonObject4 = jsonElement2.getAsJsonObject();
               String asString = asJsonObject4.get("name").getAsString();
               BedrockBone currentBedrockBone2 = (BedrockBone)hashMap.get(asString);
               if (asJsonObject4.has("parent")) {
                  String asString2 = asJsonObject4.get("parent").getAsString();
                  BedrockBone currentBedrockBone3 = (BedrockBone)hashMap.get(asString2);
                  if (currentBedrockBone3 != null) {
                     currentBedrockBone3.list.add(currentBedrockBone2);
                     currentBedrockBone2.bedrockBone = currentBedrockBone3;
                  }
               } else {
                  bedrockModel.list.add(currentBedrockBone2);
               }
            }
         }

         return bedrockModel;
      } else {
         System.err.println("[Cosmetics] No minecraft:geometry found in model");
         return null;
      }
   }

   private static void buildCubeQuads(
      BedrockCube bedrockCube,
      float[] floatArray,
      float[] floatArray2,
      float inflate,
      boolean mirror,
      JsonObject jsonObject,
      int textureWidth,
      int textureHeight
   ) {
      float originX = floatArray[0];
      float originY = floatArray[1];
      float originZ = floatArray[2];
      float sizeX = floatArray2[0];
      float sizeY = floatArray2[1];
      float sizeZ = floatArray2[2];
      originX -= inflate;
      originY -= inflate;
      originZ -= inflate;
      sizeX += inflate * 2.0F;
      sizeY += inflate * 2.0F;
      sizeZ += inflate * 2.0F;
      float uvX = 0.0F;
      float uvY = 0.0F;
      boolean hasUvObject = false;
      JsonObject jsonObject2 = null;
      if (jsonObject.has("uv")) {
         JsonElement jsonElement = jsonObject.get("uv");
         if (jsonElement.isJsonArray()) {
            JsonArray asJsonArray = jsonElement.getAsJsonArray();
            uvX = asJsonArray.get(0).getAsFloat();
            uvY = asJsonArray.get(1).getAsFloat();
         } else if (jsonElement.isJsonObject()) {
            hasUvObject = true;
            jsonObject2 = jsonElement.getAsJsonObject();
         }
      }

      float negMaxX = -(originX + sizeX) / 16.0F;
      float minY = originY / 16.0F;
      float minZ = originZ / 16.0F;
      float negMinX = -originX / 16.0F;
      float maxY = (originY + sizeY) / 16.0F;
      float maxZ = (originZ + sizeZ) / 16.0F;
      if (hasUvObject && jsonObject2 != null) {
         bedrockCube.recoveredClass0278Array[0] = getBedrockQuad(
            jsonObject2,
            "west",
            negMaxX,
            minY,
            minZ,
            negMaxX,
            maxY,
            maxZ,
            -1.0F,
            0.0F,
            0.0F,
            textureWidth,
            textureHeight
         );
         bedrockCube.recoveredClass0278Array[1] = getBedrockQuad(
            jsonObject2,
            "east",
            negMinX,
            minY,
            minZ,
            negMinX,
            maxY,
            maxZ,
            1.0F,
            0.0F,
            0.0F,
            textureWidth,
            textureHeight
         );
         bedrockCube.recoveredClass0278Array[2] = getBedrockQuad(
            jsonObject2,
            "down",
            negMaxX,
            minY,
            minZ,
            negMinX,
            minY,
            maxZ,
            0.0F,
            -1.0F,
            0.0F,
            textureWidth,
            textureHeight
         );
         bedrockCube.recoveredClass0278Array[3] = getBedrockQuad(
            jsonObject2, "up", negMaxX, maxY, minZ, negMinX, maxY, maxZ, 0.0F, 1.0F, 0.0F, textureWidth, textureHeight
         );
         bedrockCube.recoveredClass0278Array[4] = getBedrockQuad(
            jsonObject2,
            "north",
            negMaxX,
            minY,
            minZ,
            negMinX,
            maxY,
            minZ,
            0.0F,
            0.0F,
            -1.0F,
            textureWidth,
            textureHeight
         );
         bedrockCube.recoveredClass0278Array[5] = getBedrockQuad(
            jsonObject2,
            "south",
            negMaxX,
            minY,
            maxZ,
            negMinX,
            maxY,
            maxZ,
            0.0F,
            0.0F,
            1.0F,
            textureWidth,
            textureHeight
         );
      } else {
         float textureWidthFloat = textureWidth;
         float textureHeightFloat = textureHeight;
         bedrockCube.recoveredClass0278Array[0] = getBedrockQuad2(
            negMaxX,
            minY,
            minZ,
            negMaxX,
            maxY,
            maxZ,
            -1.0F,
            0.0F,
            0.0F,
            uvX,
            uvY,
            sizeZ,
            sizeY,
            sizeX,
            textureWidthFloat,
            textureHeightFloat,
            "west"
         );
         bedrockCube.recoveredClass0278Array[1] = getBedrockQuad2(
            negMinX,
            minY,
            minZ,
            negMinX,
            maxY,
            maxZ,
            1.0F,
            0.0F,
            0.0F,
            uvX,
            uvY,
            sizeZ,
            sizeY,
            sizeX,
            textureWidthFloat,
            textureHeightFloat,
            "east"
         );
         bedrockCube.recoveredClass0278Array[2] = getBedrockQuad2(
            negMaxX,
            minY,
            minZ,
            negMinX,
            minY,
            maxZ,
            0.0F,
            -1.0F,
            0.0F,
            uvX,
            uvY,
            sizeZ,
            sizeY,
            sizeX,
            textureWidthFloat,
            textureHeightFloat,
            "down"
         );
         bedrockCube.recoveredClass0278Array[3] = getBedrockQuad2(
            negMaxX,
            maxY,
            minZ,
            negMinX,
            maxY,
            maxZ,
            0.0F,
            1.0F,
            0.0F,
            uvX,
            uvY,
            sizeZ,
            sizeY,
            sizeX,
            textureWidthFloat,
            textureHeightFloat,
            "up"
         );
         bedrockCube.recoveredClass0278Array[4] = getBedrockQuad2(
            negMaxX,
            minY,
            minZ,
            negMinX,
            maxY,
            minZ,
            0.0F,
            0.0F,
            -1.0F,
            uvX,
            uvY,
            sizeZ,
            sizeY,
            sizeX,
            textureWidthFloat,
            textureHeightFloat,
            "north"
         );
         bedrockCube.recoveredClass0278Array[5] = getBedrockQuad2(
            negMaxX,
            minY,
            maxZ,
            negMinX,
            maxY,
            maxZ,
            0.0F,
            0.0F,
            1.0F,
            uvX,
            uvY,
            sizeZ,
            sizeY,
            sizeX,
            textureWidthFloat,
            textureHeightFloat,
            "south"
         );
      }
   }

   public static BedrockModel getBedrockModel2(String jsonString) {
      try {
         JsonObject asJsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
         return getBedrockModel(asJsonObject);
      } catch (Exception exception) {
         System.err.println("[Cosmetics] Failed to parse Bedrock model: " + exception);
         return null;
      }
   }

   private static BedrockQuad getBedrockQuad2(
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      float normalX,
      float normalY,
      float normalZ,
      float uvX,
      float uvY,
      float sizeZ,
      float sizeY,
      float sizeX,
      float textureWidth,
      float textureHeight,
      String faceName
   ) {
      float u;
      float v;
      float uvWidth;
      float uvHeight;
      switch (faceName) {
         case "north":
            u = (uvX + sizeZ + sizeX) / textureWidth;
            v = (uvY + sizeZ) / textureHeight;
            uvWidth = sizeX / textureWidth;
            uvHeight = sizeY / textureHeight;
            break;
         case "south":
            u = (uvX + sizeZ + sizeX + sizeZ) / textureWidth;
            v = (uvY + sizeZ) / textureHeight;
            uvWidth = sizeX / textureWidth;
            uvHeight = sizeY / textureHeight;
            break;
         case "east":
            u = uvX / textureWidth;
            v = (uvY + sizeZ) / textureHeight;
            uvWidth = sizeZ / textureWidth;
            uvHeight = sizeY / textureHeight;
            break;
         case "west":
            u = (uvX + sizeZ + sizeX) / textureWidth;
            v = (uvY + sizeZ) / textureHeight;
            uvWidth = sizeZ / textureWidth;
            uvHeight = sizeY / textureHeight;
            break;
         case "up":
            u = (uvX + sizeZ) / textureWidth;
            v = uvY / textureHeight;
            uvWidth = sizeX / textureWidth;
            uvHeight = sizeZ / textureHeight;
            break;
         case "down":
            u = (uvX + sizeZ + sizeX) / textureWidth;
            v = uvY / textureHeight;
            uvWidth = sizeX / textureWidth;
            uvHeight = sizeZ / textureHeight;
            break;
         default:
            u = 0.0F;
            v = 0.0F;
            uvWidth = 0.0F;
            uvHeight = 0.0F;
      }

      BedrockVertex[] recoveredClass0057Array = getBedrockVertex(
         faceName, x1, y1, z1, x2, y2, z2, u, v, uvWidth, uvHeight
      );
      return new BedrockQuad(recoveredClass0057Array, normalX, normalY, normalZ);
   }

   private static BedrockVertex[] getBedrockVertex(
      String faceName,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      float u,
      float v,
      float uvWidth,
      float uvHeight
   ) {
      BedrockVertex[] recoveredClass0057Array = new BedrockVertex[4];
      float u2 = u + uvWidth;
      float v2 = v + uvHeight;
      switch (faceName) {
         case "north":
            recoveredClass0057Array[0] = new BedrockVertex(x2, y2, z1, u, v);
            recoveredClass0057Array[1] = new BedrockVertex(x1, y2, z1, u2, v);
            recoveredClass0057Array[2] = new BedrockVertex(x1, y1, z1, u2, v2);
            recoveredClass0057Array[3] = new BedrockVertex(x2, y1, z1, u, v2);
            break;
         case "south":
            recoveredClass0057Array[0] = new BedrockVertex(x1, y2, z2, u, v);
            recoveredClass0057Array[1] = new BedrockVertex(x2, y2, z2, u2, v);
            recoveredClass0057Array[2] = new BedrockVertex(x2, y1, z2, u2, v2);
            recoveredClass0057Array[3] = new BedrockVertex(x1, y1, z2, u, v2);
            break;
         case "east":
            recoveredClass0057Array[0] = new BedrockVertex(x2, y2, z2, u, v);
            recoveredClass0057Array[1] = new BedrockVertex(x2, y2, z1, u2, v);
            recoveredClass0057Array[2] = new BedrockVertex(x2, y1, z1, u2, v2);
            recoveredClass0057Array[3] = new BedrockVertex(x2, y1, z2, u, v2);
            break;
         case "west":
            recoveredClass0057Array[0] = new BedrockVertex(x1, y2, z1, u, v);
            recoveredClass0057Array[1] = new BedrockVertex(x1, y2, z2, u2, v);
            recoveredClass0057Array[2] = new BedrockVertex(x1, y1, z2, u2, v2);
            recoveredClass0057Array[3] = new BedrockVertex(x1, y1, z1, u, v2);
            break;
         case "up":
            recoveredClass0057Array[0] = new BedrockVertex(x1, y2, z1, u, v);
            recoveredClass0057Array[1] = new BedrockVertex(x1, y2, z2, u, v2);
            recoveredClass0057Array[2] = new BedrockVertex(x2, y2, z2, u2, v2);
            recoveredClass0057Array[3] = new BedrockVertex(x2, y2, z1, u2, v);
            break;
         case "down":
            recoveredClass0057Array[0] = new BedrockVertex(x2, y1, z1, u, v);
            recoveredClass0057Array[1] = new BedrockVertex(x2, y1, z2, u, v2);
            recoveredClass0057Array[2] = new BedrockVertex(x1, y1, z2, u2, v2);
            recoveredClass0057Array[3] = new BedrockVertex(x1, y1, z1, u2, v);
      }

      return recoveredClass0057Array;
   }
}
