package io.github.mortuusars.exposure.camera.viewfinder;


import com.google.common.base.Preconditions;
import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.camera.Camera;
import io.github.mortuusars.exposure.camera.CameraClient;
import io.github.mortuusars.exposure.camera.infrastructure.FocalRange;
import io.github.mortuusars.exposure.camera.infrastructure.ZoomDirection;
import io.github.mortuusars.exposure.item.CameraItem;
import io.github.mortuusars.exposure.util.Fov;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class Viewfinder {
    public static final float ZOOM_STEP = 8f;
    public static final float ZOOM_PRECISE_MODIFIER = 0.25f;
    private static boolean isOpen;

    private static FocalRange focalRange = new FocalRange(18, 55);
    private static double targetFov = 90f;
    private static double currentFov = targetFov;
    private static boolean shouldRestoreFov;

    public static boolean isOpen() {
        return isOpen;
    }

    public static boolean isLookingThrough() {
        return isOpen() && (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON
                || Minecraft.getInstance().options.getCameraType() == CameraType.THIRD_PERSON_FRONT);
    }

    public static void open() {
        LocalPlayer player = Minecraft.getInstance().player;
        Preconditions.checkState(player != null, "Player should not be null");
        Preconditions.checkState(player.level().isClientSide(), "This should be called only client-side.");

        if (isOpen())
            return;

        Camera<?> camera = Camera.getCamera(player).orElseThrow();
        CameraItem cameraItem = camera.get().getItem();
        ItemStack cameraStack = camera.get().getStack();

        focalRange = cameraItem.getFocalRange(cameraStack);
        targetFov = Fov.focalLengthToFov(Mth.clamp(cameraItem.getFocalLength(cameraStack), focalRange.min(), focalRange.max()));

        isOpen = true;

        ViewfinderShader.setPrevious(ViewfinderShader.getCurrent().orElse(null));
        ViewfinderShader.update();
        ViewfinderOverlay.setup();
    }

    public static void update() {
        @Nullable LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        updateSelfieMode();
        ViewfinderShader.update();
    }

    public static void updateSelfieMode() {
        boolean inSelfieMode = Minecraft.getInstance().options.getCameraType() == CameraType.THIRD_PERSON_FRONT;
        CameraClient.setSelfieMode(inSelfieMode);
    }

    public static void close() {
        if (!isOpen()) {
            return;
        }

        isOpen = false;
        targetFov = Minecraft.getInstance().options.fov().get();

        ViewfinderShader.removeShader();
        ViewfinderShader.restorePrevious();
    }

    public static FocalRange getFocalRange() {
        return focalRange;
    }

    public static double getCurrentFov() {
        return currentFov;
    }

    public static float getSelfieCameraDistance() {
        return 1.75f;
    }

    public static void zoom(ZoomDirection direction, boolean precise) {
        double step = ZOOM_STEP * (1f - Mth.clamp((focalRange.min() - currentFov) / focalRange.min(), 0.3f, 1f));
        double inertia = Math.abs(targetFov - currentFov) * 0.8f;
        double change = step + inertia;

        if (precise)
            change *= ZOOM_PRECISE_MODIFIER;

        double prevFov = targetFov;

        double fov = Mth.clamp(targetFov + (direction == ZoomDirection.IN ? -change : +change),
                Fov.focalLengthToFov(focalRange.max()),
                Fov.focalLengthToFov(focalRange.min()));

        if (Math.abs(prevFov - fov) > 0.01f)
            Objects.requireNonNull(Minecraft.getInstance().player).playSound(Exposure.SoundEvents.CAMERA_LENS_RING_CLICK.get());

        targetFov = fov;

        CameraClient.setZoom(Fov.fovToFocalLength(fov));
    }

    public static double modifyMouseSensitivity(double sensitivity) {
        if (!isLookingThrough())
            return sensitivity;

        double modifier = Mth.clamp(1f - (Config.Client.VIEWFINDER_ZOOM_SENSITIVITY_MODIFIER.get()
                * ((Minecraft.getInstance().options.fov().get() - currentFov) / 5f)), 0.01, 2f);
        return sensitivity * modifier;
    }

    public static boolean handleMouseScroll(ZoomDirection direction) {
        if (isLookingThrough()) {
            zoom(direction, false);
            return true;
        }

        return false;
    }

    public static double modifyFov(double fov) {
        if (isLookingThrough()) {
            currentFov = Mth.lerp(Math.min(0.6f * Minecraft.getInstance().getDeltaFrameTime(), 0.6f), currentFov, targetFov);
            shouldRestoreFov = true;
            return currentFov;
        }
        else if (shouldRestoreFov && Math.abs(currentFov - fov) > 0.00001) {
            currentFov = Mth.lerp(Math.min(0.8f * Minecraft.getInstance().getDeltaFrameTime(), 0.8f), currentFov, fov);
            return currentFov;
        } else {
            currentFov = fov;
            shouldRestoreFov = false;
            return fov;
        }
    }
}
