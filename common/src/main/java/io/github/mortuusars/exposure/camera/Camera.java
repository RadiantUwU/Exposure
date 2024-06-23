package io.github.mortuusars.exposure.camera;

import com.google.common.base.Preconditions;
import io.github.mortuusars.exposure.camera.viewfinder.Viewfinder;
import io.github.mortuusars.exposure.item.CameraItem;
import io.github.mortuusars.exposure.util.ItemAndStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public abstract class Camera<T extends CameraItem> {
    private static final SortedMap<ResourceLocation, Function<Player, @Nullable Camera<?>>> CAMERA_GETTERS = new TreeMap<>();

    public static void registerCameraGetter(ResourceLocation id, Function<Player, @Nullable Camera<?>> cameraGetter) {
        Preconditions.checkState(!CAMERA_GETTERS.containsKey(id), "Camera getter with ID '{}' is already registered.", id);
        CAMERA_GETTERS.put(id, cameraGetter);
    }

    public static <T extends CameraItem> Optional<Camera<T>> getCamera(Player player, Class<T> clazz) {
        for (Function<Player, Camera<?>> getter : CAMERA_GETTERS.values()) {
            @Nullable Camera<?> camera = getter.apply(player);
            if (camera != null && camera.get().getItem().getClass().equals(clazz)) {
                @SuppressWarnings("unchecked")
                Camera<T> cameraOfType = (Camera<T>) camera;
                return Optional.of(cameraOfType);
            }
        }

        return Optional.empty();
    }

    public static Optional<Camera<?>> getCamera(Player player) {
        for (Function<Player, Camera<?>> getter : CAMERA_GETTERS.values()) {
            @Nullable Camera<?> camera = getter.apply(player);
            if (camera != null) {
                return Optional.of(camera);
            }
        }

        return Optional.empty();
    }

    public abstract void activate(Player player);
    public abstract void deactivate(Player player);
    public abstract ItemAndStack<T> get();

    public void clientTick() {

    }

    public Camera<T> apply(BiConsumer<T, ItemStack> function) {
        get().apply(function);
        return this;
    }

    public static void onLocalPlayerTick(Player player) {
        Preconditions.checkState(player.isLocalPlayer(), "{} is not a LocalPlayer.", player);
        boolean viewfinderOpen = Viewfinder.isOpen();

        getCamera(player).ifPresentOrElse(camera -> {
            if (!viewfinderOpen) {
                Viewfinder.open();
            } else {
                Viewfinder.update();
            }
            camera.clientTick();
        }, Viewfinder::close);
    }
}
