package io.github.mortuusars.exposure.mixin;

import io.github.mortuusars.exposure.camera.CameraClient;
import io.github.mortuusars.exposure.camera.viewfinder.Viewfinder;
import io.github.mortuusars.exposure.gui.screen.camera.ViewfinderControlsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;added()V"))
    void onSetScreen(Screen screen, CallbackInfo ci) {
        if (Viewfinder.isOpen() && !(screen instanceof ViewfinderControlsScreen)) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null)
                CameraClient.deactivate(player);
        }
    }
}
