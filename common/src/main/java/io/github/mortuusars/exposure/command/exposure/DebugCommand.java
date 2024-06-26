package io.github.mortuusars.exposure.command.exposure;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.camera.capture.CapturedFramesHistory;
import io.github.mortuusars.exposure.camera.infrastructure.FrameData;
import io.github.mortuusars.exposure.item.ChromaticSheetItem;
import io.github.mortuusars.exposure.network.Packets;
import io.github.mortuusars.exposure.network.packet.client.ClearRenderingCacheS2CP;
import io.github.mortuusars.exposure.network.packet.client.OnFrameAddedS2CP;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class DebugCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        return Commands.literal("debug")
                .then(Commands.literal("clearRenderingCache")
                        .executes(DebugCommand::clearRenderingCache))
                .then(Commands.literal("chromaticFromLastThreeExposures")
                        .executes(DebugCommand::chromaticFromLastThreeExposures));
    }

    private static int clearRenderingCache(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack stack = context.getSource();
        ServerPlayer player = stack.getPlayerOrException();
        Packets.sendToClient(new ClearRenderingCacheS2CP(), player);
        return 0;
    }

    private static int chromaticFromLastThreeExposures(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack stack = context.getSource();
        ServerPlayer player = stack.getPlayerOrException();

        List<CompoundTag> frames = CapturedFramesHistory.get();
        if (frames.size() < 3) {
            stack.sendFailure(Component.literal("Not enough frames were captured in this session. 3 is required."));
            return 1;
        }

        try {
            ChromaticSheetItem item = Exposure.Items.CHROMATIC_SHEET.get();
            ItemStack itemStack = new ItemStack(item);

            item.addExposure(itemStack, frames.get(2)); // Red
            item.addExposure(itemStack, frames.get(1)); // Green
            item.addExposure(itemStack, frames.get(0)); // Blue

            ItemStack photographStack = item.finalize(player.level(), itemStack, player.getScoreboardName());

            CompoundTag frame = photographStack.getTag();
            Preconditions.checkState(frame != null);

            Packets.sendToClient(new OnFrameAddedS2CP(frame), player); // Adds frame to client CapturedFramesHistory

            stack.sendSuccess(() -> Component.literal("Created chromatic exposure: " + frame.getString(FrameData.ID)), true);
        } catch (Exception e) {
            stack.sendFailure(Component.literal("Failed to create chromatic exposure: " + e));
            return 1;
        }

        return 0;
    }
}
