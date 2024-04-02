package io.github.mortuusars.exposure.camera.capture.component;

import io.github.mortuusars.exposure.data.ExposureSize;
import io.github.mortuusars.exposure.data.storage.ExposureExporter;
import io.github.mortuusars.exposure.render.modifiers.IPixelModifier;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class ExposureExporterComponent extends ExposureExporter implements ICaptureComponent {
    public ExposureExporterComponent(String name) {
        super(name);
    }

    @Override
    public ExposureExporterComponent withFolder(String folder) {
        super.withFolder(folder);
        return this;
    }

    @Override
    public ExposureExporterComponent withDefaultFolder() {
        super.withDefaultFolder();
        return this;
    }

    @Override
    public ExposureExporterComponent organizeByWorld(@Nullable String worldName) {
        super.organizeByWorld(worldName);
        return this;
    }

    @Override
    public ExposureExporterComponent organizeByWorld(boolean organize, Supplier<@Nullable String> worldNameSupplier) {
        super.organizeByWorld(organize, worldNameSupplier);
        return this;
    }

    @Override
    public ExposureExporterComponent withModifier(IPixelModifier modifier) {
        super.withModifier(modifier);
        return this;
    }

    @Override
    public ExposureExporterComponent withSize(ExposureSize size) {
        super.withSize(size);
        return this;
    }
}
