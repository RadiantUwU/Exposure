package io.github.mortuusars.exposure.camera.infrastructure;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.logging.LogUtils;
import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.data.Lenses;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class FocalRange implements StringRepresentable {
    public static final int ALLOWED_MIN = 10;
    public static final int ALLOWED_MAX = 300;

    private final int min;
    private final int max;

    public FocalRange(int min, int max) {
        Preconditions.checkArgument(ALLOWED_MIN <= min && min <= ALLOWED_MAX,
                min + " is not in allowed range for 'min'.");
        Preconditions.checkArgument(ALLOWED_MIN <= max && max <= ALLOWED_MAX,
                max + " is not in allowed range for 'max'.");
        Preconditions.checkArgument(min <= max,
                "'min' should not be larger than 'max'. min: " + min + ", max: " + max);
        this.min = min;
        this.max = max;
    }

    public FocalRange(int fixedValue) {
        Preconditions.checkArgument(ALLOWED_MIN <= fixedValue && fixedValue <= ALLOWED_MAX,
                fixedValue + " is not in allowed range: " + ALLOWED_MIN + "-" + ALLOWED_MAX);
        this.min = fixedValue;
        this.max = fixedValue;
    }

    public static FocalRange fromNetwork(FriendlyByteBuf buffer) {
        int min = buffer.readInt();
        int max = buffer.readInt();
        return new FocalRange(min, max);
    }

    public void toNetwork(FriendlyByteBuf buffer) {
        buffer.writeInt(min);
        buffer.writeInt(max);
    }

    public boolean isPrime() {
        return min == max;
    }

    public static FocalRange ofStack(ItemStack stack) {
        if (stack.isEmpty())
            return getDefault();

        if (!stack.is(Exposure.Tags.Items.LENSES)) {
            LogUtils.getLogger().error(stack + " is not a valid lens. Should have '#exposure:lenses' tag.");
            return getDefault();
        }

        return Lenses.getFocalRangeOf(stack).orElse(getDefault());
    }

    public static @NotNull FocalRange getDefault() {
        return parse(Config.Common.CAMERA_DEFAULT_FOCAL_RANGE.get());
    }

    @Override
    public @NotNull String getSerializedName() {
        return isPrime() ? Integer.toString(min) : min + "-" + max;
    }

    public static FocalRange parse(String value) {
        int dashIndex = value.indexOf("-");
        if (dashIndex == -1) {
            int prime = Integer.parseInt(value);
            return new FocalRange(prime);
        }

        int min = Integer.parseInt(value.substring(0, dashIndex));
        int max = Integer.parseInt(value.substring(dashIndex + 1));
        return new FocalRange(min, max);
    }

    public static FocalRange fromJson(@Nullable JsonElement json) {
        if (json == null || json.isJsonNull())
            throw new JsonSyntaxException("Item cannot be null");

        if (json.isJsonPrimitive()) {
            int fixedValue = json.getAsInt();
            return new FocalRange(fixedValue);
        }

        if (json.isJsonObject()) {
            JsonObject obj = json.getAsJsonObject();
            int min = obj.get("min").getAsInt();
            int max = obj.get("max").getAsInt();
            return new FocalRange(min, max);
        }

        throw new JsonSyntaxException("Invalid FocalRange json. Expected a number or json object with 'min' and 'max'.");
    }

    public int min() {
        return min;
    }

    public int max() {
        return max;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (FocalRange) obj;
        return this.min == that.min && this.max == that.max;
    }

    @Override
    public int hashCode() {
        return Objects.hash(min, max);
    }

    @Override
    public String toString() {
        if (isPrime())
            return "FocalRange[" + "fixed=" + min + ']';
        else
            return "FocalRange[" + "min=" + min + ", " + "max=" + max + ']';
    }
}
