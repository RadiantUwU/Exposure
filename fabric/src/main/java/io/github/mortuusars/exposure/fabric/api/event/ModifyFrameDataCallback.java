package io.github.mortuusars.exposure.fabric.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Can be used to add additional data to the frame or modify existing data. This data can be used in advancements or quests afterward.
 * Fired only on the server side.
 */
public interface ModifyFrameDataCallback {
    Event<ModifyFrameDataCallback> EVENT = EventFactory.createArrayBacked(ModifyFrameDataCallback.class,
            (listeners) -> (player, cameraStack, frame, entitiesInFrame) -> {
                for (ModifyFrameDataCallback listener : listeners) {
                    listener.modifyFrameData(player, cameraStack, frame, entitiesInFrame);
                }
            });

    void modifyFrameData(ServerPlayer player, ItemStack cameraStack, CompoundTag frame, List<Entity> entitiesInFrame);
}
