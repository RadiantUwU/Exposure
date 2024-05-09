package io.github.mortuusars.exposure.fabric.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Fired when Camera tries to take a photo. Cancelable.
 * Client-side event wouldn't fire if server-side event was canceled.
 * If canceled only on the client - shutter would be opened, but the image would not be captured.
 * All checks are passed at this point, and if this event is not canceled - photo will be taken.
 */
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
