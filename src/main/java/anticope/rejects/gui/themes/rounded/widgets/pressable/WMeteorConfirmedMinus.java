package anticope.rejects.gui.themes.rounded.widgets.pressable;

import anticope.rejects.gui.themes.rounded.MeteorRoundedGuiTheme;
import anticope.rejects.gui.themes.rounded.MeteorWidget;
import anticope.rejects.utils.gui.GuiUtils;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.pressable.WConfirmedMinus;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class WMeteorConfirmedMinus extends WConfirmedMinus implements MeteorWidget {
    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        MeteorRoundedGuiTheme theme = theme();
        double pad = pad();
        double thickness = theme.scale(2);
        double s = theme.scale(3);
        int round = theme.roundAmount();

        Color outline = theme.outlineColor.get(pressed, mouseOver);
        Color fg = pressedOnce ? theme.backgroundColor.get(pressed, mouseOver) : theme.minusColor.get();
        Color bg = pressedOnce ? theme.minusColor.get() : theme.backgroundColor.get(pressed, mouseOver);

        GuiUtils.quadRounded(renderer, x + thickness, y + thickness, width - thickness * 2, height - thickness * 2, bg, round - thickness);
        GuiUtils.quadOutlineRounded(renderer, this, outline, round, thickness);
        renderer.quad(x + pad, y + height / 2 - s / 2, width - pad * 2, s, fg);
    }
}
