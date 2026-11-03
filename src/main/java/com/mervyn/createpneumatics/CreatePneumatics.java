package com.mervyn.createpneumatics;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(CreatePneumatics.MODID)
public class CreatePneumatics {
    public static final String MODID = "createpneumatics";
    public static final Logger LOGGER = LogUtils.getLogger();

    static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("pneumatics", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.createpneumatics"))
            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
            .icon(() -> Pneumatics.BLOWER_ITEM.get().getDefaultInstance())
            .build());

    public CreatePneumatics(IEventBus modEventBus, ModContainer modContainer) {
        TABS.register(modEventBus);
        Pneumatics.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(PneumaticPropagator::flush);
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> event.getDispatcher()
                .register(Commands.literal(MODID).requires(source -> source.hasPermission(2)).then(PneumaticCommands.node())));
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
