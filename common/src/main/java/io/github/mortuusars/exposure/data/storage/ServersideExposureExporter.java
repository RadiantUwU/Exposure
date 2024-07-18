package io.github.mortuusars.exposure.data.storage;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.camera.infrastructure.FilmType;
import io.github.mortuusars.exposure.data.ExposureSize;
import io.github.mortuusars.exposure.render.modifiers.ExposurePixelModifiers;
import io.github.mortuusars.exposure.render.modifiers.IPixelModifier;
import io.github.mortuusars.exposure.util.Color;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.FastColor;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ServersideExposureExporter extends ExposureExporter<ServersideExposureExporter> {
    public ServersideExposureExporter(String name) {
        super(name);
    }

    @Override
    public boolean save(byte[] mapColorPixels, int width, int height, CompoundTag properties) {
        try {
            BufferedImage image = convertToBufferedImage(mapColorPixels, width, height, properties);
            return save(image, properties);
        }
        catch (Exception e) {
            Exposure.LOGGER.error("Failed to save exposure: {}", e.toString());
            return false;
        }
    }

    public boolean save(BufferedImage image, CompoundTag properties) {
        // Existing file would be overwritten
        try {
            String filepath = getFolder() + "/" + (getWorldSubfolder() != null ? getWorldSubfolder() + "/" : "") + getName() + ".png";
            File outputFile = new File(filepath);
            boolean ignored = outputFile.getParentFile().mkdirs();

            if (!ImageIO.write(image, "png", outputFile)) {
                Exposure.LOGGER.error("Exposure was not saved. No appropriate writer has been found.");
                return false;
            }

            if (properties.contains(ExposureSavedData.TIMESTAMP_PROPERTY, CompoundTag.TAG_LONG)) {
                long unixSeconds = properties.getLong(ExposureSavedData.TIMESTAMP_PROPERTY);
                trySetFileCreationDate(outputFile.getAbsolutePath(), unixSeconds);
            }

            Exposure.LOGGER.info("Exposure saved: {}", outputFile);
            return true;
        } catch (IOException e) {
            Exposure.LOGGER.error("Failed to save exposure to file: {}", e.toString());
            return false;
        }
    }

    @NotNull
    protected BufferedImage convertToBufferedImage(byte[] MapColorPixels, int width, int height, CompoundTag properties) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        IPixelModifier modifier = getModifier();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int ABGR = MapColor.getColorFromPackedId(MapColorPixels[x + y * width]); // Mojang returns BGR color
                ABGR = modifier.modifyPixel(ABGR);

                // Tint image like it's rendered in LightroomScreen or NegativeExposureScreen:
                // This is not the best place for it, but I haven't found better a better one.
                if (modifier == ExposurePixelModifiers.NEGATIVE_FILM) {
                    @Nullable FilmType filmType = FilmType.byName(properties.getString(ExposureSavedData.TYPE_PROPERTY));
                    if (filmType != null) {

                        int a = FastColor.ABGR32.alpha(ABGR);
                        int b = FastColor.ABGR32.blue(ABGR);
                        int g = FastColor.ABGR32.green(ABGR);
                        int r = FastColor.ABGR32.red(ABGR);

                        b = b * filmType.frameB / 255;
                        g = g * filmType.frameG / 255;
                        r = r * filmType.frameR / 255;

                        ABGR = FastColor.ABGR32.color(a, b, g, r);
                    }
                }

                image.setRGB(x, y, Color.BGRtoRGB(ABGR));
            }
        }

        if (getSize() != ExposureSize.X1) {
            image = resize(image, getSize());
        }

        return image;
    }

    protected BufferedImage resize(BufferedImage sourceImage, ExposureSize size) {
        int targetWidth = sourceImage.getWidth() * size.getMultiplier();
        int targetHeight = sourceImage.getHeight() * size.getMultiplier();
        Image scaledInstance = sourceImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_FAST);
        BufferedImage outputImg = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        outputImg.getGraphics().drawImage(scaledInstance, 0, 0, null);
        return outputImg;
    }
}
