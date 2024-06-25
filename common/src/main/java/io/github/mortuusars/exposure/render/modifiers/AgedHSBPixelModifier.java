package io.github.mortuusars.exposure.render.modifiers;

import io.github.mortuusars.exposure.util.Color;
import io.github.mortuusars.exposure.util.HUSLColorConverter;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.StringUtils;

public class AgedHSBPixelModifier implements IPixelModifier {
    public final int tintColor;
    public final double[] tintColorHsluv;
    public final float tintOpacity;
    public final int blackPoint;
    public final int whitePoint;

    /**
     * @param tintColor in 0xXXXXXX rgb format. Only rightmost 24 bits would be used, anything extra will be discarded.
     * @param tintOpacity ratio of the original color to tint color. Like a layer opacity.
     * @param blackPoint Like in a Levels adjustment. 0-255.
     * @param whitePoint Like in a Levels adjustment. 0-255.
     */
    public AgedHSBPixelModifier(int tintColor, float tintOpacity, int blackPoint, int whitePoint) {
        this.tintColor = tintColor;
        String hexStr = StringUtils.leftPad(Integer.toHexString(tintColor & 0xFFFFFF), 6, "0");
        this.tintColorHsluv = HUSLColorConverter.hexToHsluv("#" + hexStr);
        this.tintOpacity = tintOpacity;
        this.blackPoint = blackPoint & 0xFF; // 0-255
        this.whitePoint = whitePoint & 0xFF; // 0-255
    }

    @Override
    public String getIdSuffix() {
        return "_sepia";
    }

    @Override
    public int modifyPixel(int ABGR) {
        int alpha = FastColor.ABGR32.alpha(ABGR);
        int blue = FastColor.ABGR32.blue(ABGR);
        int green = FastColor.ABGR32.green(ABGR);
        int red = FastColor.ABGR32.red(ABGR);

        // Modify black and white points to make the image appear faded:
        blue = (int) Mth.map(blue, 0, 255, blackPoint, whitePoint);
        green = (int) Mth.map(green, 0, 255, blackPoint, whitePoint);
        red = (int) Mth.map(red, 0, 255, blackPoint, whitePoint);

        float[] baseHSB = new float[3];
        Color.RGBtoHSB(red, green, blue, baseHSB);

        float[] tintHSB = new float[3];
        Color.RGBtoHSB(FastColor.ARGB32.red(tintColor), FastColor.ARGB32.green(tintColor), FastColor.ARGB32.blue(tintColor), tintHSB);

        // Luma is not 100% correct. It's brighter than it would have been originally, but brighter looks better.
        int luma = Mth.clamp((int) (0.45 * red + 0.65 * green + 0.2 * blue), 0, 255);
        int tintedRGB = Color.HSBtoRGB(tintHSB[0], tintHSB[1], luma / 255f);

        // Blend two colors together:
        int newBlue = Mth.clamp((int) Mth.lerp(tintOpacity, blue, FastColor.ARGB32.blue(tintedRGB)), 0, 255);
        int newGreen = Mth.clamp((int) Mth.lerp(tintOpacity, green, FastColor.ARGB32.green(tintedRGB)), 0, 255);
        int newRed = Mth.clamp((int) Mth.lerp(tintOpacity, red, FastColor.ARGB32.red(tintedRGB)), 0, 255);

        return FastColor.ABGR32.color(alpha, newBlue, newGreen, newRed);
    }

    @Override
    public String toString() {
        return "AgedHSBPixelModifier{" +
                "tintColor=#" + Integer.toHexString(tintColor) +
                ", tintOpacity=" + tintOpacity +
                ", blackPoint=" + blackPoint +
                ", whitePoint=" + whitePoint +
                '}';
    }
}
