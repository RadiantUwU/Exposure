package io.github.mortuusars.exposure.integration.kubejs.event;

import dev.latvian.mods.kubejs.player.PlayerEventJS;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ShutterOpeningEventJS extends PlayerEventJS {
    private final Player player;
    private final ItemStack cameraStack;
    private final int lightLevel;
    private final boolean shouldFlashFire;

    public ShutterOpeningEventJS(Player player, ItemStack cameraStack, int lightLevel, boolean shouldFlashFire) {
        this.player = player;
        this.cameraStack = cameraStack;
        this.lightLevel = lightLevel;
        this.shouldFlashFire = shouldFlashFire;
    }

    @Override
    public Player getEntity() {
        return player;
    }

    public ItemStack getCameraStack() {
        return cameraStack;
    }

    public int getLightLevel() {
        return lightLevel;
    }

    public boolean isShouldFlashFire() {
        return shouldFlashFire;
    }
}
