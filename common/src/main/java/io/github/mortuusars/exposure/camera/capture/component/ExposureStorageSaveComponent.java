package io.github.mortuusars.exposure.camera.capture.component;

import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.data.storage.ExposureSavedData;
import net.minecraft.nbt.CompoundTag;

public class ExposureStorageSaveComponent implements ICaptureComponent {
    private final String exposureId;
    private final boolean sendToServer;

    public ExposureStorageSaveComponent(String exposureId, boolean sendToServer) {
        this.exposureId = exposureId;
        this.sendToServer = sendToServer;
    }

    @Override
    public boolean save(byte[] pixels, int width, int height, CompoundTag properties) {
        long unixTime = System.currentTimeMillis() / 1000L;
        properties.putLong(ExposureSavedData.TIMESTAMP_PROPERTY, unixTime);

        ExposureSavedData exposureSavedData = new ExposureSavedData(width, height, pixels, properties);

        ExposureClient.getExposureStorage().put(exposureId, exposureSavedData);
        if (sendToServer)
            ExposureClient.getExposureSender().send(exposureId, exposureSavedData);

        return true;
    }
}
