package io.github.mortuusars.exposure.item;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.entity.PhotographFrameEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HangingEntityItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.NotNull;

public class PhotographFrameItem extends HangingEntityItem {
    public PhotographFrameItem(Properties properties) {
        super(Exposure.EntityTypes.PHOTOGRAPH_FRAME.get(), properties);
    }

    //TODO: test build height return InteractionResult.CONSUME;

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        BlockPos clickedPos = context.getClickedPos();
        Direction direction = context.getClickedFace();
        BlockPos resultPos = clickedPos.relative(direction);
        Player player = context.getPlayer();
        ItemStack itemInHand = context.getItemInHand();
        if (player == null || player.level().isOutsideBuildHeight(resultPos) || !player.mayUseItemAt(resultPos, direction, itemInHand))
            return InteractionResult.FAIL;

        Level level = context.getLevel();
        PhotographFrameEntity photographEntity = new PhotographFrameEntity(level, resultPos, direction);

        CompoundTag compoundTag = itemInHand.getTag();
        if (compoundTag != null) {
            EntityType.updateCustomEntityTag(level, player, photographEntity, compoundTag);
        }


        for (int i = 2; i >= 0; i--) {
            photographEntity.setSize(i);
            if (photographEntity.survives()) {

                if (!level.isClientSide) {
                    photographEntity.playPlacementSound();
                    level.gameEvent(player, GameEvent.ENTITY_PLACE, photographEntity.position());
                    level.addFreshEntity(photographEntity);
                }

                if (!player.isCreative())
                    photographEntity.setFrameItem(itemInHand.split(1));
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        return InteractionResult.FAIL;
    }
}
