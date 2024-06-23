package io.github.mortuusars.exposure.camera.capture.component;

import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.camera.capture.Capture;

public class FlashComponent implements ICaptureComponent {
    
    @Override
    public int getTicksDelay(Capture capture) {
        return 1 + Config.Client.FLASH_CAPTURE_DELAY_TICKS.get();
    }

    @Override
    public void initialize(Capture capture) {
        int ticksDelay = capture.getTicksDelay();
        int framesDelay = capture.getFramesDelay();
        if (ticksDelay > 6) {
            Exposure.LOGGER.warn("Capture ticksDelay of '{}' can be too long for use with a flash. " +
                    "The flash might disappear in that time.", ticksDelay);
        }
        if (framesDelay > 20) {
            Exposure.LOGGER.warn("Capture framesDelay of '{}' can be too long for use with a flash. " +
                    "The flash might disappear in that time.", ticksDelay);
        }
    }
}
