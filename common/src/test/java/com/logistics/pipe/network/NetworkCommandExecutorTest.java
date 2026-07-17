package com.logistics.pipe.network;

import com.logistics.core.lib.network.IWorldView;
import com.logistics.core.lib.network.NetworkCommand;
import com.logistics.core.lib.storage.IItemKey;
import com.logistics.test.MinecraftTestEnvironment;
import com.logistics.test.TestItemKey;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Maps the tri-state result of {@link IWorldView#dispatch} onto a {@link CommandResult}. */
@DisplayName("NetworkCommandExecutor")
class NetworkCommandExecutorTest extends MinecraftTestEnvironment {

    private long dispatchReturn;
    private Object[] lastDispatchArgs;
    private int dispatchCalls;

    private final IWorldView worldView = new IWorldView() {
        @Override
        public boolean isPipe(BlockPos pos) {
            return false;
        }

        @Override
        public List<BlockPos> getConnectedNeighbors(BlockPos pos) {
            return List.of();
        }

        @Override
        public boolean matchesSinkFilter(BlockPos pos, ItemStack stack) {
            return false;
        }

        @Override
        public long dispatch(BlockPos provider, BlockPos requester, IItemKey item, long amount, UUID deliveryId) {
            dispatchCalls++;
            lastDispatchArgs = new Object[] {provider, requester, item, amount, deliveryId};
            return dispatchReturn;
        }

        @Override
        public boolean isClientSide() {
            return false;
        }

        @Override
        public void broadcastAlert(BlockPos pos, Component message) {}

        @Override
        public long gameTime() {
            return 0L;
        }
    };

    private NetworkCommandExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new NetworkCommandExecutor(worldView);
    }

    private NetworkCommand.ExtractCommand extract(long amount) {
        return new NetworkCommand.ExtractCommand(
                new BlockPos(1, 2, 3),
                new BlockPos(4, 5, 6),
                new TestItemKey(Items.DIAMOND),
                amount,
                new UUID(7L, 8L));
    }

    @Test
    @DisplayName("a positive dispatch reports success with the shipped count")
    void extract_positiveShipped_isOk() {
        dispatchReturn = 5;
        CommandResult result = executor.execute(extract(8));
        assertThat(result).isEqualTo(CommandResult.ok(5));
    }

    @Test
    @DisplayName("a zero dispatch (nothing shipped) fails")
    void extract_zeroShipped_fails() {
        dispatchReturn = 0;
        CommandResult result = executor.execute(extract(8));
        assertThat(result).isEqualTo(CommandResult.failed());
    }

    @Test
    @DisplayName("a negative dispatch (provider busy) defers rather than failing")
    void extract_negativeShipped_defers() {
        dispatchReturn = -1;
        CommandResult result = executor.execute(extract(8));
        assertThat(result).isEqualTo(CommandResult.defer());
        assertThat(result.isDeferred()).isTrue();
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("an extract forwards its command fields verbatim to the world view")
    void extract_forwardsCommandFields() {
        dispatchReturn = 3;
        NetworkCommand.ExtractCommand cmd = extract(8);
        executor.execute(cmd);

        assertThat(dispatchCalls).isEqualTo(1);
        assertThat(lastDispatchArgs)
                .containsExactly(cmd.provider(), cmd.requester(), cmd.item(), cmd.amount(), cmd.deliveryId());
    }

    @Test
    @DisplayName("crafter-input insertion is a reserved no-op that fails without touching the world")
    void insertCrafterInputs_failsWithoutDispatch() {
        CommandResult result = executor.execute(
                new NetworkCommand.InsertCrafterInputsCommand(new BlockPos(0, 0, 0), Map.of()));
        assertThat(result).isEqualTo(CommandResult.failed());
        assertThat(dispatchCalls).isZero();
    }

    @Test
    @DisplayName("direct delivery is a reserved no-op that fails without touching the world")
    void deliver_failsWithoutDispatch() {
        CommandResult result = executor.execute(new NetworkCommand.DeliverCommand(
                new BlockPos(0, 0, 0), new TestItemKey(Items.DIAMOND), 4, new UUID(1L, 2L)));
        assertThat(result).isEqualTo(CommandResult.failed());
        assertThat(dispatchCalls).isZero();
    }
}
