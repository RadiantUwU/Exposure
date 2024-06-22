package io.github.mortuusars.exposure.data.storage;

import com.mojang.blaze3d.platform.NativeImage;
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Date;
import java.util.function.Supplier;

public class ExposureExporter {
    
    private final String name;

    private String folder = "exposures";
    @Nullable
    private String worldName = null;
    private IPixelModifier modifier = ExposurePixelModifiers.EMPTY;
    private ExposureSize size = ExposureSize.X1;

    public ExposureExporter(String name) {
        this.name = name;
    }

    public String getFolder() { return folder; }
    public @Nullable String getWorldSubfolder() { return worldName; }
    public IPixelModifier getModifier() { return modifier; }
    public ExposureSize getSize() { return size; }

    public ExposureExporter withFolder(String folder) {
        this.folder = folder;
        return this;
    }

    public ExposureExporter withDefaultFolder() {
        this.folder = "exposures";
        return this;
    }

    public ExposureExporter organizeByWorld(@Nullable String worldName) {
        this.worldName = worldName;
        return this;
    }

    public ExposureExporter organizeByWorld(boolean organize, Supplier<@Nullable String> worldNameSupplier) {
        this.worldName = organize ? worldNameSupplier.get() : null;
        return this;
    }

    public ExposureExporter withModifier(IPixelModifier modifier) {
        this.modifier = modifier;
        return this;
    }

    public ExposureExporter withSize(ExposureSize size) {
        this.size = size;
        return this;
    }

    public boolean save(ExposureSavedData data) {
        return save(data.getPixels(), data.getWidth(), data.getHeight(), data.getProperties());
    }

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
            File outputFile = new File(folder + "/" + (worldName != null ? worldName + "/" : "") + name + ".png");
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

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int bgr = MapColor.getColorFromPackedId(MapColorPixels[x + y * width]); // Mojang returns BGR color
                bgr = modifier.modifyPixel(bgr);

                // Tint image like it's rendered in LightroomScreen or NegativeExposureScreen:
                // This is not the best place for it, but I haven't found better place.
                if (modifier == ExposurePixelModifiers.NEGATIVE_FILM) {
                    @Nullable FilmType filmType = FilmType.byName(properties.getString(ExposureSavedData.TYPE_PROPERTY));
                    if (filmType != null) {

                        int a = FastColor.ABGR32.alpha(bgr);
                        int b = FastColor.ABGR32.blue(bgr);
                        int g = FastColor.ABGR32.green(bgr);
                        int r = FastColor.ABGR32.red(bgr);

                        b = b * filmType.frameB / 255;
                        g = g * filmType.frameG / 255;
                        r = r * filmType.frameR / 255;

                        bgr = FastColor.ABGR32.color(a, b, g, r);
                    }
                }

                image.setPixelRGBA(x, y, bgr);
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
