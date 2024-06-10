package io.github.mortuusars.exposure.menu;

import com.google.common.base.Preconditions;
import io.github.mortuusars.exposure.Exposure;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ItemRenameMenu extends AbstractContainerMenu {
    public static final int MAX_NAME_LENGTH = AnvilMenu.MAX_NAME_LENGTH;
    public static final int APPLY_BUTTON_ID = 0;

    protected final Inventory playerInventory;
    protected final int slot;
    protected final Item item;

    protected String itemName;

    public ItemRenameMenu(int containerId, Inventory playerInventory, int slot) {
        super(Exposure.MenuTypes.ITEM_RENAME.get(), containerId);
        this.playerInventory = playerInventory;
        this.slot = slot;
        ItemStack itemStack = playerInventory.getItem(slot);
        Preconditions.checkArgument(!itemStack.isEmpty(), "Cannot rename empty item in slot " + slot);
        this.item = itemStack.getItem();
        this.itemName = itemStack.getHoverName().getString();

        SimpleContainer container = new SimpleContainer(itemStack);
        addSlot(new Slot(container, 0, 4, 16) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
            @Override
            public boolean mayPickup(Player player) { return false; }
            @Override
            public boolean isHighlightable() { return false; }
        });
    }

    public static ItemRenameMenu fromBuffer(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        return new ItemRenameMenu(containerId, playerInventory, buffer.readInt());
    }

    public Player getPlayer() {
        return playerInventory.player;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return item.getClass().isAssignableFrom(player.getInventory().getItem(slot).getItem().getClass());
    }

    public void updateResult() {
        ItemStack itemStack = getSlot(0).getItem();
        ItemStack itemStack2 = itemStack.copy();

        if (Util.isBlank(itemName)) {
            if (itemStack.hasCustomHoverName()) {
                itemStack2.resetHoverName();
            }
        } else if (!itemName.equals(itemStack.getHoverName().getString())) {
            itemStack2.setHoverName(Component.literal(itemName));
        }

        getSlot(0).set(itemStack2);
        broadcastChanges();
    }

    public String getItemName() {
        return itemName;
    }

    public boolean setItemName(String itemName) {
        String string = validateName(itemName);
        if (string == null || !string.equals(itemName)) {
            return false;
        }

        this.itemName = string;

//        if (this.getSlot(2).hasItem()) {
//            ItemStack itemStack = this.getSlot(2).getItem();
//            if (Util.isBlank(string)) {
//                itemStack.resetHoverName();
//            } else {
//                itemStack.setHoverName(Component.literal(string));
//            }
//        }

        updateResult();
        return true;
    }

    @Nullable
    public String validateName(String itemName) {
        String string = SharedConstants.filterText(itemName);
        if (string.length() <= MAX_NAME_LENGTH) {
            return string;
        }
        return null;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == APPLY_BUTTON_ID) {
            playerInventory.setItem(slot, getSlot(0).getItem());
        }
        return super.clickMenuButton(player, id);
    }
}
