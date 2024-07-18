package io.github.mortuusars.exposure.integration;

import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.PlatformHelper;
import io.github.mortuusars.exposure.integration.compat.RealCameraCompat;

public class ModCompatibility {
    private static final String REAL_CAMERA = "realcamera";

    @SuppressWarnings("ConstantValue")
    public static void handle() {
        if (Config.Client.REAL_CAMERA_DISABLE_IN_VIEWFINDER.get() && PlatformHelper.isModLoaded(REAL_CAMERA)) {
            try {
                RealCameraCompat.init();
            }
            catch (Exception e) {
                Exposure.LOGGER.error("Failed to apply Real Camera compatibility. {}", e.toString());
            }
        }
    }
}
