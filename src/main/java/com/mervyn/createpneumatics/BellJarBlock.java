package com.mervyn.createpneumatics;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// sits on a basin or depot and works on whatever is in it. FACING is where the gauge looks
public class BellJarBlock extends HorizontalDirectionalBlock implements IBE<BellJarBlockEntity> {
    private static final MapCodec<BellJarBlock> CODEC = simpleCodec(BellJarBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 3, 15), Block.box(2, 3, 2, 14, 14, 14), Block.box(6, 14, 6, 10, 16, 10));

    public BellJarBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
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
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    // a bare click vents the jar, which is how a batch is given up on
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide)
            if (BellJarBlockEntity.bottom(level, pos) instanceof BellJarBlockEntity be)
                be.vent();
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && state.getBlock() != newState.getBlock()) {
            withBlockEntityDo(level, pos, BellJarBlockEntity::dropContents);
            // breaking any jar in a stack opens the shared chamber
            if (level.getBlockState(pos.below()).getBlock() instanceof BellJarBlock && BellJarBlockEntity.bottom(level, pos.below()) instanceof BellJarBlockEntity be)
                be.vent();
        }
        IBE.onRemove(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    public static Direction facing(BlockState state) {
        return state.getValue(FACING);
    }

    @Override
    public Class<BellJarBlockEntity> getBlockEntityClass() {
        return BellJarBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BellJarBlockEntity> getBlockEntityType() {
        return Pneumatics.JAR_BE.get();
    }
}
