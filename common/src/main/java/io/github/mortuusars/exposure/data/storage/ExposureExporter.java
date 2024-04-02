package io.github.mortuusars.exposure.data.storage;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.exposure.camera.infrastructure.FilmType;
import io.github.mortuusars.exposure.data.ExposureSize;
import io.github.mortuusars.exposure.render.modifiers.ExposurePixelModifiers;
import io.github.mortuusars.exposure.render.modifiers.IPixelModifier;
import io.github.mortuusars.exposure.util.ColorUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
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
        BufferedImage image;

        try {
            image = convertToBufferedImage(mapColorPixels, width, height, properties);
        }
        catch (Exception e) {
            LogUtils.getLogger().error("Cannot convert exposure pixels to BufferedImage: " + e);
            return false;
        }

        return save(image, properties);
    }

    public boolean save(BufferedImage image, CompoundTag properties) {
        try {
            File outputFile = new File(folder + "/" + (worldName != null ? worldName + "/" : "") + name + ".png");
            // Existing file would be overwritten
            boolean ignored = outputFile.mkdirs();
            ImageIO.write(image, "png", outputFile);

            if (properties.contains(ExposureSavedData.TIMESTAMP_PROPERTY, CompoundTag.TAG_LONG)) {
                long unixSeconds = properties.getLong(ExposureSavedData.TIMESTAMP_PROPERTY);
                trySetFileCreationDate(outputFile.getAbsolutePath(), unixSeconds);
            }

            LogUtils.getLogger().info("Exposure saved: " + outputFile);
            return true;
        } catch (IOException e) {
            LogUtils.getLogger().error("Exposure file was not saved: " + e);
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
    protected BufferedImage convertToBufferedImage(byte[] MapColorPixels, int width, int height, CompoundTag properties) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int bgr = MapColor.getColorFromPackedId(MapColorPixels[x + y * width]); // Mojang returns BGR color
                bgr = modifier.modifyPixel(bgr);

                // Tint image like it's rendered in LightroomScreen or NegativeExposureScreen:
                // This is not the best place for it, but I haven't found better place.
                if (modifier == ExposurePixelModifiers.NEGATIVE_FILM) {
                    @Nullable FilmType filmType = FilmType.byName(properties.getString(ExposureSavedData.TYPE_PROPERTY));
                    if (filmType != null) {
                        int a = (bgr >> 24) & 0xFF;
                        int b = (bgr >> 16) & 0xFF;
                        int g = (bgr >> 8) & 0xFF;
                        int r = bgr & 0xFF;

                        b = b * filmType.frameB / 255;
                        g = g * filmType.frameG / 255;
                        r = r * filmType.frameR / 255;

                        bgr = a << 24 | b << 16 | g << 8 | r;
                    }
                }

                int rgb = ColorUtils.BGRtoRGB(bgr);

                image.setRGB(x, y, rgb);
            }
        }

        if (getSize() != ExposureSize.X1)
            image = resizeImage(image, getSize());

        return image;
    }

    protected BufferedImage resizeImage(BufferedImage sourceImage, ExposureSize size) {
        int targetWidth = sourceImage.getWidth() * size.getMultiplier();
        int targetHeight = sourceImage.getHeight() * size.getMultiplier();
        Image scaledInstance = sourceImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_FAST);
        BufferedImage outputImg = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        outputImg.getGraphics().drawImage(scaledInstance, 0, 0, null);
        return outputImg;
    }
}
