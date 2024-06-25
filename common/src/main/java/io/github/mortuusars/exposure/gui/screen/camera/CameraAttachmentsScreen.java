package io.github.mortuusars.exposure.gui.screen.camera;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.camera.infrastructure.FocalRange;
import io.github.mortuusars.exposure.data.Lenses;
import io.github.mortuusars.exposure.data.filter.Filters;
import io.github.mortuusars.exposure.gui.screen.ItemListScreen;
import io.github.mortuusars.exposure.item.CameraItem;
import io.github.mortuusars.exposure.menu.CameraAttachmentsMenu;
import io.github.mortuusars.exposure.sound.OnePerPlayerSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class CameraAttachmentsScreen extends AbstractContainerScreen<CameraAttachmentsMenu> {
    public static final ResourceLocation TEXTURE = Exposure.resource("textures/gui/camera_attachments.png");

    protected final Player player;

    protected Map<Integer, Rect2i> slotPlaceholders = Collections.emptyMap();

    protected final HoveredElement flash = new HoveredElement(List.of(new Rect2i(96, 11, 28, 27)),
            () -> getMenu().getSlot(CameraItem.FLASH_ATTACHMENT.slot()).hasItem());
    protected final HoveredElement filterOnLens = new HoveredElement(List.of(new Rect2i(114, 57, 13, 6),
            new Rect2i(110, 63, 17, 24)), () -> getMenu().getSlot(CameraItem.LENS_ATTACHMENT.slot()).hasItem());
    protected final HoveredElement lens = new HoveredElement(List.of(new Rect2i(93, 48, 33, 34)),
            () -> getMenu().getSlot(CameraItem.LENS_ATTACHMENT.slot()).hasItem());
    protected final HoveredElement filter = new HoveredElement(List.of(new Rect2i(110, 55, 13, 6),
            new Rect2i(106, 61, 17, 24)), () -> !getMenu().getSlot(CameraItem.LENS_ATTACHMENT.slot()).hasItem());
    protected final HoveredElement lensBuiltIn = new HoveredElement(List.of(new Rect2i(93, 48, 29, 32)),
            () -> !getMenu().getSlot(CameraItem.LENS_ATTACHMENT.slot()).hasItem());
    protected final HoveredElement viewfinder = new HoveredElement(List.of(new Rect2i(65, 25, 30, 12),
            new Rect2i(72, 31, 39, 11), new Rect2i(80, 42, 24, 5)), () -> true);

    public CameraAttachmentsScreen(CameraAttachmentsMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.player = playerInventory.player;
    }

    @Override
    public void added() {
        OnePerPlayerSounds.play(player, Exposure.SoundEvents.CAMERA_GENERIC_CLICK.get(), SoundSource.PLAYERS, 0.9f, 0.9f);
    }

    @Override
    protected void init() {
        this.imageHeight = 185;
        inventoryLabelY = this.imageHeight - 94;
        super.init();

        slotPlaceholders = Map.of(
                CameraItem.FILM_ATTACHMENT.slot(), new Rect2i(238, 0, 18, 18),
                CameraItem.FLASH_ATTACHMENT.slot(), new Rect2i(238, 18, 18, 18),
                CameraItem.LENS_ATTACHMENT.slot(), new Rect2i(238, 36, 18, 18),
                CameraItem.FILTER_ATTACHMENT.slot(), new Rect2i(238, 54, 18, 18)
        );
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        for (Slot slot : getMenu().slots) {
            if (!slot.mayPickup(player)) {
                guiGraphics.renderItem(slot.getItem(), leftPos + slot.x, topPos + slot.y);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                guiGraphics.blit(TEXTURE, leftPos + slot.x - 2, topPos + slot.y - 2, 350, 236, 92, 20, 20, 256, 256);
                RenderSystem.disableBlend();
            }
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        renderSlotPlaceholders(guiGraphics, mouseX, mouseY, partialTick);

        renderAttachments(guiGraphics, mouseX, mouseY, partialTick);

        for (Slot slot : getMenu().slots) {
            if (!slot.mayPickup(player)) {
                guiGraphics.blit(TEXTURE, leftPos + slot.x - 2, topPos + slot.y - 2, 236, 72, 20, 20);
            }
        }

        RenderSystem.disableBlend();
    }

    private void renderAttachments(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (getMenu().getSlot(CameraItem.FLASH_ATTACHMENT.slot()).hasItem()) {
            int vOffset = isMouseOver(flash, mouseX, mouseY) ? 28 : 0;
            guiGraphics.blit(TEXTURE, leftPos + 96, topPos + 11, 176, vOffset, 28, 28);
        }

        boolean hasLens = getMenu().getSlot(CameraItem.LENS_ATTACHMENT.slot()).hasItem();
        if (hasLens) {
            int vOffset = isMouseOver(lens, mouseX, mouseY) && !isMouseOver(filterOnLens, mouseX, mouseY) ? 37 : 0;
            guiGraphics.blit(TEXTURE, leftPos + 93, topPos + 47, 176, 56 + vOffset, 35, 37);
        } else if (isMouseOver(lensBuiltIn, mouseX, mouseY) && !isMouseOver(filter, mouseX, mouseY)) {
            guiGraphics.blit(TEXTURE, leftPos + 93, topPos + 47, 176, 130, 31, 35);
        }

        Slot filterSlot = getMenu().getSlot(CameraItem.FILTER_ATTACHMENT.slot());
        int filterX = hasLens ? 102 : 98;
        int filterY = hasLens ? 54 : 52;
        if (filterSlot.hasItem()) {
            Filters.of(filterSlot.getItem()).ifPresent(filter -> {
                int tintRGB = filter.getTintColor();
                float r = ((tintRGB >> 16) & 0xFF) / 255f;
                float g = ((tintRGB >> 8) & 0xFF) / 255f;
                float b = (tintRGB & 0xFF) / 255f;

                if (isMouseOver(filterOnLens, mouseX, mouseY) || isMouseOver(this.filter, mouseX, mouseY)) {
                    r *= 1.35f;
                    g *= 1.35f;
                    b *= 1.35f;
                }

                RenderSystem.setShaderColor(r, g, b, 1.0F);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();

                ResourceLocation filterTexture = filter.getAttachmentTexture();
                guiGraphics.blit(filterTexture, leftPos + filterX, topPos + filterY, 0, 0, 32, 32, 32, 32);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            });
        } else if (isMouseOver(filterOnLens, mouseX, mouseY)) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            guiGraphics.blit(TEXTURE, leftPos + 110, topPos + 58, 176, 165, 15, 23);
        } else if (isMouseOver(filter, mouseX, mouseY)) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            guiGraphics.blit(TEXTURE, leftPos + 106, topPos + 56, 176, 165, 15, 23);
        }

        if (isMouseOver(viewfinder, mouseX, mouseY) && !isMouseOver(flash, mouseX, mouseY)) {
            guiGraphics.blit(TEXTURE, leftPos + 65, topPos + 24, 42, 185, 49, 26);
        }
    }

    protected boolean isMouseOver(HoveredElement element, int mouseX, int mouseY) {
        if (!element.isEnabled.get()) {
            return false;
        }

        mouseX -= leftPos;
        mouseY -= topPos;

        for (Rect2i area : element.hoverArea) {
            if (mouseX >= area.getX() && mouseX < area.getX() + area.getWidth() &&
                    mouseY >= area.getY() && mouseY < area.getY() + area.getHeight()) {
                return true;
            }
        }

        return false;
    }

    protected void renderSlotPlaceholders(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        for (int slotIndex : slotPlaceholders.keySet()) {
            Slot slot = getMenu().getSlot(slotIndex);
            if (!slot.hasItem()) {
                Rect2i placeholder = slotPlaceholders.get(slotIndex);
                guiGraphics.blit(TEXTURE, leftPos + slot.x - 1, topPos + slot.y - 1,
                        placeholder.getX(), placeholder.getY(), placeholder.getWidth(), placeholder.getHeight());
            }
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        if (isMouseOver(flash, x, y)) {
            guiGraphics.renderTooltip(font, font.split(
                    Component.translatable("gui.exposure.camera_attachments.flash.tooltip"), 230), x, y);
        } else if (isMouseOver(viewfinder, x, y)) {
            Component key = Component.literal(ExposureClient.getCameraControlsKey().getTranslatedKeyMessage().getString())
                    .withStyle(ChatFormatting.GRAY);
            Component middleClick = Config.Client.VIEWFINDER_MIDDLE_CLICK_CONTROLS.get()
                    ? Component.translatable("gui.exposure.camera_attachments.viewfinder.tooltip.or_middle_click")
                    : Component.empty();
            guiGraphics.renderTooltip(font, font.split(
                    Component.translatable("gui.exposure.camera_attachments.viewfinder.tooltip", key, middleClick), 230), x, y);
        } else if (isMouseOver(filter, x, y) || isMouseOver(filterOnLens, x, y)) {
            guiGraphics.renderTooltip(font, font.split(
                    Component.translatable("gui.exposure.camera_attachments.filter.tooltip"), 230), x, y);
        } else if (isMouseOver(lens, x, y) || isMouseOver(lensBuiltIn, x, y)) {
            guiGraphics.renderTooltip(font, font.split(
                    Component.translatable("gui.exposure.camera_attachments.lens.tooltip"), 230), x, y);
        } else {
            super.renderTooltip(guiGraphics, x, y);
        }
    }

    @Override
    protected @NotNull List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> tooltip = super.getTooltipFromContainerItem(stack);
        if (stack.is(Exposure.Tags.Items.LENSES) && hoveredSlot != null && hoveredSlot.getItem().equals(stack)) {
            tooltip.add(Component.translatable("gui.exposure.viewfinder.focal_length", FocalRange.ofStack(stack).getSerializedName())
                    .withStyle(ChatFormatting.GOLD));
        }
        return tooltip;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (int) mouseX;
        int y = (int) mouseY;

        if (isMouseOver(filter, x, y) || isMouseOver(filterOnLens, x, y)) {
            List<ItemStack> itemStacks = new ArrayList<>();
            for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(Exposure.Tags.Items.FILTERS)) {
                itemStacks.add(new ItemStack(holder));
            }

            ItemListScreen screen = new ItemListScreen(this, Component.translatable("gui.exposure.filters"), itemStacks) {
                @Override
                protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
                    List<Component> tooltip = super.getTooltipFromContainerItem(stack);
                    if (Minecraft.getInstance().options.advancedItemTooltips) {
                        Filters.of(stack).ifPresent(filter ->
                                tooltip.add(Component.literal(filter.getShader().toString())
                                        .withStyle(ChatFormatting.GRAY)));
                    }
                    return tooltip;
                }
            };
            Minecraft.getInstance().setScreen(screen);

            return true;
        } else if (isMouseOver(lens, x, y) || isMouseOver(lensBuiltIn, x, y)) {
            List<ItemStack> itemStacks = new ArrayList<>();
            for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(Exposure.Tags.Items.LENSES)) {
                itemStacks.add(new ItemStack(holder));
            }

            ItemListScreen screen = new ItemListScreen(this, Component.translatable("gui.exposure.lenses"), itemStacks) {
                @Override
                protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
                    List<Component> tooltip = super.getTooltipFromContainerItem(stack);
                    Lenses.getFocalRangeOf(stack).ifPresent(fr ->
                            tooltip.add(Component.translatable("gui.exposure.viewfinder.focal_length", fr.getSerializedName())
                                    .withStyle(ChatFormatting.GOLD)));
                    return tooltip;
                }
            };
            Minecraft.getInstance().setScreen(screen);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    public record HoveredElement(List<Rect2i> hoverArea, Supplier<Boolean> isEnabled) { }
}
