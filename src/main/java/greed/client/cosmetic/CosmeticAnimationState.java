package greed.client.cosmetic;

import greed.client.cosmetic.BedrockBone;
import greed.client.cosmetic.BedrockModel;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.Map.Entry;

public final class CosmeticAnimationState {
   final int id;
   final String name;
   final int positionType;
   public final float floatVal;
   public final float floatVal2;
   public final float floatVal3;
   public final float floatVal4;
   public final float floatVal5;
   public final float floatVal6;
   public final float floatVal7;
   public final BedrockModel bedrockModel;
   public final Map<String, CosmeticAnimationState.ChannelState> map;
   public final Map<String, float[]> map2 = new HashMap<>();
   private final Map<String, BedrockBone> boneByName = new HashMap<>();
   private final String[] channelNameCache = new String[16];
   private final boolean[] channelCacheValid = new boolean[16];
   public int intVal3;

   private String findChannelContainingAny(String... stringArray) {
      for (String channelName : this.map.keySet()) {
         String normalizedName = normalizeChannelName(channelName);

         for (String keyword : stringArray) {
            if (normalizedName.contains(keyword)) {
               return channelName;
            }
         }
      }

      return null;
   }

   private String findChannelByName(String stringArray) {
      for (String channelName : this.map.keySet()) {
         if (normalizeChannelName(channelName).equals(stringArray)) {
            return channelName;
         }
      }

      return this.findChannelContainingAny(stringArray);
   }

   String selectChannelByState(boolean flying, boolean inWater, boolean sneaking, boolean moving) {
      if (this.map.isEmpty()) {
         return null;
      }

      if (flying) {
         String channelName = this.findChannelContainingAny("elytra", "flying", "fly");
         if (channelName != null) {
            return channelName;
         }
      }

      if (inWater) {
         if (moving) {
            String channelName = this.findChannelContainingAll("moving", "water");
            if (channelName != null) {
               return channelName;
            }
         }

         String channelName = this.findChannelContainingAny("swimming", "swim");
         if (channelName != null) {
            return channelName;
         }

         channelName = this.findChannelContainingAll("idle", "water");
         if (channelName != null) {
            return channelName;
         }

         channelName = this.findChannelContainingAll("main", "water");
         if (channelName != null) {
            return channelName;
         }
      }

      if (sneaking) {
         if (moving) {
            String channelName = this.findChannelContainingAll("moving", "sneak");
            if (channelName != null) {
               return channelName;
            }
         }

         String channelName = this.findChannelContainingAll("idle", "sneak");
         if (channelName != null) {
            return channelName;
         }

         channelName = this.findChannelContainingAll("main", "sneak");
         if (channelName != null) {
            return channelName;
         }

         channelName = this.findChannelContainingAny("sneak", "crouch");
         if (channelName != null) {
            return channelName;
         }
      }

      if (moving) {
         String channelName = this.findChannelContainingAny("walking", "walk", "moving", "move", "run");
         if (channelName != null && !normalizeChannelName(channelName).contains("water") && !normalizeChannelName(channelName).contains("sneak")) {
            return channelName;
         }
      }

      String channelName = this.findChannelByName("idle");
      if (channelName != null && !normalizeChannelName(channelName).contains("water") && !normalizeChannelName(channelName).contains("sneak")) {
         return channelName;
      }

      String mainChannelName = this.findChannelByName("main");
      if (mainChannelName != null && !normalizeChannelName(mainChannelName).contains("water") && !normalizeChannelName(mainChannelName).contains("sneak")) {
         return mainChannelName;
      }

      for (String channelName2 : this.map.keySet()) {
         String normalizedName = normalizeChannelName(channelName2);
         if (!normalizedName.contains("gui") && !normalizedName.contains("preview")) {
            return channelName2;
         }
      }

      return this.map.keySet().iterator().next();
   }

   private void applyStoredTransform(BedrockBone bedrockBone) {
      float[] floatArray = this.map2.get(bedrockBone.strVal);
      if (floatArray != null) {
         bedrockBone.processData5(floatArray[0]);
         bedrockBone.processData7(floatArray[1]);
         bedrockBone.processData8(floatArray[2]);
         bedrockBone.processData2(floatArray[3]);
         bedrockBone.processData13(floatArray[4]);
         bedrockBone.processData4(floatArray[5]);
         bedrockBone.processData9(floatArray[6]);
         bedrockBone.processData12(floatArray[7]);
         bedrockBone.processData6(floatArray[8]);
      }

      for (BedrockBone bedrockBone2 : bedrockBone.list) {
         this.applyStoredTransform(bedrockBone2);
      }
   }

   private String findChannelContainingAll(String... stringArray) {
      for (String channelName : this.map.keySet()) {
         String normalizedName = normalizeChannelName(channelName);
         boolean allMatch = true;

         for (String keyword : stringArray) {
            allMatch &= normalizedName.contains(keyword);
         }

         if (allMatch) {
            return channelName;
         }
      }

      return null;
   }

   public String formatText5(boolean flying, boolean inWater, boolean sneaking, boolean moving) {
      int stateKey = (flying ? 1 : 0) | (inWater ? 2 : 0) | (sneaking ? 4 : 0) | (moving ? 8 : 0);
      if (!this.channelCacheValid[stateKey]) {
         this.channelNameCache[stateKey] = this.selectChannelByState(flying, inWater, sneaking, moving);
         this.channelCacheValid[stateKey] = true;
      }

      return this.channelNameCache[stateKey];
   }

   private static String normalizeChannelName(String channelName) {
      return channelName.toLowerCase(Locale.ROOT);
   }

   private void collectBoneTransforms(BedrockBone bedrockBone) {
      this.map2
         .put(
            bedrockBone.strVal,
            new float[]{
               bedrockBone.computeFloat7(),
               bedrockBone.computeFloat8(),
               bedrockBone.computeFloat4(),
               bedrockBone.computeFloat5(),
               bedrockBone.computeFloat(),
               bedrockBone.computeFloat10(),
               bedrockBone.computeFloat11(),
               bedrockBone.computeFloat3(),
               bedrockBone.computeFloat9()
            }
         );
      this.boneByName.putIfAbsent(bedrockBone.strVal, bedrockBone);
      this.intVal3 = this.intVal3 + bedrockBone.list2.size();

      for (BedrockBone bedrockBone2 : bedrockBone.list) {
         this.collectBoneTransforms(bedrockBone2);
      }
   }

   public CosmeticAnimationState(
      int id,
      String name,
      int positionType,
      float scale,
      float x,
      float y,
      float z,
      float yaw,
      float pitch,
      float roll,
      BedrockModel initialBedrockModel,
      Map<String, CosmeticAnimationState.ChannelState> channels
   ) {
      this.id = id;
      this.name = name;
      this.positionType = positionType;
      this.floatVal = scale;
      this.floatVal2 = x;
      this.floatVal3 = y;
      this.floatVal4 = z;
      this.floatVal5 = yaw;
      this.floatVal6 = pitch;
      this.floatVal7 = roll;
      this.bedrockModel = initialBedrockModel;
      this.map = channels;

      for (BedrockBone bedrockBone : initialBedrockModel.list) {
         this.collectBoneTransforms(bedrockBone);
      }
   }

   public void updateState() {
      for (BedrockBone bedrockBone : this.bedrockModel.list) {
         this.applyStoredTransform(bedrockBone);
      }
   }

   public BedrockBone getBedrockBone(String boneName) {
      return this.boneByName.get(boneName);
   }

   public static final class BoneState {
      final NavigableMap<Float, float[]> map4 = new TreeMap<>();
      float[] floatValArray;
      private float[] cachedTimes;
      private float[][] cachedValues;

      public boolean checkState() {
         return this.floatValArray == null && this.map4.isEmpty();
      }

      float[] sampleTransform(float time, float[] fallback) {
         return this.computeFloat2(time, fallback, new float[3]);
      }

      public float[] computeFloat2(float time, float[] floatArray, float[] floatArray2) {
         if (this.floatValArray != null) {
            return this.floatValArray;
         }

         if (this.map4.isEmpty()) {
            return floatArray;
         }

         if (this.map4.size() == 1) {
            return this.map4.firstEntry().getValue();
         }

         if (this.cachedTimes == null || this.cachedTimes.length != this.map4.size()) {
            float[] floatArray3 = new float[this.map4.size()];
            float[][] floatArrayArray = new float[this.map4.size()][];
            int pointIndex = 0;

            for (Entry<Float, float[]> entry : this.map4.entrySet()) {
               floatArray3[pointIndex] = entry.getKey();
               floatArrayArray[pointIndex++] = entry.getValue();
            }

            this.cachedValues = floatArrayArray;
            this.cachedTimes = floatArray3;
         }

         float[] floatArray4 = this.cachedTimes;
         int pointCount = floatArray4.length;
         if (time <= floatArray4[0]) {
            return this.cachedValues[0];
         }

         if (time >= floatArray4[pointCount - 1]) {
            return this.cachedValues[pointCount - 1];
         }

         int lowIndex = 0;
         int highIndex = pointCount - 1;

         while (highIndex - lowIndex > 1) {
            int midIndex = lowIndex + highIndex >>> 1;
            if (floatArray4[midIndex] <= time) {
               lowIndex = midIndex;
            } else {
               highIndex = midIndex;
            }
         }

         if (floatArray4[lowIndex] == time) {
            return this.cachedValues[lowIndex];
         }

         float fraction = (time - floatArray4[lowIndex]) / (floatArray4[highIndex] - floatArray4[lowIndex]);
         fraction = Math.max(0.0F, Math.min(1.0F, fraction));
         float[] floatArray5 = this.cachedValues[lowIndex];
         float[] floatArray6 = this.cachedValues[highIndex];
         floatArray2[0] = floatArray5[0] + fraction * (floatArray6[0] - floatArray5[0]);
         floatArray2[1] = floatArray5[1] + fraction * (floatArray6[1] - floatArray5[1]);
         floatArray2[2] = floatArray5[2] + fraction * (floatArray6[2] - floatArray5[2]);
         return floatArray2;
      }
   }

   public static final class ChannelState {
      final String name;
      public final boolean flag;
      public final float floatVal8;
      public final Map<String, CosmeticAnimationState.KeyframeState> map5;

      ChannelState(String name, boolean loop, float duration, Map<String, CosmeticAnimationState.KeyframeState> keyframes) {
         this.name = name;
         this.flag = loop;
         this.floatVal8 = Math.max(0.001F, duration);
         this.map5 = keyframes;
      }
   }

   public static final class KeyframeState {
      public final CosmeticAnimationState.BoneState nested0533;
      public final CosmeticAnimationState.BoneState nested05332;
      public final CosmeticAnimationState.BoneState nested05333;

      KeyframeState(CosmeticAnimationState.BoneState rotationState, CosmeticAnimationState.BoneState positionState, CosmeticAnimationState.BoneState scaleState) {
         this.nested0533 = rotationState;
         this.nested05332 = positionState;
         this.nested05333 = scaleState;
      }
   }
}
