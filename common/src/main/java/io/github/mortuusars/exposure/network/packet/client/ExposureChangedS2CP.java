package io.github.mortuusars.exposure.network.packet.client;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.network.PacketDirection;
import io.github.mortuusars.exposure.network.handler.ClientPacketsHandler;
import io.github.mortuusars.exposure.network.packet.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public record ExposureChangedS2CP(String exposureId) implements IPacket {
    public static final ResourceLocation ID = Exposure.resource("exposure_changed");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    public FriendlyByteBuf toBuffer(FriendlyByteBuf buffer) {
        buffer.writeUtf(exposureId);
        return buffer;
    }

    public static ExposureChangedS2CP fromBuffer(FriendlyByteBuf buffer) {
        return new ExposureChangedS2CP(buffer.readUtf());
    }

    @Override
    public boolean handle(PacketDirection direction, @Nullable Player player) {
        ClientPacketsHandler.onExposureChanged(this);
        return true;
    }
}
