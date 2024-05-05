package io.github.mortuusars.exposure.fabric.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface StartExposingFrameClientsideCallback {
    Event<StartExposingFrameClientsideCallback> EVENT = EventFactory.createArrayBacked(StartExposingFrameClientsideCallback.class,
            (listeners) -> (player, cameraStack, exposureId, lightLevel, flashHasFired) -> {
                for (StartExposingFrameClientsideCallback listener : listeners) {
                    listener.onExposeFrameClientside(player, cameraStack, exposureId, lightLevel, flashHasFired);
                }
            });

    void onExposeFrameClientside(Player player, ItemStack cameraStack, String exposureId, int lightLevel, boolean flashHasFired);
}
