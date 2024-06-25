package io.github.mortuusars.exposure.data;

import io.github.mortuusars.exposure.camera.infrastructure.FocalRange;
import io.github.mortuusars.exposure.network.Packets;
import io.github.mortuusars.exposure.network.packet.IPacket;
import io.github.mortuusars.exposure.network.packet.client.SyncLensesS2CP;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Lenses {
    private static ConcurrentMap<Ingredient, FocalRange> lenses = new ConcurrentHashMap<>();

    public static void reload(ConcurrentMap<Ingredient, FocalRange> newLenses) {
        lenses.clear();
        lenses = newLenses;
    }

    public static Optional<FocalRange> getFocalRangeOf(ItemStack stack) {
        for (var lens : lenses.entrySet()) {
            if (lens.getKey().test(stack))
                return Optional.of(lens.getValue());
        }

        return Optional.empty();
    }

    public static IPacket getSyncToClientPacket() {
        return new SyncLensesS2CP(new ConcurrentHashMap<>(lenses));
    }

    public static void onDatapackSync(@Nullable ServerPlayer joiningPlayer) {
        IPacket packet = getSyncToClientPacket();

        if (joiningPlayer != null)
            Packets.sendToClient(packet, joiningPlayer);
        else
            Packets.sendToAllClients(packet);
    }
}
