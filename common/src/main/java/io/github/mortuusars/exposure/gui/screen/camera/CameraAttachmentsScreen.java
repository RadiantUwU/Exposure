package io.github.mortuusars.exposure.gui.screen.camera;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.camera.infrastructure.FocalRange;
import io.github.mortuusars.exposure.data.filter.Filters;
import io.github.mortuusars.exposure.item.CameraItem;
import io.github.mortuusars.exposure.menu.CameraAttachmentsMenu;
import io.github.mortuusars.exposure.sound.OnePerPlayerSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CameraAttachmentsScreen extends AbstractContainerScreen<CameraAttachmentsMenu> {
    public static final ResourceLocation TEXTURE = Exposure.resource("textures/gui/camera_attachments.png");

    protected Map<Integer, Rect2i> slotPlaceholders = Collections.emptyMap();

    public CameraAttachmentsScreen(CameraAttachmentsMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    public void added() {
        if (Minecraft.getInstance().player != null)
            OnePerPlayerSounds.play(Minecraft.getInstance().player, Exposure.SoundEvents.CAMERA_GENERIC_CLICK.get(),
                    SoundSource.PLAYERS, 0.9f, 0.9f);
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

        if (Minecraft.getInstance().player != null) {
            for (Slot slot : getMenu().slots) {
                if (!slot.mayPickup(Minecraft.getInstance().player)) {
                    guiGraphics.renderItem(slot.getItem(), leftPos + slot.x, topPos + slot.y);
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(0, 0, 350);
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    guiGraphics.blit(TEXTURE, leftPos + slot.x, topPos + slot.y, 176, 20, 16, 16);
                    RenderSystem.disableBlend();
                    guiGraphics.pose().popPose();
                }
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

        if (Minecraft.getInstance().player != null) {
            for (Slot slot : getMenu().slots) {
                if (!slot.mayPickup(Minecraft.getInstance().player)) {
                    guiGraphics.blit(TEXTURE, leftPos + slot.x - 2, topPos + slot.y - 2, 176, 0, 20, 20);
                }
            }
        }

        RenderSystem.disableBlend();
    }

    private void renderAttachments(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Slot flashSlot = menu.slots.get(CameraItem.FLASH_ATTACHMENT.slot());
        if (flashSlot.hasItem())
            guiGraphics.blit(TEXTURE, leftPos + 96, topPos + 11, 0, 185, 28, 28);

        Slot lensSlot = menu.slots.get(CameraItem.LENS_ATTACHMENT.slot());
        boolean hasLens = lensSlot.hasItem();
        if (hasLens)
            guiGraphics.blit(TEXTURE, leftPos + 97, topPos + 49, 28, 185, 31, 35);

        Slot filterSlot = menu.slots.get(CameraItem.FILTER_ATTACHMENT.slot());
        if (filterSlot.hasItem()) {
            int x = hasLens ? 102 : 98;
            int y = hasLens ? 54 : 52;

            Filters.of(filterSlot.getItem()).ifPresent(filter -> {
                int tintRGB = filter.getTintColor();
                float r = ((tintRGB >> 16) & 0xFF) / 255f;
                float g = ((tintRGB >> 8) & 0xFF) / 255f;
                float b = (tintRGB & 0xFF) / 255f;

                RenderSystem.setShaderColor(r, g, b, 1.0F);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();

                ResourceLocation filterTexture = filter.getAttachmentTexture();
                guiGraphics.blit(filterTexture, leftPos + x, topPos + y, 0, 0, 32, 32, 32, 32);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            });
        }
    }

    private void renderSlotPlaceholders(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
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
        super.renderTooltip(guiGraphics, x, y);
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
}
