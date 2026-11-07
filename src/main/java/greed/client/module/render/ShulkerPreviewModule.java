package greed.client.module.render;

import greed.client.setting.BooleanSetting;
import greed.mixin.ContainerComponentMixin;
import greed.client.event.EventListener;
import greed.client.event.HudRenderEvent;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.PacketEvent;
import greed.client.event.PlayerTickEvent;
import greed.client.render.WorldProjection;
import greed.client.render.Render2D;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.HashMap;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.TypedEntityData;
import net.minecraft.entity.data.DataTracker.SerializedEntry;
import net.minecraft.item.BundleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(name = "ShulkerPreview", description = "Превью содержимого шалкера на земле и при наведении.", category = ModuleCategory.RENDER)
public final class ShulkerPreviewModule extends Module {
   private static ShulkerPreviewModule shulkerPreviewModule;
   private static final Identifier identifier = Identifier.of("greed", "textures/container/container.png");
   public BooleanSetting onGround;
   public BooleanSetting onHover;
   public BooleanSetting bundleOnGround;
   private final HashMap<TypedEntityData<BlockEntityType<?>>, ArrayList<ItemStack>> map = new HashMap<>();
   private final HashMap<Integer, ArrayList<ItemStack>> map2 = new HashMap<>();
   private final HashMap<Integer, ArrayList<ItemStack>> map3 = new HashMap<>();
   private final HashMap<Integer, ArrayList<ItemStack>> map4 = new HashMap<>();
   private final HashMap<Integer, ArrayList<ItemStack>> map5 = new HashMap<>();
   private final HashMap<Integer, Long> map6 = new HashMap<>();
   private final HashMap<Integer, ArrayList<ItemStack>> map7 = new HashMap<>();
   private ArrayList<ItemStack> list;
   private long timeMillis;
   private final EventListener<HudRenderEvent> hudRenderListener = hudRenderEvent -> {
      if (mc.player != null
         && mc.world != null
         && mc.gameRenderer != null
         && (this.onGround.isChecked() || this.bundleOnGround.isChecked())) {
         for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof ItemEntity itemEntity && !itemEntity.getStack().isEmpty()) {
               if (this.bundleOnGround.isChecked() && itemEntity.getStack().getItem() instanceof BundleItem && !this.getArrayList(itemEntity).isEmpty()) {
                  Vec3d vec3d = WorldProjection.getVec3d2(
                     itemEntity.getLerpedPos(hudRenderEvent.computeFloat()).add(0.0, 0.25, 0.0),
                     mc.getWindow().getScaledWidth(),
                     mc.getWindow().getScaledHeight()
                  );
                  if (vec3d != null) {
                     this.renderElement3(
                        hudRenderEvent.getDrawContext(),
                        (float)vec3d.x,
                        (float)vec3d.y
                           - TagsModule.getInstance()
                              .computeFloat2(
                                 (float)mc.gameRenderer.getCamera().getCameraPos().distanceTo(itemEntity.getLerpedPos(hudRenderEvent.computeFloat()))
                              )
                           - 2.0F,
                        Math.max(
                              0.5F,
                              1.1F
                                 - (float)mc.gameRenderer.getCamera().getCameraPos().distanceTo(itemEntity.getLerpedPos(hudRenderEvent.computeFloat()))
                                    * 0.006F
                           )
                           / mc.getWindow().getScaleFactor(),
                        this.getArrayList(itemEntity)
                     );
                  }

                  Render2D.bumpGuiLayer(hudRenderEvent.getDrawContext());
               } else if (this.onGround.isChecked() && Block.getBlockFromItem(itemEntity.getStack().getItem()) instanceof ShulkerBoxBlock shulkerBoxBlock) {
                  Vec3d vec3d2 = WorldProjection.getVec3d2(
                     itemEntity.getLerpedPos(hudRenderEvent.computeFloat()).add(0.0, 0.25, 0.0),
                     mc.getWindow().getScaledWidth(),
                     mc.getWindow().getScaledHeight()
                  );
                  if (vec3d2 != null) {
                     this.renderElement(
                        hudRenderEvent.getDrawContext(),
                        (float)vec3d2.x,
                        (float)vec3d2.y
                           - TagsModule.getInstance()
                              .computeFloat2(
                                 (float)mc.gameRenderer.getCamera().getCameraPos().distanceTo(itemEntity.getLerpedPos(hudRenderEvent.computeFloat()))
                              )
                           - 2.0F,
                        Math.max(
                              0.5F,
                              1.1F
                                 - (float)mc.gameRenderer.getCamera().getCameraPos().distanceTo(itemEntity.getLerpedPos(hudRenderEvent.computeFloat()))
                                    * 0.006F
                           )
                           / mc.getWindow().getScaleFactor(),
                        this.getArrayList3(itemEntity.getStack()),
                        this.computeInt(shulkerBoxBlock)
                     );
                  }

                  Render2D.bumpGuiLayer(hudRenderEvent.getDrawContext());
               }
            }
         }
      }
   };
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (mc.player != null) {
         for (int index = 0; index < 36; index++) {
            ItemStack stack = mc.player.getInventory().getStack(index);
            boolean local4768_4 = !stack.isEmpty()
               && stack.getItem() instanceof BundleItem
               && stack.get(DataComponentTypes.BUNDLE_CONTENTS) != null
               && !stack.get(DataComponentTypes.BUNDLE_CONTENTS).isEmpty();
            if (!local4768_4) {
               if (this.map7.containsKey(index)) {
                  this.list = this.map7.remove(index);
                  this.timeMillis = System.currentTimeMillis();
               }
            } else {
               ArrayList<ItemStack> local4768_5 = this.map7.get(index);
               if (local4768_5 == null || local4768_5.size() != stack.get(DataComponentTypes.BUNDLE_CONTENTS).size()) {
                  ArrayList<ItemStack> local4768_6 = new ArrayList<>();

                  for (ItemStack itemStack : stack.get(DataComponentTypes.BUNDLE_CONTENTS).iterateCopy()) {
                     local4768_6.add(itemStack);
                  }

                  this.map7.put(index, local4768_6);
               }
            }
         }
      }
   };
   private final EventListener<PacketEvent> packetListener = packetEvent -> {
      if (packetEvent.isIncoming()) {
         if (packetEvent.getPacket() instanceof EntityEquipmentUpdateS2CPacket entityEquipmentUpdateS2CPacket) {
            for (Pair<EquipmentSlot, ItemStack> local4755_4 : entityEquipmentUpdateS2CPacket.getEquipmentList()) {
               if (local4755_4.getFirst() == EquipmentSlot.MAINHAND || local4755_4.getFirst() == EquipmentSlot.OFFHAND) {
                  int local4755_5 = entityEquipmentUpdateS2CPacket.getEntityId() * 2 + (local4755_4.getFirst() == EquipmentSlot.OFFHAND ? 1 : 0);
                  ItemStack itemStack2 = (ItemStack)local4755_4.getSecond();
                  boolean local4755_7 = !itemStack2.isEmpty()
                     && itemStack2.getItem() instanceof BundleItem
                     && itemStack2.get(DataComponentTypes.BUNDLE_CONTENTS) != null
                     && !itemStack2.get(DataComponentTypes.BUNDLE_CONTENTS).isEmpty();
                  if (local4755_7) {
                     if (this.map4.size() >= 256) {
                        this.map4.clear();
                     }

                     ArrayList<ItemStack> local4755_8 = new ArrayList<>();

                     for (ItemStack itemStack3 : itemStack2.get(DataComponentTypes.BUNDLE_CONTENTS).iterateCopy()) {
                        local4755_8.add(itemStack3);
                     }

                     this.map4.put(local4755_5, local4755_8);
                  } else if (this.map4.containsKey(local4755_5)) {
                     if (this.map5.size() >= 256) {
                        this.map5.clear();
                        this.map6.clear();
                     }

                     this.map5.put(local4755_5, this.map4.remove(local4755_5));
                     this.map6.put(local4755_5, System.currentTimeMillis());
                  }
               }
            }
         } else if (packetEvent.getPacket() instanceof EntityTrackerUpdateS2CPacket entityTrackerUpdateS2CPacket) {
            for (SerializedEntry<?> local4755_4 : entityTrackerUpdateS2CPacket.trackedValues()) {
               if (local4755_4.value() instanceof ItemStack itemStack4 && !itemStack4.isEmpty() && itemStack4.getItem() instanceof BundleItem) {
                  if (this.map2.size() >= 256) {
                     this.map2.clear();
                  }

                  if (this.list != null && System.currentTimeMillis() - this.timeMillis < 3000L) {
                     this.map2.put(entityTrackerUpdateS2CPacket.id(), this.list);
                  } else {
                     if (this.map3.size() >= 256) {
                        this.map3.clear();
                     }

                     if (!this.map3.containsKey(entityTrackerUpdateS2CPacket.id())
                        && itemStack4.get(DataComponentTypes.BUNDLE_CONTENTS) != null
                        && !itemStack4.get(DataComponentTypes.BUNDLE_CONTENTS).isEmpty()) {
                        ArrayList<ItemStack> local4755_6 = new ArrayList<>();

                        for (ItemStack itemStack5 : itemStack4.get(DataComponentTypes.BUNDLE_CONTENTS).iterateCopy()) {
                           local4755_6.add(itemStack5);
                        }

                        this.map3.put(entityTrackerUpdateS2CPacket.id(), local4755_6);
                     }
                  }
               }
            }
         }
      }
   };

   private int computeInt(ShulkerBoxBlock shulkerBoxBlock) {
      return shulkerBoxBlock.getColor() != null ? 0xFF000000 | shulkerBoxBlock.getColor().getEntityColor() : -6660960;
   }

   private ArrayList<ItemStack> getArrayList(ItemEntity itemEntity) {
      ArrayList<ItemStack> local4757_2 = this.map2.get(itemEntity.getId());
      if (local4757_2 != null) {
         return local4757_2;
      }

      int local4757_3 = -1;
      double local4757_4 = 64.0;

      for (Integer local4757_7 : this.map5.keySet()) {
         if (System.currentTimeMillis() - this.map6.getOrDefault(local4757_7, 0L) <= 5000L) {
            Entity entityById = mc.world.getEntityById(local4757_7 / 2);
            if (entityById != null && !(entityById.squaredDistanceTo(itemEntity) >= local4757_4)) {
               local4757_4 = entityById.squaredDistanceTo(itemEntity);
               local4757_3 = local4757_7;
            }
         }
      }

      if (local4757_3 != -1) {
         ArrayList<ItemStack> local4757_6 = this.map5.remove(local4757_3);
         this.map6.remove(local4757_3);
         this.map2.put(itemEntity.getId(), local4757_6);
         return local4757_6;
      }

      ArrayList<ItemStack> local4757_6 = this.map3.get(itemEntity.getId());
      if (local4757_6 != null) {
         return local4757_6;
      }

      ArrayList<ItemStack> local4757_7 = new ArrayList<>();
      if (itemEntity.getStack().get(DataComponentTypes.BUNDLE_CONTENTS) != null) {
         for (ItemStack itemStack : itemEntity.getStack().get(DataComponentTypes.BUNDLE_CONTENTS).iterate()) {
            local4757_7.add(itemStack);
         }
      }

      return local4757_7;
   }

   private ArrayList<ItemStack> getArrayList2(ItemStack itemStack) {
      DefaultedList<ItemStack> local4758_2 = DefaultedList.ofSize(27, ItemStack.EMPTY);
      if (itemStack.get(DataComponentTypes.CONTAINER) != null) {
         DefaultedList<ItemStack> items = ((ContainerComponentMixin)(Object)itemStack.get(DataComponentTypes.CONTAINER)).getItems();

         for (int index = 0; index < items.size() && index < 27; index++) {
            local4758_2.set(index, items.get(index));
         }
      }

      if (local4758_2.stream().allMatch(itemStack2 -> itemStack2.isEmpty())
         && itemStack.<TypedEntityData<BlockEntityType<?>>>get(DataComponentTypes.BLOCK_ENTITY_DATA) != null) {
         ArrayList<ItemStack> local4758_3 = this.map.get(itemStack.get(DataComponentTypes.BLOCK_ENTITY_DATA));
         if (local4758_3 != null) {
            return local4758_3;
         }

         ShulkerBoxBlockEntity shulkerBoxBlockEntity = new ShulkerBoxBlockEntity(BlockPos.ORIGIN, Blocks.SHULKER_BOX.getDefaultState());
         if (itemStack.get(DataComponentTypes.BLOCK_ENTITY_DATA).applyToBlockEntity(shulkerBoxBlockEntity, mc.world.getRegistryManager())) {
            for (int index2 = 0; index2 < 27; index2++) {
               local4758_2.set(index2, shulkerBoxBlockEntity.getStack(index2));
            }
         }

         if (this.map.size() >= 256) {
            this.map.clear();
         }

         this.map.put(itemStack.get(DataComponentTypes.BLOCK_ENTITY_DATA), new ArrayList<>(local4758_2));
      }

      return new ArrayList<>(local4758_2);
   }

   private void renderElement(DrawContext drawContext, float local4760_2, float local4760_3, float local4760_4, ArrayList<ItemStack> local4760_5, int local4760_6) {
      drawContext.getMatrices().pushMatrix();
      drawContext.getMatrices().translate(local4760_2, local4760_3);
      drawContext.getMatrices().scale(local4760_4, local4760_4);
      drawContext.drawTexture(RenderPipelines.GUI_TEXTURED, identifier, -88, -73, 0.0F, 0.0F, 176, 67, 176, 67, local4760_6);

      for (int index = 0; index < Math.min(local4760_5.size(), 27); index++) {
         if (!local4760_5.get(index).isEmpty()) {
            drawContext.getMatrices().pushMatrix();
            drawContext.getMatrices().translate(-72.0F + index % 9 * 18.0F, -57.0F + index / 9 * 18.0F);
            drawContext.getMatrices().scale(0.85F, 0.85F);
            drawContext.drawItem(local4760_5.get(index), -8, -8);
            drawContext.drawStackOverlay(mc.textRenderer, local4760_5.get(index), -8, -8);
            drawContext.getMatrices().popMatrix();
         }
      }

      drawContext.getMatrices().popMatrix();
   }

   public static ShulkerPreviewModule getInstance() {
      if (shulkerPreviewModule == null) {
         shulkerPreviewModule = new ShulkerPreviewModule();
      }

      return shulkerPreviewModule;
   }

   private ShulkerPreviewModule() {
      this.onGround = this.addSetting(new BooleanSetting("На земле", "Показывать содержимое шалкера, лежащего на земле.", true));
      this.onHover = this.addSetting(new BooleanSetting("При наведении", "Показывать содержимое при наведении на шалкер в инвентаре.", true));
      this.bundleOnGround = this.addSetting(new BooleanSetting("Мешок на земле", "Показывать содержимое мешка, лежащего на земле.", true));
   }

   public void renderElement2(DrawContext drawContext, ItemStack itemStack, int local4765_3, int local4765_4) {
      if (this.isEnabled() && this.onHover.isChecked() && Block.getBlockFromItem(itemStack.getItem()) instanceof ShulkerBoxBlock shulkerBoxBlock) {
         ArrayList<ItemStack> local4765_6 = this.getArrayList3(itemStack);
         if (local4765_6.stream().anyMatch(itemStack2 -> !itemStack2.isEmpty())) {
            this.renderElement(drawContext, local4765_3, local4765_4 - 6.0F, 0.75F, local4765_6, this.computeInt(shulkerBoxBlock));
         }
      }
   }

   private ArrayList<ItemStack> getArrayList3(ItemStack itemStack) {
      return this.getArrayList2(itemStack);
   }

   private void renderElement3(DrawContext drawContext, float local4767_2, float local4767_3, float local4767_4, ArrayList<ItemStack> local4767_5) {
      int local4767_6 = Math.max(2, (int)Math.ceil(Math.sqrt(local4767_5.size() + 1.0)));
      int local4767_7 = (int)Math.ceil((double)local4767_5.size() / local4767_6);
      int local4767_8 = local4767_6 * 18 + 8;
      int local4767_9 = local4767_7 * 18 + 8;
      drawContext.getMatrices().pushMatrix();
      drawContext.getMatrices().translate(local4767_2, local4767_3);
      drawContext.getMatrices().scale(local4767_4, local4767_4);
      drawContext.fill(-local4767_8 / 2 - 1, -local4767_9 - 1, local4767_8 / 2 + 1, 1, 1347420415);
      drawContext.fill(-local4767_8 / 2, -local4767_9, local4767_8 / 2, 0, -267386864);

      for (int index = 0; index < local4767_5.size(); index++) {
         if (!local4767_5.get(index).isEmpty()) {
            int local4767_11 = -local4767_8 / 2 + 4 + index % local4767_6 * 18;
            int local4767_12 = -local4767_9 + 4 + index / local4767_6 * 18;
            drawContext.fill(local4767_11, local4767_12, local4767_11 + 18, local4767_12 + 18, 1720421259);
            drawContext.drawItem(local4767_5.get(index), local4767_11 + 1, local4767_12 + 1);
            drawContext.drawStackOverlay(mc.textRenderer, local4767_5.get(index), local4767_11 + 1, local4767_12 + 1);
         }
      }

      drawContext.getMatrices().popMatrix();
   }
}
