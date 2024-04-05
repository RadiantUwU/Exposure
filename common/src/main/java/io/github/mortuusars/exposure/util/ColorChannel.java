package io.github.mortuusars.exposure.util;

import io.github.mortuusars.exposure.Exposure;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public enum ColorChannel implements StringRepresentable {
    RED(0xFFD8523E),
    GREEN(0xFF7BC64B),
    BLUE(0xFF4E73CE);

    private final int color;

    ColorChannel(int color) {
        this.color = color;
    }

    public int getRepresentationColor() {
        return color;
    }

    public static Optional<ColorChannel> fromStack(ItemStack stack) {
        if (stack.is(Exposure.Tags.Items.RED_FILTERS))
            return Optional.of(RED);
        else if (stack.is(Exposure.Tags.Items.GREEN_FILTERS))
            return Optional.of(GREEN);
        else if (stack.is(Exposure.Tags.Items.BLUE_FILTERS))
            return Optional.of(BLUE);
        else
            return Optional.empty();
    }

    public static ColorChannel fromStringOrDefault(String serializedName, ColorChannel defaultValue) {
        for (ColorChannel value : values()) {
            if (value.getSerializedName().equals(serializedName))
                return value;
        }
        return defaultValue;
    }

    public static Optional<ColorChannel> fromString(String serializedName) {
        for (ColorChannel value : values()) {
            if (value.getSerializedName().equals(serializedName))
                return Optional.of(value);
        }
        return Optional.empty();
    }

    @Override
    public @NotNull String getSerializedName() {
        return toString().toLowerCase();
    }
}
