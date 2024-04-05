package io.github.mortuusars.exposure.gui.screen;

import com.google.common.base.Preconditions;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.datafixers.util.Either;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.block.entity.Lightroom;
import io.github.mortuusars.exposure.block.entity.LightroomBlockEntity;
import io.github.mortuusars.exposure.camera.infrastructure.FilmType;
import io.github.mortuusars.exposure.camera.infrastructure.FrameData;
import io.github.mortuusars.exposure.item.DevelopedFilmItem;
import io.github.mortuusars.exposure.menu.LightroomMenu;
import io.github.mortuusars.exposure.render.modifiers.ExposurePixelModifiers;
import io.github.mortuusars.exposure.util.ColorChannel;
import io.github.mortuusars.exposure.util.PagingDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LightroomScreen extends AbstractContainerScreen<LightroomMenu> {
    public static final ResourceLocation MAIN_TEXTURE = Exposure.resource("textures/gui/lightroom.png");
    public static final ResourceLocation FILM_OVERLAYS_TEXTURE = Exposure.resource("textures/gui/lightroom_film_overlays.png");
    public static final int FRAME_SIZE = 54;

    protected Player player;
    protected Button printButton;
    protected ChromaticProcessToggleButton processToggleButton;

    public LightroomScreen(LightroomMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.player = playerInventory.player;
    }

    @Override
    protected void init() {
        imageWidth = 176;
        imageHeight = 210;
        super.init();
        inventoryLabelY = 116;

        printButton = new ImageButton(leftPos + 117, topPos + 89, 22, 22, 176, 17,
                22, MAIN_TEXTURE, 256, 256, this::onPrintButtonPressed,
                (b, ps, x, y) -> {
                    List<Component> lines = new ArrayList<>();
                    lines.add(Component.translatable("gui.exposure.lightroom.print"));
                    if (player.isCreative()) {
                        lines.add(Component.translatable("gui.exposure.lightroom.print.creative_tooltip"));
                    }
                    renderTooltip(ps, lines, Optional.empty(), x, y);
                }, Component.translatable("gui.exposure.lightroom.print"));
        addRenderableWidget(printButton);

        processToggleButton = new ChromaticProcessToggleButton(leftPos - 19, topPos + 91,
                (b, ps, x, y) -> {
                    Lightroom.Process process = getMenu().getBlockEntity().getProcess();
                    List<Component> lines = new ArrayList<>();
                    lines.add(Component.translatable("gui.exposure.lightroom.process." + process.getSerializedName()));
                    if (process == Lightroom.Process.CHROMATIC) {
                        lines.add(Component.translatable("gui.exposure.lightroom.process.chromatic.info")
                                .withStyle(ChatFormatting.GRAY));
                    }
                    renderTooltip(ps, lines, Optional.empty(), x, y);
                },
                this::onProcessToggleButtonPressed, () -> getMenu().getBlockEntity().getProcess());
        addRenderableWidget(processToggleButton);

        updateButtons();
    }

    protected void onPrintButtonPressed(Button button) {
        if (Minecraft.getInstance().gameMode != null) {
            if (Screen.hasShiftDown() && player.isCreative())
                Minecraft.getInstance().gameMode.handleInventoryButtonClick(getMenu().containerId, LightroomMenu.PRINT_CREATIVE_BUTTON_ID);
            else
                Minecraft.getInstance().gameMode.handleInventoryButtonClick(getMenu().containerId, LightroomMenu.PRINT_BUTTON_ID);
        }
    }

    protected void onProcessToggleButtonPressed(Button button) {
        if (Minecraft.getInstance().gameMode != null)
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(getMenu().containerId, LightroomMenu.TOGGLE_PROCESS_BUTTON_ID);
    }

    @Override
    public void render(@NotNull PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        updateButtons();

        renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);
        renderTooltip(poseStack, mouseX, mouseY);
    }

    protected void updateButtons() {
        printButton.active = getMenu().getBlockEntity().canPrint() || (player.isCreative() && Screen.hasShiftDown() && getMenu().getBlockEntity().canPrintInCreativeMode());
        printButton.visible = !getMenu().isPrinting();

        processToggleButton.active = true;
        processToggleButton.visible = getMenu().getExposedFrames().getCompound(
                getMenu().getSelectedFrame()).getBoolean(FrameData.CHROMATIC);
    }

    @Override
    protected void renderBg(@NotNull PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, MAIN_TEXTURE);
        blit(poseStack, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        blit(poseStack, leftPos - 27, topPos + 34, 0, 208, 28, 31);

        // PLACEHOLDER ICONS
        if (!getMenu().slots.get(Lightroom.FILM_SLOT).hasItem())
            blit(poseStack, leftPos - 21, topPos + 41, 238, 0, 18, 18);
        if (!getMenu().slots.get(Lightroom.PAPER_SLOT).hasItem())
            blit(poseStack, leftPos + 7, topPos + 91, 238, 18, 18, 18);
        if (!getMenu().slots.get(Lightroom.CYAN_SLOT).hasItem())
            blit(poseStack, leftPos + 41, topPos + 91, 238, 36, 18, 18);
        if (!getMenu().slots.get(Lightroom.MAGENTA_SLOT).hasItem())
            blit(poseStack, leftPos + 59, topPos + 91, 238, 54, 18, 18);
        if (!getMenu().slots.get(Lightroom.YELLOW_SLOT).hasItem())
            blit(poseStack, leftPos + 77, topPos + 91, 238, 72, 18, 18);
        if (!getMenu().slots.get(Lightroom.BLACK_SLOT).hasItem())
            blit(poseStack, leftPos + 95, topPos + 91, 238, 90, 18, 18);

        if (getMenu().isPrinting()) {
            int progress = getMenu().getData().get(LightroomBlockEntity.CONTAINER_DATA_PROGRESS_ID);
            int time = getMenu().getData().get(LightroomBlockEntity.CONTAINER_DATA_PRINT_TIME_ID);
            int width = progress != 0 && time != 0 ? progress * 24 / time : 0;
            blit(poseStack, leftPos + 116, topPos + 91, 176, 0, width, 17);
        }

        RenderSystem.setShaderTexture(0, FILM_OVERLAYS_TEXTURE);

        ListTag frames = getMenu().getExposedFrames();
        if (frames.isEmpty()) {
            blit(poseStack, leftPos + 4, topPos + 15, 0, 136, 168, 68);
            return;
        }

        ItemStack filmStack = getMenu().getSlot(Lightroom.FILM_SLOT).getItem();
        if (!(filmStack.getItem() instanceof DevelopedFilmItem film))
            return;

        FilmType negative = film.getType();

        int selectedFrame = getMenu().getSelectedFrame();
        @Nullable CompoundTag leftFrame = getMenu().getFrameIdByIndex(selectedFrame - 1);
        @Nullable CompoundTag centerFrame = getMenu().getFrameIdByIndex(selectedFrame);
        @Nullable CompoundTag rightFrame = getMenu().getFrameIdByIndex(selectedFrame + 1);

        RenderSystem.setShaderColor(negative.filmR, negative.filmG, negative.filmB, negative.filmA);

        // Left film part
        blit(poseStack, leftPos + 1, topPos + 15, 0, leftFrame != null ? 68 : 0, 54, 68);
        // Center film part
        blit(poseStack, leftPos + 55, topPos + 15, 55, rightFrame != null ? 0 : 68, 64, 68);
        // Right film part
        if (rightFrame != null) {
            boolean hasMoreFrames = selectedFrame + 2 < frames.size();
            blit(poseStack, leftPos + 119, topPos + 15, 120, hasMoreFrames ? 68 : 0, 56, 68);
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (leftFrame != null)
            renderFrame(leftFrame, poseStack, leftPos + 6, topPos + 22, FRAME_SIZE, isOverLeftFrame(mouseX, mouseY) ? 0.8f : 0.25f, negative);
        if (centerFrame != null)
            renderFrame(centerFrame, poseStack, leftPos + 61, topPos + 22, FRAME_SIZE, 0.9f, negative);
        if (rightFrame != null)
            renderFrame(rightFrame, poseStack, leftPos + 116, topPos + 22, FRAME_SIZE, isOverRightFrame(mouseX, mouseY) ? 0.8f : 0.25f, negative);

        RenderSystem.setShaderColor(negative.filmR, negative.filmG, negative.filmB, negative.filmA);
        RenderSystem.setShaderTexture(0, MAIN_TEXTURE);

        if (getMenu().getBlockEntity().isAdvancingFrameOnPrint()) {
            poseStack.pushPose();
            poseStack.translate(0, 0, 800);

            if (selectedFrame < getMenu().getTotalFrames() - 1)
                blit(poseStack, leftPos + 111, topPos + 44, 200, 0, 10, 10);
            else
                blit(poseStack, leftPos + 111, topPos + 44, 210, 0, 10, 10);

            poseStack.popPose();
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    protected void renderTooltip(@NotNull PoseStack poseStack, int mouseX, int mouseY) {
        super.renderTooltip(poseStack, mouseX, mouseY);

        boolean advancedTooltips = Minecraft.getInstance().options.advancedItemTooltips;
        int selectedFrame = getMenu().getSelectedFrame();
        List<Component> tooltipLines = new ArrayList<>();

        int hoveredFrameIndex = -1;

        if (isOverLeftFrame(mouseX, mouseY)) {
            hoveredFrameIndex = selectedFrame - 1;
            tooltipLines.add(Component.translatable("gui.exposure.lightroom.previous_frame"));
        } else if (isOverCenterFrame(mouseX, mouseY)) {
            hoveredFrameIndex = selectedFrame;
            tooltipLines.add(Component.translatable("gui.exposure.lightroom.current_frame", Integer.toString(getMenu().getSelectedFrame() + 1)));
        } else if (isOverRightFrame(mouseX, mouseY)) {
            hoveredFrameIndex = selectedFrame + 1;
            tooltipLines.add(Component.translatable("gui.exposure.lightroom.next_frame"));
        }

        if (hoveredFrameIndex != -1)
            addFrameInfoTooltipLines(tooltipLines, hoveredFrameIndex, advancedTooltips);

        renderTooltip(poseStack, tooltipLines, Optional.empty(), mouseX, mouseY);
    }

    private void addFrameInfoTooltipLines(List<Component> tooltipLines, int frameIndex, boolean isAdvancedTooltips) {
        @Nullable CompoundTag frame = getMenu().getFrameIdByIndex(frameIndex);
        if (frame != null) {
            ColorChannel.fromString(frame.getString(FrameData.CHROMATIC_CHANNEL)).ifPresent(c ->
                    tooltipLines.add(Component.translatable("gui.exposure.channel." + c.getSerializedName())
                        .withStyle(Style.EMPTY.withColor(c.getRepresentationColor()))));

            if (isAdvancedTooltips) {
                Either<String, ResourceLocation> idOrTexture = FrameData.getIdOrTexture(frame);
                MutableComponent component = idOrTexture.map(
                                id -> !id.isEmpty() ? Component.translatable("gui.exposure.frame.id",
                                        Component.literal(id).withStyle(ChatFormatting.GRAY)) : Component.empty(),
                                texture -> Component.translatable("gui.exposure.frame.texture",
                                        Component.literal(texture.toString()).withStyle(ChatFormatting.GRAY)))
                        .withStyle(ChatFormatting.DARK_GRAY);
                tooltipLines.add(component);
            }
        }
    }

    private boolean isOverLeftFrame(int mouseX, int mouseY) {
        ListTag frames = getMenu().getExposedFrames();
        int selectedFrame = getMenu().getSelectedFrame();
        return selectedFrame - 1 >= 0 && selectedFrame - 1 < frames.size() && isHovering(6, 22, FRAME_SIZE, FRAME_SIZE, mouseX, mouseY);
    }

    private boolean isOverCenterFrame(int mouseX, int mouseY) {
        ListTag frames = getMenu().getExposedFrames();
        int selectedFrame = getMenu().getSelectedFrame();
        return selectedFrame >= 0 && selectedFrame < frames.size() && isHovering(61, 22, FRAME_SIZE, FRAME_SIZE, mouseX, mouseY);
    }

    private boolean isOverRightFrame(int mouseX, int mouseY) {
        ListTag frames = getMenu().getExposedFrames();
        int selectedFrame = getMenu().getSelectedFrame();
        return selectedFrame + 1 >= 0 && selectedFrame + 1 < frames.size() && isHovering(116, 22, FRAME_SIZE, FRAME_SIZE, mouseX, mouseY);
    }

    public void renderFrame(@Nullable CompoundTag frame, PoseStack poseStack, float x, float y, float size, float alpha, FilmType negative) {
        if (frame == null)
            return;

        poseStack.pushPose();
        poseStack.translate(x, y, 0);

        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        Either<String, ResourceLocation> idOrTexture = FrameData.getIdOrTexture(frame);
        ExposureClient.getExposureRenderer().render(idOrTexture, ExposurePixelModifiers.NEGATIVE_FILM, poseStack, bufferSource,
                0, 0, size, size, 0, 0, 1, 1, LightTexture.FULL_BRIGHT,
                negative.frameR, negative.frameG, negative.frameB, Mth.clamp((int) Math.ceil(alpha * 255), 0, 255));

        bufferSource.endBatch();
        poseStack.popPose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        Preconditions.checkState(minecraft != null);
        Preconditions.checkState(minecraft.gameMode != null);

        if (minecraft.options.keyLeft.matches(keyCode, scanCode) || keyCode == InputConstants.KEY_LEFT) {
            changeFrame(PagingDirection.PREVIOUS);
            return true;
        } else if (minecraft.options.keyRight.matches(keyCode, scanCode) || keyCode == InputConstants.KEY_RIGHT) {
            changeFrame(PagingDirection.NEXT);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        boolean handled = super.mouseScrolled(mouseX, mouseY, delta);

        if (!handled) {
            if (delta >= 0.0 && isOverCenterFrame((int) mouseX, (int) mouseY)) // Scroll Up
                enterFrameInspectMode();
        }

        return handled;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Preconditions.checkState(minecraft != null);
            Preconditions.checkState(minecraft.gameMode != null);

            if (isOverCenterFrame((int) mouseX, (int) mouseY)) {
                enterFrameInspectMode();
                return true;
            }

            if (isOverLeftFrame((int) mouseX, (int) mouseY)) {
                changeFrame(PagingDirection.PREVIOUS);
                return true;
            }

            if (isOverRightFrame((int) mouseX, (int) mouseY)) {
                changeFrame(PagingDirection.NEXT);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    public void changeFrame(PagingDirection navigation) {
        if ((navigation == PagingDirection.PREVIOUS && getMenu().getSelectedFrame() == 0)
                || (navigation == PagingDirection.NEXT && getMenu().getSelectedFrame() == getMenu().getTotalFrames() - 1))
            return;

        Preconditions.checkState(minecraft != null);
        Preconditions.checkState(minecraft.player != null);
        Preconditions.checkState(minecraft.gameMode != null);
        int buttonId = navigation == PagingDirection.NEXT ? LightroomMenu.NEXT_FRAME_BUTTON_ID : LightroomMenu.PREVIOUS_FRAME_BUTTON_ID;
        minecraft.gameMode.handleInventoryButtonClick(getMenu().containerId, buttonId);
        player.playSound(Exposure.SoundEvents.CAMERA_LENS_RING_CLICK.get(), 1f, player.getLevel()
                .getRandom().nextFloat() * 0.4f + 0.8f);

        // Update block entity clientside to faster update advance frame arrows:
        getMenu().getBlockEntity().setSelectedFrame(getMenu().getBlockEntity().getSelectedFrameIndex() + (navigation == PagingDirection.NEXT ? 1 : -1));
    }

    private void enterFrameInspectMode() {
        Minecraft.getInstance().setScreen(new FilmFrameInspectScreen(this, getMenu()));
        player.playSound(Exposure.SoundEvents.CAMERA_LENS_RING_CLICK.get(), 1f, 1.3f);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
        return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, mouseButton)
                && hoveredSlot == null;
    }
}
