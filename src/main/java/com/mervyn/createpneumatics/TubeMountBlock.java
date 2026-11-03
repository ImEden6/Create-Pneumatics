package com.mervyn.createpneumatics;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// gauge and valve: a small fitting stuck to one face of a tube, FACING points at the tube
public abstract class TubeMountBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Map.of(
            Direction.NORTH, Block.box(4, 4, 0, 12, 12, 6),
            Direction.SOUTH, Block.box(4, 4, 10, 12, 12, 16),
            Direction.WEST, Block.box(0, 4, 4, 6, 12, 12),
            Direction.EAST, Block.box(10, 4, 4, 16, 12, 12),
            Direction.DOWN, Block.box(4, 0, 4, 12, 6, 12),
            Direction.UP, Block.box(4, 10, 4, 12, 16, 12)));

    protected TubeMountBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
        return canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.relative(state.getValue(FACING))).getBlock() instanceof PneumaticTubeBlock;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction side, BlockState neighbour, LevelAccessor level,
            BlockPos pos, BlockPos neighbourPos) {
        if (side == state.getValue(FACING) && !canSurvive(state, level, pos))
            return Blocks.AIR.defaultBlockState();
        return state;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }
}
