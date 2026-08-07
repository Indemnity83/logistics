package com.logistics.pipe.network.packet;

import com.logistics.LogisticsPipe;
import com.logistics.pipe.ui.FluidSupplierScreenHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Packet sent from client to server to configure the Fluid Supplier Pipe: the target amount (mB) to
 * keep stocked, and the two mode toggles (fulfillment mode, minimum-deficit threshold). The fluid
 * filter itself is set or cleared separately, by clicking the fluid gauge (see
 * {@link ClickFluidSupplierGaugePacket}). The client always sends the full desired state — all three
 * fields are re-applied idempotently on every edit. Routed through the player's open
 * {@link FluidSupplierScreenHandler}, which owns the pipe reference and applies the change to the module.
 */
public record SetFluidSupplierPacket(int targetMb, int fulfillmentModeOrdinal, int minThresholdOrdinal)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SetFluidSupplierPacket> TYPE =
            new CustomPacketPayload.Type<>(LogisticsPipe.resource("set_fluid_supplier").toIdentifier());

    public static final StreamCodec<RegistryFriendlyByteBuf, SetFluidSupplierPacket> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    SetFluidSupplierPacket::targetMb,
                    ByteBufCodecs.VAR_INT,
                    SetFluidSupplierPacket::fulfillmentModeOrdinal,
                    ByteBufCodecs.VAR_INT,
                    SetFluidSupplierPacket::minThresholdOrdinal,
                    SetFluidSupplierPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayer player) {
        if (player.containerMenu instanceof FluidSupplierScreenHandler menu) {
            menu.applyFromClient(player, targetMb, fulfillmentModeOrdinal, minThresholdOrdinal);
        }
    }
}
