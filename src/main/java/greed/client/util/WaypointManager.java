package greed.client.util;

import greed.client.render.FontText;
import greed.client.ui.hud.HudRenderContext;
import greed.client.ui.hud.HudStyle;
import greed.client.render.ShapeDrawing;
import greed.client.render.WorldProjection;

import greed.client.event.EventListener;
import greed.client.render.FontFace;
import greed.client.event.HudRenderEvent;
import greed.client.MinecraftAccess;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class WaypointManager implements MinecraftAccess {
   private static WaypointManager waypointManager;
   private static final int intVal = new Color(0, 0, 0, 120).getRGB();
   private static final int intVal2 = new Color(200, 200, 200, 220).getRGB();
   private final LinkedHashMap<String, WaypointManager.DataRecord> map = new LinkedHashMap<>();
   private final HashMap<String, Float> map2 = new HashMap<>();
   private long timeMillis = System.currentTimeMillis();
   private final EventListener<HudRenderEvent> hudRenderListener = hudRenderEvent -> {
      if (mc.player != null && mc.world != null && !this.map.isEmpty()) {
         long local1289_2 = System.currentTimeMillis();
         this.map.values().removeIf(local1283_2 -> local1283_2.endAtMs() > 0L && local1289_2 - local1283_2.endAtMs() > 90000L);
         this.map2.keySet().retainAll(this.map.keySet());
         float local1289_4 = Math.min(1.0F, (float)(local1289_2 - this.timeMillis) / 300.0F);
         this.timeMillis = local1289_2;
         HudRenderContext.renderElement(hudRenderEvent.getDrawContext());

         for (WaypointManager.DataRecord local1289_6 : this.map.values()) {
            if (local1289_6.dimensionPath().isEmpty() || local1289_6.dimensionPath().equals(mc.world.getRegistryKey().getValue().getPath())) {
               float local1289_7 = this.map2
                  .merge(local1289_6.name(), local1289_4, (local1294_0, local1294_1) -> Math.min(1.0F, local1294_0 + local1294_1));
               Vec3d vec3d = WorldProjection.getVec3d2(local1289_6.pos(), hudRenderEvent.computeInt2(), hudRenderEvent.computeInt());
               if (vec3d != null) {
                  float local1289_9 = local1289_7 < 0.5F
                     ? 2.0F * local1289_7 * local1289_7
                     : 1.0F - (2.0F - 2.0F * local1289_7) * (2.0F - 2.0F * local1289_7) / 2.0F;
                  if (!(local1289_9 < 0.02F)) {
                     float local1289_10 = (float)mc.player.getEntityPos().distanceTo(local1289_6.pos());
                     float local1289_11 = (float)vec3d.x * (GuiScale.computeInt() / GuiScale.computeFloat());
                     float local1289_12 = (float)vec3d.y * (GuiScale.computeInt() / GuiScale.computeFloat());
                     HudRenderContext.processData3(local1289_11, local1289_12, (float)Math.max(0.6, 1.2 - local1289_10 / 150.0) * local1289_9);
                     String local1289_13 = String.format(Locale.US, "%.1fм", local1289_10);
                     float local1289_14 = FontText.computeFloat(FontFace.SEMI_BOLD, local1289_6.name(), 6.0F);
                     float local1289_15 = FontText.computeFloat(FontFace.SEMI_BOLD, local1289_13, 6.0F);
                     float local1289_16 = 4.0F + local1289_14 + 3.0F + local1289_15 + 4.0F;
                     float local1289_17 = 13.0F;
                     float local1289_18 = Math.round((local1289_11 - local1289_16 / 2.0F) * 2.0F) / 2.0F;
                     float local1289_19 = Math.round((local1289_12 - local1289_17 / 2.0F) * 2.0F) / 2.0F;
                     HudStyle.processData2(local1289_18, local1289_19, local1289_16, local1289_17, local1289_9, 4.5F, ShapeDrawing.TypeOption.LOW);
                     FontText.processData3(
                        FontFace.SEMI_BOLD,
                        local1289_6.name(),
                        local1289_18 + 4.0F,
                        local1289_19 + local1289_17 / 2.0F - 3.5F,
                        6.0F,
                        HudStyle.computeInt4(local1289_9)
                     );
                     FontText.processData3(
                        FontFace.SEMI_BOLD,
                        local1289_13,
                        local1289_18 + 4.0F + local1289_14 + 3.0F,
                        local1289_19 + local1289_17 / 2.0F - 3.5F,
                        6.0F,
                        HudStyle.computeInt2(local1289_9)
                     );
                     ArrayList<String> local1289_20 = new ArrayList<>(local1289_6.lines());
                     if (local1289_6.openAtMs() != 0L) {
                        float local1289_21 = (float)(local1289_6.openAtMs() - local1289_2) / 1000.0F;
                        float local1289_22 = (float)(local1289_6.endAtMs() - local1289_2) / 1000.0F;
                        local1289_20.add(
                           local1289_21 > 0.0F
                              ? "До начала: " + String.format(Locale.US, local1289_21 < 30.0F ? "%.1f" : "%.0f", local1289_21) + "с"
                              : (
                                 local1289_22 > 0.0F
                                    ? "До конца: " + String.format(Locale.US, local1289_22 < 30.0F ? "%.1f" : "%.0f", local1289_22) + "с"
                                    : "Конец ивента!"
                              )
                        );
                     }

                     if (!local1289_20.isEmpty()) {
                        float local1289_21 = 0.0F;

                        for (String local1289_23 : local1289_20) {
                           local1289_21 = Math.max(local1289_21, FontText.computeFloat(FontFace.SEMI_BOLD, local1289_23, 5.0F));
                        }

                        local1289_21 += 8.0F;
                        float local1289_22 = 3.5F + local1289_20.size() * 6.5F + 1.5F;
                        float local1289_23 = Math.round((local1289_11 - local1289_21 / 2.0F) * 2.0F) / 2.0F;
                        float local1289_24 = local1289_19 + local1289_17 + 2.0F;
                        HudStyle.processData2(
                           local1289_23, local1289_24, local1289_21, local1289_22, local1289_9, 4.5F, ShapeDrawing.TypeOption.LOW
                        );
                        float local1289_25 = local1289_24 + 3.0F;

                        for (int index = 0; index < local1289_20.size(); index++) {
                           FontText.processData3(
                              FontFace.SEMI_BOLD,
                              local1289_20.get(index),
                              local1289_11 - FontText.computeFloat(FontFace.SEMI_BOLD, local1289_20.get(index), 5.0F) / 2.0F,
                              local1289_25,
                              5.0F,
                              local1289_6.openAtMs() != 0L && index == local1289_20.size() - 1
                                 ? HudStyle.computeInt4(local1289_9)
                                 : HudStyle.computeInt2(local1289_9)
                           );
                           local1289_25 += 6.5F;
                        }
                     }

                     HudRenderContext.updateState();
                  }
               }
            }
         }
      }
   };

   public void updateState() {
      this.map.clear();
   }

   public ArrayList<String> getArrayList() {
      return new ArrayList<>(this.map.keySet());
   }

   public static WaypointManager getInstance() {
      if (waypointManager == null) {
         waypointManager = new WaypointManager();
      }

      return waypointManager;
   }

   public boolean matchesCriteria(String local1287_1) {
      return this.map.remove(local1287_1) != null;
   }

   public void processData(String local1288_1, int local1288_2, int local1288_3, int local1288_4) {
      this.map
         .put(
            local1288_1,
            new WaypointManager.DataRecord(local1288_1, new Vec3d(local1288_2 + 0.5, local1288_3 + 0.5, local1288_4 + 0.5), new ArrayList<>(), 0L, 0L, "")
         );
   }

   public void processData2(String local1291_1, BlockPos blockPos, int local1291_3, int local1291_4, String... stringArray) {
      ArrayList<String> local1291_6 = new ArrayList<>();

      for (String local1291_10 : stringArray) {
         if (local1291_10 != null && !local1291_10.isBlank()) {
            local1291_6.add(local1291_10.trim());
         }
      }

      this.map
         .put(
            local1291_1,
            new WaypointManager.DataRecord(
               local1291_1,
               blockPos.toCenterPos(),
               local1291_6,
               System.currentTimeMillis() + local1291_3 * 1000L,
               System.currentTimeMillis() + (local1291_3 + local1291_4) * 1000L,
               mc.world == null ? "" : mc.world.getRegistryKey().getValue().getPath()
            )
         );
   }

   public int computeInt() {
      return this.map.size();
   }

   public record DataRecord(String name, Vec3d pos, ArrayList<String> lines, long openAtMs, long endAtMs, String dimensionPath) {
   }
}
