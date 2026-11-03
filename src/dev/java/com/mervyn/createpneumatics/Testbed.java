package com.mervyn.createpneumatics;

import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

// Dev only: /createpneumatics testbed builds every pneumatics rig in front of you, motors stopped, so
// each one is started by scrolling its creative motor
@EventBusSubscriber(modid = CreatePneumatics.MODID)
public final class Testbed {
    private Testbed() {}

    @SubscribeEvent
    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(CreatePneumatics.MODID).then(Commands.literal("testbed")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    build(player);
                    ctx.getSource().sendSuccess(() -> Component.literal("pneumatics testbed built, scroll a creative motor to start its rig"), false);
                    return 1;
                })));
    }

    // same layout as the showcase, turned round so it lands in front instead of behind
    static void build(ServerPlayer player) {
        BlockPos anchor = player.blockPosition();
        Direction ahead = player.getDirection().getOpposite();
        PneumaticCommands.showcase(player, anchor.relative(ahead, -26).above(), ahead);
        JarTests.build(player, anchor.relative(ahead, -26).above().relative(ahead.getClockWise(), 8), ahead);
        PadTests.build(player, anchor.relative(ahead, -44).above(2), ahead);
        PadTests.motors(player.serverLevel(), false);
        TransitTests.build(player, anchor.relative(ahead, -64).above(), ahead);
    }
}
