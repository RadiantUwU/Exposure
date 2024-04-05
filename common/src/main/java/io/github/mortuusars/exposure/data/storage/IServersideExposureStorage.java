package io.github.mortuusars.exposure.data.storage;

public interface IServersideExposureStorage extends IExposureStorage {
    void sendExposureChanged(String exposureId);
}
