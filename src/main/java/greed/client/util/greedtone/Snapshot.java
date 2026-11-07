package greed.client.util.greedtone;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

public final class Snapshot {

    private final Long2ObjectOpenHashMap<WorldChunk> chunks;
    private final ItemStack[] hotbar;
    private final boolean placeable;
    public static boolean canFly;
    private Snapshot(Long2ObjectOpenHashMap<WorldChunk> chunks, ItemStack[] hotbar, boolean placeable) {
        this.chunks = chunks;
        this.hotbar = hotbar;
        this.placeable = placeable;
    }

    public static Snapshot capture(MinecraftClient mc) {
        ClientWorld world = mc.world;
        ClientPlayerEntity player = mc.player;
        Long2ObjectOpenHashMap<WorldChunk> map = new Long2ObjectOpenHashMap<>();
        int radius = Math.min(mc.options.getClampedViewDistance(), 32);
        int pcx = player.getBlockPos().getX() >> 4;
        int pcz = player.getBlockPos().getZ() >> 4;
        for (int cx = pcx - radius; cx <= pcx + radius; cx++) {
            for (int cz = pcz - radius; cz <= pcz + radius; cz++) {
                WorldChunk chunk = world.getChunkManager().getWorldChunk(cx, cz);
                if (chunk != null) {
                    map.put(ChunkPos.toLong(cx, cz), chunk);
                }
            }
        }
        ItemStack[] hotbar = new ItemStack[9];
        boolean placeable = false;
        for (int i = 0; i < 9; i++) {
            hotbar[i] = player.getInventory().getStack(i).copy();
            if (isThrowaway(hotbar[i])) {
                placeable = true;
            }
        }
        if(player.getAbilities().flying) {
            canFly = true;
        }else{
            canFly = false;
        }
        return new Snapshot(map, hotbar, placeable);
    }
    public static boolean canFly(){
        return canFly;
    }
    public static boolean isThrowaway(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() instanceof BlockItem blockItem
                && Settings.throwaway.contains(blockItem.getBlock());
    }

    public WorldChunk chunk(int cx, int cz) {
        return chunks.get(ChunkPos.toLong(cx, cz));
    }

    public ItemStack[] hotbar() {
        return hotbar;
    }

    public boolean placeable() {
        return placeable;
    }
}
