package greed.client.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;

public final class MathUtils {
   private static float[] smoothedValue;
   private static float[] targetValue;
   private static long lastTargetUpdateMillis = 0L;
   private static SecureRandom secureRandom = new SecureRandom();

   public static float lerp(float start, float end, float progress) {
      return start + progress * (end - start);
   }

   public static float clampUnit(float value) {
      return computeFloat12(0.0F, 1.0F, value);
   }

   public static int clampInt(int min, int max, int value) {
      return Math.max(min, Math.min(max, value));
   }

   public static float randomFloat(float min, float max) {
      validateRange(min, max);
      return min + ThreadLocalRandom.current().nextFloat() * (max - min);
   }

   public static double roundToStep(double value, double step) {
      if (step <= 0.0) {
         throw new IllegalArgumentException("Step size must be positive");
      }

      long steps = Math.round(value / step);
      return steps * step;
   }

   public static double computeDouble(double start, double end, float progress) {
      return end + (start - end) * progress;
   }

   public static float computeFloat(float value, float step) {
      if (step <= 0.0F) {
         throw new IllegalArgumentException("Step size must be positive");
      }

      int steps = Math.round(value / step);
      return steps * step;
   }

   public static float computeFloat2(float min, float max) {
      validateRange(min, max);
      return min >= max ? min : (float)ThreadLocalRandom.current().nextDouble(min, max);
   }

   public static int computeInt(int min, int max, float progress) {
      return min + (int)(progress * (max - min));
   }

   public static float computeFloat3(float min, float max) {
      float randomA = secureRandom.nextFloat();
      float randomB = secureRandom.nextFloat();
      return min + (max - min) * ((randomA + randomB) / 2.0F);
   }

   public static double computeDouble2(double min, double max) {
      validateRange(min, max);
      return min >= max ? min : ThreadLocalRandom.current().nextDouble(min, max);
   }

   public static double computeDouble3(double start, double end, double progress) {
      return start + progress * (end - start);
   }

   public static float computeFloat4(float degrees) {
      return computeFloat12(0.0F, 360.0F, degrees);
   }

   public static float computeFloat5(float value, float step) {
      double rounded = (double)Math.round(value / step) * step;
      BigDecimal bigDecimal = new BigDecimal(rounded);
      bigDecimal = bigDecimal.setScale(3, RoundingMode.HALF_UP);
      return bigDecimal.floatValue();
   }

   public static float computeFloat6(double value) {
      return (float)value;
   }

   public static double computeDouble4(double valueA, double valueB) {
      return Math.abs(valueA - valueB);
   }

   public static SecureRandom getSecureRandom() {
      return secureRandom;
   }

   public static float computeFloat7(float start, float end) {
      return start + (end - start) * secureRandom.nextFloat();
   }

   public static float computeFloat8(float min, float max) {
      double randomU = secureRandom.nextDouble();
      double randomV = secureRandom.nextDouble();
      double noise = secureRandom.nextGaussian() * 0.02F;
      double curve = Math.pow(randomU, 1.0 + secureRandom.nextDouble() * 0.7);
      double weight = (randomV * 0.8 + 0.1) * (Math.log1p(randomU * 3.0) * 0.5 + 0.5);
      return (float)(min + (max - min) * curve * weight + noise);
   }

   private static void validateRange(double min, double max) {
      if (min > max) {
         throw new IllegalArgumentException("Minimum value cannot be greater than maximum value.");
      }
   }

   public static double computeDouble5(double value, int scale) {
      double factor = Math.pow(10.0, scale);
      return Math.round(value * factor) / factor;
   }

   public static double computeDouble6(double value) {
      return computeDouble7(0.0, 1.0, value);
   }

   private MathUtils() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static long computeLong(long min, long max) {
      return secureRandom.nextLong(min, max);
   }

   public static float computeFloat9(float min, float max) {
      validateRange(min, max);
      return min + ThreadLocalRandom.current().nextFloat() * (max - min);
   }

   public static double computeDouble7(double min, double max, double value) {
      return Math.max(min, Math.min(max, value));
   }

   public static float computeFloat10(float min, float max) {
      switch (secureRandom.nextInt(4)) {
         case 0:
            return computeFloat3(min, max);
         case 1:
            return computeFloat8(min, max);
         case 2:
            return computeFloat11(min, max);
         default:
            return computeFloat7(min, max);
      }
   }

   public static float computeFloat11(float min, float max) {
      return min + (max - min) * (float)Math.random();
   }

   public static float computeFloat12(float min, float max, float value) {
      return Math.max(min, Math.min(max, value));
   }

   public static float computeFloat13(float min, float max, float smoothingFactor) {
      if (smoothedValue == null) {
         smoothedValue = new float[]{(min + max) / 2.0F};
         targetValue = new float[]{randomFloat(min, max)};
      }

      if (System.currentTimeMillis() - lastTargetUpdateMillis > 250.0 + Math.random() * 300.0) {
         targetValue[0] = randomFloat(min, max);
         lastTargetUpdateMillis = System.currentTimeMillis();
      }

      smoothedValue[0] = smoothedValue[0] + (targetValue[0] - smoothedValue[0]) * smoothingFactor;
      return smoothedValue[0];
   }

   public static float computeFloat14(float start, float end, float progress) {
      return end + (start - end) * progress;
   }

   public static double computeDouble8(double degrees) {
      return computeDouble7(0.0, 360.0, degrees);
   }

   public static double computeDouble9(double x1, double y1, double z1, double x2, double y2, double z2) {
      double dx = x2 - x1;
      double dy = y2 - y1;
      double dz = z2 - z1;
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }
}
