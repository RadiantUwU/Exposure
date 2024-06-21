package io.github.mortuusars.exposure.item;

import com.google.common.base.Preconditions;
import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.PlatformHelper;
import io.github.mortuusars.exposure.block.FlashBlock;
import io.github.mortuusars.exposure.camera.AttachmentSound;
import io.github.mortuusars.exposure.camera.AttachmentType;
import io.github.mortuusars.exposure.camera.capture.Capture;
import io.github.mortuusars.exposure.camera.capture.CaptureManager;
import io.github.mortuusars.exposure.camera.capture.FileCapture;
import io.github.mortuusars.exposure.camera.capture.ScreenshotCapture;
import io.github.mortuusars.exposure.camera.capture.component.*;
import io.github.mortuusars.exposure.camera.capture.converter.DitheringColorConverter;
import io.github.mortuusars.exposure.camera.capture.converter.SimpleColorConverter;
import io.github.mortuusars.exposure.camera.infrastructure.*;
import io.github.mortuusars.exposure.camera.viewfinder.ViewfinderClient;
import io.github.mortuusars.exposure.data.filter.Filters;
import io.github.mortuusars.exposure.menu.CameraAttachmentsMenu;
import io.github.mortuusars.exposure.network.Packets;
import io.github.mortuusars.exposure.network.packet.client.OnFrameAddedS2CP;
import io.github.mortuusars.exposure.network.packet.client.StartExposureS2CP;
import io.github.mortuusars.exposure.network.packet.server.CameraInHandAddFrameC2SP;
import io.github.mortuusars.exposure.network.packet.server.OpenCameraAttachmentsPacketC2SP;
import io.github.mortuusars.exposure.sound.OnePerPlayerSounds;
import io.github.mortuusars.exposure.sound.OnePerPlayerSoundsClient;
import io.github.mortuusars.exposure.util.CameraInHand;
import io.github.mortuusars.exposure.util.ColorChannel;
import io.github.mortuusars.exposure.util.ItemAndStack;
import io.github.mortuusars.exposure.util.LevelUtil;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class CameraItem extends Item {
    public static final AttachmentType FILM_ATTACHMENT = new AttachmentType("Film", 0,
            stack -> stack.getItem() instanceof FilmRollItem, AttachmentSound.FILM);
    public static final AttachmentType FLASH_ATTACHMENT = new AttachmentType("Flash", 1,
            stack -> stack.is(Exposure.Tags.Items.FLASHES), AttachmentSound.FLASH);
    public static final AttachmentType LENS_ATTACHMENT = new AttachmentType("Lens", 2,
            stack -> stack.is(Exposure.Tags.Items.LENSES), AttachmentSound.LENS);
    public static final AttachmentType FILTER_ATTACHMENT = new AttachmentType("Filter", 3,
            stack -> stack.is(Exposure.Tags.Items.FILTERS), AttachmentSound.FILTER);

    public static final List<AttachmentType> ATTACHMENTS = List.of(
            FILM_ATTACHMENT,
            FLASH_ATTACHMENT,
            LENS_ATTACHMENT,
            FILTER_ATTACHMENT);

    public static final List<ShutterSpeed> SHUTTER_SPEEDS = List.of(
            new ShutterSpeed("15\""),
            new ShutterSpeed("8\""),
            new ShutterSpeed("4\""),
            new ShutterSpeed("2\""),
            new ShutterSpeed("1\""),
            new ShutterSpeed("2"),
            new ShutterSpeed("4"),
            new ShutterSpeed("8"),
            new ShutterSpeed("15"),
            new ShutterSpeed("30"),
            new ShutterSpeed("60"),
            new ShutterSpeed("125"),
            new ShutterSpeed("250"),
            new ShutterSpeed("500")
    );

    public CameraItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack) {
        return 1000;
    }

    public boolean isBarVisible(@NotNull ItemStack stack) {
        if (!Config.Client.CAMERA_SHOW_FILM_BAR_ON_ITEM.get())
            return false;
        return getAttachment(stack, FILM_ATTACHMENT)
                .map(f -> f.getItem() instanceof FilmRollItem filmRollItem && filmRollItem.isBarVisible(f))
                .orElse(false);
    }

    public int getBarWidth(@NotNull ItemStack stack) {
        if (!Config.Client.CAMERA_SHOW_FILM_BAR_ON_ITEM.get())
            return 0;
        return getAttachment(stack, FILM_ATTACHMENT)
                .map(f -> f.getItem() instanceof FilmRollItem filmRollItem ? filmRollItem.getBarWidth(f) : 0)
                .orElse(0);
    }

    public int getBarColor(@NotNull ItemStack stack) {
        if (!Config.Client.CAMERA_SHOW_FILM_BAR_ON_ITEM.get())
            return 0;
        return getAttachment(stack, FILM_ATTACHMENT)
                .map(f -> f.getItem() instanceof FilmRollItem filmRollItem ? filmRollItem.getBarColor(f) : 0)
                .orElse(0);
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack otherStack, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY)
            return false;

        if (otherStack.isEmpty() && Config.Common.CAMERA_GUI_RIGHT_CLICK_ATTACHMENTS_SCREEN.get()) {
            if (!(slot.container instanceof Inventory)) {
                return false; // Cannot open when not in player's inventory
            }

            if (player.isCreative() && player.level().isClientSide()/* && CameraItemClientExtensions.isInCreativeModeInventory()*/) {
                Packets.sendToServer(new OpenCameraAttachmentsPacketC2SP(slot.getContainerSlot()));
                return true;
            }

            openCameraAttachmentsMenu(player, slot.getContainerSlot());
            return true;
        }

        if (PlatformHelper.canShear(otherStack) && !isTooltipRemoved(stack)) {
            if (otherStack.isDamageableItem()) {
                // broadcasting break event is expecting item to be in hand,
                // but making it work for carried items would be too much work for such small feature.
                // No one will ever notice it anyway.
                otherStack.hurtAndBreak(1, player, pl -> pl.broadcastBreakEvent(InteractionHand.MAIN_HAND));
            }

            if (player.level().isClientSide)
                player.playSound(SoundEvents.SHEEP_SHEAR);

            setTooltipRemoved(stack, true);
            return true;
        }

        if (isTooltipRemoved(stack) && (otherStack.getItem() instanceof BookItem || otherStack.getItem() instanceof WritableBookItem
                || otherStack.getItem() instanceof WrittenBookItem || otherStack.getItem() instanceof KnowledgeBookItem)) {
            setTooltipRemoved(stack, false);
            if (player.level().isClientSide)
                player.playSound(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT);
            return true;
        }

        if (Config.Common.CAMERA_GUI_RIGHT_CLICK_HOTSWAP.get()) {
            for (AttachmentType attachmentType : getAttachmentTypes(stack)) {
                if (attachmentType.matches(otherStack)) {
                    Optional<ItemStack> current = getAttachment(stack, attachmentType);

                    if (otherStack.getCount() > 1 && current.isPresent()) {
                        if (player.level().isClientSide())
                            OnePerPlayerSoundsClient.play(player, Exposure.SoundEvents.CAMERA_LENS_RING_CLICK.get(), SoundSource.PLAYERS, 0.9f, 1f);
                        return true; // Cannot swap when holding more than one item
                    }

                    setAttachment(stack, attachmentType, otherStack.split(1));
                    access.set(current.orElse(otherStack));
                    attachmentType.sound().playOnePerPlayer(player, false);
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> components, @NotNull TooltipFlag isAdvanced) {
        if (Config.Client.CAMERA_SHOW_FILM_FRAMES_IN_TOOLTIP.get()) {
            getAttachment(stack, FILM_ATTACHMENT).ifPresent(f -> {
                if (f.getItem() instanceof FilmRollItem filmRollItem) {
                    int exposed = filmRollItem.getExposedFramesCount(f);
                    int max = filmRollItem.getMaxFrameCount(f);
                    components.add(Component.translatable("item.exposure.camera.tooltip.film_roll_frames", exposed, max));
                }
            });
        }

        if (!isTooltipRemoved(stack) && Config.Client.CAMERA_SHOW_TOOLTIP_DETAILS.get()) {
            boolean rClickAttachments = Config.Common.CAMERA_GUI_RIGHT_CLICK_ATTACHMENTS_SCREEN.get();
            boolean rClickHotswap = Config.Common.CAMERA_GUI_RIGHT_CLICK_HOTSWAP.get();

            if (rClickAttachments || rClickHotswap) {
                if (Screen.hasShiftDown()) {
                    if (rClickAttachments)
                        components.add(Component.translatable("item.exposure.camera.tooltip.details_attachments_screen"));
                    if (rClickHotswap)
                        components.add(Component.translatable("item.exposure.camera.tooltip.details_hotswap"));
                    components.add(Component.translatable("item.exposure.camera.tooltip.details_remove_tooltip"));
                } else
                    components.add(Component.translatable("tooltip.exposure.hold_for_details"));
            }
        }
    }

    public boolean isActive(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().getBoolean("Active");
    }

    public void setActive(ItemStack stack, boolean active) {
        stack.getOrCreateTag().putBoolean("Active", active);
    }

    public void activate(Player player, ItemStack stack) {
        if (!isActive(stack)) {
            setActive(stack, true);
            player.gameEvent(GameEvent.EQUIP); // Sends skulk vibrations
            playCameraSound(player, Exposure.SoundEvents.VIEWFINDER_OPEN.get(), 0.35f, 0.9f, 0.2f);
        }
    }

    public void deactivate(Player player, ItemStack stack) {
        if (isActive(stack)) {
            setActive(stack, false);
            player.gameEvent(GameEvent.EQUIP);
            playCameraSound(player, Exposure.SoundEvents.VIEWFINDER_CLOSE.get(), 0.35f, 0.9f, 0.2f);
        }
    }

    public boolean isInSelfieMode(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().getBoolean("Selfie");
    }

    public void setSelfieMode(ItemStack stack, boolean selfie) {
        stack.getOrCreateTag().putBoolean("Selfie", selfie);
    }

    public boolean isTooltipRemoved(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().getBoolean("TooltipRemoved");
    }

    public void setTooltipRemoved(ItemStack stack, boolean removed) {
        stack.getOrCreateTag().putBoolean("TooltipRemoved", removed);
    }

    public void setSelfieModeWithEffects(Player player, ItemStack stack, boolean selfie) {
        setSelfieMode(stack, selfie);
        player.level().playSound(player, player, Exposure.SoundEvents.CAMERA_LENS_RING_CLICK.get(), SoundSource.PLAYERS, 1f, 1.5f);
    }

    public boolean isShutterOpen(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().getBoolean("ShutterOpen");
    }

    public void setShutterOpen(Level level, ItemStack stack, ShutterSpeed shutterSpeed, boolean exposingFrame, boolean flashHasFired) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean("ShutterOpen", true);
        tag.putInt("ShutterTicks", Math.max(shutterSpeed.getTicks(), 1));
        tag.putLong("ShutterCloseTimestamp", level.getGameTime() + Math.max(shutterSpeed.getTicks(), 1));
        if (exposingFrame)
            tag.putBoolean("ExposingFrame", true);
        if (flashHasFired)
            tag.putBoolean("FlashHasFired", true);
    }

    public void setShutterClosed(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove("ShutterOpen");
            tag.remove("ShutterTicks");
            tag.remove("ShutterCloseTimestamp");
            tag.remove("ExposingFrame");
            tag.remove("FlashHasFired");
        }
    }

    public void openShutter(Player player, Level level, ItemStack stack, ShutterSpeed shutterSpeed, boolean exposingFrame, boolean flashHasFired) {
        setShutterOpen(player.level(), stack, shutterSpeed, exposingFrame, flashHasFired);

        player.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
        playCameraSound(null, player, Exposure.SoundEvents.SHUTTER_OPEN.get(), exposingFrame ? 0.7f : 0.5f,
                exposingFrame ? 1.1f : 1.25f, 0.2f);
        if (shutterSpeed.getMilliseconds() > 500) // More than 1/2
            OnePerPlayerSounds.playForAllClients(player, Exposure.SoundEvents.SHUTTER_TICKING.get(), SoundSource.PLAYERS, 1f, 1f);
    }

    public void closeShutter(Player player, ItemStack stack) {
        long closedAtTimestamp = stack.getTag() != null ? stack.getTag().getLong("ShutterCloseTimestamp") : -1;
        boolean exposingFrame = stack.getTag() != null && stack.getTag().getBoolean("ExposingFrame");
        boolean flashHasFired = stack.getTag() != null && stack.getTag().getBoolean("FlashHasFired");

        setShutterClosed(stack);

        if (player.level().getGameTime() - closedAtTimestamp < 60) { // Skip effects if shutter "was closed" long ago
            player.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
            player.getCooldowns().addCooldown(this, flashHasFired ? 10 : 2);
            playCameraSound(player, player, Exposure.SoundEvents.SHUTTER_CLOSE.get(), 0.7f, 1.1f, 0.2f);
            if (exposingFrame) {
                ItemAndStack<FilmRollItem> film = getFilm(stack).orElseThrow();

                float fullness = (float) film.getItem().getExposedFramesCount(film.getStack()) / film.getItem().getMaxFrameCount(film.getStack());
                boolean lastFrame = fullness == 1f;

                if (lastFrame)
                    OnePerPlayerSounds.play(player, Exposure.SoundEvents.FILM_ADVANCE_LAST.get(), SoundSource.PLAYERS, 1f, 1f);
                else {
                    OnePerPlayerSounds.play(player, Exposure.SoundEvents.FILM_ADVANCING.get(), SoundSource.PLAYERS,
                            1f, 0.9f + 0.1f * fullness);
                }
            }
        }
    }

    @SuppressWarnings("unused")
    public void playCameraSound(@NotNull Player player, SoundEvent sound, float volume, float pitch) {
        playCameraSound(player, sound, volume, pitch, 0f);
    }

    public void playCameraSound(@NotNull Player player, SoundEvent sound, float volume, float pitch, float pitchVariety) {
        playCameraSound(player, player, sound, volume, pitch, pitchVariety);
    }

    public void playCameraSound(@Nullable Player player, @NotNull Player originPlayer, SoundEvent sound, float volume, float pitch, float pitchVariety) {
        if (pitchVariety > 0f)
            pitch = pitch - (pitchVariety / 2f) + (originPlayer.getRandom().nextFloat() * pitchVariety);
        originPlayer.level().playSound(player, originPlayer, sound, SoundSource.PLAYERS, volume, pitch);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected) {
        if (!(entity instanceof Player player))
            return;

        if (isShutterOpen(stack)) {
            if (stack.getTag() != null && stack.getTag().contains("ShutterTicks")) {
                int ticks = stack.getTag().getInt("ShutterTicks");
                if (ticks <= 0)
                    closeShutter(player, stack);
                else {
                    ticks--;
                    stack.getTag().putInt("ShutterTicks", ticks);
                }
            } else {
                closeShutter(player, stack);
            }
        }

        boolean inOffhand = player.getOffhandItem().equals(stack);
        boolean inHand = isSelected || inOffhand;

        if (!inHand) {
            deactivate(player, stack);
        }
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null) {
            InteractionHand hand = context.getHand();
            if (hand == InteractionHand.MAIN_HAND && CameraInHand.getActiveHand(player) == InteractionHand.OFF_HAND)
                return InteractionResult.PASS;

            return useCamera(player, hand);
        }
        return InteractionResult.CONSUME; // To not play attack animation.
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && CameraInHand.getActiveHand(player) == InteractionHand.OFF_HAND)
            return InteractionResultHolder.pass(player.getItemInHand(hand));

        useCamera(player, hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    public InteractionResult useCamera(Player player, InteractionHand hand) {
        if (player.getCooldowns().isOnCooldown(this))
            return InteractionResult.FAIL;

        ItemStack cameraStack = player.getItemInHand(hand);
        if (cameraStack.isEmpty() || cameraStack.getItem() != this)
            return InteractionResult.PASS;

        boolean active = isActive(cameraStack);

        if (!active && player.isSecondaryUseActive()) {
            if (isShutterOpen(cameraStack)) {
                player.displayClientMessage(Component.translatable("item.exposure.camera.camera_attachments.fail.shutter_open")
                        .withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }

            int cameraSlot = getMatchingSlotInInventory(player.getInventory(), cameraStack);
            if (cameraSlot < 0)
                return InteractionResult.FAIL;

            openCameraAttachmentsMenu(player, cameraSlot);
            return InteractionResult.SUCCESS;
        }

        if (!active) {
            activate(player, cameraStack);
            player.getCooldowns().addCooldown(this, 4);

            if (player.level().isClientSide) {
                // Release use key after activating. Otherwise, if right click is still held - camera will take a shot
                CameraItemClientExtensions.releaseUseButton();
            }

            return InteractionResult.CONSUME; // Consume to not play animation
        }


        // Taking a shot:

        if (!(player instanceof ServerPlayer))
            return InteractionResult.CONSUME;

        playCameraSound(null, player, Exposure.SoundEvents.CAMERA_RELEASE_BUTTON_CLICK.get(), 0.3f, 1f, 0.1f);

        Optional<ItemAndStack<FilmRollItem>> filmAttachment = getFilm(cameraStack);

        if (filmAttachment.isEmpty())
            return InteractionResult.FAIL;

        ItemAndStack<FilmRollItem> film = filmAttachment.get();
        boolean exposingFilm = film.getItem().canAddFrame(film.getStack());

        if (!exposingFilm)
            return InteractionResult.FAIL;

        if (isShutterOpen(cameraStack))
            return InteractionResult.FAIL;

        int lightLevel = LevelUtil.getLightLevelAt(player.level(), player.blockPosition());
        boolean shouldFlashFire = shouldFlashFire(player, cameraStack);
        ShutterSpeed shutterSpeed = getShutterSpeed(cameraStack);

        if (PlatformHelper.fireShutterOpeningEvent(player, cameraStack, lightLevel, shouldFlashFire))
            return InteractionResult.FAIL; // Canceled

        boolean flashHasFired = shouldFlashFire && tryUseFlash(player, cameraStack);

        openShutter(player, player.level(), cameraStack, shutterSpeed, true, flashHasFired);

        if (player instanceof ServerPlayer serverPlayer) {
            Packets.sendToClient(new StartExposureS2CP(createExposureId(player), hand, flashHasFired, lightLevel), serverPlayer);
        }

        return InteractionResult.CONSUME; // Consume to not play animation
    }

    public void exposeFrameClientside(Player player, InteractionHand hand, String exposureId, boolean flashHasFired, int lightLevel) {
        Preconditions.checkState(player.level().isClientSide, "Should only be called on client.");

        ItemStack cameraStack = player.getItemInHand(hand);

        if (PlatformHelper.fireShutterOpeningEvent(player, cameraStack, lightLevel, flashHasFired))
            return; // Canceled

        boolean projectingFile = hasInterplanarProjectorFilter(cameraStack) && ExposureClient.isShaderActive();

        CompoundTag frame = new CompoundTag();

        if (projectingFile) {
            frame.putBoolean(FrameData.PROJECTED, true);
        }

        // Base properties. It's easier to add them client-side.
        frame.putString(FrameData.ID, exposureId);
        frame.putString(FrameData.TIMESTAMP, Util.getFilenameFormattedDateTime());
        frame.putString(FrameData.PHOTOGRAPHER, player.getScoreboardName());
        frame.putUUID(FrameData.PHOTOGRAPHER_ID, player.getUUID());

        if (!projectingFile) {
            frame.putInt(FrameData.LIGHT_LEVEL, lightLevel);
            frame.putFloat(FrameData.SUN_ANGLE, player.level().getSunAngle(0));
            if (flashHasFired)
                frame.putBoolean(FrameData.FLASH, true);
            if (isInSelfieMode(cameraStack))
                frame.putBoolean(FrameData.SELFIE, true);

            if (ExposureClient.isShaderActive()) {
                // Chromatic only for black and white:
                boolean isBW = getAttachment(cameraStack, FILM_ATTACHMENT)
                        .map(f -> f.getItem() instanceof IFilmItem filmItem && filmItem.getType() == FilmType.BLACK_AND_WHITE)
                        .orElse(false);
                if (isBW) {
                    getAttachment(cameraStack, FILTER_ATTACHMENT).flatMap(ColorChannel::fromStack).ifPresent(c -> {
                        frame.putBoolean(FrameData.CHROMATIC, true);
                        frame.putString(FrameData.CHROMATIC_CHANNEL, c.getSerializedName());
                    });
                }
            }
        }

        List<UUID> entitiesInFrame;

        if (projectingFile) {
            entitiesInFrame = Collections.emptyList();
        }
        else {
            entitiesInFrame = EntitiesInFrame.get(player, ViewfinderClient.getCurrentFov(), 12, isInSelfieMode(cameraStack))
                    .stream()
                    .map(Entity::getUUID)
                    .toList();
        }

        Packets.sendToServer(new CameraInHandAddFrameC2SP(hand, frame, entitiesInFrame));

        startCapture(player, cameraStack, exposureId, flashHasFired);

//        if (ExposureClient.isShaderActive()) {
//            getAttachment(cameraStack, CameraItem.FILTER_ATTACHMENT).ifPresent(filter -> {
//                if (filter.getItem() instanceof InterplanarProjectorItem interplanarProjector
//                        && interplanarProjector.isConsumable(filter)) {
//                    ViewfinderClient.getCurrentShader().ifPresent(current -> {
//                        Filters.getShaderOf(filter).ifPresent(shader -> {
//                            if (current.equals(shader))
//                                ViewfinderClient.removeShader();
//                        });
//                    });
//
//                    filter.shrink(1);
//                    setAttachment(cameraStack, CameraItem.FILTER_ATTACHMENT, filter);
//                }
//            });
//        }
    }

    protected void startCapture(Player player, ItemStack cameraStack, String exposureId, boolean flashHasFired) {
        Capture capture;

        Optional<ItemAndStack<InterplanarProjectorItem>> projector = getAttachment(cameraStack, FILTER_ATTACHMENT)
                .map(filter -> filter.getItem() instanceof InterplanarProjectorItem ? new ItemAndStack<>(filter) : null);

        if (projector.isPresent() && ExposureClient.isShaderActive()) {
            ItemAndStack<InterplanarProjectorItem> filter = projector.get();
            String filepath = filter.getItem().getFilename(filter.getStack()).orElse("");

            AtomicBoolean removeShaderWhenDone = new AtomicBoolean(false);

            ViewfinderClient.getCurrentShader().ifPresent(current -> {
                Filters.getShaderOf(filter.getStack()).ifPresent(shader -> {
                    if (current.equals(shader)) {
                        removeShaderWhenDone.set(true);
                    }
                });
            });

            capture = createFileCapture(player, cameraStack, exposureId, filepath, filter.getItem().isDithered(filter.getStack()));
            capture.ifCapturingFailed(() -> {
                Capture regularCapture = createCapture(player, cameraStack, exposureId, flashHasFired);
                if (removeShaderWhenDone.get()) {
                    regularCapture.whenImageCaptured(() -> {
                        Minecraft.getInstance().execute(() -> {
                            ViewfinderClient.removeShader();
                            player.level().playSound(player, player, Exposure.SoundEvents.INTERPLANAR_PROJECT.get(),
                                    SoundSource.PLAYERS, 0.8f, 0.6f);
                            for (int i = 0; i < 32; ++i) {
                                player.level().addParticle(ParticleTypes.PORTAL, player.getX(), player.getY() + player.getRandom().nextDouble() * 2.0, player.getZ(), player.getRandom().nextGaussian(), 0.0, player.getRandom().nextGaussian());
                            }
                        });
                    });
                }
                CaptureManager.enqueue(regularCapture);
            });
            if (removeShaderWhenDone.get()) {
                capture.whenImageCaptured(() -> {
                    Minecraft.getInstance().execute(() -> {
                        ViewfinderClient.removeShader();
                        player.level().playSound(player, player, Exposure.SoundEvents.INTERPLANAR_PROJECT.get(),
                                SoundSource.PLAYERS, 0.8f, 1.1f);
                        for (int i = 0; i < 32; ++i) {
                            player.level().addParticle(ParticleTypes.PORTAL, player.getX(), player.getY() + player.getRandom().nextDouble() * 2.0, player.getZ(), player.getRandom().nextGaussian(), 0.0, player.getRandom().nextGaussian());
                        }
                    });
                });
            }
        }
        else {
            capture = createCapture(player, cameraStack, exposureId, flashHasFired);
        }

        CaptureManager.enqueue(capture);
    }

    protected Capture createCapture(Player player, ItemStack cameraStack, String exposureId, boolean flash) {
        ItemAndStack<FilmRollItem> film = getFilm(cameraStack).orElseThrow();
        int frameSize = film.getItem().getFrameSize(film.getStack());
        float brightnessStops = getShutterSpeed(cameraStack).getStopsDifference(ShutterSpeed.DEFAULT);

        ArrayList<ICaptureComponent> components = new ArrayList<>();

        components.add(new ExposureStorageSaveComponent(exposureId, true));

        components.add(new BaseComponent());
        if (flash) {
            components.add(new FlashComponent());
        }
        if (brightnessStops != 0) {
            components.add(new BrightnessComponent(brightnessStops));
        }
        if (film.getItem().getType() == FilmType.BLACK_AND_WHITE) {
            Optional<ItemStack> filter = getAttachment(cameraStack, FILTER_ATTACHMENT);
            filter.flatMap(ColorChannel::fromStack).ifPresentOrElse(
                    channel -> components.add(new SelectiveChannelBlackAndWhiteComponent(channel)),
                    () -> components.add(new BlackAndWhiteComponent()));
        }

        return new ScreenshotCapture()
                .setFilmType(film.getItem().getType())
                .setSize(frameSize)
                .setBrightnessStops(brightnessStops)
                .addComponents(components)
                .setConverter(new DitheringColorConverter());
    }

    protected Capture createFileCapture(Player player, ItemStack cameraStack, String exposureId,
                                        String filepath, boolean dither) {
        ItemAndStack<FilmRollItem> film = getFilm(cameraStack).orElseThrow();
        FilmType filmType = film.getItem().getType();
        int frameSize = film.getItem().getFrameSize(film.getStack());

        return new FileCapture(filepath,
                error -> player.displayClientMessage(error.getCasualTranslation().withStyle(ChatFormatting.RED), false))
                .setFilmType(filmType)
                .setSize(frameSize)
                .addComponents(new ExposureStorageSaveComponent(exposureId, true))
                .setConverter(dither ? new DitheringColorConverter() : new SimpleColorConverter())
                .cropFactor(1)
                .setAsyncCapturing(true);
    }

    public void addFrame(ServerPlayer player, ItemStack cameraStack, InteractionHand hand, CompoundTag frame, List<Entity> entities) {
        if (!frame.getBoolean(FrameData.PROJECTED)) {
            addFrameData(player, cameraStack, frame, entities);
        }

        PlatformHelper.fireModifyFrameDataEvent(player, cameraStack, frame, entities);

        addFrameToFilm(cameraStack, frame);

        player.awardStat(Exposure.Stats.FILM_FRAMES_EXPOSED);
        Exposure.Advancements.FILM_FRAME_EXPOSED.trigger(player, new ItemAndStack<>(cameraStack), frame);

        Packets.sendToClient(new OnFrameAddedS2CP(frame), player);

        PlatformHelper.fireFrameAddedEvent(player, cameraStack, frame);
    }

    public void addFrameToFilm(ItemStack cameraStack, CompoundTag frame) {
        ItemAndStack<FilmRollItem> film = getFilm(cameraStack)
                .orElseThrow(() -> new IllegalStateException("Camera should have film inserted. " + cameraStack));

        film.getItem().addFrame(film.getStack(), frame);
        setFilm(cameraStack, film.getStack());
    }

    protected boolean shouldFlashFire(Player player, ItemStack cameraStack) {
        if (getAttachment(cameraStack, FLASH_ATTACHMENT).isEmpty())
            return false;

        return switch (getFlashMode(cameraStack)) {
            case OFF -> false;
            case ON -> true;
            case AUTO -> LevelUtil.getLightLevelAt(player.level(), player.blockPosition()) < 8;
        };
    }

    @SuppressWarnings("unused")
    public boolean tryUseFlash(Player player, ItemStack cameraStack) {
        Level level = player.level();
        BlockPos playerHeadPos = player.blockPosition().above();
        @Nullable BlockPos flashPos = null;

        if (level.getBlockState(playerHeadPos).isAir() || level.getFluidState(playerHeadPos).isSourceOfType(Fluids.WATER))
            flashPos = playerHeadPos;
        else {
            for (Direction direction : Direction.values()) {
                BlockPos pos = playerHeadPos.relative(direction);
                if (level.getBlockState(pos).isAir() || level.getFluidState(pos).isSourceOfType(Fluids.WATER)) {
                    flashPos = pos;
                }
            }
        }

        if (flashPos == null)
            return false;

        level.setBlock(flashPos, Exposure.Blocks.FLASH.get().defaultBlockState()
                .setValue(FlashBlock.WATERLOGGED, level.getFluidState(flashPos)
                        .isSourceOfType(Fluids.WATER)), Block.UPDATE_ALL_IMMEDIATE);
        level.playSound(player, player, Exposure.SoundEvents.FLASH.get(), SoundSource.PLAYERS, 1f, 1f);

        player.gameEvent(GameEvent.PRIME_FUSE);
        player.awardStat(Exposure.Stats.FLASHES_TRIGGERED);

        // Send particles to other players:
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            Vec3 pos = player.position();
            pos = pos.add(0, 1, 0).add(player.getLookAngle().multiply(0.5, 0, 0.5));
            ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(ParticleTypes.FLASH, false,
                    pos.x, pos.y, pos.z, 0, 0, 0, 0, 0);
            for (ServerPlayer pl : serverLevel.players()) {
                if (!pl.equals(serverPlayer)) {
                    pl.connection.send(packet);
                    RandomSource r = serverLevel.getRandom();
                    for (int i = 0; i < 4; i++) {
                        pl.connection.send(new ClientboundLevelParticlesPacket(ParticleTypes.END_ROD, false,
                                pos.x + r.nextFloat() * 0.5f - 0.25f, pos.y + r.nextFloat() * 0.5f + 0.2f, pos.z + r.nextFloat() * 0.5f - 0.25f,
                                0, 0, 0, 0, 0));
                    }
                }
            }
        }
        return true;
    }

    public void addFrameData(ServerPlayer player, ItemStack cameraStack, CompoundTag frame, List<Entity> entitiesInFrame) {
        Level level = player.level();

        ListTag pos = new ListTag();
        pos.add(IntTag.valueOf(player.blockPosition().getX()));
        pos.add(IntTag.valueOf(player.blockPosition().getY()));
        pos.add(IntTag.valueOf(player.blockPosition().getZ()));
        frame.put(FrameData.POSITION, pos);

        frame.putString(FrameData.DIMENSION, player.level().dimension().location().toString());

        player.level().getBiome(player.blockPosition()).unwrapKey().map(ResourceKey::location)
                .ifPresent(biome -> frame.putString(FrameData.BIOME, biome.toString()));

        int surfaceHeight = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, player.getBlockX(), player.getBlockZ());
        level.updateSkyBrightness();
        int skyLight = level.getBrightness(LightLayer.SKY, player.blockPosition());

        if (player.isUnderWater())
            frame.putBoolean(FrameData.UNDERWATER, true);

        if (player.getBlockY() < surfaceHeight && skyLight < 2)
            frame.putBoolean(FrameData.IN_CAVE, true);
        else if (!player.isUnderWater()) {
            Biome.Precipitation precipitation = level.getBiome(player.blockPosition()).value().getPrecipitationAt(player.blockPosition());
            if (level.isThundering() && precipitation != Biome.Precipitation.NONE)
                frame.putString(FrameData.WEATHER, precipitation == Biome.Precipitation.SNOW ? "Snowstorm" : "Thunder");
            else if (level.isRaining() && precipitation != Biome.Precipitation.NONE)
                frame.putString(FrameData.WEATHER, precipitation == Biome.Precipitation.SNOW ? "Snow" : "Rain");
            else
                frame.putString(FrameData.WEATHER, "Clear");
        }

        addStructuresInfo(player, frame);

        if (!entitiesInFrame.isEmpty()) {
            ListTag entities = new ListTag();

            for (Entity entity : entitiesInFrame) {
                CompoundTag entityInfoTag = createEntityInFrameTag(entity, player, cameraStack);
                if (entityInfoTag.isEmpty())
                    continue;

                entities.add(entityInfoTag);

                // Duplicate entity id as a separate field in the tag.
                // Can then be used by FTBQuests nbt matching (it's hard to match from a list), for example.
                frame.putBoolean(entityInfoTag.getString(FrameData.ENTITY_ID), true);
            }

            if (!entities.isEmpty())
                frame.put(FrameData.ENTITIES_IN_FRAME, entities);
        }
    }

    protected void addStructuresInfo(@NotNull ServerPlayer player, CompoundTag frame) {
        Map<Structure, LongSet> allStructuresAt = player.serverLevel().structureManager().getAllStructuresAt(player.blockPosition());

        List<Structure> inside = new ArrayList<>();

        for (Structure structure : allStructuresAt.keySet()) {
            StructureStart structureAt = player.serverLevel().structureManager().getStructureAt(player.blockPosition(), structure);
            if (structureAt.isValid()) {
                inside.add(structure);
            }
        }

        Registry<Structure> structures = player.serverLevel().registryAccess().registryOrThrow(Registries.STRUCTURE);
        ListTag structuresTag = new ListTag();

        for (Structure structure : inside) {
            ResourceLocation key = structures.getKey(structure);
            if (key != null)
                structuresTag.add(StringTag.valueOf(key.toString()));
        }

        if (!structuresTag.isEmpty()) {
            frame.put("Structures", structuresTag);
        }
    }

    protected CompoundTag createEntityInFrameTag(Entity entity, Player photographer, ItemStack cameraStack) {
        CompoundTag tag = new CompoundTag();
        ResourceLocation entityRL = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());

        tag.putString(FrameData.ENTITY_ID, entityRL.toString());

        ListTag pos = new ListTag();
        pos.add(IntTag.valueOf((int) entity.getX()));
        pos.add(IntTag.valueOf((int) entity.getY()));
        pos.add(IntTag.valueOf((int) entity.getZ()));
        tag.put(FrameData.ENTITY_POSITION, pos);

        tag.putFloat(FrameData.ENTITY_DISTANCE, photographer.distanceTo(entity));

        if (entity instanceof Player player)
            tag.putString(FrameData.ENTITY_PLAYER_NAME, player.getScoreboardName());

        return tag;
    }

    public void openCameraAttachmentsMenu(Player player, int cameraSlotIndex) {
        ItemStack stack = player.getInventory().getItem(cameraSlotIndex);
        Preconditions.checkState(stack.getItem() instanceof CameraItem,
                "Cannot open Camera Attachments UI: " + stack + " is not a CameraItem.");

        if (player instanceof ServerPlayer serverPlayer) {
            MenuProvider menuProvider = new MenuProvider() {
                @Override
                public @NotNull Component getDisplayName() {
                    return stack.hasCustomHoverName() ? stack.getHoverName() : Component.translatable("container.exposure.camera");
                }

                @Override
                public @NotNull AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
                    return new CameraAttachmentsMenu(containerId, playerInventory, cameraSlotIndex);
                }
            };

            PlatformHelper.openMenu(serverPlayer, menuProvider, buffer -> buffer.writeInt(cameraSlotIndex));
        }
    }

    protected int getMatchingSlotInInventory(Inventory inventory, ItemStack stack) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).equals(stack)) {
                return i;
            }
        }
        return -1;
    }

    protected String createExposureId(Player player) {
        // This method is called only server-side and then gets sent to client in a packet
        // because gameTime is different between client/server, and IDs won't match.
        return player.getName().getString() + "_" + player.level().getGameTime();
    }

    public FocalRange getFocalRange(ItemStack cameraStack) {
        return getAttachment(cameraStack, LENS_ATTACHMENT).map(FocalRange::ofStack).orElse(getDefaultFocalRange());
    }

    public FocalRange getDefaultFocalRange() {
        return FocalRange.getDefault();
    }

    /**
     * This method is called after we take a screenshot. Otherwise, due to the delays (flash, etc) - particles would be captured as well.
     */
    @SuppressWarnings("unused")
    public void spawnClientsideFlashEffects(@NotNull Player player, ItemStack cameraStack) {
        Preconditions.checkState(player.level().isClientSide, "This methods should only be called client-side.");
        Level level = player.level();
        Vec3 pos = player.position();
        Vec3 lookAngle = player.getLookAngle();
        pos = pos.add(0, 1, 0).add(lookAngle.multiply(0.8f, 0.8f, 0.8f));

        RandomSource r = level.getRandom();
        for (int i = 0; i < 3; i++) {
            level.addParticle(ParticleTypes.END_ROD,
                    pos.x + r.nextFloat() - 0.5f,
                    pos.y + r.nextFloat() + 0.15f,
                    pos.z + r.nextFloat() - 0.5f,
                    lookAngle.x * 0.025f + r.nextFloat() * 0.025f,
                    lookAngle.y * 0.025f + r.nextFloat() * 0.025f,
                    lookAngle.z * 0.025f + r.nextFloat() * 0.025f);
        }
    }

    // ---

    @SuppressWarnings("unused")
    public List<AttachmentType> getAttachmentTypes(ItemStack cameraStack) {
        return ATTACHMENTS;
    }

    public Optional<AttachmentType> getAttachmentTypeForSlot(ItemStack cameraStack, int slot) {
        List<AttachmentType> attachmentTypes = getAttachmentTypes(cameraStack);
        for (AttachmentType attachmentType : attachmentTypes) {
            if (attachmentType.slot() == slot)
                return Optional.of(attachmentType);
        }
        return Optional.empty();
    }

    public Optional<ItemAndStack<FilmRollItem>> getFilm(ItemStack cameraStack) {
        return getAttachment(cameraStack, FILM_ATTACHMENT).map(ItemAndStack::new);
    }

    public void setFilm(ItemStack cameraStack, ItemStack filmStack) {
        setAttachment(cameraStack, FILM_ATTACHMENT, filmStack);
    }

    public Optional<ItemStack> getAttachment(ItemStack cameraStack, AttachmentType attachmentType) {
        if (cameraStack.getTag() != null && cameraStack.getTag().contains(attachmentType.id(), Tag.TAG_COMPOUND)) {
            ItemStack itemStack = ItemStack.of(cameraStack.getTag().getCompound(attachmentType.id()));
            if (!itemStack.isEmpty())
                return Optional.of(itemStack);
        }
        return Optional.empty();
    }

    public void setAttachment(ItemStack cameraStack, AttachmentType attachmentType, ItemStack attachmentStack) {
        if (attachmentStack.isEmpty()) {
            if (cameraStack.getTag() != null)
                cameraStack.getOrCreateTag().remove(attachmentType.id());
        } else {
            Preconditions.checkState(attachmentType.matches(attachmentStack),
                    attachmentStack + " is not valid for the '" + attachmentType + "' attachment type.");

            cameraStack.getOrCreateTag().put(attachmentType.id(), attachmentStack.save(new CompoundTag()));
        }

        if (attachmentType == LENS_ATTACHMENT)
            setZoom(cameraStack, getFocalRange(cameraStack).min());
    }

    // ---

    /**
     * Returns all possible Shutter Speeds for this camera.
     */
    @SuppressWarnings("unused")
    public List<ShutterSpeed> getAllShutterSpeeds(ItemStack cameraStack) {
        return SHUTTER_SPEEDS;
    }

    public ShutterSpeed getShutterSpeed(ItemStack cameraStack) {
        return ShutterSpeed.loadOrDefault(cameraStack.getOrCreateTag());
    }

    public void setShutterSpeed(ItemStack cameraStack, ShutterSpeed shutterSpeed) {
        shutterSpeed.save(cameraStack.getOrCreateTag());
    }

    public float getFocalLength(ItemStack cameraStack) {
        return cameraStack.hasTag() ? cameraStack.getOrCreateTag().getFloat("Zoom") : getFocalRange(cameraStack).min();
    }

    public void setZoom(ItemStack cameraStack, double focalLength) {
        cameraStack.getOrCreateTag().putDouble("Zoom", focalLength);
    }

    public CompositionGuide getCompositionGuide(ItemStack cameraStack) {
        if (!cameraStack.hasTag() || !cameraStack.getOrCreateTag().contains("CompositionGuide", Tag.TAG_STRING))
            return CompositionGuides.NONE;

        return CompositionGuides.byIdOrNone(cameraStack.getOrCreateTag().getString("CompositionGuide"));
    }

    public void setCompositionGuide(ItemStack cameraStack, CompositionGuide guide) {
        cameraStack.getOrCreateTag().putString("CompositionGuide", guide.getId());
    }

    public FlashMode getFlashMode(ItemStack cameraStack) {
        if (!cameraStack.hasTag() || !cameraStack.getOrCreateTag().contains("FlashMode", Tag.TAG_STRING))
            return FlashMode.OFF;

        return FlashMode.byIdOrOff(cameraStack.getOrCreateTag().getString("FlashMode"));
    }

    public void setFlashMode(ItemStack cameraStack, FlashMode flashMode) {
        cameraStack.getOrCreateTag().putString("FlashMode", flashMode.getId());
    }

    public boolean hasInterplanarProjectorFilter(ItemStack cameraStack) {
        return getAttachment(cameraStack, FILTER_ATTACHMENT)
                .map(stack -> stack.getItem() instanceof InterplanarProjectorItem)
                .orElse(false);
    }
}
