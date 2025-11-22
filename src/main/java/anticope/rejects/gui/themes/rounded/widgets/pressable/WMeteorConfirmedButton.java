package anticope.rejects.gui.themes.rounded.widgets.pressable;

import anticope.rejects.gui.themes.rounded.MeteorRoundedGuiTheme;
import anticope.rejects.gui.themes.rounded.MeteorWidget;
import anticope.rejects.utils.gui.GuiUtils;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;
import meteordevelopment.meteorclient.gui.widgets.pressable.WConfirmedButton;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class WMeteorConfirmedButton extends WConfirmedButton implements MeteorWidget {
    public WMeteorConfirmedButton(String text, String confirmText, GuiTexture texture) {
        super(text, confirmText, texture);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        MeteorRoundedGuiTheme theme = theme();
        double pad = pad();
        double border = theme.scale(2);
        int round = theme.roundAmount();

        Color outline = theme.outlineColor.get(pressed, mouseOver);
        Color fg = pressedOnce ? theme.backgroundColor.get(pressed, mouseOver) : theme.textColor.get();
        Color bg = pressedOnce ? theme.textColor.get() : theme.backgroundColor.get(pressed, mouseOver);

        GuiUtils.quadRounded(renderer, x + border, y + border, width - border * 2, height - border * 2, bg, round - border);
        GuiUtils.quadOutlineRounded(renderer, this, outline, round, border);

        String currentText = getText();
        if (currentText != null) {
            renderer.text(currentText, x + width / 2 - textWidth / 2, y + pad, fg, false);
        } else {
            double ts = theme.textHeight();
            renderer.quad(x + width / 2 - ts / 2, y + pad, ts, ts, texture, fg);
        }
    }
}
