package anticope.rejects.modules.bots.elytrabot.utils;

import anticope.rejects.modules.bots.elytrabot.AutoLand;
import anticope.rejects.utils.world.BlockHelper;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.NoFall;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ElytraBotHelper {


    public static BlockPos findTakeoffPos() {
        List<BlockPos> nearby = BlockHelper.getSphere(mc.player.getBlockPos(), 15, 15);
        for (BlockPos pos : nearby) {
            // open space
            if (!BlockHelper.isAirBlock(pos)) continue;
            // open space above
            if (!BlockHelper.isAirBlock(pos.up())) continue;
            return pos;
        }
        return null;
    }

    public static NoFall getNoFall() {
        return Modules.get().get(NoFall.class);
    }

    public static boolean checkNoFall() {
        NoFall noFall = getNoFall();
        if (noFall.isActive()) {
            noFall.toggle();
            return true;
        }
        return false;
    }

    public static void useAutoLand() {
        AutoLand autoLand = Modules.get().get(AutoLand.class);
        if (!autoLand.isActive()) autoLand.toggle();
    }

    public static BlockPos getStartPos(double jumpY, boolean overworld) {
        // todo maybe something for maintainY here?
        BlockPos playerPos = mc.player.getBlockPos();
        if (overworld) return playerPos.add(0, 4, 0);
        else if (Math.abs(jumpY - mc.player.getY()) <= 2) return playerPos.offset(Direction.UP, (int) (jumpY + 1));
        else return playerPos.add(0, 1, 0);
    }

    public static BlockPos getLookingAheadPos(int checkDistance) {
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVec(1.0f);

        Vec3d targetPos = eyePos.add(
            lookVec.x * checkDistance,
            lookVec.y * checkDistance,
            lookVec.z * checkDistance
        );

        return new BlockPos(
            (int) Math.floor(targetPos.x),
            (int) Math.floor(targetPos.y),
            (int) Math.floor(targetPos.z)
        );
    }

}
