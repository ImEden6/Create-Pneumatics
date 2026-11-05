package com.mervyn.createpneumatics.data;

import com.mervyn.createpneumatics.CreatePneumatics;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = CreatePneumatics.MODID)
public final class PneumaticsData {
    private PneumaticsData() {}

    @SubscribeEvent
    static void gather(GatherDataEvent event) {
        var output = event.getGenerator().getPackOutput();
        event.getGenerator().addProvider(event.includeServer(), new VacuumRecipes(output, event.getLookupProvider()));
        event.getGenerator().addProvider(event.includeServer(), new CraftingRecipes(output, event.getLookupProvider()));
    }
}
