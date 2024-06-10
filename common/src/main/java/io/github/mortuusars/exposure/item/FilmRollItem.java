package io.github.mortuusars.exposure.item;

import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.PlatformHelper;
import io.github.mortuusars.exposure.camera.infrastructure.FilmType;
import io.github.mortuusars.exposure.gui.ClientGUI;
import io.github.mortuusars.exposure.menu.ItemRenameMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FilmRollItem extends Item implements IFilmItem {
    private final FilmType filmType;
    private final int barColor;

    public FilmRollItem(FilmType filmType, int barColor, Properties properties) {
        super(properties);
        this.filmType = filmType;
        this.barColor = barColor;
    }

    @Override
    public FilmType getType() {
        return filmType;
    }

    public boolean isBarVisible(@NotNull ItemStack stack) {
        return getExposedFramesCount(stack) > 0;
    }

    public int getBarWidth(@NotNull ItemStack stack) {
        return Math.min(1 + 12 * getExposedFramesCount(stack) / getMaxFrameCount(stack), 13);
    }

    public int getBarColor(@NotNull ItemStack stack) {
        return barColor;
    }

    public void addFrame(ItemStack filmStack, CompoundTag frame) {
        CompoundTag tag = filmStack.getOrCreateTag();

        if (!tag.contains(FRAMES_TAG, Tag.TAG_LIST)) {
            tag.put(FRAMES_TAG, new ListTag());
        }

        ListTag listTag = tag.getList(FRAMES_TAG, Tag.TAG_COMPOUND);

        if (listTag.size() >= getMaxFrameCount(filmStack))
            throw new IllegalStateException("Cannot add more frames than film could fit. Size: " + listTag.size());

        listTag.add(frame);
        tag.put(FRAMES_TAG, listTag);
    }

    public boolean canAddFrame(ItemStack filmStack) {
        if (!filmStack.hasTag() || !filmStack.getOrCreateTag().contains(FRAMES_TAG, Tag.TAG_LIST))
            return true;

        return filmStack.getOrCreateTag().getList(FRAMES_TAG, Tag.TAG_COMPOUND).size() < getMaxFrameCount(filmStack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced) {
        int exposedFrames = getExposedFramesCount(stack);
        if (exposedFrames > 0) {
            int totalFrames = getMaxFrameCount(stack);
            tooltipComponents.add(Component.translatable("item.exposure.film_roll.tooltip.frame_count", exposedFrames, totalFrames)
                    .withStyle(ChatFormatting.GRAY));
        }

        int frameSize = getFrameSize(stack);
        if (frameSize != getDefaultFrameSize()) {
            tooltipComponents.add(Component.translatable("item.exposure.film_roll.tooltip.frame_size",
                    Component.literal(String.format("%.1f", frameSize / 10f)))
                            .withStyle(ChatFormatting.GRAY));
        }

        if (Config.Common.FILM_ROLL_RENAMING.get()) {
            tooltipComponents.add(Component.translatable("item.exposure.film_roll.tooltip.renaming"));
        }

        // Create compat:
        int developingStep = stack.getTag() != null ? stack.getTag().getInt("CurrentDevelopingStep") : 0;
        if (Config.Common.CREATE_SPOUT_DEVELOPING_ENABLED.get() && developingStep > 0) {
            List<? extends String> totalSteps = Config.Common.spoutDevelopingSequence(getType()).get();

            MutableComponent stepsComponent = Component.literal("");

            for (int i = 0; i < developingStep; i++) {
                stepsComponent.append(Component.literal("I").withStyle(ChatFormatting.GOLD));
            }

            for (int i = developingStep; i < totalSteps.size(); i++) {
                stepsComponent.append(Component.literal("I").withStyle(ChatFormatting.DARK_GRAY));
            }

            tooltipComponents.add(Component.translatable("item.exposure.film_roll.tooltip.developing_step", stepsComponent)
                    .withStyle(ChatFormatting.GOLD));
        }

        //noinspection ConstantValue
        if (exposedFrames > 0 && !PlatformHelper.isModLoaded("jei") && Config.Client.RECIPE_TOOLTIPS_WITHOUT_JEI.get()) {
            ClientGUI.addFilmRollDevelopingTooltip(stack, level, tooltipComponents, isAdvanced);
        }
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (!Config.Common.FILM_ROLL_RENAMING.get() || !(player instanceof ServerPlayer serverPlayer)) {
            return super.use(level, player, usedHand);
        }

        int slot = getMatchingSlotInInventory(player.getInventory(), player.getItemInHand(usedHand));
        MenuProvider menuProvider = new MenuProvider() {
            @Override
            public @NotNull Component getDisplayName() {
                return Component.translatable("gui.exposure.item_rename.title");
            }

            @Override
            public @NotNull AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
                return new ItemRenameMenu(containerId, playerInventory, slot);
            }
        };
        PlatformHelper.openMenu(serverPlayer, menuProvider, buffer -> buffer.writeInt(slot));
        return InteractionResultHolder.success(player.getItemInHand(usedHand));
    }

    protected int getMatchingSlotInInventory(Inventory inventory, ItemStack stack) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).equals(stack)) {
                return i;
            }
        }
        return -1;
    }
}
