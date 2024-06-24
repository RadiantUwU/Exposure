package io.github.mortuusars.exposure.advancement.predicate;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import io.github.mortuusars.exposure.camera.infrastructure.FrameData;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.NbtPredicate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ExposurePredicate {
    public static final ExposurePredicate ANY = new ExposurePredicate(MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY,
            NbtPredicate.ANY, MinMaxBounds.Ints.ANY, EntityPredicate.ANY);

    private final MinMaxBounds.Ints dayTime;
    private final MinMaxBounds.Ints lightLevel;
    private final NbtPredicate nbt;
    private final MinMaxBounds.Ints entitiesInFrameCount;
    private final EntityPredicate entityInFrame;

    public ExposurePredicate(MinMaxBounds.Ints dayTime,
                             MinMaxBounds.Ints lightLevel,
                             NbtPredicate nbtPredicate,
                             MinMaxBounds.Ints entitiesInFrameCount,
                             EntityPredicate entityInFramePredicate) {
        this.dayTime = dayTime;
        this.lightLevel = lightLevel;
        this.nbt = nbtPredicate;
        this.entitiesInFrameCount = entitiesInFrameCount;
        this.entityInFrame = entityInFramePredicate;
    }

    public boolean matches(ServerPlayer player, CompoundTag frameTag, List<Entity> entitiesInFrame) {
        if (this.equals(ANY)) {
            return true;
        }

        return this.dayTime.matches(frameTag.contains(FrameData.DAYTIME, Tag.TAG_INT) ? frameTag.getInt(FrameData.DAYTIME) : -1)
                && this.lightLevel.matches(frameTag.contains(FrameData.LIGHT_LEVEL, Tag.TAG_INT) ? frameTag.getInt(FrameData.LIGHT_LEVEL) : -1)
                && this.entitiesInFrameCount.matches(entitiesInFrame.size())
                && this.nbt.matches(frameTag)
                && entitiesMatch(player, entitiesInFrame);
    }

    protected boolean entitiesMatch(ServerPlayer player, List<Entity> entitiesInFrame) {
        // Handles the case where the list is empty
        if (entityInFrame.equals(EntityPredicate.ANY)) {
            return true;
        }

        for (Entity entity : entitiesInFrame) {
            if (entityInFrame.matches(player, entity))
                return true;
        }

        return false;
    }

    public JsonElement serializeToJson() {
        if (this == ANY)
            return JsonNull.INSTANCE;

        JsonObject json = new JsonObject();
        json.add("day_time", lightLevel.serializeToJson());
        json.add("light_level", lightLevel.serializeToJson());
        json.add("nbt", nbt.serializeToJson());
        json.add("entities_count", entitiesInFrameCount.serializeToJson());
        json.add("entity_in_frame", entityInFrame.serializeToJson());

        return json;
    }

    public static ExposurePredicate fromJson(@Nullable JsonElement json) {
        if (json == null || json.isJsonNull())
            return ANY;

        JsonObject jsonobject = GsonHelper.convertToJsonObject(json, "exposure");

        return new ExposurePredicate(
                MinMaxBounds.Ints.fromJson(jsonobject.get("day_time")),
                MinMaxBounds.Ints.fromJson(jsonobject.get("light_level")),
                NbtPredicate.fromJson(jsonobject.get("nbt")),
                MinMaxBounds.Ints.fromJson(jsonobject.get("entities_count")),
                EntityPredicate.fromJson(jsonobject.get("entity_in_frame")));
    }
}
