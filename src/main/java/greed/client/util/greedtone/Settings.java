package greed.client.util.greedtone;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.Set;

public final class Settings {

    public static boolean allowBreak = true;
    public static boolean allowPlace = true;
    public static boolean allowParkour = true;
    public static boolean allowWater = true;
    public static boolean autoEat = true;
    public static int eatBelow = 14;
    public static int maxFall = 3;
    public static int maxFallWater = 24;
    public static int maxNodes = 80000;
    public static long timeoutMs = 4000;
    public static int maxReplans = 60;
    public static float turnSpeed = 14.0f;
    public static float aimSpeed = 36.0f;
    public static float walkPitch = 4.0f;
    public static boolean allowFlying = true;

    public static final Set<Block> throwaway = Set.of(
            Blocks.COBBLESTONE,
            Blocks.DIRT,
            Blocks.NETHERRACK,
            Blocks.STONE,
            Blocks.COBBLED_DEEPSLATE,
            Blocks.DEEPSLATE,
            Blocks.ANDESITE,
            Blocks.DIORITE,
            Blocks.GRANITE,
            Blocks.TUFF
    );

    public static final Set<Item> badFood = Set.of(
            Items.ROTTEN_FLESH,
            Items.SPIDER_EYE,
            Items.PUFFERFISH,
            Items.POISONOUS_POTATO,
            Items.CHORUS_FRUIT,
            Items.SUSPICIOUS_STEW,
            Items.GOLDEN_APPLE,
            Items.ENCHANTED_GOLDEN_APPLE,
            Items.CHICKEN,
            Items.HONEY_BOTTLE
    );

    private Settings() {
    }
}
