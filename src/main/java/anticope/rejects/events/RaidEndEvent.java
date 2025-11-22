package anticope.rejects.events;

import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.text.Text;

public class RaidEndEvent {
    private final ClientBossBar bossBar;
    private final Text raidName;
    private final boolean wasVictory;
    private final float finalProgress;

    public RaidEndEvent(ClientBossBar bossBar, Text raidName, boolean wasVictory, float finalProgress) {
        this.bossBar = bossBar;
        this.raidName = raidName;
        this.wasVictory = wasVictory;
        this.finalProgress = finalProgress;
    }

    public ClientBossBar getBossBar() {
        return bossBar;
    }

    public Text getRaidName() {
        return raidName;
    }

    public boolean wasVictory() {
        return wasVictory;
    }

    public float getFinalProgress() {
        return finalProgress;
    }

    public String getRaidNameString() {
        return raidName.getString();
    }
}