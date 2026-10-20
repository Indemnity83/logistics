package com.logistics.gametest.core;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link MachineControlsGameTestBody}'s methods into NeoForge's GameTest discovery.
 *
 * <p>MC 1.21.1 predates the data-driven {@code TEST_FUNCTION} registry, so registration
 * here is the legacy reflection model: {@link GameTestHolder} makes NeoForge scan this
 * type, and {@link PrefixGameTestTemplate}(false) keeps the class name out of the
 * template id so every test can share {@code logistics_gametest:empty}.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class MachineControlsGameTestRegistration {

    private MachineControlsGameTestRegistration() {}

    @GameTest(template = "empty", batch = "machinecontrols")
    public static void testEveryServerMenuCarriesControls(GameTestHelper context) {
        MachineControlsGameTestBody.testEveryServerMenuCarriesControls(context);
    }

    @GameTest(template = "empty", batch = "machinecontrols")
    public static void testMaceratorMenuHandlerValidation(GameTestHelper context) {
        MachineControlsGameTestBody.testMaceratorMenuHandlerValidation(context);
    }

    @GameTest(template = "empty", batch = "machinecontrols")
    public static void testCropFarmMenuHandlerValidation(GameTestHelper context) {
        MachineControlsGameTestBody.testCropFarmMenuHandlerValidation(context);
    }

    @GameTest(template = "empty", batch = "machinecontrols")
    public static void testMagmaticEngineMenuHandlerValidation(GameTestHelper context) {
        MachineControlsGameTestBody.testMagmaticEngineMenuHandlerValidation(context);
    }

    @GameTest(template = "empty", batch = "machinecontrols")
    public static void testMaceratorRedstoneButtonPacketPath(GameTestHelper context) {
        MachineControlsGameTestBody.testMaceratorRedstoneButtonPacketPath(context);
    }

    @GameTest(template = "empty", batch = "machinecontrols", timeoutTicks = 280)
    public static void testMaceratorUsageTracksProcessing(GameTestHelper context) {
        MachineControlsGameTestBody.testMaceratorUsageTracksProcessing(context);
    }

    @GameTest(template = "empty", batch = "machinecontrols", timeoutTicks = 200)
    public static void testMagmaticGenerationWithinMax(GameTestHelper context) {
        MachineControlsGameTestBody.testMagmaticGenerationWithinMax(context);
    }
}
