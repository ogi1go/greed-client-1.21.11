package greed.client.setting;

import java.awt.Color;

public final class ColorSetting extends Setting<ColorSetting> {
   private int argb;

   public ColorSetting(String name, String description, Color color) {
      super(name, description);
      this.argb = color.getRGB();
   }

   public ColorSetting(String name, String description) {
      this(name, description, new Color(255, 255, 255, 255));
   }

   private float[] getHsb() {
      Color color = new Color(this.argb, true);
      return Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
   }

   public int getArgb() {
      return this.argb;
   }

   public int getValue() {
      return this.argb;
   }

   public int getOpaqueRgb() {
      return 0xFF000000 | this.argb & 0x00FFFFFF;
   }

   public int getAlpha() {
      return this.argb >> 24 & 0xFF;
   }

   public float getHue() {
      return this.getHsb()[0];
   }

   public float getSaturation() {
      return this.getHsb()[1];
   }

   public float getBrightness() {
      return this.getHsb()[2];
   }

   public ColorSetting withArgb(int newArgb) {
      return this.setValue(newArgb);
   }

   public ColorSetting withAlpha(float alpha) {
      int alphaInt = Math.round(Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F);
      return this.setValue(alphaInt << 24 | this.argb & 0x00FFFFFF);
   }

   public ColorSetting withHsb(float hue, float saturation, float brightness) {
      int alpha = this.getAlpha();
      int rgb = Color.HSBtoRGB(hue, saturation, brightness) & 0x00FFFFFF;
      return this.setValue(alpha << 24 | rgb);
   }

   public ColorSetting setValue(int newArgb) {
      if (newArgb != this.argb) {
         this.argb = newArgb;
         this.notifyChanged();
      }
      return this;
   }
}