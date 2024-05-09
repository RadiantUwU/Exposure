package io.github.mortuusars.exposure.integration.kubejs.forge;

import io.github.mortuusars.exposure.forge.api.event.ModifyFrameDataEvent;
import io.github.mortuusars.exposure.forge.api.event.ShutterOpeningEvent;
import io.github.mortuusars.exposure.integration.kubejs.ExposureJSPlugin;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;

public class ExposureJSPluginImpl {
    public static void subscribeToEvents() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOW, ExposureJSPluginImpl::onShutterOpening);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOW, ExposureJSPluginImpl::onModifyFrameData);
    }

    public static void onShutterOpening(ShutterOpeningEvent event) {
        if (ExposureJSPlugin.fireShutterOpeningEvent(event.player, event.cameraStack, event.lightLevel, event.shouldFlashFire))
            event.setCanceled(true);
    }

    public static void onModifyFrameData(ModifyFrameDataEvent event) {
        ExposureJSPlugin.fireModifyFrameDataEvent(event.player, event.cameraStack, event.frame, event.entitiesInFrame);
    }
}
