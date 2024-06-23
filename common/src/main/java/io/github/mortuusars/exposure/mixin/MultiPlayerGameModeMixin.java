package io.github.mortuusars.exposure.mixin;

import io.github.mortuusars.exposure.camera.Camera;
import io.github.mortuusars.exposure.util.CameraInHand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Inject(method = "interactAt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/phys/EntityHitResult;getLocation()Lnet/minecraft/world/phys/Vec3;"),
            cancellable = true)
    void onInteractAt(Player player, Entity target, EntityHitResult ray, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (exposure$useCamera(player)) {
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }

    @Inject(method = "useItemOn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel;getWorldBorder()Lnet/minecraft/world/level/border/WorldBorder;"),
            cancellable = true)
    void onUseItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult result, CallbackInfoReturnable<InteractionResult> cir) {
        if (exposure$useCamera(player)) {
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }

    @Unique
    private static boolean exposure$useCamera(Player player) {
        MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
        if (gameMode == null) {
            return false;
        }

        Optional<Camera<?>> cameraOpt = Camera.getCamera(player);
        if (cameraOpt.isEmpty()) {
            return false;
        }

        Camera<?> camera = cameraOpt.get();
        if (camera instanceof CameraInHand<?> cameraInHand) {
            gameMode.useItem(player, cameraInHand.getHand());
            return true;
        }

        return false;
    }
}
