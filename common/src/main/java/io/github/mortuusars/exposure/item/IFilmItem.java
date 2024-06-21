package io.github.mortuusars.exposure.item;

import io.github.mortuusars.exposure.ExposureConstants;
import io.github.mortuusars.exposure.camera.infrastructure.FilmType;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public interface IFilmItem {
    String FRAME_COUNT_TAG = "FrameCount";
    String FRAME_SIZE_TAG = "FrameSize";
    String FRAMES_TAG = "Frames";

    FilmType getType();

    default int getDefaultMaxFrameCount(ItemStack filmStack) {
        return 16;
    }

    default int getMaxFrameCount(ItemStack filmStack) {
        if (filmStack.getTag() != null && filmStack.getTag().contains(FRAME_COUNT_TAG, Tag.TAG_INT))
            return filmStack.getTag().getInt(FRAME_COUNT_TAG);
        else
            return getDefaultMaxFrameCount(filmStack);
    }

    default int getDefaultFrameSize() {
        return ExposureConstants.DEFAULT_FRAME_SIZE;
    }

    default int getFrameSize(ItemStack filmStack) {
        if (filmStack.getTag() != null && filmStack.getTag().contains(FRAME_SIZE_TAG, Tag.TAG_INT))
            return Mth.clamp(filmStack.getOrCreateTag().getInt(FRAME_SIZE_TAG), 1, 2048);
        else
            return getDefaultFrameSize();
    }

    default boolean hasExposedFrame(ItemStack filmStack, int index) {
        if (index < 0 || filmStack.getTag() == null || !filmStack.getTag().contains(FRAMES_TAG, Tag.TAG_LIST))
            return false;

        ListTag list = filmStack.getTag().getList(FRAMES_TAG, Tag.TAG_COMPOUND);
        return index < list.size();
    }

    default int getExposedFramesCount(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().contains(FRAMES_TAG, Tag.TAG_LIST) ?
                stack.getTag().getList(FRAMES_TAG, Tag.TAG_COMPOUND).size() : 0;
    }

    default ListTag getExposedFrames(ItemStack filmStack) {
        return filmStack.getTag() != null ? filmStack.getTag().getList(FRAMES_TAG, Tag.TAG_COMPOUND) : new ListTag();
    }
}
