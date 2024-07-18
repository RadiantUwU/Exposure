package io.github.mortuusars.exposure.integration.compat;

import com.xtracr.realcamera.compat.DisableHelper;
import io.github.mortuusars.exposure.util.CameraInHand;
import net.minecraft.world.entity.player.Player;

public class RealCameraCompat {
    public static void init() {
        DisableHelper.registerOr("renderModel", entity ->
                entity instanceof Player player && CameraInHand.getCamera(player)
                        .map(c -> c.get().getItem().isActive(c.get().getStack()))
                        .orElse(false));
    }
}
