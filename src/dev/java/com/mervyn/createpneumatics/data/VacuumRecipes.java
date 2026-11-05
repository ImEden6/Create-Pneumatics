package com.mervyn.createpneumatics.data;

import java.util.concurrent.CompletableFuture;

import com.mervyn.createpneumatics.CreatePneumatics;
import com.mervyn.createpneumatics.Pneumatics;
import com.mervyn.createpneumatics.VacuumRecipe;
import com.simibubi.create.AllItems;
import com.simibubi.create.Create;
import com.simibubi.create.api.data.recipe.StandardProcessingRecipeGen;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.foundation.data.recipe.CommonMetal;
import com.simibubi.create.foundation.data.recipe.Mods;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.Tags;

public class VacuumRecipes extends StandardProcessingRecipeGen<VacuumRecipe> {
    private static final int BATCH = 4;

    public VacuumRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, CreatePneumatics.MODID);

        create("ration_pack_from_stew", b -> b.require(Items.MUSHROOM_STEW).output(Pneumatics.RATION_PACK.get()).output(Items.BOWL).duration(200));
        create("ration_pack_from_beetroot", b -> b.require(Items.BEETROOT_SOUP).output(Pneumatics.RATION_PACK.get()).output(Items.BOWL).duration(200));
        create("ration_pack_from_rabbit", b -> b.require(Items.RABBIT_STEW).output(Pneumatics.RATION_PACK.get(), 2).output(Items.BOWL).duration(240));
        create("dry_sponge", b -> b.require(Items.WET_SPONGE).output(Items.SPONGE).output(Fluids.WATER, 250).duration(100));
        create("optical_glass", b -> b.require(Tags.Items.GLASS_BLOCKS_COLORLESS)
                .output(Pneumatics.OPTICAL_GLASS.get()).output(.25f, Pneumatics.OPTICAL_GLASS.get(), 1)
                .duration(300).requiresHeat(HeatCondition.HEATED));

        // washing gets 1 ingot per crushed ore plus a byproduct; four at a time in the jar gets 1.375 heated, 1.625 superheated
        sinter("iron", AllItems.CRUSHED_IRON.get(), Items.IRON_INGOT, Items.REDSTONE, .75f);
        sinter("gold", AllItems.CRUSHED_GOLD.get(), Items.GOLD_INGOT, Items.QUARTZ, .5f);
        sinter("copper", AllItems.CRUSHED_COPPER.get(), Items.COPPER_INGOT, Items.CLAY_BALL, .5f);
        sinter("zinc", AllItems.CRUSHED_ZINC.get(), AllItems.ZINC_INGOT.get(), Items.GUNPOWDER, .25f);

        for (CommonMetal metal : CommonMetal.values()) {
            Item crushed = BuiltInRegistries.ITEM.get(Create.asResource("crushed_raw_" + metal.name));
            if (crushed == Items.AIR)
                continue;
            for (Mods mod : metal.mods) {
                if (mod == Mods.VANILLA || mod == Mods.CREATE)
                    continue;
                ResourceLocation ingot = mod.ingotOf(metal.getName(mod));
                for (Tier tier : Tier.values())
                    create(mod.getId() + "/" + tier.prefix + metal.name, b -> tier.apply(batch(b, crushed))
                            .output(1, ingot, tier.ingots).output(.5f, ingot, 1).whenModLoaded(mod.getId()));
            }
        }
    }

    private void sinter(String name, ItemLike crushed, ItemLike ingot, ItemLike byproduct, float odds) {
        for (Tier tier : Tier.values())
            create(tier.prefix + name, b -> tier.apply(batch(b, crushed))
                    .output(ingot, tier.ingots).output(.5f, ingot, 1).output(odds, byproduct, 2));
    }

    private static StandardProcessingRecipe.Builder<VacuumRecipe> batch(StandardProcessingRecipe.Builder<VacuumRecipe> b, ItemLike crushed) {
        for (int i = 0; i < BATCH; i++)
            b.require(crushed);
        return b;
    }

    private enum Tier {
        HEATED("sintered_", 5, 200, HeatCondition.HEATED),
        SUPERHEATED("hot_sintered_", 6, 160, HeatCondition.SUPERHEATED);

        final String prefix;
        final int ingots, time;
        final HeatCondition heat;

        Tier(String prefix, int ingots, int time, HeatCondition heat) {
            this.prefix = prefix;
            this.ingots = ingots;
            this.time = time;
            this.heat = heat;
        }

        StandardProcessingRecipe.Builder<VacuumRecipe> apply(StandardProcessingRecipe.Builder<VacuumRecipe> b) {
            return b.duration(time).requiresHeat(heat);
        }
    }

    @Override
    protected IRecipeTypeInfo getRecipeType() {
        return Pneumatics.VACUUM_TYPE;
    }
}
