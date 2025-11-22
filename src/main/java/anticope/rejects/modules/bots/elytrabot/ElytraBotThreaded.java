package anticope.rejects.modules.bots.elytrabot;

import anticope.rejects.MeteorRejectsAddon;
import anticope.rejects.modules.bots.elytrabot.utils.*;
import anticope.rejects.utils.general.BaritoneHelper;
import anticope.rejects.utils.player.ArmorHelper;
import anticope.rejects.utils.world.DistanceHelper;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.NoFall;
import meteordevelopment.meteorclient.systems.modules.player.AutoEat;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

public class ElytraBotThreaded extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final SettingGroup sgRotation = settings.createGroup("Rotation");

    private final SettingGroup sgPathfinding = settings.createGroup("Pathfinding");
    private final SettingGroup sgConnection = settings.createGroup("Connection");

    private final SettingGroup sgAvoidance = settings.createGroup("CollisionAvoidance");

    public final SettingGroup sgElytraFly = settings.createGroup("ElytraFly");

    private final SettingGroup sgCoordinates = settings.createGroup("Coordinates");

    private final SettingGroup sgInventory = settings.createGroup("Inventory");

    private final SettingGroup sgSafety = settings.createGroup("Safety");

    private final SettingGroup sgAutoEat = settings.createGroup("AutoEat");

    private final SettingGroup sgBaritone = settings.createGroup("Baritone");

    private final SettingGroup sgRender = settings.createGroup("Rendering");


    public final Setting<PathMode> botMode = sgGeneral.add(new EnumSetting.Builder<PathMode>()
        .name("travel-mode")
        .description("What mode of travel to use")
        .defaultValue(PathMode.Highway)
        .build()
    );

    private final Setting<TakeoffMode> takeoffMode = sgGeneral.add(new EnumSetting.Builder<TakeoffMode>()
        .name("takeoff-mode")
        .description("What mode to use for taking off.")
        .defaultValue(TakeoffMode.SlowGlide)
        .build()
    );

    // Baritone
    private final Setting<Boolean> useBaritone = sgBaritone.add(new BoolSetting.Builder()
        .name("use-baritone")
        .description("Whether or not to use baritone to walk a bit if stuck or a path cannot be found.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> walkDistance = sgBaritone.add(new IntSetting.Builder()
        .name("baritone-walk-distance")
        .description("How far to walk with baritone.")
        .defaultValue(20)
        .sliderMax(30)
        .visible(useBaritone::get)
        .build()
    );


    // Pathfinding

    public final Setting<Boolean> avoidLava = sgPathfinding.add(new BoolSetting.Builder()
        .name("avoid-lava")
        .description("Whether or not the pathfinding will avoid lava.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Integer> maxY = sgPathfinding.add(new IntSetting.Builder()
        .name("max-y")
        .description("The maximum Y level the pathfinding can go to. Set to -1 to disable.")
        .defaultValue(-1)
        .sliderMax(512)
        .build()
    );

    public final Setting<Boolean> maintainY = sgPathfinding.add(new BoolSetting.Builder()
        .name("maintain-y")
        .description("Keep you at or above the set Y level")
        .defaultValue(false)
        .build()
    );

    public final Setting<Integer> minYLevel = sgPathfinding.add(new IntSetting.Builder()
        .name("min-y-level")
        .description("The minimum Y level to maintain")
        .defaultValue(120)
        .min(60)
        .sliderMax(256)
        .visible(maintainY::get)
        .build()
    );

    // Connection
    public final Setting<Boolean> pingCompensation = sgConnection.add(new BoolSetting.Builder()
        .name("ping-compensation")
        .description("Reduce speeds if you encounter higher ping")
        .defaultValue(false)
        .build()
    );

    public final Setting<Integer> pingCompensationThreshold = sgConnection.add(new IntSetting.Builder()
        .name("ping-compensation-threshold")
        .description("Enable ping compensation when your ping is at or above this level")
        .defaultValue(100)
        .min(50)
        .sliderMax(1000)
        .visible(pingCompensation::get)
        .build()
    );

    // Add this to the sgConnection setting group in ElytraBotThreaded class
    public final Setting<Boolean> tpsSync = sgConnection.add(new BoolSetting.Builder()
        .name("tps-sync")
        .description("Synchronize flight speed with server TPS")
        .defaultValue(false)
        .build()
    );

    // Collision Avoidance
    private final Setting<Boolean> useCollisionAvoidance = sgAvoidance.add(new BoolSetting.Builder()
        .name("collision-avoidance")
        .description("Automatically avoids obstacles during flight.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> lookAheadDistance = sgAvoidance.add(new IntSetting.Builder()
        .name("look-ahead-distance")
        .description("How far ahead to check for obstacles.")
        .defaultValue(20)
        .min(5)
        .sliderMax(50)
        .visible(useCollisionAvoidance::get)
        .build()
    );

    // Inventory
    private final Setting<Boolean> autoSwitch = sgInventory.add(new BoolSetting.Builder()
        .name("auto-switch")
        .description("Switches equipped low durability elytra with a new one.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> restoreChest = sgInventory.add(new BoolSetting.Builder()
        .name("restore-chestplate")
        .description("Switch back to a chestplate after disabling.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Integer> switchDurability = sgInventory.add(new IntSetting.Builder()
        .name("switch-durability")
        .description("The durability threshold your elytra will be replaced at.")
        .defaultValue(2)
        .min(1)
        .max(Items.ELYTRA.getDefaultStack().getMaxDamage() - 1)
        .sliderMax(20)
        .visible(autoSwitch::get)
        .build()
    );


    // Safety
    private final Setting<Boolean> toggleOnPop = sgSafety.add(new BoolSetting.Builder()
        .name("toggle-on-pop")
        .description("Whether to toggle the module if you pop a totem or not.")
        .defaultValue(true)
        .build()
    );


    private final Setting<Boolean> useAutoLand = sgSafety.add(new BoolSetting.Builder()
        .name("use-auto-land")
        .description("Enable auto land once the goal is reached")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder()
        .name("debug")
        .description("Sends debug messages.")
        .defaultValue(false)
        .build()
    );

    // Rotation
    private final Setting<Boolean> rotate = sgRotation.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Whether to rotate to face the direction you're flying.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> rotateCooldown = sgRotation.add(new IntSetting.Builder()
        .name("rotate-cooldown")
        .description("Delay in ms between rotations")
        .defaultValue(50)
        .sliderMin(10)
        .sliderMax(1000)
        .visible(rotate::get)
        .build()
    );

    private final Setting<Double> rotationThreshold = sgRotation.add(new DoubleSetting.Builder()
        .name("rotation-threshold")
        .description("The minimum angle difference (in degrees) required to trigger rotation")
        .defaultValue(15.0)
        .min(1.0)
        .max(90.0)
        .sliderMax(90)
        .visible(rotate::get)
        .build()
    );

    // Elytra Fly
    private final Setting<FlyMode> flyMode = sgElytraFly.add(new EnumSetting.Builder<FlyMode>()
        .name("fly-mode")
        .description("What mode to use for flying.")
        .defaultValue(FlyMode.Control)
        .onChanged(this::onModeChange)
        .build()
    );

    public final Setting<Double> flySpeed = sgElytraFly.add(new DoubleSetting.Builder()
        .name("fly-speed")
        .description("The speed for control flight.")
        .defaultValue(1.81)
        .sliderMax(5)
        .visible(() -> flyMode.get() == FlyMode.Control)
        .build()
    );

    public final Setting<Double> maneuverSpeed = sgElytraFly.add(new DoubleSetting.Builder()
        .name("maneuver-speed")
        .description("The speed used for maneuvering.")
        .defaultValue(1)
        .sliderMax(3)
        .visible(() -> flyMode.get() == FlyMode.Control)
        .build()
    );

    private final Setting<Double> fireworkDelay = sgElytraFly.add(new DoubleSetting.Builder()
        .name("firework-delay")
        .description("The delay between using fireworks in seconds.")
        .defaultValue(2.0)
        .visible(() -> flyMode.get() == FlyMode.Firework)
        .build()
    );

    // Coordinates
    private final Setting<Boolean> useCoordinates = sgCoordinates.add(new BoolSetting.Builder()
        .name("use-coordinates")
        .description("If true, uses the given coordinates. If not, starts flying in the direction you are facing.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> dynamicPathUpdate = sgCoordinates.add(new BoolSetting.Builder()
        .name("dynamic-path-update")
        .description("Updates the path more frequently when traveling to further distances.")
        .defaultValue(true)
        .visible(useCoordinates::get)
        .build()
    );

    private final Setting<Integer> pathUpdateFrequency = sgCoordinates.add(new IntSetting.Builder()
        .name("path-update-frequency")
        .description("How often to update the path in ticks.")
        .defaultValue(100)
        .min(20)
        .sliderMax(200)
        .visible(dynamicPathUpdate::get)
        .build()
    );

    private final Setting<Integer> gotoX = sgCoordinates.add(new IntSetting.Builder()
        .name("goto-x")
        .description("The x coordinate the bot will try to go to.")
        .defaultValue(0)
        .sliderMin(-100000)
        .sliderMax(100000)
        .min(-30000000)
        .max(30000000)
        .visible(useCoordinates::get)
        .build()
    );

    private final Setting<Integer> gotoZ = sgCoordinates.add(new IntSetting.Builder()
        .name("goto-z")
        .description("The z coordinate the bot will try to go to.")
        .defaultValue(0)
        .sliderMin(-100000)
        .sliderMax(100000)
        .min(-30000000)
        .max(30000000)
        .visible(useCoordinates::get)
        .build()
    );

    // Auto Eat
    private final Setting<Boolean> autoEat = sgAutoEat.add(new BoolSetting.Builder()
        .name("auto-eat")
        .description("Automatically eats gaps or other food when health or hunger is low.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> minHeatlh = sgAutoEat.add(new IntSetting.Builder()
        .name("min-health")
        .description("The health value at which the bot will eat food.")
        .defaultValue(10)
        .sliderMin(1)
        .sliderMax(19)
        .visible(autoEat::get)
        .build()
    );

    private final Setting<Integer> minHunger = sgAutoEat.add(new IntSetting.Builder()
        .name("min-hunger")
        .description("The health hunger at which the bot will eat food.")
        .defaultValue(10)
        .sliderMin(1)
        .sliderMax(19)
        .visible(autoEat::get)
        .build()
    );

    public final Setting<Boolean> allowGaps = sgAutoEat.add(new BoolSetting.Builder()
        .name("allow-gaps")
        .description("Whether or not the bot is allowed to eat gapples.")
        .defaultValue(true)
        .visible(autoEat::get)
        .build()
    );

    // Render
    public final Setting<Boolean> renderPath = sgRender.add(new BoolSetting.Builder()
        .name("render-path")
        .description("Whether or not the path should be rendered.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> pathColour = sgRender.add(new ColorSetting.Builder()
        .name("path-color")
        .description("The path's color.")
        .defaultValue(new SettingColor(255, 0, 0, 150))
        .visible(renderPath::get)
        .build()
    );


    public final Setting<ChunkWaitMode> chunkWaitMode = sgGeneral.add(new EnumSetting.Builder<ChunkWaitMode>()
        .name("chunk-wait-mode")
        .description("What to do when waiting for chunks to load")
        .defaultValue(ChunkWaitMode.SlowFall)
        .build()
    );

    private final Setting<Integer> chunkLookAheadDistance = sgGeneral.add(new IntSetting.Builder()
        .name("chunk-look-ahead-distance")
        .description("How far ahead to check for unloaded chunks")
        .defaultValue(20)
        .min(5)
        .sliderMax(100)
        .build()
    );

    public BlockPos goal;

    private ArrayList<BlockPos> path;
    private Thread thread;
    private BlockPos last, previous, lastSecondPos;
    private DirectionUtil direction;


    // Booleans
    private boolean lagback, toggledNoFall, isRunning, obstacleDetected, usingIntermediateGoal;

    // Ints
    private int x, z, packetsSent, lagbackCounter, useBaritoneCounter, blocksPerSecondCounter, pathUpdateCounter;

    // Doubles
    private double jumpY = -1, blocksPerSecond;

    // Timers
    private final TimerUtil blocksPerSecondTimer = new TimerUtil();
    private final TimerUtil packetTimer = new TimerUtil();
    private final TimerUtil fireworkTimer = new TimerUtil();
    private final TimerUtil takeoffTimer = new TimerUtil();
    private final TimerUtil rotateTimer = new TimerUtil();

    // Enums
    public enum PathMode {Highway, Overworld, Tunnel}

    public enum TakeoffMode {SlowGlide, PacketFly, Jump}

    public enum FlyMode {Control, Firework}

    public enum ChunkWaitMode {Normal, StayStill, SlowFall}

    // Strings
    public String Status = "Disabled", Goal = null, Time = null, Fireworks = null;

    private final ElytraBotGroundListener groundListener = new ElytraBotGroundListener();

    public ElytraBotThreaded() {
        super(MeteorRejectsAddon.CATEGORY, "elytra-bot", "Elytra AutoPilot");
    }

    private void resetTimers() {
        blocksPerSecondTimer.reset();
        packetTimer.reset();
        fireworkTimer.reset();
        takeoffTimer.reset();
        rotateTimer.reset();
    }

    @Override
    public void onActivate() {

        resetTimers();

        int up = 1;
        if (maintainY.get()) {
            int diffY = minYLevel.get() - mc.player.getBlockY();
            if (diffY > 0) up = diffY;
        }
        Status = "Enabled";

        // this should work to properly end the thread, rather than doing thread.suspend()
        isRunning = true;

        // equip an elytra before starting the thead (doesn't seem to work when first starting in the thread)
        if (!ArmorHelper.hasElytra()) {
            FindItemResult elytra = InvUtils.find(Items.ELYTRA);
            if (elytra.found()) InvUtils.move().from(elytra.slot()).toArmor(2);
        }

        if (ElytraBotHelper.checkNoFall()) {
            toggledNoFall = true;
            info("No fall auto-disabled.");
        }


        if (goal == null) {
            if (!useCoordinates.get()) {
                if (Math.abs(Math.abs(mc.player.getX()) - Math.abs(mc.player.getZ())) <= 5 && Math.abs(mc.player.getX()) > 10 && Math.abs(mc.player.getZ()) > 10 && (botMode.get() == PathMode.Highway)) {
                    direction = DirectionUtil.getDiagonalDirection();
                } else direction = DirectionUtil.getDirection();

                goal = generateGoalFromDirection(direction, up);
                Goal = direction.name;
            } else {
                x = gotoX.get();
                z = gotoZ.get();

                // For long distances, create an intermediate goal
                updateGoalPosition();
                Goal = ("X: " + x + ", Z: " + z);
            }
        }

        thread = new Thread() {
            public void run() {
                // to stop the thread loop just set isRunning to false
                while (thread != null && thread.equals(this) && isRunning) {
                    try {
                        loop();
                    } catch (NullPointerException e) {
                        // Silent catch
                    }

                    try {
                        sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        pathUpdateCounter = 0;
        obstacleDetected = false;
        blocksPerSecondTimer.reset();
        thread.start();
    }

    @Override
    public void onDeactivate() {
        direction = null;
        path = null;
        useBaritoneCounter = 0;
        lagback = false;
        lagbackCounter = 0;
        blocksPerSecond = 0;
        blocksPerSecondCounter = 0;
        lastSecondPos = null;
        jumpY = -1;
        last = null;
        PacketFly.toggle(false);
        ElytraFly.toggle(false);
        goal = null;
        //BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().forceCancel();
        thread.interrupt();
        //MiscUtil.suspend(thread);
        thread = null;
        Status = "Disabled";
        Goal = null;
        Time = null;
        Fireworks = null;
        if (toggledNoFall) {
            NoFall noFall = Modules.get().get(NoFall.class);
            if (!noFall.isActive() && toggledNoFall) { // it shouldn't be active but better to just assume a monke user might have
                // somehow enabled it during flight
                info("Re-enabling NoFall");
                noFall.toggle();
            }
        }

        if (restoreChest.get()) groundListener.enable();
        if (useAutoLand.get()) {
            info("Automatically landing");
            ElytraBotHelper.useAutoLand();
        }

    }


    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!isRunning) toggle();
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof EntityStatusS2CPacket p)) return;
        if (p.getStatus() != EntityStatuses.USE_TOTEM_OF_UNDYING) return;

        Entity entity = p.getEntity(mc.world);
        if (entity == null || !(entity.equals(mc.player))) return;

        if (toggleOnPop.get()) {
            warning("Totem popped, disabling");
            isRunning = false;
        }
    }

    public void loop() {
        if (!Utils.canUpdate()) return;

        // Check if we've reached the goal
        if (MiscUtil.distanceTo(goal) < 5) {
            if (!usingIntermediateGoal) {
                // Only stop if this is the final goal
                info("Goal reached!");
                isRunning = false;
                // automatically land
                if (useAutoLand.get()) ElytraBotHelper.useAutoLand();
            } else {
                // If it's an intermediate goal, update the goal and continue
                info("Intermediate goal reached, updating path to final destination");
                updateGoalPosition();
                generatePath();
            }
        }

        // Dynamic path updates for coordinate navigation
        if (useCoordinates.get() && dynamicPathUpdate.get() && mc.player.isGliding()) {
            pathUpdateCounter++;
            if (pathUpdateCounter >= pathUpdateFrequency.get()) {
                pathUpdateCounter = 0;
                debug("Dynamically updating path");
                // Check if we need to update our intermediate goal
                if (usingIntermediateGoal) updateGoalPosition();
                generatePath();
            }
        }

        // Check collision avoidance
        if (useCollisionAvoidance.get() && mc.player.isGliding()) checkAndAvoidObstacles();


        // elytra check
        if (!ArmorHelper.hasElytra()) {
            error("You need an elytra.");
            isRunning = false;
        }

        // no fall check again
        NoFall noFall = Modules.get().get(NoFall.class);
        if (noFall.isActive()) {
            error("You cannot use NoFall while ElytraBot is active!");
            if (!toggledNoFall) toggledNoFall = true;
            noFall.toggle();
        }

        //if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() != Items.ELYTRA) {
        //    for (int i = 0; i < mc.player.getInventory().main.size(); i++) {
        //        ItemStack itemStack = mc.player.getInventory().getStack(i);
        //        if (itemStack.getItem().equals(Items.ELYTRA)) InvUtils.move().from(i).toArmor(2);
        //        debug("Equipping elytra");
        //        else if (i == mc.player.getInventory().main.size() - 1) {
        //            error("You need an elytra.");
        //            MiscUtil.toggle(thread);
        //        }
        //   }
        //}

        // toggle if no fireworks and using firework mode
        if (flyMode.get() == FlyMode.Firework && InvUtils.find(Items.FIREWORK_ROCKET).count() == 0) {
            error("You need fireworks in your inventory if you are using firework mode.");
            isRunning = false;
        }

        if (!MiscUtil.isInRenderDistance(ElytraBotHelper.getLookingAheadPos(chunkLookAheadDistance.get()))) {
            // Apply the appropriate velocity based on wait mode
            switch (chunkWaitMode.get()) {
                case Normal -> {
                    // Normal mode - just stop all movement
                    mc.player.setVelocity(0, 0, 0);
                }
                case StayStill -> {
                    // StayStill mode - counteract gravity to maintain exact position
                    // Apply a tiny upward force to exactly counteract gravity
                    double currentY = mc.player.getVelocity().y;
                    if (currentY < 0) {
                        // If falling, stop the fall completely
                        mc.player.setVelocity(0, 0.04, 0);
                    } else {
                        // Maintain position
                        mc.player.setVelocity(0, 0, 0);
                    }
                }
                case SlowFall -> {
                    // SlowFall mode - allow very slow controlled descent
                    // Set horizontal velocity to 0, and vertical to a very small negative value
                    mc.player.setVelocity(0, -0.03, 0); // Very slow fall rate
                }
            }

            return;
        }

        // switch low dura elytra with fresh one if setting on
        if (autoSwitch.get()) {
            ItemStack chestStack = mc.player.getEquippedStack(EquipmentSlot.CHEST);
            if (chestStack.getItem() == Items.ELYTRA) {
                if (chestStack.getMaxDamage() - chestStack.getDamage() <= switchDurability.get()) {
                    debug("Trying to switch elytra");
                    FindItemResult elytra = InvUtils.find(stack -> stack.getMaxDamage() - stack.getDamage() > switchDurability.get() && stack.getItem() == Items.ELYTRA);

                    InvUtils.move().from(elytra.slot()).toArmor(2);
                    debug("Swapped elytra");
                }
            }
        }

        // takeoff
        double preventPhase = (jumpY + 0.6);
        if (mc.player.isGliding() || mc.player.getY() < preventPhase || mc.player.isOnGround()) {
            if (PacketFly.toggled) {
                debug("//takeoff 1");
                sleep(1500);

                if (mc.player.isGliding() || mc.player.getY() < preventPhase || mc.player.isOnGround()) {
                    sleep(100);
                    debug("//takeoff 2");
                }
            }
        }

        if (!mc.player.isGliding()) {
            ElytraFly.toggle(false);

            BlockPos blockPosAbove = getPlayerPos().add(0, 2, 0);

            if (mc.player.isOnGround() && MiscUtil.isSolid(blockPosAbove) && useBaritone.get() && botMode.get() == PathMode.Highway) {
                Status = "Using baritone";
                useBaritone();
            }

            if (MiscUtil.isSolid(blockPosAbove) && botMode.get() == PathMode.Tunnel) {
                if (MiscUtil.getBlock(blockPosAbove) != Blocks.BEDROCK) {
                    Status = "Mining obstruction";
                    PlayerUtils.centerPlayer();
                    Rotations.rotate(Rotations.getYaw(blockPosAbove), Rotations.getPitch(blockPosAbove), () -> MiscUtil.mine(blockPosAbove));
                } else {
                    if (useBaritone.get()) {
                        Status = "Using baritone";
                        useBaritone();
                    } else {
                        info("The above block is bedrock and useBaritone is false.");
                        isRunning = false;
                    }
                }
            }

            if (jumpY != 1 && Math.abs(mc.player.getY() - jumpY) >= 2) {
                if (useBaritone.get() && direction != null && botMode.get() == PathMode.Highway) {
                    info("Using baritone to get back to the highway.");
                    Status = "Using baritone";
                    useBaritone();
                }
            }

            if (packetsSent < 20) {
                debug("Trying to takeoff.");
                Status = "Taking off";
            }

            fireworkTimer.ms = 0;

            if (mc.player.isOnGround()) {
                jumpY = mc.player.getY();
                generatePath();
                mc.player.jump();
                debug("Path generated, taking off.");
                //debug("Path: " + path);
            } else if (mc.player.getVelocity().y < 0) {
                if (takeoffMode.get() == TakeoffMode.PacketFly) {
                    if (mc.player.getY() > preventPhase && !PacketFly.toggled) PacketFly.toggle(true);
                    debug("Toggling on packet fly.");
                } else if (takeoffMode.get() == TakeoffMode.SlowGlide) {
                    mc.player.setVelocity(0, -0.04, 0);
                    debug("Slow gliding.");
                }


                // Don't send any more packets for about 15 seconds if the takeoff isn't successful.
                // Bcs 2b2t has this annoying thing where it will not let u open elytra if u don't stop sending the packets for a while
                if (packetsSent <= 15) {
                    if (takeoffTimer.hasPassed(650)) {
                        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                        debug("Sending elytra open packet.");
                        takeoffTimer.reset();
                        packetTimer.reset();
                        packetsSent++;
                    }
                } else if (packetTimer.hasPassed(15000)) {
                    packetsSent = 0;
                    debug("15 seconds over.");
                } else {
                    info("Waiting 15 seconds before sending elytra opening packets again");
                    Status = "Waiting to takeoff";
                }
            }
            return;
        } else if (!PacketFly.toggled) {
            packetsSent = 0;

            double speed = MiscUtil.getSpeed();
            if (speed < 0.1) {
                useBaritoneCounter++;

                if (useBaritoneCounter >= 15) {
                    useBaritoneCounter = 0;

                    if (useBaritone.get()) {
                        info("Using baritone to walk a bit because we are stuck.");
                        Status = "Using baritone";
                        useBaritone();
                    } else {
                        info("We are stuck. Enabling the 'useBaritone' setting would help.");
                        isRunning = false;
                    }
                }
            } else useBaritoneCounter = 0;

            // Handle lagback for all flight modes
            handleLagback();

            // Firework usage handled separately
            if (flyMode.get() == FlyMode.Firework) {
                if (fireworkTimer.hasPassed((int) (fireworkDelay.get() * 1000)) && !lagback) {
                    clickOnFirework();
                }
            }
        }

        float health = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        float hunger = mc.player.getHungerManager().getFoodLevel();
        int prevSlot = mc.player.getInventory().getSelectedSlot();
        if (autoEat.get() && !mc.player.isUsingItem() && !Modules.get().get(AutoEat.class).isActive()) {
            if (flyMode.get() != FlyMode.Firework || (flyMode.get() == FlyMode.Firework && !fireworkTimer.hasPassed(100))) {
                if (health <= minHeatlh.get() || hunger <= minHunger.get()) {
                    // todo dogshit code, fix
                    debug("Need to eat.");
                    for (int i = 0; i < 9; i++) {
                        Item item = mc.player.getInventory().getStack(i).getItem();
                        debug("Finding food item.");
                        if (MiscUtil.shouldEatItem(item)) {
                            MiscUtil.eat(i);
                            debug("Trying to eat item.");
                        }
                    }
                }
            }
        } else if (mc.player.isUsingItem() && health >= minHeatlh.get() && hunger >= minHunger.get()) {
            stopEating(prevSlot);
            debug("Stopped eating.");
        }

        if (path == null || path.size() <= 20 || isNextPathTooFar()) {
            generatePath();
            //debug("Generating more path.");
        }

        int distance = 12;
        if (botMode.get() == PathMode.Highway || flyMode.get() == FlyMode.Control) distance = 2;

        boolean remove = false;
        ArrayList<BlockPos> removePositions = new ArrayList<>();

        for (BlockPos pos : path) {
            if (!remove && MiscUtil.distance(pos, getPlayerPos()) <= distance) remove = true;
            if (remove) removePositions.add(pos);
        }

        for (BlockPos pos : removePositions) {
            path.remove(pos);
            previous = pos;
        }

        if (!path.isEmpty()) {
            if (direction != null) {
                debug("Going to " + direction.name);
            } else {
                debug("Going to X: " + x + " Z: " + z);

                if (blocksPerSecondTimer.hasPassed(1000)) {
                    blocksPerSecondTimer.reset();
                    if (lastSecondPos != null) {
                        blocksPerSecondCounter++;
                        blocksPerSecond += PlayerUtils.distanceTo(lastSecondPos);
                    }

                    lastSecondPos = getPlayerPos();
                }

                if (blocksPerSecondCounter == 0 || blocksPerSecond == 0) {
                    Status = "Flying (calculating ETA)";
                } else {
                    // Calculate distance to final destination, not just the intermediate goal
                    double distanceToFinal = Math.sqrt(
                        Math.pow(gotoX.get() - mc.player.getX(), 2) +
                            Math.pow(gotoZ.get() - mc.player.getZ(), 2)
                    );

                    int seconds = (int) (distanceToFinal / (blocksPerSecond / blocksPerSecondCounter));
                    int h = seconds / 3600;
                    int m = (seconds % 3600) / 60;
                    int s = seconds % 60;

                    String etaString = h + "h, " + m + "m, " + s + "s";
                    //debug("Estimated arrival in " + etaString);
                    Time = etaString;

                    // Update the Status variable to include the ETA
                    Status = "Flying (ETA: " + etaString + ")";

                    if (flyMode.get() == FlyMode.Firework) {
                        //debug("Estimated fireworks needed: " + (int) (seconds / fireworkDelay.get()));
                        Fireworks = String.valueOf(Math.round(seconds / fireworkDelay.get()));
                    }
                }
            }

            if (flyMode.get() == FlyMode.Firework) {

                Vec3d vec = new Vec3d(path.get(path.size() - 1).getX(), path.get(path.size() - 1).getY(), path.get(path.size() - 1).getZ());
                mc.player.setYaw((float) Rotations.getYaw(vec));
                mc.player.setPitch((float) Rotations.getPitch(vec));
                debug("Rotating to use firework.");
                //Status = "Flying";
            } else if (flyMode.get() == FlyMode.Control) {
                ElytraFly.toggle(true);

                BlockPos next = null;
                if (path.size() > 1) next = path.get(path.size() - 2);
                ElytraFly.setMotion(path.getLast(), next, previous);

                if (rotate.get() && next != null) {
                    if (rotateTimer.hasPassed(rotateCooldown.get())) {
                        float targetYaw = (float) Rotations.getYaw(next);
                        float targetPitch = (float) Rotations.getPitch(next);

                        // Only rotate if the angle difference is significant
                        if (needsRotation(mc.player.getYaw(), mc.player.getPitch(), targetYaw, targetPitch)) {
                            debug("Rotating player - angle difference exceeds threshold");
                            rotateTimer.reset();
                            mc.player.setYaw(targetYaw);
                            mc.player.setPitch(targetPitch);
                        }
                    }
                }
                debug("Elytra flying to next position.");
            }
        }
    }


    private float normalizeAngle(float angle) {
        angle = angle % 360;
        if (angle > 180) angle -= 360;
        if (angle < -180) angle += 360;
        return angle;
    }

    private float shortestAngleDiff(float angle1, float angle2) {
        float diff = angle1 - angle2;
        if (diff > 180) diff -= 360;
        if (diff < -180) diff += 360;
        return diff;
    }

    private boolean needsRotation(float currentYaw, float currentPitch, float targetYaw, float targetPitch) {
        // Normalize angles to be between -180 and 180 degrees
        currentYaw = normalizeAngle(currentYaw);
        targetYaw = normalizeAngle(targetYaw);

        // Calculate the shortest difference between the two angles
        float yawDiff = Math.abs(shortestAngleDiff(currentYaw, targetYaw));
        float pitchDiff = Math.abs(currentPitch - targetPitch);

        // Return true if either difference exceeds the threshold
        return yawDiff > rotationThreshold.get() || pitchDiff > rotationThreshold.get();
    }

    private static final BlockPos[] ADJACENT_HORIZONTAL = {
        new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0),
        new BlockPos(0, 0, 1), new BlockPos(0, 0, -1)
    };

    private static final BlockPos[] DIAGONAL_HORIZONTAL = {
        new BlockPos(1, 0, 1), new BlockPos(-1, 0, -1),
        new BlockPos(-1, 0, 1), new BlockPos(1, 0, -1)
    };

    private static final BlockPos[] VERTICAL = {
        new BlockPos(0, -1, 0), new BlockPos(0, 1, 0)
    };

    public void generatePath() {
        BlockPos[] positions = Stream.of(ADJACENT_HORIZONTAL, DIAGONAL_HORIZONTAL, VERTICAL)
            .flatMap(Arrays::stream)
            .toArray(BlockPos[]::new);

        ArrayList<BlockPos> checkPositions = new ArrayList<>();
        switch (botMode.get()) {
            case PathMode.Highway:
                checkPositions.addAll(Arrays.asList(ADJACENT_HORIZONTAL));
                checkPositions.addAll(Arrays.asList(DIAGONAL_HORIZONTAL));
                break;
            case PathMode.Overworld:
                int radius = 3;
                for (int x = -radius; x < radius; x++)
                    for (int z = -radius; z < radius; z++)
                        for (int y = radius; y > -radius; y--)
                            checkPositions.add(new BlockPos(x, y, z));
                break;
            case PathMode.Tunnel:
                positions = ADJACENT_HORIZONTAL;
                checkPositions.add(new BlockPos(0, -1, 0));
                break;
        }

        BlockPos start = ElytraBotHelper.getStartPos(jumpY, botMode.get().equals(PathMode.Overworld));
        if (isNextPathTooFar()) start = getPlayerPos();

        boolean updatePath = path == null || path.isEmpty() || isNextPathTooFar() || mc.player.isOnGround();

        // Use collision avoidance if enabled
        if (useCollisionAvoidance.get()) {
            if (updatePath) {
                path = CollisionAvoidance.generateAvoidancePath(null, start, goal, positions, checkPositions, 500);
            } else {
                ArrayList<BlockPos> temp = CollisionAvoidance.generateAvoidancePath(path, path.getFirst(), goal, positions, checkPositions, 500);
                try {
                    temp.addAll(path);
                } catch (NullPointerException ignored) {
                }
                path = temp;
            }
        } else {
            // Original path generation logic
            if (updatePath) {
                path = AStar.generatePath(start, goal, positions, checkPositions, 500);
            } else {
                ArrayList<BlockPos> temp = AStar.generatePath(path.getFirst(), goal, positions, checkPositions, 500);
                try {
                    temp.addAll(path);
                } catch (NullPointerException ignored) {
                }
                path = temp;
            }
        }
    }

    private void checkAndAvoidObstacles() {
        // todo is this whole thing fucked??
        if (!useCollisionAvoidance.get() || path == null || path.isEmpty()) return;

        // Check if there's an immediate collision risk
        boolean collisionDetected = CollisionAvoidance.isImmediateCollisionLikely(lookAheadDistance.get());

        if (collisionDetected) {
            obstacleDetected = true;
            debug("Obstacle detected! Adjusting path...");
            Status = "Avoiding obstacle";

            // Generate a new path that avoids the obstacle
            generatePath();
        } else if (obstacleDetected) {
            obstacleDetected = false;
            Status = "Flying";
        }
    }


    @EventHandler
    private void render3DEvent(Render3DEvent event) {
        if (path != null && renderPath.get()) {
            try {
                last = null;
                for (BlockPos pos : path) {
                    if (last != null) {
                        //debug("Rendering path.");
                        event.renderer.line(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, last.getX() + 0.5, last.getY() + 0.5, last.getZ() + 0.5, pathColour.get());
                    }

                    last = pos;
                }
            } catch (Exception exception) {
                last = null;
            }
        }
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        isRunning = false;
    }

    public void useBaritone() {
        ElytraFly.toggle(false);

        int y = (int) (jumpY - mc.player.getY());
        int x = 0;
        int z = 0;

        int blocks = walkDistance.get();
        switch (direction) {
            case ZM:
                z = -blocks;
            case XM:
                x = -blocks;
            case XP:
                x = blocks;
            case ZP:
                z = blocks;
            case XP_ZP:
                x = blocks;
                z = blocks;
            case XM_ZM:
                x = -blocks;
                z = -blocks;
            case XP_ZM:
                x = blocks;
                z = -blocks;
            case XM_ZP:
                x = -blocks;
                z = blocks;
        }

        walkTo(getPlayerPos().add(x, y, z), true);
        sleep(5000);
        sleepUntil(() -> !BaritoneHelper.isPathing(), 120000);
        BaritoneHelper.stopEverything();
    }

    private void clickOnFirework() {
        if (MeteorClient.mc.player.getMainHandStack().getItem() != Items.FIREWORK_ROCKET) {
            FindItemResult result = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
            if (result.slot() != -1) {
                InvUtils.swap(result.slot(), false);
            }
        }

        //Click
        MiscUtil.useItem();
        fireworkTimer.reset();
    }

    public BlockPos generateGoalFromDirection(DirectionUtil direction, int up) {
        // since we call mc.player.getX/Y/Z multiple times we should just have them as variables
        // and use thos
        int x = mc.player.getBlockX();
        int y = mc.player.getBlockY();
        int z = mc.player.getBlockZ();
        if (direction == DirectionUtil.ZM) {
            return new BlockPos(0, y + up, z - 30000000);
        } else if (direction == DirectionUtil.ZP) {
            return new BlockPos(0, y + up, z + 30000000);
        } else if (direction == DirectionUtil.XM) {
            return new BlockPos(x - 30000000, y + up, 0);
        } else if (direction == DirectionUtil.XP) {
            return new BlockPos(x + 30000000, y + up, 0);
        } else if (direction == DirectionUtil.XP_ZP) {
            return new BlockPos(x + 30000000, y + up, z + 30000000);
        } else if (direction == DirectionUtil.XM_ZM) {
            return new BlockPos(x - 30000000, y + up, z - 30000000);
        } else if (direction == DirectionUtil.XP_ZM) {
            return new BlockPos(x + 30000000, y + up, z - 30000000);
        } else {
            return new BlockPos(x - 30000000, y + up, z + 30000000);
        }
    }

    private BlockPos getPlayerPos() {
        return mc.player.getBlockPos();
    }

    private void walkTo(BlockPos goal, boolean sleepUntilDone) {
        BaritoneHelper.pathToBlockPos(goal);
        if (sleepUntilDone) {
            debug("Waiting for pathing to start");
            sleepUntil(BaritoneHelper::isPathing, 500);
            sleepUntil(() -> !BaritoneHelper.isPathing(), -1);
        }
    }

    private boolean isNextPathTooFar() {
        try {
            return MiscUtil.distance(getPlayerPos(), path.get(path.size() - 1)) > 15;
        } catch (Exception e) {
            return false;
        }
    }

    private void stopEating(int slot) {
        InvUtils.swap(slot, false);
        mc.options.useKey.setPressed(false);
    }

    public static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception ignored) {
        }
    }

    public static void sleepUntil(BooleanSupplier condition, int timeout) {
        long startTime = System.currentTimeMillis();
        while (true) {
            if (condition.getAsBoolean()) {
                break;
            } else if (timeout != -1 && System.currentTimeMillis() - startTime >= timeout) {
                break;
            }

            sleep(10);
        }
    }

    private void onModeChange(FlyMode flyMode) {
        Fireworks = null;
        Time = null;
    }

    private void debug(Object message) {
        if (debug.get()) info(String.valueOf(message));
    }

    private void updateGoalPosition() {
        if (!useCoordinates.get()) return;

        // Calculate the nearest point along the goal direction that's within render distance
        double dx = gotoX.get() - mc.player.getX();
        double dz = gotoZ.get() - mc.player.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);

        // If we're far from the goal, create an intermediate goal within render distance
        if (distance > 500) {
            double factor = 500 / distance;
            int intermediateX = (int) (mc.player.getX() + dx * factor);
            int intermediateZ = (int) (mc.player.getZ() + dz * factor);

            // Create an intermediate goal in the direction of the final goal
            if (maintainY.get()) goal = new BlockPos(intermediateX, minYLevel.get(), intermediateZ);
            else goal = new BlockPos(intermediateX, mc.player.getBlockY() + 1, intermediateZ);
            usingIntermediateGoal = true;
            debug("Created intermediate goal: " + goal);
        } else {
            // We're close enough to use the actual goal
            if (maintainY.get()) goal = new BlockPos(gotoX.get(), minYLevel.get(), gotoZ.get());
            else goal = new BlockPos(gotoX.get(), mc.player.getBlockY() + 1, gotoZ.get());
            usingIntermediateGoal = false;
        }
    }

    // todo need to improve this?
    private BlockPos lastCheckPos = null;

    private void handleLagback() {
        if (lastCheckPos == null) {
            lastCheckPos = mc.player.getBlockPos();
            return;
        }

        if (DistanceHelper.distanceBetween(lastCheckPos, mc.player.getBlockPos()) <= 1) {
            lagbackCounter++;
            lastCheckPos = mc.player.getBlockPos();
            if (lagbackCounter > 3) restartModule();
        }
    }

    private void restartModule() {
        MeteorRejectsAddon.MODULE_THREAD.execute(() -> {
            try {
                Thread.sleep(500);
                ElytraBotThreaded elytraBotThreaded = Modules.get().get(ElytraBotThreaded.class);
                if (!elytraBotThreaded.isActive()) elytraBotThreaded.toggle();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        toggle();
    }

    @Override
    public String getInfoString() {
        return Status;
    }

}
