package greed.client.module.combat.crystalmodules.utils.explosive;

import java.util.*;

import greed.client.event.AttackEvent;
import greed.client.event.EventBus;
import greed.client.event.InteractEntityEvent;
import greed.client.event.PostAttackEvent;
import greed.client.util.player.placement.InteractionGate;
import greed.client.util.player.placement.SilentSwitch;
import greed.mixin.ClientWorldAccessor;
import greed.mixin.InteractionManagerSlotAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.*;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.RaycastContext;

/** One confirmed-stage interaction; rotation is carried by normal movement packets. */
public final class ExplosivePackets {
    public record Command(Hand hand, int slot, BlockHitResult hit, BlockPos creates, Entity attack, boolean hidden, Vec3d aim, boolean expectClear) {
        public Command(Hand hand,int slot,BlockHitResult hit,BlockPos creates,Entity attack,boolean hidden) {
            this(hand,slot,hit,creates,attack,hidden,null,false);
        }
        public Command(Hand hand,int slot,BlockHitResult hit,BlockPos creates,Entity attack,boolean hidden,Vec3d aim) {
            this(hand,slot,hit,creates,attack,hidden,aim,false);
        }
        /** The cleared block is still solid on this client; the server sees it as air. */
        public Command clearing() {
            return new Command(hand,slot,hit,creates,attack,hidden,aim,true);
        }
    }
    private Object player, world;
    private final ExplosiveBurstBudget budget = new ExplosiveBurstBudget();
    private boolean busy;
    private final ExplosiveInventoryLedger inventory = new ExplosiveInventoryLedger();
    private final ItemStack[] snapshots = new ItemStack[10];
    private void context() {
        var mc=MinecraftClient.getInstance();
        if(player!=mc.player||world!=mc.world){player=mc.player;world=mc.world;budget.reset();inventory.clear();Arrays.fill(snapshots,null);}
    }
    public int availableCount(int slot,ItemStack stack) {
        context();
        if(snapshots[slot]==null||!ItemStack.areItemsAndComponentsEqual(snapshots[slot],stack))inventory.reset(slot);
        snapshots[slot]=stack.copy();
        return inventory.available(slot,stack.getCount(),MinecraftClient.getInstance().player.age);
    }
    public boolean isBusy() { return busy; }
    public boolean execute(List<Command> commands) {
        var mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.interactionManager == null || !mc.isOnThread()
                || mc.getNetworkHandler() == null || commands.isEmpty() || commands.size() > 1 || busy) return false;
        context();
        Set<BlockPos> future = new HashSet<>();
        Set<Entity> attacks = new HashSet<>();
        for (Command command : commands) {
            if (command.slot < 0 || command.slot > 8) return false;
            if (command.attack != null) {
                Entity entity = command.attack;
                if (!entity.isAlive() || entity.isRemoved() || !attacks.add(entity)) return false;
                var event = new InteractEntityEvent(entity); EventBus.getInstance().post(event);
                if (event.isCancelled()) return false;
                Vec3d eye = mc.player.getEyePos();
                if (!entity.getBoundingBox().expand(mc.player.getEntityInteractionRange()).contains(eye)
                        || eye.squaredDistanceTo(closest(entity.getBoundingBox(),eye))
                        > Math.pow(mc.player.getEntityInteractionRange(),2)) return false;
                Vec3d aim=command.aim==null?inside(entity.getBoundingBox(),eye):command.aim;
                Vec3d ray=aim.subtract(eye);
                Vec3d entry=entity.getBoundingBox().contains(eye)?eye:entity.getBoundingBox().raycast(eye,
                        eye.add(ray.normalize().multiply(mc.player.getEntityInteractionRange()))).orElse(null);
                if(entry==null)return false;
                if(!command.hidden){var hit=mc.world.raycast(new RaycastContext(eye,entry,
                        RaycastContext.ShapeType.OUTLINE,RaycastContext.FluidHandling.NONE,mc.player));
                    if(hit.getType()==HitResult.Type.BLOCK&&eye.squaredDistanceTo(hit.getPos())+.00001<eye.squaredDistanceTo(entry))return false;}
                continue;
            }
            var hit=command.hit;
            if (hit == null || hit.isInsideBlock() || !mc.world.isInBuildLimit(hit.getBlockPos())
                    || !mc.world.getWorldBorder().contains(hit.getBlockPos())) return false;
            Vec3d point=hit.getPos(), eye=mc.player.getEyePos(); BlockPos block=hit.getBlockPos();
            if (!Double.isFinite(point.x) || !Double.isFinite(point.y) || !Double.isFinite(point.z)
                    || point.x < block.getX() || point.x > block.getX()+1
                    || point.y < block.getY() || point.y > block.getY()+1
                    || point.z < block.getZ() || point.z > block.getZ()+1
                    || eye.squaredDistanceTo(point) > Math.pow(mc.player.getBlockInteractionRange(),2)) return false;
            var shape = future.contains(block) ? VoxelShapes.fullCube() : mc.world.getBlockState(block).getOutlineShape(mc.world,block);
            var entry=shape.raycast(eye,point.add(point.subtract(eye).normalize().multiply(.001)),block);
            if (entry == null || entry.isInsideBlock() || entry.getSide()!=hit.getSide()) return false;
            if (!command.hidden) {
                var obstruction=mc.world.raycast(new RaycastContext(eye,point,
                        RaycastContext.ShapeType.OUTLINE,RaycastContext.FluidHandling.NONE,mc.player));
                if (obstruction.getType()==HitResult.Type.BLOCK && !obstruction.getBlockPos().equals(block)
                        && (command.creates == null || !obstruction.getBlockPos().equals(command.creates))
                        && eye.squaredDistanceTo(obstruction.getPos())+.00001 < eye.squaredDistanceTo(point)) return false;
            }
            if (command.creates != null) {
                if (!block.offset(hit.getSide()).equals(command.creates)
                        || (!command.expectClear && !mc.world.getBlockState(command.creates).isReplaceable())) return false;
                future.add(command.creates);
            }
        }
        int original=mc.player.getInventory().getSelectedSlot();
        List<SilentSwitch.Use> uses=new ArrayList<>();
        for (Command command : commands) uses.add(new SilentSwitch.Use(command.slot, () -> {
            if (command.attack != null) {
                EventBus.getInstance().post (new AttackEvent(command.attack));
                mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(command.attack,mc.player.isSneaking()));
                mc.player.resetTicksSince();
            } else try (var updates=((ClientWorldAccessor)mc.world).greed$getPendingUpdateManager().incrementSequence()) {
                mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(command.hand,command.hit,updates.getSequence()));
            }
        }, () -> mc.player.swingHand(command.hand)));
        if (!budget.acquire(mc.player.age,commands.size())) return false;
        busy=true;
        try {
            SilentSwitch.executeBatch(original, InteractionGate.serverSlot(),uses,
                    slot -> mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot)),
                    slot -> ((InteractionManagerSlotAccessor)mc.interactionManager).rich$setLastSelectedSlot(slot));
            var entry=mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            int ttl=Math.clamp((entry==null?2:(entry.getLatency()+49)/50)+3,4,24);
            for(Command command:commands)if(!mc.player.isCreative()&&command.attack==null){
                int slot=command.hand==Hand.OFF_HAND?9:command.slot;
                ItemStack stack=slot==9?mc.player.getOffHandStack():mc.player.getInventory().getStack(slot);
                if(command.creates!=null||stack.isOf(Items.END_CRYSTAL)||stack.isOf(Items.GLOWSTONE))
                    inventory.reserve(slot,1,mc.player.age,ttl);
            }
            return true;
        } finally { busy=false; }
    }
    private static Vec3d closest(Box box,Vec3d eye) { return new Vec3d(Math.clamp(eye.x,box.minX,box.maxX),Math.clamp(eye.y,box.minY,box.maxY),Math.clamp(eye.z,box.minZ,box.maxZ)); }
    private static Vec3d inside(Box box,Vec3d eye) { return closest(box.contract(.03),eye); }
}
