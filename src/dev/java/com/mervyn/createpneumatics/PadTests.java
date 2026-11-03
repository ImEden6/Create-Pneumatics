package com.mervyn.createpneumatics;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlock;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock;
import com.simibubi.create.content.contraptions.piston.PistonExtensionPoleBlock;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.phys.AABB;

// showcase-only checks for the suction pad: a piston carries a pad and a charged tank four blocks.
// rows: a stocked chest to an open spot, a spawner it must refuse, a chest onto a stone it must drop
public final class PadTests {
    private static final int REACH = 4;
    private static BlockPos[] pads;
    private static BlockPos contact;
    private static BlockPos bearing, leaky, dock, arm, armFrom, armTo;
    private static Direction front;

    private PadTests() {}

    public static void build(ServerPlayer player, BlockPos at, Direction facing) {
        ServerLevel level = player.serverLevel();
        front = facing;
        Direction back = front.getOpposite(), right = front.getClockWise();
        BlockPos.betweenClosed(at.relative(back, REACH + 1).below(2), at.relative(front, REACH + 3).relative(right, 36).above(3))
                .forEach(pos -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL));
        pads = new BlockPos[3];
        for (int row = 0; row < 3; row++) {
            BlockPos piston = at.relative(right, row * 4);
            BlockPos pad = piston.relative(front);
            pads[row] = pad;
            level.setBlock(pad.below(2), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(pad.relative(front, REACH).below(2), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            if (row == 0) {
                level.setBlock(pad.below(), Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
                if (level.getBlockEntity(pad.below()) instanceof Container chest) {
                    chest.setItem(0, new ItemStack(Items.DIAMOND, 5));
                    chest.setItem(13, new ItemStack(Items.COBBLESTONE, 64));
                }
            } else if (row == 1) {
                level.setBlock(pad.below(), Blocks.SPAWNER.defaultBlockState(), Block.UPDATE_ALL);
            } else {
                level.setBlock(pad.below(), Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
                if (level.getBlockEntity(pad.below()) instanceof Container chest)
                    chest.setItem(4, new ItemStack(Items.EMERALD, 3));
                level.setBlock(pad.relative(front, REACH).below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            }

            level.setBlock(pad, Pneumatics.PAD.get().defaultBlockState().setValue(VacuumPadBlock.FACING, Direction.DOWN), Block.UPDATE_ALL);
            level.setBlock(pad.above(), Pneumatics.TANK.get().defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockEntity(pad.above()) instanceof VacuumTankBlockEntity tank) {
                tank.charge = 1;
                tank.setChanged();
            }
            level.addFreshEntity(new SuperGlueEntity(level, SuperGlueEntity.span(pad, pad.above())));

            for (int i = 1; i <= REACH; i++)
                level.setBlock(piston.relative(back, i), AllBlocks.PISTON_EXTENSION_POLE.getDefaultState().setValue(PistonExtensionPoleBlock.FACING, front), Block.UPDATE_ALL);
            if (row == 2) {
                turnRig(level, piston.relative(right, 4));
                dockRig(level, piston.relative(right, 8), front);
                BlockPos turn = bearing;
                turnRig(level, piston.relative(right, 20));
                leaky = bearing;
                bearing = turn;
                if (level.getBlockEntity(leaky.above().north()) instanceof Container chest)
                    chest.setItem(0, new ItemStack(Items.REDSTONE, 9));
                contactRig(level, piston.relative(right, 24), front);
                armRig(level, piston.relative(right, 16).relative(front, 2).below(), front);
            }
            level.setBlock(piston, AllBlocks.MECHANICAL_PISTON.getDefaultState().setValue(MechanicalPistonBlock.FACING, front)
                    .setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, front.getAxis() == Direction.Axis.X), Block.UPDATE_ALL);
        }
    }

    // a bearing turns a pad holding a north-facing chest, so the chest should land turned
    private static void turnRig(ServerLevel level, BlockPos at) {
        bearing = at;
        level.setBlock(at, AllBlocks.MECHANICAL_BEARING.getDefaultState().setValue(MechanicalBearingBlock.FACING, Direction.UP), Block.UPDATE_ALL);
        BlockPos pad = at.above();
        level.setBlock(pad, Pneumatics.PAD.get().defaultBlockState().setValue(VacuumPadBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
        level.setBlock(pad.above(), Pneumatics.TANK.get().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(pad.above()) instanceof VacuumTankBlockEntity tank)
            tank.charge = 1;
        level.addFreshEntity(new SuperGlueEntity(level, SuperGlueEntity.span(pad, pad.above())));
        level.setBlock(pad.north(), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
        level.setBlock(pad.north().below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
    }

    // a contact-mode pad set off over nothing and runs into a chest one block along
    private static void contactRig(ServerLevel level, BlockPos piston, Direction front) {
        Direction back = front.getOpposite();
        BlockPos pad = piston.relative(front);
        contact = pad;
        for (int i = 1; i <= REACH; i++)
            level.setBlock(piston.relative(back, i), AllBlocks.PISTON_EXTENSION_POLE.getDefaultState().setValue(PistonExtensionPoleBlock.FACING, front), Block.UPDATE_ALL);
        level.setBlock(piston, AllBlocks.MECHANICAL_PISTON.getDefaultState().setValue(MechanicalPistonBlock.FACING, front)
                .setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, front.getAxis() == Direction.Axis.X), Block.UPDATE_ALL);
        level.setBlock(pad, Pneumatics.PAD.get().defaultBlockState().setValue(VacuumPadBlock.FACING, Direction.DOWN), Block.UPDATE_ALL);
        if (level.getBlockEntity(pad) instanceof VacuumPadBlockEntity be)
            be.grip.setValue(VacuumPadBlockEntity.Grip.ON_CONTACT.ordinal());
        level.setBlock(pad.above(), Pneumatics.TANK.get().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(pad.above()) instanceof VacuumTankBlockEntity tank)
            tank.charge = 1;
        level.addFreshEntity(new SuperGlueEntity(level, SuperGlueEntity.span(pad, pad.above())));
        level.setBlock(pad.relative(front).below(), Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(pad.relative(front).below()) instanceof Container chest)
            chest.setItem(0, new ItemStack(Items.LAPIS_LAZULI, 12));
        level.setBlock(pad.relative(front).below(2), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pad.relative(front, REACH).below(2), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
    }

    // a piston pushes an interface with an empty tank up against a fed one, and should wait there
    private static void dockRig(ServerLevel level, BlockPos piston, Direction front) {
        Direction back = front.getOpposite(), left = front.getCounterClockWise();
        BlockPos moving = piston.relative(front);
        dock = moving;
        level.setBlock(piston.relative(back), AllBlocks.PISTON_EXTENSION_POLE.getDefaultState().setValue(PistonExtensionPoleBlock.FACING, front), Block.UPDATE_ALL);
        level.setBlock(piston, AllBlocks.MECHANICAL_PISTON.getDefaultState().setValue(MechanicalPistonBlock.FACING, front)
                .setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, front.getAxis() == Direction.Axis.X), Block.UPDATE_ALL);
        level.setBlock(moving, Pneumatics.INTERFACE.get().defaultBlockState().setValue(PneumaticInterfaceBlock.FACING, front), Block.UPDATE_ALL);
        level.setBlock(moving.above(), Pneumatics.TANK.get().defaultBlockState(), Block.UPDATE_ALL);
        level.addFreshEntity(new SuperGlueEntity(level, SuperGlueEntity.span(moving, moving.above())));
        BlockPos fixed = moving.relative(front, 2);
        level.setBlock(fixed, Pneumatics.INTERFACE.get().defaultBlockState().setValue(PneumaticInterfaceBlock.FACING, back), Block.UPDATE_ALL);
        level.setBlock(fixed.relative(front), Pneumatics.TUBE.get().defaultBlockState()
                .setValue(PneumaticTubeBlock.property(front), true).setValue(PneumaticTubeBlock.property(back), true), Block.UPDATE_ALL);
        blower(level, fixed.relative(front, 2), front, left);
    }

    // an arm takes a chest from under one pad and hands it to another
    private static void armRig(ServerLevel level, BlockPos at, Direction front) {
        Direction back = front.getOpposite(), left = front.getCounterClockWise();
        arm = at;
        armFrom = at.relative(front, 2).above();
        armTo = at.relative(back, 2).above();
        level.setBlock(at, AllBlocks.MECHANICAL_ARM.getDefaultState(), Block.UPDATE_ALL);
        for (BlockPos pad : new BlockPos[] {armFrom, armTo}) {
            level.setBlock(pad.above(), Pneumatics.PAD.get().defaultBlockState().setValue(VacuumPadBlock.FACING, Direction.DOWN), Block.UPDATE_ALL);
            level.setBlock(pad.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        }
        level.setBlock(armFrom, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(armFrom) instanceof Container chest)
            chest.setItem(2, new ItemStack(Items.GOLD_INGOT, 7));
        level.setBlock(armFrom.above(2), Pneumatics.TUBE.get().defaultBlockState()
                .setValue(PneumaticTubeBlock.property(Direction.DOWN), true).setValue(PneumaticTubeBlock.property(front), true), Block.UPDATE_ALL);
        blower(level, armFrom.above(2).relative(front), front, left);
        if (level.getBlockEntity(at) instanceof ArmBlockEntity be) {
            ListTag points = new ListTag();
            points.add(point(armFrom.above().subtract(at), "TAKE"));
            points.add(point(armTo.above().subtract(at), "DEPOSIT"));
            CompoundTag tag = be.saveWithoutMetadata(level.registryAccess());
            tag.put("InteractionPoints", points);
            be.loadWithComponents(tag, level.registryAccess());
        }
    }

    private static CompoundTag point(BlockPos offset, String mode) {
        CompoundTag point = new CompoundTag();
        point.putString("Type", CreatePneumatics.MODID + ":vacuum_pad");
        point.put("Pos", NbtUtils.writeBlockPos(offset));
        point.putString("Mode", mode);
        return point;
    }

    // a blower whose pull side is behind it, spun the way that pulls
    private static void blower(ServerLevel level, BlockPos at, Direction front, Direction left) {
        level.setBlock(at, Pneumatics.BLOWER.get().defaultBlockState().setValue(RootsBlowerBlock.HORIZONTAL_FACING, front), Block.UPDATE_ALL);
        level.setBlock(at.relative(left), AllBlocks.CREATIVE_MOTOR.get().defaultBlockState().setValue(CreativeMotorBlock.FACING, left.getOpposite()), Block.UPDATE_ALL);
        if (level.getBlockEntity(at.relative(left)) instanceof CreativeMotorBlockEntity be)
            be.generatedSpeed.setValue(-128);
    }

    // every motor the pad rigs need; the testbed places them stopped so they're started by hand
    public static void motors(ServerLevel level, boolean running) {
        for (BlockPos pad : pads)
            motor(level, pad.relative(front.getOpposite()).above(), Direction.DOWN, running ? 48 : 0);
        motor(level, bearing.below(), Direction.UP, running ? 8 : 0);
        motor(level, leaky.below(), Direction.UP, running ? 8 : 0);
        BlockPos cog = arm.relative(front.getCounterClockWise());
        level.setBlock(cog, AllBlocks.COGWHEEL.getDefaultState().setValue(CogWheelBlock.AXIS, Direction.Axis.Y), Block.UPDATE_ALL);
        motor(level, cog.below(), Direction.UP, running ? 32 : 0);
        motor(level, dock.relative(front.getOpposite()).above(), Direction.DOWN, running ? 32 : 0);
        motor(level, contact.relative(front.getOpposite()).above(), Direction.DOWN, running ? 32 : 0);
    }

    private static void motor(ServerLevel level, BlockPos pos, Direction facing, int speed) {
        level.setBlock(pos, AllBlocks.CREATIVE_MOTOR.get().defaultBlockState().setValue(CreativeMotorBlock.FACING, facing), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof CreativeMotorBlockEntity be)
            be.generatedSpeed.setValue(speed);
    }

    public static void step(ServerPlayer player, int t) {
        if (pads == null)
            return;
        ServerLevel level = player.serverLevel();
        if (t == 70)
            motors(level, true);
        if (t == 95)
            leak(level, leaky.above());
        if (t == 90)
            reload(level, bearing.above());
        if (t == 108)
            level.setBlock(bearing.below(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        if (t % 20 != 0)
            return;
        StringBuilder turn = new StringBuilder("turn pad=" + level.getBlockState(bearing.above()));
        for (Direction side : Direction.Plane.HORIZONTAL) {
            var state = level.getBlockState(bearing.above().relative(side));
            if (state.is(Blocks.CHEST))
                turn.append(' ').append(side).append(" chest facing ").append(state.getValue(ChestBlock.FACING));
        }
        log(turn.toString());
        StringBuilder leak = new StringBuilder("leak");
        for (BlockPos pos : BlockPos.betweenClosed(leaky.offset(-2, 0, -2), leaky.offset(2, 2, 2)))
            if (level.getBlockState(pos).is(Blocks.CHEST))
                leak.append(' ').append(pos.subtract(leaky).toShortString()).append(' ').append(describe(level, pos))
                        .append(" facing ").append(level.getBlockState(pos).getValue(ChestBlock.FACING));
        log(leak + drops(level, leaky));
        String tank = "dock";
        for (BlockPos pos : new BlockPos[] {dock.above(), dock.relative(front).above()})
            if (level.getBlockEntity(pos) instanceof VacuumTankBlockEntity be)
                tank += " tank at +" + (pos.equals(dock.above()) ? 0 : 1) + String.format(" charge=%.2f", be.charge);
        log(tank);
        StringBuilder touch = new StringBuilder("contact");
        for (int i = 0; i <= REACH; i++)
            if (level.getBlockState(contact.relative(front, i).below()).is(Blocks.CHEST))
                touch.append(" +").append(i).append(' ').append(describe(level, contact.relative(front, i).below()));
        log(touch.toString());
        log("arm from=" + describe(level, armFrom) + " to=" + describe(level, armTo));
        String[] names = {"chest", "spawner", "blocked"};
        for (int row = 0; row < 3; row++) {
            BlockPos start = pads[row].below(), end = pads[row].relative(front, REACH).below();
            log(names[row] + " start=" + describe(level, start) + " end=" + describe(level, end) + drops(level, end));
        }
    }

    // empties the tanks and leaves the cup a whisker above letting go
    private static void leak(ServerLevel level, BlockPos near) {
        for (AbstractContraptionEntity entity : level.getEntitiesOfClass(AbstractContraptionEntity.class, new AABB(near).inflate(2))) {
            for (var info : entity.getContraption().getBlocks().values())
                if (info.state().getBlock() instanceof VacuumTankBlock && info.nbt() != null)
                    info.nbt().putFloat("Charge", 0);
            for (var actor : entity.getContraption().getActors())
                if (actor.right != null && actor.right.blockEntityData.contains("Cargo"))
                    actor.right.blockEntityData.putFloat("Hold", 0.305f);
            log("leaked");
        }
    }

    // what a save and reload does to a moving contraption: written out, gone, read back in
    private static void reload(ServerLevel level, BlockPos near) {
        for (AbstractContraptionEntity entity : level.getEntitiesOfClass(AbstractContraptionEntity.class, new AABB(near).inflate(2))) {
            CompoundTag tag = new CompoundTag();
            if (!entity.save(tag))
                continue;
            entity.discard();
            Entity back = EntityType.loadEntityRecursive(tag, level, e -> e);
            log("reloaded contraption " + (back != null && level.addFreshEntity(back) ? "ok" : "FAILED"));
        }
    }

    private static String describe(ServerLevel level, BlockPos pos) {
        String out = level.getBlockState(pos).getBlock().toString();
        if (level.getBlockEntity(pos) instanceof Container chest) {
            StringBuilder items = new StringBuilder();
            for (int i = 0; i < chest.getContainerSize(); i++)
                if (!chest.getItem(i).isEmpty())
                    items.append(' ').append(i).append(':').append(chest.getItem(i));
            out += "[" + items.toString().trim() + "]";
        }
        return out;
    }

    private static String drops(ServerLevel level, BlockPos pos) {
        StringBuilder out = new StringBuilder();
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3))) {
            ItemStack stack = item.getItem();
            out.append(" drop=").append(stack);
            if (stack.has(DataComponents.BLOCK_ENTITY_DATA))
                out.append(" withData");
        }
        return out.toString();
    }

    private static void log(String line) {
        CreatePneumatics.LOGGER.info("PADTEST {}", line);
    }
}
