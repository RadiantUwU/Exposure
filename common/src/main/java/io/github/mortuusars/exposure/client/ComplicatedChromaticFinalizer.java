package io.github.mortuusars.exposure.client;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.camera.capture.component.ExposureStorageSaveComponent;
import io.github.mortuusars.exposure.camera.capture.converter.DitheringColorConverter;
import io.github.mortuusars.exposure.camera.infrastructure.FilmType;
import io.github.mortuusars.exposure.camera.infrastructure.FrameData;
import io.github.mortuusars.exposure.data.storage.ExposureSavedData;
import io.github.mortuusars.exposure.render.image.ExposureDataImage;
import io.github.mortuusars.exposure.render.image.IImage;
import io.github.mortuusars.exposure.render.image.TextureImage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.Queue;

public class ComplicatedChromaticFinalizer {
    private static final Queue<ChromaticData> processingQueue = new LinkedList<>();

    public static void finalizeChromatic(CompoundTag red, CompoundTag green, CompoundTag blue, String exposureId) {
        processingQueue.add(new ChromaticData(red, green, blue, exposureId, 200));
    }

    public static void clientTick() {
        @Nullable ChromaticData item = processingQueue.peek();
        if (item == null) return;

        item.tick();

        IImage[] images = item.getImages();
        if (images.length >= 3) {
            processChromatic(images[0], images[1], images[2], item.exposureId);
            processingQueue.remove();
        }
        else if (item.attempts < 0) {
            ChromaticData removedItem = processingQueue.remove();
            for (IImage image : removedItem.images) {
                image.close();
            }
            Exposure.LOGGER.error("Cannot finalize a chromatic {}. Couldn't get images in time. {}, {}, {}",
                    removedItem.exposureId, removedItem.red, removedItem.green, removedItem.blue);
        }
    }

    private static void processChromatic(IImage red, IImage green, IImage blue, String exposureId) {
        int width = Math.min(red.getWidth(), Math.min(green.getWidth(), blue.getWidth()));
        int height = Math.min(red.getHeight(), Math.min(green.getHeight(), blue.getHeight()));
        if (width <= 0 ||height <= 0) {
            Exposure.LOGGER.error("Cannot create Chromatic Photograph: Width and Height should be larger than 0. " +
                    "Width '{}', Height: '{}'.", width, height);
            return;
        }

        byte[] mapColorPixels;

        try (NativeImage image = new NativeImage(width, height, false)) {
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    int a = FastColor.ABGR32.alpha(red.getPixelABGR(x, y));
                    int b = FastColor.ABGR32.blue(blue.getPixelABGR(x, y));
                    int g = FastColor.ABGR32.green(green.getPixelABGR(x, y));
                    int r = FastColor.ABGR32.red(red.getPixelABGR(x, y));

                    int abgr = FastColor.ABGR32.color(a, b, g, r);

                    image.setPixelRGBA(x, y, abgr);
                }
            }

            mapColorPixels = new DitheringColorConverter().convert(image);
        }

        CompoundTag properties = new CompoundTag();
        properties.putString(ExposureSavedData.TYPE_PROPERTY, FilmType.COLOR.getSerializedName());
        long unixTime = System.currentTimeMillis() / 1000L;
        properties.putLong(ExposureSavedData.TIMESTAMP_PROPERTY, unixTime);

        ExposureSavedData resultData = new ExposureSavedData(width, height, mapColorPixels, properties);

        ExposureStorageSaveComponent saveComponent = new ExposureStorageSaveComponent(exposureId, true);
        saveComponent.save(resultData.getPixels(), resultData.getWidth(), resultData.getHeight(), properties);

    }

    private static class ChromaticData {
        private final CompoundTag red, green, blue;
        private final String exposureId;
        private final IImage[] images = new IImage[3];
        private int attempts;

        public ChromaticData(CompoundTag red, CompoundTag green, CompoundTag blue, String exposureId, int attempts) {
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.exposureId = exposureId;
            this.attempts = attempts;
        }

        public void tick() {
            if (images[0] == null) {
                images[0] = tryGetData(red);
            }

            if (images[1] == null) {
                images[1] = tryGetData(green);
            }

            if (images[2] == null) {
                images[2] = tryGetData(blue);
            }

            attempts--;
        }

        private @Nullable IImage tryGetData(CompoundTag frame) {
            return FrameData.getIdOrTexture(frame)
                    .map(id ->  ExposureClient.getExposureStorage().getOrQuery(id)
                                    .map(data -> new ExposureDataImage(id, data))
                                    .orElse(null),
                            TextureImage::new);
        }

        public IImage[] getImages() {
            return images;
        }
    }
}
