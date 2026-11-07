package greed.client.module.misc;

import greed.client.module.Module;
import greed.client.util.ClientChat;
import greed.client.util.greedtone.Goal;
import greed.client.util.greedtone.PathExecutor;
import greed.client.util.greedtone.Rotation;
import greed.client.util.greedtone.Settings;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public abstract class PveModule extends Module {

    protected boolean getItemFromHotBar(ItemStack targetStack) {
        if (mc.player == null) return false;

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stackInSlot = mc.player.getInventory().getStack(slot);

            if (!stackInSlot.isEmpty() && stackInSlot.getItem() == targetStack.getItem()) {
                mc.player.getInventory().setSelectedSlot(slot);
                return true;
            }
        }
        return false;
    }
//    protected boolean hasItemInHotbar(ItemStack targetStack) {
//        if (mc.player == null) return false;
//        for (int slot = 0; slot < 9; slot++) {
//            ItemStack stackInSlot = mc.player.getInventory().getStack(slot);
//            if(stackInSlot.getItem() == targetStack.getItem()) {
//                return true;
//            }
//        }
//        return false;
//    }
    protected void goTo(int x, int y, int z) {
        goTo(new BlockPos(x, y, z));
    }

    protected void goTo(BlockPos pos) {
        PathExecutor.goTo(new Goal.AtBlock(pos));
    }

    protected void goToXZ(int x, int z) {
        PathExecutor.goTo(new Goal.AtXZ(x, z));
    }

    protected void stopGoTo() {
        PathExecutor.stop();
    }

    protected boolean isGoingTo() {
        return PathExecutor.isActive();
    }

    protected boolean lookAt(double x, double y, double z) {
        return lookAt(new Vec3d(x, y, z), true);
    }

    protected boolean lookAt(BlockPos pos) {
        return lookAt(Vec3d.ofCenter(pos), true);
    }

    protected boolean lookAt(Vec3d target) {
        return lookAt(target, true);
    }

    protected boolean lookAt(Vec3d target, boolean precise) {
        ClientPlayerEntity p = mc.player;
        if (p == null) return false;

        Vec3d eye = p.getEyePos();
        double dx = target.x - eye.x;
        double dy = target.y - eye.y;
        double dz = target.z - eye.z;

        float yaw = Math.hypot(dx, dz) > 0.1 ? Rotation.yawTo(dx, dz) : p.getYaw();
        float pitch = Rotation.pitchTo(dx, dy, dz);

        return Rotation.look(p, yaw, pitch, Settings.aimSpeed, precise);
    }
    protected void sendMessage(String message) {
        ClientChat.info(Text.of(message));
    }
}