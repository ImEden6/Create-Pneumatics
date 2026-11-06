package com.mervyn.createpneumatics;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.logistics.box.PackageEntity;
import com.simibubi.create.content.logistics.box.PackageItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

// A stack riding the tubes. It always moves to the connected tube with the lowest pressure, never
// straight back, so it runs away from a blower that pushes and toward one that pulls. Where the
// slope runs out it drops into a terminal, flies out an open end, or waits.
public class TubePayloadEntity extends Entity {
    private static final EntityDataAccessor<ItemStack> ITEM = SynchedEntityData.defineId(TubePayloadEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final int GIVE_UP = 200;

    private BlockPos tube = BlockPos.ZERO;
    @Nullable
    private BlockPos from;
    private int stalled;
    private double lerpX, lerpY, lerpZ;
    private boolean lerping;

    public TubePayloadEntity(EntityType<? extends TubePayloadEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public static TubePayloadEntity launch(Level level, ItemStack stack, BlockPos tube, BlockPos from) {
        TubePayloadEntity payload = new TubePayloadEntity(Pneumatics.PAYLOAD.get(), level);
        payload.setItem(stack);
        payload.tube = tube;
        payload.from = from;
        payload.setPos(Vec3.atCenterOf(tube));
        level.addFreshEntity(payload);
        return payload;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ITEM, ItemStack.EMPTY);
    }

    public ItemStack getItem() {
        return entityData.get(ITEM);
    }

    public void setItem(ItemStack stack) {
        entityData.set(ITEM, stack);
    }

    // the server moves a payload a whole tube per tick; landing on each new position one tick later
    // lets the renderer slide between centres instead of jumping, so it stays inside a glass tube
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerping = true;
    }

    @Override
    public double lerpTargetX() {
        return lerping ? lerpX : getX();
    }

    @Override
    public double lerpTargetY() {
        return lerping ? lerpY : getY();
    }

    @Override
    public double lerpTargetZ() {
        return lerping ? lerpZ : getZ();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (lerping) {
                setPos(lerpX, lerpY, lerpZ);
                lerping = false;
            }
            return;
        }
        TubeTransportBehaviour here = TubeTransportBehaviour.get(level(), tube);
        if (here == null || getItem().isEmpty()) {
            spill(Vec3.atCenterOf(tube), Vec3.ZERO);
            return;
        }
        int steps = 1 + Math.round(Math.min(1f, Math.abs(here.pressure())) * (Config.PAYLOAD_SPEED.get() - 1));
        for (int i = 0; i < steps && isAlive(); i++)
            if (!step())
                break;
        setPos(Vec3.atCenterOf(tube));
    }

    // one face along the slope; false when it couldn't move this tick
    private boolean step() {
        Level level = level();
        BlockState state = level.getBlockState(tube);
        TubeTransportBehaviour here = TubeTransportBehaviour.get(level, tube);
        if (here == null)
            return false;
        float pressure = here.pressure();
        @Nullable Direction forced = null, barred = null;
        if (level.getBlockEntity(tube) instanceof AirDiverterBlockEntity diverter) {
            Direction exit = state.getValue(AirDiverterBlock.FACING);
            if (diverter.sends(getItem()))
                forced = exit;
            else
                barred = exit;
        }

        Direction best = null;
        float lowest = pressure;
        for (Direction side : Direction.values()) {
            if (!PneumaticTubeBlock.isOpenAt(state, side) || side == barred || tube.relative(side).equals(from))
                continue;
            BlockPos next = tube.relative(side);
            if (!level.isLoaded(next))
                continue;
            TubeTransportBehaviour there = TubeTransportBehaviour.get(level, next);
            if (there == null)
                continue;
            if (side == forced) {
                best = side;
                break;
            }
            if (there.pressure() < lowest - 0.001f) {
                lowest = there.pressure();
                best = side;
            }
        }

        if (best != null) {
            BlockPos next = tube.relative(best);
            if (level.getBlockEntity(next) instanceof AirDiverterBlockEntity diverter && !diverter.reserve(getUUID(), level.getGameTime()))
                return stall();
            if (level.getBlockEntity(tube) instanceof AirDiverterBlockEntity diverter)
                diverter.release(getUUID());
            from = tube;
            tube = next;
            stalled = 0;
            return true;
        }

        for (Direction side : Direction.values()) {
            if (!PneumaticTubeBlock.isOpenAt(state, side) || tube.relative(side).equals(from) && stalled < GIVE_UP / 4)
                continue;
            if (level.getBlockEntity(tube.relative(side)) instanceof PneumaticTerminalBlockEntity terminal) {
                terminal.receive(getItem());
                discard();
                return false;
            }
        }
        for (Direction side : Direction.values())
            if (pressure > 0 && PneumaticTubeBlock.isOpenEnd(level, tube, state, side)) {
                Vec3 out = Vec3.atCenterOf(tube).add(Vec3.atLowerCornerOf(side.getNormal()).scale(0.6));
                spill(out, Vec3.atLowerCornerOf(side.getNormal()).scale(0.2 + pressure * 0.3));
                return false;
            }
        return stall();
    }

    private boolean stall() {
        if (++stalled > GIVE_UP)
            spill(Vec3.atCenterOf(tube), Vec3.ZERO);
        return false;
    }

    private void spill(Vec3 at, Vec3 motion) {
        if (level().getBlockEntity(tube) instanceof AirDiverterBlockEntity diverter)
            diverter.release(getUUID());
        eject(level(), at, motion, getItem());
        discard();
    }

    static void eject(Level level, Vec3 at, Vec3 motion, ItemStack stack) {
        if (stack.isEmpty())
            return;
        Entity entity = PackageItem.isPackage(stack) ? PackageEntity.fromItemStack(level, at, stack) : new ItemEntity(level, at.x, at.y, at.z, stack);
        entity.setDeltaMovement(motion);
        level.addFreshEntity(entity);
        level.playSound(null, BlockPos.containing(at), Pneumatics.PAYLOAD_POP.get(), SoundSource.BLOCKS, 0.8f, 1);
        if (level instanceof ServerLevel server)
            server.sendParticles(ParticleTypes.POOF, at.x, at.y, at.z, 4, 0.1, 0.1, 0.1, 0.02);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setItem(ItemStack.parseOptional(registryAccess(), tag.getCompound("Item")));
        tube = NbtUtils.readBlockPos(tag, "Tube").orElse(blockPosition());
        from = NbtUtils.readBlockPos(tag, "From").orElse(null);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("Item", getItem().saveOptional(registryAccess()));
        tag.put("Tube", NbtUtils.writeBlockPos(tube));
        if (from != null)
            tag.put("From", NbtUtils.writeBlockPos(from));
    }
}
