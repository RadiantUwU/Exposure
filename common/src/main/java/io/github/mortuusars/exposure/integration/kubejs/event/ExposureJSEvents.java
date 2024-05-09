package io.github.mortuusars.exposure.integration.kubejs.event;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

public interface ExposureJSEvents {
    EventGroup GROUP = EventGroup.of("ExposureEvents");

    EventHandler SHUTTER_OPENING = GROUP.common("shutterOpening", () -> ShutterOpeningEventJS.class).hasResult();
    EventHandler MODIFY_FRAME_DATA = GROUP.server("modifyFrameData", () -> ModifyFrameDataEventJS.class);

    static void register() {
        GROUP.register();
    }
}
