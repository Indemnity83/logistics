package com.logistics;

/**
 * Registers every block and item the domains contribute, so tests can read the real
 * {@code BuiltInRegistries} content rather than a hand-maintained list.
 *
 * <p>Drives only the registration steps that write straight into {@code BuiltInRegistries}. Block
 * entities, menus, screens, creative tabs, and aliases must not be added: they resolve loader
 * services that have no implementation on the common test classpath. See TESTING.md.
 */
public final class DomainRegistrations {

    private static boolean registered = false;

    private DomainRegistrations() {}

    /**
     * Idempotent — {@code Registry.register} throws on a duplicate key, so repeated calls across
     * test classes in one JVM must not re-register.
     */
    public static synchronized void ensureRegistered() {
        if (registered) {
            return;
        }
        registered = true;

        LogisticsCore.BLOCK.register();
        LogisticsCore.ITEM.register();
        LogisticsCore.BUCKET.register();
        LogisticsPipe.BLOCK.register();
        LogisticsPipe.ITEM.register();
        LogisticsPower.BLOCK.register();
        LogisticsAutomation.BLOCK.register();
    }
}
