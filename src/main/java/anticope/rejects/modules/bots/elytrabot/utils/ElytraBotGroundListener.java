package anticope.rejects.modules.bots.elytrabot.utils;

import anticope.rejects.utils.player.ArmorHelper;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.ChestSwap;
import meteordevelopment.orbit.EventHandler;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ElytraBotGroundListener {

    @EventHandler
    public void chestSwapGroundListener(PlayerMoveEvent event) {
        if (mc.player != null && mc.player.isOnGround()) {
            if (ArmorHelper.hasElytra()) {
                Modules.get().get(ChestSwap.class).swap();
                disable();
            }
        }
    }

    public void enable() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    public void disable() {
        MeteorClient.EVENT_BUS.unsubscribe(this);
    }

}
