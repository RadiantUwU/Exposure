package io.github.mortuusars.exposure.network.packet.client;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.camera.infrastructure.FocalRange;
import io.github.mortuusars.exposure.network.PacketDirection;
import io.github.mortuusars.exposure.network.handler.ClientPacketsHandler;
import io.github.mortuusars.exposure.network.packet.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public record SyncLensesS2CP(ConcurrentMap<Ingredient, FocalRange> lenses) implements IPacket {
    public static final ResourceLocation ID = Exposure.resource("sync_lenses");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public FriendlyByteBuf toBuffer(FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.lenses.size());
        for (var lens : this.lenses.entrySet()) {
            Ingredient ingredient = lens.getKey();
            ingredient.toNetwork(buffer);
            FocalRange focalRange = lens.getValue();
            focalRange.toNetwork(buffer);
        }
        return buffer;
    }

    public static SyncLensesS2CP fromBuffer(FriendlyByteBuf buffer) {
        ConcurrentMap<Ingredient, FocalRange> lenses = new ConcurrentHashMap<>();

        int lensCount = buffer.readVarInt();
        for (int i = 0; i < lensCount; i++) {
            Ingredient ingredient = Ingredient.fromNetwork(buffer);
            FocalRange focalRange = FocalRange.fromNetwork(buffer);
            lenses.put(ingredient, focalRange);
        }

        return new SyncLensesS2CP(lenses);
    }

    @Override
    public boolean handle(PacketDirection direction, @Nullable Player player) {
        ClientPacketsHandler.syncLenses(this);
        return true;
    }
}
