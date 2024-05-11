package io.github.mortuusars.exposure.integration.jade;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.block.LightroomBlock;
import io.github.mortuusars.exposure.block.entity.LightroomBlockEntity;
import io.github.mortuusars.exposure.integration.jade.component_provider.LightroomComponentProvider;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin(Exposure.ID)
public class ExposureJadePlugin implements IWailaPlugin {
    public static final ResourceLocation LIGHTROOM = Exposure.resource("lightroom");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(LightroomComponentProvider.INSTANCE, LightroomBlockEntity.class);
        registration.registerItemStorage(LightroomComponentProvider.INSTANCE, LightroomBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(LightroomComponentProvider.INSTANCE, LightroomBlock.class);
    }
}
