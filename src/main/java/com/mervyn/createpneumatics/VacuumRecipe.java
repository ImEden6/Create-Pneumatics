package com.mervyn.createpneumatics;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

// createpneumatics:vacuum_processing. Matched by hand against whatever sits under the jar, not
// through RecipeManager.getRecipeFor, since the input is a loose pile of items and fluids.
public class VacuumRecipe extends StandardProcessingRecipe<RecipeInput> {
    public VacuumRecipe(ProcessingRecipeParams params) {
        super(Pneumatics.VACUUM_TYPE, params);
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return false;
    }

    @Override
    protected int getMaxInputCount() {
        return 9;
    }

    @Override
    protected int getMaxOutputCount() {
        return 4;
    }

    @Override
    protected int getMaxFluidInputCount() {
        return 2;
    }

    @Override
    protected int getMaxFluidOutputCount() {
        return 2;
    }

    @Override
    protected boolean canRequireHeat() {
        return true;
    }

    @Override
    protected boolean canSpecifyDuration() {
        return true;
    }

    // one ingredient per item taken, the way basin recipes count them
    public boolean fits(List<ItemStack> items, List<FluidStack> fluids, BlazeBurnerBlock.HeatLevel heat) {
        if (!requiredHeat.testBlazeBurner(heat))
            return false;
        List<ItemStack> left = new ArrayList<>();
        for (ItemStack stack : items)
            left.add(stack.copy());
        for (Ingredient ingredient : ingredients)
            if (!take(left, ingredient))
                return false;
        for (SizedFluidIngredient fluid : fluidIngredients)
            if (fluids.stream().noneMatch(stack -> fluid.test(stack)))
                return false;
        return true;
    }

    private static boolean take(List<ItemStack> left, Ingredient ingredient) {
        for (ItemStack stack : left)
            if (!stack.isEmpty() && ingredient.test(stack)) {
                stack.shrink(1);
                return true;
            }
        return false;
    }

    public List<ProcessingOutput> guaranteed() {
        return results.stream().filter(out -> out.getChance() >= 1).toList();
    }

    @Nullable
    public static RecipeHolder<VacuumRecipe> find(Level level, List<ItemStack> items, List<FluidStack> fluids, BlazeBurnerBlock.HeatLevel heat) {
        for (RecipeHolder<VacuumRecipe> holder : level.getRecipeManager().getAllRecipesFor(Pneumatics.VACUUM_TYPE.<RecipeInput, VacuumRecipe>getType()))
            if (holder.value().fits(items, fluids, heat))
                return holder;
        return null;
    }
}
