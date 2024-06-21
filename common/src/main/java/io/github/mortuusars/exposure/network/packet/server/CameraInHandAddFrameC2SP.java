package io.github.mortuusars.exposure.network.packet.server;

import com.google.common.base.Preconditions;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.camera.infrastructure.FrameData;
import io.github.mortuusars.exposure.item.CameraItem;
import io.github.mortuusars.exposure.item.InterplanarProjectorItem;
import io.github.mortuusars.exposure.network.PacketDirection;
import io.github.mortuusars.exposure.network.Packets;
import io.github.mortuusars.exposure.network.packet.IPacket;
import io.github.mortuusars.exposure.network.packet.client.OnFrameAddedS2CP;
import io.github.mortuusars.exposure.util.ItemAndStack;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CameraInHandAddFrameC2SP(InteractionHand hand, CompoundTag frame, List<UUID> entitiesInFrameIds) implements IPacket {
    public static final ResourceLocation ID = Exposure.resource("camera_in_hand_add_frame");

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

    public static CameraInHandAddFrameC2SP fromBuffer(FriendlyByteBuf buffer) {
        InteractionHand hand = buffer.readEnum(InteractionHand.class);
        @Nullable CompoundTag frame = buffer.readAnySizeNbt();
        if (frame == null)
            frame = new CompoundTag();

        int entitiesCount = buffer.readInt();
        List<UUID> entities = new ArrayList<>();
        for (int i = 0; i < entitiesCount; i++) {
            entities.add(buffer.readUUID());
        }

        return new CameraInHandAddFrameC2SP(hand, frame, entities);
    }

    @Override
    public boolean handle(PacketDirection direction, @Nullable Player player) {
        Preconditions.checkState(player != null, "Cannot handle packet: Player was null");
        ServerPlayer serverPlayer = ((ServerPlayer) player);

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

        cameraItem.addFrame(serverPlayer, cameraStack, hand, frame, getEntities(serverPlayer.serverLevel()));
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
