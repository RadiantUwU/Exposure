package io.github.mortuusars.exposure.render.modifiers;

import net.minecraft.util.FastColor;

@SuppressWarnings({"ClassCanBeRecord", "unused"})
public class MultiplyPixelModifier implements IPixelModifier {
    public final int multiplyColor;

    public MultiplyPixelModifier(int multiplyColor) {
        this.multiplyColor = multiplyColor;
    }

    @Override
    public int modifyPixel(int ABGR) {
        if (multiplyColor == 0)
            return ABGR;

        int alpha = FastColor.ABGR32.alpha(ABGR);
        int blue = FastColor.ABGR32.blue(ABGR);
        int green = FastColor.ABGR32.green(ABGR);
        int red = FastColor.ABGR32.red(ABGR);

        int tintAlpha = FastColor.ARGB32.alpha(ABGR);
        int tintBlue = FastColor.ARGB32.blue(ABGR);
        int tintGreen = FastColor.ARGB32.green(ABGR);
        int tintRed = FastColor.ARGB32.red(ABGR);

        alpha = Math.min(255, (alpha * tintAlpha) / 255);
        blue = Math.min(255, (blue * tintBlue) / 255);
        green = Math.min(255, (green * tintGreen) / 255);
        red = Math.min(255, (red * tintRed) / 255);

        return FastColor.ABGR32.color(alpha, blue, green, red);
    }

    @Override
    public String getIdSuffix() {
        return multiplyColor != 0 ? "_tint" + Integer.toHexString(multiplyColor) : "";
    }
}
