package io.github.mortuusars.exposure.advancement.predicate;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import io.github.mortuusars.exposure.item.CameraItem;
import io.github.mortuusars.exposure.util.ItemAndStack;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class CameraPredicate {
    public static final CameraPredicate ANY = new CameraPredicate(ItemPredicate.ANY, MinMaxBounds.Doubles.ANY,
            MinMaxBounds.Ints.ANY, ItemPredicate.ANY, ItemPredicate.ANY, ItemPredicate.ANY, ItemPredicate.ANY);

    private final ItemPredicate camera;
    private final MinMaxBounds.Doubles shutterSpeedMS;
    private final MinMaxBounds.Ints focalLength;
    private final ItemPredicate film;
    private final ItemPredicate flash;
    private final ItemPredicate lens;
    private final ItemPredicate filter;

    public CameraPredicate(ItemPredicate camera, MinMaxBounds.Doubles shutterSpeedMS,
                           MinMaxBounds.Ints focalLength, ItemPredicate film,
                           ItemPredicate flash, ItemPredicate lens, ItemPredicate filter) {
        this.camera = camera;
        this.shutterSpeedMS = shutterSpeedMS;
        this.focalLength = focalLength;
        this.film = film;
        this.flash = flash;
        this.lens = lens;
        this.filter = filter;
    }

    public boolean matches(ItemAndStack<CameraItem> camera) {
        if (this.equals(ANY)) {
            return true;
        }

        ItemStack stack = camera.getStack();
        CameraItem item = camera.getItem();

        return this.camera.matches(stack)
                && this.shutterSpeedMS.matches(item.getShutterSpeed(stack).getMilliseconds())
                && this.focalLength.matches(Mth.ceil(item.getFocalLength(stack)))
                && this.film.matches(item.getAttachment(stack, CameraItem.FILM_ATTACHMENT).orElse(ItemStack.EMPTY))
                && this.flash.matches(item.getAttachment(stack, CameraItem.FLASH_ATTACHMENT).orElse(ItemStack.EMPTY))
                && this.lens.matches(item.getAttachment(stack, CameraItem.LENS_ATTACHMENT).orElse(ItemStack.EMPTY))
                && this.filter.matches(item.getAttachment(stack, CameraItem.FILTER_ATTACHMENT).orElse(ItemStack.EMPTY));
    }

    public JsonElement serializeToJson() {
        if (this == ANY)
            return JsonNull.INSTANCE;

        JsonObject json = new JsonObject();
        json.add("camera", camera.serializeToJson());
        json.add("shutter_speed_ms", shutterSpeedMS.serializeToJson());
        json.add("focal_length", focalLength.serializeToJson());
        json.add("film", film.serializeToJson());
        json.add("flash", flash.serializeToJson());
        json.add("lens", lens.serializeToJson());
        json.add("filter", filter.serializeToJson());
        return json;
    }

    public static CameraPredicate fromJson(@Nullable JsonElement json) {
        if (json == null || json.isJsonNull())
            return ANY;

        JsonObject jsonObj = GsonHelper.convertToJsonObject(json, "camera");

        return new CameraPredicate(
                ItemPredicate.fromJson(jsonObj.getAsJsonObject("camera")),
                MinMaxBounds.Doubles.fromJson(jsonObj.getAsJsonObject("shutter_speed_ms")),
                MinMaxBounds.Ints.fromJson(jsonObj.getAsJsonObject("focal_length")),
                ItemPredicate.fromJson(jsonObj.getAsJsonObject("film")),
                ItemPredicate.fromJson(jsonObj.getAsJsonObject("flash")),
                ItemPredicate.fromJson(jsonObj.getAsJsonObject("lens")),
                ItemPredicate.fromJson(jsonObj.getAsJsonObject("filter")));
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CameraPredicate that = (CameraPredicate) o;
        return Objects.equals(camera, that.camera) && Objects.equals(shutterSpeedMS, that.shutterSpeedMS)
                && Objects.equals(focalLength, that.focalLength) && Objects.equals(film, that.film)
                && Objects.equals(flash, that.flash) && Objects.equals(lens, that.lens) && Objects.equals(filter, that.filter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(camera, shutterSpeedMS, focalLength, film, flash, lens, filter);
    }
}
