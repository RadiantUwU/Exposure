package io.github.mortuusars.exposure.forge.api.event;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired at the very end of a shot, when frame is added to the film.
 * Fired only on the server side.
 */
public class FrameAddedEvent extends Event {
    public final ServerPlayer player;
    public final ItemStack cameraStack;
    public final CompoundTag frame;

    public FrameAddedEvent(ServerPlayer player, ItemStack cameraStack, CompoundTag frame) {
        this.player = player;
        this.cameraStack = cameraStack;
        this.frame = frame;
    }
}
