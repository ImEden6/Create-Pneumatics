package com.mervyn.createpneumatics;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

// An arm working a stationary pad: taking from it lifts the block the cup faces (vacuum needed),
// handing it a block item puts that block down there (no vacuum needed). The item keeps the
// block's state and contents, so a chest goes through the arm with its inventory.
public class VacuumPadArmPoint extends ArmInteractionPoint {
    public VacuumPadArmPoint(ArmInteractionPointType type, Level level, BlockPos pos, BlockState state) {
        super(type, level, pos, state);
    }

    @Override
    protected IItemHandler getHandler(ArmBlockEntity arm) {
        return new Cup(level, pos);
    }

    record Cup(Level level, BlockPos pad) implements IItemHandler {
        private Direction facing() {
            return level.getBlockState(pad).getValue(VacuumPadBlock.FACING);
        }

        private BlockPos front() {
            return pad.relative(facing());
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            BlockPos front = front();
            return VacuumCargo.canLift(level, front) ? item(level.getBlockState(front).getBlock().asItem().getDefaultInstance()) : ItemStack.EMPTY;
        }

        private static ItemStack item(ItemStack stack) {
            return stack.getItem() instanceof BlockItem ? stack : ItemStack.EMPTY;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            BlockPos front = front();
            if (amount < 1 || !VacuumCargo.canLift(level, front) || getStackInSlot(0).isEmpty()
                    || PneumaticInterfaceBlock.pull(level, pad, facing()) < VacuumPadMovement.lift())
                return ItemStack.EMPTY;
            if (simulate)
                return getStackInSlot(0);
            CompoundTag cargo = VacuumCargo.capture(level, front);
            level.playSound(null, front, Pneumatics.PAD_GRAB.get(), SoundSource.BLOCKS, 1, 1);
            return VacuumCargo.item(level, cargo);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!(stack.getItem() instanceof BlockItem block))
                return stack;
            BlockPos front = front();
            BlockState there = level.getBlockState(front);
            if (!there.canBeReplaced() || there.getDestroySpeed(level, front) == -1)
                return stack;
            if (simulate)
                return stack.copyWithCount(stack.getCount() - 1);
            BlockState state = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).apply(block.getBlock().defaultBlockState());
            CompoundTag cargo = new CompoundTag();
            cargo.put("State", NbtUtils.writeBlockState(state));
            CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
            if (data != null)
                cargo.put("Data", data.copyTag());
            VacuumCargo.place(level, front, cargo);
            level.playSound(null, front, Pneumatics.PAD_RELEASE.get(), SoundSource.BLOCKS, 1, 1);
            return stack.copyWithCount(stack.getCount() - 1);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof BlockItem;
        }
    }

    public static class Type extends ArmInteractionPointType {
        @Override
        public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
            return state.is(Pneumatics.PAD.get());
        }

        @Override
        public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
            return new VacuumPadArmPoint(this, level, pos, state);
        }
    }
}
