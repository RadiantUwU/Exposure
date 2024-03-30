package io.github.mortuusars.exposure.block.entity;

import com.google.common.base.Preconditions;
import com.mojang.logging.LogUtils;
import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.ExposureServer;
import io.github.mortuusars.exposure.block.LightroomBlock;
import io.github.mortuusars.exposure.camera.capture.processing.FloydDither;
import io.github.mortuusars.exposure.camera.infrastructure.FilmType;
import io.github.mortuusars.exposure.camera.infrastructure.FrameData;
import io.github.mortuusars.exposure.data.storage.ExposureSavedData;
import io.github.mortuusars.exposure.item.*;
import io.github.mortuusars.exposure.menu.LightroomMenu;
import io.github.mortuusars.exposure.util.ItemAndStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("unused")
public class LightroomBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int CONTAINER_DATA_SIZE = 3;
    public static final int CONTAINER_DATA_PROGRESS_ID = 0;
    public static final int CONTAINER_DATA_PRINT_TIME_ID = 1;
    public static final int CONTAINER_DATA_SELECTED_FRAME_ID = 2;

    protected final ContainerData containerData = new ContainerData() {
        public int get(int id) {
            return switch (id) {
                case CONTAINER_DATA_PROGRESS_ID -> LightroomBlockEntity.this.progress;
                case CONTAINER_DATA_PRINT_TIME_ID -> LightroomBlockEntity.this.printTime;
                case CONTAINER_DATA_SELECTED_FRAME_ID -> LightroomBlockEntity.this.getSelectedFrameIndex();
                default -> 0;
            };
        }

        public void set(int id, int value) {
            if (id == CONTAINER_DATA_PROGRESS_ID)
                LightroomBlockEntity.this.progress = value;
            else if (id == CONTAINER_DATA_PRINT_TIME_ID)
                LightroomBlockEntity.this.printTime = value;
            else if (id == CONTAINER_DATA_SELECTED_FRAME_ID)
                LightroomBlockEntity.this.setSelectedFrame(value);
            setChanged();
        }

        public int getCount() {
            return CONTAINER_DATA_SIZE;
        }
    };

    private NonNullList<ItemStack> items = NonNullList.withSize(Lightroom.SLOTS, ItemStack.EMPTY);

    protected int selectedFrame = 0;
    protected int progress = 0;
    protected int printTime = 0;
    protected int printedPhotographsCount = 0;
    protected boolean advanceFrame;
    protected Lightroom.Process process = Lightroom.Process.REGULAR;

    public LightroomBlockEntity(BlockPos pos, BlockState blockState) {
        super(Exposure.BlockEntityTypes.LIGHTROOM.get(), pos, blockState);
    }

    public static <T extends BlockEntity> void serverTick(Level level, BlockPos blockPos, BlockState blockState, T blockEntity) {
        if (blockEntity instanceof LightroomBlockEntity lightroomBlockEntity)
            lightroomBlockEntity.tick();
    }

    protected void tick() {
        if (printTime <= 0 || !canPrint()) {
            stopPrintingProcess();
            return;
        }

        if (progress < printTime) {
            progress++;
            if (progress % 55 == 0 && printTime - progress > 12 && level != null)
                level.playSound(null, getBlockPos(), Exposure.SoundEvents.LIGHTROOM_PRINT.get(), SoundSource.BLOCKS,
                        1f, level.getRandom().nextFloat() * 0.3f + 1f);
            return;
        }

        if (tryPrint()) {
            onFramePrinted();
        }

        stopPrintingProcess();
    }

    protected void onFramePrinted() {
        if (advanceFrame)
            advanceFrame();
    }

    protected void advanceFrame() {
        ItemAndStack<DevelopedFilmItem> film = new ItemAndStack<>(getItem(Lightroom.FILM_SLOT));
        int frames = film.getItem().getExposedFramesCount(film.getStack());

        if (getSelectedFrameIndex() >= frames - 1) { // On last frame
            tryEjectFilm();
        } else {
            setSelectedFrame(getSelectedFrameIndex() + 1);
            setChanged();
        }
    }

    public boolean isAdvancingFrameOnPrint() {
        return advanceFrame;
    }

    protected boolean tryEjectFilm() {
        if (level == null || level.isClientSide || getItem(Lightroom.FILM_SLOT).isEmpty())
            return false;

        BlockPos pos = getBlockPos();
        Direction facing = level.getBlockState(pos).getValue(LightroomBlock.FACING);

        if (level.getBlockState(pos.relative(facing)).canOcclude())
            return false;

        ItemStack filmStack = removeItem(Lightroom.FILM_SLOT, 1);

        Vec3i normal = facing.getNormal();
        Vec3 point = Vec3.atCenterOf(pos).add(normal.getX() * 0.75f, normal.getY() * 0.75f, normal.getZ() * 0.75f);
        ItemEntity itemEntity = new ItemEntity(level, point.x, point.y, point.z, filmStack);
        itemEntity.setDeltaMovement(normal.getX() * 0.05f, normal.getY() * 0.05f + 0.15f, normal.getZ() * 0.05f);
        itemEntity.setDefaultPickUpDelay();
        level.addFreshEntity(itemEntity);

        inventoryContentsChanged(Lightroom.FILM_SLOT);

        return true;
    }

    public int getSelectedFrameIndex() {
        return selectedFrame;
    }

    public void setSelectedFrame(int index) {
        if (selectedFrame != index) {
            selectedFrame = index;
            stopPrintingProcess();
        }
    }

    /*
        Process setting stays where it was set by the player, But if the frame does not support it -
        image would be printed with supported process (probably regular), WITHOUT changing the process setting -
        this means that this process will be used again if some other image is supporting it.

        This was done to not reset Process in automated setups, if there were some image on a film that doesn't support selected process.
     */

    /**
     * @return Process SETTING. Can be different from actual process that would be used to print an image
     * (if frame does not support this process, for example).
     */
    public Lightroom.Process getProcess() {
        return process;
    }

    /**
     * @return Process, which currently selected image supports.
     */
    public Lightroom.Process getActualProcess() {
        ItemStack film = getItem(Lightroom.FILM_SLOT);

        if (!isSelectedFrameChromatic(film, getSelectedFrameIndex()))
            return Lightroom.Process.REGULAR;

        return process;
    }

    public void setProcess(Lightroom.Process process) {
        this.process = process;
        setChanged();
    }

    public Optional<CompoundTag> getSelectedFrame(ItemStack film) {
        if (!film.isEmpty() && film.getItem() instanceof DevelopedFilmItem developedFilm) {
            ListTag frames = developedFilm.getExposedFrames(film);
            if (frames.size() > getSelectedFrameIndex())
                return Optional.of(frames.getCompound(selectedFrame));
        }

        return Optional.empty();
    }

    public boolean isSelectedFrameChromatic(ItemStack film, int selectedFrame) {
        return getSelectedFrame(film)
                .map(frame -> frame.getBoolean(FrameData.CHROMATIC))
                .orElse(false);
    }

    public void startPrintingProcess(boolean advanceFrameOnFinish) {
        if (!canPrint())
            return;

        ItemStack filmStack = getItem(Lightroom.FILM_SLOT);
        if (!(filmStack.getItem() instanceof DevelopedFilmItem developedFilmItem))
            return;

        //TODO: Chromatic print time
        printTime = developedFilmItem.getType() == FilmType.COLOR ?
                Config.Common.LIGHTROOM_COLOR_FILM_PRINT_TIME.get() :
                Config.Common.LIGHTROOM_BW_FILM_PRINT_TIME.get();
        advanceFrame = advanceFrameOnFinish;
        if (level != null) {
            level.setBlock(getBlockPos(), level.getBlockState(getBlockPos())
                    .setValue(LightroomBlock.LIT, true), Block.UPDATE_CLIENTS);
            level.playSound(null, getBlockPos(), Exposure.SoundEvents.LIGHTROOM_PRINT.get(), SoundSource.BLOCKS,
                    1f, level.getRandom().nextFloat() * 0.3f + 1f);
        }
    }

    public void stopPrintingProcess() {
        progress = 0;
        printTime = 0;
        advanceFrame = false;
        if (level != null && level.getBlockState(getBlockPos()).getBlock() instanceof LightroomBlock)
            level.setBlock(getBlockPos(), level.getBlockState(getBlockPos())
                    .setValue(LightroomBlock.LIT, false), Block.UPDATE_CLIENTS);
    }

    public boolean isPrinting() {
        return printTime > 0;
    }

    public boolean canPrint() {
        if (getSelectedFrameIndex() < 0) // Upper bound is checked further down
            return false;

        ItemStack paperStack = getItem(Lightroom.PAPER_SLOT);
        if (paperStack.isEmpty())
            return false;

        ItemStack filmStack = getItem(Lightroom.FILM_SLOT);
        if (!(filmStack.getItem() instanceof DevelopedFilmItem developedFilm) || !developedFilm.hasExposedFrame(filmStack, getSelectedFrameIndex()))
            return false;

        Lightroom.Process process = getActualProcess();
        if (!hasDyesForPrint(filmStack, paperStack, process))
            return false;

        return canOutputToResultSlot(getItem(Lightroom.RESULT_SLOT), filmStack, process);
    }

    private boolean canOutputToResultSlot(ItemStack resultStack, ItemStack filmStack, Lightroom.Process actualProcess) {
        if (isSelectedFrameChromatic(filmStack, getSelectedFrameIndex()) && getProcess() == Lightroom.Process.CHROMATIC)
            return resultStack.isEmpty();

        return resultStack.isEmpty() || resultStack.getItem() instanceof PhotographItem
                || (resultStack.getItem() instanceof StackedPhotographsItem stackedPhotographsItem
                && stackedPhotographsItem.canAddPhotograph(resultStack));
    }

    public boolean hasDyesForPrint(ItemStack film, ItemStack paper, Lightroom.Process process) {
        int[] dyeSlots = getRequiredDyeSlotsForPrint(film, paper, process);

        for (int slot : dyeSlots) {
            if (getItem(slot).isEmpty())
                return false;
        }

        return true;
    }

    protected int[] getRequiredDyeSlotsForPrint(ItemStack film, ItemStack paper, Lightroom.Process process) {
        if (!(film.getItem() instanceof IFilmItem filmItem))
            return ArrayUtils.EMPTY_INT_ARRAY;

        if (process == Lightroom.Process.REGULAR) {
            return filmItem.getType() == FilmType.COLOR ? Lightroom.DYES_FOR_COLOR : Lightroom.DYES_FOR_BW;
        }

        if (process == Lightroom.Process.CHROMATIC) {
            int chromaticStep = getChromaticStep(paper);
            if (chromaticStep == 0) return Lightroom.DYES_FOR_CHROMATIC_RED;
            if (chromaticStep == 1) return Lightroom.DYES_FOR_CHROMATIC_GREEN;
            if (chromaticStep == 2) return Lightroom.DYES_FOR_CHROMATIC_BLUE;
        }

        return ArrayUtils.EMPTY_INT_ARRAY;
    }

    protected int getChromaticStep(ItemStack paper) {
        if (!(paper.getItem() instanceof ChromaticFragmentItem chromaticFragment))
            return 0;

        return chromaticFragment.getExposures(paper).size();
    }

    public boolean tryPrint() {
        Preconditions.checkState(level != null && !level.isClientSide, "Cannot be called clientside.");
        if (!canPrint())
            return false;

        ItemStack filmStack = getItem(Lightroom.FILM_SLOT);
        if (!(filmStack.getItem() instanceof DevelopedFilmItem developedFilm))
            return false;

        Optional<CompoundTag> selectedFrame = getSelectedFrame(filmStack);
        if (selectedFrame.isEmpty()) {
            LogUtils.getLogger().error("Unable to get selected frame '{}' : {}", getSelectedFrameIndex(), filmStack);
            return false;
        }

        CompoundTag frame = selectedFrame.get().copy();
        Lightroom.Process process = getActualProcess();
        ItemStack paperStack = getItem(Lightroom.PAPER_SLOT);

        ItemStack printResult = createPrintResult(frame, filmStack, paperStack, process);
        putPrintResultInOutputSlot(printResult);

        // Consume items required for printing:
        int[] dyesSlots = getRequiredDyeSlotsForPrint(filmStack, paperStack, process);
        for (int slot : dyesSlots) {
            getItem(slot).shrink(1);
        }
        getItem(Lightroom.PAPER_SLOT).shrink(1);

        level.playSound(null, getBlockPos(), Exposure.SoundEvents.PHOTOGRAPH_RUSTLE.get(), SoundSource.PLAYERS, 0.8f, 1f);

        printedPhotographsCount++;

        if (process != Lightroom.Process.CHROMATIC) { // Chromatics create new exposure. Marking is not needed.
            // Mark exposure as printed
            String id = frame.getString(FrameData.ID);
            if (!id.isEmpty()) {
                ExposureServer.getExposureStorage().getOrQuery(id).ifPresent(exposure -> {
                    CompoundTag properties = exposure.getProperties();
                    if (!properties.getBoolean(ExposureSavedData.WAS_PRINTED_PROPERTY)) {
                        properties.putBoolean(ExposureSavedData.WAS_PRINTED_PROPERTY, true);
                        exposure.setDirty();
                    }
                });
            }
        }

        return true;
    }

    private void putPrintResultInOutputSlot(ItemStack printResult) {
        ItemStack resultStack = getItem(Lightroom.RESULT_SLOT);
        if (resultStack.isEmpty())
            resultStack = printResult;
        else if (resultStack.getItem() instanceof PhotographItem) {
            StackedPhotographsItem stackedPhotographsItem = Exposure.Items.STACKED_PHOTOGRAPHS.get();
            ItemStack newStackedPhotographs = new ItemStack(stackedPhotographsItem);
            stackedPhotographsItem.addPhotographOnTop(newStackedPhotographs, resultStack);
            stackedPhotographsItem.addPhotographOnTop(newStackedPhotographs, printResult);
            resultStack = newStackedPhotographs;
        } else if (resultStack.getItem() instanceof StackedPhotographsItem stackedPhotographsItem) {
            stackedPhotographsItem.addPhotographOnTop(resultStack, printResult);
        } else {
            LogUtils.getLogger().error("Unexpected item in result slot: " + resultStack);
            return;
        }
        setItem(Lightroom.RESULT_SLOT, resultStack);
    }

    protected ItemStack createPrintResult(CompoundTag frame, ItemStack film, ItemStack paper, Lightroom.Process process) {
        if (!(film.getItem() instanceof IFilmItem filmItem))
            throw new IllegalStateException("Film stack is invalid: " + film);

        paper = paper.copy();

        // Type is currently used to decide what dyes are used when copying photograph.
        // Or for quests. Or other purposes. It's important.
        frame.putString(FrameData.TYPE, process == Lightroom.Process.REGULAR
                ? filmItem.getType().getSerializedName() : FilmType.COLOR.getSerializedName());

        if (process == Lightroom.Process.CHROMATIC) {
            ItemAndStack<ChromaticFragmentItem> chromaticFragment = new ItemAndStack<>(
                    paper.getItem() instanceof ChromaticFragmentItem ? paper
                            : new ItemStack(Exposure.Items.CHROMATIC_FRAGMENT.get()));

            chromaticFragment.getItem().addExposure(chromaticFragment.getStack(), frame);

            if (chromaticFragment.getItem().getExposures(chromaticFragment.getStack()).size() >= 3)
                return chromaticFragment.getItem().finalize(Objects.requireNonNull(level), chromaticFragment.getStack());

            return chromaticFragment.getStack();
        }
        else {
            ItemStack photographStack = new ItemStack(Exposure.Items.PHOTOGRAPH.get());
            photographStack.setTag(frame);
            return photographStack;
        }
    }

    private ItemStack finalizeChromaticFragment(ItemAndStack<ChromaticFragmentItem> chromaticFragment) {
        if (level == null)
            return chromaticFragment.getStack();

        List<CompoundTag> exposures = chromaticFragment.getItem().getExposures(chromaticFragment.getStack());
        Preconditions.checkState(exposures.size() == 3,
                "Finalizing Chromatic Fragment requires 3 exposures. " + chromaticFragment);

        CompoundTag redTag = exposures.get(0);
        String redId = redTag.getString(FrameData.ID);
        Optional<ExposureSavedData> redOpt = ExposureServer.getExposureStorage().getOrQuery(redId);
        if (redOpt.isEmpty()) {
            LogUtils.getLogger().error("Cannot create Chromatic Photograph: Red channel exposure '" + redId + "' is not found.");
            return chromaticFragment.getStack();
        }

        CompoundTag greenTag = exposures.get(1);
        String greenId = greenTag.getString(FrameData.ID);
        Optional<ExposureSavedData> greenOpt = ExposureServer.getExposureStorage().getOrQuery(greenId);
        if (greenOpt.isEmpty()) {
            LogUtils.getLogger().error("Cannot create Chromatic Photograph: Green channel exposure '" + greenId + "' is not found.");
            return chromaticFragment.getStack();
        }

        CompoundTag blueTag = exposures.get(2);
        String blueId = blueTag.getString(FrameData.ID);
        Optional<ExposureSavedData> blueOpt = ExposureServer.getExposureStorage().getOrQuery(blueId);
        if (blueOpt.isEmpty()) {
            LogUtils.getLogger().error("Cannot create Chromatic Photograph: Blue channel exposure '" + blueId + "' is not found.");
            return chromaticFragment.getStack();
        }

        ExposureSavedData red = redOpt.get();
        ExposureSavedData green = greenOpt.get();
        ExposureSavedData blue = blueOpt.get();

        int width = Math.min(red.getWidth(), Math.min(green.getWidth(), blue.getWidth()));
        int height = Math.min(red.getHeight(), Math.min(green.getHeight(), blue.getHeight()));

        if (width <= 0) {
            LogUtils.getLogger().error("Cannot create Chromatic Photograph: Width should be larger than 0");
            return chromaticFragment.getStack();
        }

        if (height <= 0) {
            LogUtils.getLogger().error("Cannot create Chromatic Photograph: Height should be larger than 0");
            return chromaticFragment.getStack();
        }

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int r = MapColor.getColorFromPackedId(red.getPixel(x, y)) >> 16 & 0xFF;
                int g = MapColor.getColorFromPackedId(green.getPixel(x, y)) >> 8 & 0xFF;
                int b = MapColor.getColorFromPackedId(blue.getPixel(x, y)) & 0xFF;

                int rgb = 0xFF << 24 | r << 16 | g << 8 | b;

                image.setRGB(x, y, rgb);
            }
        }

        byte[] mapColorPixels = FloydDither.ditherWithMapColors(image);

        CompoundTag properties = new CompoundTag();
        properties.putString(ExposureSavedData.TYPE_PROPERTY, FilmType.COLOR.getSerializedName());

        ExposureSavedData resultData = new ExposureSavedData(image.getWidth(), image.getHeight(), mapColorPixels, properties);

        String name;
        int underscoreIndex = redId.lastIndexOf("_");
        if (underscoreIndex != -1)
            name = redId.substring(0, underscoreIndex);
        else
            name = Integer.toString(redId.hashCode());

        String id = String.format("%s_chromatic_%s", name, level.getGameTime());

        ExposureServer.getExposureStorage().put(id, resultData);

        ItemStack photograph = new ItemStack(Exposure.Items.PHOTOGRAPH.get());

        //TODO: Only include properties common to all 3 exposures
        CompoundTag tag = redTag.copy();
        tag = tag.merge(greenTag);
        tag = tag.merge(blueTag);

        tag.putString(FrameData.ID, id);
        tag.putString(FrameData.RED_CHANNEL, redId);
        tag.putString(FrameData.GREEN_CHANNEL, greenId);
        tag.putString(FrameData.BLUE_CHANNEL, blueId);

        photograph.setTag(tag);

        return photograph;
    }

    public void dropStoredExperience(@Nullable Player player) {
        if (level == null || level.isClientSide)
            return;

        int xpPerPrint = Config.Common.LIGHTROOM_EXPERIENCE_PER_PRINT.get();
        if (xpPerPrint > 0) {
            for (int i = 0; i < printedPhotographsCount; i++) {
                ExperienceOrb.award(((ServerLevel) level), player != null ? player.position() : Vec3.atCenterOf(getBlockPos()), xpPerPrint - 1 + level.getRandom()
                        .nextInt(0, 3));
            }
        }

        printedPhotographsCount = 0;
        setChanged();
    }


    // Container

    @Override
    protected @NotNull Component getDefaultName() {
        return Component.translatable("block.exposure.lightroom");
    }

    @Override
    protected @NotNull AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new LightroomMenu(containerId, inventory, this, containerData);
    }

    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == Lightroom.FILM_SLOT) return stack.getItem() instanceof DevelopedFilmItem;
        else if (slot == Lightroom.CYAN_SLOT) return stack.is(Exposure.Tags.Items.CYAN_PRINTING_DYES);
        else if (slot == Lightroom.MAGENTA_SLOT) return stack.is(Exposure.Tags.Items.MAGENTA_PRINTING_DYES);
        else if (slot == Lightroom.YELLOW_SLOT) return stack.is(Exposure.Tags.Items.YELLOW_PRINTING_DYES);
        else if (slot == Lightroom.BLACK_SLOT) return stack.is(Exposure.Tags.Items.BLACK_PRINTING_DYES);
        else if (slot == Lightroom.PAPER_SLOT)
            return stack.is(Exposure.Tags.Items.PHOTO_PAPERS) ||
                    (stack.getItem() instanceof ChromaticFragmentItem chromatic && chromatic.getExposures(stack).size() < 3);
        else if (slot == Lightroom.RESULT_SLOT)
            return stack.getItem() instanceof PhotographItem || stack.getItem() instanceof ChromaticFragmentItem;
        return false;
    }

    protected void inventoryContentsChanged(int slot) {
        if (slot == Lightroom.FILM_SLOT)
            setSelectedFrame(0);

        setChanged();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return this.level != null && this.level.getBlockEntity(this.worldPosition) == this
                && player.distanceToSqr(this.worldPosition.getX() + 0.5D,
                this.worldPosition.getY() + 0.5D,
                this.worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public int @NotNull [] getSlotsForFace(@NotNull Direction face) {
        return Lightroom.ALL_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, @NotNull ItemStack itemStack, @Nullable Direction direction) {
        if (direction == Direction.DOWN)
            return false;
        return canPlaceItem(index, itemStack);
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return index != Lightroom.RESULT_SLOT && isItemValidForSlot(index, stack) && super.canPlaceItem(index, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, @NotNull ItemStack pStack, @NotNull Direction direction) {
        for (int outputSlot : Lightroom.OUTPUT_SLOTS) {
            if (index == outputSlot)
                return true;
        }
        return false;
    }


    // Load/Save
    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);

        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items);

        // Backwards compatibility:
        if (tag.contains("Inventory", Tag.TAG_COMPOUND)) {
            CompoundTag inventory = tag.getCompound("Inventory");
            ListTag itemsList = inventory.getList("Items", Tag.TAG_COMPOUND);

            for (int i = 0; i < itemsList.size(); i++) {
                CompoundTag itemTags = itemsList.getCompound(i);
                int slot = itemTags.getInt("Slot");

                if (slot >= 0 && slot < items.size())
                    items.set(slot, ItemStack.of(itemTags));
            }
        }

        this.setSelectedFrame(tag.getInt("SelectedFrame"));
        this.progress = tag.getInt("Progress");
        this.printTime = tag.getInt("PrintTime");
        this.printedPhotographsCount = tag.getInt("PrintedPhotographsCount");
        this.advanceFrame = tag.getBoolean("AdvanceFrame");
        this.process = Lightroom.Process.fromStringOrDefault(tag.getString("Process"), Lightroom.Process.REGULAR);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        if (getSelectedFrameIndex() > 0)
            tag.putInt("SelectedFrame", getSelectedFrameIndex());
        if (progress > 0)
            tag.putInt("Progress", progress);
        if (printTime > 0)
            tag.putInt("PrintTime", printTime);
        if (printedPhotographsCount > 0)
            tag.putInt("PrintedPhotographsCount", printedPhotographsCount);
        if (advanceFrame)
            tag.putBoolean("AdvanceFrame", true);
        if (process != Lightroom.Process.REGULAR)
            tag.putString("Process", process.getSerializedName());
    }

    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    public int getContainerSize() {
        return Lightroom.SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return getItems().stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        return getItems().get(slot);
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        ItemStack itemStack = ContainerHelper.removeItem(getItems(), slot, amount);
        if (!itemStack.isEmpty())
            inventoryContentsChanged(slot);
        return itemStack;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(getItems(), slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.getItems().set(slot, stack);
        if (stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }
        inventoryContentsChanged(slot);
    }

    @Override
    public void clearContent() {
        getItems().clear();
        inventoryContentsChanged(-1);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }


    // Sync:

    @Override
    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }
}
