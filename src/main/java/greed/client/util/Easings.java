package greed.client.util;


public final class Easings {
   public static final double BACK_C1 = 1.70158;
   public static final double BACK_C2 = 2.5949095;
   public static final double BACK_C3 = 2.70158;
   public static final double ELASTIC_C4 = Math.PI * 2.0 / 3.0;
   public static final double ELASTIC_C5 = Math.PI * 4.0 / 9.0;
   public static final EasingFunction LINEAR = local2120_0 -> local2120_0;
   public static final EasingFunction EASE_IN_QUAD = easeInIntegerPower(2);
   public static final EasingFunction EASE_OUT_QUAD = easeOutIntegerPower(2);
   public static final EasingFunction EASE_IN_OUT_QUAD = easeInOutPower(2.0);
   public static final EasingFunction EASE_IN_CUBIC = easeInIntegerPower(3);
   public static final EasingFunction EASE_OUT_CUBIC = easeOutIntegerPower(3);
   public static final EasingFunction EASE_IN_OUT_CUBIC = easeInOutPower(3.0);
   public static final EasingFunction EASE_IN_QUART = easeInIntegerPower(4);
   public static final EasingFunction EASE_OUT_QUART = easeOutIntegerPower(4);
   public static final EasingFunction EASE_IN_OUT_QUART = easeInOutPower(4.0);
   public static final EasingFunction EASE_IN_QUINT = easeInIntegerPower(5);
   public static final EasingFunction EASE_OUT_QUINT = easeOutIntegerPower(5);
   public static final EasingFunction EASE_IN_OUT_QUINT = easeInOutPower(5.0);
   public static final EasingFunction EASE_IN_SINE = local2121_0 -> 1.0 - Math.cos(local2121_0 * Math.PI / 2.0);
   public static final EasingFunction EASE_OUT_SINE = local2107_0 -> Math.sin(local2107_0 * Math.PI / 2.0);
   public static final EasingFunction EASE_IN_OUT_SINE = local2123_0 -> -(Math.cos(Math.PI * local2123_0) - 1.0) / 2.0;
   public static final EasingFunction EASE_IN_CIRC = local2133_0 -> 1.0 - Math.sqrt(1.0 - Math.pow(local2133_0, 2.0));
   public static final EasingFunction EASE_OUT_CIRC = local2124_0 -> Math.sqrt(1.0 - Math.pow(local2124_0 - 1.0, 2.0));
   public static final EasingFunction EASE_IN_OUT_CIRC = local2106_0 -> local2106_0 < 0.5
      ? (1.0 - Math.sqrt(1.0 - Math.pow(2.0 * local2106_0, 2.0))) / 2.0
      : (Math.sqrt(1.0 - Math.pow(-2.0 * local2106_0 + 2.0, 2.0)) + 1.0) / 2.0;
   public static final EasingFunction EASE_IN_ELASTIC = local2125_0 -> local2125_0 != 0.0 && local2125_0 != 1.0
      ? Math.pow(-2.0, 10.0 * local2125_0 - 10.0) * Math.sin((local2125_0 * 10.0 - 10.75) * (Math.PI * 2.0 / 3.0))
      : local2125_0;
   public static final EasingFunction EASE_OUT_ELASTIC = local2118_0 -> local2118_0 != 0.0 && local2118_0 != 1.0
      ? Math.pow(2.0, -10.0 * local2118_0) * Math.sin((local2118_0 * 10.0 - 0.75) * (Math.PI * 2.0 / 3.0)) + 1.0
      : local2118_0;
   public static final EasingFunction EASE_IN_OUT_ELASTIC = local2131_0 -> {
      if (local2131_0 != 0.0 && local2131_0 != 1.0) {
         return local2131_0 < 0.5
            ? -(Math.pow(2.0, 20.0 * local2131_0 - 10.0) * Math.sin((20.0 * local2131_0 - 11.125) * (Math.PI * 4.0 / 9.0))) / 2.0
            : Math.pow(2.0, -20.0 * local2131_0 + 10.0) * Math.sin((20.0 * local2131_0 - 11.125) * (Math.PI * 4.0 / 9.0)) / 2.0 + 1.0;
      } else {
         return local2131_0;
      }
   };
   public static final EasingFunction EASE_IN_EXPO = local2119_0 -> local2119_0 != 0.0 ? Math.pow(2.0, 10.0 * local2119_0 - 10.0) : local2119_0;
   public static final EasingFunction EASE_OUT_EXPO = local2115_0 -> local2115_0 != 1.0 ? 1.0 - Math.pow(2.0, -10.0 * local2115_0) : local2115_0;
   public static final EasingFunction EASE_IN_OUT_EXPO = local2105_0 -> {
      if (local2105_0 != 0.0 && local2105_0 != 1.0) {
         return local2105_0 < 0.5 ? Math.pow(2.0, 20.0 * local2105_0 - 10.0) / 2.0 : (2.0 - Math.pow(2.0, -20.0 * local2105_0 + 10.0)) / 2.0;
      } else {
         return local2105_0;
      }
   };
   public static final EasingFunction EASE_IN_BACK = local2116_0 -> 2.70158 * Math.pow(local2116_0, 3.0) - 1.70158 * Math.pow(local2116_0, 2.0);
   public static final EasingFunction EASE_OUT_BACK = local2127_0 -> 1.0
      + 2.70158 * Math.pow(local2127_0 - 1.0, 3.0)
      + 1.70158 * Math.pow(local2127_0 - 1.0, 2.0);
   public static final EasingFunction EASE_IN_OUT_BACK = local2111_0 -> local2111_0 < 0.5
      ? Math.pow(2.0 * local2111_0, 2.0) * (7.189819 * local2111_0 - 2.5949095) / 2.0
      : (Math.pow(2.0 * local2111_0 - 2.0, 2.0) * (3.5949095 * (local2111_0 * 2.0 - 2.0) + 2.5949095) + 2.0) / 2.0;
   public static final EasingFunction EASE_OUT_BOUNCE = local2126_0 -> {
      double local2126_2 = 7.5625;
      double local2126_4 = 2.75;
      if (local2126_0 < 1.0 / local2126_4) {
         return local2126_2 * Math.pow(local2126_0, 2.0);
      } else if (local2126_0 < 2.0 / local2126_4) {
         return local2126_2 * Math.pow(local2126_0 - 1.5 / local2126_4, 2.0) + 0.75;
      } else {
         return local2126_0 < 2.5 / local2126_4
            ? local2126_2 * Math.pow(local2126_0 - 2.25 / local2126_4, 2.0) + 0.9375
            : local2126_2 * Math.pow(local2126_0 - 2.625 / local2126_4, 2.0) + 0.984375;
      }
   };
   public static final EasingFunction EASE_IN_BOUNCE = local2114_0 -> 1.0 - EASE_OUT_BOUNCE.apply(1.0 - local2114_0);
   public static final EasingFunction EASE_IN_OUT_BOUNCE = local2129_0 -> local2129_0 < 0.5
      ? (1.0 - EASE_OUT_BOUNCE.apply(1.0 - 2.0 * local2129_0)) / 2.0
      : (1.0 + EASE_OUT_BOUNCE.apply(2.0 * local2129_0 - 1.0)) / 2.0;

   public static EasingFunction easeOutPower(double local2108_0) {
      return local2113_2 -> 1.0 - Math.pow(1.0 - local2113_2, local2108_0);
   }

   public static EasingFunction easeInIntegerPower(int local2110_0) {
      return easeInPower(local2110_0);
   }

   public static EasingFunction easeOutIntegerPower(int local2112_0) {
      return easeOutPower(local2112_0);
   }

   public static EasingFunction easeInPower(double local2122_0) {
      return local2109_2 -> Math.pow(local2109_2, local2122_0);
   }

   private Easings() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static EasingFunction easeInOutPower(double local2132_0) {
      return local2117_2 -> local2117_2 < 0.5
         ? Math.pow(2.0, local2132_0 - 1.0) * Math.pow(local2117_2, local2132_0)
         : 1.0 - Math.pow(-2.0 * local2117_2 + 2.0, local2132_0) / 2.0;
   }
}
