package com.logistics.api;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public final class LogisticsApi {
    private LogisticsApi() {}

    public static final class Registry {
        private static final TransportApi NOOP_TRANSPORT = new NoopTransportApi();
        private static volatile TransportApi transportApi = NOOP_TRANSPORT;

        private Registry() {}

        public static TransportApi transport() {
            return transportApi;
        }

        public static void transport(TransportApi api) {
            transportApi = api == null ? NOOP_TRANSPORT : api;
        }

        private static final class NoopTransportApi implements TransportApi {
            @Override
            public boolean isTransportBlock(BlockState state) {
                return false;
            }

            @Override
            public boolean tryInsert(ServerLevel world, BlockPos targetPos, ItemStack stack, Direction from) {
                return false;
            }

            @Override
            public boolean forceInsert(ServerLevel world, BlockPos targetPos, ItemStack stack, Direction from) {
                return false;
            }
        }
    }
}
