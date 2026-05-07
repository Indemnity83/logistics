package com.logistics.pipe.network.packet;

import com.logistics.LogisticsPipe;
import com.logistics.pipe.ChassisPipe;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.ui.ChassisScreenHandler;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Sent client→server when the player clicks a chassis slot's "!" button.
 * The server looks up the player's currently-open {@link ChassisScreenHandler},
 * reads the module item from the requested slot, and calls its
 * {@code onWrench} handler to open its config screen.
 */
public record OpenChassisSlotPacket(int slotIndex) implements CustomPacketPayload {
    public static final Type<OpenChassisSlotPacket> TYPE =
            new Type<>(LogisticsPipe.resource("open_chassis_slot").toIdentifier());

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenChassisSlotPacket> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    OpenChassisSlotPacket::slotIndex,
                    OpenChassisSlotPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayer player) {
        if (!(player.containerMenu instanceof ChassisScreenHandler handler)) return;

        PipeBlockEntity pe = handler.getPipeEntity();
        if (pe == null) return;

        var ctx = pe.createContext();
        var ops = player.level().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        ChassisPipe.loadSlot(ctx, slotIndex(), ops)
                .ifPresent(entry -> entry.module().onWrench(entry.scopedContext(ctx), player));
    }
}
