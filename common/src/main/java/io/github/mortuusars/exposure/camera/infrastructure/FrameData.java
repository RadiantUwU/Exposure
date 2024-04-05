package io.github.mortuusars.exposure.camera.infrastructure;

import com.mojang.datafixers.util.Either;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class FrameData {
    public static final String ID = "Id";
    public static final String TEXTURE = "Texture";
    public static final String TYPE = "Type";
    public static final String CHROMATIC = "Chromatic";
    public static final String CHROMATIC_CHANNEL = "ChromaticChannel";
    public static final String RED_CHANNEL = "RedChannel";
    public static final String GREEN_CHANNEL = "GreenChannel";
    public static final String BLUE_CHANNEL = "BlueChannel";
    public static final String SHUTTER_SPEED_MS = "ShutterSpeedMS";
    public static final String FOCAL_LENGTH = "FocalLength";
    public static final String TIMESTAMP = "Timestamp";
    public static final String PHOTOGRAPHER = "Photographer";
    public static final String PHOTOGRAPHER_ID = "PhotographerId";
    public static final String FLASH = "Flash";
    public static final String SELFIE = "Selfie";
    public static final String POSITION = "Pos";
    public static final String DIMENSION = "Dimension";
    public static final String BIOME = "Biome";
    public static final String UNDERWATER = "Underwater";
    public static final String IN_CAVE = "InCave";
    public static final String WEATHER = "Weather";
    public static final String LIGHT_LEVEL = "LightLevel";
    public static final String SUN_ANGLE = "SunAngle";
    public static final String ENTITIES_IN_FRAME = "Entities";

    public static final String ENTITY_ID = "Id";
    public static final String ENTITY_POSITION = "Pos";
    public static final String ENTITY_DISTANCE = "Distance";
    public static final String ENTITY_PLAYER_NAME = "Name";

    /**
     * If both are defined - 'Id' takes priority.
     * @return 'Either.left("")' if not found.
     */
    public static Either<String, ResourceLocation> getIdOrTexture(@NotNull CompoundTag tag) {
        String id = tag.getString(ID);
        if (!id.isEmpty())
            return Either.left(id);

        String texture = tag.getString(TEXTURE);
        if (!texture.isEmpty())
            return Either.right(new ResourceLocation(texture));

        return Either.left("");
    }
}
