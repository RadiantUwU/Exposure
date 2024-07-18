package io.github.mortuusars.exposure.data.storage;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.camera.capture.component.ICaptureComponent;
import io.github.mortuusars.exposure.camera.infrastructure.FilmType;
import io.github.mortuusars.exposure.data.ExposureSize;
import io.github.mortuusars.exposure.render.modifiers.ExposurePixelModifiers;
import io.github.mortuusars.exposure.render.modifiers.IPixelModifier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.FastColor;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Date;

public class ClientsideExposureExporter extends ExposureExporter<ClientsideExposureExporter> implements ICaptureComponent {
    public ClientsideExposureExporter(String name) {
        super(name);
    }

    @Override
    public boolean save(byte[] mapColorPixels, int width, int height, CompoundTag properties) {
        try (NativeImage image = convertToNativeImage(mapColorPixels, width, height, properties)) {
            return save(image, properties);
        }
        catch (Exception e) {
            Exposure.LOGGER.error("Cannot convert exposure pixels to NativeImage: {}", e.toString());
            return false;
        }
    }

    public boolean save(NativeImage image, CompoundTag properties) {
        // Existing file would be overwritten
        try {
            String filepath = getFolder() + "/" + (getWorldSubfolder() != null ? getWorldSubfolder() + "/" : "") + getName() + ".png";
            File outputFile = new File(filepath);
            boolean ignored = outputFile.getParentFile().mkdirs();

            image.writeToFile(outputFile);

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

    protected void trySetFileCreationDate(String filePath, long creationTimeUnixSeconds) {
        try {
            Date creationDate = Date.from(Instant.ofEpochSecond(creationTimeUnixSeconds));

            BasicFileAttributeView attributes = Files.getFileAttributeView(Paths.get(filePath), BasicFileAttributeView.class);
            FileTime creationTime = FileTime.fromMillis(creationDate.getTime());
            FileTime modifyTime = FileTime.fromMillis(System.currentTimeMillis());
            attributes.setTimes(modifyTime, modifyTime, creationTime);
        }
        catch (Exception ignored) { }
    }

    @NotNull
    protected NativeImage convertToNativeImage(byte[] MapColorPixels, int width, int height, CompoundTag properties) {
        NativeImage image = new NativeImage(width, height, false);
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

                image.setPixelRGBA(x, y, ABGR);
            }
        }

        if (getSize() != ExposureSize.X1) {
            int resultWidth = image.getWidth() * getSize().getMultiplier();
            int resultHeight = image.getHeight() * getSize().getMultiplier();
            NativeImage resized = resize(image, 0, 0, image.getWidth(), image.getHeight(), resultWidth, resultHeight);
            image.close();
            image = resized;
        }

        return image;
    }

    protected NativeImage resize(NativeImage source, int sourceX, int sourceY, int sourceWidth, int sourceHeight,
                                              int resultWidth, int resultHeight) {
        NativeImage result = new NativeImage(source.format(), resultWidth, resultHeight, false);

        for (int x = 0; x < resultWidth; x++) {
            float ratioX = x / (float)resultWidth;
            int sourcePosX = (int)(sourceX + (sourceWidth * ratioX));

            for (int y = 0; y < resultHeight; y++) {
                float ratioY = y / (float)resultHeight;
                int sourcePosY = (int)(sourceY + (sourceHeight * ratioY));
                int color = source.getPixelRGBA(sourcePosX, sourcePosY);
                result.setPixelRGBA(x, y, color);
            }
        }

        return result;
    }
}
