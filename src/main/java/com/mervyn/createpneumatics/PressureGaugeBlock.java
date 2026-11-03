package com.mervyn.createpneumatics;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

// A thin fitting stuck to one face of a tube; FACING points at the tube, whose face pressure it
// reads. The needle is a partial model drawn by GaugeRenderer; the block itself is just the dial.
public class PressureGaugeBlock extends TubeMountBlock implements IBE<PressureGaugeBlockEntity> {
    private static final MapCodec<PressureGaugeBlock> CODEC = simpleCodec(PressureGaugeBlock::new);

    public PressureGaugeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PressureGaugeBlockEntity gauge)
            return Mth.clamp(Math.round((Mth.clamp(gauge.pressure(), -1f, 1f) + 1f) / 2f * 15f), 0, 15);
        return 0;
    }

    @Override
    public Class<PressureGaugeBlockEntity> getBlockEntityClass() {
        return PressureGaugeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PressureGaugeBlockEntity> getBlockEntityType() {
        return Pneumatics.GAUGE_BE.get();
    }
}
