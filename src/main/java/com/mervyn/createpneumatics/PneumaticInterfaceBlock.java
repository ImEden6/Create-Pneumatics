package com.mervyn.createpneumatics;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.simibubi.create.AllShapes;

import net.createmod.catnip.math.VoxelShaper;

// Two of these face to face, one fed by tubes and one on a contraption, top up the contraption's
// vacuum tanks while it waits there.
public class PneumaticInterfaceBlock extends DirectionalBlock {
    private static final MapCodec<PneumaticInterfaceBlock> CODEC = simpleCodec(PneumaticInterfaceBlock::new);
    private static final VoxelShaper SHAPE = new AllShapes.Builder(Block.box(0, 0, 0, 16, 12, 16)).add(3, 12, 3, 13, 14, 13).forDirectional(Direction.UP);

    public PneumaticInterfaceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE.get(state.getValue(FACING));
    }

    // the strongest pull on any tube behind or beside it, 0 when nothing pulls
    static float pull(Level level, BlockPos pos, Direction facing) {
        float pull = 0;
        for (Direction side : Direction.values()) {
            if (side == facing)
                continue;
            BlockPos at = pos.relative(side);
            TubeTransportBehaviour tube = TubeTransportBehaviour.get(level, at);
            if (tube != null && PneumaticTubeBlock.isOpenAt(level.getBlockState(at), side.getOpposite()))
                pull = Math.max(pull, -tube.get(side.getOpposite()).pressure());
        }
        return pull;
    }
}
