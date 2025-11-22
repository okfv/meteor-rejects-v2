package anticope.rejects.modules.bots.elytrabot;

import anticope.rejects.MeteorRejectsAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class AutoLand extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAdvanced = settings.createGroup("Advanced");

    // General settings
    private final Setting<Boolean> autoActivate = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-activate")
        .description("Automatically activates when ground is detected within landing distance.")
        .defaultValue(false)
        .build());

    private final Setting<Integer> landingDistance = sgGeneral.add(new IntSetting.Builder()
        .name("landing-distance")
        .description("Distance to ground at which to start landing (in blocks).")
        .defaultValue(30)
        .min(5)
        .sliderMax(100)
        .visible(autoActivate::get)
        .build());

    private final Setting<LandingMode> landingMode = sgGeneral.add(new EnumSetting.Builder<LandingMode>()
        .name("landing-mode")
        .description("How the landing should be performed.")
        .defaultValue(LandingMode.Gradual)
        .build());

    private final Setting<Integer> landingPitch = sgGeneral.add(new IntSetting.Builder()
        .name("landing-pitch")
        .description("Pitch angle to maintain during landing (higher = steeper).")
        .defaultValue(10)
        .range(0, 90)
        .sliderMax(90)
        .visible(() -> landingMode.get() == LandingMode.Fixed)
        .build());

    private final Setting<Boolean> stopMovement = sgGeneral.add(new BoolSetting.Builder()
        .name("stop-movement")
        .description("Completely stops all horizontal movement when landing.")
        .defaultValue(false)
        .build());

    // Advanced settings
    private final Setting<Boolean> safetyCheck = sgAdvanced.add(new BoolSetting.Builder()
        .name("safety-check")
        .description("Checks for safe landing spot before beginning descent.")
        .defaultValue(true)
        .build());

    private final Setting<Double> minSpeed = sgAdvanced.add(new DoubleSetting.Builder()
        .name("minimum-speed")
        .description("Minimum speed to maintain during landing (to prevent stalling).")
        .defaultValue(0.2)
        .min(0.05)
        .sliderMax(1.0)
        .build());

    private final Setting<Boolean> debug = sgAdvanced.add(new BoolSetting.Builder()
        .name("debug")
        .description("Logs debug information in chat.")
        .defaultValue(false)
        .build());

    private boolean isLanding = false;
    private double distanceToGround = 0;
    private int tickCounter = 0;

    public AutoLand() {
        super(MeteorRejectsAddon.CATEGORY, "auto-land", "Automatically helps you land safely when flying with elytra");
    }

    @Override
    public void onActivate() {
        isLanding = false;
        if (debug.get()) info("ElytraLand activated");

        if (!isWearingElytra()) {
            if (debug.get()) error("Not wearing elytra, deactivating");
            toggle();
            return;
        }

        if (!mc.player.isGliding()) {
            if (debug.get()) error("Not currently flying with elytra, deactivating");
            toggle();
            return;
        }

        isLanding = true;
        if (debug.get()) info("Beginning landing sequence");
    }

    @Override
    public void onDeactivate() {
        isLanding = false;
        if (debug.get()) info("ElytraLand deactivated");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        tickCounter++;

        // Only run every other tick to reduce performance impact
        if (tickCounter % 2 != 0) return;

        // Auto-activate check
        if (!isLanding && autoActivate.get() && isWearingElytra() && mc.player.isGliding()) {
            distanceToGround = getDistanceToGround();
            if (distanceToGround <= landingDistance.get() && isValidLandingSpot()) {
                isLanding = true;
                if (debug.get()) info("Auto-activated landing at height: " + String.format("%.1f", distanceToGround));
            }
        }

        // Main landing logic
        if (isLanding) {
            // Check if we're still flying
            if (!isWearingElytra() || !mc.player.isGliding()) {
                if (debug.get()) info("Landing complete or interrupted");
                toggle();
                return;
            }

            distanceToGround = getDistanceToGround();
            if (debug.get() && tickCounter % 20 == 0) {
                info("Distance to ground: " + String.format("%.1f", distanceToGround) + " blocks");
            }

            // Execute landing based on selected mode
            executeLanding();
        }
    }

    private void executeLanding() {
        // Calculate speed
        Vec3d velocity = mc.player.getVelocity();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

        // Determine target pitch
        float targetPitch;

        if (landingMode.get() == LandingMode.Fixed) {
            targetPitch = landingPitch.get();
        } else {
            // Gradual mode - adjust pitch based on height
            if (distanceToGround > 20) {
                targetPitch = 0; // Level flight when high
            } else if (distanceToGround > 10) {
                targetPitch = 5; // Slight descent
            } else if (distanceToGround > 5) {
                targetPitch = 15; // Moderate descent
            } else {
                targetPitch = 30; // Steeper descent when close to ground
            }
        }

        // Apply rotation
        Rotations.rotate(mc.player.getYaw(), targetPitch);

        // Stop movement if needed
        if (stopMovement.get()) {
            // Cancel horizontal movement but maintain minimal forward momentum to prevent stalling
            if (horizontalSpeed > minSpeed.get()) {
                double slowDownFactor = minSpeed.get() / horizontalSpeed;
                Vec3d newVelocity = new Vec3d(
                    velocity.x * slowDownFactor,
                    velocity.y,
                    velocity.z * slowDownFactor
                );
                mc.player.setVelocity(newVelocity);
                if (debug.get() && tickCounter % 20 == 0) {
                    info("Reducing speed: " + String.format("%.2f", horizontalSpeed) + " → " +
                        String.format("%.2f", horizontalSpeed * slowDownFactor));
                }
            }
        }

        // If very close to ground, prepare for impact
        if (distanceToGround < 2) {
            // Level out to avoid crash landing
            Rotations.rotate(mc.player.getYaw(), 0);
        }
    }

    private double getDistanceToGround() {
        if (mc.world == null || mc.player == null) return 0;

        double maxDistance = 100; // Maximum distance to check
        Vec3d start = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        Vec3d end = start.add(0, -maxDistance, 0);

        // Raycast to find ground
        BlockHitResult result = mc.world.raycast(new RaycastContext(
            start,
            end,
            RaycastContext.ShapeType.OUTLINE,
            RaycastContext.FluidHandling.ANY,
            mc.player
        ));

        if (result != null && result.getType() == BlockHitResult.Type.BLOCK) {
            return Math.sqrt(mc.player.squaredDistanceTo(result.getPos()));
        }

        return maxDistance;
    }

    private boolean isValidLandingSpot() {
        if (!safetyCheck.get()) return true;

        Vec3d playerPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        Vec3d lookDir = mc.player.getRotationVector();

        // Check ground ahead of player
        for (int i = 2; i < 10; i++) {
            Vec3d checkPos = playerPos.add(lookDir.multiply(i));
            BlockPos blockPos = new BlockPos((int) checkPos.x, (int) checkPos.y - 1, (int) checkPos.z);

            if (mc.world.getBlockState(blockPos).isAir()) {
                // Found a gap in potential landing area
                if (debug.get()) info("Unsafe landing spot detected ahead");
                return false;
            }
        }

        return true;
    }

    private boolean isWearingElytra() {
        return mc.player != null &&
            mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA;
    }

    public enum LandingMode {
        Gradual, // Automatically adjusts pitch based on height
        Fixed    // Uses a fixed pitch angle for landing
    }
}
