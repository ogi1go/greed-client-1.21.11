package greed.client.render;

import greed.client.render.FontTexture;

import java.util.List;

public final class RasterTextLayout {
   static final RasterTextLayout rasterTextLayout = new RasterTextLayout(List.of(), 0.0F, 0.0F);
   private final List<RasterTextLayout.DataRecord> list;
   private final float floatVal;
   private final float floatVal2;

   public boolean checkState() {
      return this.list.isEmpty();
   }

   public float computeFloat() {
      return this.floatVal2;
   }

   public RasterTextLayout(List<RasterTextLayout.DataRecord> local6693_1, float local6693_2, float local6693_3) {
      this.list = List.copyOf(local6693_1);
      this.floatVal = local6693_2;
      this.floatVal2 = local6693_3;
   }

   public float computeFloat2() {
      return this.floatVal;
   }

   public List<RasterTextLayout.DataRecord> getList() {
      return this.list;
   }

   public record DataRecord(FontTexture page, List<RasterGlyphQuad> glyphs) {
      public DataRecord {
         glyphs = List.copyOf(glyphs);
      }
   }
}
