package greed.client.module.render;

import greed.client.util.TargetHudStyle;

import greed.mixin.BossBarHudMixin;
import greed.client.util.Colors;
import greed.client.event.EventListener;
import greed.client.render.FontFace;
import greed.client.event.HudRenderEvent;
import greed.client.util.ItemDrawOptions;
import greed.client.setting.ModeSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.PacketEvent;
import greed.client.event.PlayerTickEvent;
import greed.client.util.GuiScale;
import greed.client.render.WorldProjection;
import greed.client.render.FontText;
import greed.client.render.ItemRenderCache;
import greed.client.render.ShapeDrawing;
import greed.client.util.TargetHudStyle;
import greed.client.ui.AnimatedValue;
import greed.client.render.ThemeColors;
import greed.client.ui.hud.HudRenderContext;
import greed.client.ui.hud.HudStyle;
import greed.client.event.WorldRenderEvent;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.boss.BossBar.Color;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import org.joml.Matrix4f;

@ModuleInfo(name = "StructureInfo", description = "Определяет трапки и пласты.", category = ModuleCategory.RENDER)
public final class StructureInfoModule extends Module {
   private static StructureInfoModule structureInfoModule;
   public ModeSetting modeSetting;
   private static final HashSet<Block> set = new HashSet<>();
   private static final ItemStack itemStack2;
   private static final ItemStack itemStack3;
   private static final ItemStack itemStack4;
   private final ConcurrentHashMap<BlockPos, StructureInfoModule.SubContext> map = new ConcurrentHashMap<>();
   private final ConcurrentHashMap<BlockPos, Block> map2 = new ConcurrentHashMap<>();
   private final HashSet<UUID> set2 = new HashSet<>();
   private ClientWorld clientWorld;
   private final EventListener<PacketEvent> packetListener = packetEvent -> {
      if (packetEvent.isIncoming() && mc.world != null && mc.player != null) {
         if (packetEvent.getPacket() instanceof ChunkDeltaUpdateS2CPacket chunkDeltaUpdateS2CPacket) {
            if (this.modeSetting.isSelected("FunTime/SpookyTime")) {
               mc.execute(() -> chunkDeltaUpdateS2CPacket.visitUpdates((blockPos, blockState) -> {
                  if (!blockState.isAir() && set.contains(blockState.getBlock())) {
                     this.map2.put(blockPos.toImmutable(), blockState.getBlock());
                  }
               }));
            }
         } else if (packetEvent.getPacket() instanceof BlockUpdateS2CPacket blockUpdateS2CPacket) {
            if (this.modeSetting.isSelected("FunTime/SpookyTime")) {
               if (!blockUpdateS2CPacket.getState().isAir() && set.contains(blockUpdateS2CPacket.getState().getBlock())) {
                  mc.execute(() -> this.map2.put(blockUpdateS2CPacket.getPos().toImmutable(), blockUpdateS2CPacket.getState().getBlock()));
               }
            }
         } else if (packetEvent.getPacket() instanceof PlaySoundS2CPacket playSoundS2CPacket) {
            if (this.modeSetting.isSelected("HolyWorld")) {
               mc.execute(() -> {
                  if (mc.world != null && this.isEnabled()) {
                     BlockPos blockPos2 = new BlockPos((int)playSoundS2CPacket.getX(), (int)playSoundS2CPacket.getY(), (int)playSoundS2CPacket.getZ());
                     String local5242_3 = playSoundS2CPacket.getSound().value().id().toString();
                     if (this.modeSetting.isSelected("HolyWorld") && local5242_3.contains("block.beacon.deactivate")) {
                        this.map.put(blockPos2, new StructureInfoModule.SubContext(blockPos2, "Стан", itemStack4, 15000L, null, true));
                     }
                  }
               });
            }
         }
      }
   };
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      this.map.entrySet().removeIf(local5239_0 -> System.currentTimeMillis() - local5239_0.getValue().timeMillis > local5239_0.getValue().timeMillis2);
      if (mc.player != null && mc.world != null) {
         if (mc.world != this.clientWorld) {
            this.clientWorld = mc.world;
            this.map2.clear();
            this.map.clear();
            this.updateState();
         }

         Map<UUID, ClientBossBar> events = ((BossBarHudMixin)mc.inGameHud.getBossBarHud()).getEvents();
         HashSet<UUID> local5240_3 = new HashSet<>();

         for (StructureInfoModule.SubContext local5240_5 : this.map.values()) {
            if (local5240_5.box3 != null && this.matchesCriteria(local5240_5.box3)) {
               long local5240_6 = Math.max(0L, local5240_5.timeMillis2 - (System.currentTimeMillis() - local5240_5.timeMillis));
               Text text = Text.literal(
                     local5240_5.strVal + " " + (local5240_6 >= 60000L ? local5240_6 / 60000L + "мин" : (local5240_6 + 999L) / 1000L + "с")
                  )
                  .setStyle(Style.EMPTY.withColor(16777215).withBold(false));
               ClientBossBar clientBossBar = events.get(local5240_5.uUID2);
               if (clientBossBar == null) {
                  clientBossBar = new ClientBossBar(
                     local5240_5.uUID2,
                     text,
                     (float)local5240_6 / (float)local5240_5.timeMillis2,
                     Color.PURPLE,
                     net.minecraft.entity.boss.BossBar.Style.PROGRESS,
                     false,
                     false,
                     false
                  );
                  events.put(local5240_5.uUID2, clientBossBar);
               }

               clientBossBar.setName(text);
               clientBossBar.setPercent((float)local5240_6 / (float)local5240_5.timeMillis2);
               local5240_3.add(local5240_5.uUID2);
               this.set2.add(local5240_5.uUID2);
            }
         }

         this.set2.removeIf(uUID -> {
            if (local5240_3.contains(uUID)) {
               return false;
            }

            events.remove(uUID);
            return true;
         });
         if (!this.map2.isEmpty()) {
            if (this.map2.size() >= 2) {
               HashSet<Block> local5240_4 = new HashSet<>(this.map2.values());
               StructureInfoModule.TypeOption3 local5240_5 = null;

               for (StructureInfoModule.TypeOption3 local5240_9 : StructureInfoModule.TypeOption3.values()) {
                  if (local5240_9.set5.equals(local5240_4)) {
                     local5240_5 = local5240_9;
                     break;
                  }
               }

               StructureInfoModule.TypeOption2 local5240_6 = null;
               StructureInfoModule.TypeOption2[] nested0480Array = StructureInfoModule.TypeOption2.values();
               int local5240_8 = nested0480Array.length;
               int local5240_9 = 0;

               label115: {
                  StructureInfoModule.TypeOption2 local5240_10;
                  while (true) {
                     if (local5240_9 >= local5240_8) {
                        break label115;
                     }

                     local5240_10 = nested0480Array[local5240_9];
                     if (local5240_10.set3.equals(local5240_4)) {
                        break;
                     }

                     if (!local5240_10.set4.isEmpty()) {
                        HashSet<Block> local5240_11 = new HashSet<>(local5240_10.set3);
                        local5240_11.addAll(local5240_10.set4);
                        if (local5240_11.equals(local5240_4)) {
                           break;
                        }
                     }

                     local5240_9++;
                  }

                  local5240_6 = local5240_10;
               }

               if (local5240_5 != null || local5240_6 != null) {
                  int local5240_7 = Integer.MAX_VALUE;
                  local5240_8 = Integer.MAX_VALUE;
                  local5240_9 = Integer.MAX_VALUE;
                  int local5240_10 = Integer.MIN_VALUE;
                  int local5240_11 = Integer.MIN_VALUE;
                  int local5240_12 = Integer.MIN_VALUE;

                  for (BlockPos blockPos3 : this.map2.keySet()) {
                     local5240_7 = Math.min(local5240_7, blockPos3.getX());
                     local5240_8 = Math.min(local5240_8, blockPos3.getY());
                     local5240_9 = Math.min(local5240_9, blockPos3.getZ());
                     local5240_10 = Math.max(local5240_10, blockPos3.getX() + 1);
                     local5240_11 = Math.max(local5240_11, blockPos3.getY() + 1);
                     local5240_12 = Math.max(local5240_12, blockPos3.getZ() + 1);
                  }

                  Box box = new Box(local5240_7, local5240_8, local5240_9, local5240_10, local5240_11, local5240_12);
                  BlockPos blockPos4 = BlockPos.ofFloored(box.getCenter());
                  boolean local5240_15 = mc.getCurrentServerEntry() != null
                     && mc.getCurrentServerEntry().address.toLowerCase(Locale.ROOT).contains("spooky");
                  TargetHudStyle.processData4(
                     local5240_5 != null,
                     local5240_5 != null ? local5240_5 == StructureInfoModule.TypeOption3.DRAGON : local5240_6 == StructureInfoModule.TypeOption2.DRAGON
                  );
                  if (local5240_5 != null) {
                     this.map
                        .put(
                           blockPos4,
                           new StructureInfoModule.SubContext(blockPos4, local5240_5.strVal3, itemStack2, local5240_5.timeMillis3, box, false)
                        );
                  } else {
                     this.map
                        .put(
                           blockPos4,
                           new StructureInfoModule.SubContext(
                              blockPos4,
                              local5240_6.strVal2,
                              itemStack3,
                              local5240_6 == StructureInfoModule.TypeOption2.DRAGON && local5240_15
                                 ? 20000L
                                 : (box.getLengthY() <= 2.0 ? 60000L : 20000L),
                              box,
                              false
                           )
                        );
                  }
               }
            }

            this.map2.clear();
         }
      } else {
         this.map2.clear();
      }
   };
   private final EventListener<WorldRenderEvent> worldRenderListener = worldRenderEvent -> {
      if (mc.player != null && mc.world != null && this.modeSetting.isSelected("HolyWorld")) {
         for (StructureInfoModule.SubContext local5236_3 : this.map.values()) {
            if (local5236_3.flag
               && !(mc.player.getEntityPos().distanceTo(Vec3d.ofCenter(local5236_3.blockPos5)) > 100.0)
               && WorldProjection.getVec3d(Vec3d.ofCenter(local5236_3.blockPos5), 1.0F, 1.0F) != null) {
               Box box2 = new Box(local5236_3.blockPos5.up()).contract(0.0, 0.2, 0.0).expand(1.99);
               worldRenderEvent.getMatrixStack().push();
               worldRenderEvent.getMatrixStack()
                  .translate(
                     box2.minX - mc.gameRenderer.getCamera().getCameraPos().x,
                     box2.minY - mc.gameRenderer.getCamera().getCameraPos().y,
                     box2.minZ - mc.gameRenderer.getCamera().getCameraPos().z
                  );
               Matrix4f positionMatrix = worldRenderEvent.getMatrixStack().peek().getPositionMatrix();
               VertexConsumer vertexConsumer = mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayers.debugQuads());
               float[] floatArray = new float[]{0.0F, (float)(box2.maxX - box2.minX)};
               float[] floatArray2 = new float[]{0.0F, (float)(box2.maxY - box2.minY)};
               float[] floatArray3 = new float[]{0.0F, (float)(box2.maxZ - box2.minZ)};
               int[][] intArrayArray = new int[][]{{0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}};

               for (int[] intArray : intArrayArray) {
                  for (int local5236_18 : intArray) {
                     vertexConsumer.vertex(positionMatrix, floatArray[local5236_18 >> 2], floatArray2[local5236_18 >> 1 & 1], floatArray3[local5236_18 & 1])
                        .color(Colors.computeInt6(ThemeColors.intVal9, 0.3529412F));
                  }
               }

               VertexRendering.drawOutline(
                  worldRenderEvent.getMatrixStack(),
                  mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayers.lines()),
                  VoxelShapes.cuboid(
                     new Box(0.0, 0.0, 0.0, box2.maxX - box2.minX, box2.maxY - box2.minY, box2.maxZ - box2.minZ)
                  ),
                  0.0,
                  0.0,
                  0.0,
                  Colors.computeInt6(ThemeColors.intVal9, 0.55F),
                  1.0F
               );
               mc.getBufferBuilders().getEntityVertexConsumers().draw();
               worldRenderEvent.getMatrixStack().pop();
            }
         }
      }
   };

   @Override
   protected void onEnable() {
      this.map.clear();
      this.map2.clear();
      this.updateState();
   }

   @Override
   protected void onDisable() {
      this.map.clear();
      this.map2.clear();
      this.updateState();
   }

   public void onHudRender(HudRenderEvent event) {
      if (mc.player != null && mc.world != null) {
         for (StructureInfoModule.SubContext local5233_3 : this.map.values()) {
            if (!(mc.player.getEntityPos().distanceTo(local5233_3.vec3d2) > 100.0) && !(this.computeFloat(local5233_3) < 0.01F)) {
               local5233_3.animatedValue.processData(local5233_3.box3 != null && this.matchesCriteria(local5233_3.box3) ? 0.0 : 1.0);
               if (!(this.computeFloat(local5233_3) * local5233_3.animatedValue.computeDouble() < 0.01F)) {
                  Vec3d vec3d = WorldProjection.getVec3d2(local5233_3.vec3d2, event.computeInt2(), event.computeInt());
                  if (vec3d != null) {
                     float local5233_5 = (float)mc.gameRenderer.getCamera().getCameraPos().distanceTo(local5233_3.vec3d2);
                     HudRenderContext.processData3(
                        (float)vec3d.x * GuiScale.computeInt() / GuiScale.computeFloat(),
                        (float)vec3d.y * GuiScale.computeInt() / GuiScale.computeFloat(),
                        Math.max(0.3F, 1.0F - local5233_5 * 0.008F)
                           * Math.max(0.7F, 1.0F - local5233_5 * 0.005F)
                           * (0.85F + 0.15F * this.computeFloat(local5233_3) * (float)local5233_3.animatedValue.computeDouble())
                     );
                     this.processData(
                        local5233_3,
                        (float)vec3d.x * GuiScale.computeInt() / GuiScale.computeFloat(),
                        (float)vec3d.y * GuiScale.computeInt() / GuiScale.computeFloat()
                     );
                     HudRenderContext.updateState();
                  }
               }
            }
         }
      }
   }

   private StructureInfoModule() {
      this.modeSetting = this.addSetting(
         new ModeSetting("Сервер", "На каком сервере определять структуры.", "FunTime/SpookyTime", "FunTime/SpookyTime", "HolyWorld")
      );
   }

   private void processData(StructureInfoModule.SubContext local5235_1, float local5235_2, float local5235_3) {
      long local5235_4 = Math.max(0L, local5235_1.timeMillis2 - (System.currentTimeMillis() - local5235_1.timeMillis));
      String local5235_6 = local5235_4 >= 60000L ? local5235_4 / 60000L + "мин" : (local5235_4 + 999L) / 1000L + "с";
      float local5235_7 = this.computeFloat(local5235_1) * (float)local5235_1.animatedValue.computeDouble();
      float local5235_8 = 13.0F;
      float local5235_9 = FontText.computeFloat(FontFace.SEMI_BOLD, local5235_1.strVal, 6.0F);
      float local5235_10 = FontText.computeFloat(FontFace.SEMI_BOLD, local5235_6, 6.0F);
      float local5235_11 = 16.0F + local5235_9 + 3.0F + local5235_10 + 4.0F;
      float local5235_12 = Math.round((local5235_2 - local5235_11 / 2.0F) * 2.0F) / 2.0F;
      float local5235_13 = Math.round((local5235_3 - local5235_8 / 2.0F) * 2.0F) / 2.0F;
      HudStyle.processData2(local5235_12, local5235_13, local5235_11, local5235_8, local5235_7, 4.5F, ShapeDrawing.TypeOption.LOW);
      ItemRenderCache.processData(local5235_1.itemStack5, local5235_12 + 4.0F, local5235_13 + 2.5F, 8.0F, ItemDrawOptions.noDecorations(local5235_7));
      float local5235_14 = local5235_13 + local5235_8 / 2.0F - 3.5F;
      FontText.processData3(
         FontFace.SEMI_BOLD, local5235_1.strVal, local5235_12 + 16.0F, local5235_14, 6.0F, HudStyle.computeInt4(local5235_7)
      );
      FontText.processData3(
         FontFace.SEMI_BOLD, local5235_6, local5235_12 + 16.0F + local5235_9 + 3.0F, local5235_14, 6.0F, HudStyle.computeInt2(local5235_7)
      );
   }

   private boolean matchesCriteria(Box box) {
      return mc.player.getX() >= box.minX
         && mc.player.getX() < box.maxX
         && mc.player.getZ() >= box.minZ
         && mc.player.getZ() < box.maxZ
         && mc.player.getY() >= box.minY - 1.0
         && mc.player.getY() < box.maxY + 1.0;
   }

   private float computeFloat(StructureInfoModule.SubContext local5243_1) {
      float local5243_2 = Math.min(
         Math.min(1.0F, (float)(System.currentTimeMillis() - local5243_1.timeMillis) / 220.0F),
         Math.min(1.0F, (float)(local5243_1.timeMillis2 - (System.currentTimeMillis() - local5243_1.timeMillis)) / 220.0F)
      );
      return local5243_2 * local5243_2 * (3.0F - 2.0F * local5243_2);
   }

   static {
      for (StructureInfoModule.TypeOption3 local5244_3 : StructureInfoModule.TypeOption3.values()) {
         set.addAll(local5244_3.set5);
      }

      for (StructureInfoModule.TypeOption2 local5244_3 : StructureInfoModule.TypeOption2.values()) {
         set.addAll(local5244_3.set3);
         set.addAll(local5244_3.set4);
      }

      itemStack2 = Items.NETHERITE_SCRAP.getDefaultStack();
      itemStack3 = Items.DRIED_KELP.getDefaultStack();
      itemStack4 = Items.NETHER_STAR.getDefaultStack();
   }

   public static StructureInfoModule getInstance() {
      if (structureInfoModule == null) {
         structureInfoModule = new StructureInfoModule();
      }

      return structureInfoModule;
   }

   private void updateState() {
      if (mc.inGameHud != null) {
         Map<UUID, ClientBossBar> events = ((BossBarHudMixin)mc.inGameHud.getBossBarHud()).getEvents();

         for (UUID uUID : this.set2) {
            events.remove(uUID);
         }

         this.set2.clear();
      }
   }

   private static final class SubContext {
      final BlockPos blockPos5;
      final Vec3d vec3d2;
      final String strVal;
      final ItemStack itemStack5;
      final long timeMillis;
      final long timeMillis2;
      final Box box3;
      final boolean flag;
      final UUID uUID2;
      final AnimatedValue animatedValue = new AnimatedValue(220L).getAnimatedValue(1.0);

      SubContext(BlockPos blockPos, String local5249_2, ItemStack itemStack, long local5249_4, Box box, boolean local5249_7) {
         this.blockPos5 = blockPos.toImmutable();
         this.vec3d2 = box == null ? Vec3d.ofCenter(blockPos) : box.getCenter();
         this.strVal = local5249_2;
         this.itemStack5 = itemStack;
         this.timeMillis = System.currentTimeMillis();
         this.timeMillis2 = local5249_4;
         this.box3 = box;
         this.flag = local5249_7;
         this.uUID2 = UUID.randomUUID();
      }
   }

   private enum TypeOption2 {
      DEFAULT("Обычный", Set.of(Blocks.ANDESITE, Blocks.COBBLESTONE, Blocks.DEAD_TUBE_CORAL_BLOCK), Set.of()),
      DRAGON(
         "Драконий",
         Set.of(
            Blocks.CHISELED_POLISHED_BLACKSTONE,
            Blocks.LODESTONE,
            Blocks.POLISHED_BASALT,
            Blocks.POLISHED_BLACKSTONE,
            Blocks.POLISHED_BLACKSTONE_BRICKS,
            Blocks.SMOOTH_STONE
         ),
         Set.of(Blocks.NETHERITE_BLOCK)
      ),
      PEARL("Жемчужный", Set.of(Blocks.DARK_PRISMARINE, Blocks.PRISMARINE, Blocks.PRISMARINE_BRICKS), Set.of()),
      MYSTIC("Мистический", Set.of(Blocks.END_STONE_BRICKS, Blocks.PURPLE_STAINED_GLASS, Blocks.PURPUR_BLOCK, Blocks.PURPUR_PILLAR), Set.of()),
      GHOST("Призрачный", Set.of(Blocks.CHISELED_QUARTZ_BLOCK, Blocks.QUARTZ_PILLAR, Blocks.SMOOTH_QUARTZ, Blocks.WHITE_STAINED_GLASS), Set.of()),
      NIGHTMARISH("Кошмарный", Set.of(Blocks.BLACKSTONE, Blocks.BONE_BLOCK, Blocks.POLISHED_BASALT), Set.of()),
      SPOOKY(
         "Зловещий",
         Set.of(Blocks.INFESTED_CRACKED_STONE_BRICKS, Blocks.INFESTED_MOSSY_STONE_BRICKS, Blocks.INFESTED_STONE_BRICKS, Blocks.LODESTONE),
         Set.of(Blocks.INFESTED_CHISELED_STONE_BRICKS)
      ),
      LETHAL("Смертельный", Set.of(Blocks.CHISELED_NETHER_BRICKS, Blocks.MAGMA_BLOCK, Blocks.NETHER_BRICKS, Blocks.RED_NETHER_BRICKS), Set.of()),
      CRUEL(
         "Жестокий",
         Set.of(Blocks.BLACKSTONE, Blocks.CRYING_OBSIDIAN, Blocks.GILDED_BLACKSTONE, Blocks.NETHERITE_BLOCK, Blocks.POLISHED_BLACKSTONE_BRICKS),
         Set.of()
      ),
      ICY("Ледяной", Set.of(Blocks.ICE, Blocks.PACKED_ICE, Blocks.SNOW_BLOCK), Set.of()),
      INSIDIOUS("Коварный", Set.of(Blocks.CHISELED_NETHER_BRICKS, Blocks.NETHER_BRICKS, Blocks.NETHER_WART_BLOCK, Blocks.RED_NETHER_BRICKS), Set.of()),
      CURSED(
         "Проклятый",
         Set.of(Blocks.BLACKSTONE, Blocks.CHISELED_POLISHED_BLACKSTONE, Blocks.CRYING_OBSIDIAN, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.RESPAWN_ANCHOR),
         Set.of()
      ),
      EXCRUCIATING("Мучительный", Set.of(Blocks.BLACKSTONE, Blocks.COAL_BLOCK, Blocks.NETHERITE_BLOCK, Blocks.POLISHED_BLACKSTONE_BRICKS), Set.of()),
      ROYAL(
         "Царский", Set.of(Blocks.DIORITE, Blocks.LIGHT_GRAY_STAINED_GLASS, Blocks.NETHERITE_BLOCK, Blocks.POLISHED_DIORITE), Set.of(Blocks.GILDED_BLACKSTONE)
      ),
      SINGULAR("Сингулярный", Set.of(Blocks.BLACK_CONCRETE, Blocks.COAL_BLOCK), Set.of()),
      UNVOIDABLE("Неизбежный", Set.of(Blocks.ANDESITE, Blocks.DEAD_TUBE_CORAL_BLOCK, Blocks.POLISHED_BASALT), Set.of());

      final String strVal2;
      final Set<Block> set3;
      final Set<Block> set4;

      TypeOption2(String local5252_3, Set<Block> local5252_4, Set<Block> local5252_5) {
         this.strVal2 = local5252_3 + " пласт";
         this.set3 = local5252_4;
         this.set4 = local5252_5;
      }
   }

   private enum TypeOption3 {
      DEFAULT("Обычная", 15000L, Set.of(Blocks.ANDESITE, Blocks.ANDESITE_STAIRS, Blocks.COBBLESTONE, Blocks.DEAD_TUBE_CORAL_BLOCK)),
      DRAGON(
         "Драконья",
         30000L,
         Set.of(
            Blocks.CHISELED_POLISHED_BLACKSTONE,
            Blocks.LODESTONE,
            Blocks.NETHERITE_BLOCK,
            Blocks.POLISHED_BASALT,
            Blocks.POLISHED_BLACKSTONE,
            Blocks.POLISHED_BLACKSTONE_BRICK_SLAB,
            Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS,
            Blocks.POLISHED_BLACKSTONE_BRICKS,
            Blocks.RESPAWN_ANCHOR,
            Blocks.SMOOTH_STONE_SLAB
         )
      ),
      PEARL(
         "Жемчужная",
         15000L,
         Set.of(
            Blocks.DARK_PRISMARINE, Blocks.DARK_PRISMARINE_SLAB, Blocks.DARK_PRISMARINE_STAIRS, Blocks.PRISMARINE, Blocks.PRISMARINE_BRICKS, Blocks.SEA_LANTERN
         )
      ),
      MYSTIC(
         "Мистическая",
         15000L,
         Set.of(
            Blocks.END_STONE,
            Blocks.END_STONE_BRICK_WALL,
            Blocks.END_STONE_BRICKS,
            Blocks.PURPLE_STAINED_GLASS,
            Blocks.PURPUR_BLOCK,
            Blocks.PURPUR_PILLAR,
            Blocks.PURPUR_STAIRS
         )
      ),
      GHOST(
         "Призрачная",
         15000L,
         Set.of(
            Blocks.CHISELED_QUARTZ_BLOCK,
            Blocks.DIORITE,
            Blocks.QUARTZ_PILLAR,
            Blocks.QUARTZ_STAIRS,
            Blocks.SEA_LANTERN,
            Blocks.SMOOTH_QUARTZ_SLAB,
            Blocks.WHITE_STAINED_GLASS
         )
      ),
      NIGHTMARISH(
         "Кошмарная", 15000L, Set.of(Blocks.BLACKSTONE, Blocks.BLACKSTONE_SLAB, Blocks.BONE_BLOCK, Blocks.CHISELED_QUARTZ_BLOCK, Blocks.POLISHED_BASALT)
      ),
      SPOOKY(
         "Зловещая",
         15000L,
         Set.of(
            Blocks.INFESTED_CHISELED_STONE_BRICKS,
            Blocks.INFESTED_CRACKED_STONE_BRICKS,
            Blocks.INFESTED_MOSSY_STONE_BRICKS,
            Blocks.INFESTED_STONE_BRICKS,
            Blocks.LODESTONE,
            Blocks.STONE_BRICK_SLAB,
            Blocks.STONE_BRICK_STAIRS,
            Blocks.STONE_BRICK_WALL
         )
      ),
      LETHAL(
         "Смертельная",
         15000L,
         Set.of(
            Blocks.CHISELED_NETHER_BRICKS,
            Blocks.MAGMA_BLOCK,
            Blocks.NETHER_BRICKS,
            Blocks.RED_NETHER_BRICK_STAIRS,
            Blocks.RED_NETHER_BRICK_WALL,
            Blocks.RED_NETHER_BRICKS
         )
      ),
      CRUEL(
         "Жестокая",
         15000L,
         Set.of(
            Blocks.BLACKSTONE,
            Blocks.BLACKSTONE_SLAB,
            Blocks.CRYING_OBSIDIAN,
            Blocks.GILDED_BLACKSTONE,
            Blocks.NETHERITE_BLOCK,
            Blocks.POLISHED_BLACKSTONE_BRICK_SLAB,
            Blocks.POLISHED_BLACKSTONE_BRICKS
         )
      ),
      ICY("Ледяная", 15000L, Set.of(Blocks.ICE, Blocks.PACKED_ICE, Blocks.SNOW, Blocks.SNOW_BLOCK, Blocks.WHITE_CONCRETE)),
      INSIDIOUS(
         "Коварная",
         15000L,
         Set.of(
            Blocks.CHISELED_NETHER_BRICKS,
            Blocks.NETHER_BRICK_SLAB,
            Blocks.NETHER_BRICK_STAIRS,
            Blocks.NETHER_BRICKS,
            Blocks.NETHER_WART_BLOCK,
            Blocks.RED_NETHER_BRICK_STAIRS,
            Blocks.RED_NETHER_BRICKS
         )
      ),
      CURSED(
         "Проклятая",
         15000L,
         Set.of(
            Blocks.BLACKSTONE,
            Blocks.BLACKSTONE_SLAB,
            Blocks.BLACKSTONE_STAIRS,
            Blocks.CHISELED_POLISHED_BLACKSTONE,
            Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS,
            Blocks.CRYING_OBSIDIAN,
            Blocks.NETHERITE_BLOCK,
            Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS,
            Blocks.POLISHED_BLACKSTONE_BRICKS,
            Blocks.RESPAWN_ANCHOR
         )
      ),
      EXCRUCIATING(
         "Мучительная",
         15000L,
         Set.of(
            Blocks.BLACK_CONCRETE,
            Blocks.BLACKSTONE,
            Blocks.BLACKSTONE_STAIRS,
            Blocks.COAL_BLOCK,
            Blocks.NETHERITE_BLOCK,
            Blocks.POLISHED_BLACKSTONE_BRICK_SLAB,
            Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS,
            Blocks.POLISHED_BLACKSTONE_BRICKS,
            Blocks.POLISHED_BLACKSTONE_WALL
         )
      ),
      ROYAL(
         "Царская",
         15000L,
         Set.of(
            Blocks.DIORITE,
            Blocks.DIORITE_STAIRS,
            Blocks.DIORITE_WALL,
            Blocks.GILDED_BLACKSTONE,
            Blocks.LIGHT_GRAY_STAINED_GLASS,
            Blocks.NETHERITE_BLOCK,
            Blocks.POLISHED_DIORITE,
            Blocks.POLISHED_DIORITE_SLAB
         )
      ),
      SINGULAR("Сингулярная", 15000L, Set.of(Blocks.BLACK_CONCRETE, Blocks.BLACKSTONE_SLAB, Blocks.COAL_BLOCK)),
      UNVOIDABLE("Неизбежная", 15000L, Set.of(Blocks.ANDESITE, Blocks.ANDESITE_STAIRS, Blocks.DEAD_TUBE_CORAL_BLOCK, Blocks.POLISHED_BASALT));

      final String strVal3;
      final long timeMillis3;
      final Set<Block> set5;

      TypeOption3(String local5257_3, long local5257_4, Set<Block> local5257_6) {
         this.strVal3 = local5257_3 + " трапка";
         this.timeMillis3 = local5257_4;
         this.set5 = local5257_6;
      }
   }
}
