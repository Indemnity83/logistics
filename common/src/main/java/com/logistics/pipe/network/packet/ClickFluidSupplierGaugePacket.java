package com.logistics.pipe.network.packet;

import com.logistics.LogisticsPipe;
import com.logistics.pipe.ui.FluidSupplierScreenHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Packet sent from client to server when the player clicks the fluid gauge in the Fluid Supplier Pipe
 * GUI. The server resolves the action from context (there is nothing to carry in the payload itself):
 * if the player is carrying a bucket-like item that resolves to a fluid, the filter is set (or
 * replaced) from it; otherwise, if a filter is currently set, it is cleared. Routed through the
 * player's open {@link FluidSupplierScreenHandler}.
 */
public record ClickFluidSupplierGaugePacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClickFluidSupplierGaugePacket> TYPE =
            new CustomPacketPayload.Type<>(LogisticsPipe.resource("click_fluid_supplier_gauge").toIdentifier());

    public static final StreamCodec<RegistryFriendlyByteBuf, ClickFluidSupplierGaugePacket> CODEC =
            StreamCodec.unit(new ClickFluidSupplierGaugePacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayer player) {
        if (player.containerMenu instanceof FluidSupplierScreenHandler menu) {
            menu.onGaugeClicked(player);
        }
    }
}
