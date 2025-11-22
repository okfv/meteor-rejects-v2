package anticope.rejects.utils;

import anticope.rejects.modules.CaptchaSolver;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;

public class AutomationHelper {
    private static CaptchaSolver getCaptchaSolver() {
        return Modules.get().get(CaptchaSolver.class);
    }

    public static boolean isSolvingCaptcha() {
        CaptchaSolver solver = getCaptchaSolver();
        return solver != null && solver.isSolving();
    }

    private static KillAura getKillAura() {
        return Modules.get().get(KillAura.class);
    }

    public static boolean isKaActive() {
        KillAura killAura = getKillAura();
        return killAura != null && killAura.isActive();
    }

    public static void disableKa() {
        KillAura killAura = getKillAura();
        if (killAura != null && killAura.isActive()) killAura.toggle();
    }

    public static void enableKa() {
        KillAura killAura = getKillAura();
        if (killAura != null && !killAura.isActive()) killAura.toggle();
    }
}
