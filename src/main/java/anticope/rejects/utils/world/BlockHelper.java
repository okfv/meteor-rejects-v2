package anticope.rejects.utils.world;

import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import net.minecraft.block.*;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.item.Item;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import static anticope.rejects.utils.world.DistanceHelper.distanceBetween;
import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.Utils.vec3d;

public class BlockHelper {

    public static List<BlockPos> getSphere(BlockPos centerPos, int radius, int height) {
        ArrayList<BlockPos> blocks = new ArrayList<>();
        for (int i = centerPos.getX() - radius; i < centerPos.getX() + radius; i++) {
            for (int j = centerPos.getY() - height; j < centerPos.getY() + height; j++) {
                for (int k = centerPos.getZ() - radius; k < centerPos.getZ() + radius; k++) {
                    BlockPos pos = new BlockPos(i, j, k);
                    if (distanceBetween(centerPos, pos) <= radius && !blocks.contains(pos)) blocks.add(pos);
                }
            }
        }
        return blocks;
    }

    public static Block getBlock(BlockPos p) {
        if (p == null) return null;
        return mc.world.getBlockState(p).getBlock();
    }

    public static BlockState getState(BlockPos pos) {
        if (pos == null) return null;
        return mc.world.getBlockState(pos);
    }

    public static boolean canPlace(BlockPos pos) {
        if (pos == null) return false;
        if (isSolid(pos) || !World.isValid(pos) || !canReplace(pos)) return false;
        if (!mc.world.canPlace(mc.world.getBlockState(pos), pos, ShapeContext.absent())) return false;
        return mc.world.getBlockState(pos).isAir() || mc.world.getBlockState(pos).getFluidState().getFluid() instanceof FlowableFluid;
    }

    public static boolean canPlant(BlockPos pos) {
        Block b = getBlock(pos);
        if (b.equals(Blocks.SHORT_GRASS) || b.equals(Blocks.GRASS_BLOCK) || b.equals(Blocks.DIRT) || b.equals(Blocks.COARSE_DIRT)) {
            final AtomicBoolean plant = new AtomicBoolean(true);
            IntStream.rangeClosed(1, 5).forEach(i -> {
                // Check above
                BlockPos check = pos.up(i);
                if (!isAirBlock(check)) {
                    plant.set(false);
                    return;
                }
                // Check around
                for (CardinalDirection dir : CardinalDirection.values()) {
                    if (!isAirBlock(check.offset(dir.toDirection(), i))) {
                        plant.set(false);
                        return;
                    }
                }
            });
            return plant.get();
        }
        return false;
    }


    public static boolean canReplace(BlockPos pos) {
        return getState(pos).isReplaceable();
    }

    public static boolean isSolid(BlockPos pos) {
        if (isAirBlock(pos) || isLiquidBlock(pos)) return false;
        return getState(pos).isSolidBlock(mc.world, pos);
    }

    public static boolean isAirBlock(BlockPos pos) {
        Block b = getBlock(pos);
        return b.equals(Blocks.AIR) || b.equals(Blocks.CAVE_AIR) || b.equals(Blocks.VOID_AIR);
    }

    public static boolean isLiquidBlock(BlockPos pos) {
        Block b = getBlock(pos);
        return b.equals(Blocks.WATER) || b.equals(Blocks.LAVA);
    }

    public static boolean isSaplingBlock(BlockPos pos) {
        return getBlock(pos) instanceof SaplingBlock;
    }

    public static boolean isLeafBlock(BlockPos pos) {
        Block b = getBlock(pos);
        return b.equals(Blocks.ACACIA_LEAVES) ||
            b.equals(Blocks.BIRCH_LEAVES) ||
            b.equals(Blocks.DARK_OAK_LEAVES) ||
            b.equals(Blocks.JUNGLE_LEAVES) ||
            b.equals(Blocks.OAK_LEAVES) ||
            b.equals(Blocks.SPRUCE_LEAVES) ||
            b.equals(Blocks.MANGROVE_LEAVES) ||
            b.equals(Blocks.CHERRY_LEAVES);
    }

    public static boolean isLogBlock(BlockPos pos) {
        Block b = getBlock(pos);
        return b.equals(Blocks.ACACIA_LOG) ||
            b.equals(Blocks.BIRCH_LOG) ||
            b.equals(Blocks.DARK_OAK_LOG) ||
            b.equals(Blocks.JUNGLE_LOG) ||
            b.equals(Blocks.OAK_LOG) ||
            b.equals(Blocks.SPRUCE_LOG) ||
            b.equals(Blocks.MANGROVE_LOG) ||
            b.equals(Blocks.CHERRY_LOG);
    }

    public static Vec3d getBestHitPos(BlockPos pos) {
        if (pos == null) return new Vec3d(0.0, 0.0, 0.0);
        double x = MathHelper.clamp((mc.player.getX() - pos.getX()), 0.0, 1.0);
        double y = MathHelper.clamp((mc.player.getY() - pos.getY()), 0.0, 0.6);
        double z = MathHelper.clamp((mc.player.getZ() - pos.getZ()), 0.0, 1.0);
        return new Vec3d(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
    }

    public static Vector3d getVector3d(BlockPos pos) {
        return new Vector3d(pos.getX(), pos.getY(), pos.getZ());
    }


    public static class BlockPosPlus {
        private BlockPos pos;

        public BlockPosPlus(BlockPos pos) {
            set(pos);
        }

        public BlockPos getBlockPos() {
            return pos;
        }

        public void set(BlockPos pos) {
            this.pos = pos;
        }

        public void offset(CardinalDirection d) {
            this.set(this.pos.offset(d.toDirection()));
        }

        public void offset(Direction d) {
            this.set(this.pos.offset(d));
        }


        public Block getBlock() {
            return BlockHelper.getBlock(this.pos);
        }

        public BlockState getState() {
            return BlockHelper.getState(this.pos);
        }


        public boolean isAir(BlockPos p) {
            return BlockHelper.isAirBlock(this.pos);
        }

        public boolean isSolid(BlockPos pos) {
            return BlockHelper.isSolid(this.pos);
        }

        public boolean isReplacable(BlockPos pos) {
            return BlockHelper.canReplace(this.pos);
        }


        public Vec3d getHitPos() {
            return getBestHitPos(this.pos);
        }

        public BlockHitResult getHitResult() {
            return new BlockHitResult(this.getHitPos(), Direction.UP, this.pos, false);
        }


        public double getYaw() {
            return Rotations.getYaw(this.pos);
        }

        public double getPitch() {
            return Rotations.getPitch(this.pos);
        }


        public double distanceToSelf() {
            return DistanceHelper.distanceBetween(mc.player.getBlockPos(), pos);
        }

        public double distanceTo(BlockPos pos) {
            return DistanceHelper.distanceBetween(pos, this.pos);
        }


        // Placement Helpers
        public boolean canPlace(boolean checkSupport) {
            if (!BlockHelper.canPlace(this.pos)) return false;
            return !checkSupport || this.supportBelow();
        }

        public boolean supportBelow() {
            return isSolid(this.pos.down());
        }


        public Item asItem() {
            return this.getBlock().asItem();
        }

        public Vec3d asVec3d() {
            return vec3d(this.pos);
        }

    }


}
