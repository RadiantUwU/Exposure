package io.github.mortuusars.exposure.integration.kubejs.forge;

import io.github.mortuusars.exposure.forge.api.event.FrameAddedEvent;
import io.github.mortuusars.exposure.forge.api.event.ModifyFrameDataEvent;
import io.github.mortuusars.exposure.forge.api.event.ShutterOpeningEvent;
import io.github.mortuusars.exposure.integration.kubejs.ExposureJSPlugin;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;

public class ExposureJSPluginImpl {
    public static void subscribeToEvents() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOW, ExposureJSPluginImpl::fireShutterOpeningEvent);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOW, ExposureJSPluginImpl::fireModifyFrameDataEvent);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOW, ExposureJSPluginImpl::fireFrameAddedEvent);
    }

    public static void fireShutterOpeningEvent(ShutterOpeningEvent event) {
        if (ExposureJSPlugin.fireShutterOpeningEvent(event.player, event.cameraStack, event.lightLevel, event.shouldFlashFire))
            event.setCanceled(true);
    }

    public static void fireModifyFrameDataEvent(ModifyFrameDataEvent event) {
        ExposureJSPlugin.fireModifyFrameDataEvent(event.player, event.cameraStack, event.frame, event.entitiesInFrame);
    }

    public static void fireFrameAddedEvent(FrameAddedEvent event) {
        ExposureJSPlugin.fireFrameAddedEvent(event.player, event.cameraStack, event.frame);
    }
}
