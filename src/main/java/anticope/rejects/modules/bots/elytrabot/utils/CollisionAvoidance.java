package anticope.rejects.modules.bots.elytrabot.utils;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;

import static anticope.rejects.modules.bots.elytrabot.utils.MiscUtil.getBlock;
import static anticope.rejects.modules.bots.elytrabot.utils.MiscUtil.isSolid;
import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Handles collision detection and avoidance for ElytraBot
 */
public class CollisionAvoidance {
    // Range to look ahead for obstacles
    private static final int LOOK_AHEAD_DISTANCE = 20;
    // How far to deviate when avoiding obstacles
    private static final int AVOIDANCE_RADIUS = 3;
    // How many blocks to check in each direction
    private static final int SCAN_RADIUS = 5;

    /**
     * Checks if there are any solid blocks in the path ahead
     *
     * @param currentPos Current position
     * @param direction  Direction of travel
     * @return true if a collision is detected
     */
    public static boolean isCollisionAhead(BlockPos currentPos, Vec3d direction) {
        Vec3d normalizedDir = normalize(direction);

        for (int i = 1; i <= LOOK_AHEAD_DISTANCE; i++) {
            BlockPos checkPos = new BlockPos(
                (int) (currentPos.getX() + normalizedDir.x * i),
                (int) (currentPos.getY() + normalizedDir.y * i),
                (int) (currentPos.getZ() + normalizedDir.z * i)
            );

            if (isSolid(checkPos)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Normalizes a vector to a unit vector
     */
    private static Vec3d normalize(Vec3d vec) {
        double length = Math.sqrt(vec.x * vec.x + vec.y * vec.y + vec.z * vec.z);
        if (length < 0.0001) return new Vec3d(0, 0, 0);
        return new Vec3d(vec.x / length, vec.y / length, vec.z / length);
    }

    /**
     * Finds a safe path around detected obstacles
     *
     * @param currentPath Current path that needs modification
     * @param currentPos  Player's current position
     * @param goal        The destination
     * @return A modified path that avoids obstacles
     */
    public static ArrayList<BlockPos> avoidCollision(ArrayList<BlockPos> currentPath, BlockPos currentPos, BlockPos goal) {
        if (currentPath == null || currentPath.isEmpty()) {
            return currentPath;
        }

        // Check if we need to modify the path
        BlockPos nextWaypoint = currentPath.get(currentPath.size() - 1);
        Vec3d direction = new Vec3d(
            nextWaypoint.getX() - currentPos.getX(),
            nextWaypoint.getY() - currentPos.getY(),
            nextWaypoint.getZ() - currentPos.getZ()
        );

        if (!isCollisionAhead(currentPos, direction)) {
            return currentPath; // No collision detected, keep current path
        }

        // Find a clear path around the obstacle
        ArrayList<BlockPos> alternativePath = findClearPath(currentPos, goal);
        if (alternativePath != null && !alternativePath.isEmpty()) {
            // Replace the next few waypoints with the alternative path
            int replaceCount = Math.min(alternativePath.size(), 5); // Replace up to 5 waypoints

            if (currentPath.size() <= replaceCount) {
                return alternativePath;
            } else {
                ArrayList<BlockPos> newPath = new ArrayList<>(alternativePath);
                for (int i = replaceCount; i < currentPath.size(); i++) {
                    newPath.add(currentPath.get(i));
                }
                return newPath;
            }
        }

        return currentPath; // Fallback to current path
    }

    /**
     * Finds a clear path by scanning around the obstacle
     */
    private static ArrayList<BlockPos> findClearPath(BlockPos start, BlockPos goal) {
        Vec3d dirToGoal = new Vec3d(
            goal.getX() - start.getX(),
            goal.getY() - start.getY(),
            goal.getZ() - start.getZ()
        );
        Vec3d normalizedDir = normalize(dirToGoal);

        // Find the obstacle
        BlockPos obstaclePos = null;
        for (int i = 1; i <= LOOK_AHEAD_DISTANCE; i++) {
            BlockPos checkPos = new BlockPos(
                (int) (start.getX() + normalizedDir.x * i),
                (int) (start.getY() + normalizedDir.y * i),
                (int) (start.getZ() + normalizedDir.z * i)
            );

            if (isSolid(checkPos)) {
                obstaclePos = checkPos;
                break;
            }
        }

        if (obstaclePos == null) {
            return null; // No obstacle found
        }

        // Scan for clear paths around the obstacle
        ArrayList<BlockPos> bestPath = null;
        double bestScore = Double.MAX_VALUE;

        // Try different directions to go around the obstacle
        for (int y = -SCAN_RADIUS; y <= SCAN_RADIUS; y += 1) {
            for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x += 1) {
                for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z += 1) {
                    // Skip small deviations that won't help avoid obstacles
                    if (Math.abs(x) < 2 && Math.abs(y) < 2 && Math.abs(z) < 2) continue;

                    BlockPos detourPoint = new BlockPos(
                        obstaclePos.getX() + x,
                        obstaclePos.getY() + y,
                        obstaclePos.getZ() + z
                    );

                    // Check if detour point is clear
                    if (!isSolid(detourPoint) && isPathClear(start, detourPoint)) {
                        double score = start.getSquaredDistance(detourPoint)
                            + goal.getSquaredDistance(detourPoint);

                        if (score < bestScore) {
                            bestScore = score;

                            // Create the detour path
                            ArrayList<BlockPos> detourPath = new ArrayList<>();

                            // Add waypoints to safely navigate to the detour point
                            BlockPos halfway = new BlockPos(
                                (start.getX() + detourPoint.getX()) / 2,
                                (start.getY() + detourPoint.getY()) / 2,
                                (start.getZ() + detourPoint.getZ()) / 2
                            );

                            if (!isSolid(halfway)) {
                                detourPath.add(halfway);
                            }

                            detourPath.add(detourPoint);

                            // Add another point after the detour toward the goal
                            Vec3d afterDetour = new Vec3d(
                                detourPoint.getX() + normalizedDir.x * 5,
                                detourPoint.getY() + normalizedDir.y * 5,
                                detourPoint.getZ() + normalizedDir.z * 5
                            );

                            BlockPos afterDetourPos = new BlockPos((int) afterDetour.x, (int) afterDetour.y, (int) afterDetour.z);
                            if (!isSolid(afterDetourPos)) {
                                detourPath.add(afterDetourPos);
                            }

                            bestPath = detourPath;
                        }
                    }
                }
            }
        }

        return bestPath;
    }

    /**
     * Checks if there's a clear line of sight between two positions
     */
    private static boolean isPathClear(BlockPos from, BlockPos to) {
        Vec3d direction = new Vec3d(
            to.getX() - from.getX(),
            to.getY() - from.getY(),
            to.getZ() - from.getZ()
        );

        double distance = Math.sqrt(from.getSquaredDistance(to));
        Vec3d normalizedDir = normalize(direction);

        // Check several points along the path
        int steps = (int) Math.ceil(distance);
        for (int i = 1; i < steps; i++) {
            double t = i / (double) steps;
            BlockPos checkPos = new BlockPos(
                (int) (from.getX() + normalizedDir.x * distance * t),
                (int) (from.getY() + normalizedDir.y * distance * t),
                (int) (from.getZ() + normalizedDir.z * distance * t)
            );

            if (isSolid(checkPos)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if blocks ahead contain hazards like lava
     */
    public static boolean isHazardAhead(BlockPos currentPos, Vec3d direction, int lookAhead) {
        Vec3d normalizedDir = normalize(direction);

        for (int i = 1; i <= lookAhead; i++) {
            BlockPos checkPos = new BlockPos(
                (int) (currentPos.getX() + normalizedDir.x * i),
                (int) (currentPos.getY() + normalizedDir.y * i),
                (int) (currentPos.getZ() + normalizedDir.z * i)
            );

            Block block = getBlock(checkPos);
            if (block == Blocks.LAVA || block == Blocks.FIRE) {
                return true;
            }
        }

        return false;
    }

    /**
     * Detects potential collisions with the terrain based on player's current velocity
     * and position, returning true if a collision is imminent
     */
    public static boolean isImmediateCollisionLikely(int lookAhead) {
        if (mc.player == null) return false;

        Vec3d velocity = mc.player.getVelocity();
        BlockPos playerPos = mc.player.getBlockPos();

        return isCollisionAhead(playerPos, velocity) ||
            isHazardAhead(playerPos, velocity, lookAhead);
    }

    /**
     * Generate a path that avoids detected obstacles
     *
     * @param currentPath Current path
     * @param start       Starting position
     * @param goal        Goal position
     * @return An improved path with collision avoidance
     */
    public static ArrayList<BlockPos> generateAvoidancePath(ArrayList<BlockPos> currentPath, BlockPos start, BlockPos goal, BlockPos[] positions, ArrayList<BlockPos> checkPositions, int loopAmount) {
        // First check if the current path has collisions
        if (currentPath != null && !currentPath.isEmpty()) {
            ArrayList<BlockPos> avoidancePath = avoidCollision(currentPath, start, goal);
            if (avoidancePath != currentPath) {
                return avoidancePath;
            }
        }

        // If we need a new path, generate it with AStar
        return AStar.generatePath(start, goal, positions, checkPositions, loopAmount);
    }
}
