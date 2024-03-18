package io.github.mortuusars.exposure.util;

import io.github.mortuusars.exposure.Exposure;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public enum ColorChannel implements StringRepresentable {
    RED,
    GREEN,
    BLUE;

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

    @Override
    public @NotNull String getSerializedName() {
        return toString().toLowerCase();
    }
}
