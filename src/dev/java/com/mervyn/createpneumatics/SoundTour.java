package com.mervyn.createpneumatics;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

// PNEUMATICS_SHOWCASE=sounds: plays every mod sound once, one after another, with its name on the
// action bar, then quits. The two loops play twice back to back so the seam can be heard.
final class SoundTour {
    private static final int START = 60;
    private static final int GAP = 50;
    private static final List<DeferredHolder<SoundEvent, SoundEvent>> LOOPS = List.of(Pneumatics.BLOWER_RUNNING, Pneumatics.TUBE_VENT);

    private static List<DeferredHolder<SoundEvent, SoundEvent>> sounds;
    private static int tick, index, next = START;

    private SoundTour() {}

    static void tick(Minecraft mc) {
        tick++;
        if (sounds == null)
            sounds = Pneumatics.SOUNDS.getEntries().stream()
                    .map(holder -> {
                        @SuppressWarnings("unchecked")
                        DeferredHolder<SoundEvent, SoundEvent> sound = (DeferredHolder<SoundEvent, SoundEvent>) holder;
                        return sound;
                    }).toList();
        if (tick < next)
            return;
        if (index == sounds.size()) {
            mc.stop();
            return;
        }
        DeferredHolder<SoundEvent, SoundEvent> sound = sounds.get(index++);
        String name = sound.getId().getPath();
        mc.gui.setOverlayMessage(Component.literal(index + "/" + sounds.size() + "  " + name + "  ").withStyle(ChatFormatting.GOLD)
                .append(Component.translatable("subtitles.createpneumatics." + name).withStyle(ChatFormatting.WHITE)), false);
        boolean loop = LOOPS.contains(sound);
        // loops play at the volume LoopSound gives them in the world, not full blast
        float volume = loop ? .5f : 1;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound.get(), 1, volume));
        if (loop)
            mc.getSoundManager().playDelayed(SimpleSoundInstance.forUI(sound.get(), 1, volume), 40);
        next = tick + (loop ? 40 + GAP : GAP);
        CreatePneumatics.LOGGER.info("SOUNDTOUR {}/{} {}", index, sounds.size(), name);
    }
}
