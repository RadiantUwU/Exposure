package io.github.mortuusars.exposure.camera.capture.converter;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.mortuusars.exposure.camera.capture.Capture;
import io.github.mortuusars.exposure.camera.capture.processing.FloydDither;

public class DitheringColorConverter implements IImageToMapColorsConverter {
    @Override
    public byte[] convert(Capture capture, NativeImage image) {
        return FloydDither.ditherWithMapColors(image);
    }
}
