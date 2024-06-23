package io.github.mortuusars.exposure.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.camera.Camera;
import io.github.mortuusars.exposure.camera.CameraClient;
import io.github.mortuusars.exposure.camera.infrastructure.ZoomDirection;
import io.github.mortuusars.exposure.camera.viewfinder.Viewfinder;
import io.github.mortuusars.exposure.gui.ClientGUI;
import io.github.mortuusars.exposure.gui.screen.camera.ViewfinderControlsScreen;
import io.github.mortuusars.exposure.util.CameraInHand;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class KeyboardHandler {
    public static boolean handleViewfinderKeyPress(long windowId, int key, int scanCode, int action, int modifiers) {
        Minecraft minecraft = Minecraft.getInstance();
        @Nullable LocalPlayer player = minecraft.player;
        if (player == null) {
            return false;
        }

        Optional<Camera<?>> cameraOptional = CameraClient.getCamera();
        if (cameraOptional.isEmpty()) {
            return false;
        }

        if (!Config.Common.CAMERA_VIEWFINDER_ATTACK.get()
                && Minecraft.getInstance().options.keyAttack.matches(key, scanCode)
                && !(Minecraft.getInstance().screen instanceof ViewfinderControlsScreen)) {
            return true; // Block attacks
        }

        if (minecraft.options.keyTogglePerspective.matches(key, scanCode)) {
            if (action == InputConstants.PRESS)
                return true;

            CameraType currentCameraType = minecraft.options.getCameraType();
            CameraType newCameraType = currentCameraType == CameraType.FIRST_PERSON ? CameraType.THIRD_PERSON_FRONT
                    : CameraType.FIRST_PERSON;

            minecraft.options.setCameraType(newCameraType);
            return true;
        }


        if (key == InputConstants.KEY_ESCAPE || minecraft.options.keyInventory.matches(key, scanCode)) {
            if (action == InputConstants.PRESS) { // TODO: Check if activating on release is not causing problems
                if (minecraft.screen instanceof ViewfinderControlsScreen viewfinderControlsScreen) {
                    viewfinderControlsScreen.onClose();
                } else {
                    CameraClient.deactivate(player);
                }
            }
            return true;
        }

        if (!Viewfinder.isLookingThrough())
            return false;

        if (!(minecraft.screen instanceof ViewfinderControlsScreen)) {
            if (ExposureClient.getCameraControlsKey().matches(key, scanCode)) {
                ClientGUI.openViewfinderControlsScreen();
                return false; // Do not handle to keep sneaking
            }

            if (action == 1 || action == 2) { // Press or Hold
                if (key == InputConstants.KEY_ADD || key == InputConstants.KEY_EQUALS) {
                    Viewfinder.zoom(ZoomDirection.IN, false);
                    return true;
                }

                if (key == 333 /*KEY_SUBTRACT*/ || key == InputConstants.KEY_MINUS) {
                    Viewfinder.zoom(ZoomDirection.OUT, false);
                    return true;
                }
            }
        }

        return false;
    }
}
