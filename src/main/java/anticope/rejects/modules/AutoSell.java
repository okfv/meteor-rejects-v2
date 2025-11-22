package anticope.rejects.modules;

import anticope.rejects.MeteorRejectsAddon;
import anticope.rejects.utils.AutomationHelper;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoSell extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTiming = settings.createGroup("Timing");

    private final Setting<List<Item>> sellItems = sgGeneral.add(new ItemListSetting.Builder()
        .name("sell-items")
        .description("Items to automatically sell.")
        .build()
    );

    private final Setting<Integer> hotbarSlot = sgGeneral.add(new IntSetting.Builder()
        .name("hotbar-slot")
        .description("Hotbar slot to use for selling items (1-9).")
        .defaultValue(9)
        .min(1).max(9)
        .sliderMin(1).sliderMax(9)
        .build()
    );

    private final Setting<Double> actionDelay = sgTiming.add(new DoubleSetting.Builder()
        .name("action-delay")
        .description("The delay in seconds when moving items into the hotbar.")
        .defaultValue(0.5)
        .min(0.1).max(5.0)
        .sliderMin(0.1).sliderMax(2.0)
        .build()
    );

    private final Setting<Double> sellCooldown = sgTiming.add(new DoubleSetting.Builder()
        .name("sell-cooldown")
        .description("The cooldown in seconds AFTER a sale before selling the next item.")
        .defaultValue(10.0)
        .min(0.5).max(30.0)
        .sliderMin(0.5).sliderMax(10.0)
        .build()
    );

    private final Setting<Integer> kaPause = sgTiming.add(new IntSetting.Builder()
        .name("ka-pause-ticks")
        .description("The short pause in ticks to wait after disabling KillAura before acting.")
        .defaultValue(2)
        .min(0).max(20)
        .build()
    );

    public AutoSell() {
        super(MeteorRejectsAddon.CATEGORY, "auto-sell", "Automatically sells specified items using /sell command.");
    }

    private static final Pattern SALE_PATTERN = Pattern.compile(".*\\$([0-9,]+(?:\\.[0-9]{2})?)\\s+has\\s+been\\s+added\\s+to\\s+your\\s+account.*", Pattern.CASE_INSENSITIVE);

    private int actionTimer;
    private int kaPauseTimer;
    private int sellCooldownTimer;
    private int toSellSlot = -1;
    private boolean selling = false;
    private boolean disabledKA = false;
    private double totalEarnings = 0.0;
    private long startTime = 0;
    private String displayString = null;
    private int displayUpdateTimer = 0;

    @Override
    public void onActivate() {
        actionTimer = 0;
        kaPauseTimer = 0;
        sellCooldownTimer = 0;
        selling = false;
        toSellSlot = -1;
        disabledKA = false;
        totalEarnings = 0.0;
        startTime = System.currentTimeMillis();
        displayString = null;
        displayUpdateTimer = 0;
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        if (!isActive()) return;

        String message = event.getMessage().getString();
        Matcher matcher = SALE_PATTERN.matcher(message);

        if (matcher.find()) {
            try {
                String amountStr = matcher.group(1).replace(",", "");
                double amount = Double.parseDouble(amountStr);
                totalEarnings += amount;
            } catch (NumberFormatException ignored) {
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!Utils.canUpdate() || sellItems.get().isEmpty() || AutomationHelper.isSolvingCaptcha()) return;

        if (displayUpdateTimer <= 0) {
            updateDisplayString();
            displayUpdateTimer = 100;
        } else displayUpdateTimer--;

        if (sellCooldownTimer > 0) {
            sellCooldownTimer--;
            if (disabledKA) {
                AutomationHelper.enableKa();
                disabledKA = false;
            }
            return;
        }

        if (kaPauseTimer > 0) {
            kaPauseTimer--;
            return;
        }

        if (selling) {
            if (checkAndInitiateKaPause()) return;
            swapAndSell(toSellSlot);
            startSaleCooldown();
            return;
        }

        FindItemResult hotbarItem = findSellableItemInHotbar();
        if (hotbarItem != null) {
            selling = true;
            toSellSlot = hotbarItem.slot();
            resetActionTimer();
            return;
        }

        FindItemResult invItem = findSellableItemInAll();
        if (invItem != null) {
            if (checkAndInitiateKaPause()) return;

            if (actionTimer > 0) actionTimer--;
            else moveItemToSell(invItem);
            return;
        }

        if (disabledKA) {
            AutomationHelper.enableKa();
            disabledKA = false;
        }
    }

    private boolean checkAndInitiateKaPause() {
        if (AutomationHelper.isKaActive()) {
            disabledKA = true;
            AutomationHelper.disableKa();
            kaPauseTimer = kaPause.get();
            return true;
        }
        return false;
    }

    private void startSaleCooldown() {
        selling = false;
        toSellSlot = -1;
        sellCooldownTimer = (int) (sellCooldown.get() * 20);
    }

    private void resetActionTimer() {
        actionTimer = (int) (actionDelay.get() * 20);
    }

    private FindItemResult findSellableItemInHotbar() {
        for (Item item : sellItems.get()) {
            FindItemResult result = InvUtils.findInHotbar(i -> i.getItem() == item && i.getCount() >= i.getMaxCount());
            if (result.found()) return result;
        }
        return null;
    }

    private FindItemResult findSellableItemInAll() {
        for (Item item : sellItems.get()) {
            FindItemResult result = InvUtils.find(i -> i.getItem() == item && i.getCount() >= i.getMaxCount());
            if (result.found()) return result;
        }
        return null;
    }

    private void moveItemToSell(FindItemResult item) {
        InvUtils.move().from(item.slot()).to(hotbarSlot.get() - 1);
    }

    private void swapAndSell(int slot) {
        InvUtils.swap(slot, false);
        mc.player.networkHandler.sendChatCommand("sell hand");
    }

    private void updateDisplayString() {
        if (totalEarnings > 0) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            double hoursElapsed = elapsedTime / (1000.0 * 60.0 * 60.0);

            if (hoursElapsed > 0) {
                double earningsPerHour = totalEarnings / hoursElapsed;
                displayString = String.format("$%.2f ($%.0f/hr)", totalEarnings, earningsPerHour);
            } else displayString = String.format("$%.2f", totalEarnings);
        } else displayString = null;
    }

    @Override
    public String getInfoString() {
        return displayString;
    }
}
