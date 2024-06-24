package io.github.mortuusars.exposure.advancement.trigger;

import com.google.gson.JsonObject;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.advancement.predicate.CameraPredicate;
import io.github.mortuusars.exposure.advancement.predicate.ExposurePredicate;
import io.github.mortuusars.exposure.item.CameraItem;
import io.github.mortuusars.exposure.util.ItemAndStack;
import net.minecraft.advancements.critereon.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FrameExposedTrigger extends SimpleCriterionTrigger<FrameExposedTrigger.TriggerInstance> {
    public static final ResourceLocation ID = Exposure.resource("frame_exposed");

    public @NotNull ResourceLocation getId() {
        return ID;
    }

    @Override
    protected @NotNull TriggerInstance createInstance(JsonObject json, @NotNull ContextAwarePredicate predicate,
                                                      @NotNull DeserializationContext deserializationContext) {
        CameraPredicate camera = CameraPredicate.fromJson(json.get("camera"));
        ExposurePredicate exposure = ExposurePredicate.fromJson(json.get("exposure"));
        LocationPredicate location = LocationPredicate.fromJson(json.get("location"));
        return new TriggerInstance(predicate, camera, exposure, location);
    }

    public void trigger(ServerPlayer player, ItemAndStack<CameraItem> camera, CompoundTag frame, List<Entity> entitiesInFrame) {
        this.trigger(player, triggerInstance -> triggerInstance.matches(player, camera, frame, entitiesInFrame));
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final CameraPredicate cameraPredicate;
        private final ExposurePredicate exposurePredicate;
        private final LocationPredicate locationPredicate;

        public TriggerInstance(ContextAwarePredicate predicate, CameraPredicate cameraPredicate, ExposurePredicate exposurePredicate, LocationPredicate locationPredicate) {
            super(ID, predicate);
            this.cameraPredicate = cameraPredicate;
            this.locationPredicate = locationPredicate;
            this.exposurePredicate = exposurePredicate;
        }

        public boolean matches(ServerPlayer player, ItemAndStack<CameraItem> camera, CompoundTag frame, List<Entity> entitiesInFrame) {
            return cameraPredicate.matches(camera)
                    && exposurePredicate.matches(player, frame, entitiesInFrame)
                    && locationPredicate.matches(player.serverLevel(), player.getX(), player.getY(), player.getZ());
        }

        public @NotNull JsonObject serializeToJson(@NotNull SerializationContext conditions) {
            JsonObject jsonobject = super.serializeToJson(conditions);
            jsonobject.add("camera", this.cameraPredicate.serializeToJson());
            jsonobject.add("exposure", this.exposurePredicate.serializeToJson());
            jsonobject.add("location", this.locationPredicate.serializeToJson());
            return jsonobject;
        }
    }
}