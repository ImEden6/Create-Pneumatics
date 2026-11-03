package com.mervyn.createpneumatics;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// A sealed box with two interlocking lobes in it. FACING is the pressure side: the flange that
// pushes when the shaft runs positive. The opposite flange pulls, so reversing rotation swaps
// push and pull without touching block state, and rotating the block swaps which run is which.
public class RootsBlowerBlock extends HorizontalKineticBlock implements IBE<RootsBlowerBlockEntity> {
    private static final MapCodec<RootsBlowerBlock> CODEC = simpleCodec(RootsBlowerBlock::new);

    // Boxes as drawn for FACING = SOUTH: housing, the two flanges on the front/back, the two
    // shaft stubs on the sides. Rotated per facing below.
    private static final int[][] PARTS = {
            {2, 2, 2, 14, 14, 14},
            {4, 4, 0, 12, 12, 2},
            {4, 4, 14, 12, 12, 16},
            {0, 4, 4, 2, 12, 12},
            {14, 4, 4, 16, 12, 12}};
    private static final Map<Direction, VoxelShape> SHAPES = buildShapes();

    public RootsBlowerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    private static Map<Direction, VoxelShape> buildShapes() {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        for (Direction facing : List.of(Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST)) {
            VoxelShape shape = Shapes.empty();
            for (int[] part : PARTS) {
                int[] b = map(part, facing);
                shape = Shapes.or(shape, Block.box(b[0], b[1], b[2], b[3], b[4], b[5]));
            }
            shapes.put(facing, shape);
        }
        return shapes;
    }

    // Rotates the base boxes so the drawn south flange ends up on the block's facing side.
    private static int[] map(int[] b, Direction facing) {
        return switch (facing) {
            case NORTH -> new int[] {16 - b[3], b[1], 16 - b[5], 16 - b[0], b[4], 16 - b[2]};
            case EAST -> new int[] {b[2], b[1], 16 - b[3], b[5], b[4], 16 - b[0]};
            case WEST -> new int[] {16 - b[5], b[1], b[0], 16 - b[2], b[4], b[3]};
            default -> b;
        };
    }

    public boolean isFlange(BlockState state, Direction side) {
        Direction facing = state.getValue(HORIZONTAL_FACING);
        return side == facing || side == facing.getOpposite();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState fallback = super.getStateForPlacement(context);
        Direction face = context.getClickedFace();
        if (!face.getAxis().isHorizontal())
            return fallback;
        // Placing against the face of a tube makes the pressure flange point at it.
        BlockPos behind = context.getClickedPos().relative(face.getOpposite());
        if (!(context.getLevel().getBlockState(behind).getBlock() instanceof PneumaticTubeBlock))
            return fallback;
        return defaultBlockState().setValue(HORIZONTAL_FACING, face.getOpposite());
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getClockWise().getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == getRotationAxis(state);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && state.getBlock() != newState.getBlock())
            PneumaticPropagator.changed(level, pos);
        IBE.onRemove(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(HORIZONTAL_FACING));
    }

    @Override
    public Class<RootsBlowerBlockEntity> getBlockEntityClass() {
        return RootsBlowerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends RootsBlowerBlockEntity> getBlockEntityType() {
        return Pneumatics.BLOWER_BE.get();
    }
}
