package com.mervyn.createpneumatics;

import java.util.List;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

public class PneumaticTerminalBlockEntity extends SmartBlockEntity {
    private static final int COOLDOWN = 4;

    private int cooldown;
    final IItemHandler inlet = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return send(stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
        }
    };

    public PneumaticTerminalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public void tick() {
        super.tick();
        if (cooldown > 0)
            cooldown--;
    }

    // the whole stack goes as one payload into whichever tube pulls hardest, if any tube has air in it
    ItemStack send(ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || cooldown > 0 || level.isClientSide)
            return stack;
        BlockPos best = null;
        float lowest = Float.MAX_VALUE;
        for (Direction side : Direction.values()) {
            BlockPos at = worldPosition.relative(side);
            TubeTransportBehaviour tube = TubeTransportBehaviour.get(level, at);
            if (tube == null || tube.pressure() == 0 || !PneumaticTubeBlock.isOpenAt(level.getBlockState(at), side.getOpposite()))
                continue;
            if (tube.pressure() < lowest) {
                lowest = tube.pressure();
                best = at;
            }
        }
        if (best == null)
            return stack;
        if (!simulate) {
            TubePayloadEntity.launch(level, stack.copy(), best, worldPosition);
            cooldown = COOLDOWN;
            level.playSound(null, worldPosition, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1, 1.4f);
        }
        return ItemStack.EMPTY;
    }

    // into the inventory below if there is one, out onto the floor otherwise
    void receive(ItemStack stack) {
        IItemHandler below = level.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.below(), Direction.UP);
        ItemStack left = stack.copy();
        if (below != null)
            for (int slot = 0; slot < below.getSlots() && !left.isEmpty(); slot++)
                left = below.insertItem(slot, left, false);
        if (left.isEmpty()) {
            level.playSound(null, worldPosition, SoundEvents.CHICKEN_EGG, SoundSource.BLOCKS, 0.6f, 0.6f);
            return;
        }
        TubePayloadEntity.eject(level, Vec3.atCenterOf(worldPosition).add(0, 0.7, 0), new Vec3(0, 0.2, 0), left);
    }
}
