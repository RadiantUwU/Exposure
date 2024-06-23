package io.github.mortuusars.exposure.util;

import io.github.mortuusars.exposure.camera.Camera;
import io.github.mortuusars.exposure.item.CameraItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class CameraInHand<T extends CameraItem> extends Camera<T> {
    private final InteractionHand hand;
    private final ItemAndStack<T> cameraItemAndStack;

    public static <T extends CameraItem> @Nullable Camera<T> ofPlayer(Player player, Class<T> clazz) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack itemInHand = player.getItemInHand(hand);
            if (itemInHand.getItem() instanceof CameraItem cameraItem && cameraItem.isActive(itemInHand)
                    && itemInHand.getItem().getClass().equals(clazz)) {
                return new CameraInHand<>(hand, new ItemAndStack<>(itemInHand));
            }
        }
        return null;
    }

    public CameraInHand(InteractionHand hand, @Nullable ItemAndStack<T> cameraItemAndStack) {
        this.hand = hand;
        this.cameraItemAndStack = cameraItemAndStack;
    }

    @Override
    public void activate(Player player) {
        get().getItem().activate(player, get().getStack());
    }

    @Override
    public void deactivate(Player player) {
        get().getItem().deactivate(player, get().getStack());
    }

    @Override
    public ItemAndStack<T> get() {
        return cameraItemAndStack;
    }

    public InteractionHand getHand() {
        return hand;
    }
}
