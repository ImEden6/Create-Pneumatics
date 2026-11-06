package com.mervyn.createpneumatics.client;

import com.mervyn.createpneumatics.CreatePneumatics;
import com.mervyn.createpneumatics.Pneumatics;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;

import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.createmod.catnip.lang.FontHelper;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = CreatePneumatics.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = CreatePneumatics.MODID, value = Dist.CLIENT)
public class PneumaticsClient {
    public PneumaticsClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        PneumaticPartials.init();
        PonderIndex.addPlugin(new PneumaticsPonder());
    }

    @SubscribeEvent
    static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> SimpleBlockEntityVisualizer.builder(Pneumatics.BLOWER_BE.get())
                .factory(BlowerVisual::new)
                .skipVanillaRender(be -> true)
                .apply());
        event.enqueueWork(PneumaticsClient::shiftTooltips);
    }

    private static void shiftTooltips() {
        for (Item item : BuiltInRegistries.ITEM)
            if (BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(CreatePneumatics.MODID))
                TooltipModifier.REGISTRY.register(item, new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                        .andThen(TooltipModifier.mapNull(KineticStats.create(item))));
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Pneumatics.BLOWER_BE.get(), BlowerRenderer::new);
        event.registerBlockEntityRenderer(Pneumatics.GAUGE_BE.get(), GaugeRenderer::new);
        event.registerBlockEntityRenderer(Pneumatics.JAR_BE.get(), BellJarRenderer::new);
        event.registerBlockEntityRenderer(Pneumatics.DIVERTER_BE.get(), SmartBlockEntityRenderer::new);
        event.registerEntityRenderer(Pneumatics.PAYLOAD.get(), PayloadRenderer::new);
    }
}
