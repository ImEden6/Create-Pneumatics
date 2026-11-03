package com.mervyn.createpneumatics;

import java.util.List;

import static com.simibubi.create.content.kinetics.base.HorizontalKineticBlock.HORIZONTAL_FACING;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class RootsBlowerBlockEntity extends KineticBlockEntity {

    public RootsBlowerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    public float supply(Direction flange) {
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof RootsBlowerBlock blower) || !blower.isFlange(state, flange))
            return 0f;
        float speed = getSpeed();
        if (speed == 0f)
            return 0f;
        float sign = Math.signum(convertToDirection(speed, state.getValue(HORIZONTAL_FACING)));
        float magnitude = Math.min(Math.abs(speed) / PneumaticTubeBlockEntity.RPM_SCALE, 1f);
        boolean front = state.getValue(HORIZONTAL_FACING) == flange;
        return (front ? sign : -sign) * magnitude;
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        if (level == null || level.isClientSide)
            return;
        if (previousSpeed != getSpeed())
            PneumaticPropagator.changed(level, worldPosition);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // Pressure is never persisted; a freshly loaded network gets one rebuild.
        if (level != null)
            PneumaticPropagator.queue(level, worldPosition);
    }
}
