package io.github.mortuusars.exposure.camera.capture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

public class ScreenshotCapture extends Capture {
    @Override
    public NativeImage captureImage() {
        return Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget());
    }
}
