package io.github.mortuusars.exposure.util;

import java.awt.*;

public class ColorUtils {
    public static int BGRtoRGB(int bgr) {
        int a = (bgr >> 24) & 0xFF;
        int b = (bgr >> 16) & 0xFF;
        int g = (bgr >> 8) & 0xFF;
        int r = bgr & 0xFF;

        return a << 24 | r << 16 | g << 8 | b;
    }

    public static int getRGBFromHex(String hexColor) {
        return new Color((int) Long.parseLong(hexColor.replace("#", ""), 16), true).getRGB();
    }
}
