package com.mervyn.createpneumatics;

import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.api.contraption.ContraptionMovementSetting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.Tags;

// A block lifted off the world by a suction pad: its state plus its block entity tag, nothing tied
// to a level, so it can ride inside a contraption's block entity data and survive a save.
public final class VacuumCargo {
    private VacuumCargo() {}

    public static boolean canLift(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getBlock() instanceof LiquidBlock)
            return false;
        if (!BlockMovementChecks.isMovementAllowed(state, level, pos) || BlockMovementChecks.isBrittle(state))
            return false;
        if (state.is(Tags.Blocks.RELOCATION_NOT_SUPPORTED) || ContraptionMovementSetting.get(state) == ContraptionMovementSetting.UNMOVABLE)
            return false;
        if (Config.PAD_BLACKLIST.get().contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString()))
            return false;
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) || state.hasProperty(BlockStateProperties.BED_PART)
                || state.hasProperty(BlockStateProperties.CHEST_TYPE) && state.getValue(BlockStateProperties.CHEST_TYPE) != ChestType.SINGLE)
            return false;
        BlockEntity be = level.getBlockEntity(pos);
        return be == null || be instanceof BaseContainerBlockEntity || be instanceof SignBlockEntity
                || be instanceof BeehiveBlockEntity || be instanceof SkullBlockEntity;
    }

    // no destroyBlock: that would drop loot and run break logic
    public static CompoundTag capture(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        CompoundTag cargo = new CompoundTag();
        cargo.put("State", NbtUtils.writeBlockState(state));
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            cargo.put("Data", be.saveWithFullMetadata(level.registryAccess()));
            level.removeBlockEntity(pos);
        }
        level.setBlock(pos, state.getFluidState().createLegacyBlock(), Block.UPDATE_ALL | Block.UPDATE_MOVE_BY_PISTON);
        return cargo;
    }

    public static BlockState state(Level level, CompoundTag cargo) {
        return NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), cargo.getCompound("State"));
    }

    // the same order Contraption.addBlocksToWorld puts blocks down in
    public static boolean place(Level level, BlockPos target, CompoundTag cargo) {
        BlockState there = level.getBlockState(target);
        if (!there.canBeReplaced() || there.getDestroySpeed(level, target) == -1)
            return false;
        BlockState state = state(level, cargo);
        for (Direction side : Direction.values())
            state = state.updateShape(side, level.getBlockState(target.relative(side)), level, target, target.relative(side));
        if (state.getBlock() instanceof SimpleWaterloggedBlock && state.hasProperty(BlockStateProperties.WATERLOGGED))
            state = state.setValue(BlockStateProperties.WATERLOGGED, level.getFluidState(target).getType() == Fluids.WATER);
        level.setBlock(target, state, Block.UPDATE_MOVE_BY_PISTON | Block.UPDATE_ALL);
        if (cargo.contains("Data")) {
            BlockEntity be = level.getBlockEntity(target);
            if (be != null) {
                CompoundTag data = cargo.getCompound("Data").copy();
                data.putInt("x", target.getX());
                data.putInt("y", target.getY());
                data.putInt("z", target.getZ());
                be.loadWithComponents(data, level.registryAccess());
                be.setChanged();
            }
        }
        level.updateNeighborsAt(target, state.getBlock());
        state.updateNeighbourShapes(level, target, Block.UPDATE_ALL);
        return true;
    }

    // last resort when there's nowhere to put it: the block as an item, carrying its contents
    public static void drop(Level level, BlockPos at, CompoundTag cargo) {
        ItemStack stack = item(level, cargo);
        if (!stack.isEmpty())
            Containers.dropItemStack(level, at.getX() + .5, at.getY() + .5, at.getZ() + .5, stack);
    }

    // keeps the state's properties and the block entity tag, so placing it back gives the same block
    public static ItemStack item(Level level, CompoundTag cargo) {
        BlockState state = state(level, cargo);
        ItemStack stack = new ItemStack(state.getBlock());
        if (stack.isEmpty())
            return stack;
        BlockItemStateProperties properties = BlockItemStateProperties.EMPTY;
        for (Property<?> property : state.getProperties())
            properties = with(properties, state, property);
        stack.set(DataComponents.BLOCK_STATE, properties);
        if (cargo.contains("Data")) {
            CompoundTag data = cargo.getCompound("Data").copy();
            data.remove("x");
            data.remove("y");
            data.remove("z");
            stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(data));
        }
        return stack;
    }

    private static <T extends Comparable<T>> BlockItemStateProperties with(BlockItemStateProperties properties, BlockState state, Property<T> property) {
        return properties.with(property, state.getValue(property));
    }
}
