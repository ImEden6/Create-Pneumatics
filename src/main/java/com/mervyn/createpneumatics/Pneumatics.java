package com.mervyn.createpneumatics;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.contraption.ContraptionMovementSetting;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Pneumatics {
    static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreatePneumatics.MODID);
    static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreatePneumatics.MODID);
    static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreatePneumatics.MODID);
    static final DeferredRegister<DisplaySource> DISPLAY_SOURCES = DeferredRegister.create(CreateRegistries.DISPLAY_SOURCE, CreatePneumatics.MODID);
    static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, CreatePneumatics.MODID);
    static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, CreatePneumatics.MODID);
    static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, CreatePneumatics.MODID);
    static final DeferredRegister<ArmInteractionPointType> ARM_POINTS = DeferredRegister.create(CreateRegistries.ARM_INTERACTION_POINT_TYPE, CreatePneumatics.MODID);

    public static final DeferredHolder<ArmInteractionPointType, VacuumPadArmPoint.Type> PAD_ARM_POINT = ARM_POINTS.register("vacuum_pad", VacuumPadArmPoint.Type::new);

    public static final DeferredHolder<SoundEvent, SoundEvent> BLOWER_RUNNING = sound("blower.running");
    public static final DeferredHolder<SoundEvent, SoundEvent> TUBE_VENT = sound("tube.vent");
    public static final DeferredHolder<SoundEvent, SoundEvent> TERMINAL_LAUNCH = sound("terminal.launch");
    public static final DeferredHolder<SoundEvent, SoundEvent> TERMINAL_ARRIVE = sound("terminal.arrive");
    public static final DeferredHolder<SoundEvent, SoundEvent> PAYLOAD_POP = sound("payload.pop");
    public static final DeferredHolder<SoundEvent, SoundEvent> VALVE_OPEN = sound("valve.open");
    public static final DeferredHolder<SoundEvent, SoundEvent> VALVE_CLOSE = sound("valve.close");
    public static final DeferredHolder<SoundEvent, SoundEvent> JAR_SEAL = sound("bell_jar.seal");
    public static final DeferredHolder<SoundEvent, SoundEvent> JAR_VENT = sound("bell_jar.vent");
    public static final DeferredHolder<SoundEvent, SoundEvent> JAR_DONE = sound("bell_jar.done");
    public static final DeferredHolder<SoundEvent, SoundEvent> PAD_GRAB = sound("vacuum_pad.grab");
    public static final DeferredHolder<SoundEvent, SoundEvent> PAD_RELEASE = sound("vacuum_pad.release");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CreatePneumatics.MODID, name)));
    }

    private static BlockBehaviour.Properties brass() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(2f).sound(SoundType.COPPER);
    }

    public static final DeferredBlock<RootsBlowerBlock> BLOWER = BLOCKS.register("roots_blower",
            () -> new RootsBlowerBlock(brass().noOcclusion()));
    public static final DeferredBlock<PneumaticTubeBlock> TUBE = BLOCKS.register("pneumatic_tube",
            () -> new PneumaticTubeBlock(brass().noOcclusion()));
    public static final DeferredBlock<PneumaticTubeBlock> GLASS_TUBE = BLOCKS.register("glass_pneumatic_tube",
            () -> new PneumaticTubeBlock(brass().sound(SoundType.GLASS).noOcclusion()));
    public static final DeferredBlock<PressureGaugeBlock> GAUGE = BLOCKS.register("pressure_gauge",
            () -> new PressureGaugeBlock(brass().noOcclusion()));
    public static final DeferredBlock<ReliefValveBlock> VALVE = BLOCKS.register("relief_valve",
            () -> new ReliefValveBlock(brass().noOcclusion()));

    public static final DeferredBlock<BellJarBlock> JAR = BLOCKS.register("bell_jar",
            () -> new BellJarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(1f).sound(SoundType.GLASS).noOcclusion()));

    public static final DeferredBlock<VacuumPadBlock> PAD = BLOCKS.register("vacuum_pad",
            () -> new VacuumPadBlock(brass().noOcclusion()));
    public static final DeferredBlock<PneumaticInterfaceBlock> INTERFACE = BLOCKS.register("pneumatic_interface",
            () -> new PneumaticInterfaceBlock(brass().noOcclusion()));
    public static final DeferredBlock<PneumaticTerminalBlock> TERMINAL = BLOCKS.register("pneumatic_terminal",
            () -> new PneumaticTerminalBlock(brass().noOcclusion()));
    public static final DeferredBlock<AirDiverterBlock> DIVERTER = BLOCKS.register("air_diverter",
            () -> new AirDiverterBlock(brass().noOcclusion()));
    public static final DeferredBlock<VacuumTankBlock> TANK = BLOCKS.register("vacuum_tank",
            () -> new VacuumTankBlock(brass().noOcclusion()));

    public static final DeferredItem<BlockItem> JAR_ITEM = ITEMS.registerSimpleBlockItem(JAR);
    public static final DeferredItem<BlockItem> PAD_ITEM = ITEMS.registerSimpleBlockItem(PAD);
    public static final DeferredItem<BlockItem> TANK_ITEM = ITEMS.registerSimpleBlockItem(TANK);
    public static final DeferredItem<BlockItem> INTERFACE_ITEM = ITEMS.registerSimpleBlockItem(INTERFACE);
    public static final DeferredItem<BlockItem> TERMINAL_ITEM = ITEMS.registerSimpleBlockItem(TERMINAL);
    public static final DeferredItem<BlockItem> DIVERTER_ITEM = ITEMS.registerSimpleBlockItem(DIVERTER);
    public static final DeferredItem<Item> RATION_PACK = ITEMS.registerSimpleItem("ration_pack",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.9f).build()));
    public static final DeferredItem<Item> OPTICAL_GLASS = ITEMS.registerSimpleItem("optical_glass");
    public static final DeferredItem<BlockItem> BLOWER_ITEM = ITEMS.registerSimpleBlockItem(BLOWER);
    public static final DeferredItem<BlockItem> TUBE_ITEM = ITEMS.registerSimpleBlockItem(TUBE);
    public static final DeferredItem<BlockItem> GLASS_TUBE_ITEM = ITEMS.registerSimpleBlockItem(GLASS_TUBE);
    public static final DeferredItem<BlockItem> GAUGE_ITEM = ITEMS.registerSimpleBlockItem(GAUGE);
    public static final DeferredItem<BlockItem> VALVE_ITEM = ITEMS.registerSimpleBlockItem(VALVE);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RootsBlowerBlockEntity>> BLOWER_BE = BLOCK_ENTITIES.register("roots_blower",
            () -> BlockEntityType.Builder.of((pos, state) -> new RootsBlowerBlockEntity(Pneumatics.BLOWER_BE.get(), pos, state), BLOWER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PneumaticTubeBlockEntity>> TUBE_BE = BLOCK_ENTITIES.register("pneumatic_tube",
            () -> BlockEntityType.Builder.of((pos, state) -> new PneumaticTubeBlockEntity(Pneumatics.TUBE_BE.get(), pos, state), TUBE.get(), GLASS_TUBE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PressureGaugeBlockEntity>> GAUGE_BE = BLOCK_ENTITIES.register("pressure_gauge",
            () -> BlockEntityType.Builder.of((pos, state) -> new PressureGaugeBlockEntity(Pneumatics.GAUGE_BE.get(), pos, state), GAUGE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BellJarBlockEntity>> JAR_BE = BLOCK_ENTITIES.register("bell_jar",
            () -> BlockEntityType.Builder.of((pos, state) -> new BellJarBlockEntity(Pneumatics.JAR_BE.get(), pos, state), JAR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VacuumPadBlockEntity>> PAD_BE = BLOCK_ENTITIES.register("vacuum_pad",
            () -> BlockEntityType.Builder.of((pos, state) -> new VacuumPadBlockEntity(Pneumatics.PAD_BE.get(), pos, state), PAD.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VacuumTankBlockEntity>> TANK_BE = BLOCK_ENTITIES.register("vacuum_tank",
            () -> BlockEntityType.Builder.of((pos, state) -> new VacuumTankBlockEntity(Pneumatics.TANK_BE.get(), pos, state), TANK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PneumaticTerminalBlockEntity>> TERMINAL_BE = BLOCK_ENTITIES.register("pneumatic_terminal",
            () -> BlockEntityType.Builder.of((pos, state) -> new PneumaticTerminalBlockEntity(Pneumatics.TERMINAL_BE.get(), pos, state), TERMINAL.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AirDiverterBlockEntity>> DIVERTER_BE = BLOCK_ENTITIES.register("air_diverter",
            () -> BlockEntityType.Builder.of((pos, state) -> new AirDiverterBlockEntity(Pneumatics.DIVERTER_BE.get(), pos, state), DIVERTER.get()).build(null));

    static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, CreatePneumatics.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<TubePayloadEntity>> PAYLOAD = ENTITY_TYPES.register("tube_payload",
            () -> EntityType.Builder.<TubePayloadEntity>of(TubePayloadEntity::new, MobCategory.MISC).sized(0.3f, 0.3f)
                    .clientTrackingRange(8).updateInterval(1).noSummon().build("tube_payload"));

    private static final ResourceLocation VACUUM_ID = ResourceLocation.fromNamespaceAndPath(CreatePneumatics.MODID, "vacuum_processing");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<VacuumRecipe>> VACUUM_SERIALIZER = SERIALIZERS.register("vacuum_processing",
            () -> new StandardProcessingRecipe.Serializer<>(VacuumRecipe::new));
    public static final DeferredHolder<RecipeType<?>, RecipeType<VacuumRecipe>> VACUUM_RECIPE = RECIPE_TYPES.register("vacuum_processing",
            () -> RecipeType.simple(VACUUM_ID));
    public static final IRecipeTypeInfo VACUUM_TYPE = new IRecipeTypeInfo() {
        @Override
        public ResourceLocation getId() {
            return VACUUM_ID;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends RecipeSerializer<?>> T getSerializer() {
            return (T) VACUUM_SERIALIZER.get();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <I extends RecipeInput, R extends Recipe<I>> RecipeType<R> getType() {
            return (RecipeType<R>) VACUUM_RECIPE.get();
        }
    };

    public static final DeferredHolder<DisplaySource, PressureGaugeDisplaySource> GAUGE_SOURCE =
            DISPLAY_SOURCES.register("pressure_gauge", PressureGaugeDisplaySource::new);

    private Pneumatics() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        DISPLAY_SOURCES.register(modBus);
        SERIALIZERS.register(modBus);
        RECIPE_TYPES.register(modBus);
        ARM_POINTS.register(modBus);
        SOUNDS.register(modBus);
        ENTITY_TYPES.register(modBus);
        modBus.addListener(Pneumatics::registerCapabilities);
        modBus.addListener(PadCargo::register);
        modBus.addListener(JarCycleSync::register);
        modBus.addListener(Pneumatics::commonSetup);
        modBus.addListener(Pneumatics::fillTab);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BlockStressValues.IMPACTS.register(BLOWER.get(), () -> 8.0);
            DisplaySource.BY_BLOCK.add(GAUGE.get(), GAUGE_SOURCE.get());
            MovementBehaviour.REGISTRY.register(JAR.get(), new BellJarMovement());
            MovementBehaviour.REGISTRY.register(PAD.get(), new VacuumPadMovement());
            ContraptionMovementSetting.REGISTRY.register(PAD.get(), () -> ContraptionMovementSetting.NO_PICKUP);
            MovementBehaviour.REGISTRY.register(INTERFACE.get(), new PneumaticInterfaceMovement());
        });
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TERMINAL_BE.get(), (be, side) -> be.inlet);
    }

    private static void fillTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != CreatePneumatics.TAB.getKey())
            return;
        event.accept(BLOWER_ITEM.get());
        event.accept(TUBE_ITEM.get());
        event.accept(GLASS_TUBE_ITEM.get());
        event.accept(GAUGE_ITEM.get());
        event.accept(VALVE_ITEM.get());
        event.accept(JAR_ITEM.get());
        event.accept(PAD_ITEM.get());
        event.accept(TANK_ITEM.get());
        event.accept(INTERFACE_ITEM.get());
        event.accept(TERMINAL_ITEM.get());
        event.accept(DIVERTER_ITEM.get());
        event.accept(RATION_PACK.get());
        event.accept(OPTICAL_GLASS.get());
    }

    public static boolean isFlange(BlockState state, Direction flange) {
        return state.getBlock() instanceof RootsBlowerBlock blower && blower.isFlange(state, flange);
    }

    // gauges and valves sit on a tube face, FACING points at the tube
    public static boolean isMountedOn(BlockState state, Direction towardsTube) {
        return (state.getBlock() instanceof PressureGaugeBlock || state.getBlock() instanceof ReliefValveBlock)
                && state.getValue(BlockStateProperties.FACING) == towardsTube;
    }
}
