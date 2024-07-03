package io.github.mortuusars.exposure.camera;

import com.google.common.base.Preconditions;
import io.github.mortuusars.exposure.camera.infrastructure.CompositionGuide;
import io.github.mortuusars.exposure.camera.infrastructure.FlashMode;
import io.github.mortuusars.exposure.camera.infrastructure.ShutterSpeed;
import io.github.mortuusars.exposure.network.Packets;
import io.github.mortuusars.exposure.network.packet.server.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public class CameraClient {
    public static Optional<Camera<?>> getCamera() {
        return Camera.getCamera(Minecraft.getInstance().player);
    }

    public static void deactivate(Player player) {
        Preconditions.checkState(player.level().isClientSide, "Can only be called clientside.");
        Camera.getCamera(player).ifPresent(camera -> camera.deactivate(player));
        Packets.sendToServer(new DeactivateCameraC2SP());
    }

    public static void setZoom(double focalLength) {
        getCamera()
                .ifPresent(c -> {
                    c.apply((item, stack) -> item.setZoom(stack, focalLength));
                    Packets.sendToServer(new CameraSetZoomC2SP(focalLength));
                });
    }

    public static void setShutterSpeed(ShutterSpeed shutterSpeed) {
        getCamera()
                .ifPresent(c -> {
                    c.apply((item, stack) -> item.setShutterSpeed(stack, shutterSpeed));
                    Packets.sendToServer(new CameraSetShutterSpeedC2SP(shutterSpeed));
                });
    }

    public static void setFlashMode(FlashMode flashMode) {
        getCamera()
                .ifPresent(c -> {
                    c.apply((item, stack) -> item.setFlashMode(stack, flashMode));
                    Packets.sendToServer(new CameraSetFlashModeC2SP(flashMode));
                });
    }

    public static void setCompositionGuide(CompositionGuide guide) {
        getCamera()
                .ifPresent(c -> {
                    c.apply((item, stack) -> item.setCompositionGuide(stack, guide));
                    Packets.sendToServer(new CameraSetCompositionGuideC2SP(guide));
                });
    }

    public static void setSelfieMode(boolean inSelfieMode) {
        getCamera()
                .ifPresent(c -> {
                    c.apply((item, stack) -> item.setSelfieModeWithEffects(Minecraft.getInstance().player, stack, inSelfieMode));
                    Packets.sendToServer(new CameraSetSelfieModeC2SP(inSelfieMode));
                });
    }
}
