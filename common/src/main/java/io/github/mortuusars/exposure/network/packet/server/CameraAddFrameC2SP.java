package io.github.mortuusars.exposure.network.packet.server;

import com.google.common.base.Preconditions;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.camera.Camera;
import io.github.mortuusars.exposure.camera.infrastructure.FrameData;
import io.github.mortuusars.exposure.item.CameraItem;
import io.github.mortuusars.exposure.item.InterplanarProjectorItem;
import io.github.mortuusars.exposure.network.PacketDirection;
import io.github.mortuusars.exposure.network.packet.IPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record CameraAddFrameC2SP(InteractionHand hand, CompoundTag frame, List<UUID> entitiesInFrameIds) implements IPacket {
    public static final ResourceLocation ID = Exposure.resource("camera_add_frame");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    public FriendlyByteBuf toBuffer(FriendlyByteBuf buffer) {
        buffer.writeEnum(hand);
        buffer.writeNbt(frame);
        buffer.writeInt(entitiesInFrameIds.size());
        for (UUID uuid : entitiesInFrameIds) {
            buffer.writeUUID(uuid);
        }
        return buffer;
    }

    public static CameraAddFrameC2SP fromBuffer(FriendlyByteBuf buffer) {
        InteractionHand hand = buffer.readEnum(InteractionHand.class);
        @Nullable CompoundTag frame = buffer.readAnySizeNbt();
        if (frame == null)
            frame = new CompoundTag();

        int entitiesCount = buffer.readInt();
        List<UUID> entities = new ArrayList<>();
        for (int i = 0; i < entitiesCount; i++) {
            entities.add(buffer.readUUID());
        }

        return new CameraAddFrameC2SP(hand, frame, entities);
    }

    @Override
    public boolean handle(PacketDirection direction, @Nullable Player player) {
        Preconditions.checkState(player != null, "Cannot handle packet: Player was null");
        ServerPlayer serverPlayer = ((ServerPlayer) player);

        Camera.getCamera(player).ifPresentOrElse(camera -> {
            CameraItem cameraItem = camera.get().getItem();
            cameraItem.addFrame(serverPlayer, camera.get().getStack(), frame, getEntities(serverPlayer.serverLevel()));
            cameraItem.onFrameAdded(serverPlayer, camera.get().getStack(), frame, getEntities(serverPlayer.serverLevel()));
        }, () -> Exposure.LOGGER.error("Cannot add frame. Player is not using a Camera."));

        ItemStack cameraStack = player.getItemInHand(hand);
        if (!(cameraStack.getItem() instanceof CameraItem cameraItem))
            throw new IllegalStateException("Item in hand in not a Camera.");

        if (frame.getBoolean(FrameData.PROJECTED)) {
            cameraItem.getAttachment(cameraStack, CameraItem.FILTER_ATTACHMENT).ifPresent(filter -> {
                if (filter.getItem() instanceof InterplanarProjectorItem interplanarProjector) {
                    player.level().playSound(player, player, Exposure.SoundEvents.INTERPLANAR_PROJECT.get(),
                            SoundSource.PLAYERS, 0.8f, 1f);

                    if (interplanarProjector.isConsumable(filter)) {
                        filter.shrink(1);
                        cameraItem.setAttachment(cameraStack, CameraItem.FILTER_ATTACHMENT, filter);
                    }
                }
            });
        }
        return true;
    }

    private List<Entity> getEntities(ServerLevel level) {
        List<Entity> entitiesInFrame = new ArrayList<>();
        for (UUID uuid : entitiesInFrameIds) {
            @Nullable Entity entity = level.getEntity(uuid);
            if (entity != null)
                entitiesInFrame.add(entity);
        }
        return entitiesInFrame;
    }
}
