package com.mervyn.createpneumatics.compat;

import java.util.List;

import com.mervyn.createpneumatics.CreatePneumatics;
import com.mervyn.createpneumatics.Pneumatics;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

// JEI only loads this when it's installed, and EMI reads JEI plugins, so nothing else references it
@JeiPlugin
public class PneumaticsJei implements IModPlugin {
    private static IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(CreatePneumatics.MODID, "jei");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    public static void showVacuum() {
        if (runtime != null)
            runtime.getRecipesGui().showTypes(List.of(VacuumCategory.TYPE));
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new VacuumCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(VacuumCategory.TYPE, Minecraft.getInstance().level.getRecipeManager()
                .getAllRecipesFor(Pneumatics.VACUUM_RECIPE.get()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(VacuumCategory.TYPE, Pneumatics.JAR_ITEM.get(), Pneumatics.BLOWER_ITEM.get());
    }
}
