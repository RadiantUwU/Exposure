package io.github.mortuusars.exposure;

import io.github.mortuusars.exposure.camera.infrastructure.FilmType;
import io.github.mortuusars.exposure.camera.infrastructure.FocalRange;
import io.github.mortuusars.exposure.data.ExposureSize;
import net.minecraftforge.common.ForgeConfigSpec;

import java.awt.*;
import java.util.List;

/**
 * Using ForgeConfigApiPort on fabric allows using forge config in both environments and without extra dependencies on forge.
 */
public class Config {
    public static class Common {
        public static final ForgeConfigSpec SPEC;

        // Camera
        public static final ForgeConfigSpec.ConfigValue<String> CAMERA_DEFAULT_FOCAL_RANGE;

        // Lightroom
        public static final ForgeConfigSpec.IntValue LIGHTROOM_BW_PRINT_TIME;
        public static final ForgeConfigSpec.IntValue LIGHTROOM_COLOR_PRINT_TIME;
        public static final ForgeConfigSpec.IntValue LIGHTROOM_CHROMATIC_PRINT_TIME;
        public static final ForgeConfigSpec.IntValue LIGHTROOM_EXPERIENCE_PER_PRINT_BW;
        public static final ForgeConfigSpec.IntValue LIGHTROOM_EXPERIENCE_PER_PRINT_COLOR;
        public static final ForgeConfigSpec.IntValue LIGHTROOM_EXPERIENCE_PER_PRINT_CHROMATIC;

        // Photographs
        public static final ForgeConfigSpec.IntValue STACKED_PHOTOGRAPHS_MAX_SIZE;

        // Compatibility
        public static final ForgeConfigSpec.BooleanValue CREATE_SPOUT_DEVELOPING_ENABLED;
        public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CREATE_SPOUT_DEVELOPING_SEQUENCE_COLOR;
        public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CREATE_SPOUT_DEVELOPING_SEQUENCE_BW;

        static {
            ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

            builder.push("Camera");
            {
                CAMERA_DEFAULT_FOCAL_RANGE = builder
                        .comment("Default focal range of the camera (without a lens attached).",
                                "Allowed range: " + FocalRange.ALLOWED_MIN + "-" + FocalRange.ALLOWED_MAX,
                                "Default: 18-55")
                        .define("DefaultFocalRange", "18-55");
            }
            builder.pop();

            builder.push("Lightroom");
            {
                LIGHTROOM_BW_PRINT_TIME = builder
                        .comment("Time in ticks to print black and white photograph. Default: 80")
                        .defineInRange("BlackAndWhitePrintTime", 80, 1, Integer.MAX_VALUE);
                LIGHTROOM_COLOR_PRINT_TIME = builder
                        .comment("Time in ticks to print color photograph. Default: 200")
                        .defineInRange("ColorPrintTime", 200, 1, Integer.MAX_VALUE);
                LIGHTROOM_CHROMATIC_PRINT_TIME = builder
                        .comment("Time in ticks to print one channel of a chromatic photograph. Default: 120")
                        .defineInRange("ChromaticPrintTime", 120, 1, Integer.MAX_VALUE);
                LIGHTROOM_EXPERIENCE_PER_PRINT_BW = builder
                        .comment("Amount of experience awarded per printed black and white Photograph. Set to 0 to disable. Default: 2")
                        .defineInRange("ExperiencePerPrintBW", 2, 0, 99);
                LIGHTROOM_EXPERIENCE_PER_PRINT_COLOR = builder
                        .comment("Amount of experience awarded per printed color Photograph. Set to 0 to disable. Default: 4")
                        .defineInRange("ExperiencePerPrintColor", 4, 0, 99);
                LIGHTROOM_EXPERIENCE_PER_PRINT_CHROMATIC = builder
                        .comment("Amount of experience awarded per printed chromatic Photograph (when all three channels have been printed). Set to 0 to disable. Default: 5")
                        .defineInRange("ExperiencePerPrintChromatic", 5, 0, 99);
            }
            builder.pop();

            builder.push("Photographs");
            {
                STACKED_PHOTOGRAPHS_MAX_SIZE = builder
                        .comment("How many photographs can be stacked in Stacked Photographs item. Default: 16.",
                                "Larger numbers may cause errors. Use at your own risk.")
                        .defineInRange("StackedPhotographsMaxSize", 16, 2, 64);
            }
            builder.pop();

            builder.push("Integration");
            {
                builder.push("Create");
                {
                    builder.push("SequencedSpoutFilmDeveloping");
                    {
                        CREATE_SPOUT_DEVELOPING_ENABLED = builder
                                .comment("Film can be developed with create Spout Filling. Default: true")
                                .define("Enabled", true);
                        CREATE_SPOUT_DEVELOPING_SEQUENCE_COLOR = builder
                                .comment("Fluid spouting sequence required to develop color film.")
                                .defineList("ColorFilmSequence", PlatformHelper.getDefaultSpoutDevelopmentColorSequence(), o -> true);
                        CREATE_SPOUT_DEVELOPING_SEQUENCE_BW = builder
                                .comment("Fluid spouting sequence required to develop black and white film.")
                                .defineList("BlackAndWhiteFilmSequence", PlatformHelper.getDefaultSpoutDevelopmentBWSequence(), o -> true);
                    }
                    builder.pop();
                }
                builder.pop();
            }
            builder.pop();

            SPEC = builder.build();
        }

        public static ForgeConfigSpec.ConfigValue<List<? extends String>> spoutDevelopingSequence(FilmType filmType) {
            return filmType == FilmType.COLOR ? CREATE_SPOUT_DEVELOPING_SEQUENCE_COLOR : CREATE_SPOUT_DEVELOPING_SEQUENCE_BW;
        }
    }

    public static class Client {
        public static final ForgeConfigSpec SPEC;

        // UI
        public static final ForgeConfigSpec.BooleanValue RECIPE_TOOLTIPS_WITHOUT_JEI;
        public static final ForgeConfigSpec.BooleanValue CAMERA_SHOW_OPEN_WITH_SNEAK_IN_TOOLTIP;
        public static final ForgeConfigSpec.BooleanValue CAMERA_SHOW_FILM_BAR_ON_ITEM;
        public static final ForgeConfigSpec.BooleanValue PHOTOGRAPH_SHOW_PHOTOGRAPHER_IN_TOOLTIP;
        public static final ForgeConfigSpec.BooleanValue PHOTOGRAPH_IN_HAND_HIDE_CROSSHAIR;
        public static final ForgeConfigSpec.BooleanValue SIGNED_ALBUM_GLINT;
        public static final ForgeConfigSpec.BooleanValue ALBUM_SHOW_PHOTOS_COUNT;

        // CAPTURE
        public static final ForgeConfigSpec.IntValue FLASH_CAPTURE_DELAY_TICKS;

        // VIEWFINDER
        public static final ForgeConfigSpec.DoubleValue VIEWFINDER_ZOOM_SENSITIVITY_MODIFIER;
        public static final ForgeConfigSpec.ConfigValue<String> VIEWFINDER_BACKGROUND_COLOR;
        public static final ForgeConfigSpec.ConfigValue<String> VIEWFINDER_FONT_MAIN_COLOR;
        public static final ForgeConfigSpec.ConfigValue<String> VIEWFINDER_FONT_SECONDARY_COLOR;
        public static final ForgeConfigSpec.BooleanValue VIEWFINDER_MIDDLE_CLICK_CONTROLS;

        // IMAGE SAVING
        public static final ForgeConfigSpec.BooleanValue SAVE_EXPOSURE_TO_FILE_WHEN_VIEWED;
        public static final ForgeConfigSpec.BooleanValue EXPOSURE_SAVING_LEVEL_SUBFOLDER;
        public static final ForgeConfigSpec.EnumValue<ExposureSize> EXPOSURE_SAVING_SIZE;

        static {
            ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

            {
                builder.push("UI");

                RECIPE_TOOLTIPS_WITHOUT_JEI = builder
                        .comment("Tooltips for Developing Film Rolls and Copying Photographs will be shown on Film Rolls and Photographs respectively, describing the crafting recipe. ",
                                "Only when JEI is not installed. (Only JEI shows these recipes, not REI or EMI)")
                        .define("RecipeTooltipsWithoutJei", true);

                CAMERA_SHOW_OPEN_WITH_SNEAK_IN_TOOLTIP = builder
                        .comment("'Use while sneaking to open' message will be shown in Camera item tooltip.")
                        .define("CameraSneakOpenTooltip", true);

                CAMERA_SHOW_FILM_BAR_ON_ITEM = builder
                        .comment("Film Roll fullness bar will be shown on the Camera item. Same as it does on Film Roll item.",
                                "Default: false")
                        .define("CameraShowsFilmBar", false);

                PHOTOGRAPH_SHOW_PHOTOGRAPHER_IN_TOOLTIP = builder
                        .comment("Photographer name will be shown in Photograph's tooltip.")
                        .define("PhotographPhotographerNameTooltip", false);

                PHOTOGRAPH_IN_HAND_HIDE_CROSSHAIR = builder
                        .comment("Crosshair will not get in the way when holding a photograph.")
                        .define("PhotographInHandHideCrosshair", true);

                ALBUM_SHOW_PHOTOS_COUNT = builder
                        .comment("Album will show how many photographs they contain in a tooltip.")
                        .define("AlbumShowPhotosCount", true);

                SIGNED_ALBUM_GLINT = builder
                        .comment("Signed Album item will have an enchantment glint.")
                        .define("SignedAlbumGlint", true);


                {
                    builder.push("Viewfinder");
                    VIEWFINDER_ZOOM_SENSITIVITY_MODIFIER = builder
                            .comment("Mouse sensitivity modifier per 5 degrees of fov. Set to 0 to disable sensitivity changes.")
                            .defineInRange("ZoomSensitivityModifier", 0.048, 0.0, 1.0);
                    VIEWFINDER_BACKGROUND_COLOR = builder.define("BackgroundColorHex", "FA1F1D1B");
                    VIEWFINDER_FONT_MAIN_COLOR = builder.define("FontMainColorHex", "FF2B2622");
                    VIEWFINDER_FONT_SECONDARY_COLOR = builder.define("FontSecondaryColorHex", "FF7A736C");
                    VIEWFINDER_MIDDLE_CLICK_CONTROLS = builder
                            .comment("Clicking middle mouse button will open Viewfinder Controls. This is independent of Open Camera Controls keybind.",
                                    "Allows opening camera controls without dismounting from a vehicle - and keeping controls on sneak as well.",
                                    "Default: true")
                            .define("MiddleClickOpensControls", true);
                    builder.pop();
                }

                builder.pop();
            }

            {
                builder.push("Capture");
                FLASH_CAPTURE_DELAY_TICKS = builder
                        .comment("Delay in ticks before capturing an image when shooting with flash." +
                                "\nIf you experience flash synchronization issues (Flash having no effect on the image) - try increasing the value.")
                        .defineInRange("FlashCaptureDelayTicks", 3, 1, 6);
                builder.pop();
            }

            {
                builder.push("FileSaving");
                SAVE_EXPOSURE_TO_FILE_WHEN_VIEWED = builder
                        .comment("When the Photograph is viewed in UI, image will be saved to 'exposures' folder as a png.")
                        .define("SavePhotographs", true);
                EXPOSURE_SAVING_LEVEL_SUBFOLDER = builder
                        .comment("When saving, exposures will be placed into folder corresponding to current world name.")
                        .define("WorldNameSubfolder", true);
                EXPOSURE_SAVING_SIZE = builder
                        .comment("Saved exposures will be enlarged by this multiplier.",
                                "Given the default exposure size of 320 - this will produce:",
                                "320/640/960/1280px png image. Be careful with larger frame sizes.",
                                "Default: X2")
                        .defineEnum("Size", ExposureSize.X2);

                builder.pop();
            }

            SPEC = builder.build();
        }

        public static int getBackgroundColor() {
            return getColorFromHex(VIEWFINDER_BACKGROUND_COLOR.get());
        }

        public static int getMainFontColor() {
            return getColorFromHex(VIEWFINDER_FONT_MAIN_COLOR.get());
        }

        public static int getSecondaryFontColor() {
            return getColorFromHex(VIEWFINDER_FONT_SECONDARY_COLOR.get());
        }

        private static int getColorFromHex(String hexColor) {
            return new Color((int) Long.parseLong(hexColor.replace("#", ""), 16), true).getRGB();
        }
    }
}
