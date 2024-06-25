package io.github.mortuusars.exposure.camera.capture.component;

import io.github.mortuusars.exposure.camera.capture.Capture;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

import java.awt.*;

public class BlackAndWhiteComponent implements ICaptureComponent {
    @Override
    public int modifyPixel(Capture capture, int colorABGR) {
        int alpha = FastColor.ABGR32.alpha(colorABGR);
        int blue = FastColor.ABGR32.blue(colorABGR);
        int green = FastColor.ABGR32.green(colorABGR);
        int red = FastColor.ABGR32.red(colorABGR);

        // Weights adding up to more than 1 - to make the image slightly brighter
        int luma = Mth.clamp((int) (0.299 * red + 0.587 * green + 0.114 * blue), 0, 255);

        // Slightly increase the contrast
        int contrast = 145;
        luma = Mth.clamp((luma - 128) * contrast / 128 + 128, 0, 255);

        return FastColor.ABGR32.color(alpha, luma, luma, luma);
    }
}
