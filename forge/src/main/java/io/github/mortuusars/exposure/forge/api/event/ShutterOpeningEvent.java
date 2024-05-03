package io.github.mortuusars.exposure.forge.api.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

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
