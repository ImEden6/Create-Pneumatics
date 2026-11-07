package com.mervyn.createpneumatics;

import java.util.Arrays;
import java.util.List;

import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.resources.ResourceLocation;

// PNEUMATICS_SHOWCASE=ponder: opens the ponder for one block per scene, skips to a few points along it and
// saves run/screenshots/ponder_<scene>_<n>.png, then quits.
final class PonderTour {
    private static final List<String> BLOCKS = List.of("roots_blower", "relief_valve", "bell_jar", "pneumatic_terminal", "vacuum_pad");
    private static final float[] POINTS = {.2f, .45f, .7f, .95f};
    private static final int START = 60;
    private static final int STEP = 30;
    private static final String ONLY = System.getenv().getOrDefault("PNEUMATICS_ONLY", "");
    // PNEUMATICS_TICKS=134,138 shoots those scene ticks instead of the fractions above
    private static final String TICKS = System.getenv().getOrDefault("PNEUMATICS_TICKS", "");
    private static final int[] AT = TICKS.isEmpty() ? null : Arrays.stream(TICKS.split(",")).mapToInt(Integer::parseInt).toArray();

    private static final List<String> TODO = BLOCKS.stream().filter(s -> s.contains(ONLY)).toList();
    private static int tick, scene, point = -1;
    private static PonderUI ui;

    private PonderTour() {}

    static void tick(Minecraft mc) {
        tick++;
        if (tick < START || (tick - START) % STEP != 0)
            return;
        if (scene == TODO.size()) {
            mc.stop();
            return;
        }
        String name = TODO.get(scene);
        if (point == -1) {
            ui = PonderUI.of(ResourceLocation.fromNamespaceAndPath(CreatePneumatics.MODID, name));
            ScreenOpener.open(ui);
            point = 0;
            return;
        }
        if (point > 0)
            Screenshot.grab(mc.gameDirectory, "ponder_" + ui.getActiveScene().getId().getPath() + "_" + point + ".png", mc.getMainRenderTarget(), message -> {});
        if (point == (AT == null ? POINTS.length : AT.length)) {
            scene++;
            point = -1;
            return;
        }
        ui.seekToTime(AT == null ? (int) (ui.getActiveScene().getTotalTime() * POINTS[point]) : AT[point]);
        point++;
    }
}
