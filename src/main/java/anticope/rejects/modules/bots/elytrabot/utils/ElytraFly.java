package anticope.rejects.modules.bots.elytrabot.utils;

import anticope.rejects.modules.bots.elytrabot.ElytraBotThreaded;
import anticope.rejects.utils.network.NetworkHelper;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.world.TickRate;
import net.minecraft.util.math.BlockPos;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ElytraFly {
    public static boolean toggled;

    public static void toggle(boolean on) {
        toggled = on;
    }

    public static void setMotion(BlockPos pos, BlockPos next, BlockPos previous) {
        if (!toggled) return;

        ElytraBotThreaded elytraBot = Modules.get().get(ElytraBotThreaded.class);
        double x = 0, y = 0, z = 0;

        // Apply ping compensation if enabled
        double pingCompensationFactor = 1.0;
        if (elytraBot.pingCompensation.get()) {
            int currentPing = NetworkHelper.getCurrentPing();
            if (currentPing > elytraBot.pingCompensationThreshold.get() && currentPing != -1) {
                // Calculate a factor between 0.5 and 1.0 based on ping
                // Higher ping = lower factor = slower speed
                pingCompensationFactor = Math.max(0.5, 1.0 - ((currentPing - elytraBot.pingCompensationThreshold.get()) / 1000.0));
            }
        }

        // Apply TPS synchronization if enabled
        double tpsCompensationFactor = 1.0;
        if (elytraBot.tpsSync.get()) {
            float currentTps = TickRate.INSTANCE.getTickRate();
            // Only apply if TPS is below normal (20)
            if (currentTps < 20.0f && currentTps > 0.0f) {
                // Calculate ratio of current TPS to normal TPS (20)
                tpsCompensationFactor = currentTps / 20.0f;
                // Limit the minimum factor to 0.5 to prevent extremely slow speeds
                tpsCompensationFactor = Math.max(0.5, tpsCompensationFactor);
            }
        }

        // Combine the compensation factors
        double compensationFactor = pingCompensationFactor * tpsCompensationFactor;

        // Check maintainY setting and adjust target Y if needed
        double targetY = pos.getY() + 0.4;
        if (elytraBot.maintainY.get() && mc.player.getY() < elytraBot.minYLevel.get()) {
            // Prioritize climbing to minimum Y level
            targetY = elytraBot.minYLevel.get();
        }

        double xDiff = (pos.getX() + 0.5) - mc.player.getX();
        double yDiff = targetY - mc.player.getY();
        double zDiff = (pos.getZ() + 0.5) - mc.player.getZ();

        double speed = elytraBot.flySpeed.get() * compensationFactor;

        int amount = 0;
        try {
            if (Math.abs(next.getX() - previous.getX()) > 0) amount++;
            if (Math.abs(next.getY() - previous.getY()) > 0) amount++;
            if (Math.abs(next.getZ() - previous.getZ()) > 0) amount++;
            if (amount > 1) {
                speed = elytraBot.maneuverSpeed.get() * compensationFactor;

                //If the previous and next is both diagonal then use real speed
                if (next.getX() - previous.getX() == next.getZ() - previous.getZ() && next.getY() - previous.getY() == 0) {
                    if (xDiff >= 1 && zDiff >= 1 || xDiff <= -1 && zDiff <= -1) {
                        speed = elytraBot.flySpeed.get() * compensationFactor;
                    }
                }
            }
        } catch (Exception nullPointerProbablyIdk) {
            speed = elytraBot.maneuverSpeed.get() * compensationFactor;
        }

        // Rest of the method remains the same, but using compensationFactor for all speed calculations

        if ((int) xDiff > 0) {
            x = speed;
        } else if ((int) xDiff < 0) {
            x = -speed;
        }

        // Modify Y velocity calculation to account for maintainY setting
        if ((int) yDiff > 0) {
            // If we need to gain altitude, especially if below minYLevel, boost upward speed
            y = elytraBot.maintainY.get() && mc.player.getY() < elytraBot.minYLevel.get()
                ? Math.max((elytraBot.maneuverSpeed.get() * compensationFactor) * 1.5, 0.8) // Boost upward speed
                : elytraBot.maneuverSpeed.get() * compensationFactor;
        } else if ((int) yDiff < 0) {
            // Only allow downward velocity if we're above minYLevel or maintainY is disabled
            y = (!elytraBot.maintainY.get() || mc.player.getY() > elytraBot.minYLevel.get())
                ? -elytraBot.maneuverSpeed.get() * compensationFactor
                : 0; // Prevent going below minYLevel
        }

        if ((int) zDiff > 0) {
            z = speed;
        } else if ((int) zDiff < 0) {
            z = -speed;
        }

        mc.player.setVelocity(x, y, z);

        double centerSpeed = 0.2 * compensationFactor;
        double centerCheck = 0.1;

        // Add a special case for Y velocity when near minYLevel to create a "floor" effect
        double finalYVelocity;
        if (elytraBot.maintainY.get() && mc.player.getY() < elytraBot.minYLevel.get() + 1) {
            // Force upward movement when below or near minYLevel
            finalYVelocity = y == 0 ? Math.max(0.1, yDiff > centerCheck ? centerSpeed : 0) : y;
        } else {
            finalYVelocity = y == 0 ? yDiff > centerCheck ? centerSpeed : yDiff < -centerCheck ? -centerSpeed : 0 : y;
        }

        mc.player.setVelocity(
            (x == 0 ? xDiff > centerCheck ? centerSpeed : xDiff < -centerCheck ? -centerSpeed : 0 : mc.player.getVelocity().x),
            finalYVelocity,
            (z == 0 ? zDiff > centerCheck ? centerSpeed : zDiff < -centerCheck ? -centerSpeed : 0 : mc.player.getVelocity().z)
        );
    }
}
