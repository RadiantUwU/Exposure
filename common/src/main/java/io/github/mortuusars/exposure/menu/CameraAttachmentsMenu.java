package io.github.mortuusars.exposure.menu;

import com.google.common.base.Preconditions;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.camera.AttachmentType;
import io.github.mortuusars.exposure.item.CameraItem;
import io.github.mortuusars.exposure.util.ItemAndStack;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class CameraAttachmentsMenu extends AbstractContainerMenu {
    private final int attachmentSlotsCount;
    private final int cameraSlotIndex;
    private final Player player;
    private final ItemAndStack<CameraItem> camera;

    private boolean clientContentsInitialized;

    public CameraAttachmentsMenu(int containerId, Inventory playerInventory, int cameraSlotIndex) {
        super(Exposure.MenuTypes.CAMERA.get(), containerId);

        ItemStack cameraStack = playerInventory.items.get(cameraSlotIndex);
        Preconditions.checkState(cameraStack.getItem() instanceof CameraItem,
                "Failed to open Camera Attachments. " + cameraStack + " is not a CameraItem.");

        this.player = playerInventory.player;
        this.cameraSlotIndex = cameraSlotIndex;
        this.camera = new ItemAndStack<>(cameraStack);

        SimpleContainer container = new SimpleContainer(getCameraAttachments(camera).toArray(ItemStack[]::new)) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }
        };

        this.attachmentSlotsCount = addAttachmentSlots(container);
        addPlayerSlots(playerInventory);
    }

    public ItemAndStack<CameraItem> getCamera() {
        return camera;
    }

    /**
     * Only called client-side.
     */
    @Override
    public void initializeContents(int stateId, List<ItemStack> items, ItemStack carried) {
        clientContentsInitialized = false;
        super.initializeContents(stateId, items, carried);
        clientContentsInitialized = true;
    }

    protected int addAttachmentSlots(Container container) {
        int attachmentSlots = 0;

        int[][] slots = new int[][]{
                // SlotId, x, y, maxStackSize
                {CameraItem.FILM_ATTACHMENT.slot(), 13, 42, 1},
                {CameraItem.FLASH_ATTACHMENT.slot(), 147, 15, 1},
                {CameraItem.LENS_ATTACHMENT.slot(), 147, 43, 1},
                {CameraItem.FILTER_ATTACHMENT.slot(), 147, 71, 1}
        };

        for (int[] slot : slots) {
            Optional<AttachmentType> attachment = camera.getItem()
                    .getAttachmentTypeForSlot(camera.getStack(), slot[0]);

            if (attachment.isPresent()) {
                addSlot(new FilteredSlot(container, slot[0], slot[1], slot[2], slot[3],
                        this::onItemInSlotChanged, attachment.get().itemPredicate()));
                attachmentSlots++;
            }
        }

        return attachmentSlots;
    }

    protected void addPlayerSlots(Inventory playerInventory) {
        //Player Inventory
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, (column + row * 9) + 9, column * 18 + 8, 103 + row * 18){
                    @Override
                    public boolean mayPickup(@NotNull Player player) {
                        return super.mayPickup(player) && getContainerSlot() != cameraSlotIndex;
                    }

                    @Override
                    public boolean isActive() {
                        return getContainerSlot() != cameraSlotIndex;
                    }

                    @Override
                    public boolean isHighlightable() {
                        return getContainerSlot() != cameraSlotIndex;
                    }
                });
            }
        }

        //Hotbar
        for (int slot = 0; slot < 9; slot++) {
            int finalSlot = slot;
            addSlot(new Slot(playerInventory, finalSlot, slot * 18 + 8, 161) {
                @Override
                public boolean mayPickup(@NotNull Player player) {
                    return super.mayPickup(player) && getContainerSlot() != cameraSlotIndex;
                }

                @Override
                public boolean isActive() {
                    return getContainerSlot() != cameraSlotIndex;
                }

                @Override
                public boolean isHighlightable() {
                    return getContainerSlot() != cameraSlotIndex;
                }
            });
        }
    }

    protected void onItemInSlotChanged(FilteredSlot.SlotChangedArgs args) {
        int slotId = args.slot().getSlotId();
        ItemStack newStack = args.newStack();

        camera.getItem().getAttachmentTypeForSlot(camera.getStack(), slotId).ifPresent(type -> {
            camera.getItem().setAttachment(camera.getStack(), type, newStack);

            if (player.getLevel().isClientSide() && clientContentsInitialized)
                type.sound().playOnePerPlayer(player, newStack.isEmpty());
        });
    }

    private static NonNullList<ItemStack> getCameraAttachments(ItemAndStack<CameraItem> camera) {
        NonNullList<ItemStack> items = NonNullList.create();

        List<AttachmentType> attachmentTypes = camera.getItem().getAttachmentTypes(camera.getStack());
        for (AttachmentType attachmentType : attachmentTypes) {
            items.add(camera.getItem().getAttachment(camera.getStack(), attachmentType).orElse(ItemStack.EMPTY));
        }

        return items;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int slotIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot clickedSlot = this.slots.get(slotIndex);
        if (clickedSlot.hasItem()) {
            ItemStack slotStack = clickedSlot.getItem();
            itemstack = slotStack.copy();
            if (slotIndex < attachmentSlotsCount) {
                if (!this.moveItemStackTo(slotStack, attachmentSlotsCount, this.slots.size(), true))
                    return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(slotStack, 0, attachmentSlotsCount, false))
                    return ItemStack.EMPTY;
            }

            if (slotStack.isEmpty())
                clickedSlot.set(ItemStack.EMPTY);
            else
                clickedSlot.setChanged();
        }

        return itemstack;
    }

    /**
     * Fixed method to respect slot photo limit.
     */
    @Override
    protected boolean moveItemStackTo(ItemStack movedStack, int startIndex, int endIndex, boolean reverseDirection) {
        boolean hasRemainder = false;
        int i = startIndex;
        if (reverseDirection) {
            i = endIndex - 1;
        }
        if (movedStack.isStackable()) {
            while (!movedStack.isEmpty() && !(!reverseDirection ? i >= endIndex : i < startIndex)) {
                Slot slot = this.slots.get(i);
                ItemStack slotStack = slot.getItem();
                if (!slotStack.isEmpty() && ItemStack.isSameItemSameTags(movedStack, slotStack)) {
                    int maxSize;
                    int j = slotStack.getCount() + movedStack.getCount();
                    if (j <= (maxSize = Math.min(slot.getMaxStackSize(), movedStack.getMaxStackSize()))) {
                        movedStack.setCount(0);
                        slotStack.setCount(j);
                        slot.setChanged();
                        hasRemainder = true;
                    } else if (slotStack.getCount() < maxSize) {
                        movedStack.shrink(maxSize - slotStack.getCount());
                        slotStack.setCount(maxSize);
                        slot.setChanged();
                        hasRemainder = true;
                    }
                }
                if (reverseDirection) {
                    --i;
                    continue;
                }
                ++i;
            }
        }
        if (!movedStack.isEmpty()) {
            i = reverseDirection ? endIndex - 1 : startIndex;
            while (!(!reverseDirection ? i >= endIndex : i < startIndex)) {
                Slot slot1 = this.slots.get(i);
                ItemStack movedStack1 = slot1.getItem();
                if (movedStack1.isEmpty() && slot1.mayPlace(movedStack)) {
                    if (movedStack.getCount() > slot1.getMaxStackSize()) {
                        slot1.set(movedStack.split(slot1.getMaxStackSize()));
                    } else {
                        slot1.set(movedStack.split(movedStack.getCount()));
                    }
                    slot1.setChanged();
                    hasRemainder = true;
                    break;
                }
                if (reverseDirection) {
                    --i;
                    continue;
                }
                ++i;
            }
        }
        return hasRemainder;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);

        camera.getItem().setDisassembled(camera.getStack(), false);

        // Without this, client inventory is syncing properly when menu is closed. (only when opened by r-click in GUI)
        player.inventoryMenu.resumeRemoteUpdates();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return player.getInventory().getItem(cameraSlotIndex).equals(camera.getStack());
    }

    public static CameraAttachmentsMenu fromBuffer(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        return new CameraAttachmentsMenu(containerId, playerInventory, buffer.readInt());
    }
}
