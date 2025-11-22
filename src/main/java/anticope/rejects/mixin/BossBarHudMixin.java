package anticope.rejects.mixin;

import anticope.rejects.events.RaidEndEvent;
import anticope.rejects.events.RaidProgressEvent;
import anticope.rejects.events.RaidStartEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.network.packet.s2c.play.BossBarS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;

@Mixin(BossBarHud.class)
public class BossBarHudMixin {
    @Shadow
    @Final
    private Map<UUID, ClientBossBar> bossBars;

    @Inject(method = "handlePacket", at = @At("TAIL"))
    private void onHandlePacket(BossBarS2CPacket packet, CallbackInfo ci) {
        // Use the packet's accept method to handle different operation types
        packet.accept(new BossBarS2CPacket.Consumer() {
            @Override
            public void add(UUID uuid, Text name, float percent, BossBar.Color color, BossBar.Style style, boolean darkenSky, boolean dragonMusic, boolean thickenFog) {
                if (isRaidBossBar(name)) {
                    // Raid started - since we're at TAIL, the boss bar should already be added by vanilla
                    ClientBossBar raidBossBar = bossBars.get(uuid);
                    if (raidBossBar != null) {
                        RaidStartEvent event = new RaidStartEvent(raidBossBar, name, percent);
                        MeteorClient.EVENT_BUS.post(event);
                    } else {
                        // Fallback: create a temporary ClientBossBar for the event
                        // This shouldn't happen if vanilla logic executed properly
                        ClientBossBar tempBossBar = new ClientBossBar(uuid, name, percent, color, style, darkenSky, dragonMusic, thickenFog);
                        RaidStartEvent event = new RaidStartEvent(tempBossBar, name, percent);
                        MeteorClient.EVENT_BUS.post(event);
                    }
                }
            }

            @Override
            public void remove(UUID uuid) {
                ClientBossBar existingBossBar = bossBars.get(uuid);
                if (existingBossBar != null && isRaidBossBar(existingBossBar.getName())) {
                    // Raid ended
                    boolean wasVictory = existingBossBar.getPercent() <= 0.0f;
                    RaidEndEvent event = new RaidEndEvent(
                        existingBossBar, 
                        existingBossBar.getName(), 
                        wasVictory, 
                        existingBossBar.getPercent()
                    );
                    MeteorClient.EVENT_BUS.post(event);
                }
            }

            @Override
            public void updateProgress(UUID uuid, float percent) {
                ClientBossBar existingBossBar = bossBars.get(uuid);
                if (existingBossBar != null && isRaidBossBar(existingBossBar.getName())) {
                    // Raid progress changed
                    float oldProgress = existingBossBar.getPercent();
                    
                    RaidProgressEvent event = new RaidProgressEvent(
                        existingBossBar,
                        existingBossBar.getName(),
                        oldProgress,
                        percent
                    );
                    MeteorClient.EVENT_BUS.post(event);
                }
            }

            @Override
            public void updateName(UUID uuid, Text name) {
                // Not needed for raid detection
            }

            @Override
            public void updateStyle(UUID uuid, BossBar.Color color, BossBar.Style style) {
                // Not needed for raid detection
            }

            @Override
            public void updateProperties(UUID uuid, boolean darkenSky, boolean dragonMusic, boolean thickenFog) {
                // Not needed for raid detection
            }
        });
    }

    @Unique
    private boolean isRaidBossBar(Text name) {
        if (name == null) return false;
        
        // Check if it's a translatable text with raid-related translation key
        TextContent content = name.getContent();
        if (content instanceof TranslatableTextContent translatable) {
            String key = translatable.getKey();
            // Known raid translation keys
            return key.equals("event.minecraft.raid") ||
                   key.equals("event.minecraft.raid.victory") ||
                   key.equals("event.minecraft.raid.defeat") ||
                   key.startsWith("event.minecraft.raid") ||
                   key.contains("raid") ||
                   key.contains("pillager");
        }
        
        // Only use getString as absolute last resort for non-translatable text
        String nameString = name.getString().toLowerCase();
        return nameString.contains("raid") || nameString.contains("pillager") || nameString.contains("illager");
    }
}