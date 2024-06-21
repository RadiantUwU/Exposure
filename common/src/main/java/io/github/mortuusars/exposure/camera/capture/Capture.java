package io.github.mortuusars.exposure.camera.capture;

import com.google.common.base.Preconditions;
import com.mojang.blaze3d.platform.NativeImage;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.ExposureConstants;
import io.github.mortuusars.exposure.camera.capture.component.ICaptureComponent;
import io.github.mortuusars.exposure.camera.capture.converter.IImageToMapColorsConverter;
import io.github.mortuusars.exposure.camera.capture.converter.SimpleColorConverter;
import io.github.mortuusars.exposure.camera.infrastructure.FilmType;
import io.github.mortuusars.exposure.data.storage.ExposureSavedData;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public abstract class Capture {
    protected FilmType type = FilmType.COLOR;
    protected int size = 320;
    protected float cropFactor = Exposure.CROP_FACTOR;
    protected float brightnessStops = 0f;
    protected boolean asyncCapturing = false;
    protected boolean asyncProcessing = true;
    protected Collection<ICaptureComponent> components = Collections.emptyList();
    protected IImageToMapColorsConverter converter = new SimpleColorConverter();
    protected Runnable onImageCaptured = () -> {};
    protected Runnable onCapturingFailed = () -> {};

    protected int ticksDelay = -1;
    protected int framesDelay = -1;
    protected long captureTick;
    protected long currentTick;
    protected boolean isCapturing = false;
    protected boolean done = false;

    public abstract @Nullable NativeImage captureImage();

    public boolean isDone() {
        return done;
    }

    public int getTicksDelay() {
        return (int) (captureTick - Objects.requireNonNull(Minecraft.getInstance().level).getGameTime());
    }

    public int getFramesDelay() {
        return framesDelay;
    }

    public FilmType getFilmType() {
        return type;
    }

    public Capture setFilmType(FilmType type) {
        this.type = type;
        return this;
    }

    public int getSize() {
        return size;
    }

    public Capture setSize(int size) {
        Preconditions.checkArgument(size > 0, "'size [{}] is not valid' should be larger than 0.", size);
        this.size = size;
        return this;
    }

    public float getCropFactor() {
        return cropFactor;
    }

    public Capture cropFactor(float cropFactor) {
        Preconditions.checkArgument(cropFactor != 0, "'cropFactor [{}] is not valid' should be larger than 0.", cropFactor);
        this.cropFactor = cropFactor;
        return this;
    }

    public float getBrightnessStops() {
        return brightnessStops;
    }

    public Capture setBrightnessStops(float brightnessStops) {
        this.brightnessStops = brightnessStops;
        return this;
    }

    public Capture setAsyncCapturing(boolean asyncCapturing) {
        this.asyncCapturing = asyncCapturing;
        return this;
    }

    public Capture setAsyncProcessing(boolean asyncProcessing) {
        this.asyncProcessing = asyncProcessing;
        return this;
    }

    public Capture addComponents(ICaptureComponent... components) {
        this.components = List.of(components);
        return this;
    }

    public Capture addComponents(Collection<ICaptureComponent> components) {
        this.components = components;
        return this;
    }

    public Capture setConverter(IImageToMapColorsConverter converter) {
        this.converter = converter;
        return this;
    }

    public Capture ifCapturingFailed(Runnable runnable) {
        this.onCapturingFailed = runnable;
        return this;
    }

    public Capture whenImageCaptured(Runnable runnable) {
        this.onImageCaptured = runnable;
        return this;
    }

    public void initialize() {
        for (ICaptureComponent modifier : components) {
            ticksDelay = Math.max(ticksDelay, modifier.getTicksDelay(this));
            framesDelay = Math.max(framesDelay, modifier.getFramesDelay(this));
        }

        for (ICaptureComponent modifier : components) {
            modifier.initialize(this);
        }

        currentTick = Objects.requireNonNull(Minecraft.getInstance().level).getGameTime();
        captureTick = currentTick + ticksDelay;

        if (currentTick == captureTick && framesDelay <= 0) {
            for (ICaptureComponent modifier : components) {
                modifier.onDelayTick(this, 0);
                modifier.onDelayFrame(this, 0);
            }
        }
    }

    public synchronized void tick() {
        if (delayTick() || isCapturing) {
            return;
        }

        isCapturing = true;

        if (asyncCapturing) {
            CompletableFuture.supplyAsync(this::captureImage)
                    .thenAccept(this::onImageCaptured);
        }
        else {
            @Nullable NativeImage image = captureImage();
            onImageCaptured(image);
        }
    }

    protected boolean delayTick() {
        long lastTick = currentTick;
        currentTick = Objects.requireNonNull(Minecraft.getInstance().level).getGameTime();

        if (ticksDelay > 0) {
            if (lastTick < currentTick) {
                ticksDelay--;

                for (ICaptureComponent modifier : components) {
                    modifier.onDelayTick(this, ticksDelay);
                }

                if (ticksDelay == 0 && framesDelay == 0) {
                    for (ICaptureComponent modifier : components) {
                        modifier.onDelayFrame(this, 0);
                    }
                }
            }

            return true;
        }

        if (framesDelay > 0) {
            framesDelay--;

            for (ICaptureComponent modifier : components) {
                modifier.onDelayFrame(this, framesDelay);
            }

            return true;
        }
        return false;
    }

    protected void onImageCaptured(@Nullable NativeImage image) {
        if (image == null) {
            done = true;
//            isCapturing = false;
            onCapturingFailed.run();
            return;
        }

        onImageCaptured.run();

        if (asyncCapturing) {
            Minecraft.getInstance().execute(() -> {
                for (ICaptureComponent modifier : components) {
                    modifier.imageTaken(this, image);
                }
            });
        }
        else {
            for (ICaptureComponent modifier : components) {
                modifier.imageTaken(this, image);
            }
        }

        if (asyncProcessing && !asyncCapturing) { // It's already async when asyncCapturing
            CompletableFuture.runAsync((() -> processImage(image)));
        }
        else {
            processImage(image);
        }

        done = true;
        isCapturing = false;
    }

    public void processImage(@NotNull NativeImage image) {
        try (@Nullable NativeImage processedImage = scaleCropAndProcess(image)) {
            if (processedImage == null) {
                return;
            }

            NativeImage modifiedImage = processedImage;

            for (ICaptureComponent component : components) {
                modifiedImage = component.modifyImage(this, modifiedImage);
            }

            byte[] pixels = converter.convert(this, modifiedImage);

            for (ICaptureComponent component : components) {
                component.teardown(this);
            }

            CompoundTag properties = new CompoundTag();
            properties.putString(ExposureSavedData.TYPE_PROPERTY, getFilmType().getSerializedName());

            for (ICaptureComponent component : components) {
                component.save(pixels, modifiedImage.getWidth(), modifiedImage.getHeight(), properties);
            }
        } catch (Exception e) {
            Exposure.LOGGER.error(e.toString());
        } finally {
            try {
                for (ICaptureComponent component : components) {
                    component.end(this);
                }
            } catch (Exception e) {
                Exposure.LOGGER.error(e.toString());
            }

            isCapturing = false;
        }
    }

    protected @Nullable NativeImage scaleCropAndProcess(@NotNull NativeImage sourceImage) {
        int sWidth = sourceImage.getWidth();
        int sHeight = sourceImage.getHeight();

        int sourceSize = Math.min(sWidth, sHeight);
        float crop = sourceSize - (sourceSize / getCropFactor());
        sourceSize -= (int) crop;

        int sourceXStart = sWidth > sHeight ? (sWidth - sHeight) / 2 : 0;
        int sourceYStart = sHeight > sWidth ? (sHeight - sWidth) / 2 : 0;

        sourceXStart += (int) (crop / 2);
        sourceYStart += (int) (crop / 2);

        int size = getSize();

        try (sourceImage) {
            return resizeWithModification(sourceImage, sourceXStart, sourceYStart, sourceSize, sourceSize, size, size);
        } catch (Exception e) {
            Exposure.LOGGER.error("Failed to process an image: {}", e.toString());
        }

        return null;
    }


    /**
     * Resizes and applies component pixel modifications to every pixel.
     * Use NativeImage#resizeSubRectTo would be nicer, but I haven't found a way to make it use Nearest Neighbor interpolation.
     */
    public NativeImage resizeWithModification(NativeImage source, int sourceX, int sourceY, int sourceWidth, int sourceHeight,
                                              int resultWidth, int resultHeight) {
        NativeImage result = new NativeImage(source.format(), resultWidth, resultHeight, false);

        for (int x = 0; x < resultWidth; x++) {
            float ratioX = x / (float)resultWidth;
            int sourcePosX = (int)(sourceX + (sourceWidth * ratioX));

            for (int y = 0; y < resultHeight; y++) {
                float ratioY = y / (float)resultHeight;
                int sourcePosY = (int)(sourceY + (sourceHeight * ratioY));

                int color = source.getPixelRGBA(sourcePosX, sourcePosY);

                for (ICaptureComponent component : components) {
                    color = component.modifyPixel(this, color);
                }

                result.setPixelRGBA(x, y, color);
            }
        }

        return result;
    }
}
