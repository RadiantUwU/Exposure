package io.github.mortuusars.exposure.network.packet.server;

import com.google.common.base.Preconditions;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.camera.Camera;
import io.github.mortuusars.exposure.network.PacketDirection;
import io.github.mortuusars.exposure.network.packet.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public record CameraSetSelfieModeC2SP(boolean isInSelfieMode) implements IPacket {
    public static final ResourceLocation ID = Exposure.resource("camera_set_selfie_mode");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    public FriendlyByteBuf toBuffer(FriendlyByteBuf buffer) {
        buffer.writeBoolean(isInSelfieMode);
        return buffer;
    }

    public static CameraSetSelfieModeC2SP fromBuffer(FriendlyByteBuf buffer) {
        return new CameraSetSelfieModeC2SP(buffer.readBoolean());
    }

    @Override
    public boolean handle(PacketDirection direction, @Nullable Player player) {
        Preconditions.checkState(player != null, "Cannot handle packet: Player was null");

        Camera.getCamera(player).ifPresent(camera -> {
            camera.get().getItem().setSelfieModeWithEffects(player, camera.get().getStack(), isInSelfieMode);
        });

        return true;
    }
}