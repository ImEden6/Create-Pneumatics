package com.mervyn.createpneumatics;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

// open dumps the whole network. redstone holds it open, a click flips it
public class ReliefValveBlock extends TubeMountBlock {
    private static final MapCodec<ReliefValveBlock> CODEC = simpleCodec(ReliefValveBlock::new);
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public ReliefValveBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(OPEN, false).setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OPEN, POWERED);
    }

    public boolean isOpen(BlockState state) {
        return state.getValue(OPEN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null)
            return null;
        boolean powered = context.getLevel().hasNeighborSignal(context.getClickedPos());
        return state.setValue(POWERED, powered).setValue(OPEN, powered);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide)
            set(level, pos, state.setValue(OPEN, !state.getValue(OPEN)));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean movedByPiston) {
        if (level.isClientSide)
            return;
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED))
            set(level, pos, state.setValue(POWERED, powered).setValue(OPEN, powered));
    }

    private void set(Level level, BlockPos pos, BlockState state) {
        boolean open = state.getValue(OPEN);
        level.setBlock(pos, state, Block.UPDATE_ALL);
        level.playSound(null, pos, open ? SoundEvents.FIRE_EXTINGUISH : SoundEvents.IRON_TRAPDOOR_CLOSE,
                SoundSource.BLOCKS, 0.4f, open ? 1.6f : 1.2f);
        PneumaticPropagator.changed(level, pos.relative(state.getValue(FACING)));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide && oldState.getBlock() != this)
            level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!level.isClientSide && newState.getBlock() != this)
            PneumaticPropagator.changed(level, pos.relative(state.getValue(FACING)));
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        PneumaticPropagator.changed(level, pos.relative(state.getValue(FACING)));
    }
}
