package io.github.mortuusars.exposure.camera.capture.component;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.camera.capture.Capture;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

public class BaseComponent implements ICaptureComponent {
    private final boolean hideGuiValue;
    private boolean storedGuiHidden;
    private CameraType storedCameraType;

    public BaseComponent(boolean hideGuiOnCapture) {
        this.hideGuiValue = hideGuiOnCapture;
        storedGuiHidden = false;
        storedCameraType = CameraType.FIRST_PERSON;
    }

    public BaseComponent() {
        this(true);
    }

    @Override
    public int getFramesDelay(Capture capture) {
        return Config.Client.CAPTURE_DELAY_FRAMES.get();
    }

    @Override
    public void onDelayFrame(Capture capture, int delayFramesLeft) {
        if (delayFramesLeft == Math.max(0, Config.Client.CAPTURE_DELAY_FRAMES.get() - 1)) { // Right before capturing
            Minecraft mc = Minecraft.getInstance();
            storedGuiHidden = mc.options.hideGui;
            storedCameraType = mc.options.getCameraType();

            mc.options.hideGui = hideGuiValue;
            CameraType cameraType = Minecraft.getInstance().options.getCameraType()
                    == CameraType.THIRD_PERSON_FRONT ? CameraType.THIRD_PERSON_FRONT : CameraType.FIRST_PERSON;
            mc.options.setCameraType(cameraType);
        }
    }

    @Override
    public void screenshotTaken(Capture capture, NativeImage screenshot) {
        Minecraft mc = Minecraft.getInstance();
        mc.options.hideGui = storedGuiHidden;
        mc.options.setCameraType(storedCameraType);
    }
}
