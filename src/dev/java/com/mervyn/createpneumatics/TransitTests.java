package com.mervyn.createpneumatics;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

// showcase-only transit checks. Push side: a terminal sends diamonds and cobble down a line, a
// diverter filtered to diamonds sends those down its branch, the rest reach the end. Pull side:
// a terminal at the far end sends iron back toward the blower, into a terminal beside it.
public final class TransitTests {
    private static BlockPos send, sorted, rest, far, near, diverter;

    private TransitTests() {}

    public static void build(ServerPlayer player, BlockPos at, Direction front) {
        ServerLevel level = player.serverLevel();
        Direction back = front.getOpposite(), left = front.getCounterClockWise(), right = front.getClockWise();
        BlockPos.betweenClosed(at.relative(back, 7).relative(left, 3).below(), at.relative(front, 11).relative(right, 5).above(3))
                .forEach(pos -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL));

        for (int i = 1; i <= 8; i++)
            if (i != 5)
                tube(level, at.relative(front, i), front);
        diverter = at.relative(front, 5);
        level.setBlock(diverter, Pneumatics.DIVERTER.get().defaultBlockState().setValue(AirDiverterBlock.FACING, right)
                .setValue(PneumaticTubeBlock.property(front), true).setValue(PneumaticTubeBlock.property(back), true)
                .setValue(PneumaticTubeBlock.property(right), true), Block.UPDATE_ALL);
        for (int i = 1; i <= 2; i++)
            tube(level, diverter.relative(right, i), right);
        for (int i = 1; i <= 4; i++)
            tube(level, at.relative(back, i), front);

        level.setBlock(at, Pneumatics.BLOWER.get().defaultBlockState().setValue(RootsBlowerBlock.HORIZONTAL_FACING, front), Block.UPDATE_ALL);
        level.setBlock(at.relative(left), AllBlocks.CREATIVE_MOTOR.get().defaultBlockState().setValue(CreativeMotorBlock.FACING, right), Block.UPDATE_ALL);
        if (level.getBlockEntity(at.relative(left)) instanceof CreativeMotorBlockEntity be)
            be.generatedSpeed.setValue(-128);

        send = terminal(level, at.relative(front, 1).relative(right));
        sorted = terminal(level, diverter.relative(right, 3));
        rest = terminal(level, at.relative(front, 9));
        far = terminal(level, at.relative(back, 5));
        near = terminal(level, at.relative(back, 1).relative(right));
        for (BlockPos pos : new BlockPos[] {sorted, rest, near})
            level.setBlock(pos.below(), Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(diverter) instanceof AirDiverterBlockEntity be)
            be.filter.setFilter(new ItemStack(Items.DIAMOND));
    }

    private static BlockPos terminal(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Pneumatics.TERMINAL.get().defaultBlockState(), Block.UPDATE_ALL);
        return pos;
    }

    private static void tube(ServerLevel level, BlockPos pos, Direction along) {
        level.setBlock(pos, Pneumatics.GLASS_TUBE.get().defaultBlockState()
                .setValue(PneumaticTubeBlock.property(along), true)
                .setValue(PneumaticTubeBlock.property(along.getOpposite()), true), Block.UPDATE_ALL);
    }

    public static void step(ServerPlayer player, int t) {
        if (send == null)
            return;
        ServerLevel level = player.serverLevel();
        if (t == 80)
            send(level, send, new ItemStack(Items.DIAMOND, 3));
        if (t == 85)
            send(level, send, new ItemStack(Items.COBBLESTONE, 16));
        if (t == 90)
            send(level, send, new ItemStack(Items.DIAMOND, 2));
        if (t == 80)
            send(level, far, new ItemStack(Items.IRON_INGOT, 4));
        if (t % 10 != 0 || t < 70)
            return;
        int riding = level.getEntitiesOfClass(TubePayloadEntity.class, new AABB(send).inflate(16)).size();
        log("t=" + t + " riding=" + riding + " sorted=" + contents(level, sorted) + " rest=" + contents(level, rest) + " near=" + contents(level, near)
                + " pressure=" + String.format("%.2f", TubeTransportBehaviour.get(level, diverter).pressure()));
    }

    private static void send(ServerLevel level, BlockPos terminal, ItemStack stack) {
        if (level.getBlockEntity(terminal) instanceof PneumaticTerminalBlockEntity be)
            log("send " + stack + " left=" + be.inlet.insertItem(0, stack, false));
    }

    private static String contents(ServerLevel level, BlockPos terminal) {
        StringBuilder out = new StringBuilder("[");
        if (level.getBlockEntity(terminal.below()) instanceof Container chest)
            for (int i = 0; i < chest.getContainerSize(); i++)
                if (!chest.getItem(i).isEmpty())
                    out.append(chest.getItem(i)).append(' ');
        return out.toString().trim() + "]";
    }

    private static void log(String line) {
        CreatePneumatics.LOGGER.info("TRANSIT {}", line);
    }
}
