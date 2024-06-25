package io.github.mortuusars.exposure.network.packet.server;

import com.google.common.base.Preconditions;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.camera.Camera;
import io.github.mortuusars.exposure.camera.infrastructure.ShutterSpeed;
import io.github.mortuusars.exposure.network.PacketDirection;
import io.github.mortuusars.exposure.network.packet.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public record CameraSetShutterSpeedC2SP(ShutterSpeed shutterSpeed) implements IPacket {
    public static final ResourceLocation ID = Exposure.resource("camera_set_shutter_speed");
    @Override
    public ResourceLocation getId() {
        return ID;
    }

    public FriendlyByteBuf toBuffer(FriendlyByteBuf buffer) {
        shutterSpeed.toBuffer(buffer);
        return buffer;
    }

    public static CameraSetShutterSpeedC2SP fromBuffer(FriendlyByteBuf buffer) {
        return new CameraSetShutterSpeedC2SP(ShutterSpeed.fromBuffer(buffer));
    }

    @Override
    public boolean handle(PacketDirection direction, @Nullable Player player) {
        Preconditions.checkState(player != null, "Cannot handle packet {}: Player was null", ID);
        Camera.getCamera(player).ifPresent(c -> c.get().getItem().setShutterSpeed(c.get().getStack(), shutterSpeed));
        return true;
    }
}
