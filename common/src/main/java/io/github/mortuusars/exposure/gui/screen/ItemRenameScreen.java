package io.github.mortuusars.exposure.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.menu.ItemRenameMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ItemRenameScreen extends AbstractContainerScreen<ItemRenameMenu> {
    public static final ResourceLocation TEXTURE = Exposure.resource("textures/gui/item_rename.png");

    private EditBox name;

    public ItemRenameScreen(ItemRenameMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        imageWidth = 206;
        imageHeight = 66;
        inventoryLabelX = -999;
        inventoryLabelY = -999;
        titleLabelX = 28;
        super.init();

        name = new EditBox(this.font, leftPos + 32, topPos + 21, 142, 12, Component.translatable("gui.exposure.item_rename.title"));
        name.setTextColor(-1);
        name.setTextColorUneditable(-1);
        name.setBordered(false);
        name.setMaxLength(ItemRenameMenu.MAX_NAME_LENGTH);
        name.setResponder(this::onNameChanged);
        name.setValue(getMenu().getItemName());
        addWidget(name);
        setInitialFocus(name);

        ImageButton applyButton = new ImageButton(leftPos + 133, topPos + 42, 19, 19,
                imageWidth, 0, 19, TEXTURE, 256, 256,
                button -> confirm(), Component.translatable("gui.exposure.item_rename.apply"));
        applyButton.setTooltip(Tooltip.create(Component.translatable("gui.exposure.item_rename.apply")));
        addRenderableWidget(applyButton);

        ImageButton cancelButton = new ImageButton(leftPos + 154, topPos + 42, 19, 19,
                imageWidth + 19, 0, 19, TEXTURE, 256, 256,
                button -> cancel(), Component.translatable("gui.exposure.item_rename.cancel"));
        cancelButton.setTooltip(Tooltip.create(Component.translatable("gui.exposure.item_rename.cancel")));
        addRenderableWidget(cancelButton);
    }

    private void confirm() {
        getMenu().clickMenuButton(Minecraft.getInstance().player, ItemRenameMenu.APPLY_BUTTON_ID);
        Objects.requireNonNull(Minecraft.getInstance().gameMode).handleInventoryButtonClick(getMenu().containerId, ItemRenameMenu.APPLY_BUTTON_ID);
        onClose();
    }

    private void cancel() {
        onClose();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        String string = this.name.getValue();
        this.init(minecraft, width, height);
        this.name.setValue(string);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        name.tick();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.name.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        super.renderTooltip(guiGraphics, x, y);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }


    private void onNameChanged(String name) {
        ItemStack itemStack = getMenu().getSlot(0).getItem();
        if (!itemStack.hasCustomHoverName() && name.equals(itemStack.getHoverName().getString())) {
            name = "";
        }

        if (getMenu().setItemName(name) && Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.connection.send(new ServerboundRenameItemPacket(name));
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            onClose();
        }

        if (keyCode != InputConstants.KEY_TAB && (name.keyPressed(keyCode, scanCode, modifiers) || name.canConsumeInput())) {
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == InputConstants.MOUSE_BUTTON_RIGHT && name.isMouseOver(mouseX, mouseY)) {
            name.setValue("");
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}
