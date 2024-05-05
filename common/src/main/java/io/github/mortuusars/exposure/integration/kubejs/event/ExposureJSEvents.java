package io.github.mortuusars.exposure.integration.kubejs.event;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

public interface ExposureJSEvents {
    EventGroup GROUP = EventGroup.of("ExposureEvents");

    EventHandler SHUTTER_OPENING = GROUP.server("shutterOpening", () -> ShutterOpeningEventJS.class).hasResult();

    EventHandler START_EXPOSING_FRAME = GROUP.client("startExposingFrame", () -> ShutterOpeningEventJS.class);

    static void register() {
        GROUP.register();
    }
}
