package io.github.mortuusars.exposure.data.storage;

import io.github.mortuusars.exposure.data.ExposureSize;
import io.github.mortuusars.exposure.render.modifiers.ExposurePixelModifiers;
import io.github.mortuusars.exposure.render.modifiers.IPixelModifier;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Date;
import java.util.function.Supplier;

public abstract class ExposureExporter<T extends ExposureExporter<?>> {
    private final String name;
    private String folder = "exposures";
    @Nullable
    private String worldName = null;
    private IPixelModifier modifier = ExposurePixelModifiers.EMPTY;
    private ExposureSize size = ExposureSize.X1;

    public ExposureExporter(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public String getFolder() { return folder; }
    public @Nullable String getWorldSubfolder() { return worldName; }
    public IPixelModifier getModifier() { return modifier; }
    public ExposureSize getSize() { return size; }

    @SuppressWarnings("unchecked")
    public T withFolder(String folder) {
        this.folder = folder;
        return (T)this;
    }

    @SuppressWarnings("unchecked")
    public T withDefaultFolder() {
        this.folder = "exposures";
        return (T)this;
    }

    @SuppressWarnings("unchecked")
    public T organizeByWorld(@Nullable String worldName) {
        this.worldName = worldName;
        return (T)this;
    }

    @SuppressWarnings("unchecked")
    public T organizeByWorld(boolean organize, Supplier<@Nullable String> worldNameSupplier) {
        this.worldName = organize ? worldNameSupplier.get() : null;
        return (T)this;
    }

    @SuppressWarnings("unchecked")
    public T withModifier(IPixelModifier modifier) {
        this.modifier = modifier;
        return (T)this;
    }

    @SuppressWarnings("unchecked")
    public T withSize(ExposureSize size) {
        this.size = size;
        return (T)this;
    }

    public boolean save(ExposureSavedData data) {
        return save(data.getPixels(), data.getWidth(), data.getHeight(), data.getProperties());
    }

    public abstract boolean save(byte[] mapColorPixels, int width, int height, CompoundTag properties);

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
}
