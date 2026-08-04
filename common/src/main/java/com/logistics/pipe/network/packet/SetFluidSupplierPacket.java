package com.logistics.pipe.network.packet;

import com.logistics.LogisticsPipe;
import com.logistics.pipe.ui.FluidSupplierScreenHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Packet sent from client to server to configure the Fluid Supplier Pipe: set the target amount (mB)
 * to keep stocked, or clear the fluid filter ({@code clearFluid}). Routed through the player's open
 * {@link FluidSupplierScreenHandler}, which owns the pipe reference and applies the change to the module.
 */
public record SetFluidSupplierPacket(int targetMb, boolean clearFluid) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SetFluidSupplierPacket> TYPE =
            new CustomPacketPayload.Type<>(LogisticsPipe.resource("set_fluid_supplier").toIdentifier());

    public static final StreamCodec<RegistryFriendlyByteBuf, SetFluidSupplierPacket> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    SetFluidSupplierPacket::targetMb,
                    ByteBufCodecs.BOOL,
                    SetFluidSupplierPacket::clearFluid,
                    SetFluidSupplierPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayer player) {
        if (player.containerMenu instanceof FluidSupplierScreenHandler menu) {
            menu.applyFromClient(player, targetMb, clearFluid);
        }
    }
}
