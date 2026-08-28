package com.logistics.gametest;

import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * One test: the path segment its function/instance is registered under (relative to
 * {@code logistics_gametest:}), the tick budget, and the shared body to run. See
 * {@link GameTestRegistrationSupport}.
 */
public record GameTestCase(String path, int maxTicks, Consumer<GameTestHelper> body) {}
