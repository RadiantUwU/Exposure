package io.github.mortuusars.exposure.item;

import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.PlatformHelper;
import io.github.mortuusars.exposure.camera.infrastructure.FrameData;
import io.github.mortuusars.exposure.gui.ClientGUI;
import io.github.mortuusars.exposure.gui.component.PhotographTooltip;
import io.github.mortuusars.exposure.util.ItemAndStack;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class PhotographItem extends Item {
    public PhotographItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack stack) {
        return FrameData.hasIdOrTexture(stack) ? Optional.of(new PhotographTooltip(stack)) : Optional.empty();
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced) {
        if (stack.getTag() == null) {
            return;
        }

        int generation = stack.getTag().getInt("generation");
        if (generation > 0)
            tooltipComponents.add(Component.translatable("item.exposure.photograph.generation." + generation)
                    .withStyle(ChatFormatting.GRAY));

        String photographerName = stack.getTag().getString(FrameData.PHOTOGRAPHER);
        if (!photographerName.isEmpty() && Config.Client.PHOTOGRAPH_SHOW_PHOTOGRAPHER_IN_TOOLTIP.get()) {
            tooltipComponents.add(Component.translatable("item.exposure.photograph.photographer_tooltip",
                            Component.literal(photographerName).withStyle(ChatFormatting.WHITE))
                    .withStyle(ChatFormatting.GRAY));
        }

        // The value is not constant here
        //noinspection ConstantValue
        if (generation < 2 && !PlatformHelper.isModLoaded("jei") && Config.Client.RECIPE_TOOLTIPS_WITHOUT_JEI.get()) {
            ClientGUI.addPhotographCopyingTooltip(stack, level, tooltipComponents, isAdvanced);
        }

        if (isAdvanced.isAdvanced()) {
            String str = FrameData.getIdOrTexture(stack.getTag()).map(
                    id -> "Exposure Id: " + id,
                    texture -> "Texture: " + texture);
            tooltipComponents.add(Component.literal(str).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack itemInHand = player.getItemInHand(hand);

        if (!FrameData.hasIdOrTexture(itemInHand)) {
            Exposure.LOGGER.warn("No Id or Texture is defined. - {}", itemInHand);
            return InteractionResultHolder.pass(itemInHand);
        }

        if (level.isClientSide) {
            ClientGUI.openPhotographScreen(List.of(new ItemAndStack<>(itemInHand)));
            player.playSound(Exposure.SoundEvents.PHOTOGRAPH_RUSTLE.get(), 0.6f, 1.1f);
        }

        return InteractionResultHolder.success(itemInHand);
    }

    @Override
    public boolean overrideOtherStackedOnMe(@NotNull ItemStack stack, @NotNull ItemStack other, @NotNull Slot slot, @NotNull ClickAction action, @NotNull Player player, @NotNull SlotAccess access) {
        if (action != ClickAction.SECONDARY)
            return false;

        if (other.getItem() instanceof PhotographItem) {
            StackedPhotographsItem stackedPhotographsItem = Exposure.Items.STACKED_PHOTOGRAPHS.get();
            ItemStack stackedPhotographsStack = new ItemStack(stackedPhotographsItem);

            stackedPhotographsItem.addPhotographOnTop(stackedPhotographsStack, stack);
            stackedPhotographsItem.addPhotographOnTop(stackedPhotographsStack, other);
            slot.set(stackedPhotographsStack);
            access.set(ItemStack.EMPTY);

            StackedPhotographsItem.playAddSoundClientside(player);

            return true;
        }

        return false;
    }
}
