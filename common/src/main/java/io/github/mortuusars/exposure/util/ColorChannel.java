package io.github.mortuusars.exposure.util;

import io.github.mortuusars.exposure.Exposure;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public enum ColorChannel {
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
}
