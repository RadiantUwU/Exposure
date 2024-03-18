package io.github.mortuusars.exposure.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ChromaticFragmentItem extends Item {
    public static final String EXPOSURES_TAG = "Exposures";
    public ChromaticFragmentItem(Properties properties) {
        super(properties);
    }

    public List<CompoundTag> getExposures(ItemStack stack) {
        if (stack.getTag() == null || !stack.getTag().contains(EXPOSURES_TAG, Tag.TAG_LIST))
            return Collections.emptyList();

        ListTag channelsList = stack.getTag().getList(EXPOSURES_TAG, Tag.TAG_COMPOUND);
        return channelsList.stream().map(t -> (CompoundTag)t).collect(Collectors.toList());
    }

    public void addExposure(ItemStack stack, CompoundTag frame) {
        ListTag channelsList = getOrCreateExposuresTag(stack);
        channelsList.add(frame);
        stack.getOrCreateTag().put(EXPOSURES_TAG, channelsList);
    }

    private ListTag getOrCreateExposuresTag(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        ListTag list = tag.getList(EXPOSURES_TAG, Tag.TAG_COMPOUND);
        tag.put(EXPOSURES_TAG, list);
        return list;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {

        List<CompoundTag> exposures = getExposures(stack);

        if (exposures.size() >= 1)
            tooltipComponents.add(Component.literal("■").withStyle(ChatFormatting.RED));
        if (exposures.size() >= 2)
            tooltipComponents.add(Component.literal("■").withStyle(ChatFormatting.GREEN));
        if (exposures.size() >= 3)
            tooltipComponents.add(Component.literal("■").withStyle(ChatFormatting.BLUE));
    }
}
