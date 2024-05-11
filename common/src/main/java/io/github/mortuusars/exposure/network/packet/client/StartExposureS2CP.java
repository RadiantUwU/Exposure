package io.github.mortuusars.exposure.network.packet.client;

import com.google.common.base.Preconditions;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.network.PacketDirection;
import io.github.mortuusars.exposure.network.handler.ClientPacketsHandler;
import io.github.mortuusars.exposure.network.packet.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record StartExposureS2CP(@NotNull String exposureId, @NotNull InteractionHand activeHand,
                                boolean flashHasFired, int lightLevel) implements IPacket {
    public static final ResourceLocation ID = Exposure.resource("start_exposure");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    public FriendlyByteBuf toBuffer(FriendlyByteBuf buffer) {
        Preconditions.checkState(!exposureId.isEmpty(), "path cannot be empty.");
        buffer.writeUtf(exposureId);
        buffer.writeEnum(activeHand);
        buffer.writeBoolean(flashHasFired);
        buffer.writeInt(lightLevel);
        return buffer;
    }

    public static StartExposureS2CP fromBuffer(FriendlyByteBuf buffer) {
        return new StartExposureS2CP(buffer.readUtf(), buffer.readEnum(InteractionHand.class), buffer.readBoolean(), buffer.readInt());
    }

    @Override
    public boolean handle(PacketDirection direction, @Nullable Player player) {
        ClientPacketsHandler.startExposure(this);
        return true;
    }
}
