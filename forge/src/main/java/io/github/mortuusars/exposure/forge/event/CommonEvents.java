package io.github.mortuusars.exposure.forge.event;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.command.ExposureCommand;
import io.github.mortuusars.exposure.command.ShaderCommand;
import io.github.mortuusars.exposure.command.TestCommand;
import io.github.mortuusars.exposure.command.argument.ExposureLookArgument;
import io.github.mortuusars.exposure.command.argument.ExposureSizeArgument;
import io.github.mortuusars.exposure.command.argument.ShaderLocationArgument;
import io.github.mortuusars.exposure.command.argument.TextureLocationArgument;
import io.github.mortuusars.exposure.data.Lenses;
import io.github.mortuusars.exposure.data.LensesDataLoader;
import io.github.mortuusars.exposure.network.forge.PacketsImpl;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class CommonEvents {
    public static class ModBus {
        @SubscribeEvent
        public static void commonSetup(FMLCommonSetupEvent event) {
            event.enqueueWork(() -> {
                PacketsImpl.register();
                Exposure.Advancements.register();
                Exposure.Stats.register();
            });
        }
    }

    public static class ForgeBus {
        @SubscribeEvent
        public static void serverStarting(ServerStartingEvent event) {
            Exposure.initServer(event.getServer());
        }

        @SubscribeEvent
        public static void addReloadListeners(AddReloadListenerEvent event) {
            event.addListener(new LensesDataLoader());
        }

        @SubscribeEvent
        public static void onDatapackSync(OnDatapackSyncEvent event) {
            Lenses.onDatapackSync(event.getPlayerList(), event.getPlayer());
        }

        @SubscribeEvent
        public static void registerCommands(RegisterCommandsEvent event) {
            ArgumentTypeInfos.registerByClass(ShaderLocationArgument.class, SingletonArgumentInfo.contextFree(ShaderLocationArgument::new));
            ArgumentTypeInfos.registerByClass(TextureLocationArgument.class, SingletonArgumentInfo.contextFree(TextureLocationArgument::new));
            ArgumentTypeInfos.registerByClass(ExposureSizeArgument.class, SingletonArgumentInfo.contextFree(ExposureSizeArgument::new));
            ArgumentTypeInfos.registerByClass(ExposureLookArgument.class, SingletonArgumentInfo.contextFree(ExposureLookArgument::new));

            ExposureCommand.register(event.getDispatcher());
            ShaderCommand.register(event.getDispatcher());
            TestCommand.register(event.getDispatcher());
        }
    }
}
