package com.logistics.pipe.network;

import com.logistics.LogisticsPipe;
import com.logistics.pipe.ChassisPipe;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.item.ModuleItem;
import com.logistics.pipe.ui.ChassisScreenHandler;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Sent client→server when the player clicks a chassis slot's "!" button.
 * The server looks up the player's currently-open {@link ChassisScreenHandler},
 * reads the module item from the requested slot, and calls its
 * {@link com.logistics.pipe.modules.Module#onWrench} to open its config screen.
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

    public static void register() {
        PayloadTypeRegistry.playC2S().register(TYPE, CODEC);

        ServerPlayNetworking.registerGlobalReceiver(TYPE, (packet, context) -> {
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                if (!(player.containerMenu instanceof ChassisScreenHandler handler)) return;

                PipeBlockEntity pe = handler.getPipeEntity();
                if (pe == null) return;

                var ctx = pe.createContext();
                Tag tag = ctx.moduleState(ChassisPipe.STATE_KEY).get(String.valueOf(packet.slotIndex));
                if (tag == null) return;

                ItemStack.CODEC.parse(NbtOps.INSTANCE, tag).result()
                        .map(ItemStack::getItem)
                        .filter(item -> item instanceof ModuleItem)
                        .map(item -> ((ModuleItem) item).createModule())
                        .ifPresent(module -> module.onWrench(ctx, player));
            });
        });
    }
}
