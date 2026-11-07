package greed.client.util;

import greed.client.util.GlintMode;

public record ItemDrawOptions(float alpha, int color, GlintMode glintMode, boolean showCount, boolean showDurability, float glintStrength) {
   private static final ItemDrawOptions DEFAULTS = new ItemDrawOptions(1.0F, -1, GlintMode.AUTO, true, true, 0.62F);

   private static float clamp(float local1898_0, float local1898_1, float local1898_2) {
      return Math.max(local1898_1, Math.min(local1898_2, local1898_0));
   }

   public static ItemDrawOptions noDecorations(float local1899_0) {
      return DEFAULTS.alpha(local1899_0).count(false).durability(false);
   }

   private static int normalizeColor(int local1900_0) {
      return (local1900_0 & 0xFF000000) == 0 && (local1900_0 & 16777215) != 0 ? local1900_0 | 0xFF000000 : local1900_0;
   }

   public ItemDrawOptions glintStrength(float local1901_1) {
      return new ItemDrawOptions(this.alpha, this.color, this.glintMode, this.showCount, this.showDurability, local1901_1);
   }

   public ItemDrawOptions count(boolean local1904_1) {
      return new ItemDrawOptions(this.alpha, this.color, this.glintMode, local1904_1, this.showDurability, this.glintStrength);
   }

   public ItemDrawOptions alpha(float local1906_1) {
      return new ItemDrawOptions(local1906_1, this.color, this.glintMode, this.showCount, this.showDurability, this.glintStrength);
   }

   public ItemDrawOptions {
      alpha = clamp(alpha, 0.0F, 1.0F);
      color = normalizeColor(color);
      glintMode = glintMode == null ? GlintMode.AUTO : glintMode;
      glintStrength = clamp(glintStrength, 0.0F, 1.0F);
   }

   public ItemDrawOptions glint(GlintMode newGlintMode) {
      return new ItemDrawOptions(this.alpha, this.color, newGlintMode, this.showCount, this.showDurability, this.glintStrength);
   }

   public ItemDrawOptions durability(boolean local1911_1) {
      return new ItemDrawOptions(this.alpha, this.color, this.glintMode, this.showCount, local1911_1, this.glintStrength);
   }

   public static ItemDrawOptions decorated(float local1912_0) {
      return DEFAULTS.alpha(local1912_0).count(true).durability(true);
   }

   public static ItemDrawOptions defaults() {
      return DEFAULTS;
   }

   public ItemDrawOptions color(int local1917_1) {
      return new ItemDrawOptions(this.alpha, local1917_1, this.glintMode, this.showCount, this.showDurability, this.glintStrength);
   }
}
