package anticope.rejects.events;

import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.text.Text;

public class RaidProgressEvent {
    private final ClientBossBar bossBar;
    private final Text raidName;
    private final float oldProgress;
    private final float newProgress;

    public RaidProgressEvent(ClientBossBar bossBar, Text raidName, float oldProgress, float newProgress) {
        this.bossBar = bossBar;
        this.raidName = raidName;
        this.oldProgress = oldProgress;
        this.newProgress = newProgress;
    }

    public ClientBossBar getBossBar() {
        return bossBar;
    }

    public Text getRaidName() {
        return raidName;
    }

    public float getOldProgress() {
        return oldProgress;
    }

    public float getNewProgress() {
        return newProgress;
    }

    public String getRaidNameString() {
        return raidName.getString();
    }

    public float getProgressDelta() {
        return newProgress - oldProgress;
    }
}