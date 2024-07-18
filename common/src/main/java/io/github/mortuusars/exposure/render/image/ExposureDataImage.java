package io.github.mortuusars.exposure.render.image;

import io.github.mortuusars.exposure.data.storage.ExposureSavedData;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;

public class ExposureDataImage implements IImage {
    private final String name;
    private final ExposureSavedData exposureData;

    public ExposureDataImage(String name, @NotNull ExposureSavedData exposureData) {
        this.name = name;
        this.exposureData = exposureData;
    }

    @Override
    public String getName() {
        return name;
    }

    public int getWidth() {
        return exposureData.getWidth();
    }

    public int getHeight() {
        return exposureData.getHeight();
    }

    public int getPixelABGR(int x, int y) {
        return MapColor.getColorFromPackedId(exposureData.getPixel(x, y));
    }
}
