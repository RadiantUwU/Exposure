package io.github.mortuusars.exposure.network.fabric;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.exposure.network.PacketDirection;
import io.github.mortuusars.exposure.network.packet.ExposureDataPartPacket;
import io.github.mortuusars.exposure.network.packet.IPacket;
import io.github.mortuusars.exposure.network.packet.server.*;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class PacketsImpl {
    @Nullable
    private static MinecraftServer server;

    public static void registerC2SPackets() {
        ServerPlayNetworking.registerGlobalReceiver(ExposureDataPartPacket.ID, new ServerHandler(ExposureDataPartPacket::fromBuffer));

        ServerPlayNetworking.registerGlobalReceiver(OpenCameraAttachmentsPacketC2SP.ID, new ServerHandler(OpenCameraAttachmentsPacketC2SP::fromBuffer));
        ServerPlayNetworking.registerGlobalReceiver(DeactivateCamerasInHandC2SP.ID, new ServerHandler(DeactivateCamerasInHandC2SP::fromBuffer));
        ServerPlayNetworking.registerGlobalReceiver(CameraSetZoomC2SP.ID, new ServerHandler(CameraSetZoomC2SP::fromBuffer));
        ServerPlayNetworking.registerGlobalReceiver(CameraSetCompositionGuideC2SP.ID, new ServerHandler(CameraSetCompositionGuideC2SP::fromBuffer));
        ServerPlayNetworking.registerGlobalReceiver(CameraSetFlashModeC2SP.ID, new ServerHandler(CameraSetFlashModeC2SP::fromBuffer));
        ServerPlayNetworking.registerGlobalReceiver(CameraSetShutterSpeedC2SP.ID, new ServerHandler(CameraSetShutterSpeedC2SP::fromBuffer));
        ServerPlayNetworking.registerGlobalReceiver(CameraInHandAddFrameC2SP.ID, new ServerHandler(CameraInHandAddFrameC2SP::fromBuffer));
        ServerPlayNetworking.registerGlobalReceiver(CameraSetSelfieModeC2SP.ID, new ServerHandler(CameraSetSelfieModeC2SP::fromBuffer));
        ServerPlayNetworking.registerGlobalReceiver(QueryExposureDataC2SP.ID, new ServerHandler(QueryExposureDataC2SP::fromBuffer));

        ServerPlayNetworking.registerGlobalReceiver(AlbumSyncNoteC2SP.ID, new ServerHandler(AlbumSyncNoteC2SP::fromBuffer));
        ServerPlayNetworking.registerGlobalReceiver(AlbumSignC2SP.ID, new ServerHandler(AlbumSignC2SP::fromBuffer));
    }

    public static void registerS2CPackets() {
        ClientPackets.registerS2CPackets();
    }

    public static void sendToServer(IPacket packet) {
        ClientPackets.sendToServer(packet);
    }

    public static void sendToClient(IPacket packet, ServerPlayer player) {
        ServerPlayNetworking.send(player, packet.getId(), packet.toBuffer(PacketByteBufs.create()));
    }

    public static void sendToAllClients(IPacket packet) {
        if (server == null) {
            LogUtils.getLogger().warn("Cannot send a packet to all players. Server is not present.");
            return;
        }

        FriendlyByteBuf packetBuffer = packet.toBuffer(PacketByteBufs.create());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, packet.getId(), packetBuffer);
        }
    }

    public static void onServerStarting(MinecraftServer server) {
        // Store server to access from static context:
        PacketsImpl.server = server;
    }

    public static void onServerStopped(MinecraftServer server) {
        PacketsImpl.server = null;
    }

    private record ServerHandler(Function<FriendlyByteBuf, IPacket> decodeFunction) implements ServerPlayNetworking.PlayChannelHandler {
        @Override
        public void receive(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, FriendlyByteBuf buf, PacketSender responseSender) {
            IPacket packet = decodeFunction.apply(buf);
            packet.handle(PacketDirection.TO_SERVER, player);
        }
    }
}
