package com.mervyn.createpneumatics;

import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

// The end of a transit line. Items go in from above (funnels, chutes, hoppers, a player's hand)
// and come out into whatever sits below it, or onto the floor.
public class PneumaticTerminalBlock extends Block implements IBE<PneumaticTerminalBlockEntity> {
    public PneumaticTerminalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide)
            return ItemInteractionResult.SUCCESS;
        return onBlockEntityUseItemOn(level, pos, terminal -> {
            ItemStack left = terminal.send(stack, false);
            if (left.getCount() == stack.getCount())
                return ItemInteractionResult.FAIL;
            player.setItemInHand(hand, left);
            return ItemInteractionResult.SUCCESS;
        });
    }

    @Override
    public Class<PneumaticTerminalBlockEntity> getBlockEntityClass() {
        return PneumaticTerminalBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PneumaticTerminalBlockEntity> getBlockEntityType() {
        return Pneumatics.TERMINAL_BE.get();
    }
}
