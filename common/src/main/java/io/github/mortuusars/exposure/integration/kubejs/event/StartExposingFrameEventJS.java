package io.github.mortuusars.exposure.integration.kubejs.event;

import dev.latvian.mods.kubejs.player.PlayerEventJS;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Fired before capturing a frame. Client-side only.
 * This event is not cancellable. To cancel a capture - use {@link ShutterOpeningEventJS}.
 */
public class StartExposingFrameEventJS extends PlayerEventJS {
    private final Player player;
    private final ItemStack cameraStack;
    private final String exposureId;
    private final int lightLevel;
    private final boolean flashHasFired;

    public StartExposingFrameEventJS(Player player, ItemStack cameraStack, String exposureId, int lightLevel, boolean flashHasFired) {
        this.player = player;
        this.cameraStack = cameraStack;
        this.exposureId = exposureId;
        this.lightLevel = lightLevel;
        this.flashHasFired = flashHasFired;
    }

    @Override
    public Player getEntity() {
        return player;
    }

    public ItemStack getCameraStack() {
        return cameraStack;
    }

    public String getExposureId() {
        return exposureId;
    }

    public int getLightLevel() {
        return lightLevel;
    }

    public boolean shouldFlashFire() {
        return flashHasFired;
    }
}
