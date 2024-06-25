package io.github.mortuusars.exposure.render.modifiers;

import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

@SuppressWarnings("ClassCanBeRecord")
public class NegativeFilmPixelModifier implements IPixelModifier {
    public final boolean simulateFilmTransparency;

    public NegativeFilmPixelModifier(boolean simulateFilmTransparency) {
        this.simulateFilmTransparency = simulateFilmTransparency;
    }

    @Override
    public String getIdSuffix() {
        return simulateFilmTransparency ? "_negative_film" : "_negative";
    }

    @Override
    public int modifyPixel(int ABGR) {
        int alpha = FastColor.ABGR32.alpha(ABGR);
        int blue = FastColor.ABGR32.blue(ABGR);
        int green = FastColor.ABGR32.green(ABGR);
        int red = FastColor.ABGR32.red(ABGR);

        if (simulateFilmTransparency) {
            // Modify opacity to make lighter colors transparent, like in real film.
            int brightness = (blue + green + red) / 3;
            int opacity = (int) Mth.clamp(brightness * 1.5f, 0, 255);
            alpha = (alpha * opacity) / 255;
        }

        // Invert
        blue = 255 - blue;
        green = 255 - green;
        red = 255 - red;

        return FastColor.ABGR32.color(alpha, blue, green, red);
    }

    @Override
    public String toString() {
        return "NegativeFilmPixelModifier{simulateTransparency=" + simulateFilmTransparency + '}';
    }
}
