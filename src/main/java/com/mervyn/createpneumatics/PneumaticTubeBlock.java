package com.mervyn.createpneumatics;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;

// A sealed brass line. The six booleans are "open at this face" rather than "touching a tube": a
// line that ends in mid-air keeps its end face open, which is what makes it vent. A neighbour that
// can hold pressure caps the face, which is what makes a run hold.
public class PneumaticTubeBlock extends Block implements IBE<PneumaticTubeBlockEntity> {
    private static final MapCodec<PneumaticTubeBlock> CODEC = simpleCodec(PneumaticTubeBlock::new);

    private static final VoxelShape CORE = Block.box(5.5, 5.5, 5.5, 10.5, 10.5, 10.5);
    private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Map.of(
            Direction.NORTH, Block.box(6, 6, 0, 10, 10, 8),
            Direction.SOUTH, Block.box(6, 6, 8, 10, 10, 16),
            Direction.WEST, Block.box(0, 6, 6, 8, 10, 10),
            Direction.EAST, Block.box(8, 6, 6, 16, 10, 10),
            Direction.UP, Block.box(6, 10, 6, 10, 16, 10),
            Direction.DOWN, Block.box(6, 0, 6, 10, 6, 10)));

    private final Map<BlockState, VoxelShape> shapes = new HashMap<>();
    // Set while our own deferred tick writes a corrected state back, so onPlace does not queue a
    // second pass that would write the same state again.
    private boolean recomputing;

    public PneumaticTubeBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (BooleanProperty side : PipeBlock.PROPERTY_BY_DIRECTION.values())
            state = state.setValue(side, false);
        registerDefaultState(state);
        for (BlockState possible : stateDefinition.getPossibleStates())
            shapes.put(possible, shape(possible));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PipeBlock.PROPERTY_BY_DIRECTION.values().toArray(BooleanProperty[]::new));
    }

    public static BooleanProperty property(Direction side) {
        return PipeBlock.PROPERTY_BY_DIRECTION.get(side);
    }

    public static boolean isOpenAt(BlockState state, Direction side) {
        return state.getValue(property(side));
    }

    public static boolean canConnectTo(BlockState neighbour, Direction towards) {
        if (neighbour.getBlock() instanceof PneumaticTubeBlock || neighbour.getBlock() instanceof BellJarBlock
                || neighbour.getBlock() instanceof VacuumTankBlock || neighbour.getBlock() instanceof VacuumPadBlock
                || neighbour.getBlock() instanceof PneumaticInterfaceBlock || neighbour.getBlock() instanceof PneumaticTerminalBlock)
            return true;
        if (Pneumatics.isFlange(neighbour, towards.getOpposite()))
            return true;
        return Pneumatics.isMountedOn(neighbour, towards.getOpposite());
    }

    public static boolean isOpenEnd(BlockGetter level, BlockPos pos, BlockState state, Direction side) {
        if (!isOpenAt(state, side))
            return false;
        BlockPos neighbourPos = pos.relative(side);
        BlockState neighbour = level.getBlockState(neighbourPos);
        if (canConnectTo(neighbour, side))
            return false;
        return !neighbour.isFaceSturdy(level, neighbourPos, side.getOpposite());
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return updateBlockState(defaultBlockState(), context.getNearestLookingDirection(), null,
                context.getLevel(), context.getClickedPos());
    }

    // Mirrors FluidPipeBlock: keep the face we were told about, then fill in the run so a loose end
    // stays open and a bare tube still draws as a straight segment along its preferred axis.
    public BlockState updateBlockState(BlockState state, Direction preferred, @Nullable Direction ignore,
            BlockGetter level, BlockPos pos) {
        BlockState previous = state;
        int previousSides = 0;
        for (Direction side : Direction.values())
            if (previous.getValue(property(side)))
                previousSides++;

        for (Direction side : Direction.values())
            if (side != ignore)
                state = state.setValue(property(side), canConnectTo(level.getBlockState(pos.relative(side)), side));

        Direction connected = null;
        for (Direction side : Direction.values())
            if (state.getValue(property(side))) {
                if (connected != null)
                    return state;
                connected = side;
            }

        if (connected != null)
            return state.setValue(property(connected.getOpposite()), true);

        if (previousSides == 2)
            return previous;

        return state.setValue(property(preferred), true).setValue(property(preferred.getOpposite()), true);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction side, BlockState neighbour, LevelAccessor level,
            BlockPos pos, BlockPos neighbourPos) {
        return updateBlockState(state, side, side.getOpposite(), level, pos);
    }

    private static Direction openDirection(BlockState state) {
        for (Direction side : Direction.values())
            if (state.getValue(property(side)))
                return side;
        return Direction.NORTH;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide || recomputing)
            return;
        // A piston moves the state verbatim and only tells us it moved. Defer, so every block in the
        // push has landed before the faces are worked out again.
        if (state != oldState || movedByPiston)
            level.scheduleTick(pos, this, 1, TickPriority.HIGH);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && state.getBlock() != newState.getBlock())
            PneumaticPropagator.changed(level, pos);
        IBE.onRemove(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from,
            boolean movedByPiston) {
        if (level.isClientSide)
            return;
        Direction side = null;
        for (Direction candidate : Direction.values())
            if (pos.relative(candidate).equals(from)) {
                side = candidate;
                break;
            }
        if (side == null || !isOpenAt(state, side))
            return;
        level.scheduleTick(pos, this, 1, TickPriority.HIGH);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState updated = updateBlockState(state, openDirection(state), null, level, pos);
        if (updated != state) {
            recomputing = true;
            try {
                level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
            } finally {
                recomputing = false;
            }
        }
        PneumaticPropagator.changed(level, pos);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        BlockState result = state;
        for (Direction side : Direction.values())
            result = result.setValue(property(rotation.rotate(side)), isOpenAt(state, side));
        return result;
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = state;
        for (Direction side : Direction.values())
            result = result.setValue(property(mirror.mirror(side)), isOpenAt(state, side));
        return result;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.get(state);
    }

    private static VoxelShape shape(BlockState state) {
        VoxelShape shape = CORE;
        for (Direction side : Direction.values())
            if (state.getValue(property(side)))
                shape = Shapes.or(shape, ARMS.get(side));
        return shape;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    public Class<PneumaticTubeBlockEntity> getBlockEntityClass() {
        return PneumaticTubeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PneumaticTubeBlockEntity> getBlockEntityType() {
        return Pneumatics.TUBE_BE.get();
    }
}
