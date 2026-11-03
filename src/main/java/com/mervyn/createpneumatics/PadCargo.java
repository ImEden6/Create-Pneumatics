package com.mervyn.createpneumatics;

import org.apache.commons.lang3.tuple.MutablePair;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// A pad on a moving contraption picked something up or let it go; clients draw the new cargo, or
// none when the state is empty
public record PadCargo(int entity, BlockPos pad, CompoundTag state) implements CustomPacketPayload {
    public static final Type<PadCargo> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreatePneumatics.MODID, "pad_cargo"));
    public static final StreamCodec<ByteBuf, PadCargo> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PadCargo::entity,
            BlockPos.STREAM_CODEC, PadCargo::pad,
            ByteBufCodecs.COMPOUND_TAG, PadCargo::state,
            PadCargo::new);

    @Override
    public Type<PadCargo> type() {
        return TYPE;
    }

    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, STREAM_CODEC, PadCargo::handle);
    }

    private static void handle(PadCargo release, IPayloadContext ctx) {
        if (!(ctx.player().level().getEntity(release.entity) instanceof AbstractContraptionEntity entity))
            return;
        for (MutablePair<StructureBlockInfo, MovementContext> actor : entity.getContraption().getActors())
            if (actor.left.pos().equals(release.pad) && actor.right != null) {
                if (release.state.isEmpty())
                    actor.right.data.remove("State");
                else
                    actor.right.data.put("State", release.state);
            }
    }
}
