package com.mervyn.createpneumatics;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.processing.recipe.ProcessingOutput;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidStack;

// The jar's whole state machine, with no level in it, so the block entity and the contraption
// actor run the exact same thing. The host hands it the supply each tick and does the I/O.
//
// Pressure runs 0 (open air) to -1 (hard vacuum) and chases whatever the tube offers at a rate
// set by the chamber volume, so a big jar visibly takes longer to pump down.
public class BellJarCycle {
    public enum Phase { IDLE, PUMPING, RUNNING, DONE }

    public static float threshold() {
        return -Config.JAR_VACUUM.get().floatValue();
    }

    // above this while running the vacuum counts as lost and the batch is spoilt
    private static final float LOST = -0.35f;
    // a sealed jar with nothing feeding it creeps back to air this much per tick
    private static final float SEEP = 0.0004f;

    public Phase phase = Phase.IDLE;
    public float pressure;
    public int timer;
    @Nullable
    public ResourceLocation recipe;
    public final List<ItemStack> batch = new ArrayList<>();
    public final List<FluidStack> fluidBatch = new ArrayList<>();
    public final List<ItemStack> outputs = new ArrayList<>();
    public final List<FluidStack> fluidOutputs = new ArrayList<>();

    // supply is the tube's pressure at the jar, or NaN when nothing is connected (moving, or capped)
    public void tickPressure(float supply, float volume) {
        if (Float.isNaN(supply)) {
            pressure = Math.min(0, pressure + SEEP);
            return;
        }
        float target = Math.min(0, supply);
        pressure += (target - pressure) * Config.JAR_PUMP_RATE.get().floatValue() / volume;
        if (Math.abs(pressure) < 0.001f)
            pressure = 0;
    }

    public void start(RecipeHolder<VacuumRecipe> holder, List<ItemStack> items, List<FluidStack> fluids) {
        recipe = holder.id();
        batch.clear();
        batch.addAll(items);
        fluidBatch.clear();
        fluidBatch.addAll(fluids);
        phase = Phase.PUMPING;
    }

    public void tick(@Nullable VacuumRecipe current, RandomSource random) {
        switch (phase) {
            case PUMPING -> {
                if (current == null) {
                    spoil();
                    return;
                }
                if (pressure <= threshold()) {
                    phase = Phase.RUNNING;
                    timer = Math.max(1, current.getProcessingDuration());
                }
            }
            case RUNNING -> {
                if (current == null || pressure > LOST) {
                    finish(current, random, false);
                    return;
                }
                if (--timer <= 0)
                    finish(current, random, true);
            }
            default -> {}
        }
    }

    // valve opened, jar opened or contraption taken apart: keep what's certain, lose the bonus
    public void vent(@Nullable VacuumRecipe current, RandomSource random) {
        if (phase == Phase.RUNNING)
            finish(current, random, false);
        else if (phase == Phase.PUMPING)
            spoil();
        pressure = 0;
    }

    private void finish(@Nullable VacuumRecipe current, RandomSource random, boolean clean) {
        if (current == null) {
            spoil();
            return;
        }
        List<ProcessingOutput> rolls = clean ? current.getRollableResults() : current.guaranteed();
        outputs.addAll(current.rollResults(rolls, random));
        if (clean)
            for (FluidStack fluid : current.getFluidResults())
                fluidOutputs.add(fluid.copy());
        batch.clear();
        fluidBatch.clear();
        recipe = null;
        phase = Phase.DONE;
    }

    // nothing was made, so the inputs come back out as they went in
    private void spoil() {
        outputs.addAll(batch);
        fluidOutputs.addAll(fluidBatch);
        batch.clear();
        fluidBatch.clear();
        recipe = null;
        phase = Phase.DONE;
    }

    public void emptied() {
        if (phase == Phase.DONE && outputs.isEmpty() && fluidOutputs.isEmpty())
            phase = Phase.IDLE;
    }

    public boolean busy() {
        return phase != Phase.IDLE;
    }

    public CompoundTag write(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Phase", phase.name());
        tag.putFloat("Pressure", pressure);
        tag.putInt("Timer", timer);
        if (recipe != null)
            tag.putString("Recipe", recipe.toString());
        tag.put("Batch", items(batch, registries));
        tag.put("Outputs", items(outputs, registries));
        tag.put("Fluids", fluids(fluidBatch, registries));
        tag.put("FluidOutputs", fluids(fluidOutputs, registries));
        return tag;
    }

    public static BellJarCycle read(CompoundTag tag, HolderLookup.Provider registries) {
        BellJarCycle cycle = new BellJarCycle();
        if (tag.isEmpty())
            return cycle;
        cycle.phase = Phase.valueOf(tag.getString("Phase"));
        cycle.pressure = tag.getFloat("Pressure");
        cycle.timer = tag.getInt("Timer");
        cycle.recipe = tag.contains("Recipe") ? ResourceLocation.tryParse(tag.getString("Recipe")) : null;
        readItems(tag.getList("Batch", Tag.TAG_COMPOUND), cycle.batch, registries);
        readItems(tag.getList("Outputs", Tag.TAG_COMPOUND), cycle.outputs, registries);
        readFluids(tag.getList("Fluids", Tag.TAG_COMPOUND), cycle.fluidBatch, registries);
        readFluids(tag.getList("FluidOutputs", Tag.TAG_COMPOUND), cycle.fluidOutputs, registries);
        return cycle;
    }

    private static ListTag items(List<ItemStack> stacks, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (ItemStack stack : stacks)
            if (!stack.isEmpty())
                list.add(stack.save(registries));
        return list;
    }

    private static ListTag fluids(List<FluidStack> stacks, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (FluidStack stack : stacks)
            if (!stack.isEmpty())
                list.add(stack.save(registries));
        return list;
    }

    private static void readItems(ListTag list, List<ItemStack> into, HolderLookup.Provider registries) {
        for (int i = 0; i < list.size(); i++)
            ItemStack.parse(registries, list.getCompound(i)).ifPresent(into::add);
    }

    private static void readFluids(ListTag list, List<FluidStack> into, HolderLookup.Provider registries) {
        for (int i = 0; i < list.size(); i++)
            FluidStack.parse(registries, list.getCompound(i)).ifPresent(into::add);
    }
}
