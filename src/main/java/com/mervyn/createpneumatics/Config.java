package com.mervyn.createpneumatics;

import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue BLOWER_RANGE = BUILDER
            .comment("How many tubes a Roots Blower can push pressure through")
            .defineInRange("blowerRange", 16, 1, 64);

    public static final ModConfigSpec.DoubleValue BLOWER_PRESSURE_SCALE = BUILDER
            .comment("Multiplier on the pressure a Roots Blower produces")
            .defineInRange("blowerPressureScale", 1.0, 0.0, 10.0);

    public static final ModConfigSpec.BooleanValue VENT_RUNS_PROCESSING = BUILDER
            .comment("Whether a venting tube processes items pushed through it like a fan would")
            .define("ventRunsProcessing", true);

    public static final ModConfigSpec.DoubleValue JAR_VACUUM = BUILDER
            .comment("How hard a vacuum a Bell Jar needs before it starts a recipe, 0 is open air and 1 is a full vacuum")
            .defineInRange("jarVacuum", 0.6, 0.05, 1.0);

    public static final ModConfigSpec.DoubleValue JAR_PUMP_RATE = BUILDER
            .comment("How fast a Bell Jar pumps down toward the tube's pressure, per tick, for a single jar")
            .defineInRange("jarPumpRate", 0.04, 0.001, 1.0);

    public static final ModConfigSpec.DoubleValue PAD_LIFT = BUILDER
            .comment("How much vacuum a Suction Pad needs to lift a block and keep holding it")
            .defineInRange("padLift", 0.3, 0.05, 1.0);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> PAD_BLACKLIST = BUILDER
            .comment("Block ids a Suction Pad refuses to lift, on top of what Create already won't move")
            .defineListAllowEmpty("padBlacklist", List.of(), () -> "minecraft:chest",
                    id -> id instanceof String s && ResourceLocation.tryParse(s) != null);

    public static final ModConfigSpec.DoubleValue PAD_GRAB_COST = BUILDER
            .comment("How much charge a Suction Pad takes out of a Vacuum Tank for each block it grabs")
            .defineInRange("padGrabCost", 0.25, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue PAD_LEAK = BUILDER
            .comment("How much vacuum a held block leaks out of the pad's cup each tick, made up from tanks while they last")
            .defineInRange("padLeak", 0.0005, 0.0, 0.1);

    public static final ModConfigSpec.DoubleValue INTERFACE_RATE = BUILDER
            .comment("How much charge a Pneumatic Interface puts into each docked tank per tick")
            .defineInRange("interfaceRate", 0.02, 0.001, 1.0);

    public static final ModConfigSpec.IntValue PAYLOAD_SPEED = BUILDER
            .comment("Most tubes a payload can cross in one tick, at full pressure")
            .defineInRange("payloadSpeed", 2, 1, 4);

    public static final ModConfigSpec.IntValue DIVERTER_TIMEOUT = BUILDER
            .comment("Ticks an Air Diverter waits on a payload that reserved it before letting the next one through")
            .defineInRange("diverterTimeout", 40, 5, 400);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
