package io.github.mortuusars.exposure.item;

import com.google.common.base.Preconditions;
import com.mojang.blaze3d.platform.NativeImage;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.ExposureServer;
import io.github.mortuusars.exposure.camera.capture.processing.FloydDither;
import io.github.mortuusars.exposure.camera.infrastructure.FilmType;
import io.github.mortuusars.exposure.camera.infrastructure.FrameData;
import io.github.mortuusars.exposure.data.storage.ExposureSavedData;
import io.github.mortuusars.exposure.network.Packets;
import io.github.mortuusars.exposure.network.packet.client.WaitForExposureChangeS2CP;
import io.github.mortuusars.exposure.util.ColorChannel;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ChromaticSheetItem extends Item {
    public static final String EXPOSURES_TAG = "Exposures";

    public ChromaticSheetItem(Properties properties) {
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

        if (!exposures.isEmpty()) {
            MutableComponent component = Component.translatable("gui.exposure.channel.red")
                    .withStyle(Style.EMPTY.withColor(ColorChannel.RED.getRepresentationColor()));

            if (exposures.size() >= 2){
                component.append(Component.translatable("gui.exposure.channel.separator").withStyle(ChatFormatting.GRAY));
                component.append(Component.translatable("gui.exposure.channel.green")
                        .withStyle(Style.EMPTY.withColor(ColorChannel.GREEN.getRepresentationColor())));
            }

            if (exposures.size() >= 3) {
                component.append(Component.translatable("gui.exposure.channel.separator").withStyle(ChatFormatting.GRAY));
                component.append(Component.translatable("gui.exposure.channel.blue")
                        .withStyle(Style.EMPTY.withColor(ColorChannel.BLUE.getRepresentationColor())));
            }

            tooltipComponents.add(component);

            if (exposures.size() >= 3) {
                tooltipComponents.add(Component.translatable("item.exposure.chromatic_sheet.use_tooltip").withStyle(ChatFormatting.GRAY));
            }
        }
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        if (!level.isClientSide && getExposures(stack).size() >= 3) {
            ItemStack result = finalize(level, stack);
            player.setItemInHand(usedHand, result);
            return InteractionResultHolder.success(result);
        }

        return super.use(level, player, usedHand);
    }

    public ItemStack finalize(@NotNull Level level, ItemStack stack) {
        Preconditions.checkState(!level.isClientSide, "Can only finalize server-side.");

        List<CompoundTag> exposures = getExposures(stack);

        Preconditions.checkState(exposures.size() >= 3, 
                "Finalizing Chromatic Fragment requires 3 exposures. " + stack);

        CompoundTag redTag = exposures.get(0);
        String redId = redTag.getString(FrameData.ID);
        Optional<ExposureSavedData> redOpt = ExposureServer.getExposureStorage().getOrQuery(redId);
        if (redOpt.isEmpty()) {
            Exposure.LOGGER.error("Cannot create Chromatic Photograph: Red channel exposure '{}' is not found.", redId);
            return stack;
        }

        CompoundTag greenTag = exposures.get(1);
        String greenId = greenTag.getString(FrameData.ID);
        Optional<ExposureSavedData> greenOpt = ExposureServer.getExposureStorage().getOrQuery(greenId);
        if (greenOpt.isEmpty()) {
            Exposure.LOGGER.error("Cannot create Chromatic Photograph: Green channel exposure '{}' is not found.", greenId);
            return stack;
        }

        CompoundTag blueTag = exposures.get(2);
        String blueId = blueTag.getString(FrameData.ID);
        Optional<ExposureSavedData> blueOpt = ExposureServer.getExposureStorage().getOrQuery(blueId);
        if (blueOpt.isEmpty()) {
            Exposure.LOGGER.error("Cannot create Chromatic Photograph: Blue channel exposure '{}' is not found.", blueId);
            return stack;
        }

        String name;
        int underscoreIndex = redId.lastIndexOf("_");
        if (underscoreIndex != -1)
            name = redId.substring(0, underscoreIndex);
        else
            name = Integer.toString(redId.hashCode());

        String id = String.format("%s_chromatic_%s", name, level.getGameTime());
        
        ItemStack photograph = new ItemStack(Exposure.Items.PHOTOGRAPH.get());

        // It would probably be better to make a tag that contains properties common to all 3 tags,
        // but it's tricky to implement, and it wouldn't be noticed most of the time.
        CompoundTag tag = redTag.copy();
        tag = tag.merge(greenTag);
        tag = tag.merge(blueTag);

        tag.remove(FrameData.CHROMATIC_CHANNEL);

        tag.putString(FrameData.ID, id);
        tag.putBoolean(FrameData.CHROMATIC, true);
        tag.putString(FrameData.RED_CHANNEL, redId);
        tag.putString(FrameData.GREEN_CHANNEL, greenId);
        tag.putString(FrameData.BLUE_CHANNEL, blueId);
        tag.putString(FrameData.TYPE, FilmType.COLOR.getSerializedName());

        photograph.setTag(tag);

        Packets.sendToAllClients(new WaitForExposureChangeS2CP(id));

        new Thread(() -> {
            try {
                processAndSaveTrichrome(redOpt.get(), greenOpt.get(), blueOpt.get(), id);
            } catch (Exception e) {
                Exposure.LOGGER.error("Cannot process and save Chromatic Photograph: {}", e.toString());
            }
        }).start();

        return photograph;
    }

    //TODO: move to dedicated class
    protected void processAndSaveTrichrome(ExposureSavedData red, ExposureSavedData green, ExposureSavedData blue, String id) {
        int width = Math.min(red.getWidth(), Math.min(green.getWidth(), blue.getWidth()));
        int height = Math.min(red.getHeight(), Math.min(green.getHeight(), blue.getHeight()));
        if (width <= 0 ||height <= 0) {
            Exposure.LOGGER.error("Cannot create Chromatic Photograph: Width and Height should be larger than 0. " +
                    "Width '{}', Height: '{}'.", width, height);
            return;
        }

        NativeImage image = new NativeImage(width, height, false);

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int r = MapColor.getColorFromPackedId(red.getPixel(x, y)) >> 16 & 0xFF;
                int g = MapColor.getColorFromPackedId(green.getPixel(x, y)) >> 8 & 0xFF;
                int b = MapColor.getColorFromPackedId(blue.getPixel(x, y)) & 0xFF;

                int rgb = 0xFF << 24 | r << 16 | g << 8 | b;

                image.setPixelRGBA(x, y, rgb);
            }
        }

        byte[] mapColorPixels = FloydDither.ditherWithMapColors(image);
        image.close();

        CompoundTag properties = new CompoundTag();
        properties.putString(ExposureSavedData.TYPE_PROPERTY, FilmType.COLOR.getSerializedName());
        long unixTime = System.currentTimeMillis() / 1000L;
        properties.putLong(ExposureSavedData.TIMESTAMP_PROPERTY, unixTime);

        ExposureSavedData resultData = new ExposureSavedData(image.getWidth(), image.getHeight(), mapColorPixels, properties);
        ExposureServer.getExposureStorage().put(id, resultData);

        // Because we save exposure off-thread, and item was already created before chromatic processing has even begun -
        // we need to update clients, otherwise, client wouldn't know that and will think that the exposure is missing.
        ExposureServer.getExposureStorage().sendExposureChanged(id);
    }
}
