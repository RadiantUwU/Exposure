package io.github.mortuusars.exposure.mixin;

import io.github.mortuusars.exposure.client.gui.screen.camera.ViewfinderControlsScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin {
    @Shadow public boolean isDown;

    /**
     * Allows moving when ControlsScreen is open.
     * This should also handle {@link net.minecraft.client.ToggleKeyMapping} on fabric (forge has separate mixin for it).
     */
    @Inject(method = "isDown", at = @At(value = "HEAD"), cancellable = true)
    private void isDown(CallbackInfoReturnable<Boolean> cir) {
        if (Minecraft.getInstance().screen instanceof ViewfinderControlsScreen)
            cir.setReturnValue(this.isDown);
    }
}
