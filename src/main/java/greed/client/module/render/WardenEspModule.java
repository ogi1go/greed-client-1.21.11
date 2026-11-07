package greed.client.module.render;

import greed.client.event.EventListener;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.PacketEvent;
import greed.client.event.PlayerTickEvent;
import greed.client.render.WorldProjection;

import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(name = "WardenEsp", description = "Показывает таймер над сундуком вардена через стены.", category = ModuleCategory.RENDER)
public final class WardenEspModule extends Module {
   private static WardenEspModule wardenEspModule;
   private static final Pattern pattern = Pattern.compile("(?:\\d{1,2}:\\d{2}|\\d+\\s*(?:сек|с|s|мин|m)\\b)");
   private final IntOpenHashSet set = new IntOpenHashSet();
   private final HashMap<BlockPos, Long> map = new HashMap<>();
   private final EventListener<PacketEvent> packetListener = packetEvent -> {
      if (packetEvent.isIncoming()
         && packetEvent.getPacket() instanceof PlaySoundS2CPacket playSoundS2CPacket
         && playSoundS2CPacket.getSound().value().id().getPath().startsWith("entity.warden")) {
         mc.execute(() -> this.processData(new Vec3d(playSoundS2CPacket.getX(), playSoundS2CPacket.getY(), playSoundS2CPacket.getZ())));
      }
   };
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      this.set.clear();
      if (mc.player != null && mc.world != null) {
         long local3094_2 = System.currentTimeMillis();
         this.map
            .entrySet()
            .removeIf(
               local3083_3 -> local3094_2 - local3083_3.getValue() > 30000L
                  || !(mc.world.getBlockState(local3083_3.getKey()).getBlock() instanceof ChestBlock)
                  || this.matchesCriteria3(local3083_3.getKey())
            );

         for (Entity entity : mc.world.getEntities()) {
            if (this.getText2(entity) != null && this.matchesCriteria(entity)) {
               this.set.add(entity.getId());
            }
         }
      } else {
         this.map.clear();
      }
   };

   private WardenEspModule() {
   }

   private void renderElement(DrawContext drawContext, Vec3d vec3d, Text text) {
      Vec3d vec3d2 = WorldProjection.getVec3d2(vec3d, drawContext.getScaledWindowWidth(), drawContext.getScaledWindowHeight());
      if (vec3d2 != null) {
         float local3081_5 = Math.max(0.3F, 1.0F - (float)mc.gameRenderer.getCamera().getCameraPos().distanceTo(vec3d) * 0.008F);
         List<OrderedText> local3081_6 = mc.textRenderer.wrapLines(text, 200);
         int local3081_7 = 9 + 1;
         drawContext.getMatrices().pushMatrix();
         drawContext.getMatrices().translate((float)vec3d2.x, (float)vec3d2.y);
         drawContext.getMatrices().scale(local3081_5, local3081_5);
         int local3081_8 = -local3081_6.size() * local3081_7;

         for (int index = 0; index < local3081_6.size(); index++) {
            int local3081_10 = mc.textRenderer.getWidth(local3081_6.get(index));
            int local3081_11 = local3081_8 + index * local3081_7;
            drawContext.fill(-local3081_10 / 2 - 1, local3081_11 - 1, local3081_10 / 2 + 1, local3081_11 + local3081_7 - 1, 1073741824);
            drawContext.drawText(mc.textRenderer, local3081_6.get(index), -local3081_10 / 2, local3081_11, -1, false);
         }

         drawContext.getMatrices().popMatrix();
      }
   }

   @Override
   protected void onDisable() {
      this.set.clear();
      this.map.clear();
   }

   private boolean matchesCriteria(Entity entity) {
      BlockPos blockPos = this.getBlockPos2(entity.getBlockPos());
      return blockPos != null && this.matchesCriteria3(blockPos);
   }

   public static WardenEspModule getInstance() {
      if (wardenEspModule == null) {
         wardenEspModule = new WardenEspModule();
      }

      return wardenEspModule;
   }

   public void renderElement2(DrawContext drawContext, float local3087_2) {
      if (this.isEnabled() && mc.player != null && mc.world != null && (!this.set.isEmpty() || !this.map.isEmpty())) {
         for (BlockPos blockPos : this.map.keySet()) {
            this.renderElement(drawContext, Vec3d.ofBottomCenter(blockPos).add(0.0, 1.5, 0.0), Text.literal("Не активирован").withColor(16733525));
         }

         IntIterator intIterator = this.set.iterator();

         while (intIterator.hasNext()) {
            int local3087_4 = (Integer)intIterator.next();
            Entity entityById = mc.world.getEntityById(local3087_4);
            Text text = entityById == null ? null : this.getText2(entityById);
            if (text != null) {
               this.renderElement(
                  drawContext,
                  entityById.getLerpedPos(local3087_2).add(0.0, entityById instanceof ArmorStandEntity ? entityById.getHeight() + 0.5 : 0.5, 0.0),
                  text
               );
            }
         }

         drawContext.createNewRootLayer();
      }
   }

   private Text getText2(Entity entity) {
      if (entity instanceof TextDisplayEntity textDisplayEntity) {
         return textDisplayEntity.getText();
      } else {
         return entity instanceof ArmorStandEntity armorStandEntity && armorStandEntity.hasCustomName() ? armorStandEntity.getCustomName() : null;
      }
   }

   private BlockPos getBlockPos2(BlockPos blockPos) {
      for (int index = 0; index <= 4; index++) {
         for (int index2 = -1; index2 <= 1; index2++) {
            for (int index3 = -1; index3 <= 1; index3++) {
               BlockPos blockPos2 = blockPos.add(index2, -index, index3);
               if (mc.world.getBlockState(blockPos2).getBlock() instanceof ChestBlock) {
                  return blockPos2;
               }
            }
         }
      }

      return null;
   }

   public boolean matchesCriteria2(Entity entity) {
      return this.isEnabled() && this.set.contains(entity.getId());
   }

   private void processData(Vec3d vec3d) {
      if (mc.world != null) {
         ChunkPos chunkPos = new ChunkPos(BlockPos.ofFloored(vec3d));

         for (int index = chunkPos.x - 1; index <= chunkPos.x + 1; index++) {
            for (int index2 = chunkPos.z - 1; index2 <= chunkPos.z + 1; index2++) {
               if (mc.world.isChunkLoaded(index, index2)) {
                  for (BlockEntity blockEntity : mc.world.getChunk(index, index2).getBlockEntities().values()) {
                     if (blockEntity instanceof ChestBlockEntity
                        && blockEntity.getPos().toCenterPos().squaredDistanceTo(vec3d) <= 256.0
                        && !this.matchesCriteria3(blockEntity.getPos())) {
                        this.map.put(blockEntity.getPos().toImmutable(), System.currentTimeMillis());
                     }
                  }
               }
            }
         }
      }
   }

   private boolean matchesCriteria3(BlockPos blockPos) {
      for (Entity entity : mc.world.getOtherEntities(null, new Box(blockPos).expand(1.5, 4.0, 1.5))) {
         Text text = this.getText2(entity);
         if (text != null
            && entity.getY() > blockPos.getY()
            && pattern.matcher(text.getString().replaceAll("(?i)§[0-9a-fk-or]", "")).find()) {
            return true;
         }
      }

      return false;
   }
}
