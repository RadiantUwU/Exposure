package io.github.mortuusars.exposure.integration.kubejs;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.event.EventResult;
import dev.latvian.mods.kubejs.script.ScriptType;
import io.github.mortuusars.exposure.integration.kubejs.event.ExposureJSEvents;
import io.github.mortuusars.exposure.integration.kubejs.event.ShutterOpeningEventJS;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ExposureJSPlugin extends KubeJSPlugin {
    @Override
    public void init() {
        subscribeToEvents();
    }

    @ExpectPlatform
    public static void subscribeToEvents() {
        throw new AssertionError();
    }

    @Override
    public void registerEvents() {
        ExposureJSEvents.register();
    }

    public static boolean onShutterOpening(Player player, ItemStack cameraStack, int lightLevel, boolean shouldFlashFire) {
        EventResult result = ExposureJSEvents.SHUTTER_OPENING.post(player.level().isClientSide ? ScriptType.CLIENT : ScriptType.SERVER,
                new ShutterOpeningEventJS(player, cameraStack, lightLevel, shouldFlashFire));
        return result.interruptTrue() || result.interruptFalse() || result.interruptDefault();
    }
}
