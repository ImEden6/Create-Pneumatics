package com.mervyn.createpneumatics;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

// debug only: /oddsnends pneumatics dump <pos> and rebuild <pos>
public final class PneumaticCommands {
    private PneumaticCommands() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("pneumatics")
                .then(Commands.literal("dump")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(PneumaticCommands::dump)))
                .then(Commands.literal("rebuild")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(PneumaticCommands::rebuild)));
    }

    private static int dump(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        TubeTransportBehaviour tube = TubeTransportBehaviour.get(level, pos);
        if (tube == null) {
            ctx.getSource().sendFailure(Component.literal("no tube at " + pos.toShortString()));
            return 0;
        }
        StringBuilder out = new StringBuilder(pos.toShortString());
        for (Direction side : Direction.values()) {
            out.append("\n ").append(side.getName()).append(' ').append(String.format("%.3f", tube.get(side).pressure()));
            if (PneumaticTubeBlock.isOpenEnd(level, pos, level.getBlockState(pos), side))
                out.append(" open");
        }
        ctx.getSource().sendSuccess(() -> Component.literal(out.toString()), false);
        return 1;
    }

    // the showcase run's demo: a motor-driven blower pushing out a run that vents over a campfire,
    // and pulling through a run with a valve that sucks at its open top
    public static void showcase(ServerPlayer player, BlockPos at, Direction front) {
        ServerLevel level = player.serverLevel();
        Direction back = front.getOpposite(), left = front.getCounterClockWise(), right = front.getClockWise();
        BlockPos.betweenClosed(at.relative(front, 7).relative(left, 3), at.relative(back, 5).relative(right, 3).above(4))
                .forEach(pos -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL));

        for (int i = 1; i <= 4; i++)
            tube(level, at.relative(front, i), front);
        for (int i = 1; i <= 3; i++)
            tube(level, at.relative(back, i), front);
        for (int i = 1; i <= 2; i++)
            tube(level, at.relative(back, 3).above(i), Direction.UP);

        level.setBlock(at, Pneumatics.BLOWER.get().defaultBlockState().setValue(RootsBlowerBlock.HORIZONTAL_FACING, front), Block.UPDATE_ALL);
        BlockPos motor = at.relative(left);
        level.setBlock(motor, AllBlocks.CREATIVE_MOTOR.get().defaultBlockState().setValue(CreativeMotorBlock.FACING, right), Block.UPDATE_ALL);
        if (level.getBlockEntity(motor) instanceof CreativeMotorBlockEntity be)
            be.generatedSpeed.setValue(-128);

        level.setBlock(at.relative(front, 2).above(), Pneumatics.GAUGE.get().defaultBlockState().setValue(TubeMountBlock.FACING, Direction.DOWN), Block.UPDATE_ALL);
        level.setBlock(at.relative(back, 2).above(), Pneumatics.GAUGE.get().defaultBlockState().setValue(TubeMountBlock.FACING, Direction.DOWN), Block.UPDATE_ALL);
        level.setBlock(at.relative(back, 1).relative(right), Pneumatics.VALVE.get().defaultBlockState().setValue(TubeMountBlock.FACING, left), Block.UPDATE_ALL);
        level.setBlock(at.relative(front, 6).below(), Blocks.CAMPFIRE.defaultBlockState(), Block.UPDATE_ALL);

        BlockPos jar = at.relative(back, 3).relative(right);
        level.setBlock(jar.below(), AllBlocks.BASIN.getDefaultState(), Block.UPDATE_ALL);
        level.setBlock(jar, Pneumatics.JAR.get().defaultBlockState().setValue(BellJarBlock.FACING, right), Block.UPDATE_ALL);
        IItemHandler basin = level.getCapability(Capabilities.ItemHandler.BLOCK, jar.below(), Direction.UP);
        if (basin != null)
            basin.insertItem(0, new ItemStack(Items.MUSHROOM_STEW), false);
    }

    private static void tube(ServerLevel level, BlockPos pos, Direction along) {
        level.setBlock(pos, Pneumatics.TUBE.get().defaultBlockState()
                .setValue(PneumaticTubeBlock.property(along), true)
                .setValue(PneumaticTubeBlock.property(along.getOpposite()), true), Block.UPDATE_ALL);
    }

    private static int rebuild(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        long start = System.nanoTime();
        PneumaticPropagator.changed(ctx.getSource().getLevel(), pos);
        long micros = (System.nanoTime() - start) / 1000;
        ctx.getSource().sendSuccess(() -> Component.literal("rebuilt in " + micros + "us"), false);
        return 1;
    }
}
