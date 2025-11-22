package anticope.rejects.modules.misc;

import anticope.rejects.MeteorRejectsAddon;
import anticope.rejects.events.RaidEndEvent;
import anticope.rejects.events.RaidProgressEvent;
import anticope.rejects.events.RaidStartEvent;
import anticope.rejects.utils.player.InventoryHelper;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

public class RaidTimer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRefill = settings.createGroup("Auto Refill");

    private final Setting<Integer> potionUseDelay = sgGeneral.add(new IntSetting.Builder()
        .name("potion-use-delay")
        .description("Delay in minutes after getting Bad Omen before drinking again.")
        .defaultValue(5)
        .range(1, 60)
        .sliderRange(1, 60)
        .build()
    );

    private final Setting<Boolean> autoRefill = sgRefill.add(new BoolSetting.Builder()
        .name("auto-refill")
        .description("Automatically refill ominous potions from inventory to hotbar.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> refillSlot = sgRefill.add(new IntSetting.Builder()
        .name("refill-slot")
        .description("Hotbar slot to refill ominous potions to (1-9).")
        .defaultValue(1)
        .min(1)
        .max(9)
        .sliderMin(1)
        .sliderMax(9)
        .visible(() -> autoRefill.get())
        .build()
    );

    private final Setting<Integer> swapDelay = sgRefill.add(new IntSetting.Builder()
        .name("swap-delay")
        .description("Delay in milliseconds between inventory swaps.")
        .defaultValue(250)
        .min(50)
        .max(2000)
        .sliderMin(50)
        .sliderMax(1000)
        .visible(() -> autoRefill.get())
        .build()
    );



    private final Setting<Boolean> raidNotifications = sgGeneral.add(new BoolSetting.Builder()
        .name("raid-notifications")
        .description("Show chat notifications for raid events.")
        .defaultValue(true)
        .build()
    );

    private boolean hasBadOmen = false;
    private boolean raidActive = false;
    private Text currentRaidName = null;
    private long raidStartTime = 0;

    public RaidTimer() {
        super(MeteorRejectsAddon.CATEGORY, "raid-timer", "Tracks raid progress and automatically drinks ominous potions to trigger raids.");
    }

    private boolean drinking;
    private long lastEffectTime; // Time we last successfully got the Bad Omen effect
    private long lastSwapTime; // Time we last performed an inventory swap

    @Override
    public void onActivate() {
        // Reset state on activation
        drinking = false;
        // Set lastEffectTime to a value that allows immediate drinking if needed
        long delayMs = potionUseDelay.get() * 60 * 1000L;
        lastEffectTime = System.currentTimeMillis() - delayMs;
        lastSwapTime = 0;
    }

    @Override
    public void onDeactivate() {
        // Ensure we stop drinking when the module is disabled
        if (drinking) {
            stopDrinking();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        // Check for Bad Omen effect
        boolean currentBadOmen = mc.player.hasStatusEffect(StatusEffects.BAD_OMEN);
        
        if (currentBadOmen && !hasBadOmen) {
            // Bad Omen just applied
            hasBadOmen = true;
        } else if (!currentBadOmen) {
            hasBadOmen = false;
        }

        // Check if we have the Bad Omen effect
        if (mc.player.hasStatusEffect(StatusEffects.BAD_OMEN)) {
            // If we were drinking, we have succeeded. Stop and record the time.
            if (drinking) {
                stopDrinking();
                lastEffectTime = System.currentTimeMillis();
            }
            // Do nothing else; we have the effect as intended.
            return;
        }

        // If we reach here, we do NOT have the Bad Omen effect.
        // Check if the cooldown period has passed.
        long currentTime = System.currentTimeMillis();
        long delayMs = potionUseDelay.get() * 60 * 1000L;
        if (currentTime - lastEffectTime < delayMs) {
            // We are still in the cooldown period, do nothing.
            return;
        }

        // Check if we need to refill hotbar before drinking
        if (autoRefill.get()) {
            refillHotbar();
        }

        // If cooldown is over, we need to start or continue drinking.
        drinkPotion();
    }

    @EventHandler
    private void onRaidStart(RaidStartEvent event) {
        raidActive = true;
        currentRaidName = event.getRaidName();
        raidStartTime = System.currentTimeMillis();
        
        if (raidNotifications.get()) {
            ChatUtils.info("Raid started: %s", event.getRaidNameString());
        }
    }

    @EventHandler
    private void onRaidEnd(RaidEndEvent event) {
        if (raidActive && raidNotifications.get()) {
            long duration = System.currentTimeMillis() - raidStartTime;
            String result = event.wasVictory() ? "Victory" : "Defeat";
            ChatUtils.info("Raid ended: %s (%s) - Duration: %.1fs", 
                event.getRaidNameString(), result, duration / 1000.0);
        }
        
        raidActive = false;
        currentRaidName = null;
        raidStartTime = 0;
    }

    @EventHandler
    private void onRaidProgress(RaidProgressEvent event) {
        if (raidActive && raidNotifications.get()) {
            // todo working,  not sure where/how to use this yet..
            //float progressPercent = event.getNewProgress() * 100;
            //ChatUtils.info("Raid progress: %.1f%%", progressPercent);
        }
    }

    private void drinkPotion() {
        // First, check if we're already holding an ominous bottle
        if (InventoryHelper.getMainhandItem() == Items.OMINOUS_BOTTLE) {
            // Start pressing the "use" key (right-click) and set our state
            mc.options.useKey.setPressed(true);
            drinking = true;
            return;
        }

        // Look for an Ominous Bottle in the hotbar first
        FindItemResult hotbarBottle = InvUtils.findInHotbar(Items.OMINOUS_BOTTLE);
        
        if (hotbarBottle.found()) {
            // Switch to the potion slot in hotbar
            InvUtils.swap(hotbarBottle.slot(), false);
            // Start pressing the "use" key (right-click) and set our state
            mc.options.useKey.setPressed(true);
            drinking = true;
            return;
        }

        // If not found in hotbar, look in all inventory
        FindItemResult inventoryBottle = InvUtils.find(Items.OMINOUS_BOTTLE);
        
        if (inventoryBottle.found()) {
            // Move the bottle from inventory to hotbar for next tick
            if (autoRefill.get()) {
                refillHotbar();
            }
            return; // Return and let next tick handle the drinking after refill
        }

        // If no bottle is found anywhere, stop any action
        if (drinking) stopDrinking(); // Stop trying if we run out mid-drink
    }

    private void stopDrinking() {
        // Stop pressing the "use" key and update our state
        mc.options.useKey.setPressed(false);
        drinking = false;
    }

    private void refillHotbar() {
        long currentTime = System.currentTimeMillis();
        
        // Check swap delay
        if (currentTime - lastSwapTime < swapDelay.get()) {
            return;
        }
        
        int internalSlot = refillSlot.get() - 1; // Convert 1-9 to 0-8
        
        // Check if the specified hotbar slot needs refilling
        net.minecraft.item.ItemStack currentStack = mc.player.getInventory().getStack(internalSlot);
        
        if (currentStack.isEmpty() || currentStack.getItem() != Items.OMINOUS_BOTTLE) {
            // Find ominous bottle in inventory (excluding hotbar)
            FindItemResult bottle = InvUtils.find(itemStack -> 
                itemStack.getItem() == Items.OMINOUS_BOTTLE, 9, 36);
            
            if (bottle.found()) {
                // Move the bottle to the specified hotbar slot
                InvUtils.move().from(bottle.slot()).to(internalSlot);
                lastSwapTime = currentTime;
                info("Refilled slot " + refillSlot.get() + " with Ominous Bottle");
            }
        }
    }



    public boolean isRaidActive() {
        return raidActive;
    }

    public String getCurrentRaidName() {
        return currentRaidName != null ? currentRaidName.getString() : null;
    }

    public long getRaidDuration() {
        return raidActive ? System.currentTimeMillis() - raidStartTime : 0;
    }

    @Override
    public String getInfoString() {
        if (!isActive()) return null;

        long currentTime = System.currentTimeMillis();
        long delayMs = potionUseDelay.get() * 60 * 1000L;
        long timeLeft = delayMs - (currentTime - lastEffectTime);

        if (timeLeft <= 0) return "Drinking";

        // Convert to mm:ss format
        long minutes = timeLeft / (60 * 1000);
        long seconds = (timeLeft % (60 * 1000)) / 1000;

        return String.format("%02d:%02d", minutes, seconds) + " left";
    }
}
