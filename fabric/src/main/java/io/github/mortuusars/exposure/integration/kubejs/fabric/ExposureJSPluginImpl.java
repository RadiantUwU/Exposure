package io.github.mortuusars.exposure.integration.kubejs.fabric;

import io.github.mortuusars.exposure.fabric.api.event.ShutterOpeningCallback;
import io.github.mortuusars.exposure.fabric.api.event.StartExposingFrameClientsideCallback;
import io.github.mortuusars.exposure.integration.kubejs.ExposureJSPlugin;

public class ExposureJSPluginImpl {
    public static void subscribeToEvents() {
        ShutterOpeningCallback.EVENT.register(ExposureJSPlugin::onShutterOpening);
        StartExposingFrameClientsideCallback.EVENT.register(ExposureJSPlugin::onStartExposingFrame);
    }
}
