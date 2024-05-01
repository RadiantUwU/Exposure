package io.github.mortuusars.exposure.camera;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.sound.OnePerPlayerSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class AttachmentSound {
    public static final AttachmentSound NONE = new AttachmentSound(null, 0f, 0f, null, 0f, 0f);
    public static final AttachmentSound FILM = new AttachmentSound(Exposure.SoundEvents.FILM_ADVANCING, 0.9f, 1f,
            Exposure.SoundEvents.FILM_REMOVED, 0.7f, 1f);
    public static final AttachmentSound FLASH = new AttachmentSound(Exposure.SoundEvents.CAMERA_GENERIC_CLICK, 0.6f, 1.15f,
            Exposure.SoundEvents.CAMERA_GENERIC_CLICK, 0.35f, 0.95f);
    public static final AttachmentSound LENS = new AttachmentSound(Exposure.SoundEvents.LENS_INSERT, 1f, 1f,
            Exposure.SoundEvents.LENS_REMOVE, 1f, 1f);
    public static final AttachmentSound FILTER = new AttachmentSound(Exposure.SoundEvents.FILTER_INSERT, 0.8f, 1f,
            Exposure.SoundEvents.FILTER_REMOVE, 0.5f, 1f);

    @Nullable
    private final Supplier<SoundEvent> inserted;
    private final float insertedVolume;
    private final float insertedPitch;
    @Nullable
    private final Supplier<SoundEvent> removed;
    private final float removedVolume;
    private final float removedPitch;

    public AttachmentSound(@Nullable Supplier<SoundEvent> inserted, float insertedVolume, float insertedPitch, @Nullable Supplier<SoundEvent> removed, float removedVolume, float removedPitch) {
        this.inserted = inserted;
        this.insertedVolume = insertedVolume;
        this.insertedPitch = insertedPitch;
        this.removed = removed;
        this.removedVolume = removedVolume;
        this.removedPitch = removedPitch;
    }

    public AttachmentSound(@Nullable Supplier<SoundEvent> inserted, float insertedVolume, float insertedPitch) {
        this.inserted = inserted;
        this.insertedVolume = insertedVolume;
        this.insertedPitch = insertedPitch;
        this.removed = null;
        this.removedVolume = 1f;
        this.removedPitch = 1f;
    }

    public void playOnePerPlayer(Player player, boolean isRemoved) {
        @Nullable Supplier<SoundEvent> sound = isRemoved ? removed : inserted;
        if (sound != null)
            OnePerPlayerSounds.play(player, sound.get(), SoundSource.PLAYERS,
                    isRemoved ? removedVolume : insertedVolume, isRemoved ? removedPitch : insertedPitch);
    }

    public @Nullable Supplier<SoundEvent> getInserted() {
        return inserted;
    }

    public float getInsertedVolume() {
        return insertedVolume;
    }

    public float getInsertedPitch() {
        return insertedPitch;
    }

    public @Nullable Supplier<SoundEvent> getRemoved() {
        return removed;
    }

    public float getRemovedVolume() {
        return removedVolume;
    }

    public float getRemovedPitch() {
        return removedPitch;
    }
}
