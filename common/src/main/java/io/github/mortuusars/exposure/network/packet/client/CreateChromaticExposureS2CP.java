package io.github.mortuusars.exposure.network.packet.client;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.network.PacketDirection;
import io.github.mortuusars.exposure.network.handler.ClientPacketsHandler;
import io.github.mortuusars.exposure.network.packet.IPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public record CreateChromaticExposureS2CP(CompoundTag red, CompoundTag green, CompoundTag blue, String exposureId) implements IPacket {
    public static final ResourceLocation ID = Exposure.resource("create_chromatic_exposure");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    public FriendlyByteBuf toBuffer(FriendlyByteBuf buffer) {
        buffer.writeNbt(red);
        buffer.writeNbt(green);
        buffer.writeNbt(blue);
        buffer.writeUtf(exposureId);
        return buffer;
    }

    public static CreateChromaticExposureS2CP fromBuffer(FriendlyByteBuf buffer) {
        return new CreateChromaticExposureS2CP(buffer.readAnySizeNbt(), buffer.readAnySizeNbt(), buffer.readAnySizeNbt(), buffer.readUtf());
    }

    @Override
    public boolean handle(PacketDirection direction, @Nullable Player player) {
        ClientPacketsHandler.createChromaticExposure(this);
        return true;
    }
}
