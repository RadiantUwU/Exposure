package io.github.mortuusars.exposure.block.entity;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public class Lightroom {
    public enum Process implements StringRepresentable {
        REGULAR,
        CHROMATIC;

        public static Process fromStringOrDefault(String serializedName, Process defaultValue) {
            for (Process value : values()) {
                if (value.getSerializedName().equals(serializedName))
                    return value;
            }
            return defaultValue;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name().toLowerCase();
        }
    }

    public static final int SLOTS = 7;
    public static final int FILM_SLOT = 0;
    public static final int PAPER_SLOT = 1;
    public static final int CYAN_SLOT = 2;
    public static final int MAGENTA_SLOT = 3;
    public static final int YELLOW_SLOT = 4;
    public static final int BLACK_SLOT = 5;
    public static final int RESULT_SLOT = 6;

    public static final int[] ALL_SLOTS = new int[] { 0, 1, 2, 3, 4, 5, 6 };
    public static final int[] OUTPUT_SLOTS = new int[] { 6 };

    public static final int[] DYES_FOR_BW = new int[] { BLACK_SLOT };
    public static final int[] DYES_FOR_COLOR = new int[] { CYAN_SLOT, MAGENTA_SLOT, YELLOW_SLOT, BLACK_SLOT };
    public static final int[] DYES_FOR_CHROMATIC_RED = new int[] { MAGENTA_SLOT, YELLOW_SLOT };
    public static final int[] DYES_FOR_CHROMATIC_BLUE = new int[] { CYAN_SLOT, MAGENTA_SLOT };
    public static final int[] DYES_FOR_CHROMATIC_GREEN = new int[] { CYAN_SLOT, YELLOW_SLOT };
}
