package io.github.mortuusars.exposure.camera.capture.component;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.camera.capture.Capture;
import io.github.mortuusars.exposure.util.CameraInHand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class FlashComponent implements ICaptureComponent {
    
    @Override
    public int getTicksDelay(Capture capture) {
        return 1 + Config.Client.FLASH_CAPTURE_DELAY_TICKS.get();
    }

    @Override
    public void initialize(Capture capture) {
        int ticksDelay = capture.getTicksDelay();
        int framesDelay = capture.getFramesDelay();
        if (ticksDelay > 6) {
            Exposure.LOGGER.warn("Capture ticksDelay of '{}' can be too long for use with a flash. " +
                    "The flash might disappear in that time.", ticksDelay);
        }
        if (framesDelay > 20) {
            Exposure.LOGGER.warn("Capture framesDelay of '{}' can be too long for use with a flash. " +
                    "The flash might disappear in that time.", ticksDelay);
        }
    }

    @Override
    public void imageTaken(Capture capture, NativeImage screenshot) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null)
            return;

        CameraInHand cameraInHand = new CameraInHand(player);
        if (cameraInHand.isEmpty())
            return;

        cameraInHand.getItem().spawnClientsideFlashEffects(player, cameraInHand.getStack());
    }
}
