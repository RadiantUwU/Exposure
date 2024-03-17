package io.github.mortuusars.exposure.command.exposure;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import io.github.mortuusars.exposure.ExposureServer;
import io.github.mortuusars.exposure.camera.capture.processing.FloydDither;
import io.github.mortuusars.exposure.camera.infrastructure.FilmType;
import io.github.mortuusars.exposure.data.storage.ExposureSavedData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.material.MapColor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Optional;

public class TrichromeCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        return Commands.literal("trichrome")
                .then(Commands.argument("red", StringArgumentType.word())
                        .then(Commands.argument("green", StringArgumentType.word())
                                .then(Commands.argument("blue", StringArgumentType.word())
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(TrichromeCommand::createTrichrome)))));
    }

    private static int createTrichrome(CommandContext<CommandSourceStack> context) {
        CommandSourceStack stack = context.getSource();

        String redID = StringArgumentType.getString(context, "red");
        String greenID = StringArgumentType.getString(context, "green");
        String blueID = StringArgumentType.getString(context, "blue");

        Optional<ExposureSavedData> redOpt = ExposureServer.getExposureStorage().getOrQuery(redID);
        if (redOpt.isEmpty()) {
            context.getSource().sendFailure(Component.literal("Exposure [%s] for the red channel is not found.".formatted(redID)));
            return 0;
        }

        Optional<ExposureSavedData> greenOpt = ExposureServer.getExposureStorage().getOrQuery(greenID);
        if (greenOpt.isEmpty()) {
            context.getSource().sendFailure(Component.literal("Exposure [%s] for the green channel is not found.".formatted(greenID)));
            return 0;
        }

        Optional<ExposureSavedData> blueOpt = ExposureServer.getExposureStorage().getOrQuery(blueID);
        if (blueOpt.isEmpty()) {
            context.getSource().sendFailure(Component.literal("Exposure [%s] for the blue channel is not found.".formatted(blueID)));
            return 0;
        }

        ExposureSavedData red = redOpt.get();
        ExposureSavedData green = greenOpt.get();
        ExposureSavedData blue = blueOpt.get();

        int width = Math.min(red.getWidth(), Math.min(green.getWidth(), blue.getWidth()));
        int height = Math.min(red.getHeight(), Math.min(green.getHeight(), blue.getHeight()));

        if (width <= 0) {
            context.getSource().sendFailure(Component.literal("Width should be larger than 0"));
            return 0;
        }

        if (height <= 0) {
            context.getSource().sendFailure(Component.literal("Height should be larger than 0"));
            return 0;
        }

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int b = MapColor.getColorFromPackedId(red.getPixel(x, y)) >> 16 & 0xFF;
                int g = MapColor.getColorFromPackedId(green.getPixel(x, y)) >> 8 & 0xFF;
                int r = MapColor.getColorFromPackedId(blue.getPixel(x, y)) & 0xFF;

                int rgb = 0xFF << 24 | r << 16 | g << 8 | b;

                image.setRGB(x, y, rgb);
            }
        }

        String id = StringArgumentType.getString(context, "id");


        File file = new File("C:\\Users\\mortuus\\Desktop\\" + id + ".png");
        try {
            ImageIO.write(image, "png", file);
        }
        catch (Exception e) {
            LogUtils.getLogger().error(e.toString());
        }


        byte[] mapColorPixels = FloydDither.ditherWithMapColors(image);

        CompoundTag properties = new CompoundTag();
        properties.putString(ExposureSavedData.TYPE_PROPERTY, FilmType.COLOR.getSerializedName());

        ExposureSavedData resultData = new ExposureSavedData(image.getWidth(), image.getHeight(), mapColorPixels, properties);


        ExposureServer.getExposureStorage().put(id, resultData);

        MutableComponent idComponent = Component.literal(id)
                .withStyle(Style.EMPTY
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, id)));

        context.getSource().sendSuccess(() -> Component.literal("Trichrome image [%s] created.".formatted(idComponent)), true);

        return 0;
    }
}
