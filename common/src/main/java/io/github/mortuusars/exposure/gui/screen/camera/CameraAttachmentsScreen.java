package io.github.mortuusars.exposure.gui.screen.camera;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.camera.infrastructure.FocalRange;
import io.github.mortuusars.exposure.item.CameraItem;
import io.github.mortuusars.exposure.menu.CameraAttachmentsMenu;
import io.github.mortuusars.exposure.sound.OnePerPlayerSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CameraAttachmentsScreen extends AbstractContainerScreen<CameraAttachmentsMenu> {
    public static final ResourceLocation TEXTURE = Exposure.resource("textures/gui/camera_attachments.png");

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
    }

    @Override
    public void render(@NotNull PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);

        if (Minecraft.getInstance().player != null) {
            for (Slot slot : getMenu().slots) {
                if (!slot.mayPickup(Minecraft.getInstance().player)) {
                    renderItem(slot.getItem(), leftPos + slot.x, topPos + slot.y);
                    poseStack.pushPose();
                    poseStack.translate(0, 0, 350);
                    Screen.fill(poseStack, leftPos + slot.x - 1, topPos + slot.y - 1,
                            leftPos + slot.x + 17, topPos + slot.y + 17, 0x66c8c8c8);
                    poseStack.popPose();
                }
            }
        }

        this.renderTooltip(poseStack, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, TEXTURE);
        blit(poseStack, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        Slot filmSlot = menu.slots.get(CameraItem.FILM_ATTACHMENT.slot());
        if (!filmSlot.hasItem())
            blit(poseStack, leftPos + filmSlot.x - 1, topPos + filmSlot.y - 1, 238, 0, 18, 18);

        Slot flashSlot = menu.slots.get(CameraItem.FLASH_ATTACHMENT.slot());
        if (!flashSlot.hasItem())
            blit(poseStack, leftPos + flashSlot.x - 1, topPos + flashSlot.y - 1, 238, 18, 18, 18);
        else
            blit(poseStack, leftPos + 99, topPos + 7, 0, 185, 24, 28);

        Slot lensSlot = menu.slots.get(CameraItem.LENS_ATTACHMENT.slot());
        boolean hasLens = lensSlot.hasItem();
        if (hasLens)
            blit(poseStack, leftPos + 103, topPos + 49, 24, 185, 31, 35);
        else
            blit(poseStack, leftPos + lensSlot.x - 1, topPos + lensSlot.y - 1, 238, 36, 18, 18);

        Slot filterSlot = menu.slots.get(CameraItem.FILTER_ATTACHMENT.slot());
        if (filterSlot.hasItem()) {
            int x = hasLens ? 116 : 106;
            int y = hasLens ? 58 : 53;

            float r = 1f;
            float g = 1f;
            float b = 1f;

            ResourceLocation key = Registry.ITEM.getKey(filterSlot.getItem().getItem());
            if (key.getNamespace().equals("minecraft") && key.getPath().contains("_stained_glass_pane")) {
                String colorString = key.getPath().replace("_stained_glass_pane", "");
                DyeColor color = DyeColor.byName(colorString, DyeColor.WHITE);
                int rgb = color.getFireworkColor();
                r = Mth.clamp(((rgb >> 16) & 0xFF) / 255f, 0f, 1f);
                g = Mth.clamp((((rgb >> 8) & 0xFF) / 255f), 0f, 1f);
                b = Mth.clamp((rgb & 0xFF) / 255f, 0f, 1f);
            }

            RenderSystem.setShaderColor(r, g, b, 1f);

            if (!filterSlot.getItem().is(Items.GLASS_PANE))
                blit(poseStack, leftPos + x, topPos + y, 55, 185, 15, 23); // Glass part

            blit(poseStack, leftPos + x, topPos + y, 70, 185, 15, 23); // Glares
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        } else {
            blit(poseStack, leftPos + filterSlot.x - 1, topPos + filterSlot.y - 1, 238, 54, 18, 18);
        }

        RenderSystem.disableBlend();
    }

    @Override
    public @NotNull List<Component> getTooltipFromItem(ItemStack stack) {
        List<Component> tooltip = super.getTooltipFromItem(stack);
        if (stack.is(Exposure.Tags.Items.LENSES) && hoveredSlot != null && hoveredSlot.getItem().equals(stack)) {
            tooltip.add(Component.translatable("gui.exposure.viewfinder.focal_length", FocalRange.ofStack(stack).getSerializedName())
                    .withStyle(ChatFormatting.GOLD));
        }
        return tooltip;
    }
}
