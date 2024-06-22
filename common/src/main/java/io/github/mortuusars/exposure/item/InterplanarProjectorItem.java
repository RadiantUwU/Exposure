package io.github.mortuusars.exposure.item;

import io.github.mortuusars.exposure.Exposure;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class InterplanarProjectorItem extends Item {
    public InterplanarProjectorItem(Properties properties) {
        super(properties);
    }

    public boolean isDithered(ItemStack stack) {
        return stack.getTag() == null || !stack.getTag().getBoolean("Clean");
    }

    public void setDithered(ItemStack stack, boolean dithered) {
        stack.getOrCreateTag().putBoolean("Clean", !dithered);
    }

    public boolean isConsumable(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> components, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, components, isAdvanced);

        if (isDithered(stack)){
            components.add(Component.translatable("item.exposure.interplanar_projector.mode.dithered"));
        }
        else {
            components.add(Component.translatable("item.exposure.interplanar_projector.mode.clear"));
        }

        if (Screen.hasShiftDown()) {
            if (isConsumable(stack)) {
                components.add(Component.translatable("item.exposure.interplanar_projector.tooltip.consumed_info"));
            }
            components.add(Component.translatable("item.exposure.interplanar_projector.tooltip.info"));
            components.add(Component.translatable("item.exposure.interplanar_projector.tooltip.switch_info"));
        }
        else {
            components.add(Component.translatable("tooltip.exposure.hold_for_details"));
        }
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (other.isEmpty() && action == ClickAction.SECONDARY) {
            setDithered(stack, !isDithered(stack));
            if (player.level().isClientSide) {
                player.playSound(Exposure.SoundEvents.CAMERA_GENERIC_CLICK.get(), 0.6f, 1f);
            }
            return true;
        }

        return super.overrideOtherStackedOnMe(stack, other, slot, action, player, access);
    }

    public Optional<String> getFilename(ItemStack stack) {
        return stack.hasCustomHoverName() ? Optional.of(stack.getHoverName().getString()) : Optional.empty();
    }
}
