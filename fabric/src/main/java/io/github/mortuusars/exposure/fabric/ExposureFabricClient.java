package io.github.mortuusars.exposure.fabric;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.client.ComplicatedChromaticFinalizer;
import io.github.mortuusars.exposure.fabric.resources.ExposureFabricClientReloadListener;
import io.github.mortuusars.exposure.fabric.resources.FabricFiltersLoader;
import io.github.mortuusars.exposure.gui.component.PhotographTooltip;
import io.github.mortuusars.exposure.gui.screen.ItemRenameScreen;
import io.github.mortuusars.exposure.gui.screen.LightroomScreen;
import io.github.mortuusars.exposure.gui.screen.album.AlbumScreen;
import io.github.mortuusars.exposure.gui.screen.album.LecternAlbumScreen;
import io.github.mortuusars.exposure.gui.screen.camera.CameraAttachmentsScreen;
import io.github.mortuusars.exposure.network.fabric.PacketsImpl;
import io.github.mortuusars.exposure.render.PhotographEntityRenderer;
import io.github.mortuusars.exposure.render.PhotographFrameEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.server.packs.PackType;

public class ExposureFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ExposureClient.init();

        ExposureClient.registerKeymappings(KeyBindingHelper::registerKeyBinding);

        MenuScreens.register(Exposure.MenuTypes.CAMERA.get(), CameraAttachmentsScreen::new);
        MenuScreens.register(Exposure.MenuTypes.ALBUM.get(), AlbumScreen::new);
        MenuScreens.register(Exposure.MenuTypes.LECTERN_ALBUM.get(), LecternAlbumScreen::new);
        MenuScreens.register(Exposure.MenuTypes.LIGHTROOM.get(), LightroomScreen::new);
        MenuScreens.register(Exposure.MenuTypes.ITEM_RENAME.get(), ItemRenameScreen::new);

        ModelLoadingPlugin.register(pluginContext ->
                pluginContext.addModels(ExposureClient.Models.CAMERA_GUI,
                        ExposureClient.Models.PHOTOGRAPH_FRAME_SMALL,
                        ExposureClient.Models.PHOTOGRAPH_FRAME_SMALL_STRIPPED,
                        ExposureClient.Models.PHOTOGRAPH_FRAME_MEDIUM,
                        ExposureClient.Models.PHOTOGRAPH_FRAME_MEDIUM_STRIPPED,
                        ExposureClient.Models.PHOTOGRAPH_FRAME_LARGE,
                        ExposureClient.Models.PHOTOGRAPH_FRAME_LARGE_STRIPPED));

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new ExposureFabricClientReloadListener());
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new FabricFiltersLoader());

        EntityRendererRegistry.register(Exposure.EntityTypes.PHOTOGRAPH.get(), PhotographEntityRenderer::new);
        EntityRendererRegistry.register(Exposure.EntityTypes.PHOTOGRAPH_FRAME.get(), PhotographFrameEntityRenderer::new);

        TooltipComponentCallback.EVENT.register(data -> data instanceof PhotographTooltip photographTooltip ? photographTooltip : null);
        ClientTickEvents.END_CLIENT_TICK.register(client -> ComplicatedChromaticFinalizer.clientTick());

        PacketsImpl.registerS2CPackets();
    }
}
