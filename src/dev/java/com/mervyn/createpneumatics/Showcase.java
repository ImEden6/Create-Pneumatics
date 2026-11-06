package com.mervyn.createpneumatics;

import java.util.List;
import java.util.Map;

import com.mervyn.createpneumatics.compat.PneumaticsJei;


import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

// Dev only, switched on by the createpneumatics.showcase system property that the clientShowcase
// run sets. Once in the world it builds the showcase, flies the camera through fixed views,
// saves a screenshot at each into run/screenshots/showcase_*.png and quits.
@EventBusSubscriber(modid = CreatePneumatics.MODID, value = Dist.CLIENT)
final class Showcase {
    static final boolean ENABLED = Boolean.getBoolean("createpneumatics.showcase");

    @SubscribeEvent
    static void clientSetup(FMLClientSetupEvent event) {
        if (ENABLED)
            NeoForge.EVENT_BUS.addListener(Showcase::tick);
    }

    // eye position and target, in blocks: ahead of the starting spot, to its right, and up.
    // move is false for a follow-up shot from the same spot.
    private record View(String name, int at, boolean move, double ahead, double right, double up, double lookAhead, double lookRight, double lookUp) {}

    private static View view(String name, int at, double ahead, double right, double up, double lookAhead, double lookRight, double lookUp) {
        return new View(name, at, true, ahead, right, up, lookAhead, lookRight, lookUp);
    }

    private static View again(String name, int at) {
        return new View(name, at, false, 0, 0, 0, 0, 0, 0);
    }

    private static final int SETUP = 60;
    // PNEUMATICS_SHOWCASE picks which machine the camera visits; everything gets built either way
    private static final Map<String, List<View>> SETS = Map.of(
            "pneumatics", List.of(
                    view("pneumatics_side", 200, -26, 6, 3, -26, 0, 1.5),
                    view("pneumatics_angle", 280, -20.5, 4, 3.5, -26, 0, 1.2),
                    view("pneumatics_blower", 360, -24.8, 1.6, 4.2, -26, 0, 1.4),
                    view("pneumatics_pull", 440, -31, 3, 4, -28.5, 0, 2),
                    view("pneumatics_jar", 520, -28.2, 3.2, 2.4, -29, 1, 0.6),
                    view("pneumatics_jar_late", 700, -28.2, 3.2, 2.4, -29, 1, 0.6)),
            "padtest", List.of(
                    view("padtest_moving", 142, -36, 4, 5, -42, 4, 2),
                    again("padtest_after", 320),
                    view("padtest_leak", 380, -39, 30, 5, -44, 28, 3)),
            "transit", List.of(
                    view("transit_close", 141, -58.5, -2.5, 3, -60.5, 0.5, 1.5),
                    again("transit_close_late", 147),
                    view("transit_moving", 210, -55, 9, 7, -62, 1, 1),
                    again("transit_after", 300)),
            "jei", List.of(again("jei_vacuum", 200)),
            "jartest", List.of(
                    view("jartest_turning", 160, -22, 10, 6, -28.5, 11, 1.5),
                    again("jartest_after", 340)));
    private static final String SET = System.getenv().getOrDefault("PNEUMATICS_SHOWCASE", "pneumatics");
    private static final List<View> VIEWS = SETS.getOrDefault(SET, SETS.get("pneumatics"));
    private static final int QUIT = VIEWS.get(VIEWS.size() - 1).at + 100;

    private static int tick;
    private static BlockPos anchor;
    private static Direction ahead;

    private Showcase() {}

    static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        IntegratedServer server = mc.getSingleplayerServer();
        if (mc.player == null || mc.level == null || server == null)
            return;
        if (SET.equals("sounds")) {
            SoundTour.tick(mc);
            return;
        }
        if (SET.equals("ponder")) {
            PonderTour.tick(mc);
            return;
        }
        tick++;
        if (tick == SETUP) {
            mc.options.hideGui = true;
            anchor = mc.player.blockPosition();
            ahead = mc.player.getDirection();
            server.execute(() -> {
                PneumaticCommands.showcase(player(server), anchor.relative(ahead, -26).above(), ahead);
                TransitTests.build(player(server), anchor.relative(ahead, -64).above(), ahead);
                PadTests.build(player(server), anchor.relative(ahead, -44).above(2), ahead);
                JarTests.build(player(server), anchor.relative(ahead, -26).above().relative(ahead.getClockWise(), 8), ahead);
            });
        }
        if (anchor == null)
            return;
        int t = tick - SETUP;
        server.execute(() -> JarTests.step(player(server), t));
        server.execute(() -> PadTests.step(player(server), t));
        server.execute(() -> TransitTests.step(player(server), t));
        for (View view : VIEWS) {
            if (tick == view.at - 50 && view.move)
                server.execute(() -> moveTo(player(server), view));
            if (tick == view.at - 30 && view.name.startsWith("jei_"))
                PneumaticsJei.showVacuum();
            if (tick == view.at)
                Screenshot.grab(mc.gameDirectory, "showcase_" + view.name + ".png", mc.getMainRenderTarget(), message -> {});
        }
        if (tick == QUIT)
            mc.stop();
    }

    private static ServerPlayer player(IntegratedServer server) {
        return server.getPlayerList().getPlayers().get(0);
    }

    private static Vec3 point(double a, double r, double u) {
        Direction right = ahead.getClockWise();
        return Vec3.atBottomCenterOf(anchor).add(ahead.getStepX() * a + right.getStepX() * r, u, ahead.getStepZ() * a + right.getStepZ() * r);
    }

    private static void moveTo(ServerPlayer player, View view) {
        Vec3 eye = point(view.ahead, view.right, view.up), look = point(view.lookAhead, view.lookRight, view.lookUp);
        Vec3 d = look.subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.teleportTo(player.serverLevel(), eye.x, eye.y - player.getEyeHeight(), eye.z, yaw, pitch);
    }
}
