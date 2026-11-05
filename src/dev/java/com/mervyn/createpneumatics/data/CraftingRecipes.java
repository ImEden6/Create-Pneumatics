package com.mervyn.createpneumatics.data;

import java.util.concurrent.CompletableFuture;

import com.mervyn.createpneumatics.Pneumatics;
import com.simibubi.create.AllBlocks;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;

public class CraftingRecipes extends RecipeProvider {
    private static final TagKey<Item> BRASS_PLATE = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "plates/brass"));

    public CraftingRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput out) {
        ItemLike tube = Pneumatics.TUBE_ITEM.get(), casing = AllBlocks.BRASS_CASING.get();

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, tube, 4).pattern("PGP")
                .define('P', BRASS_PLATE).define('G', Tags.Items.GLASS_PANES_COLORLESS)
                .unlockedBy("has_brass_plate", has(BRASS_PLATE)).save(out);
        shaped(Pneumatics.GLASS_TUBE_ITEM.get(), 4).pattern("GPG")
                .define('P', BRASS_PLATE).define('G', Tags.Items.GLASS_BLOCKS_COLORLESS).save(out);
        shaped(Pneumatics.BLOWER_ITEM.get(), 1).pattern("PCP").pattern("TST").pattern("PCP")
                .define('P', BRASS_PLATE).define('C', casing).define('T', tube).define('S', AllBlocks.SHAFT.get()).save(out);
        shaped(Pneumatics.GAUGE_ITEM.get(), 1).pattern(" N ").pattern("PGP").pattern(" T ")
                .define('N', Tags.Items.NUGGETS_IRON).define('P', BRASS_PLATE).define('G', Tags.Items.GLASS_PANES_COLORLESS).define('T', tube).save(out);
        shaped(Pneumatics.VALVE_ITEM.get(), 1).pattern("R").pattern("T")
                .define('R', Items.LEVER).define('T', tube).save(out);
        shaped(Pneumatics.PAD_ITEM.get(), 1).pattern("T").pattern("P").pattern("K")
                .define('T', tube).define('P', BRASS_PLATE).define('K', Items.DRIED_KELP).save(out);
        shaped(Pneumatics.TANK_ITEM.get(), 1).pattern("PTP").pattern("PFP").pattern("PPP")
                .define('P', BRASS_PLATE).define('T', tube).define('F', AllBlocks.FLUID_TANK.get()).save(out);
        shaped(Pneumatics.INTERFACE_ITEM.get(), 1).pattern("PKP").pattern("CTC")
                .define('P', BRASS_PLATE).define('K', Items.DRIED_KELP).define('C', casing).define('T', tube).save(out);
        shaped(Pneumatics.TERMINAL_ITEM.get(), 1).pattern("PHP").pattern("T T").pattern("PCP")
                .define('P', BRASS_PLATE).define('H', Items.HOPPER).define('T', tube).define('C', casing).save(out);
        shaped(Pneumatics.DIVERTER_ITEM.get(), 1).pattern(" T ").pattern("TCT").pattern(" R ")
                .define('T', tube).define('C', casing).define('R', Tags.Items.DUSTS_REDSTONE).save(out);
        shaped(Pneumatics.JAR_ITEM.get(), 1).pattern("GGG").pattern("G G").pattern("PTP")
                .define('G', Tags.Items.GLASS_BLOCKS_COLORLESS).define('P', BRASS_PLATE).define('T', tube).save(out);
    }

    private static ShapedRecipeBuilder shaped(ItemLike result, int count) {
        return ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result, count).unlockedBy("has_tube", has(Pneumatics.TUBE_ITEM.get()));
    }
}
