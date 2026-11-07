package greed.client.module.combat.crystalmodules.utils;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;


public final class CrystalDamage {
    private CrystalDamage() {}

    public static float getExplosionDamage(
            Vec3d source,
            PlayerEntity player,
            boolean optimized,
            boolean unused1,
            boolean unused2) {
        return estimate(source, player, null, false);
    }

    public static float getDamageOfGhostBlock(
            Vec3d source, PlayerEntity player, BlockPos ghost, boolean unused1, boolean unused2) {
        return estimate(source, player, ghost, true);
    }

    public static float getIgnoredDamage(Vec3d source, PlayerEntity player, BlockPos ignored) {
        return estimate(source, player, ignored, false);
    }

    private static float estimate(
            Vec3d source, PlayerEntity player, BlockPos special, boolean ghost) {
        return estimate(source, player, ghost ? special : null, ghost ? null : special, 12);
    }

    public static float getAnchorDamage(
            Vec3d source, PlayerEntity player, BlockPos anchor, BlockPos shield) {
        return estimate(source, player, shield, anchor, 10);
    }

    private static float estimate(
            Vec3d source,
            PlayerEntity player,
            BlockPos ghostBlock,
            BlockPos ignoredBlock,
            double diameter) {
        return estimate(source, player, ghostBlock, ignoredBlock, diameter, false, Vec3d.ZERO);
    }

    public static float sample(
            Vec3d source,
            PlayerEntity player,
            BlockPos ghost,
            BlockPos ignored,
            boolean anchor,
            boolean conservative,
            Vec3d offset) {
        return estimate(source, player, ghost, ignored, anchor ? 10 : 12, conservative, offset);
    }


    public static float sampleCleared(Vec3d source, PlayerEntity player, BlockPos ghost,
            BlockPos ignored, boolean anchor, boolean conservative, Vec3d offset,
            java.util.Set<BlockPos> removed) {
        return estimate(source, player, ghost, ignored, anchor ? 10 : 12, conservative, offset, removed);
    }

    private static float estimate(
            Vec3d source,
            PlayerEntity player,
            BlockPos ghostBlock,
            BlockPos ignoredBlock,
            double diameter,
            boolean conservative,
            Vec3d offset) {
        return estimate(
                source,
                player,
                ghostBlock,
                ignoredBlock,
                diameter,
                conservative,
                offset,
                java.util.Set.of());
    }

    private static float estimate(
            Vec3d source,
            PlayerEntity player,
            BlockPos ghostBlock,
            BlockPos ignoredBlock,
            double diameter,
            boolean conservative,
            Vec3d offset,
            java.util.Set<BlockPos> removed) {
        var world = MinecraftClient.getInstance().world;
        if (world == null || player.isCreative() || player.isSpectator()) return 0;
        double distance = player.getEntityPos().add(offset).distanceTo(source) / diameter;
        if (distance > 1) return 0;
        BlockView view =
                !conservative && ghostBlock == null && ignoredBlock == null && removed.isEmpty()
                        ? world
                        : new BlockView() {
                            @Override
                            public BlockEntity getBlockEntity(BlockPos pos) {
                                return world.getBlockEntity(pos);
                            }

                            @Override
                            public BlockState getBlockState(BlockPos pos) {
                                if (pos.equals(ignoredBlock) || removed.contains(pos))
                                    return Blocks.AIR.getDefaultState();
                                if (pos.equals(ghostBlock))
                                    return Blocks.OBSIDIAN.getDefaultState();
                                BlockState state = world.getBlockState(pos);
                                // Fragile cover may disappear in a nearby chained explosion.
                                if (conservative && state.getBlock().getBlastResistance() < 600)
                                    return Blocks.AIR.getDefaultState();
                                return state;
                            }

                            @Override
                            public FluidState getFluidState(BlockPos pos) {
                                return getBlockState(pos).getFluidState();
                            }

                            @Override
                            public int getHeight() {
                                return world.getHeight();
                            }

                            @Override
                            public int getBottomY() {
                                return world.getBottomY();
                            }
                        };
        Box box = player.getBoundingBox().offset(offset);
        double dx = 1 / ((box.maxX - box.minX) * 2 + 1);
        double dy = 1 / ((box.maxY - box.minY) * 2 + 1);
        double dz = 1 / ((box.maxZ - box.minZ) * 2 + 1);
        double offsetX = (1 - Math.floor(1 / dx) * dx) / 2;
        double offsetZ = (1 - Math.floor(1 / dz) * dz) / 2;
        int clear = 0, samples = 0;
        for (double x = 0; x <= 1; x += dx)
            for (double y = 0; y <= 1; y += dy)
                for (double z = 0; z <= 1; z += dz) {
                    Vec3d point =
                            new Vec3d(
                                    MathHelper.lerp(x, box.minX, box.maxX) + offsetX,
                                    MathHelper.lerp(y, box.minY, box.maxY),
                                    MathHelper.lerp(z, box.minZ, box.maxZ) + offsetZ);
                    if (view.raycast(
                                            new RaycastContext(
                                                    point,
                                                    source,
                                                    RaycastContext.ShapeType.COLLIDER,
                                                    RaycastContext.FluidHandling.NONE,
                                                    player))
                                    .getType()
                            == HitResult.Type.MISS) clear++;
                    samples++;
                }
        double impact = (1 - distance) * clear / Math.max(1.0, samples);
        float damage = (float) Math.floor((impact * impact + impact) / 2 * 7 * diameter + 1);
        damage =
                switch (world.getDifficulty()) {
                    case PEACEFUL -> 0;
                    case EASY -> Math.min(damage / 2 + 1, damage);
                    case HARD -> damage * 1.5f;
                    default -> damage;
                };
        float armor = player.getArmor();
        float toughness = (float) player.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS);
        float protection = MathHelper.clamp(armor - damage / (2 + toughness / 4), armor * .2f, 20);
        damage *= 1 - protection / 25;
        var resistance = player.getStatusEffect(StatusEffects.RESISTANCE);
        if (resistance != null) damage *= Math.max(0, 1 - (resistance.getAmplifier() + 1) * .2f);
        int enchantProtection = 0;
        for (EquipmentSlot slot :
                new EquipmentSlot[] {
                    EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
                }) {
            var enchants = player.getEquippedStack(slot).getEnchantments();
            for (var enchant : enchants.getEnchantments()) {
                int level = enchants.getLevel(enchant);
                if (enchant.matchesKey(Enchantments.PROTECTION)) enchantProtection += level;
                if (enchant.matchesKey(Enchantments.BLAST_PROTECTION))
                    enchantProtection += level * 2;
            }
        }
        return Math.max(0, damage * (1 - Math.min(20, enchantProtection) / 25f));
    }
}
