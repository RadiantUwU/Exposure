package io.github.mortuusars.exposure.network.handler;

import com.google.common.base.Preconditions;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.camera.capture.Capture;
import io.github.mortuusars.exposure.camera.capture.CaptureManager;
import io.github.mortuusars.exposure.camera.capture.CapturedFramesHistory;
import io.github.mortuusars.exposure.camera.capture.component.BaseComponent;
import io.github.mortuusars.exposure.camera.capture.component.ExposureExporterComponent;
import io.github.mortuusars.exposure.camera.capture.component.ExposureStorageSaveComponent;
import io.github.mortuusars.exposure.camera.capture.component.ICaptureComponent;
import io.github.mortuusars.exposure.camera.capture.converter.DitheringColorConverter;
import io.github.mortuusars.exposure.camera.capture.converter.SimpleColorConverter;
import io.github.mortuusars.exposure.camera.infrastructure.FrameData;
import io.github.mortuusars.exposure.data.Lenses;
import io.github.mortuusars.exposure.data.ExposureSize;
import io.github.mortuusars.exposure.gui.screen.NegativeExposureScreen;
import io.github.mortuusars.exposure.gui.screen.PhotographScreen;
import io.github.mortuusars.exposure.item.CameraItem;
import io.github.mortuusars.exposure.item.PhotographItem;
import io.github.mortuusars.exposure.network.packet.client.*;
import io.github.mortuusars.exposure.render.modifiers.ExposurePixelModifiers;
import io.github.mortuusars.exposure.util.ClientsideWorldNameGetter;
import io.github.mortuusars.exposure.util.ColorUtils;
import io.github.mortuusars.exposure.util.ItemAndStack;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ClientPacketsHandler {
    public static void applyShader(ApplyShaderS2CP packet) {
        executeOnMainThread(() -> {
            if (packet.shaderLocation().getPath().equals("none")) {
                Minecraft.getInstance().gameRenderer.shutdownEffect();
            } else {
                Minecraft.getInstance().gameRenderer.loadEffect(packet.shaderLocation());
            }
        });
    }

    public static void exposeScreenshot(int size) {
        Preconditions.checkState(size > 0, size + " size is invalid. Should be larger than 0.");
        if (size == Integer.MAX_VALUE)
            size = Math.min(Minecraft.getInstance().getWindow().getWidth(), Minecraft.getInstance().getWindow()
                    .getHeight());

        int finalSize = size;
        executeOnMainThread(() -> {
            String filename = Util.getFilenameFormattedDateTime();
            CompoundTag frameData = new CompoundTag();
            frameData.putString(FrameData.ID, filename);
            Capture capture = new Capture()
                    .setSize(finalSize)
                    .cropFactor(1f)
                    .addComponents(
                            new BaseComponent(true),
                            new ExposureExporterComponent(filename)
                                    .organizeByWorld(Config.Client.EXPOSURE_SAVING_LEVEL_SUBFOLDER.get(),
                                            ClientsideWorldNameGetter::getWorldName)
                                    .withModifier(ExposurePixelModifiers.EMPTY)
                                    .withSize(ExposureSize.X1),
                            new ICaptureComponent() {
                                @Override
                                public void end(Capture capture) {
                                    LogUtils.getLogger().info("Saved exposure screenshot: " + filename);
                                }
                            })
                    .setConverter(new DitheringColorConverter());
            CaptureManager.enqueue(capture);
        });
    }

    public static void loadExposure(String exposureId, String path, int size, boolean dither) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (StringUtil.isNullOrEmpty(exposureId)) {
            if (player == null)
                throw new IllegalStateException("Cannot load exposure: path is null or empty and player is null.");
            exposureId = player.getName().getString() + player.getLevel().getGameTime();
        }

        String finalExposureId = exposureId;
        new Thread(() -> {
            try {
                BufferedImage read = ImageIO.read(new File(path));

                NativeImage image = new NativeImage(read.getWidth(), read.getHeight(), false);

                for (int x = 0; x < read.getWidth(); x++) {
                    for (int y = 0; y < read.getHeight(); y++) {
                        image.setPixelRGBA(x, y, ColorUtils.BGRtoRGB(read.getRGB(x, y)));
                    }
                }

                Capture capture = new Capture()
                        .setSize(size)
                        .cropFactor(1f)
                        .addComponents(new ExposureStorageSaveComponent(finalExposureId, true))
                        .setConverter(dither ? new DitheringColorConverter() : new SimpleColorConverter());
                capture.processImage(image);

                CompoundTag frameData = new CompoundTag();
                frameData.putString(FrameData.ID, finalExposureId);

                CapturedFramesHistory.add(frameData);

                LogUtils.getLogger()
                        .info("Loaded exposure from file '" + path + "' with Id: '" + finalExposureId + "'.");
                Objects.requireNonNull(Minecraft.getInstance().player).displayClientMessage(
                        Component.translatable("command.exposure.load_from_file.success", finalExposureId)
                                .withStyle(ChatFormatting.GREEN), false);
            } catch (IOException e) {
                LogUtils.getLogger().error("Cannot load exposure:" + e);
                Objects.requireNonNull(Minecraft.getInstance().player).displayClientMessage(
                        Component.translatable("command.exposure.load_from_file.failure")
                                .withStyle(ChatFormatting.RED), false);
            }
        }).start();
    }

    public static void startExposure(StartExposureS2CP packet) {
        Minecraft.getInstance().execute(() -> {
            @Nullable LocalPlayer player = Minecraft.getInstance().player;
            Preconditions.checkState(player != null, "Player cannot be null.");

            ItemStack itemInHand = player.getItemInHand(packet.activeHand());
            if (!(itemInHand.getItem() instanceof CameraItem cameraItem) || !cameraItem.isActive(itemInHand))
                throw new IllegalStateException("Player should have active Camera in hand. " + itemInHand);

            cameraItem.exposeFrameClientside(player, packet.activeHand(), packet.exposureId(), packet.flashHasFired(), packet.lightLevel());
        });
    }

    public static void showExposure(ShowExposureS2CP packet) {
        executeOnMainThread(() -> {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                LogUtils.getLogger().error("Cannot show exposures. Player is null.");
                return;
            }

            boolean negative = packet.negative();

            @Nullable Screen screen;

            if (packet.latest()) {
                screen = createLatestScreen(player, negative);
            } else {
                if (negative) {
                    Either<String, ResourceLocation> idOrTexture = packet.isTexture() ?
                            Either.right(new ResourceLocation(packet.idOrPath())) : Either.left(packet.idOrPath());
                    screen = new NegativeExposureScreen(List.of(idOrTexture));
                } else {
                    ItemStack stack = new ItemStack(Exposure.Items.PHOTOGRAPH.get());
                    CompoundTag tag = new CompoundTag();
                    tag.putString(packet.isTexture() ? FrameData.TEXTURE : FrameData.ID, packet.idOrPath());
                    stack.setTag(tag);

                    screen = new PhotographScreen(List.of(new ItemAndStack<>(stack)));
                }
            }

            if (screen != null)
                Minecraft.getInstance().setScreen(screen);
        });
    }

    private static @Nullable Screen createLatestScreen(Player player, boolean negative) {
        List<CompoundTag> latestFrames = CapturedFramesHistory.get()
                .stream()
                .filter(frame -> !frame.getString(FrameData.ID).isEmpty())
                .toList();

        if (latestFrames.isEmpty()) {
            player.displayClientMessage(Component.translatable("command.exposure.show.latest.error.no_exposures"), false);
            return null;
        }

        if (negative) {
            List<Either<String, ResourceLocation>> exposures = new ArrayList<>();
            for (CompoundTag frame : latestFrames) {
                String exposureId = frame.getString(FrameData.ID);
                exposures.add(Either.left(exposureId));
            }
            return new NegativeExposureScreen(exposures);
        } else {
            List<ItemAndStack<PhotographItem>> photographs = new ArrayList<>();

            for (CompoundTag frame : latestFrames) {
                ItemStack stack = new ItemStack(Exposure.Items.PHOTOGRAPH.get());
                stack.setTag(frame);

                photographs.add(new ItemAndStack<>(stack));
            }

            return new PhotographScreen(photographs);
        }
    }

    public static void clearRenderingCache() {
        executeOnMainThread(() -> ExposureClient.getExposureRenderer().clearData());
    }

    public static void syncLenses(SyncLensesS2CP packet) {
        executeOnMainThread(() -> Lenses.reload(packet.lenses()));
    }

    public static void waitForExposureChange(WaitForExposureChangeS2CP packet) {
        executeOnMainThread(() -> ExposureClient.getExposureStorage().putOnWaitingList(packet.exposureId()));
    }

    public static void onExposureChanged(ExposureChangedS2CP packet) {
        executeOnMainThread(() -> {
            ExposureClient.getExposureStorage().remove(packet.exposureId());
            ExposureClient.getExposureRenderer().clearDataSingle(packet.exposureId(), true);
        });
    }

    public static void onFrameAdded(OnFrameAddedS2CP packet) {
        executeOnMainThread(() -> CapturedFramesHistory.add(packet.frame()));
    }

    private static void executeOnMainThread(Runnable runnable) {
        Minecraft.getInstance().execute(runnable);
    }

}
