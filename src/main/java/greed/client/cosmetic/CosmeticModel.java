package greed.client.cosmetic;

import greed.client.GreedClient;
import greed.client.cosmetic.CosmeticWarmup;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.model.json.Transformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class CosmeticModel {
   private static final Map<Integer, CosmeticModel> modelCache = new ConcurrentHashMap<>();
   private static final String[] faceDirections = new String[]{"north", "south", "east", "west", "up", "down"};
   private static final float[] zeroVector = new float[]{0.0F, 0.0F, 0.0F};
   private static final float[] oneVector = new float[]{1.0F, 1.0F, 1.0F};
   private final ArrayList<CosmeticModel.TextureDefinition> textures;
   private final ArrayList<CosmeticModel.BoneDefinition> bones;
   private final CosmeticModel.AnimationDefinition idleAnimation;
   private final CosmeticModel.AnimationDefinition walkAnimation;
   private final float[] displayHead;
   private final HashMap<String, Transformation> displayTransformations;
   private final float[] bounds = new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
   private final Vector3fc[] boundingCorners;
   private final boolean hasAnimations;
   private static final MatrixStack scratchMatrixStack = new MatrixStack();
   private static final Quaternionf scratchQuaternion = new Quaternionf();
   private final CosmeticModel.BoneDefinition[] bonesByIndex;
   private final CosmeticModel.KeyframeDefinition[][][] idleKeyframesByBone;
   private final CosmeticModel.KeyframeDefinition[][][] walkKeyframesByBone;
   private final float[] boneTransforms;
   private final float[][] idleChannels = new float[3][3];
   private final float[][] walkChannels = new float[3][3];
   private final Quaternionf displayRotation;
   private float lastAnimationTime;
   private float lastInterpolation;
   private boolean transformsDirty;
   private int frameColumn;
   private int frameRow;

   private void renderModel(MatrixStack matrixStack, VertexConsumer vertexConsumer, int light, float animationTime, float interpolation, int textureIndex) {
      this.updateBoneTransforms(animationTime, interpolation);
      CosmeticModel.AtlasDefinition atlas = this.textures.get(textureIndex).animation();
      if (atlas != null) {
         int frameIndex = atlas.frameAt((int)(animationTime * 20.0F));
         this.frameColumn = frameIndex % atlas.columns();
         this.frameRow = frameIndex / atlas.columns();
      }

      for (CosmeticModel.BoneDefinition bone : this.bones) {
         this.renderBone(bone, matrixStack, vertexConsumer, light, atlas, textureIndex);
      }
   }

   private static float[] parseFloatArray(JsonArray jsonArray, int size) {
      float[] floatArray = new float[size];

      for (int currentIndex = 0; currentIndex < Math.min(size, jsonArray.size()); currentIndex++) {
         floatArray[currentIndex] = jsonArray.get(currentIndex).getAsFloat();
      }

      return floatArray;
   }

   private static void collectBones(CosmeticModel.BoneDefinition bone, ArrayList<CosmeticModel.BoneDefinition> bones) {
      bones.add(bone);

      for (CosmeticModel.BoneDefinition child : bone.children()) {
         collectBones(child, bones);
      }
   }

   private static float[] interpolateKeyframes(
      CosmeticModel.KeyframeDefinition[] keyframes, float animationTime, float length, float[] floatArray, float[] floatArray2
   ) {
      if (keyframes.length == 0) {
         return floatArray;
      }

      if (keyframes.length == 1) {
         return keyframes[0].value();
      }

      CosmeticModel.KeyframeDefinition fromKeyframe = keyframes[keyframes.length - 1];
      CosmeticModel.KeyframeDefinition toKeyframe = keyframes[0];
      float fromTime = fromKeyframe.time() - length;
      float toTime = toKeyframe.time();

      for (int currentIndex = 0; currentIndex < keyframes.length - 1; currentIndex++) {
         if (animationTime >= keyframes[currentIndex].time() && animationTime <= keyframes[currentIndex + 1].time()) {
            fromKeyframe = keyframes[currentIndex];
            toKeyframe = keyframes[currentIndex + 1];
            fromTime = fromKeyframe.time();
            toTime = toKeyframe.time();
            break;
         }
      }

      if (animationTime > keyframes[keyframes.length - 1].time()) {
         fromKeyframe = keyframes[keyframes.length - 1];
         toKeyframe = keyframes[0];
         fromTime = fromKeyframe.time();
         toTime = toKeyframe.time() + length;
      }

      if (fromKeyframe.step()) {
         return fromKeyframe.value();
      }

      float sampleTime = animationTime < fromTime ? animationTime + length : animationTime;
      float progress = toTime == fromTime ? 0.0F : MathHelper.clamp((sampleTime - fromTime) / (toTime - fromTime), 0.0F, 1.0F);
      floatArray2[0] = MathHelper.lerp(progress, fromKeyframe.value()[0], toKeyframe.value()[0]);
      floatArray2[1] = MathHelper.lerp(progress, fromKeyframe.value()[1], toKeyframe.value()[1]);
      floatArray2[2] = MathHelper.lerp(progress, fromKeyframe.value()[2], toKeyframe.value()[2]);
      return floatArray2;
   }

   public static boolean matchesCriteria(CosmeticEntry cosmeticEntry) {
      if (modelCache.containsKey(cosmeticEntry.id())) {
         return true;
      }

      try {
         JsonObject asJsonObject = JsonParser.parseString(new String(CosmeticPreviewAtlas.getByte(cosmeticEntry.modelResource()), StandardCharsets.UTF_8))
            .getAsJsonObject();
         ArrayList<CosmeticModel.TextureDefinition> textures = new ArrayList<>();

         for (JsonElement jsonElement : asJsonObject.getAsJsonArray("textures")) {
            JsonObject asJsonObject2 = jsonElement.getAsJsonObject();
            String asString = asJsonObject2.get("file").getAsString();
            CosmeticModel.AtlasDefinition atlas = null;
            if (asJsonObject2.has("animation")) {
               JsonObject asJsonObject3 = asJsonObject2.getAsJsonObject("animation");
               JsonArray asJsonArray = asJsonObject3.getAsJsonArray("frames");
               int[] intArray = new int[asJsonArray.size()];
               int[] intArray2 = new int[asJsonArray.size()];
               int totalTicks = 0;

               for (int currentIndex = 0; currentIndex < asJsonArray.size(); currentIndex++) {
                  intArray[currentIndex] = asJsonArray.get(currentIndex).getAsJsonArray().get(0).getAsInt();
                  intArray2[currentIndex] = Math.max(1, asJsonArray.get(currentIndex).getAsJsonArray().get(1).getAsInt());
                  totalTicks += intArray2[currentIndex];
               }

               atlas = new CosmeticModel.AtlasDefinition(
                  asJsonObject3.get("columns").getAsInt(),
                  asJsonObject3.get("rows").getAsInt(),
                  asJsonObject3.get("imageWidth").getAsInt(),
                  asJsonObject3.get("imageHeight").getAsInt(),
                  intArray,
                  intArray2,
                  totalTicks
               );
            }

            textures.add(
               new CosmeticModel.TextureDefinition(
                  Identifier.of("greed", "cosmetics/" + asString),
                  asString,
                  asJsonObject2.get("uvWidth").getAsFloat(),
                  asJsonObject2.get("uvHeight").getAsFloat(),
                  atlas
               )
            );
         }

         ArrayList<CosmeticModel.BoneDefinition> bones = new ArrayList<>();
         int[] intArray3 = new int[]{0};

         for (JsonElement jsonElement2 : asJsonObject.getAsJsonArray("bones")) {
            bones.add(parseBone(jsonElement2.getAsJsonObject(), zeroVector, textures, intArray3));
         }

         JsonObject jsonObject = asJsonObject.has("animations") ? asJsonObject.getAsJsonObject("animations") : new JsonObject();
         HashMap<String, Transformation> displayTransformations = new HashMap<>();
         if (asJsonObject.has("display")) {
            for (Entry<String, JsonElement> entry : asJsonObject.getAsJsonObject("display").entrySet()) {
               float[] floatArray = parseFloatArray(entry.getValue().getAsJsonArray(), 9);
               displayTransformations.put(
                  entry.getKey(),
                  new Transformation(
                     new Vector3f(floatArray[0], floatArray[1], floatArray[2]),
                     new Vector3f(floatArray[3], floatArray[4], floatArray[5]),
                     new Vector3f(floatArray[6], floatArray[7], floatArray[8])
                  )
               );
            }
         }

         CosmeticModel cosmeticModel = new CosmeticModel(
            textures,
            bones,
            jsonObject.has("idle") ? parseAnimation(jsonObject.getAsJsonObject("idle")) : null,
            jsonObject.has("walk") ? parseAnimation(jsonObject.getAsJsonObject("walk")) : null,
            asJsonObject.has("displayHead") ? parseFloatArray(asJsonObject.getAsJsonArray("displayHead"), 9) : null,
            displayTransformations
         );

         for (int currentIndex2 = 0; currentIndex2 < textures.size(); currentIndex2++) {
            if (!CosmeticPreviewAtlas.matchesCriteria2(
               textures.get(currentIndex2).id(),
               textures.get(currentIndex2).file(),
               currentIndex2 == textures.size() - 1 ? () -> modelCache.put(cosmeticEntry.id(), cosmeticModel) : null
            )) {
               return false;
            }
         }

         return !textures.isEmpty();
      } catch (Exception exception) {
         GreedClient.logger().warn("[Cosmetics] model {} not loaded: {}", cosmeticEntry.modelResource(), exception.toString());
         return false;
      }
   }

   private static void applyRotation(MatrixStack matrixStack, float rotationX, float rotationY, float rotationZ) {
      if (rotationZ != 0.0F) {
         matrixStack.multiply(scratchQuaternion.rotationZ(rotationZ * (float) (Math.PI / 180.0)));
      }

      if (rotationY != 0.0F) {
         matrixStack.multiply(scratchQuaternion.rotationY(rotationY * (float) (Math.PI / 180.0)));
      }

      if (rotationX != 0.0F) {
         matrixStack.multiply(scratchQuaternion.rotationX(rotationX * (float) (Math.PI / 180.0)));
      }
   }

   public void renderElement3(MatrixStack matrixStack) {
      if (this.displayHead != null) {
         matrixStack.translate(this.displayHead[3], this.displayHead[4], this.displayHead[5]);
         matrixStack.multiply(this.displayRotation);
         matrixStack.scale(this.displayHead[6], this.displayHead[7], this.displayHead[8]);
      }

      matrixStack.translate(-0.5F, -0.5F, -0.5F);
   }

   public void renderElement4(MatrixStack matrixStack, ItemDisplayContext itemDisplayContext) {
      float modelWidth = Math.max(1.0F, this.bounds[3] - this.bounds[0]);
      float modelHeight = Math.max(1.0F, this.bounds[4] - this.bounds[1]);
      float fitScale = Math.min(18.0F / modelWidth, 22.0F / modelHeight);
      matrixStack.translate((itemDisplayContext.isLeftHand() ? -1.5F : 1.5F) / 16.0F, (itemDisplayContext.isFirstPerson() ? 1.5F : 0.0F) / 16.0F, -0.125F);
      matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(itemDisplayContext.isLeftHand() ? 90.0F : -90.0F));
      matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(itemDisplayContext.isLeftHand() ? -8.0F : 8.0F));
      matrixStack.scale(fitScale, fitScale, fitScale);
      matrixStack.translate(
         -(this.bounds[0] + this.bounds[3]) / 32.0F,
         -(this.bounds[1] + this.bounds[4]) / 32.0F,
         -(this.bounds[2] + this.bounds[5]) / 32.0F
      );
   }

   private CosmeticModel(
      ArrayList<CosmeticModel.TextureDefinition> textures,
      ArrayList<CosmeticModel.BoneDefinition> bones,
      CosmeticModel.AnimationDefinition idleAnimation,
      CosmeticModel.AnimationDefinition walkAnimation,
      float[] floatArray,
      HashMap<String, Transformation> displayTransformations
   ) {
      this.textures = textures;
      this.bones = bones;
      this.idleAnimation = idleAnimation;
      this.walkAnimation = walkAnimation;
      this.displayHead = floatArray;
      this.displayTransformations = displayTransformations;
      ArrayList<CosmeticModel.BoneDefinition> allBones = new ArrayList<>();

      for (CosmeticModel.BoneDefinition bone : bones) {
         collectBones(bone, allBones);
      }

      int boneCount = 0;

      for (CosmeticModel.BoneDefinition bone : allBones) {
         boneCount = Math.max(boneCount, bone.index() + 1);
      }

      this.bonesByIndex = new CosmeticModel.BoneDefinition[boneCount];
      this.idleKeyframesByBone = new CosmeticModel.KeyframeDefinition[boneCount][][];
      this.walkKeyframesByBone = new CosmeticModel.KeyframeDefinition[boneCount][][];

      for (CosmeticModel.BoneDefinition bone : allBones) {
         this.bonesByIndex[bone.index()] = bone;
         this.idleKeyframesByBone[bone.index()] = idleAnimation == null ? null : idleAnimation.tracksByBone().get(bone.uuid());
         this.walkKeyframesByBone[bone.index()] = walkAnimation == null ? null : walkAnimation.tracksByBone().get(bone.uuid());
      }

      this.boneTransforms = new float[boneCount * 9];
      this.displayRotation = floatArray == null
         ? null
         : new Quaternionf()
            .rotationXYZ(floatArray[0] * (float) (Math.PI / 180.0), floatArray[1] * (float) (Math.PI / 180.0), floatArray[2] * (float) (Math.PI / 180.0));

      for (CosmeticModel.BoneDefinition bone : bones) {
         this.expandBounds(bone);
      }

      if (this.bounds[0] == Float.MAX_VALUE) {
         System.arraycopy(new float[]{0.0F, 0.0F, 0.0F, 16.0F, 16.0F, 16.0F}, 0, this.bounds, 0, 6);
      }

      this.boundingCorners = new Vector3fc[8];

      for (int currentIndex = 0; currentIndex < 8; currentIndex++) {
         this.boundingCorners[currentIndex] = new Vector3f(
            this.bounds[(currentIndex & 1) == 0 ? 0 : 3] / 16.0F,
            this.bounds[(currentIndex & 2) == 0 ? 1 : 4] / 16.0F,
            this.bounds[(currentIndex & 4) == 0 ? 2 : 5] / 16.0F
         );
      }

      this.hasAnimations = textures.stream().anyMatch(texture -> texture.animation() != null);
   }

   public boolean checkState() {
      return this.hasAnimations;
   }

   public Transformation getTransformation(ItemDisplayContext itemDisplayContext) {
      return this.displayTransformations.getOrDefault(itemDisplayContext.asString(), Transformation.IDENTITY);
   }

   public void renderElement5(MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int light, float animationTime, float interpolation) {
      for (int currentIndex = 0; currentIndex < this.textures.size(); currentIndex++) {
         int currentIndex2 = currentIndex;
         orderedRenderCommandQueue.submitCustom(matrixStack, RenderLayers.entityCutoutNoCull(this.textures.get(currentIndex).id()), (entry, vertexConsumer) -> {
            MatrixStack matrixStack2 = scratchMatrixStack;
            matrixStack2.peek().getPositionMatrix().set(entry.getPositionMatrix());
            matrixStack2.peek().getNormalMatrix().set(entry.getNormalMatrix());
            this.renderModel(matrixStack2, vertexConsumer, light, animationTime, interpolation, currentIndex2);
         });
      }
   }

   private static CosmeticModel.AnimationDefinition parseAnimation(JsonObject jsonObject) {
      HashMap<String, CosmeticModel.KeyframeDefinition[][]> tracksByBone = new HashMap<>();

      for (Entry<String, JsonElement> trackEntry : jsonObject.getAsJsonObject("tracks").entrySet()) {
         ArrayList<CosmeticModel.KeyframeDefinition> positionKeyframes = new ArrayList<>();
         ArrayList<CosmeticModel.KeyframeDefinition> rotationKeyframes = new ArrayList<>();
         ArrayList<CosmeticModel.KeyframeDefinition> scaleKeyframes = new ArrayList<>();

         for (JsonElement jsonElement : trackEntry.getValue().getAsJsonArray()) {
            JsonObject asJsonObject = jsonElement.getAsJsonObject();
            CosmeticModel.KeyframeDefinition keyframe = new CosmeticModel.KeyframeDefinition(
               asJsonObject.get("time").getAsFloat(), parseFloatArray(asJsonObject.getAsJsonArray("value"), 3), asJsonObject.get("step").getAsBoolean()
            );
            switch (asJsonObject.get("channel").getAsString()) {
               case "position":
                  positionKeyframes.add(keyframe);
                  break;
               case "scale":
                  scaleKeyframes.add(keyframe);
                  break;
               default:
                  rotationKeyframes.add(keyframe);
            }
         }

         tracksByBone.put(
            trackEntry.getKey(),
            new CosmeticModel.KeyframeDefinition[][]{
               positionKeyframes.toArray(CosmeticModel.KeyframeDefinition[]::new),
               rotationKeyframes.toArray(CosmeticModel.KeyframeDefinition[]::new),
               scaleKeyframes.toArray(CosmeticModel.KeyframeDefinition[]::new)
            }
         );
      }

      return new CosmeticModel.AnimationDefinition(jsonObject.get("length").getAsFloat(), tracksByBone);
   }

   public void renderElement6(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, float animationTime, float interpolation) {
      for (int currentIndex = 0; currentIndex < this.textures.size(); currentIndex++) {
         VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(RenderLayers.entityCutoutNoCull(this.textures.get(currentIndex).id()));
         this.renderModel(matrixStack, vertexConsumer, light, animationTime, interpolation, currentIndex);
      }
   }

   private static void sampleKeyframe(CosmeticModel.KeyframeDefinition[] keyframes, float animationTime, float length, float[] floatArray, float[] floatArray2) {
      float[] floatArray3 = interpolateKeyframes(keyframes, animationTime, length, floatArray, floatArray2);
      if (floatArray3 != floatArray2) {
         System.arraycopy(floatArray3, 0, floatArray2, 0, 3);
      }
   }

   private void renderBone(
      CosmeticModel.BoneDefinition bone,
      MatrixStack matrixStack,
      VertexConsumer vertexConsumer,
      int light,
      CosmeticModel.AtlasDefinition atlas,
      int textureIndex
   ) {
      float[] floatArray = this.boneTransforms;
      int boneOffset = bone.index() * 9;
      matrixStack.push();
      matrixStack.translate(floatArray[boneOffset], floatArray[boneOffset + 1], floatArray[boneOffset + 2]);
      applyRotation(matrixStack, floatArray[boneOffset + 3], floatArray[boneOffset + 4], floatArray[boneOffset + 5]);
      matrixStack.scale(floatArray[boneOffset + 6], floatArray[boneOffset + 7], floatArray[boneOffset + 8]);

      for (CosmeticModel.CubeDefinition cube : bone.cubes()) {
         this.renderCube(cube, matrixStack, vertexConsumer, light, atlas, textureIndex);
      }

      for (CosmeticModel.BoneDefinition child : bone.children()) {
         this.renderBone(child, matrixStack, vertexConsumer, light, atlas, textureIndex);
      }

      matrixStack.pop();
   }

   public static CosmeticModel getCosmeticModel(CosmeticEntry cosmeticEntry) {
      CosmeticModel cosmeticModel = modelCache.get(cosmeticEntry.id());
      if (cosmeticModel == null) {
         CosmeticWarmup.processData(cosmeticEntry);
      }

      return cosmeticModel;
   }

   private void updateBoneTransforms(float animationTime, float interpolation) {
      if (!this.transformsDirty || this.lastAnimationTime != animationTime || this.lastInterpolation != interpolation) {
         float idleTime = this.idleAnimation != null && !(this.idleAnimation.length() <= 0.0F) ? animationTime % this.idleAnimation.length() : 0.0F;
         float walkTime = this.walkAnimation != null && !(this.walkAnimation.length() <= 0.0F) ? animationTime % this.walkAnimation.length() : 0.0F;
         float[] floatArray = this.boneTransforms;
         float[] floatArray2 = this.idleChannels[0];
         float[] floatArray3 = this.idleChannels[1];
         float[] floatArray4 = this.idleChannels[2];
         float[] floatArray5 = this.walkChannels[0];
         float[] floatArray6 = this.walkChannels[1];
         float[] floatArray7 = this.walkChannels[2];

         for (int currentIndex = 0; currentIndex < this.bonesByIndex.length; currentIndex++) {
            CosmeticModel.BoneDefinition bone = this.bonesByIndex[currentIndex];
            CosmeticModel.KeyframeDefinition[][] idleKeyframes = this.idleKeyframesByBone[currentIndex];
            CosmeticModel.KeyframeDefinition[][] walkKeyframes = this.walkKeyframesByBone[currentIndex];
            if (idleKeyframes != null) {
               sampleKeyframe(idleKeyframes[0], idleTime, this.idleAnimation.length(), zeroVector, floatArray2);
               sampleKeyframe(idleKeyframes[1], idleTime, this.idleAnimation.length(), zeroVector, floatArray3);
               sampleKeyframe(idleKeyframes[2], idleTime, this.idleAnimation.length(), oneVector, floatArray4);
            } else {
               System.arraycopy(zeroVector, 0, floatArray2, 0, 3);
               System.arraycopy(zeroVector, 0, floatArray3, 0, 3);
               System.arraycopy(oneVector, 0, floatArray4, 0, 3);
            }

            if (walkKeyframes != null) {
               sampleKeyframe(walkKeyframes[0], walkTime, this.walkAnimation.length(), zeroVector, floatArray5);
               sampleKeyframe(walkKeyframes[1], walkTime, this.walkAnimation.length(), zeroVector, floatArray6);
               sampleKeyframe(walkKeyframes[2], walkTime, this.walkAnimation.length(), oneVector, floatArray7);
            } else {
               System.arraycopy(zeroVector, 0, floatArray5, 0, 3);
               System.arraycopy(zeroVector, 0, floatArray6, 0, 3);
               System.arraycopy(oneVector, 0, floatArray7, 0, 3);
            }

            int boneOffset = currentIndex * 9;

            for (int currentIndex2 = 0; currentIndex2 < 3; currentIndex2++) {
               floatArray[boneOffset + currentIndex2] = (
                     bone.offset()[currentIndex2] + MathHelper.lerp(interpolation, floatArray2[currentIndex2], floatArray5[currentIndex2])
                  )
                  / 16.0F;
               floatArray[boneOffset + 3 + currentIndex2] = bone.rotation()[currentIndex2]
                  + MathHelper.lerp(interpolation, floatArray3[currentIndex2], floatArray6[currentIndex2]);
               floatArray[boneOffset + 6 + currentIndex2] = MathHelper.lerp(interpolation, floatArray4[currentIndex2], floatArray7[currentIndex2]);
            }
         }

         this.lastAnimationTime = animationTime;
         this.lastInterpolation = interpolation;
         this.transformsDirty = true;
      }
   }

   private void expandBounds(CosmeticModel.BoneDefinition bone) {
      for (CosmeticModel.CubeDefinition cube : bone.cubes()) {
         for (int currentIndex = 0; currentIndex < 3; currentIndex++) {
            this.bounds[currentIndex] = Math.min(this.bounds[currentIndex], Math.min(cube.from()[currentIndex], cube.to()[currentIndex]));
            this.bounds[currentIndex + 3] = Math.max(
               this.bounds[currentIndex + 3], Math.max(cube.from()[currentIndex], cube.to()[currentIndex])
            );
         }
      }

      for (CosmeticModel.BoneDefinition child : bone.children()) {
         this.expandBounds(child);
      }
   }

   public Vector3fc[] getVector3fc() {
      return this.boundingCorners;
   }

   private static CosmeticModel.BoneDefinition parseBone(
      JsonObject jsonObject, float[] floatArray, ArrayList<CosmeticModel.TextureDefinition> textures, int[] intArray
   ) {
      int boneIndex = (int)(intArray[0]++);
      float[] floatArray2 = parseFloatArray(jsonObject.getAsJsonArray("origin"), 3);
      ArrayList<CosmeticModel.CubeDefinition> cubes = new ArrayList<>();

      for (JsonElement jsonElement : jsonObject.getAsJsonArray("cubes")) {
         JsonObject asJsonObject = jsonElement.getAsJsonObject();
         float[] floatArray3 = parseFloatArray(asJsonObject.getAsJsonArray("from"), 3);
         float[] floatArray4 = parseFloatArray(asJsonObject.getAsJsonArray("to"), 3);
         float[] floatArray5 = parseFloatArray(asJsonObject.getAsJsonArray("origin"), 3);
         float[] floatArray6 = parseFloatArray(asJsonObject.getAsJsonArray("rotation"), 3);
         float minX = (Math.min(floatArray3[0], floatArray4[0]) - floatArray5[0]) / 16.0F;
         float minY = (Math.min(floatArray3[1], floatArray4[1]) - floatArray5[1]) / 16.0F;
         float minZ = (Math.min(floatArray3[2], floatArray4[2]) - floatArray5[2]) / 16.0F;
         float maxX = (Math.max(floatArray3[0], floatArray4[0]) - floatArray5[0]) / 16.0F;
         float maxY = (Math.max(floatArray3[1], floatArray4[1]) - floatArray5[1]) / 16.0F;
         float maxZ = (Math.max(floatArray3[2], floatArray4[2]) - floatArray5[2]) / 16.0F;
         boolean thinX = maxX - minX < 5.0E-4F;
         boolean thinY = maxY - minY < 5.0E-4F;
         boolean thinZ = maxZ - minZ < 5.0E-4F;
         boolean[] booleanArray = new boolean[textures.size()];
         ArrayList<CosmeticModel.FaceUvDefinition> faces = new ArrayList<>();

         for (JsonElement jsonElement2 : asJsonObject.getAsJsonArray("faces")) {
            JsonObject asJsonObject2 = jsonElement2.getAsJsonObject();
            float[] floatArray7 = parseFloatArray(asJsonObject2.getAsJsonArray("uv"), 4);
            int direction = 0;

            while (direction < 5 && !faceDirections[direction].equals(asJsonObject2.get("dir").getAsString())) {
               direction++;
            }

            int asInt = asJsonObject2.get("texture").getAsInt();
            boolean hasTexture = asInt >= 0 && asInt < textures.size();
            boolean skip = !hasTexture
               || (direction <= 1 ? thinX || thinY : (direction <= 3 ? thinY || thinZ : thinX || thinZ));
            float uvWidth = hasTexture ? textures.get(asInt).uvWidth() : 1.0F;
            float uvHeight = hasTexture ? textures.get(asInt).uvHeight() : 1.0F;
            float normalX = 0.0F;
            float normalY = 0.0F;
            float normalZ = 0.0F;
            float[] floatArray8;
            switch (direction) {
               case 0:
                  floatArray8 = new float[]{
                     maxX,
                     maxY,
                     minZ,
                     maxX,
                     minY,
                     minZ,
                     minX,
                     minY,
                     minZ,
                     minX,
                     maxY,
                     minZ
                  };
                  normalZ = -1.0F;
                  break;
               case 1:
                  floatArray8 = new float[]{
                     minX,
                     maxY,
                     maxZ,
                     minX,
                     minY,
                     maxZ,
                     maxX,
                     minY,
                     maxZ,
                     maxX,
                     maxY,
                     maxZ
                  };
                  normalZ = 1.0F;
                  break;
               case 2:
                  floatArray8 = new float[]{
                     maxX,
                     maxY,
                     maxZ,
                     maxX,
                     minY,
                     maxZ,
                     maxX,
                     minY,
                     minZ,
                     maxX,
                     maxY,
                     minZ
                  };
                  normalX = 1.0F;
                  break;
               case 3:
                  floatArray8 = new float[]{
                     minX,
                     maxY,
                     minZ,
                     minX,
                     minY,
                     minZ,
                     minX,
                     minY,
                     maxZ,
                     minX,
                     maxY,
                     maxZ
                  };
                  normalX = -1.0F;
                  break;
               case 4:
                  floatArray8 = new float[]{
                     minX,
                     maxY,
                     minZ,
                     minX,
                     maxY,
                     maxZ,
                     maxX,
                     maxY,
                     maxZ,
                     maxX,
                     maxY,
                     minZ
                  };
                  normalY = 1.0F;
                  break;
               default:
                  floatArray8 = new float[]{
                     minX,
                     minY,
                     maxZ,
                     minX,
                     minY,
                     minZ,
                     maxX,
                     minY,
                     minZ,
                     maxX,
                     minY,
                     maxZ
                  };
                  normalY = -1.0F;
            }

            if (!skip) {
               booleanArray[asInt] = true;
            }

            faces.add(
               new CosmeticModel.FaceUvDefinition(
                  direction,
                  floatArray7[0] / uvWidth,
                  floatArray7[1] / uvHeight,
                  floatArray7[2] / uvWidth,
                  floatArray7[3] / uvHeight,
                  Math.floorMod(asJsonObject2.get("rotation").getAsInt() / 90, 4),
                  asInt,
                  skip,
                  floatArray8,
                  normalX,
                  normalY,
                  normalZ
               )
            );
         }

         Quaternionf quaternionf = floatArray6[0] == 0.0F && floatArray6[1] == 0.0F && floatArray6[2] == 0.0F
            ? null
            : new Quaternionf()
               .rotationZ(floatArray6[2] * (float) (Math.PI / 180.0))
               .rotateY(floatArray6[1] * (float) (Math.PI / 180.0))
               .rotateX(floatArray6[0] * (float) (Math.PI / 180.0));
         float[] floatArray9 = new float[]{
            (floatArray5[0] - floatArray2[0]) / 16.0F, (floatArray5[1] - floatArray2[1]) / 16.0F, (floatArray5[2] - floatArray2[2]) / 16.0F
         };
         cubes.add(
            new CosmeticModel.CubeDefinition(floatArray3, floatArray4, floatArray5, floatArray6, faces, floatArray9, quaternionf, booleanArray)
         );
      }

      ArrayList<CosmeticModel.BoneDefinition> children = new ArrayList<>();

      for (JsonElement jsonElement3 : jsonObject.getAsJsonArray("children")) {
         children.add(parseBone(jsonElement3.getAsJsonObject(), floatArray2, textures, intArray));
      }

      float[] floatArray10 = new float[]{floatArray2[0] - floatArray[0], floatArray2[1] - floatArray[1], floatArray2[2] - floatArray[2]};
      return new CosmeticModel.BoneDefinition(
         boneIndex,
         jsonObject.get("uuid").getAsString(),
         floatArray2,
         floatArray10,
         parseFloatArray(jsonObject.getAsJsonArray("rotation"), 3),
         cubes,
         children
      );
   }

   private void renderCube(
      CosmeticModel.CubeDefinition cube,
      MatrixStack matrixStack,
      VertexConsumer vertexConsumer,
      int light,
      CosmeticModel.AtlasDefinition atlas,
      int textureIndex
   ) {
      if (cube.hasTexture()[textureIndex]) {
         matrixStack.push();
         matrixStack.translate(cube.offset()[0], cube.offset()[1], cube.offset()[2]);
         if (cube.rotationQuaternion() != null) {
            matrixStack.multiply(cube.rotationQuaternion());
         }

         net.minecraft.client.util.math.MatrixStack.Entry entry = matrixStack.peek();

         for (CosmeticModel.FaceUvDefinition face : cube.faces()) {
            if (face.texture() == textureIndex && !face.skip()) {
               float u1 = face.u1();
               float v1 = face.v1();
               float u2 = face.u2();
               float v2 = face.v2();
               if (atlas != null) {
                  u1 = (this.frameColumn + u1) / atlas.columns();
                  u2 = (this.frameColumn + u2) / atlas.columns();
                  v1 = (this.frameRow + v1) / atlas.rows();
                  v2 = (this.frameRow + v2) / atlas.rows();
                  float texelInsetU = 0.5F / atlas.imageWidth() * Math.signum(u2 - u1);
                  float texelInsetV = 0.5F / atlas.imageHeight() * Math.signum(v2 - v1);
                  u1 += texelInsetU;
                  u2 -= texelInsetU;
                  v1 += texelInsetV;
                  v2 -= texelInsetV;
               }

               float[] floatArray = face.corners();

               for (int currentIndex = 0; currentIndex < 4; currentIndex++) {
                  int cornerIndex = (currentIndex + face.uvTurns()) % 4;
                  float u = cornerIndex <= 1 ? u1 : u2;
                  float v = cornerIndex != 0 && cornerIndex != 3 ? v2 : v1;
                  vertexConsumer.vertex(entry, floatArray[currentIndex * 3], floatArray[currentIndex * 3 + 1], floatArray[currentIndex * 3 + 2])
                     .color(255, 255, 255, 255)
                     .texture(u, v)
                     .overlay(OverlayTexture.DEFAULT_UV)
                     .light(light)
                     .normal(entry, face.nx(), face.ny(), face.nz());
               }
            }
         }

         matrixStack.pop();
      }
   }

   private record BoneDefinition(
      int index,
      String uuid,
      float[] origin,
      float[] offset,
      float[] rotation,
      ArrayList<CosmeticModel.CubeDefinition> cubes,
      ArrayList<CosmeticModel.BoneDefinition> children
   ) {
   }

   private record AnimationDefinition(float length, HashMap<String, CosmeticModel.KeyframeDefinition[][]> tracksByBone) {
   }

   private record CubeDefinition(
      float[] from,
      float[] to,
      float[] origin,
      float[] rotation,
      ArrayList<CosmeticModel.FaceUvDefinition> faces,
      float[] offset,
      Quaternionf rotationQuaternion,
      boolean[] hasTexture
   ) {
   }

   private record KeyframeDefinition(float time, float[] value, boolean step) {
   }

   private record FaceUvDefinition(
      int direction,
      float u1,
      float v1,
      float u2,
      float v2,
      int uvTurns,
      int texture,
      boolean skip,
      float[] corners,
      float nx,
      float ny,
      float nz
   ) {
   }

   private record TextureDefinition(Identifier id, String file, float uvWidth, float uvHeight, CosmeticModel.AtlasDefinition animation) {
   }

   private record AtlasDefinition(int columns, int rows, int imageWidth, int imageHeight, int[] frameIndexes, int[] frameTicks, int totalTicks) {
      int frameAt(int tick) {
         int cycleTick = this.totalTicks == 0 ? 0 : Math.floorMod(tick, this.totalTicks);

         for (int currentIndex = 0; currentIndex < this.frameIndexes.length; currentIndex++) {
            if (cycleTick < this.frameTicks[currentIndex]) {
               return this.frameIndexes[currentIndex];
            }

            cycleTick -= this.frameTicks[currentIndex];
         }

         return this.frameIndexes.length == 0 ? 0 : this.frameIndexes[this.frameIndexes.length - 1];
      }
   }
}
