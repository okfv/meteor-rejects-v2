package anticope.rejects.modules;

import anticope.rejects.MeteorRejectsAddon;
import anticope.rejects.utils.CaptchaHelper;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

public class CaptchaSolver extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> autoDisable = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-disable")
        .description("Automatically disable the module if no match is found.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> fuzzyMatching = sgGeneral.add(new BoolSetting.Builder()
        .name("fuzzy-matching")
        .description("Enable fuzzy matching for items like 'Bed' -> 'Yellow Bed'.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> solveDelay = sgGeneral.add(new DoubleSetting.Builder()
        .name("solve-delay")
        .description("How long before solving the captcha (in seconds).")
        .defaultValue(1.0)
        .min(0.0)
        .max(5.0)
        .sliderMin(0.0)
        .sliderMax(5.0)
        .build()
    );

    public CaptchaSolver() {
        super(MeteorRejectsAddon.CATEGORY, "captcha-solver", "Automatically solves captcha screens by matching item names.");
    }

    private int solveTimer = 0;
    private boolean solving = false;
    private boolean disabledKA = false;

    public boolean isSolving() {
        return solving;
    }

    @Override
    public void onActivate() {
        solving = false;
        resetSolveTimer();
        disabledKA = false;
    }

    private void resetSolveTimer() {
        solveTimer = (int) (solveDelay.get() * 20);
    }

    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (event.screen == null) return;
        if (event.screen.getTitle().getString().contains("CAPTCHA")) {
            solving = true;
            KillAura ka = Modules.get().get(KillAura.class);
            if (ka.isActive()) {
                ka.toggle();
                disabledKA = true;
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!Utils.canUpdate() || mc.player == null || mc.interactionManager == null) return;
        if (!solving) {
            if (disabledKA) {
                KillAura ka = Modules.get().get(KillAura.class);
                if (!ka.isActive()) ka.toggle();
                disabledKA = false;
            }
            return;
        }
        if (solveTimer > 0) solveTimer--;

        if (needsSolve()) solveCaptcha();
    }

    private boolean needsSolve() {
        if (mc.currentScreen instanceof HandledScreen<?> screen) return screen.getTitle().getString().contains("CAPTCHA");
        return false;
    }

    private void solveCaptcha() {
        if (mc.currentScreen instanceof HandledScreen<?> screen) {
            String[] parts = screen.getTitle().getString().split("Click ");
            String targetItem = parts[1].trim();

            var screenHandler = screen.getScreenHandler();
            if (screenHandler == null) return;

            int totalSlots = screenHandler.slots.size();
            int containerSlots = Math.max(0, totalSlots - 36);

            for (int i = 0; i < containerSlots; i++) {
                ItemStack stack = screenHandler.getSlot(i).getStack();

                if (stack.isEmpty()) continue;
                String itemName = getItemDisplayName(stack);

                if (CaptchaHelper.checkCaptchaItem(itemName, targetItem, fuzzyMatching.get())) {
                    mc.interactionManager.clickSlot(screenHandler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
                    ChatUtils.info("Captcha solved for " + itemName + " !");

                    resetSolveTimer();
                    solving = false;
                    return;
                }
            }

            if (autoDisable.get()) {
                ChatUtils.error("Failed to solve the captcha.");
                toggle();
            }

            resetSolveTimer();
            solving = false;
            mc.player.closeHandledScreen();
        }
    }

    private String getItemDisplayName(ItemStack stack) {
        Text displayName = stack.getName();
        return displayName.getString();
    }
}
