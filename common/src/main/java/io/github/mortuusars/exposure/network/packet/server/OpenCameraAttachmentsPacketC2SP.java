package io.github.mortuusars.exposure.network.packet.server;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.item.CameraItem;
import io.github.mortuusars.exposure.network.PacketDirection;
import io.github.mortuusars.exposure.network.packet.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public record OpenCameraAttachmentsPacketC2SP(int cameraSlotIndex) implements IPacket {
    public static final ResourceLocation ID = Exposure.resource("open_camera_attachments");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public FriendlyByteBuf toBuffer(FriendlyByteBuf buffer) {
        buffer.writeInt(cameraSlotIndex);
        return buffer;
    }

    public static OpenCameraAttachmentsPacketC2SP fromBuffer(FriendlyByteBuf buffer) {
        return new OpenCameraAttachmentsPacketC2SP(buffer.readInt());
    }

    @Override
    public boolean handle(PacketDirection direction, @Nullable Player player) {
        if (player == null)
            throw new IllegalStateException("Cannot handle the packet: Player was null");

        ItemStack stack = player.getInventory().getItem(cameraSlotIndex);
        if (stack.getItem() instanceof CameraItem cameraItem)
            cameraItem.openCameraAttachmentsMenu(player, cameraSlotIndex);

        return true;
    }
}
