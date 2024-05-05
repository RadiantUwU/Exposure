package io.github.mortuusars.exposure.forge.api.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired on the server-side when Camera tries to take a photo.
 * All checks are passed at this point, and if this event is not canceled - photo will be taken.
 */
@Cancelable
public class ShutterOpeningEvent extends Event {
    public final Player player;
    public final ItemStack cameraStack;
    public final int lightLevel;
    public final boolean shouldFlashFire;

    public ShutterOpeningEvent(Player player, ItemStack cameraStack, int lightLevel, boolean shouldFlashFire) {
        this.player = player;
        this.cameraStack = cameraStack;
        this.lightLevel = lightLevel;
        this.shouldFlashFire = shouldFlashFire;
    }
}
