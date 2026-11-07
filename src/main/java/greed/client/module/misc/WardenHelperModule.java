package greed.client.module.misc;

import greed.client.util.Colors;
import greed.client.event.EventListener;
import greed.client.render.FontFace;
import greed.client.event.HudRenderEvent;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.module.ModuleManager;
import greed.client.event.PlayerTickEvent;
import greed.client.util.GuiScale;
import greed.client.render.WorldProjection;
import greed.client.render.FontText;
import greed.client.render.ThemeColors;
import greed.client.ui.hud.HudRenderContext;
import greed.client.render.Render2D;
import greed.client.util.Stopwatch;
import greed.client.event.WorldRenderEvent;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import org.joml.Matrix4f;

@ModuleInfo(
   name = "WardenHelper",
   description = "Подсвечивает сундуки вардена и показывает их таймеры: отсчёт, «Активен», «Не активированный».",
   category = ModuleCategory.MISC
)
public final class WardenHelperModule extends Module {
   private static WardenHelperModule wardenHelperModule;
   private static final Pattern pattern = Pattern.compile("(?:(\\d{1,2}):(\\d{2})|(\\d+)\\s*(?:сек|сек\\.|с|s)|(\\d+)\\s*(?:мин|мин\\.|m))");
   private final ConcurrentHashMap<BlockPos, Long> map = new ConcurrentHashMap<>();
   private final Stopwatch stopwatch = new Stopwatch();
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> this.updateState();
   private final EventListener<HudRenderEvent> hudRenderListener = event -> this.onHudRender(event);
   private final EventListener<WorldRenderEvent> worldRenderListener = event2 -> this.onWorldRender(event2);

   private int computeInt(long local1918_1) {
      return local1918_1 > System.currentTimeMillis() ? -43691 : (local1918_1 > 0L ? -11141291 : -171);
   }

   private void updateState() {
      if (mc.player != null && mc.world != null && this.stopwatch.hasExceededMillis(250L)) {
         this.stopwatch.reset();

         for (int index = mc.player.getChunkPos().x - 6; index <= mc.player.getChunkPos().x + 6; index++) {
            for (int index2 = mc.player.getChunkPos().z - 6; index2 <= mc.player.getChunkPos().z + 6; index2++) {
               if (mc.world.isChunkLoaded(index, index2)) {
                  for (BlockEntity blockEntity : mc.world.getChunk(index, index2).getBlockEntities().values()) {
                     if (blockEntity instanceof ChestBlockEntity && !(mc.player.squaredDistanceTo(Vec3d.ofCenter(blockEntity.getPos())) > 6400.0)) {
                        if (this.computeInt2(blockEntity.getPos()) >= 0) {
                           this.map.put(blockEntity.getPos().toImmutable(), System.currentTimeMillis() + this.computeInt2(blockEntity.getPos()) * 1000L);
                        } else {
                           this.map.putIfAbsent(blockEntity.getPos().toImmutable(), 0L);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void onWorldRender(WorldRenderEvent event) {
      if (mc.player != null && mc.world != null) {
         for (Entry<BlockPos, Long> local1921_3 : this.map.entrySet()) {
            if (!(mc.player.getEntityPos().distanceTo(Vec3d.ofCenter(local1921_3.getKey())) > 80.0)
               && mc.world.getBlockEntity(local1921_3.getKey()) instanceof ChestBlockEntity) {
               Box box = new Box(local1921_3.getKey()).contract(0.0625, 0.0625, 0.0625);
               event.getMatrixStack().push();
               event.getMatrixStack()
                  .translate(
                     box.minX - mc.gameRenderer.getCamera().getCameraPos().x,
                     box.minY - mc.gameRenderer.getCamera().getCameraPos().y,
                     box.minZ - mc.gameRenderer.getCamera().getCameraPos().z
                  );
               Matrix4f positionMatrix = event.getMatrixStack().peek().getPositionMatrix();
               VertexConsumer vertexConsumer = mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayers.debugQuads());
               float[] floatArray = new float[]{0.0F, (float)(box.maxX - box.minX)};
               float[] floatArray2 = new float[]{0.0F, (float)(box.maxY - box.minY)};
               float[] floatArray3 = new float[]{0.0F, (float)(box.maxZ - box.minZ)};
               int[][] intArrayArray = new int[][]{{0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}};

               for (int[] intArray : intArrayArray) {
                  for (int local1921_18 : intArray) {
                     vertexConsumer.vertex(positionMatrix, floatArray[local1921_18 >> 2], floatArray2[local1921_18 >> 1 & 1], floatArray3[local1921_18 & 1])
                        .color(Colors.computeInt6(this.computeInt(local1921_3.getValue()), 0.18F));
                  }
               }

               VertexRendering.drawOutline(
                  event.getMatrixStack(),
                  mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayers.lines()),
                  VoxelShapes.cuboid(
                     new Box(0.0, 0.0, 0.0, box.maxX - box.minX, box.maxY - box.minY, box.maxZ - box.minZ)
                  ),
                  0.0,
                  0.0,
                  0.0,
                  Colors.computeInt6(this.computeInt(local1921_3.getValue()), 0.55F),
                  1.0F
               );
               mc.getBufferBuilders().getEntityVertexConsumers().draw();
               event.getMatrixStack().pop();
            }
         }
      }
   }

   public static WardenHelperModule getInstance() {
      if (wardenHelperModule == null) {
         wardenHelperModule = new WardenHelperModule();
      }

      return wardenHelperModule;
   }

   private int computeInt2(BlockPos blockPos) {
      for (Entity entity : mc.world
         .getOtherEntities(
            mc.player,
            new Box(blockPos).expand(3.0, 4.0, 3.0),
            entity2 -> entity2 instanceof ArmorStandEntity armorStandEntity && armorStandEntity.hasCustomName() || entity2 instanceof TextDisplayEntity
         )) {
         Matcher matcher = pattern.matcher(
            (entity instanceof TextDisplayEntity textDisplayEntity ? textDisplayEntity.getText() : entity.getName())
               .getString()
               .replaceAll("(?i)§[0-9a-fk-or]", "")
               .trim()
         );
         if (matcher.find()) {
            if (matcher.group(1) != null) {
               return Integer.parseInt(matcher.group(1)) * 60 + Integer.parseInt(matcher.group(2));
            }

            if (matcher.group(3) != null) {
               return Integer.parseInt(matcher.group(3));
            }

            return Integer.parseInt(matcher.group(4)) * 60;
         }
      }

      return -1;
   }

   private void onHudRender(HudRenderEvent event) {
      if (mc.player != null && mc.world != null) {
         for (Entry<BlockPos, Long> local1927_3 : this.map.entrySet()) {
            if (!(mc.player.getEntityPos().distanceTo(Vec3d.ofCenter(local1927_3.getKey())) > 80.0)
               && mc.world.getBlockEntity(local1927_3.getKey()) instanceof ChestBlockEntity) {
               Vec3d vec3d = WorldProjection.getVec3d2(
                  Vec3d.ofCenter(local1927_3.getKey()).add(0.0, 0.7, 0.0), event.computeInt2(), event.computeInt()
               );
               if (vec3d != null) {
                  HudRenderContext.processData3(
                     (float)vec3d.x * GuiScale.computeInt() / GuiScale.computeFloat(),
                     (float)vec3d.y * GuiScale.computeInt() / GuiScale.computeFloat(),
                     (float)Math.max(0.55F, 1.0 - mc.player.getEntityPos().distanceTo(Vec3d.ofCenter(local1927_3.getKey())) * 0.008F)
                  );
                  Render2D.drawRect(
                     (float)vec3d.x * GuiScale.computeInt() / GuiScale.computeFloat()
                        - (FontText.computeFloat(FontFace.SEMI_BOLD, this.formatText(local1927_3.getValue()), 4.0F) + 4.0F) / 2.0F,
                     (float)vec3d.y * GuiScale.computeInt() / GuiScale.computeFloat() - 4.5F,
                     FontText.computeFloat(FontFace.SEMI_BOLD, this.formatText(local1927_3.getValue()), 4.0F) + 4.0F,
                     9.0F,
                     0.0F,
                     Colors.computeInt7(ThemeColors.intVal, 133)
                  );
                  FontText.processData3(
                     FontFace.SEMI_BOLD,
                     this.formatText(local1927_3.getValue()),
                     (float)vec3d.x * GuiScale.computeInt() / GuiScale.computeFloat()
                        - FontText.computeFloat(FontFace.SEMI_BOLD, this.formatText(local1927_3.getValue()), 4.0F) / 2.0F,
                     (float)vec3d.y * GuiScale.computeInt() / GuiScale.computeFloat() - 2.45F,
                     4.0F,
                     this.computeInt(local1927_3.getValue())
                  );
                  HudRenderContext.updateState();
               }
            }
         }
      }
   }

   private String formatText(long local1928_1) {
      if (local1928_1 > System.currentTimeMillis()) {
         return (local1928_1 - System.currentTimeMillis()) / 60000L + ":" + String.format("%02d", (local1928_1 - System.currentTimeMillis()) / 1000L % 60L);
      } else {
         return local1928_1 > 0L ? "Активен" : "Активирован";
      }
   }

   private WardenHelperModule() {
      ModuleManager.unregisterModule(this);
   }
}
