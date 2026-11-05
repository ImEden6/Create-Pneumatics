package com.mervyn.createpneumatics;

import java.util.List;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlock;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

// showcase-only checks for the jar: valve opened mid-run, a ride on a bearing, an nbt round trip, and sintering over a superheated burner
public final class JarTests {
    private static BlockPos valveJar, valve, bearing, rideJar, tallJar, sinterJar;
    private static Direction front;

    private JarTests() {}

    public static void build(ServerPlayer player, BlockPos at, Direction facing) {
        ServerLevel level = player.serverLevel();
        front = facing;
        Direction back = front.getOpposite(), left = front.getCounterClockWise(), right = front.getClockWise();
        BlockPos.betweenClosed(at.relative(front, 2).relative(left, 2), at.relative(back, 6).relative(right, 7).above(4))
                .forEach(pos -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL));

        level.setBlock(at, Pneumatics.BLOWER.get().defaultBlockState().setValue(RootsBlowerBlock.HORIZONTAL_FACING, front), Block.UPDATE_ALL);
        BlockPos motor = at.relative(left);
        level.setBlock(motor, AllBlocks.CREATIVE_MOTOR.get().defaultBlockState().setValue(CreativeMotorBlock.FACING, right), Block.UPDATE_ALL);
        if (level.getBlockEntity(motor) instanceof CreativeMotorBlockEntity be)
            be.generatedSpeed.setValue(-128);
        for (int i = 1; i <= 3; i++)
            level.setBlock(at.relative(back, i), Pneumatics.TUBE.get().defaultBlockState()
                    .setValue(PneumaticTubeBlock.property(front), true)
                    .setValue(PneumaticTubeBlock.property(back), true), Block.UPDATE_ALL);
        valve = at.relative(back, 1).relative(right);
        level.setBlock(valve, Pneumatics.VALVE.get().defaultBlockState().setValue(TubeMountBlock.FACING, left), Block.UPDATE_ALL);
        valveJar = at.relative(back, 4);
        jarOnBasin(level, valveJar, right);

        BlockPos tall = at.relative(right, 3);
        level.setBlock(tall, Pneumatics.BLOWER.get().defaultBlockState().setValue(RootsBlowerBlock.HORIZONTAL_FACING, front), Block.UPDATE_ALL);
        BlockPos tallMotor = tall.relative(left);
        level.setBlock(tallMotor, AllBlocks.CREATIVE_MOTOR.get().defaultBlockState().setValue(CreativeMotorBlock.FACING, right), Block.UPDATE_ALL);
        if (level.getBlockEntity(tallMotor) instanceof CreativeMotorBlockEntity be)
            be.generatedSpeed.setValue(-128);
        for (int i = 1; i <= 3; i++)
            level.setBlock(tall.relative(back, i), Pneumatics.TUBE.get().defaultBlockState()
                    .setValue(PneumaticTubeBlock.property(front), true)
                    .setValue(PneumaticTubeBlock.property(back), true), Block.UPDATE_ALL);
        tallJar = tall.relative(back, 4);
        jarOnBasin(level, tallJar, right);
        for (int i = 1; i <= 2; i++)
            level.setBlock(tallJar.above(i), Pneumatics.JAR.get().defaultBlockState().setValue(BellJarBlock.FACING, right), Block.UPDATE_ALL);

        bearing = at.relative(right, 5).relative(back, 2);
        level.setBlock(bearing, AllBlocks.MECHANICAL_BEARING.getDefaultState().setValue(MechanicalBearingBlock.FACING, Direction.UP), Block.UPDATE_ALL);
        rideJar = bearing.above(2);
        jarOnBasin(level, rideJar, right);
        level.addFreshEntity(new SuperGlueEntity(level, SuperGlueEntity.span(rideJar.below(), rideJar)));

        BlockPos hot = at.relative(right, 7);
        level.setBlock(hot, Pneumatics.BLOWER.get().defaultBlockState().setValue(RootsBlowerBlock.HORIZONTAL_FACING, front), Block.UPDATE_ALL);
        BlockPos hotMotor = hot.relative(left);
        level.setBlock(hotMotor, AllBlocks.CREATIVE_MOTOR.get().defaultBlockState().setValue(CreativeMotorBlock.FACING, right), Block.UPDATE_ALL);
        if (level.getBlockEntity(hotMotor) instanceof CreativeMotorBlockEntity be)
            be.generatedSpeed.setValue(-128);
        for (int i = 1; i <= 3; i++)
            level.setBlock(hot.relative(back, i), Pneumatics.TUBE.get().defaultBlockState()
                    .setValue(PneumaticTubeBlock.property(front), true)
                    .setValue(PneumaticTubeBlock.property(back), true), Block.UPDATE_ALL);
        sinterJar = hot.relative(back, 4);
        BlockPos burner = sinterJar.below(2);
        level.setBlock(burner, AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.SMOULDERING), Block.UPDATE_ALL);
        BlazeBurnerBlock.tryInsert(level.getBlockState(burner), level, burner, AllItems.BLAZE_CAKE.asStack(), false, true, false);
        level.setBlock(sinterJar.below(), AllBlocks.BASIN.getDefaultState(), Block.UPDATE_ALL);
        level.setBlock(sinterJar, Pneumatics.JAR.get().defaultBlockState().setValue(BellJarBlock.FACING, right), Block.UPDATE_ALL);
        IItemHandler basin = level.getCapability(Capabilities.ItemHandler.BLOCK, sinterJar.below(), Direction.UP);
        if (basin != null)
            basin.insertItem(0, AllItems.CRUSHED_IRON.asStack(8), false);
    }

    private static void jarOnBasin(ServerLevel level, BlockPos jar, Direction dial) {
        level.setBlock(jar.below(), AllBlocks.BASIN.getDefaultState(), Block.UPDATE_ALL);
        level.setBlock(jar, Pneumatics.JAR.get().defaultBlockState().setValue(BellJarBlock.FACING, dial), Block.UPDATE_ALL);
        IItemHandler basin = level.getCapability(Capabilities.ItemHandler.BLOCK, jar.below(), Direction.UP);
        if (basin != null)
            basin.insertItem(0, new ItemStack(Items.MUSHROOM_STEW), false);
    }

    public static void step(ServerPlayer player, int t) {
        if (valveJar == null)
            return;
        ServerLevel level = player.serverLevel();
        if (t == 20)
            seedRunning(level, rideJar);
        if (t == 150)
            roundTrip(level, valveJar);
        if (t == 160) {
            level.getBlockState(valve).useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(valve), Direction.UP, valve, false));
            log("valve opened");
        }
        if (t == 60) {
            BlockPos motor = bearing.below();
            level.setBlock(motor, AllBlocks.CREATIVE_MOTOR.get().defaultBlockState().setValue(CreativeMotorBlock.FACING, Direction.UP), Block.UPDATE_ALL);
            if (level.getBlockEntity(motor) instanceof CreativeMotorBlockEntity be)
                be.generatedSpeed.setValue(16);
            log("bearing motor on");
        }
        if (t == 200) {
            level.setBlock(bearing.below(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            log("bearing motor off");
        }
        if (t % 20 == 0) {
            report(level, "valve", valveJar);
            report(level, "ride", rideJar);
            report(level, "tall", tallJar);
            report(level, "sinter", sinterJar);
        }
    }

    @SuppressWarnings("unchecked")
    private static void seedRunning(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof BellJarBlockEntity be))
            return;
        RecipeHolder<VacuumRecipe> holder = (RecipeHolder<VacuumRecipe>) level.getRecipeManager()
                .byKey(ResourceLocation.fromNamespaceAndPath(CreatePneumatics.MODID, "vacuum_processing/ration_pack_from_stew")).orElseThrow();
        IItemHandler basin = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.below(), Direction.UP);
        ItemStack stew = basin == null ? new ItemStack(Items.MUSHROOM_STEW) : basin.extractItem(0, 1, false);
        BellJarCycle cycle = be.cycle();
        cycle.start(holder, List.of(stew), List.of());
        cycle.pressure = -0.9f;
        cycle.phase = BellJarCycle.Phase.RUNNING;
        cycle.timer = 2000;
        be.setChanged();
        be.sendData();
        log("ride jar seeded running");
    }

    private static void roundTrip(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof BellJarBlockEntity be))
            return;
        CompoundTag saved = be.saveWithFullMetadata(level.registryAccess());
        BellJarBlockEntity fresh = new BellJarBlockEntity(Pneumatics.JAR_BE.get(), pos, be.getBlockState());
        fresh.loadWithComponents(saved, level.registryAccess());
        CompoundTag again = fresh.saveWithFullMetadata(level.registryAccess());
        log("round trip " + (saved.equals(again) ? "same" : "DIFFERS") + " phase " + fresh.cycle().phase
                + " timer " + fresh.cycle().timer + " batch " + fresh.cycle().batch);
    }

    private static void report(ServerLevel level, String name, BlockPos pos) {
        StringBuilder out = new StringBuilder(name);
        if (level.getBlockEntity(pos) instanceof BellJarBlockEntity be) {
            BellJarCycle c = be.cycle();
            out.append(' ').append(c.phase).append(String.format(" p=%.2f t=%d batch=%s out=%s", c.pressure, c.timer, c.batch, c.outputs));
        } else
            out.append(" no jar (").append(level.getBlockState(pos).getBlock()).append(')');
        if (level.getBlockState(pos.below(2)).hasProperty(BlazeBurnerBlock.HEAT_LEVEL))
            out.append(" heat=").append(level.getBlockState(pos.below(2)).getValue(BlazeBurnerBlock.HEAT_LEVEL));
        IItemHandler basin = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.below(), Direction.UP);
        if (basin != null) {
            out.append(" basin");
            for (int i = 0; i < basin.getSlots(); i++)
                if (!basin.getStackInSlot(i).isEmpty())
                    out.append(' ').append(basin.getStackInSlot(i));
        }
        log(out.toString());
    }

    private static void log(String line) {
        CreatePneumatics.LOGGER.info("JARTEST {}", line);
    }
}
