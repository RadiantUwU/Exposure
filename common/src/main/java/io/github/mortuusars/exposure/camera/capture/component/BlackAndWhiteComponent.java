package io.github.mortuusars.exposure.camera.capture.component;

import io.github.mortuusars.exposure.camera.capture.Capture;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

import java.awt.*;

public class BlackAndWhiteComponent implements ICaptureComponent {
    @Override
    public int modifyPixel(Capture capture, int color) {
        int red = FastColor.ARGB32.red(color);
        int green = FastColor.ARGB32.green(color);
        int blue = FastColor.ARGB32.blue(color);

        // Weights adding up to more than 1 - to make the image slightly brighter
        int luma = Mth.clamp((int) (0.4 * red + 0.6 * green + 0.15 * blue), 0, 255);

        // Slightly increase the contrast
        int contrast = 136;
        luma = Mth.clamp((luma - 128) * contrast / 128 + 128, 0, 255);

        return FastColor.ARGB32.color(255, luma, luma, luma);
    }
}
