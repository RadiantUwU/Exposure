package io.github.mortuusars.exposure.gui.screen;

import com.google.common.base.Preconditions;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.datafixers.util.Either;
import com.mojang.math.Axis;
import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.camera.capture.component.FileSaveComponent;
import io.github.mortuusars.exposure.gui.screen.element.Pager;
import io.github.mortuusars.exposure.render.ExposureRenderer;
import io.github.mortuusars.exposure.item.PhotographItem;
import io.github.mortuusars.exposure.render.modifiers.ExposurePixelModifiers;
import io.github.mortuusars.exposure.util.ItemAndStack;
import io.github.mortuusars.exposure.util.PagingDirection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PhotographScreen extends ZoomableScreen {
    public static final ResourceLocation WIDGETS_TEXTURE = Exposure.resource("textures/gui/widgets.png");

    private final List<ItemAndStack<PhotographItem>> photographs;
    private final List<String> savedExposures = new ArrayList<>();

    private final Pager pager = new Pager(Exposure.SoundEvents.CAMERA_LENS_RING_CLICK.get());

    public PhotographScreen(List<ItemAndStack<PhotographItem>> photographs) {
        super(Component.empty());
        Preconditions.checkState(!photographs.isEmpty(), "No photographs to display.");
        this.photographs = photographs;

        // Query all photographs:
        for (ItemAndStack<PhotographItem> photograph : photographs) {
            @Nullable Either<String, ResourceLocation> idOrTexture = photograph.getItem()
                    .getIdOrTexture(photograph.getStack());
            if (idOrTexture != null)
                idOrTexture.ifLeft(id -> ExposureClient.getExposureStorage().getOrQuery(id));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        zoomFactor = (float) height / ExposureRenderer.SIZE;

        ImageButton previousButton = new ImageButton(0, (int) (height / 2f - 16 / 2f), 16, 16,
                0, 0, 16, WIDGETS_TEXTURE, 256, 256,
                button -> pager.changePage(PagingDirection.PREVIOUS), Component.translatable("gui.exposure.previous_page"));
        addRenderableWidget(previousButton);

        ImageButton nextButton = new ImageButton(width - 16, (int) (height / 2f - 16 / 2f), 16, 16,
                16, 0, 16, WIDGETS_TEXTURE, 256, 256,
                button -> pager.changePage(PagingDirection.NEXT), Component.translatable("gui.exposure.next_page"));
        addRenderableWidget(nextButton);

        pager.init(photographs.size(), true, previousButton, nextButton);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        pager.update();

        renderBackground(guiGraphics);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 500); // Otherwise exposure will overlap buttons
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.pose().popPose();

        guiGraphics.pose().pushPose();

        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().translate(width / 2f, height / 2f, 0);
        guiGraphics.pose().scale(scale, scale, scale);
        guiGraphics.pose().translate(ExposureRenderer.SIZE / -2f, ExposureRenderer.SIZE / -2f, 0);

        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(Tesselator.getInstance()
                .getBuilder());

        // Rendering paper bottom to top:
        for (int i = Math.min(2, photographs.size() - 1); i > 0; i--) {
            float posOffset = 4 * i;
            int brightness = Mth.clamp(255 - 50 * i, 0, 255);

            float rotateOffset = ExposureRenderer.SIZE / 2f;

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(posOffset, posOffset, 0);

            guiGraphics.pose().translate(rotateOffset, rotateOffset, 0);
            guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(i * 90 + 90));
            guiGraphics.pose().translate(-rotateOffset, -rotateOffset, 0);

            ExposureClient.getExposureRenderer().renderPaperTexture(guiGraphics.pose(),
                    bufferSource, 0, 0, ExposureRenderer.SIZE, ExposureRenderer.SIZE, 0, 0, 1, 1,
                    LightTexture.FULL_BRIGHT, brightness, brightness, brightness, 255);

            guiGraphics.pose().popPose();
        }

        ItemAndStack<PhotographItem> photograph = photographs.get(pager.getCurrentPage());
        @Nullable Either<String, ResourceLocation> idOrTexture = photograph.getItem()
                .getIdOrTexture(photograph.getStack());
        if (idOrTexture != null) {
            ExposureClient.getExposureRenderer().renderOnPaper(idOrTexture, ExposurePixelModifiers.EMPTY, guiGraphics.pose(), bufferSource,
                    0, 0, ExposureRenderer.SIZE, ExposureRenderer.SIZE, 0, 0, 1, 1,
                    LightTexture.FULL_BRIGHT, 255, 255, 255, 255, false);
        } else {
            ExposureClient.getExposureRenderer().renderPaperTexture(guiGraphics.pose(), bufferSource,
                    0, 0, ExposureRenderer.SIZE, ExposureRenderer.SIZE, 0, 0, 1, 1,
                    LightTexture.FULL_BRIGHT, 255, 255, 255, 255);
        }
        bufferSource.endBatch();

        guiGraphics.pose().popPose();

        trySaveToFile(photograph, idOrTexture);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return pager.handleKeyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (Screen.hasControlDown() && player != null && player.isCreative()) {
            ItemAndStack<PhotographItem> photograph = photographs.get(pager.getCurrentPage());
            @Nullable Either<String, ResourceLocation> idOrTexture = photograph.getItem().getIdOrTexture(photograph.getStack());

            if (keyCode == InputConstants.KEY_S) {
                if (idOrTexture != null) {
                    idOrTexture.ifLeft(id -> {
                        if (savedExposures.contains(id))
                            return;

                        ExposureClient.getExposureStorage().getOrQuery(id).ifPresent(exposure -> {
                            savedExposures.add(id);
                            new Thread(() -> FileSaveComponent.withDefaultFolders(id).save(exposure.getPixels(),
                                    exposure.getWidth(), exposure.getHeight(), exposure.getProperties()), "ExposureSaving").start();
                        });
                    });
                }

                return true;
            }

            if (keyCode == InputConstants.KEY_C) {
                if (idOrTexture != null) {
                    String text = idOrTexture.map(id -> id, ResourceLocation::toString);
                    Minecraft.getInstance().keyboardHandler.setClipboard(text);
                }
                return true;
            }

            if (keyCode == InputConstants.KEY_P) {
                @Nullable MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
                if (idOrTexture != null && gameMode != null)
                    gameMode.handleCreativeModeItemDrop(photograph.getStack().copy());
                return true;
            }
        }

        return pager.handleKeyReleased(keyCode, scanCode, modifiers) || super.keyReleased(keyCode, scanCode, modifiers);
    }

    private void trySaveToFile(ItemAndStack<PhotographItem> photograph, @Nullable Either<String, ResourceLocation> idOrTexture) {
        if (!Config.Client.EXPOSURE_SAVING.get() || idOrTexture == null || Minecraft.getInstance().player == null)
            return;

        CompoundTag tag = photograph.getStack().getTag();
        if (tag == null
                || !tag.contains("PhotographerId", Tag.TAG_INT_ARRAY)
                || !tag.getUUID("PhotographerId").equals(Minecraft.getInstance().player.getUUID())) {
            return;
        }

        idOrTexture.ifLeft(id -> {
            if (savedExposures.contains(id))
                return;

            ExposureClient.getExposureStorage().getOrQuery(id).ifPresent(exposure -> {
                savedExposures.add(id);
                new Thread(() -> FileSaveComponent.withDefaultFolders(id)
                        .save(exposure.getPixels(), exposure.getWidth(), exposure.getHeight(), exposure.getProperties()), "ExposureSaving").start();
            });
        });
    }
}
