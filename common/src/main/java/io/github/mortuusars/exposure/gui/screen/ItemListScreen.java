package io.github.mortuusars.exposure.gui.screen;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.mortuusars.exposure.Exposure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ItemListScreen extends Screen {
    public static final ResourceLocation TEXTURE = Exposure.resource("textures/gui/item_list.png");

    protected final Screen parent;
    protected final List<ItemStack> items;

    protected int imageWidth = 176;
    protected int imageHeight = 166;
    protected int titleLabelX = 8;
    protected int titleLabelY = 6;
    protected int leftPos;
    protected int topPos;

    protected int rowsCount;
    @Nullable
    protected Slot hoveredSlot;
    protected List<Slot> slots = new ArrayList<>();

    public ItemListScreen(Screen parent, Component title, List<ItemStack> items) {
        super(title);
        this.parent = parent;
        this.items = items;

        SimpleContainer container = new SimpleContainer(items.toArray(ItemStack[]::new));

        List<List<ItemStack>> rows = Lists.partition(items, 9);

        rowsCount = rows.size();

        int rowX = 8;
        int rowY = 18;

        for (int row = 0; row < rows.size(); row++) {
            List<ItemStack> stacks = rows.get(row);

            // Centers row if it has fewer items than 9
            int rowXToCenterOffset = ((9 * 18) - (stacks.size() * 18)) / 2;

            for (int column = 0; column < stacks.size(); column++) {
                int slotIndex = row * 9 + column;
                slots.add(new Slot(container, slotIndex, rowX + rowXToCenterOffset + (column * 18), rowY) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }

                    @Override
                    public boolean mayPickup(Player player) {
                        return false;
                    }
                });
            }

            rowY += 18;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        this.imageWidth = 176;
        this.imageHeight = 24 + (rowsCount * 18);
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int left = leftPos;
        int top = topPos;
        renderBackground(guiGraphics);
        renderBg(guiGraphics, partialTick, mouseX, mouseY);
        RenderSystem.disableDepthTest();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(left, top, 0.0f);
        hoveredSlot = null;
        for (Slot slot : slots) {
            if (slot.isActive()) {
                renderSlot(guiGraphics, slot);
            }
            if (!isHovering(slot, mouseX, mouseY) || !slot.isActive()) {
                continue;
            }
            this.hoveredSlot = slot;
            if (!hoveredSlot.isHighlightable()) {
                continue;
            }
            renderSlotHighlight(guiGraphics, slot.x, slot.y, 0);
        }
        this.renderLabels(guiGraphics, mouseX, mouseY);
        guiGraphics.pose().popPose();
        RenderSystem.enableDepthTest();

        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Render BG expanding it according to number of rows
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, 17);
        for (int i = 0; i < rowsCount; i++) {
            guiGraphics.blit(TEXTURE, leftPos, topPos + 17 + (i * 18), 0, 17, imageWidth, 18);
        }
        guiGraphics.blit(TEXTURE, leftPos, topPos + 17 + (rowsCount * 18), 0, 35, imageWidth, 7);

        for (Slot slot : slots) {
            guiGraphics.blit(TEXTURE, leftPos + slot.x - 1, topPos + slot.y - 1, 176, 0, 18, 18);
        }
    }

    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
    }

    protected void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        int x = slot.x;
        int y = slot.y;
        ItemStack itemStack = slot.getItem();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0f, 0.0f, 100.0f);
        guiGraphics.renderItem(itemStack, x, y, slot.x + slot.y * imageWidth);
        guiGraphics.renderItemDecorations(font, itemStack, x, y, null);
        guiGraphics.pose().popPose();
    }

    public static void renderSlotHighlight(GuiGraphics guiGraphics, int x, int y, int blitOffset) {
        guiGraphics.fillGradient(RenderType.guiOverlay(), x, y, x + 16, y + 16, -2130706433, -2130706433, blitOffset);
    }

    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        if (hoveredSlot != null && hoveredSlot.hasItem()) {
            ItemStack itemStack = hoveredSlot.getItem();
            guiGraphics.renderTooltip(font, getTooltipFromContainerItem(itemStack), itemStack.getTooltipImage(), x, y);
        }
    }

    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        return AbstractContainerScreen.getTooltipFromItem(Minecraft.getInstance(), stack);
    }

    protected boolean isHovering(Slot slot, double mouseX, double mouseY) {
        return this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY);
    }

    protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        int i = this.leftPos;
        int j = this.topPos;
        return (mouseX -= (double)i) >= (double)(x - 1) && mouseX < (double)(x + width + 1) && (mouseY -= (double)j) >= (double)(y - 1) && mouseY < (double)(y + height + 1);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (Minecraft.getInstance().options.keyInventory.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
