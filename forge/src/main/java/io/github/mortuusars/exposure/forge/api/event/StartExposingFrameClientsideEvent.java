package io.github.mortuusars.exposure.forge.api.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired before capturing a frame. Client-side only.
 * This event is not cancellable. To cancel a capture - use {@link ShutterOpeningEvent}.
 */
public class StartExposingFrameClientsideEvent extends Event {
    public final Player player;
    public final ItemStack cameraStack;
    public final String exposureId;
    public final int lightLevel;
    public final boolean flashHasFired;

    public StartExposingFrameClientsideEvent(Player player, ItemStack cameraStack, String exposureId, int lightLevel, boolean flashHasFired) {
        this.player = player;
        this.cameraStack = cameraStack;
        this.exposureId = exposureId;
        this.lightLevel = lightLevel;
        this.flashHasFired = flashHasFired;
    }
}
