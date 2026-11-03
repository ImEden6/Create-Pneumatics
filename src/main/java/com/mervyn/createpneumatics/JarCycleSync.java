package com.mervyn.createpneumatics;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// a jar riding a contraption moved its needle or changed phase; the client's copy of the jar
// never hears about it any other way
public record JarCycleSync(int entity, BlockPos jar, CompoundTag cycle) implements CustomPacketPayload {
    public static final Type<JarCycleSync> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreatePneumatics.MODID, "jar_cycle"));
    public static final StreamCodec<ByteBuf, JarCycleSync> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, JarCycleSync::entity,
            BlockPos.STREAM_CODEC, JarCycleSync::jar,
            ByteBufCodecs.COMPOUND_TAG, JarCycleSync::cycle,
            JarCycleSync::new);

    @Override
    public Type<JarCycleSync> type() {
        return TYPE;
    }

    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, STREAM_CODEC, JarCycleSync::handle);
    }

    private static void handle(JarCycleSync sync, IPayloadContext ctx) {
        if (ctx.player().level().getEntity(sync.entity) instanceof AbstractContraptionEntity entity
                && entity.getContraption().getBlockEntityClientSide(sync.jar) instanceof BellJarBlockEntity jar)
            jar.moved(BellJarCycle.read(sync.cycle, ctx.player().registryAccess()));
    }
}
