package io.github.mortuusars.exposure.camera;

import com.google.common.base.Preconditions;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.camera.viewfinder.Viewfinder;
import io.github.mortuusars.exposure.item.CameraItem;
import io.github.mortuusars.exposure.util.CameraInHand;
import io.github.mortuusars.exposure.util.ItemAndStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.*;
import java.util.function.Function;

public abstract class Camera<T extends CameraItem> {
    public static final Camera<CameraItem> EMPTY = new Camera<>() {
        @Override
        public boolean isActive() { return false; }
        @Override
        public void activate() { }
        @Override
        public void deactivate() { }
        @Override
        public ItemAndStack<CameraItem> getItem() { return null; }
    };

    private static final SortedMap<ResourceLocation, Function<Player, Camera<?>>> CAMERA_GETTERS = new TreeMap<>();

    static {
//        registerCameraGetter(Exposure.resource("camera_in_hand"), player -> CameraInHand);
    }

    public static void registerCameraGetter(ResourceLocation id, Function<Player, Camera<?>> cameraGetter) {
        CAMERA_GETTERS.put(id, cameraGetter);
    }

    public static boolean isUsingCamera(Player player) {
        return getCamera(player).isActive();
    }

    public static <T extends CameraItem> Optional<Camera<T>> getCamera(Player player, Class<T> clazz) {
        for (Function<Player, Camera<?>> getter : CAMERA_GETTERS.values()) {
            Camera<?> camera = getter.apply(player);
            if (!camera.equals(EMPTY) && camera.getItem().getItem().getClass().equals(clazz)) {
                @SuppressWarnings("unchecked")
                Camera<T> cameraOfType = (Camera<T>) camera;
                return Optional.of(cameraOfType);
            }
        }

        return Optional.empty();
    }

    public static Camera<?> getCamera(Player player) {
        for (Function<Player, Camera<?>> getter : CAMERA_GETTERS.values()) {
            Camera<?> camera = getter.apply(player);
            if (!camera.equals(EMPTY)) {
                return camera;
            }
        }

        return EMPTY;
    }

    public abstract boolean isActive();
    public abstract void activate();
    public abstract void deactivate();
    public abstract ItemAndStack<T> getItem();

    public boolean isEmpty() {
        return EMPTY.equals(this);
    }

    public void onLocalPlayerTick(Player player) {
        Preconditions.checkState(player.isLocalPlayer(), "{} is not a LocalPlayer.", player);
        boolean viewfinderOpen = Viewfinder.isOpen();

        if (isActive()) {
            if (!viewfinderOpen) {
                Viewfinder.open();
            }
            else {
                Viewfinder.update();
            }
        }
        else {
            Viewfinder.close();
        }
    }
}
