package greed.client.setting;

import java.util.function.Supplier;

public class NumberSetting extends Setting<NumberSetting> {
   private float value;
   private float minimum = 0.0F;
   private float maximum = 1.0F;
   private float step = 0.01F;
   private boolean integral;
   private String suffix = "";
   private Supplier<Boolean> automatic = () -> Boolean.FALSE;

   // Конструкторы
   public NumberSetting(String name, String description) {
      super(name, description);
      this.value = 0.0F;
   }

   protected NumberSetting(
           String name, String description, float initialValue,
           float initialMinimum, float initialMaximum, float initialStep, boolean initialIntegral
   ) {
      super(name, description);
      this.minimum = initialMinimum;
      this.maximum = initialMaximum;
      this.step = initialStep <= 0.0F ? (initialIntegral ? 1.0F : 0.01F) : initialStep;
      this.integral = initialIntegral;
      this.value = this.clampAndSnap(initialValue);
   }

   // Геттеры и сеттеры значения
   public float getValue() {
      return this.value;
   }

   public NumberSetting setValue(float newValue) {
      float clampedValue = this.clampAndSnap(newValue);
      if (clampedValue != this.value) {
         this.value = clampedValue;
         this.notifyChanged();
      }
      return this;
   }

   public float floatValue() {
      return this.value;
   }

   public double doubleValue() {
      return this.value;
   }

   public int intValue() {
      return Math.round(this.value);
   }

   public int getInt() {
      return this.intValue();
   }

   // Настройка диапазона и шага
   public NumberSetting withRange(float newMinimum, float newMaximum) {
      this.minimum = newMinimum;
      this.maximum = newMaximum;
      this.integral = false;
      this.value = this.clampAndSnap(this.value);
      return this;
   }

   public NumberSetting withIntegerRange(int newMinimum, int newMaximum) {
      this.minimum = newMinimum;
      this.maximum = newMaximum;
      this.integral = true;
      this.step = 1.0F;
      this.value = this.clampAndSnap(this.value);
      return this;
   }

   public NumberSetting withStep(float newStep) {
      if (newStep > 0.0F) {
         this.step = newStep;
         this.value = this.clampAndSnap(this.value);
      }
      return this;
   }

   public NumberSetting withIntegerStep(int newStep) {
      return this.withStep(newStep);
   }

   public NumberSetting withSuffix(String suffix) {
      this.suffix = suffix == null ? "" : suffix;
      return this;
   }

   // Дополнительные параметры
   public NumberSetting automaticWhen(Supplier<Boolean> newAutomatic) {
      if (newAutomatic != null) {
         this.automatic = newAutomatic;
      }
      return this;
   }

   public boolean isAutomatic() {
      return Boolean.TRUE.equals(this.automatic.get());
   }

   public boolean isIntegral() {
      return this.integral;
   }

   public float getMinimum() {
      return this.minimum;
   }

   public float getMaximum() {
      return this.maximum;
   }

   public float getStep() {
      return this.step;
   }

   public String getSuffix() {
      return this.suffix;
   }

   // Работа с процентами (фракцией) для слайдеров
   public float getFraction() {
      return this.maximum <= this.minimum ? 0.0F : (this.value - this.minimum) / (this.maximum - this.minimum);
   }

   public void setFraction(float fraction) {
      float clampedFraction = Math.max(0.0F, Math.min(1.0F, fraction));
      this.setValue(this.minimum + (this.maximum - this.minimum) * clampedFraction);
   }

   // Отображение значения
   public String getDisplayValue() {
      int places = this.decimalPlaces();
      String formattedNumber = places <= 0
              ? Integer.toString(Math.round(this.value))
              : String.format("%." + places + "f", this.value);
      return this.suffix.isEmpty() ? formattedNumber : formattedNumber + this.suffix;
   }

   private int decimalPlaces() {
      return !this.integral && !(this.step >= 1.0F) ? Math.max(1, (int)Math.ceil(-Math.log10(this.step))) : 0;
   }

   // Ограничение и привязка к шагу
   private float clampAndSnap(float rawValue) {
      float clamped = Math.max(this.minimum, Math.min(this.maximum, rawValue));
      if (this.step > 0.0F) {
         clamped = this.minimum + Math.round((clamped - this.minimum) / this.step) * this.step;
         clamped = Math.max(this.minimum, Math.min(this.maximum, clamped));
      }
      return this.integral ? Math.round(clamped) : clamped;
   }
}