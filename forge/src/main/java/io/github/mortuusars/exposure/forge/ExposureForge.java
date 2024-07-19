package io.github.mortuusars.exposure.forge;

import com.mojang.serialization.Codec;
import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.forge.event.ClientEvents;
import io.github.mortuusars.exposure.forge.event.CommonEvents;
import io.github.mortuusars.exposure.forge.integration.create.CreateFilmDeveloping;
import io.github.mortuusars.exposure.forge.loot.LootTableAdditionModifier;
import io.github.mortuusars.exposure.integration.ModCompatibilityClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(Exposure.ID)
public class ExposureForge {
    public ExposureForge() {
        Exposure.init();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.Common.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.Client.SPEC);

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::onConfigReloading);

        RegisterImpl.BLOCKS.register(modEventBus);
        RegisterImpl.BLOCK_ENTITY_TYPES.register(modEventBus);
        RegisterImpl.ENTITY_TYPES.register(modEventBus);
        RegisterImpl.ITEMS.register(modEventBus);
        RegisterImpl.MENU_TYPES.register(modEventBus);
        RegisterImpl.RECIPE_SERIALIZERS.register(modEventBus);
        RegisterImpl.SOUND_EVENTS.register(modEventBus);
        RegisterImpl.COMMAND_ARGUMENT_TYPES.register(modEventBus);

        LootModifiers.LOOT_MODIFIERS.register(modEventBus);

        modEventBus.register(CommonEvents.ModBus.class);
        MinecraftForge.EVENT_BUS.register(CommonEvents.ForgeBus.class);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            modEventBus.register(ClientEvents.ModBus.class);
            MinecraftForge.EVENT_BUS.register(ClientEvents.ForgeBus.class);
        });
    }

    public static class LootModifiers {
        private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
                DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Exposure.ID);
        public static final RegistryObject<Codec<LootTableAdditionModifier>> LOOT_TABLE_ADDITION =
                LOOT_MODIFIERS.register("loot_table_addition", LootTableAdditionModifier.CODEC);
    }

    private void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getType() == ModConfig.Type.COMMON && ModList.get().isLoaded("create")) {
            CreateFilmDeveloping.clearCachedData();
        }

        if (event.getConfig().getType() == ModConfig.Type.CLIENT) {
            ModCompatibilityClient.handle();
        }
    }
}
