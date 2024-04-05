package io.github.mortuusars.exposure.data.storage;

public interface IClientsideExposureStorage extends IExposureStorage {
    /**
     * Patiently wait for exposure to change and do not go crazy until server notifies us that exposure is ready.
     * (Prevents log spam and so on).
     */
    void putOnWaitingList(String exposureId);
    void remove(String id);
}
