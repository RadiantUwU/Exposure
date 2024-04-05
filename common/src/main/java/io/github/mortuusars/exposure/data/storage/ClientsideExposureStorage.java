package io.github.mortuusars.exposure.data.storage;

import io.github.mortuusars.exposure.network.Packets;
import io.github.mortuusars.exposure.network.packet.server.QueryExposureDataC2SP;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ClientsideExposureStorage implements IClientsideExposureStorage {
    private final Map<String, @NotNull ExposureSavedData> CACHE = new HashMap<>();
    private final List<String> QUERIED_IDS = new ArrayList<>();
    private final List<String> WAITING_IDS = new ArrayList<>();

    @Override
    public Optional<ExposureSavedData> getOrQuery(String id) {
        ExposureSavedData exposureData = CACHE.get(id);

        if(exposureData == null && !WAITING_IDS.contains(id) && !QUERIED_IDS.contains(id)) {
            Packets.sendToServer(new QueryExposureDataC2SP(id));
            QUERIED_IDS.add(id);
        }

        return Optional.ofNullable(exposureData);
    }

    @Override
    public void put(String id, ExposureSavedData data) {
        CACHE.put(id, data);
        QUERIED_IDS.remove(id);
        WAITING_IDS.remove(id);
    }

    @Override
    public void putOnWaitingList(String exposureId) {
        if (!WAITING_IDS.contains(exposureId))
            WAITING_IDS.add(exposureId);
    }

    @Override
    public List<String> getAllIds() {
        return CACHE.keySet().stream().toList();
    }

    @Override
    public void remove(String id) {
        CACHE.remove(id);
        QUERIED_IDS.remove(id);
        WAITING_IDS.remove(id);
    }

    @Override
    public void clear() {
        CACHE.clear();
        QUERIED_IDS.clear();
        WAITING_IDS.clear();
    }
}
