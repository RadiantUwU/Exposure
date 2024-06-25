package io.github.mortuusars.exposure.fabric.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Fired at the very end of a shot, when frame is added to the film.
 * Fired only on the server side.
 */
public interface FrameAddedCallback {
    Event<FrameAddedCallback> EVENT = EventFactory.createArrayBacked(FrameAddedCallback.class,
            (listeners) -> (player, cameraStack, frame) -> {
                for (FrameAddedCallback listener : listeners) {
                    listener.onFrameAdded(player, cameraStack, frame);
                }
            });

    void onFrameAdded(ServerPlayer player, ItemStack cameraStack, CompoundTag frame);
}
