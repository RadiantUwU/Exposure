package io.github.mortuusars.exposure.integration.jade.component_provider;

import io.github.mortuusars.exposure.block.entity.Lightroom;
import io.github.mortuusars.exposure.block.entity.LightroomBlockEntity;
import io.github.mortuusars.exposure.integration.jade.ExposureJadePlugin;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.Nullable;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;
import snownee.jade.impl.ui.ProgressArrowElement;

import java.util.List;

public enum LightroomComponentProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor>,
        IServerExtensionProvider<LightroomBlockEntity, ItemStack> {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
        CompoundTag tag = blockAccessor.getServerData();

        if (tag.getBoolean("Empty"))
            return;

        IElementHelper helper = IElementHelper.get();

        tooltip.add(helper.spacer(0, 0));

        ItemStack film = ItemStack.of(tag.getCompound("Film"));
        if (!film.isEmpty()) {
            tooltip.append(helper.item(film));
            tooltip.append(helper.text(Component.literal("|").withStyle(ChatFormatting.GRAY))
                    .size(new Vec2(11, 12))
                    .translate(new Vec2(5, 6))
                    .message(null));
        }

        ItemStack paper = ItemStack.of(tag.getCompound("Paper"));
        if (!paper.isEmpty()) {
            tooltip.append(helper.item(paper));
            tooltip.append(helper.text(Component.literal("+").withStyle(ChatFormatting.GRAY))
                    .size(new Vec2(12, 12))
                    .translate(new Vec2(5, 6))
                    .message(null));
        }

        for (String dye : new String[] {"Cyan", "Yellow", "Magenta", "Black"}) {
            ItemStack stack = ItemStack.of(tag.getCompound(dye));
            if (!stack.isEmpty())
                tooltip.append(helper.item(stack));
        }

//        tooltip.append(helper.item(ItemStack.of(tag.getCompound("Cyan"))));
//        tooltip.append(helper.item(ItemStack.of(tag.getCompound("Yellow"))));
//        tooltip.append(helper.item(ItemStack.of(tag.getCompound("Magenta"))));
//        tooltip.append(helper.item(ItemStack.of(tag.getCompound("Black"))));

        tooltip.append(new ProgressArrowElement(tag.getFloat("Progress")));

        tooltip.append(helper.item(ItemStack.of(tag.getCompound("Result"))));


        Lightroom.Process process = Lightroom.Process.fromStringOrDefault(tag.getString("Process"), Lightroom.Process.REGULAR);
        if (process != Lightroom.Process.REGULAR)
            tooltip.add(helper.text(Component.translatable("gui.exposure.lightroom.process." + process.getSerializedName())));

        tooltip.add(helper.spacer(0, 2));
    }

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor blockAccessor) {
        if (blockAccessor.getBlockEntity() instanceof LightroomBlockEntity lightroomBlockEntity) {
            if (lightroomBlockEntity.isEmpty()) {
                tag.putBoolean("Empty", true);
                return;
            }

            tag.put("Film", lightroomBlockEntity.getItem(Lightroom.FILM_SLOT).save(new CompoundTag()));
            tag.put("Paper", lightroomBlockEntity.getItem(Lightroom.PAPER_SLOT).save(new CompoundTag()));
            tag.put("Cyan", lightroomBlockEntity.getItem(Lightroom.CYAN_SLOT).save(new CompoundTag()));
            tag.put("Yellow", lightroomBlockEntity.getItem(Lightroom.YELLOW_SLOT).save(new CompoundTag()));
            tag.put("Magenta", lightroomBlockEntity.getItem(Lightroom.MAGENTA_SLOT).save(new CompoundTag()));
            tag.put("Black", lightroomBlockEntity.getItem(Lightroom.BLACK_SLOT).save(new CompoundTag()));
            tag.put("Result", lightroomBlockEntity.getItem(Lightroom.RESULT_SLOT).save(new CompoundTag()));

            tag.putString("Process", lightroomBlockEntity.getProcess().getSerializedName());

            tag.putFloat("Progress", lightroomBlockEntity.getProgressPercentage());
        }
    }

    @Override
    public @Nullable List<ViewGroup<ItemStack>> getGroups(ServerPlayer serverPlayer, ServerLevel serverLevel, LightroomBlockEntity lightroomBlockEntity, boolean b) {
        return null;
    }

    @Override
    public ResourceLocation getUid() {
        return ExposureJadePlugin.LIGHTROOM;
    }
}
