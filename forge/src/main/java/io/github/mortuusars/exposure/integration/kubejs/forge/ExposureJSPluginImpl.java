package io.github.mortuusars.exposure.integration.kubejs.forge;

import io.github.mortuusars.exposure.forge.api.event.ShutterOpeningEvent;
import io.github.mortuusars.exposure.integration.kubejs.ExposureJSPlugin;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ExposureJSPluginImpl {
    public static void subscribeToEvents() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOW, ExposureJSPluginImpl::onShutterOpening);
    }

    @SubscribeEvent
    public static void onShutterOpening(ShutterOpeningEvent event) {
        if (ExposureJSPlugin.onShutterOpening(event.player, event.cameraStack, event.lightLevel, event.shouldFlashFire))
            event.setCanceled(true);
    }
}
