package com.mervyn.createpneumatics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.IItemHandler;

public class BellJarBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    static final int MAX_HEIGHT = 8;

    BellJarCycle cycle = new BellJarCycle();
    @Nullable
    private RecipeHolder<VacuumRecipe> current;
    private float sentPressure;
    private BellJarCycle.Phase sentPhase = BellJarCycle.Phase.IDLE;
    private float needle, previousNeedle;

    public BellJarBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide) {
            previousNeedle = needle;
            needle += (cycle.pressure - needle) * 0.2f;
            return;
        }
        if (stacked()) {
            if (cycle.busy() || cycle.pressure != 0)
                vent();
            return;
        }
        int height = height();
        float supply = supply(height);
        cycle.tickPressure(supply, height);
        if (cycle.phase == BellJarCycle.Phase.IDLE && supply < BellJarCycle.threshold() && level.getGameTime() % 10 == 0)
            tryStart();
        BellJarCycle.Phase before = cycle.phase;
        cycle.tick(recipe(), level.random);
        // a batch that ran its time out still has its vacuum; one that lost it ends quietly
        if (before == BellJarCycle.Phase.RUNNING && cycle.phase == BellJarCycle.Phase.DONE && cycle.pressure <= BellJarCycle.LOST)
            level.playSound(null, worldPosition, Pneumatics.JAR_DONE.get(), SoundSource.BLOCKS, 0.6f, 1);
        if (cycle.phase == BellJarCycle.Phase.DONE)
            unload();

        if (cycle.phase != sentPhase || Math.abs(cycle.pressure - sentPressure) > 0.02f) {
            if (cycle.phase == BellJarCycle.Phase.RUNNING && sentPhase == BellJarCycle.Phase.PUMPING)
                level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.5f, 1.4f);
            sentPhase = cycle.phase;
            sentPressure = cycle.pressure;
            sendData();
            setChanged();
        }
    }

    // jars stacked on this one share its chamber; only the bottom jar runs the cycle
    public boolean stacked() {
        return level.getBlockState(worldPosition.below()).getBlock() instanceof BellJarBlock;
    }

    private int height() {
        int height = 1;
        while (height < MAX_HEIGHT && level.getBlockState(worldPosition.above(height)).getBlock() instanceof BellJarBlock)
            height++;
        return height;
    }

    @Nullable
    static BellJarBlockEntity bottom(Level level, BlockPos pos) {
        while (level.getBlockState(pos.below()).getBlock() instanceof BellJarBlock)
            pos = pos.below();
        return level.getBlockEntity(pos) instanceof BellJarBlockEntity be ? be : null;
    }

    // the strongest vacuum any tube pressed against the chamber offers, NaN when none is
    private float supply(int height) {
        float best = Float.NaN;
        for (int y = 0; y < height; y++)
            for (Direction side : Direction.values()) {
                if (side == Direction.DOWN)
                    continue;
                float pressure = tubeAt(worldPosition.above(y), side);
                if (!Float.isNaN(pressure))
                    best = Float.isNaN(best) ? pressure : Math.min(best, pressure);
            }
        return best;
    }

    private float tubeAt(BlockPos jar, Direction side) {
        BlockPos at = jar.relative(side);
        TubeTransportBehaviour tube = TubeTransportBehaviour.get(level, at);
        if (tube == null || !PneumaticTubeBlock.isOpenAt(level.getBlockState(at), side.getOpposite()))
            return Float.NaN;
        return tube.get(side.getOpposite()).pressure();
    }

    @Nullable
    private VacuumRecipe recipe() {
        if (cycle.recipe == null)
            return null;
        if (current == null || !current.id().equals(cycle.recipe))
            current = level.getRecipeManager().byKey(cycle.recipe)
                    .filter(holder -> holder.value() instanceof VacuumRecipe)
                    .map(holder -> {
                        @SuppressWarnings("unchecked")
                        RecipeHolder<VacuumRecipe> vacuum = (RecipeHolder<VacuumRecipe>) holder;
                        return vacuum;
                    }).orElse(null);
        return current == null ? null : current.value();
    }

    @Nullable
    private IItemHandler items() {
        return level.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.below(), Direction.UP);
    }

    @Nullable
    private IFluidHandler fluids() {
        return level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.below(), Direction.UP);
    }

    private BlazeBurnerBlock.HeatLevel heat() {
        return BasinBlockEntity.getHeatLevelOf(level.getBlockState(worldPosition.below(2)));
    }

    private void tryStart() {
        IItemHandler items = items();
        IFluidHandler fluids = fluids();
        if (items == null && fluids == null)
            return;
        List<ItemStack> stacks = new ArrayList<>();
        if (items != null)
            for (int slot = 0; slot < items.getSlots(); slot++)
                stacks.add(items.getStackInSlot(slot));
        List<FluidStack> tanks = new ArrayList<>();
        if (fluids != null)
            for (int tank = 0; tank < fluids.getTanks(); tank++)
                tanks.add(fluids.getFluidInTank(tank));
        RecipeHolder<VacuumRecipe> holder = VacuumRecipe.find(level, stacks, tanks, heat());
        if (holder == null)
            return;

        List<ItemStack> taken = new ArrayList<>();
        for (Ingredient ingredient : holder.value().getIngredients())
            taken.add(extract(items, ingredient));
        List<FluidStack> drained = new ArrayList<>();
        for (SizedFluidIngredient ingredient : holder.value().getFluidIngredients())
            drained.add(drain(fluids, ingredient));
        current = holder;
        cycle.start(holder, taken, drained);
        level.playSound(null, worldPosition, Pneumatics.JAR_SEAL.get(), SoundSource.BLOCKS, 0.7f, 1);
    }

    private static ItemStack extract(@Nullable IItemHandler items, Ingredient ingredient) {
        if (items != null)
            for (int slot = 0; slot < items.getSlots(); slot++)
                if (ingredient.test(items.getStackInSlot(slot)))
                    return items.extractItem(slot, 1, false);
        return ItemStack.EMPTY;
    }

    private static FluidStack drain(@Nullable IFluidHandler fluids, SizedFluidIngredient ingredient) {
        if (fluids != null)
            for (int tank = 0; tank < fluids.getTanks(); tank++) {
                FluidStack in = fluids.getFluidInTank(tank);
                if (ingredient.test(in))
                    return fluids.drain(in.copyWithAmount(ingredient.amount()), IFluidHandler.FluidAction.EXECUTE);
            }
        return FluidStack.EMPTY;
    }

    // outputs go back into the surface as fast as it takes them; whatever doesn't fit waits
    private void unload() {
        IItemHandler items = items();
        IFluidHandler fluids = fluids();
        for (Iterator<ItemStack> it = cycle.outputs.iterator(); it.hasNext();) {
            ItemStack stack = it.next();
            ItemStack left = stack;
            if (items != null)
                for (int slot = 0; slot < items.getSlots() && !left.isEmpty(); slot++)
                    left = items.insertItem(slot, left, false);
            if (left.isEmpty())
                it.remove();
            else
                stack.setCount(left.getCount());
        }
        for (Iterator<FluidStack> it = cycle.fluidOutputs.iterator(); it.hasNext();) {
            FluidStack stack = it.next();
            int filled = fluids == null ? 0 : fluids.fill(stack, IFluidHandler.FluidAction.EXECUTE);
            stack.shrink(filled);
            if (stack.isEmpty())
                it.remove();
        }
        cycle.emptied();
    }

    void vent() {
        if (cycle.pressure == 0 && !cycle.busy())
            return;
        cycle.vent(recipe(), level.random);
        level.playSound(null, worldPosition, Pneumatics.JAR_VENT.get(), SoundSource.BLOCKS, 0.7f, 1);
        sendData();
        setChanged();
    }

    void dropContents() {
        for (ItemStack stack : cycle.batch)
            Containers.dropItemStack(level, worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5, stack);
        for (ItemStack stack : cycle.outputs)
            Containers.dropItemStack(level, worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5, stack);
        // fluids can't lie on the ground, so they go back into the basin if it'll take them
        IFluidHandler fluids = fluids();
        if (fluids != null) {
            for (FluidStack stack : cycle.fluidBatch)
                fluids.fill(stack, IFluidHandler.FluidAction.EXECUTE);
            for (FluidStack stack : cycle.fluidOutputs)
                fluids.fill(stack, IFluidHandler.FluidAction.EXECUTE);
        }
        cycle = new BellJarCycle();
    }

    // the copy a contraption draws isn't ticked, so the needle goes straight to the reading
    void moved(BellJarCycle cycle) {
        this.cycle = cycle;
        needle = previousNeedle = cycle.pressure;
    }

    public float needle(float partialTicks) {
        return Mth.lerp(partialTicks, previousNeedle, needle);
    }

    public BellJarCycle cycle() {
        return cycle;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    ").append(Component.translatable("block.createpneumatics.bell_jar")));
        tooltip.add(Component.literal("    ").append(Component.translatable("createpneumatics.gauge.reading", Math.round(cycle.pressure * 100))
                .withStyle(ChatFormatting.AQUA)));
        tooltip.add(Component.literal("    ").append(Component.translatable("createpneumatics.jar." + cycle.phase.name().toLowerCase())
                .withStyle(ChatFormatting.GRAY)));
        return true;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Cycle", cycle.write(registries));
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        cycle = BellJarCycle.read(tag.getCompound("Cycle"), registries);
    }
}
