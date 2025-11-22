package anticope.rejects.events;

import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.text.Text;

public class RaidStartEvent {
    private final ClientBossBar bossBar;
    private final Text raidName;
    private final float initialProgress;

    public RaidStartEvent(ClientBossBar bossBar, Text raidName, float initialProgress) {
        this.bossBar = bossBar;
        this.raidName = raidName;
        this.initialProgress = initialProgress;
    }

    public ClientBossBar getBossBar() {
        return bossBar;
    }

    public Text getRaidName() {
        return raidName;
    }

    public float getInitialProgress() {
        return initialProgress;
    }

    public String getRaidNameString() {
        return raidName.getString();
    }
}