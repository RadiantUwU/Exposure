package io.github.mortuusars.exposure.entity;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.PlatformHelper;
import io.github.mortuusars.exposure.item.PhotographItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class PhotographFrameEntity extends HangingEntity {
    public static final Logger LOGGER = LogUtils.getLogger();

    protected static final EntityDataAccessor<Integer> DATA_SIZE = SynchedEntityData.defineId(PhotographFrameEntity.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<ItemStack> DATA_FRAME_ITEM = SynchedEntityData.defineId(PhotographFrameEntity.class, EntityDataSerializers.ITEM_STACK);
    protected static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(PhotographFrameEntity.class, EntityDataSerializers.ITEM_STACK);
    protected static final EntityDataAccessor<Boolean> DATA_GLOWING = SynchedEntityData.defineId(PhotographFrameEntity.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Integer> DATA_ITEM_ROTATION = SynchedEntityData.defineId(PhotographFrameEntity.class, EntityDataSerializers.INT);

    protected int size = 0;

    public PhotographFrameEntity(EntityType<? extends PhotographFrameEntity> entityType, Level level) {
        super(entityType, level);
    }

    public PhotographFrameEntity(Level level, BlockPos pos, Direction facingDirection) {
        super(Exposure.EntityTypes.PHOTOGRAPH_FRAME.get(), level, pos);
        setDirection(facingDirection);
        setItem(ItemStack.EMPTY);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double d = Config.Client.PHOTOGRAPH_FRAME_CULLING_DISTANCE.get() * getViewScale();
        return distance < d * d;
    }

    protected void defineSynchedData() {
        this.getEntityData().define(DATA_SIZE, 0);
        this.getEntityData().define(DATA_FRAME_ITEM, ItemStack.EMPTY);
        this.getEntityData().define(DATA_ITEM, ItemStack.EMPTY);
        this.getEntityData().define(DATA_GLOWING, false);
        this.getEntityData().define(DATA_ITEM_ROTATION, 0);
    }

    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (key.equals(DATA_ITEM)) {
            onItemChanged(getItem());
        }
        if (key.equals(DATA_SIZE)) {
            size = getEntityData().get(DATA_SIZE);
            recalculateBoundingBox();
        }
    }

    @Override
    public void recreateFromPacket(@NotNull ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        int packedData = packet.getData();
        int size = (packedData >> 8) & 0xFF;
        int direction = packedData & 0xFF;
        setSize(size);
        setDirection(Direction.from3DDataValue(direction));
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        int packedData = (size << 8) | direction.get3DDataValue();
        return new ClientboundAddEntityPacket(this, packedData, this.getPos());
    }

    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ItemStack item = getItem();
        if (!item.isEmpty()) {
            tag.put("Item", item.save(new CompoundTag()));
            tag.putBoolean("Glowing", this.isGlowing());
            tag.putByte("ItemRotation", (byte) this.getRotation());
        }
        ItemStack frameItem = getFrameItem();
        if (!frameItem.isEmpty())
            tag.put("FrameItem", frameItem.save(new CompoundTag()));

        tag.putByte("Size", (byte) getSize());
        tag.putByte("Facing", (byte) direction.get3DDataValue());
        tag.putBoolean("Invisible", isInvisible());
    }

    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        CompoundTag frameItemTag = tag.getCompound("FrameItem");
        if (!frameItemTag.isEmpty()) {
            ItemStack itemstack = ItemStack.of(frameItemTag);
            if (itemstack.isEmpty()) {
                LOGGER.warn("Unable to load frame item from: {}", frameItemTag);
                itemstack = new ItemStack(Exposure.Items.PHOTOGRAPH_FRAME.get());
            }

            setFrameItem(itemstack);
        }

        CompoundTag itemTag = tag.getCompound("Item");
        if (!itemTag.isEmpty()) {
            ItemStack itemstack = ItemStack.of(itemTag);
            if (itemstack.isEmpty())
                LOGGER.warn("Unable to load item from: {}", itemTag);

            setItem(itemstack);
            setGlowing(tag.getBoolean("Glowing"));
            setRotation(tag.getByte("ItemRotation"));
        }

        setSize(tag.getByte("Size"));
        setDirection(Direction.from3DDataValue(tag.getByte("Facing")));
        setInvisible(tag.getBoolean("Invisible"));
    }

    @Override
    public @NotNull Vec3 trackingPosition() {
        return Vec3.atLowerCornerOf(this.pos);
    }

    @Override
    protected float getEyeHeight(@NotNull Pose pose, @NotNull EntityDimensions dimensions) {
        return 0f;
    }

    @Override
    public int getWidth() {
        return getSize() * 16 + 16;
    }

    @Override
    public int getHeight() {
        return getSize() * 16 + 16;
    }

    @Nullable
    @Override
    public ItemStack getPickResult() {
        ItemStack item = getItem();
        if (!item.isEmpty())
            return item.copy();

        return getFrameItem().copy();
    }

    @Override
    protected void recalculateBoundingBox() {
        //noinspection ConstantValue
        if (this.direction == null)
            return;

        double x = (double)this.pos.getX() + 0.5;
        double y = (double)this.pos.getY() + 0.5;
        double z = (double)this.pos.getZ() + 0.5;

        double widthOffset = getWidth() % 32 == 0 ? 0.5 : 0.0;
        double heightOffset = getHeight() % 32 == 0 ? 0.5 : 0.0;
        if (getSize() == 2) {
            widthOffset += 1;
            heightOffset += 1;
        }

        double hangOffset = 0.46875;

        if (getDirection().getAxis().isHorizontal()) {
            x -= getDirection().getStepX() * hangOffset;
            z -= getDirection().getStepZ() * hangOffset;
            Direction direction = getDirection().getCounterClockWise();
            setPosRaw(x += widthOffset * (double)direction.getStepX(), y += heightOffset, z += widthOffset * (double)direction.getStepZ());
            double xSize = this.getWidth();
            double ySize = this.getHeight();
            double zSize = this.getWidth();
            if (getDirection().getAxis() == Direction.Axis.Z)
                zSize = 1.0;
            else
                xSize = 1.0;
            setBoundingBox(new AABB(x - (xSize /= 32.0), y - (ySize /= 32.0), z - (zSize /= 32.0), x + xSize, y + ySize, z + zSize));
        }
        else {
            y -= getDirection().getStepY() * hangOffset;
            setPosRaw(x += widthOffset, y, z -= heightOffset);
            double xSize = getWidth();
            double zSize = getHeight();
            setBoundingBox(new AABB(x - (xSize /= 32.0), y - (1.0 / 32.0), z - (zSize /= 32.0), x + xSize, y + 1.0 / 32.0, z + zSize));
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean survives() {
        if (!level().noCollision(this))
            return false;

        int sizeX = Math.max(1, getWidth() / 16);
        int sizeY = Math.max(1, getHeight() / 16);
        BlockPos baseBlockPos = pos.relative(direction.getOpposite());

        if (getDirection().getAxis().isHorizontal()) {
            Direction direction = getDirection().getCounterClockWise();
            BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
            for (int pX = 0; pX < sizeX; ++pX) {
                for (int pY = 0; pY < sizeY; ++pY) {
                    mPos.set(baseBlockPos).move(direction, pX).move(Direction.UP, pY);
                    BlockState blockState = level().getBlockState(mPos);
                    if (blockState.isSolid() || DiodeBlock.isDiode(blockState)) continue;
                    return false;
                }
            }
        } else {
            BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
            for (int pX = 0; pX < sizeX; ++pX) {
                for (int pY = 0; pY < sizeY; ++pY) {
                    mPos.set(baseBlockPos).move(Direction.NORTH, pX).move(Direction.EAST, pY);
                    BlockState blockState = level().getBlockState(mPos);
                    if (blockState.isSolid() || DiodeBlock.isDiode(blockState)) continue;
                    return false;
                }
            }
        }

        return level().getEntities(this, getBoundingBox(), HANGING_ENTITY).isEmpty();
    }

    @Override
    protected void setDirection(@NotNull Direction facingDirection) {
        Validate.notNull(facingDirection);

        direction = facingDirection;
        if (facingDirection.getAxis().isHorizontal()) {
            setXRot(0.0f);
            setYRot(direction.get2DDataValue() * 90);
        } else {
            setXRot(-90 * facingDirection.getAxisDirection().getStep());
            setYRot(0.0f);
        }
        xRotO = getXRot();
        yRotO = getYRot();
        recalculateBoundingBox();
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        getEntityData().set(DATA_SIZE, Mth.clamp(size, 0, 2));
        this.size = size;
        recalculateBoundingBox();
    }

    public ItemStack getFrameItem() {
        return getEntityData().get(DATA_FRAME_ITEM);
    }

    public void setFrameItem(ItemStack stack) {
        getEntityData().set(DATA_FRAME_ITEM, stack);
    }

    public ItemStack getItem() {
        return getEntityData().get(DATA_ITEM);
    }

    public void setItem(ItemStack stack) {
        getEntityData().set(DATA_ITEM, stack);
    }

    protected void onItemChanged(ItemStack itemStack) {
        if (!itemStack.isEmpty()) {
            itemStack.setEntityRepresentation(this);
        }
    }

    public boolean isGlowing() {
        return getEntityData().get(DATA_GLOWING);
    }

    public void setGlowing(boolean glowing) {
        getEntityData().set(DATA_GLOWING, glowing);
    }

    public int getRotation() {
        return getEntityData().get(DATA_ITEM_ROTATION);
    }

    public void setRotation(int rotation) {
        getEntityData().set(DATA_ITEM_ROTATION, rotation % 4);
    }

    @Override
    public @NotNull InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack itemInHand = player.getItemInHand(hand);
        if (!isInvisible() && canShear(itemInHand)) {
            if (!level().isClientSide) {
                setInvisible(true);
                itemInHand.hurtAndBreak(1, player, (pl) -> pl.broadcastBreakEvent(hand));
                gameEvent(GameEvent.BLOCK_CHANGE, player);
                playSound(SoundEvents.SHEEP_SHEAR, 1f, level().getRandom().nextFloat() * 0.2f + 0.9f);
            }
            return InteractionResult.SUCCESS;
        }

        if (itemInHand.getItem() instanceof PhotographItem && getItem().isEmpty()) {
            setItem(itemInHand.copy());
            itemInHand.shrink(1);
            gameEvent(GameEvent.BLOCK_CHANGE, player);
            return InteractionResult.SUCCESS;
        }

        if (itemInHand.is(Items.GLOW_INK_SAC)) {
            setGlowing(true);
            itemInHand.shrink(1);
            if (!level().isClientSide) {
                playSound(SoundEvents.GLOW_INK_SAC_USE);
                gameEvent(GameEvent.BLOCK_CHANGE, player);
            }
            return InteractionResult.SUCCESS;
        }

        if (!level().isClientSide) {
            this.playSound(getRotateSound(), 1.0F, level().getRandom().nextFloat() * 0.2f + 0.9f);
            this.setRotation(getRotation() + 1);
            gameEvent(GameEvent.BLOCK_CHANGE, player);
        }

        return InteractionResult.SUCCESS;
    }

    public boolean canShear(ItemStack stack) {
        return PlatformHelper.canShear(stack);
    }

    @Override
    public boolean hurt(@NotNull DamageSource damageSource, float amount) {
        if (isInvulnerableTo(damageSource))
            return false;

        if (!damageSource.is(DamageTypeTags.IS_EXPLOSION) && !getItem().isEmpty()) {
            if (!level().isClientSide) {
                dropItem(damageSource.getEntity(), false);
                gameEvent(GameEvent.BLOCK_CHANGE, damageSource.getEntity());
                playSound(getRemoveItemSound(), 1.0f, 1.0f);
            }
            return true;
        }

        return super.hurt(damageSource, amount);
    }

    @Override
    public void dropItem(@Nullable Entity brokenEntity) {
        playSound(getBreakSound(), 1.0f, 1.0f);
        dropItem(brokenEntity, true);
        gameEvent(GameEvent.BLOCK_CHANGE, brokenEntity);
    }

    protected void dropItem(@Nullable Entity entity, boolean dropSelf) {
        ItemStack itemStack = getItem();
        setItem(ItemStack.EMPTY);
        if (!level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS))
            return;

        if (entity instanceof Player player && player.isCreative())
            return;

        if (dropSelf)
            spawnAtLocation(getFrameItem());

        if (!itemStack.isEmpty())
            spawnAtLocation(itemStack.copy());
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && isGlowing() && level().getRandom().nextFloat() < 0.01f) {
            AABB bb = getBoundingBox();
            Vec3i normal = getDirection().getNormal();
            level().addParticle(ParticleTypes.END_ROD,
                    position().x + (level().getRandom().nextFloat() * (bb.getXsize() * 0.75f) - bb.getXsize() * 0.75f / 2),
                    position().y + (level().getRandom().nextFloat() * (bb.getYsize() * 0.75f) - bb.getYsize() * 0.75f / 2),
                    position().z + (level().getRandom().nextFloat() * (bb.getZsize() * 0.75f) - bb.getZsize() * 0.75f / 2),
                    level().getRandom().nextFloat() * 0.02f * normal.getX(),
                    level().getRandom().nextFloat() * 0.02f * normal.getY(),
                    level().getRandom().nextFloat() * 0.02f * normal.getZ());
        }
    }

    @Override
    public @NotNull SlotAccess getSlot(int slot) {
        if (slot == 0) {
            return new SlotAccess(){

                @Override
                public @NotNull ItemStack get() {
                    return PhotographFrameEntity.this.getItem();
                }

                @Override
                public boolean set(ItemStack carried) {
                    PhotographFrameEntity.this.setItem(carried);
                    return true;
                }
            };
        }
        return super.getSlot(slot);
    }

    @Override
    public float getNameTagOffsetY() {
        return (getSize() + 1) / 2f + 0.35f;
    }

    @Override
    public void playPlacementSound() {
        this.playSound(this.getPlaceSound(), 1.0F, level().getRandom().nextFloat() * 0.2f + 0.7f);
    }

    public SoundEvent getPlaceSound() {
        return SoundEvents.ITEM_FRAME_PLACE;
    }

    public SoundEvent getBreakSound() {
        return SoundEvents.ITEM_FRAME_BREAK;
    }

    public SoundEvent getRotateSound() {
        return Exposure.SoundEvents.PHOTOGRAPH_RUSTLE.get();
    }

    public SoundEvent getRemoveItemSound() {
        return SoundEvents.ITEM_FRAME_REMOVE_ITEM;
    }
}
