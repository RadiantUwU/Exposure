package io.github.mortuusars.exposure.fabric;

import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import fuzs.forgeconfigapiport.api.config.v2.ModConfigEvents;
import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.command.ExposureCommand;
import io.github.mortuusars.exposure.command.ShaderCommand;
import io.github.mortuusars.exposure.command.TestCommand;
import io.github.mortuusars.exposure.data.Lenses;
import io.github.mortuusars.exposure.fabric.integration.create.CreateFilmDeveloping;
import io.github.mortuusars.exposure.fabric.resources.FabricLensesDataLoader;
import io.github.mortuusars.exposure.integration.ModCompatibilityClient;
import io.github.mortuusars.exposure.network.fabric.PacketsImpl;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.fabricmc.fabric.api.loot.v2.LootTableSource;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootDataManager;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootTableReference;
import net.minecraftforge.fml.config.ModConfig;

public class ExposureFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Exposure.init();

        ModConfigEvents.reloading(Exposure.ID).register(config -> {
            if (config.getType() == ModConfig.Type.COMMON && FabricLoader.getInstance().isModLoaded("create")) {
                CreateFilmDeveloping.clearCachedData();
            }

            if (config.getType() == ModConfig.Type.CLIENT) {
                ModCompatibilityClient.handle();
            }
        });

        ForgeConfigRegistry.INSTANCE.register(Exposure.ID, ModConfig.Type.COMMON, Config.Common.SPEC);
        ForgeConfigRegistry.INSTANCE.register(Exposure.ID, ModConfig.Type.CLIENT, Config.Client.SPEC);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ExposureCommand.register(dispatcher);
            ShaderCommand.register(dispatcher);
            TestCommand.register(dispatcher);
        });

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(content -> {
            content.accept(Exposure.Items.CAMERA.get());
            content.accept(Exposure.Items.BLACK_AND_WHITE_FILM.get());
            content.accept(Exposure.Items.COLOR_FILM.get());
            content.accept(Exposure.Items.DEVELOPED_BLACK_AND_WHITE_FILM.get());
            content.accept(Exposure.Items.DEVELOPED_COLOR_FILM.get());
            content.accept(Exposure.Items.PHOTOGRAPH.get());
            content.accept(Exposure.Items.AGED_PHOTOGRAPH.get());
            content.accept(Exposure.Items.INTERPLANAR_PROJECTOR.get());
            content.accept(Exposure.Items.STACKED_PHOTOGRAPHS.get());
            content.accept(Exposure.Items.PHOTOGRAPH_FRAME.get());
            content.accept(Exposure.Items.ALBUM.get());
        });

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(content -> {
            content.accept(Exposure.Items.LIGHTROOM.get());
        });

        Exposure.Advancements.register();
        Exposure.Stats.register();

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new FabricLensesDataLoader());

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            Exposure.initServer(server);
            PacketsImpl.onServerStarting(server);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(PacketsImpl::onServerStopped);

        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> Lenses.onDatapackSync(player));

        LootTableEvents.MODIFY.register(ExposureFabric::modifyLoot);

        PacketsImpl.registerC2SPackets();
    }

    private static void modifyLoot(ResourceManager resourceManager, LootDataManager manager,
                                   ResourceLocation table, LootTable.Builder builder, LootTableSource source) {
        if (!Config.Common.LOOT_ADDITION.get() || !source.isBuiltin())
            return;

        if (BuiltInLootTables.SIMPLE_DUNGEON.equals(table)) {
            builder.pool(LootPool.lootPool()
                    .add(LootTableReference.lootTableReference(Exposure.resource("chests/simple_dungeon")))
                    .build());
        }
        if (BuiltInLootTables.ABANDONED_MINESHAFT.equals(table)) {
            builder.pool(LootPool.lootPool()
                    .add(LootTableReference.lootTableReference(Exposure.resource("chests/abandoned_mineshaft")))
                    .build());
        }
        if (BuiltInLootTables.STRONGHOLD_CROSSING.equals(table)) {
            builder.pool(LootPool.lootPool()
                    .add(LootTableReference.lootTableReference(Exposure.resource("chests/stronghold")))
                    .build());
        }
        if (BuiltInLootTables.VILLAGE_PLAINS_HOUSE.equals(table)) {
            builder.pool(LootPool.lootPool()
                    .add(LootTableReference.lootTableReference(Exposure.resource("chests/village_plains_house")))
                    .build());
        }
        if (BuiltInLootTables.SHIPWRECK_MAP.equals(table)) {
            builder.pool(LootPool.lootPool()
                    .add(LootTableReference.lootTableReference(Exposure.resource("chests/shipwreck_map")))
                    .build());
        }
    }
}
