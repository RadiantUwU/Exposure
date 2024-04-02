package io.github.mortuusars.exposure.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.mortuusars.exposure.data.ExposureLook;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class ExposureLookArgument implements ArgumentType<ExposureLook> {
    @Override
    public ExposureLook parse(StringReader reader) throws CommandSyntaxException {
        String string = reader.readString();
        @Nullable ExposureLook look = ExposureLook.byName(string);
        if (look == null)
            throw new SimpleCommandExceptionType(Component.translatable("argument.enum.invalid", string)).create();

        return look;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(Arrays.stream(ExposureLook.values())
                .filter(l -> l != ExposureLook.REGULAR)
                .map(ExposureLook::getSerializedName), builder);
    }

    public static ExposureLook getLook(final CommandContext<?> context, final String name) {
        return context.getArgument(name, ExposureLook.class);
    }
}
