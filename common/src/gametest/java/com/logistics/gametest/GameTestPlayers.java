package com.logistics.gametest;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

/**
 * Server players for GameTest bodies that are never logged in or added to the level, so they need no
 * connection, fire no login events or payloads, and leave nothing behind in the shared test level.
 *
 * <p>Use these instead of {@code makeMockServerPlayerInLevel}, whose real login sends loader payloads
 * (JEI's sync, for one) over a connection that never negotiated them.
 */
public final class GameTestPlayers {
    private GameTestPlayers() {}

    /** A server player that reports {@code mode} as its game mode and holds that mode's abilities. */
    public static ServerPlayer mockServerPlayer(GameTestHelper context, GameType mode) {
        return new MockServerPlayer(context, mode, "test-mock-player");
    }

    /**
     * The player {@link #mockServerPlayer} builds, open for subclasses that also capture what the player
     * is sent. The game mode is fixed for the player's lifetime.
     *
     * <p>Only the overrides differ per version. 26.x and 1.21.11 override {@code gameMode()}; 1.21.1, which
     * has no {@code gameMode()}, overrides {@code isCreative()} and {@code isSpectator()}. Where it exists,
     * {@code isClientAuthoritative()} returns false, as vanilla's own mock player does from 26.2.
     */
    public static class MockServerPlayer extends ServerPlayer {
        private final GameType mode;

        public MockServerPlayer(GameTestHelper context, GameType mode, String name) {
            super(
                    context.getLevel().getServer(),
                    context.getLevel(),
                    new GameProfile(UUID.randomUUID(), name),
                    ClientInformation.createDefault());
            this.mode = mode;
            mode.updatePlayerAbilities(getAbilities());
        }

        @Override
        public GameType gameMode() {
            return mode;
        }

        @Override
        public boolean isClientAuthoritative() {
            return false;
        }
    }
}
