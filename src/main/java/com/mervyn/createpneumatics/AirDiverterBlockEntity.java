package com.mervyn.createpneumatics;

import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

// One payload through the junction at a time: whoever reserves it first goes, the rest wait a face
// back. A reservation that's never released (payload unloaded mid-junction) runs out on its own.
public class AirDiverterBlockEntity extends PneumaticTubeBlockEntity {
    FilteringBehaviour filter;
    @Nullable
    private UUID holder;
    private long since;

    public AirDiverterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        filter = new FilteringBehaviour(this, new Slot());
        behaviours.add(filter);
    }

    boolean sends(ItemStack stack) {
        return !getBlockState().getValue(AirDiverterBlock.POWERED) && filter.test(stack);
    }

    boolean reserve(UUID payload, long now) {
        if (holder != null && !holder.equals(payload) && now - since < Config.DIVERTER_TIMEOUT.get())
            return false;
        holder = payload;
        since = now;
        return true;
    }

    void release(UUID payload) {
        if (payload.equals(holder))
            holder = null;
    }

    // the filter sits on the face opposite the branch, or on top when a tube takes that face
    private static class Slot extends ValueBoxTransform.Sided {
        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 13.05);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction side) {
            return side == face(state);
        }

        private static Direction face(BlockState state) {
            Direction back = state.getValue(AirDiverterBlock.FACING).getOpposite();
            if (!PneumaticTubeBlock.isOpenAt(state, back))
                return back;
            return PneumaticTubeBlock.isOpenAt(state, Direction.UP) ? Direction.DOWN : Direction.UP;
        }
    }
}
