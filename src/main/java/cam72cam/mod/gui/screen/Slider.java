package cam72cam.mod.gui.screen;

import cam72cam.mod.entity.Player;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.gui.widget.ExtendedSlider;

/** Standard slider */
public abstract class Slider extends Button {

    private static class HackSlider extends ExtendedSlider {
        Runnable onSlider;

        public HackSlider(int x, int y, int width, int height, Component prefix, Component suffix, double minValue, double maxValue, double currentValue, double stepSize, int precision, boolean drawString) {
            super(x, y, width, height, prefix, suffix, minValue, maxValue, currentValue, stepSize, precision, drawString);
        }

        @Override
        protected void applyValue() {
            onSlider.run();
        }
    }

    public Slider(IScreenBuilder builder, int x, int y, String text, double min, double max, double start, boolean doublePrecision) {
        super(builder, new HackSlider(builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, 150, 20, Component.literal(text), Component.literal(""), min, max, start, 0, doublePrecision ? 2 : 0, true));
        ((HackSlider) this.button).onSlider = Slider.this::onSlider;
    }

    @Override
    public void onClick(Player.Hand hand) {

    }

    /** Called when the slider value is changed */
    public abstract void onSlider();

    public int getValueInt() {
        return ((ExtendedSlider) button).getValueInt();
    }

    public double getValue() {
        return ((ExtendedSlider) button).getValue();
    }
}
