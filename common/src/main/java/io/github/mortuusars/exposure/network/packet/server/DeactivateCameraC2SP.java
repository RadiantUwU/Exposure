package io.github.mortuusars.exposure.network.packet.server;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.camera.Camera;
import io.github.mortuusars.exposure.network.PacketDirection;
import io.github.mortuusars.exposure.network.packet.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public record DeactivateCameraC2SP() implements IPacket {
    public static final ResourceLocation ID = Exposure.resource("deactivate_cameras");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    public static DeactivateCameraC2SP fromBuffer(FriendlyByteBuf buffer) {
        return new DeactivateCameraC2SP();
    }

    @Override
    public boolean handle(PacketDirection direction, @Nullable Player player) {
        if (player == null)
            throw new IllegalStateException("Cannot handle the packet: Player was null");

        Camera.getCamera(player).ifPresent(camera -> camera.deactivate(player));
        return true;
    }
}
