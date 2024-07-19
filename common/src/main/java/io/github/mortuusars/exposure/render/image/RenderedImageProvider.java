package io.github.mortuusars.exposure.render.image;

import com.mojang.datafixers.util.Either;
import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.camera.infrastructure.FrameData;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class RenderedImageProvider {
    public static final RenderedImageProvider EMPTY = new RenderedImageProvider(new EmptyImage());
    public static final RenderedImageProvider HIDDEN = new RenderedImageProvider(TextureImage.getTexture(Exposure.resource("textures/exposure/pack.png")));

    protected final IImage image;

    public RenderedImageProvider(IImage image) {
        this.image = image;
    }

    public static RenderedImageProvider fromFrame(CompoundTag frame) {
        if (Config.Client.HIDE_PHOJECTED_PHOTOGRAPHS_MADE_BY_OTHERS.get() && frame.getBoolean(FrameData.PROJECTED)) {
            if (Minecraft.getInstance().player != null
                    && frame.contains(FrameData.PHOTOGRAPHER_ID)
                    && !frame.getUUID(FrameData.PHOTOGRAPHER_ID).equals(Minecraft.getInstance().player.getUUID())) {
                return HIDDEN;
            }
        }
        else if (Config.Client.HIDE_ALL_PHOTOGRAPHS_MADE_BY_OTHERS.get()) {
            if (Minecraft.getInstance().player != null
                    && frame.contains(FrameData.PHOTOGRAPHER_ID)
                    && !frame.getUUID(FrameData.PHOTOGRAPHER_ID).equals(Minecraft.getInstance().player.getUUID())) {
                return HIDDEN;
            }
        }

        Either<String, ResourceLocation> idOrTexture = FrameData.getIdOrTexture(frame);
        @Nullable IImage image = idOrTexture.map(
                id -> ExposureClient.getExposureStorage().getOrQuery(id)
                        .map(data -> new ExposureDataImage(id, data))
                        .orElse(null),
                TextureImage::getTexture);

        if (image != null) {
            return new RenderedImageProvider(image);
        }

        return EMPTY;
    }

    public IImage get() {
        return image;
    }

    public String getInstanceId() {
        return get().getImageId();
    }
}
