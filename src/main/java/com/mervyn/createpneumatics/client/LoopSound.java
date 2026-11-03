package com.mervyn.createpneumatics.client;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;

// A blower's hum or a vent's hiss. The block entity calls keep() every client tick while it runs;
// once the calls stop the loop fades out over a few ticks.
public class LoopSound extends AbstractTickableSoundInstance {
    private static final float LOUDEST = .5f;
    private static final Map<BlockEntity, LoopSound> PLAYING = new HashMap<>();

    private final BlockEntity be;
    private float target;
    private int quiet;

    private LoopSound(SoundEvent event, BlockEntity be) {
        super(event, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.be = be;
        BlockPos pos = be.getBlockPos();
        x = pos.getX() + .5;
        y = pos.getY() + .5;
        z = pos.getZ() + .5;
        looping = true;
        delay = 0;
        volume = 0.01f;
    }

    public static void keep(BlockEntity be, SoundEvent event, float loudness, float pitch) {
        Minecraft mc = Minecraft.getInstance();
        if (be.getLevel() != mc.level)
            return;
        LoopSound sound = PLAYING.get(be);
        if (sound == null || !mc.getSoundManager().isActive(sound)) {
            PLAYING.values().removeIf(s -> !mc.getSoundManager().isActive(s));
            sound = new LoopSound(event, be);
            PLAYING.put(be, sound);
            mc.getSoundManager().play(sound);
        }
        sound.quiet = 0;
        sound.target = LOUDEST * Math.min(1, loudness);
        sound.pitch = pitch;
    }

    @Override
    public void tick() {
        if (be.isRemoved() || ++quiet > 2) {
            volume -= LOUDEST / 4;
            if (volume <= 0)
                stop();
            return;
        }
        volume += Math.signum(target - volume) * Math.min(Math.abs(target - volume), LOUDEST / 4);
    }
}
