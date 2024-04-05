package io.github.mortuusars.exposure.data;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum ExposureSize implements StringRepresentable {
    X1(1),
    X2(2),
    X3(3),
    X4(4);

    private final int multiplier;

    ExposureSize(int multiplier) {
        this.multiplier = multiplier;
    }

    public int getMultiplier() {
        return multiplier;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name().toLowerCase();
    }

    public static @Nullable ExposureSize byName(String name) {
        for (ExposureSize value : values()) {
            if (value.getSerializedName().equals(name))
                return value;
        }
        return null;
    }
}
