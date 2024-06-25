package io.github.mortuusars.exposure;

import com.google.common.base.Preconditions;
import com.mojang.blaze3d.platform.InputConstants;
import io.github.mortuusars.exposure.data.storage.ClientsideExposureStorage;
import io.github.mortuusars.exposure.data.storage.IClientsideExposureStorage;
import io.github.mortuusars.exposure.data.transfer.ExposureReceiver;
import io.github.mortuusars.exposure.data.transfer.ExposureSender;
import io.github.mortuusars.exposure.data.transfer.IExposureReceiver;
import io.github.mortuusars.exposure.data.transfer.IExposureSender;
import io.github.mortuusars.exposure.item.*;
import io.github.mortuusars.exposure.network.Packets;
import io.github.mortuusars.exposure.render.ExposureRenderer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class ExposureClient {
    private static final IClientsideExposureStorage exposureStorage = new ClientsideExposureStorage();
    private static final ExposureRenderer exposureRenderer = new ExposureRenderer();

    private static IExposureSender exposureSender;
    private static IExposureReceiver exposureReceiver;

    @Nullable
    private static KeyMapping openCameraControlsKey = null;

    public static void init() {
        exposureSender = new ExposureSender((packet, player) -> Packets.sendToServer(packet), ExposureSender.TO_SERVER_PACKET_SPLIT_THRESHOLD);
        exposureReceiver = new ExposureReceiver(exposureStorage);

        registerItemModelProperties();
    }

    public static IClientsideExposureStorage getExposureStorage() {
        return exposureStorage;
    }

    public static IExposureSender getExposureSender() {
        return exposureSender;
    }

    public static IExposureReceiver getExposureReceiver() {
        return exposureReceiver;
    }

    public static ExposureRenderer getExposureRenderer() {
        return exposureRenderer;
    }

    public static void registerKeymappings(Function<KeyMapping, KeyMapping> registerFunction) {
        KeyMapping keyMapping = new KeyMapping("key.exposure.camera_controls",
                InputConstants.UNKNOWN.getValue(), "category.exposure");

        openCameraControlsKey = registerFunction.apply(keyMapping);
    }

    private static void registerItemModelProperties() {
        ItemProperties.register(Exposure.Items.CAMERA.get(), new ResourceLocation("camera_state"), CameraItemClientExtensions::itemPropertyFunction);
        ItemProperties.register(Exposure.Items.CHROMATIC_SHEET.get(), new ResourceLocation("channels"), (stack, clientLevel, livingEntity, seed) ->
                stack.getItem() instanceof ChromaticSheetItem chromaticSheet ?
                        chromaticSheet.getExposures(stack).size() / 10f : 0f);
        ItemProperties.register(Exposure.Items.STACKED_PHOTOGRAPHS.get(), new ResourceLocation("count"),
                (stack, clientLevel, livingEntity, seed) ->
                        stack.getItem() instanceof StackedPhotographsItem stackedPhotographsItem ?
                                stackedPhotographsItem.getPhotographsCount(stack) / 100f : 0f);
        ItemProperties.register(Exposure.Items.ALBUM.get(), new ResourceLocation("photos"),
                (stack, clientLevel, livingEntity, seed) ->
                        stack.getItem() instanceof AlbumItem albumItem ? albumItem.getPhotographsCount(stack) / 100f : 0f);
        ItemProperties.register(Exposure.Items.INTERPLANAR_PROJECTOR.get(), new ResourceLocation("active"),
                (stack, clientLevel, livingEntity, seed) -> stack.hasCustomHoverName() ? 1f : 0f);
    }

    public static void onScreenAdded(Screen screen) {

    }

    public static KeyMapping getCameraControlsKey() {
        Preconditions.checkState(openCameraControlsKey != null,
                "Viewfinder Controls key mapping was not registered");

        return openCameraControlsKey.isUnbound() ? Minecraft.getInstance().options.keyShift : openCameraControlsKey;
    }

    public static boolean isShaderActive() {
        return Minecraft.getInstance().gameRenderer.currentEffect() != null && Minecraft.getInstance().gameRenderer.effectActive;
    }

    public static class Models {
        public static final ModelResourceLocation CAMERA_GUI =
                new ModelResourceLocation(Exposure.ID, "camera_gui", "inventory");
        public static final ModelResourceLocation PHOTOGRAPH_FRAME_SMALL =
                new ModelResourceLocation(Exposure.ID, "photograph_frame_small", "");
        public static final ModelResourceLocation PHOTOGRAPH_FRAME_SMALL_STRIPPED =
                new ModelResourceLocation(Exposure.ID, "photograph_frame_small_stripped", "");
        public static final ModelResourceLocation PHOTOGRAPH_FRAME_MEDIUM =
                new ModelResourceLocation(Exposure.ID, "photograph_frame_medium", "");
        public static final ModelResourceLocation PHOTOGRAPH_FRAME_MEDIUM_STRIPPED =
                new ModelResourceLocation(Exposure.ID, "photograph_frame_medium_stripped", "");
        public static final ModelResourceLocation PHOTOGRAPH_FRAME_LARGE =
                new ModelResourceLocation(Exposure.ID, "photograph_frame_large", "");
        public static final ModelResourceLocation PHOTOGRAPH_FRAME_LARGE_STRIPPED =
                new ModelResourceLocation(Exposure.ID, "photograph_frame_large_stripped", "");
    }
}
