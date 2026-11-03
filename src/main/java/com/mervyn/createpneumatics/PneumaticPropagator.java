package com.mervyn.createpneumatics;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

// Rebuilds a network from scratch whenever something about it changes. Nothing here runs per tick.
//
// Pressure is never stored on a network object and never saved. It lives on the six faces of each
// tube and is thrown away and recomputed on every change, which is why a piston move, a chunk
// reload or a world reload cannot leave stale pressure behind.
public final class PneumaticPropagator {
    // A runaway loop or a pathological build should not stall the server. Real networks are tiny.
    private static final int MAX_TUBES = 4096;
    // Pressure is squeezed to nothing within this many blocks of an opening.
    private static final int LEAK_BLOCKS = 3;

    // tubes loading in with their chunk all ask for a rebuild in the same tick; they're collected
    // here and rebuilt once per network at the end of the tick instead of once per tube
    private static final Map<Level, Set<BlockPos>> QUEUED = new WeakHashMap<>();

    private PneumaticPropagator() {}

    public static void queue(Level level, BlockPos pos) {
        if (!level.isClientSide)
            QUEUED.computeIfAbsent(level, l -> new LinkedHashSet<>()).add(pos.immutable());
    }

    static void flush(LevelTickEvent.Post event) {
        Level level = event.getLevel();
        Set<BlockPos> queued = QUEUED.remove(level);
        if (queued == null)
            return;
        Set<BlockPos> visited = new HashSet<>();
        for (BlockPos pos : queued)
            changed(level, pos, visited);
    }

    public static void changed(Level level, BlockPos origin) {
        if (!level.isClientSide)
            changed(level, origin, new HashSet<>());
    }

    // a removed tube can split a line in two, so each side gets its own pass: an open valve on
    // one half must not empty the other
    private static void changed(Level level, BlockPos origin, Set<BlockPos> visited) {
        if (!level.isLoaded(origin))
            return;
        for (BlockPos start : seeds(level, origin))
            if (!visited.contains(start))
                rebuild(level, start, visited);
    }

    private static void rebuild(Level level, BlockPos start, Set<BlockPos> visited) {
        List<BlockPos> order = new ArrayList<>();
        Set<Flange> flanges = new LinkedHashSet<>();
        boolean vented = false;

        Deque<BlockPos> frontier = new ArrayDeque<>();
        frontier.addLast(start);

        while (!frontier.isEmpty() && order.size() < MAX_TUBES) {
            BlockPos pos = frontier.pollFirst();
            if (!visited.add(pos))
                continue;
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof PneumaticTubeBlock))
                continue;
            order.add(pos);

            for (Direction side : Direction.values()) {
                if (!PneumaticTubeBlock.isOpenAt(state, side))
                    continue;
                BlockPos target = pos.relative(side);
                // reading an unloaded chunk would load it; the line just ends at the border for now
                if (!level.isLoaded(target))
                    continue;
                BlockState targetState = level.getBlockState(target);
                if (targetState.getBlock() instanceof PneumaticTubeBlock) {
                    if (!visited.contains(target))
                        frontier.addLast(target);
                    continue;
                }
                Direction towardsTube = side.getOpposite();
                if (Pneumatics.isFlange(targetState, towardsTube)) {
                    flanges.add(new Flange(target, towardsTube));
                } else if (Pneumatics.isMountedOn(targetState, towardsTube)
                        && targetState.getBlock() instanceof ReliefValveBlock valve
                        && valve.isOpen(targetState)) {
                    // An open valve is a hole in the network, not a weak spot in it.
                    vented = true;
                }
            }
        }

        for (BlockPos pos : order) {
            TubeTransportBehaviour pipe = TubeTransportBehaviour.get(level, pos);
            if (pipe != null)
                pipe.wipePressure();
        }
        if (!vented && !order.isEmpty())
            pressurise(level, order, flanges);

        for (BlockPos pos : order)
            if (level.getBlockEntity(pos) instanceof PneumaticTubeBlockEntity tube)
                tube.pressureChanged();
    }

    private static void pressurise(Level level, List<BlockPos> order, Set<Flange> flanges) {

        int range = Math.max(1, Config.BLOWER_RANGE.get());
        double scale = Config.BLOWER_PRESSURE_SCALE.get();

        for (Flange flange : flanges) {
            if (!(level.getBlockEntity(flange.pos()) instanceof RootsBlowerBlockEntity blower))
                continue;
            float supply = blower.supply(flange.dir());
            if (supply == 0)
                continue;
            spread(level, flange.pos().relative(flange.dir()), supply > 0,
                    (float) (Math.abs(supply) * scale), range);
        }

        applyLeaks(level, order);
    }

    private static List<BlockPos> seeds(Level level, BlockPos origin) {
        if (level.getBlockState(origin).getBlock() instanceof PneumaticTubeBlock)
            return List.of(origin);
        List<BlockPos> seeds = new ArrayList<>();
        for (Direction side : Direction.values()) {
            BlockPos target = origin.relative(side);
            if (level.isLoaded(target) && level.getBlockState(target).getBlock() instanceof PneumaticTubeBlock)
                seeds.add(target);
        }
        return seeds;
    }

    // One blower flange feeding one direction. Distance is counted in tubes, not in blocks along a
    // diagonal, so a bend costs the same as a straight run.
    private static void spread(Level level, BlockPos start, boolean inbound, float strength, int range) {
        if (!(level.getBlockState(start).getBlock() instanceof PneumaticTubeBlock))
            return;

        Map<BlockPos, Integer> distance = new HashMap<>();
        Deque<BlockPos> frontier = new ArrayDeque<>();
        distance.put(start, 0);
        frontier.addLast(start);

        while (!frontier.isEmpty()) {
            BlockPos pos = frontier.pollFirst();
            int d = distance.get(pos);
            if (d >= range)
                continue;

            TubeTransportBehaviour pipe = TubeTransportBehaviour.get(level, pos);
            if (pipe != null)
                pipe.addPressure(inbound, strength * (1f - (float) d / range));

            BlockState state = level.getBlockState(pos);
            for (Direction side : Direction.values()) {
                if (!PneumaticTubeBlock.isOpenAt(state, side))
                    continue;
                BlockPos next = pos.relative(side);
                if (distance.containsKey(next) || !level.isLoaded(next))
                    continue;
                if (!(level.getBlockState(next).getBlock() instanceof PneumaticTubeBlock))
                    continue;
                distance.put(next, d + 1);
                frontier.addLast(next);
            }
        }
    }

    // Air escapes at an open end, so pressure falls away as the opening gets closer. The tube that
    // owns the opening keeps a quarter of its pressure, which is what its AirCurrent blows with;
    // the full value is back four blocks in. A network with no opening is untouched.
    private static void applyLeaks(Level level, List<BlockPos> order) {
        Map<BlockPos, Integer> distance = new HashMap<>();
        Deque<BlockPos> frontier = new ArrayDeque<>();

        for (BlockPos pos : order) {
            if (!hasOpenEnd(level, pos))
                continue;
            distance.put(pos, 0);
            frontier.addLast(pos);
        }

        while (!frontier.isEmpty()) {
            BlockPos pos = frontier.pollFirst();
            int d = distance.get(pos);
            if (d >= LEAK_BLOCKS)
                continue;
            BlockState state = level.getBlockState(pos);
            for (Direction side : Direction.values()) {
                if (!PneumaticTubeBlock.isOpenAt(state, side))
                    continue;
                BlockPos next = pos.relative(side);
                if (distance.containsKey(next) || !level.isLoaded(next))
                    continue;
                if (!(level.getBlockState(next).getBlock() instanceof PneumaticTubeBlock))
                    continue;
                distance.put(next, d + 1);
                frontier.addLast(next);
            }
        }

        for (Map.Entry<BlockPos, Integer> entry : distance.entrySet()) {
            TubeTransportBehaviour pipe = TubeTransportBehaviour.get(level, entry.getKey());
            if (pipe == null)
                continue;
            float factor = Math.min(1f, (entry.getValue() + 1f) / (LEAK_BLOCKS + 1f));
            pipe.scalePressure(factor);
        }
    }

    private static boolean hasOpenEnd(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        for (Direction side : Direction.values())
            if (level.isLoaded(pos.relative(side)) && PneumaticTubeBlock.isOpenEnd(level, pos, state, side))
                return true;
        return false;
    }

    private record Flange(BlockPos pos, Direction dir) {}
}
