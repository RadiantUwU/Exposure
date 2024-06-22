package io.github.mortuusars.exposure.command.exposure;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.ExposureServer;
import io.github.mortuusars.exposure.command.argument.ExposureLookArgument;
import io.github.mortuusars.exposure.command.argument.ExposureSizeArgument;
import io.github.mortuusars.exposure.command.suggestion.ExposureIdSuggestionProvider;
import io.github.mortuusars.exposure.data.ExposureLook;
import io.github.mortuusars.exposure.data.ExposureSize;
import io.github.mortuusars.exposure.data.storage.ExposureExporter;
import io.github.mortuusars.exposure.data.storage.ExposureSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class ExportCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        return literal("export")
                .requires((stack) -> stack.hasPermission(3))
                .then(id())
                .then(all());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> id() {
        return literal("id")
                .then(argument("id", StringArgumentType.string())
                        .suggests(new ExposureIdSuggestionProvider())
                        .executes(context -> exportExposures(context.getSource(),
                                List.of(StringArgumentType.getString(context, "id")),
                                ExposureSize.X1,
                                ExposureLook.REGULAR))
                        .then(argument("size", new ExposureSizeArgument())
                                .executes(context -> exportExposures(context.getSource(),
                                        List.of(StringArgumentType.getString(context, "id")),
                                        ExposureSizeArgument.getSize(context, "size"),
                                        ExposureLook.REGULAR))
                                .then(argument("look", new ExposureLookArgument())
                                        .executes(context -> exportExposures(context.getSource(),
                                                List.of(StringArgumentType.getString(context, "id")),
                                                ExposureSizeArgument.getSize(context, "size"),
                                                ExposureLookArgument.getLook(context, "look"))))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> all() {
        return literal("all")
                .executes(context -> exportAll(context.getSource(), ExposureSize.X1, ExposureLook.REGULAR))
                .then(argument("size", new ExposureSizeArgument())
                        .executes(context -> exportAll(context.getSource(),
                                ExposureSizeArgument.getSize(context, "size"),
                                ExposureLook.REGULAR))
                        .then(argument("look", new ExposureLookArgument())
                                .executes(context -> exportAll(context.getSource(),
                                        ExposureSizeArgument.getSize(context, "size"),
                                        ExposureLookArgument.getLook(context, "look")))));
    }

    private static int exportAll(CommandSourceStack source, ExposureSize size, ExposureLook look) {
        List<String> ids = ExposureServer.getExposureStorage().getAllIds();
        return exportExposures(source, ids, size, look);
    }

    private static int exportExposures(CommandSourceStack stack, List<String> exposureIds, ExposureSize size, ExposureLook look) {
        int savedCount = 0;

        File folder = stack.getServer().getWorldPath(LevelResource.ROOT).resolve("exposures").toFile();
        boolean ignored = folder.mkdirs();

        for (String id : exposureIds) {
            Optional<ExposureSavedData> data = ExposureServer.getExposureStorage().getOrQuery(id);
            if (data.isEmpty()) {
                stack.sendFailure(Component.translatable("command.exposure.export.failure.not_found", id));
                continue;
            }

            ExposureSavedData exposureSavedData = data.get();
            String name = id + look.getIdSuffix();

            boolean saved = new ExposureExporter(name)
                    .withFolder(folder.getAbsolutePath().replace("\\.\\", "\\").replace("/./", "/"))
                    .withModifier(look.getModifier())
                    .withSize(size)
                    .save(exposureSavedData);

            if (saved)
                stack.sendSuccess(() ->
                        Component.translatable("command.exposure.export.success.saved_exposure_id", id), true);

            savedCount++;
        }

        if (savedCount > 0) {
            String folderPath = getFolderPath(folder);
            Component folderComponent = Component.literal(folderPath)
                    .withStyle(ChatFormatting.UNDERLINE)
                    .withStyle(arg -> arg.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, folderPath)));
            Component component = Component.translatable("command.exposure.export.success.result", savedCount, folderComponent);
            stack.sendSuccess(() -> component, true);
        } else
            stack.sendFailure(Component.translatable("command.exposure.export.failure.none_saved"));

        return 0;
    }

    @NotNull
    private static String getFolderPath(File folder) {
        String folderPath;
        try {
            folderPath = folder.getCanonicalPath();
        } catch (IOException e) {
            Exposure.LOGGER.error(e.toString());
            folderPath = folder.getAbsolutePath();
        }
        return folderPath;
    }
}
