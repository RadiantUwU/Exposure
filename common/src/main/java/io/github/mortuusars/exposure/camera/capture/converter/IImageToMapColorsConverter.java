package io.github.mortuusars.exposure.camera.capture.converter;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.mortuusars.exposure.camera.capture.Capture;

import java.awt.image.BufferedImage;

public interface IImageToMapColorsConverter {
    byte[] convert(Capture capture, NativeImage image);
    byte[] convert(NativeImage image);
}
