package io.github.mortuusars.exposure.fabric.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface ShutterOpeningCallback {
    Event<ShutterOpeningCallback> EVENT = EventFactory.createArrayBacked(ShutterOpeningCallback.class,
            (listeners) -> (player, cameraStack, lightLevel, shouldFlashFire) -> {
                for (ShutterOpeningCallback listener : listeners) {
                    if (listener.onShutterOpening(player, cameraStack, lightLevel, shouldFlashFire))
                        return true;
                }

                return false;
            });

    boolean onShutterOpening(Player player, ItemStack cameraStack, int lightLevel, boolean shouldFlashFire);
}
