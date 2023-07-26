package io.github.mortuusars.exposure.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.*;

@OnlyIn(Dist.CLIENT)
public class OnePerPlayerSoundsClient {
    private static final Map<Player, List<SoundInstance>> instances = new HashMap<>();

    public static void play(Player sourcePlayer, SoundEvent soundEvent, SoundSource source, float volume, float pitch) {
        Level level = sourcePlayer.getLevel();
        stop(sourcePlayer, soundEvent);

        EntityBoundSoundInstance soundInstance = new EntityBoundSoundInstance(soundEvent, source, volume, pitch,
                sourcePlayer, level.getRandom().nextLong());

        List<SoundInstance> playingSounds = Optional.ofNullable(instances.get(sourcePlayer)).orElse(new ArrayList<>());
        playingSounds.add(soundInstance);
        instances.put(sourcePlayer, playingSounds);

        Minecraft.getInstance().getSoundManager().play(soundInstance);
    }

    public static void stop(Player sourcePlayer, SoundEvent soundEvent) {
        if (instances.containsKey(sourcePlayer)) {
            ResourceLocation soundLocation = soundEvent.getLocation();
            List<SoundInstance> playingSounds = instances.remove(sourcePlayer);
            for (int i = playingSounds.size() - 1; i >= 0; i--) {
                SoundInstance soundInstance = playingSounds.get(i);
                if (soundInstance.getLocation().equals(soundLocation)) {
                    Minecraft.getInstance().getSoundManager().stop(soundInstance);
                    playingSounds.remove(i);
                }
            }

            instances.put(sourcePlayer, playingSounds);
        }
    }
}
