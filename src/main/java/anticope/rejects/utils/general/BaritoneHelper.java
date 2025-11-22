package anticope.rejects.utils.general;


import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import meteordevelopment.meteorclient.pathing.BaritonePathManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class BaritoneHelper {

    private static final IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
    // meteor's baritone path manager
    private static final BaritonePathManager pathManager = new BaritonePathManager();

    public static void pathToBlockPos(BlockPos pos) {
        baritone.getCustomGoalProcess().setGoalAndPath(new GoalBlock(pos));
    }

    public static void pause() {
        pathManager.pause();
    }

    public static void resume() {
        pathManager.resume();
    }

    public static void stopEverything() {
        stopPathing();
        baritone.getMineProcess().cancel(); // not sure if this is needed too or calling the rest, we'll see.
    }

    public static void stopPathing() {
        baritone.getPathingBehavior().cancelEverything();
        baritone.getPathingBehavior().forceCancel();
    }

    public static void stopFollowing() {
        baritone.getFollowProcess().cancel();
    }


    public static void followEntity(Entity e) {

    }

    public static boolean isFollowing() {
        return baritone.getFollowProcess().isActive();
    }

    public static boolean isFarming() {
        return baritone.getFarmProcess().isActive();
    }

    public static boolean hasGoal() {
        return baritone.getPathingBehavior().getGoal() != null;
    }

    public static Goal getGoal() {
        return baritone.getPathingBehavior().getGoal();
    }

    public static boolean isAtGoal() {
        try {
            return getGoal().isInGoal(mc.player.getBlockPos());
        } catch (Exception ignored) {
            return false;
        }

    }

    public static boolean isPathing() {
        return baritone.getPathingBehavior().isPathing();
    }

    public static boolean hasPath() {
        return baritone.getPathingBehavior().hasPath();
    }
}
