package io.github.mortuusars.exposure.item;

import com.google.common.base.Preconditions;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.ExposureServer;
import io.github.mortuusars.exposure.camera.capture.converter.DitheringColorConverter;
import io.github.mortuusars.exposure.camera.infrastructure.FilmType;
import io.github.mortuusars.exposure.camera.infrastructure.FrameData;
import io.github.mortuusars.exposure.data.storage.ExposureSavedData;
import io.github.mortuusars.exposure.network.Packets;
import io.github.mortuusars.exposure.network.packet.client.CreateChromaticExposureS2CP;
import io.github.mortuusars.exposure.network.packet.client.WaitForExposureChangeS2CP;
import io.github.mortuusars.exposure.render.ExposureDataImage;
import io.github.mortuusars.exposure.render.IImage;
import io.github.mortuusars.exposure.util.ColorChannel;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.FastColor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
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
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (entity instanceof ServerPlayer player
                && stack.getTag() != null && stack.getTag().getBoolean("RequiresDeferredFinalization")) {
            ItemStack finalizedItem = finalize(level, stack, player.getScoreboardName(), player);
            player.getInventory().setItem(slotId, finalizedItem);
        }
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        if (!level.isClientSide && getExposures(stack).size() >= 3) {
            ItemStack result = finalize(level, stack, player.getScoreboardName(),
                    player instanceof ServerPlayer serverPlayer ? serverPlayer : null);
            player.setItemInHand(usedHand, result);
            return InteractionResultHolder.success(result);
        }

        return super.use(level, player, usedHand);
    }

    public ItemStack finalize(@NotNull Level level, ItemStack stack, String idPrefix, @Nullable ServerPlayer player) {
        Preconditions.checkState(!level.isClientSide, "Can only finalize server-side.");

        List<CompoundTag> exposures = getExposures(stack);

        Preconditions.checkState(exposures.size() >= 3, 
                "Finalizing Chromatic Fragment requires 3 exposures. " + stack);

        CompoundTag redFrame = exposures.get(0);
        CompoundTag greenFrame = exposures.get(1);
        CompoundTag blueFrame = exposures.get(2);

        String exposureId = getChromaticExposureId(level, idPrefix);

        ItemStack photograph = new ItemStack(Exposure.Items.PHOTOGRAPH.get());

        // It would probably be better to make a tag that contains properties common to all 3 tags,
        // but it's tricky to implement, and it wouldn't be noticed most of the time.
        CompoundTag tag = redFrame.copy();
        tag = tag.merge(greenFrame);
        tag = tag.merge(blueFrame);

        tag.remove(FrameData.CHROMATIC_CHANNEL);

        tag.putString(FrameData.ID, exposureId);
        tag.putBoolean(FrameData.CHROMATIC, true);
        tag.putString(FrameData.RED_CHANNEL, getFrameName(redFrame));
        tag.putString(FrameData.GREEN_CHANNEL, getFrameName(greenFrame));
        tag.putString(FrameData.BLUE_CHANNEL, getFrameName(blueFrame));
        tag.putString(FrameData.TYPE, FilmType.COLOR.getSerializedName());

        photograph.setTag(tag);

        boolean hasTexture = hasTextureFrame(redFrame, greenFrame, blueFrame);

        if (!hasTexture) {
            return finalizeServerside(stack, photograph, level, exposureId, redFrame, greenFrame, blueFrame);
        }
        else {
            if (player != null) {
                Packets.sendToClient(new CreateChromaticExposureS2CP(redFrame, greenFrame, blueFrame, exposureId), player);
                return photograph;
            }
            else {
                stack.getOrCreateTag().putBoolean("RequiresDeferredFinalization", true);
                return stack;
            }
        }
    }

    private static @NotNull String getChromaticExposureId(@NotNull Level level, String idPrefix) {
        return String.format("%s_chromatic_%s", idPrefix, level.getGameTime());
    }

    protected String getFrameName(CompoundTag frame) {
        if (frame.contains(FrameData.ID))
            return frame.getString(FrameData.ID);

        if (frame.contains(FrameData.TEXTURE))
            return frame.getString(FrameData.TEXTURE);

        return "unknown";
    }

    protected boolean hasTextureFrame(CompoundTag red, CompoundTag green, CompoundTag blue) {
        if (!red.contains(FrameData.ID) && red.contains(FrameData.TEXTURE))
            return true;

        if (!green.contains(FrameData.ID) && green.contains(FrameData.TEXTURE))
            return true;

        if (!blue.contains(FrameData.ID) && blue.contains(FrameData.TEXTURE))
            return true;

        return false;
    }

    protected ItemStack finalizeServerside(ItemStack chromaticSheetStack, ItemStack photographStack, @NotNull Level level, String exposureId, CompoundTag redFrame, CompoundTag greenFrame, CompoundTag blueFrame) {
        @Nullable IImage redImage = getExposureImage(redFrame);
        if (redImage == null) {
            Exposure.LOGGER.error("Cannot create Chromatic Photograph: Red channel image is not found in frame {}.", redFrame);
            return chromaticSheetStack;
        }

        @Nullable IImage greenImage = getExposureImage(greenFrame);
        if (greenImage == null) {
            Exposure.LOGGER.error("Cannot create Chromatic Photograph: Green channel image is not found in frame {}.", greenFrame);
            return chromaticSheetStack;
        }

        @Nullable IImage blueImage = getExposureImage(blueFrame);
        if (blueImage == null) {
            Exposure.LOGGER.error("Cannot create Chromatic Photograph: Blue channel image is not found in frame {}.", blueFrame);
            return chromaticSheetStack;
        }

        Packets.sendToAllClients(new WaitForExposureChangeS2CP(exposureId));

        new Thread(() -> {
            try {
                processAndSaveTrichrome(redImage, greenImage, blueImage, exposureId);
            } catch (Exception e) {
                Exposure.LOGGER.error("Cannot process and save Chromatic Photograph: {}", e.toString());
            }
        }).start();

        return photographStack;
    }

    protected @Nullable IImage getExposureImage(CompoundTag frame) {
        if (frame.contains(FrameData.ID, Tag.TAG_STRING)) {
            String exposureId = frame.getString(FrameData.ID);
            return ExposureServer.getExposureStorage().getOrQuery(exposureId)
                    .map(data -> new ExposureDataImage(exposureId, data))
                    .orElse(null);
        }
        return null;
    }

    @SuppressWarnings("UnusedReturnValue")
    protected boolean processAndSaveTrichrome(IImage red, IImage green, IImage blue, String id) {
        int width = Math.min(red.getWidth(), Math.min(green.getWidth(), blue.getWidth()));
        int height = Math.min(red.getHeight(), Math.min(green.getHeight(), blue.getHeight()));
        if (width <= 0 ||height <= 0) {
            Exposure.LOGGER.error("Cannot create Chromatic Photograph: Width and Height should be larger than 0. " +
                    "Width '{}', Height: '{}'.", width, height);
            return false;
        }

        int[][] pixels = new int[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int a = FastColor.ABGR32.alpha(red.getPixelABGR(x, y));
                int b = FastColor.ABGR32.blue(blue.getPixelABGR(x, y));
                int g = FastColor.ABGR32.green(green.getPixelABGR(x, y));
                int r = FastColor.ABGR32.red(red.getPixelABGR(x, y));

                int abgr = FastColor.ARGB32.color(a, r, g, b);

                pixels[y][x] = abgr;
            }
        }

        byte[] mapColorPixels = new DitheringColorConverter().convert(pixels);

//        try (NativeImage image = new NativeImage(width, height, false)) {
//            for (int x = 0; x < image.getWidth(); x++) {
//                for (int y = 0; y < image.getHeight(); y++) {
//                    int a = FastColor.ABGR32.alpha(red.getPixelABGR(x, y));
//                    int b = FastColor.ABGR32.blue(blue.getPixelABGR(x, y));
//                    int g = FastColor.ABGR32.green(green.getPixelABGR(x, y));
//                    int r = FastColor.ABGR32.red(red.getPixelABGR(x, y));
//
//                    int abgr = FastColor.ABGR32.color(a, b, g, r);
//
//                    image.setPixelRGBA(x, y, abgr);
//                }
//            }
//
//            mapColorPixels = new DitheringColorConverter().convert(image);
//        }

        CompoundTag properties = new CompoundTag();
        properties.putString(ExposureSavedData.TYPE_PROPERTY, FilmType.COLOR.getSerializedName());
        long unixTime = System.currentTimeMillis() / 1000L;
        properties.putLong(ExposureSavedData.TIMESTAMP_PROPERTY, unixTime);

        ExposureSavedData resultData = new ExposureSavedData(width, height, mapColorPixels, properties);
        ExposureServer.getExposureStorage().put(id, resultData);

        // Because we save exposure off-thread, and item was already created before chromatic processing has even begun -
        // we need to update clients, otherwise, client wouldn't know that and will think that the exposure is missing.
        ExposureServer.getExposureStorage().sendExposureChanged(id);
        return true;
    }
}
