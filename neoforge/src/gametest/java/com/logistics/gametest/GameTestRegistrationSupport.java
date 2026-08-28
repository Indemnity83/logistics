package com.logistics.gametest;

import com.logistics.core.lib.resource.ResourceId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Shared boilerplate for wiring a domain's {@link GameTestCase} list into MC's data-driven
 * GameTest registries. Each domain's {@code *GameTestRegistration} class:
 *
 * <ol>
 *   <li>declares its {@code TESTS} list and eagerly registers their functions via
 *       {@link #registerFunctions}, assigned to a static field so it runs before
 *       {@link GameTestFunctions#TEST_FUNCTION}'s bus registration (see
 *       {@code LogisticsGameTestMod});
 *   <li>calls {@link #registerInstances} from its own {@code @SubscribeEvent} handler for
 *       {@link RegisterGameTestsEvent}, passing a namespace-unique environment id — every
 *       domain's handler runs against the same event/registry, so a shared id across domains
 *       would collide.
 * </ol>
 *
 * <p>NeoForge's {@code GameTestInstance} ticks the environment/structure differently than Fabric's
 * {@code @GameTest} shim before handing control to the test body, so a {@link GameTestCase}'s
 * {@code maxTicks} generally needs ~20 more headroom here than its Fabric
 * {@code @GameTest(maxTicks=...)} counterpart for the same callbacks to land within budget.
 */
public final class GameTestRegistrationSupport {

    // Unlike the old annotation-based framework, a missing structure now hard-fails placement
    // instead of falling back to a generated empty box. Reuse vanilla's own "minecraft:empty"
    // structure — the same one GameTestInstances.ALWAYS_PASS uses — rather than shipping our own.
    private static final ResourceId EMPTY_STRUCTURE = ResourceId.of("empty");

    private GameTestRegistrationSupport() {}

    public static Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> registerFunctions(
            List<GameTestCase> tests) {
        Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> functions = new LinkedHashMap<>();
        for (GameTestCase test : tests) {
            functions.put(test.path(), GameTestFunctions.TEST_FUNCTION.register(test.path(), () -> test.body()));
        }
        return functions;
    }

    public static void registerInstances(
            RegisterGameTestsEvent event,
            String environmentId,
            List<GameTestCase> tests,
            Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> functions) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
            ResourceId.in("logistics_gametest", environmentId).toIdentifier(), new TestEnvironmentDefinition.AllOf());

        for (GameTestCase test : tests) {
            ResourceId id = ResourceId.in("logistics_gametest", test.path());
            ResourceKey<Consumer<GameTestHelper>> functionKey = functions.get(test.path()).getKey();
            TestData<Holder<TestEnvironmentDefinition<?>>> data =
                new TestData<>(environment, EMPTY_STRUCTURE.toIdentifier(), test.maxTicks(), 0, true);
            event.registerTest(id.toIdentifier(), new FunctionGameTestInstance(functionKey, data));
        }
    }
}
